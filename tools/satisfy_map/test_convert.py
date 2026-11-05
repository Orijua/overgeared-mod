"""Pruebas de convert.py. Se pueden ejecutar con `python3 -m unittest` o con `pytest` desde esta carpeta.

Las pruebas con imagenes usan un mapa sintetico pequeno; las del recurso real (satisfy_map/west) se saltan si no existe,
y la de regeneracion se salta si falta el PNG del canon (vive en el repo de docs).
"""
import json
import sys
import tempfile
import unittest
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import convert as c  # noqa: E402

SEA_RGB = (88, 118, 124)
GRASS_RGB = (113, 107, 19)
ROCK_RGB = (86, 66, 57)
RIVER_RGB = (103, 138, 142)
TEXT_RGB = (30, 25, 20)
EMPTY_OVERRIDES = {"origin_px": [128, 96], "ignore_rects": [], "forests": [], "protected": []}
REAL_OUT = c.HERE.parents[1] / "src/main/resources/data/overgearedmod/satisfy_map/west"
REAL_SOURCE = Path("/home/user/overgeared-docs/canon/Mapa_continente_oeste.png")


def synthetic_map() -> np.ndarray:
    """256x192 px: mar, una isla de hierba (64..192 x 48..144) con un rio estrecho y una marca de texto en el mar."""
    img = np.zeros((192, 256, 3), np.uint8)
    img[:] = SEA_RGB
    img[48:144, 64:192] = GRASS_RGB
    img[90:96, 64:192] = RIVER_RGB  # rio de 6 px: debe volverse tierra
    img[20:26, 20:26] = TEXT_RGB  # texto suelto en el mar: debe seguir siendo mar
    return img


def write_png(arr: np.ndarray, path: Path) -> None:
    Image.fromarray(arr).save(path)


class ClassifyColorsTest(unittest.TestCase):
    def cls(self, rgb):
        return int(c.classify_colors(np.array([[rgb]], np.uint8))[0, 0])

    def test_palette_of_the_real_map(self):
        self.assertEqual(self.cls((79, 110, 116)), c.WATER)
        self.assertEqual(self.cls((153, 180, 183)), c.WATER)  # halo claro de la costa
        self.assertEqual(self.cls((113, 107, 19)), c.GRASS)
        self.assertEqual(self.cls((133, 110, 45)), c.GRASS)
        self.assertEqual(self.cls((234, 200, 125)), c.DESERT)
        self.assertEqual(self.cls((187, 124, 48)), c.ORANGE)
        self.assertEqual(self.cls((86, 66, 57)), c.ROCK)
        self.assertEqual(self.cls((225, 227, 223)), c.SNOW)
        self.assertEqual(self.cls((117, 195, 227)), c.SNOW)
        self.assertEqual(self.cls((20, 15, 10)), c.DARK)  # texto negro

    def test_snow_is_not_confused_with_sea(self):
        self.assertNotEqual(self.cls((117, 195, 227)), c.WATER)
        self.assertNotEqual(self.cls(SEA_RGB), c.SNOW)


class MorphologyTest(unittest.TestCase):
    def test_disk_opening_removes_thin_lines_but_keeps_blobs(self):
        m = np.zeros((60, 60), bool)
        m[10:50, 10:50] = True
        m[0:60, 30:32] = True  # linea fina
        opened = c.disk_opening(m, 3)
        self.assertTrue(opened[30, 15])
        self.assertFalse(opened[5, 30], "la linea fina que sale del bloque desaparece")

    def test_remove_small(self):
        m = np.zeros((20, 20), bool)
        m[2:4, 2:4] = True
        m[10:18, 10:18] = True
        out = c.remove_small(m, 10)
        self.assertFalse(out[2, 2])
        self.assertTrue(out[12, 12])

    def test_fill_nearest(self):
        v = np.array([[1, 0, 0, 2]])
        known = np.array([[True, False, False, True]])
        self.assertEqual(c.fill_nearest(v, known).tolist(), [[1, 1, 2, 2]])


class SyntheticMapTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        cls.src = Path(cls.tmp.name) / "map.png"
        write_png(synthetic_map(), cls.src)
        cls.overrides = Path(cls.tmp.name) / "overrides.json"
        cls.overrides.write_text(json.dumps(EMPTY_OVERRIDES))
        cls.out = Path(cls.tmp.name) / "out"
        cls.meta = c.convert(cls.src, cls.out, 16, cls.overrides, Path(cls.tmp.name) / "preview")
        cls.region = np.asarray(Image.open(cls.out / "region.png"))
        cls.relief = np.asarray(Image.open(cls.out / "relief.png"))

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def test_outputs_have_the_expected_format(self):
        with Image.open(self.out / "region.png") as im:
            self.assertEqual(im.mode, "P")
            self.assertEqual(im.size, (16, 12))
        with Image.open(self.out / "relief.png") as im:
            self.assertEqual(im.mode, "L")
            self.assertEqual(im.size, (16, 12))
        self.assertTrue((self.out.parent / "preview" / "west_c16_regiones.png").exists())
        self.assertTrue((self.out.parent / "preview" / "west_c16_sobre_mapa.png").exists())

    def test_meta(self):
        m = self.meta
        self.assertEqual(m["version"], c.VERSION)
        self.assertEqual((m["blocks_per_pixel"], m["cell_px"], m["cell_blocks"]), (2, 16, 32))
        self.assertEqual((m["width"], m["height"]), (16, 12))
        self.assertEqual((m["pixel_width"], m["pixel_height"]), (256, 192))
        self.assertEqual(m["origin"], {"px": [128, 96], "col": 8, "row": 6, "name": "Bairan"})
        self.assertEqual(m["region_legend"]["0"], "sea")
        self.assertEqual(m["region_legend"]["1"], "minecraft:plains")
        self.assertEqual(m["region_legend"]["8"], "minecraft:savanna")
        self.assertEqual(json.loads((self.out / "meta.json").read_text()), m)

    def test_land_and_sea(self):
        self.assertEqual(self.region[6, 8], c.PLAINS)
        self.assertEqual(self.region[0, 0], c.SEA)
        self.assertEqual(self.relief[0, 0], 1, "mar a 3 celdas de la costa, entre la plataforma y el mar profundo")
        self.assertTrue(self.relief[6, 8] >= 4)

    def test_coast_is_relief_3(self):
        land = self.region != c.SEA
        for row in range(1, 11):
            for col in range(1, 15):
                if land[row, col] and not (land[row - 1:row + 2, col - 1:col + 2]).all():
                    self.assertEqual(self.relief[row, col], 3, f"costa en ({col}, {row})")

    def test_thin_river_becomes_land_and_text_in_sea_vanishes(self):
        self.assertNotEqual(self.region[90 // 16 + 0, 8], c.SEA, "el rio de 6 px no abre un canal")
        self.assertEqual(self.region[1, 1], c.SEA, "la marca de texto no es una isla")
        self.assertEqual(self.relief[1, 1], 1)

    def test_region_zero_iff_relief_below_3(self):
        self.assertTrue(np.array_equal(self.region == 0, self.relief <= 2))

    def test_cell_px_must_be_a_multiple_of_the_work_scale(self):
        with self.assertRaises(ValueError):
            c.convert(self.src, self.out, 15, self.overrides, None)

    def test_finer_cells_keep_the_same_island(self):
        out8 = Path(self.tmp.name) / "out8"
        meta = c.convert(self.src, out8, 8, self.overrides, None)
        region8 = np.asarray(Image.open(out8 / "region.png"))
        self.assertEqual((meta["width"], meta["height"]), (32, 24))
        self.assertEqual(meta["cell_blocks"], 16)
        self.assertEqual(region8[12, 16], c.PLAINS)
        # La isla ocupa 128x96 px: 16x12 celdas de 8 px.
        self.assertEqual(int((region8 != 0).sum()), 16 * 12)

    def test_mountains_get_higher_relief(self):
        img = synthetic_map()
        # Franja de "iconos de montana": rayas alternas de roca oscura sobre la hierba.
        for x in range(96, 160, 4):
            img[60:130, x:x + 2] = ROCK_RGB
        out = Path(self.tmp.name) / "mount"
        src = Path(self.tmp.name) / "mount.png"
        write_png(img, src)
        c.convert(src, out, 16, self.overrides, None)
        relief = np.asarray(Image.open(out / "relief.png"))
        self.assertGreater(int(relief[6, 8]), int(self.relief[6, 8]))


class RealDataTest(unittest.TestCase):
    """Comprueba el recurso generado que va en el mod."""

    def setUp(self):
        if not (REAL_OUT / "meta.json").exists():
            self.skipTest("no hay datos generados")
        self.meta = json.loads((REAL_OUT / "meta.json").read_text(encoding="utf-8"))
        self.region = np.asarray(Image.open(REAL_OUT / "region.png"))
        self.relief = np.asarray(Image.open(REAL_OUT / "relief.png"))

    def test_dimensions_and_ranges(self):
        self.assertEqual(self.region.shape, (self.meta["height"], self.meta["width"]))
        self.assertEqual(self.relief.shape, self.region.shape)
        self.assertLessEqual(int(self.relief.max()), 9)
        self.assertTrue(set(np.unique(self.region).tolist()) <= {int(k) for k in self.meta["region_legend"]})
        self.assertTrue(np.array_equal(self.region == 0, self.relief <= 2))

    def test_bairan_is_on_land_in_the_plains(self):
        col, row = self.meta["origin"]["col"], self.meta["origin"]["row"]
        self.assertEqual(self.region[row, col], c.PLAINS)
        self.assertTrue(4 <= self.relief[row, col] <= 5)

    def test_the_map_has_sea_all_around_and_known_places(self):
        self.assertTrue((self.region[0] == 0).all() and (self.region[-1] == 0).all())
        self.assertTrue((self.region[:, 0] == 0).all() and (self.region[:, -1] == 0).all())
        cell = self.meta["cell_px"]

        def at(px, py):
            return int(self.region[py // cell, px // cell]), int(self.relief[py // cell, px // cell])

        self.assertEqual(at(911, 754)[0], c.DESERT_R, "Lilton Desert")
        self.assertEqual(at(2400, 1500)[0], c.HILLS, "Travian Sierra (cerca del Leviathan nest)")
        self.assertGreaterEqual(at(2400, 1500)[1], 7)
        self.assertIn(at(1690, 1614)[0], (c.PLAINS, c.SAVANNA), "Titan: tierra llana")
        self.assertEqual(at(3963, 2601)[1] <= 3, True, "Hall of Fame es una isla pequena: como mucho costa")
        self.assertEqual(at(3600, 1500)[0], c.SEA, "Red Sea")


@unittest.skipUnless(REAL_SOURCE.exists(), "falta el PNG del canon")
class RegenerationTest(unittest.TestCase):
    def test_regenerating_gives_the_committed_data(self):
        if not (REAL_OUT / "meta.json").exists():
            self.skipTest("no hay datos generados")
        meta = json.loads((REAL_OUT / "meta.json").read_text(encoding="utf-8"))
        with tempfile.TemporaryDirectory() as tmp:
            c.convert(REAL_SOURCE, Path(tmp), meta["cell_px"], c.DEFAULT_OVERRIDES, None)
            for name in ("region.png", "relief.png"):
                self.assertTrue(np.array_equal(np.asarray(Image.open(Path(tmp) / name)), np.asarray(Image.open(REAL_OUT / name))), name)
            self.assertEqual(json.loads((Path(tmp) / "meta.json").read_text()), meta)


if __name__ == "__main__":
    unittest.main()
