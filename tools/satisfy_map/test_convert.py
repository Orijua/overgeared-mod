"""Pruebas de convert.py. Se pueden ejecutar con `python3 -m unittest` o con `pytest` desde esta carpeta.

Las pruebas con imagenes usan un mapa sintetico pequeno. Las del recurso real (satisfy_map/west) estan comiteadas en el
repo del mod: si faltan, la prueba FALLA. Solo la de regeneracion se salta si falta el PNG del canon (vive en el repo
de docs): se busca en la variable de entorno SATISFY_MAP_SOURCE o, por defecto, en ../../../overgeared-docs/canon/ respecto
a esta carpeta (el repo hermano).
"""
import json
import os
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
REAL_SOURCE = Path(os.environ.get("SATISFY_MAP_SOURCE", c.HERE.parents[2] / "overgeared-docs/canon/Mapa_continente_oeste.png"))
# Versiones con las que se comprobo que la regeneracion da bytes identicos (ver README).
REFERENCE_VERSIONS = {"numpy": "2.5.3", "scipy": "1.18.1", "Pillow": "12.3.0"}
MAX_DIFFERENT_CELLS = 0.005  # con otras versiones, hasta el 0,5 % de celdas distintas


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

    def test_color_thresholds(self):
        # Limites de classify_colors: nieve azul frente a agua (v > 0.78 y s > 0.36 dejan de ser agua).
        self.assertEqual(self.cls((117, 198, 198)), c.WATER)
        self.assertEqual(self.cls((117, 199, 199)), c.SNOW)
        # Naranja frente a desierto (g < 150 es naranja; g > 160 es desierto) y a hierba entre medias.
        self.assertEqual(self.cls((200, 149, 60)), c.ORANGE)
        self.assertNotEqual(self.cls((200, 150, 60)), c.ORANGE)
        self.assertEqual(self.cls((200, 161, 100)), c.DESERT)
        self.assertNotEqual(self.cls((200, 160, 100)), c.DESERT)

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
        # Relieve 1: mar entre --shallow-blocks (64 bloques = 2 celdas, relieve 2) y --mid-blocks (160 bloques = 5 celdas);
        # la esquina (0, 0) esta a 3 celdas de la isla.
        self.assertEqual(self.relief[0, 0], 1, "mar a 3 celdas de la costa, entre la plataforma y el mar profundo")
        self.assertTrue(self.relief[6, 8] >= 4)

    def test_coast_is_relief_3(self):
        land = self.region != c.SEA
        for row in range(1, 11):
            for col in range(1, 15):
                if land[row, col] and not (land[row - 1:row + 2, col - 1:col + 2]).all():
                    self.assertEqual(self.relief[row, col], 3, f"costa en ({col}, {row})")

    def test_thin_river_becomes_land_and_text_in_sea_vanishes(self):
        # Comprobacion de la salida a escala de celda (16 px); la del rio a escala de trabajo esta en AnalyseTest.
        self.assertNotEqual(self.region[90 // 16, 8], c.SEA, "el rio de 6 px no abre un canal")
        self.assertEqual(self.region[1, 1], c.SEA, "la marca de texto no es una isla")
        self.assertEqual(self.relief[1, 1], 1, "mismo mar a 3 celdas de la costa que la esquina: relieve 1")

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


class AnalyseTest(unittest.TestCase):
    """analyse() a resolucion de trabajo (la celda de 16 px diluye un rio de 6 px: aqui se ve el pixel)."""

    def test_thin_river_stays_land(self):
        an = c.analyse(synthetic_map(), EMPTY_OVERRIDES)
        rows = slice(90 // c.WORK, 95 // c.WORK + 1)
        cols = slice(70 // c.WORK, 186 // c.WORK)  # dentro de la isla (64..192)
        self.assertFalse(an.sea[rows, cols].any(), "el rio de 6 px no es mar")
        self.assertFalse(an.lake[rows, cols].any(), "ni lago")

    def test_wide_river_is_sea(self):
        # Contrapunto: un rio de 40 px (mas ancho que river_px = 24) que toca el mar por los dos extremos es mar.
        img = synthetic_map()
        img[80:120, 64:192] = RIVER_RGB
        an = c.analyse(img, EMPTY_OVERRIDES, river_px=24.0)
        self.assertTrue(an.sea[95 // c.WORK:105 // c.WORK, 80 // c.WORK:176 // c.WORK].all(), "el rio ancho abre un canal de mar")

    def test_ignore_rects_turn_land_into_sea(self):
        overrides = dict(EMPTY_OVERRIDES, ignore_rects=[{"name": "t", "box": [64, 48, 104, 144]}])
        an = c.analyse(synthetic_map(), overrides)
        self.assertTrue(an.sea[72 // c.WORK, 84 // c.WORK], "la franja ignorada es mar")
        self.assertEqual(int(an.region[72 // c.WORK, 84 // c.WORK]), c.SEA)
        self.assertNotEqual(int(an.region[72 // c.WORK, 150 // c.WORK]), c.SEA, "el resto de la isla sigue siendo tierra")

    def test_forest_ellipse_sets_the_forest_region(self):
        overrides = dict(EMPTY_OVERRIDES, forests=[{"name": "f", "kind": "ellipse", "center": [128, 96], "radii": [20, 20]}])
        an = c.analyse(synthetic_map(), overrides)
        self.assertEqual(int(an.region[96 // c.WORK, 128 // c.WORK]), c.FOREST)
        self.assertEqual(int(c.analyse(synthetic_map(), EMPTY_OVERRIDES).region[96 // c.WORK, 128 // c.WORK]), c.PLAINS)

    def test_protected_site_stays_plains_and_low(self):
        img = synthetic_map()
        for x in range(70, 190, 4):  # montanas por toda la isla
            img[52:140, x:x + 2] = ROCK_RGB
        free = c.analyse(img, EMPTY_OVERRIDES)
        overrides = dict(EMPTY_OVERRIDES, protected=[{"name": "p", "center": [128, 96], "radius_px": 20, "region": "plains", "max_relief": 5}])
        held = c.analyse(img, overrides)
        centre = (96 // c.WORK, 128 // c.WORK)
        self.assertGreater(float(free.relief[centre]), 5.0, "sin proteger, la montana sube")
        self.assertEqual(int(held.region[centre]), c.PLAINS)
        self.assertLessEqual(float(held.relief[centre]), 5.0)


class ConvertEdgeCasesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        cls.dir = Path(cls.tmp.name)
        cls.src = cls.dir / "map.png"
        write_png(synthetic_map(), cls.src)
        cls.overrides = cls.dir / "overrides.json"
        cls.overrides.write_text(json.dumps(EMPTY_OVERRIDES))

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def read(self, folder):
        return [(folder / name).read_bytes() for name in ("region.png", "relief.png", "meta.json")]

    def test_conversion_is_deterministic(self):
        c.convert(self.src, self.dir / "a", 16, self.overrides, None)
        c.convert(self.src, self.dir / "b", 16, self.overrides, None)
        self.assertEqual(self.read(self.dir / "a"), self.read(self.dir / "b"))

    def test_rgba_input_is_accepted_like_rgb(self):
        rgba = np.dstack([synthetic_map(), np.full((192, 256), 255, np.uint8)])
        Image.fromarray(rgba, "RGBA").save(self.dir / "rgba.png")
        c.convert(self.src, self.dir / "rgb_out", 16, self.overrides, None)
        c.convert(self.dir / "rgba.png", self.dir / "rgba_out", 16, self.overrides, None)
        self.assertEqual(self.read(self.dir / "rgb_out")[:2], self.read(self.dir / "rgba_out")[:2])

    def test_origin_off_the_map_is_refused(self):
        for origin in ([300, 96], [128, 200], [-1, 10]):
            path = self.dir / "off.json"
            path.write_text(json.dumps(dict(EMPTY_OVERRIDES, origin_px=origin)))
            with self.assertRaises(ValueError, msg=str(origin)):
                c.convert(self.src, self.dir / "off", 16, path, None)


class RealDataTest(unittest.TestCase):
    """Comprueba el recurso generado que va en el mod."""

    def setUp(self):
        if not (REAL_OUT / "meta.json").exists():
            self.fail(f"faltan los datos comiteados en {REAL_OUT}")
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
        self.assertLessEqual(at(3963, 2601)[1], 3, "Hall of Fame es una isla pequena: como mucho costa")
        self.assertEqual(at(3600, 1500)[0], c.SEA, "Red Sea")


def installed_versions() -> dict:
    import PIL
    import scipy
    return {"numpy": np.__version__, "scipy": scipy.__version__, "Pillow": PIL.__version__}


class RegenerationTest(unittest.TestCase):
    def test_regenerating_gives_the_committed_data(self):
        if not (REAL_OUT / "meta.json").exists():
            self.fail(f"faltan los datos comiteados en {REAL_OUT}")
        if not REAL_SOURCE.exists():
            self.skipTest(f"falta el PNG del canon en {REAL_SOURCE}: define la variable de entorno SATISFY_MAP_SOURCE")
        meta = json.loads((REAL_OUT / "meta.json").read_text(encoding="utf-8"))
        same_versions = installed_versions() == REFERENCE_VERSIONS
        with tempfile.TemporaryDirectory() as tmp:
            c.convert(REAL_SOURCE, Path(tmp), meta["cell_px"], c.DEFAULT_OVERRIDES, None)
            for name in ("region.png", "relief.png"):
                new = np.asarray(Image.open(Path(tmp) / name))
                old = np.asarray(Image.open(REAL_OUT / name))
                if same_versions:
                    self.assertTrue(np.array_equal(new, old), name)
                else:
                    print(f"AVISO: versiones {installed_versions()} distintas de {REFERENCE_VERSIONS}: se compara con tolerancia")
                    self.assertEqual(new.shape, old.shape, name)
                    self.assertLessEqual(float((new != old).mean()), MAX_DIFFERENT_CELLS, name)
            self.assertEqual(json.loads((Path(tmp) / "meta.json").read_text()), meta)


if __name__ == "__main__":
    unittest.main()
