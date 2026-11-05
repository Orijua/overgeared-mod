#!/usr/bin/env python3
"""Convierte el mapa del canon (continente oeste) en datos por celda para el terreno de Satisfy.

Entrada : canon/Mapa_continente_oeste.png (4096x3072, 2 bloques por pixel).
Salida  : region.png (indexado 8 bits), relief.png (gris 0-9), meta.json y una vista previa coloreada.

La clasificacion es por color (el mapa es una ilustracion): ver README.md y overrides.json.
Uso: python3 convert.py --help
"""
from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage as ndi

Image.MAX_IMAGE_PIXELS = None

VERSION = 1
BLOCKS_PER_PIXEL = 2
WORK = 2  # el analisis se hace a mitad de resolucion (1 pixel de trabajo = 2x2 pixeles del mapa)

HERE = Path(__file__).resolve().parent
DEFAULT_OVERRIDES = HERE / "overrides.json"

# --- Clases de color por pixel ------------------------------------------------------------------------------
UNKNOWN, WATER, GRASS, DESERT, ORANGE, ROCK, SNOW, DARK = range(8)

# --- Regiones (indices de region.png). Los 1-8 siguen el orden de la leyenda de SatisfyMap (P F D T S K M A). ---
SEA = 0
PLAINS, FOREST, DESERT_R, TAIGA, SNOWY_PLAINS, SNOWY_TAIGA, HILLS, SAVANNA, BADLANDS, VOLCANO, PEAKS = range(1, 12)
REGIONS = {
    SEA: ("sea", "mar", (40, 90, 140)),
    PLAINS: ("minecraft:plains", "llanura", (120, 170, 60)),
    FOREST: ("minecraft:forest", "bosque", (30, 110, 40)),
    DESERT_R: ("minecraft:desert", "desierto", (240, 220, 120)),
    TAIGA: ("minecraft:taiga", "taiga", (60, 120, 100)),
    SNOWY_PLAINS: ("minecraft:snowy_plains", "llanura nevada", (235, 240, 245)),
    SNOWY_TAIGA: ("minecraft:snowy_taiga", "taiga nevada", (160, 200, 200)),
    HILLS: ("minecraft:windswept_hills", "colinas ventosas", (140, 110, 80)),
    SAVANNA: ("minecraft:savanna", "sabana", (190, 160, 70)),
    BADLANDS: ("minecraft:badlands", "mesas / tierras baldias (naranja)", (210, 110, 40)),
    VOLCANO: ("minecraft:stony_peaks", "volcan (Veldon)", (50, 45, 50)),
    PEAKS: ("minecraft:frozen_peaks", "picos nevados", (200, 225, 255)),
}
REGION_IDS = {REGIONS[i][0].split(":")[-1]: i for i in REGIONS}

RELIEF_MEANING = {
    0: "mar profundo", 1: "mar somero (lejos de la costa)", 2: "mar somero (junto a la costa) y lagos",
    3: "costa", 4: "llanura", 5: "llanura alta / pie de monte", 6: "colinas", 7: "montana baja", 8: "montana", 9: "pico",
}


# ------------------------------------------------------------------------------------------------------------
# Clasificacion por color
# ------------------------------------------------------------------------------------------------------------
def rgb_features(rgb: np.ndarray):
    """(R, G, B, valor, saturacion, matiz en grados) como float32 a partir de un array (...,3) en 0-255."""
    a = rgb.astype(np.float32)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    mx = a.max(axis=-1)
    mn = a.min(axis=-1)
    d = mx - mn + 1e-6
    v = mx / 255.0
    s = (mx - mn) / (mx + 1e-6)
    h = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) * 60.0
    return r, g, b, v, s, h


def classify_colors(rgb: np.ndarray) -> np.ndarray:
    """Clase de color (UNKNOWN, WATER, GRASS, DESERT, ORANGE, ROCK, SNOW, DARK) de cada pixel.

    Paleta real del mapa (ver README): mar azul grisaceo (79,110,116)..(153,180,183); hierba oliva (113,107,19) o
    pardo-oliva (133,110,45); desierto amarillo palido (234,200,125); mesas naranja (187,124,48); roca parda
    (74,66,57)..(163,142,112); nieve blanca (225,227,223) o azul (117,195,227); texto/iconos casi negros; castillos
    azul oscuro (B > R, no agua).
    """
    r, g, b, v, s, _ = rgb_features(rgb)
    snow_blue = (v > 0.74) & (s > 0.30) & (b > r + 55)
    water = (b > r + 10) & (g > r + 6) & (s > 0.07) & ~((v > 0.78) & (s > 0.36))
    white = (v > 0.74) & (s < 0.14) & (b >= r - 14)
    desert = (r > 195) & (g > 160) & (b < 170) & (b > 70) & (r >= g) & ~white
    orange = (r > 150) & (g < 150) & (b < 70) & (r - g > 35)
    dark = v < 0.17
    rock = (b > 0.47 * r) & (b <= r * 1.02) & (v < 0.74) & (s < 0.55) & ~water & ~dark & ~desert
    grass = (r >= b) & ~rock
    cls = np.zeros(rgb.shape[:-1], np.uint8)  # UNKNOWN
    # El orden importa: lo ultimo que se escribe gana.
    cls[grass] = GRASS
    cls[desert] = DESERT
    cls[orange] = ORANGE
    cls[rock] = ROCK
    cls[white | snow_blue] = SNOW
    cls[water] = WATER
    cls[dark] = DARK
    return cls


# ------------------------------------------------------------------------------------------------------------
# Utilidades de imagen
# ------------------------------------------------------------------------------------------------------------
def downsample(rgb: np.ndarray, f: int) -> np.ndarray:
    """Media por bloques f x f (los bordes sobrantes se descartan)."""
    h, w = rgb.shape[0] // f * f, rgb.shape[1] // f * f
    return rgb[:h, :w].reshape(h // f, f, w // f, f, 3).mean(axis=(1, 3), dtype=np.float32)


def disk_opening(mask: np.ndarray, radius: float) -> np.ndarray:
    """Apertura morfologica con un disco: quita lo mas estrecho que 2*radius (el borde de la imagen cuenta como 'mas')."""
    if radius <= 0:
        return mask.copy()
    padded = np.pad(mask, int(math.ceil(radius)) + 1, mode="edge")
    eroded = ndi.distance_transform_edt(padded) > radius
    opened = ndi.distance_transform_edt(~eroded) <= radius
    p = int(math.ceil(radius)) + 1
    return opened[p:-p, p:-p] & mask


def remove_small(mask: np.ndarray, min_area: int) -> np.ndarray:
    """Quita los componentes conexos de `mask` con menos de min_area pixeles."""
    labels, n = ndi.label(mask)
    if n == 0:
        return mask
    sizes = ndi.sum(mask, labels, index=np.arange(1, n + 1))
    keep = np.zeros(n + 1, bool)
    keep[1:] = sizes >= min_area
    return keep[labels]


def fill_nearest(values: np.ndarray, known: np.ndarray) -> np.ndarray:
    """Sustituye lo desconocido por el valor conocido mas cercano."""
    if known.all() or not known.any():
        return values
    idx = ndi.distance_transform_edt(~known, return_distances=False, return_indices=True)
    return values[tuple(idx)]


def normalized_blur(x: np.ndarray, weight: np.ndarray, sigma: float) -> np.ndarray:
    """Media gaussiana de x contando solo los pixeles con peso (convolucion normalizada)."""
    w = weight.astype(np.float32)
    return ndi.gaussian_filter(x.astype(np.float32) * w, sigma) / (ndi.gaussian_filter(w, sigma) + 1e-6)


def rect_mask(shape, box, f: int) -> np.ndarray:
    """Mascara de un rectangulo (x0, y0, x1, y1) dado en pixeles del mapa, en resolucion de trabajo."""
    m = np.zeros(shape, bool)
    x0, y0, x1, y1 = (int(round(c / f)) for c in box)
    m[max(y0, 0):max(y1, 0), max(x0, 0):max(x1, 0)] = True
    return m


def ellipse_mask(shape, center, radii, f: int) -> np.ndarray:
    yy, xx = np.mgrid[0:shape[0], 0:shape[1]]
    cx, cy = center[0] / f, center[1] / f
    rx, ry = radii[0] / f, radii[1] / f
    return ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2 <= 1.0


# ------------------------------------------------------------------------------------------------------------
# Analisis a resolucion de trabajo
# ------------------------------------------------------------------------------------------------------------
class Analysis:
    """Campos por pixel de trabajo: sea, lake (mascaras), relief (float 4-9, solo tierra) y region (indices)."""

    def __init__(self, sea, lake, relief, region, work):
        self.sea, self.lake, self.relief, self.region, self.work = sea, lake, relief, region, work


def analyse(rgb: np.ndarray, overrides: dict, river_px: float = 24.0, min_island_px: int = 700,
            min_lake_px: int = 1600, snow_belts_px: tuple = (48, 140), savanna_belt_px: float = 300.0) -> Analysis:
    """Clasifica el mapa. Las distancias se dan en pixeles del mapa original (no de trabajo)."""
    f = WORK
    work = downsample(rgb, f)
    cls = classify_colors(work)
    shape = cls.shape

    # 1. Zonas que se descartan (titulo, brujula, barcos...): son mar.
    ignore = np.zeros(shape, bool)
    for item in overrides.get("ignore_rects", []):
        ignore |= rect_mask(shape, item["box"], f)
    cls[ignore] = WATER

    # 2. Tierra / agua. Todo lo que no es agua es tierra; se quitan lineas finas (texto, rutas de barco, espuma de la
    #    costa) con una apertura, y las islas diminutas. Los rios estrechos se vuelven tierra (rios: para despues).
    land = ~(cls == WATER)
    land = disk_opening(land, 2)
    land = remove_small(land, max(min_island_px // (f * f), 1))
    water = ~land
    water = remove_small(water, 1)
    wide = disk_opening(water, river_px / f / 2)
    labels, n = ndi.label(wide)
    border = np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))
    sea = np.isin(labels, border[border > 0])
    lake = wide & ~sea
    lake = remove_small(lake, max(min_lake_px // (f * f), 1))
    water = sea | lake
    land = ~water

    # 3. Clases de color de la tierra: los iconos, el texto y los rios (agua quitada) se rellenan con lo mas cercano.
    known = land & np.isin(cls, (GRASS, DESERT, ORANGE, ROCK, SNOW))
    cls_land = fill_nearest(cls, known)
    cls_land[water] = WATER
    icons = ndi.binary_dilation(land & np.isin(cls, (UNKNOWN, DARK)), iterations=3)

    r, g, b, v, s, h = rgb_features(work)
    lum = work.mean(axis=-1)

    # 4. Relieve de tierra: densidad de pixeles oscuros (los iconos de montanas son muy oscuros respecto al suelo).
    ground = land & (cls == GRASS) & (lum > 60)
    ref = normalized_blur(lum, ground, 30)
    dark_px = ((lum < 0.8 * ref) | (cls_land == ROCK)) & land & ~icons
    mountain = ndi.gaussian_filter(dark_px.astype(np.float32), 10)
    relief = 4.0 + 5.0 * np.clip((mountain - 0.15) / (0.80 - 0.15), 0.0, 1.0)

    # 5. Campos de color suavizados.
    sigma = 8
    snow_s = ndi.gaussian_filter(((cls_land == SNOW) & land).astype(np.float32), sigma)
    des_s = ndi.gaussian_filter(((cls_land == DESERT) & land).astype(np.float32), sigma)
    org_s = ndi.gaussian_filter(((cls_land == ORANGE) & land).astype(np.float32), sigma)
    leaf = land & (cls == GRASS) & (v > 0.35) & (v < 0.8)
    hue_s = normalized_blur(h, leaf, 20)  # ~56 verde lozano, ~42 pardo-oliva seco

    # 6. Regiones por pixel (lo ultimo que se escribe gana).
    region = np.full(shape, PLAINS, np.uint8)
    strong_snow = snow_s > 0.25
    d_snow = ndi.distance_transform_edt(~strong_snow)
    region[d_snow <= snow_belts_px[1] / f] = TAIGA
    region[d_snow <= snow_belts_px[0] / f] = SNOWY_TAIGA
    region[strong_snow] = SNOWY_PLAINS
    # Sabana: tierra seca (pardo) cerca del desierto o de las mesas.
    dry_zone = (des_s > 0.35) | (org_s > 0.35)
    d_dry = ndi.distance_transform_edt(~dry_zone)
    savanna = (d_dry <= savanna_belt_px / f) & (hue_s < 49)
    region[savanna & (region == PLAINS)] = SAVANNA
    region[des_s > 0.35] = DESERT_R
    region[org_s > 0.35] = BADLANDS
    # Colinas y montanas: la altura manda sobre el color; con nieve son picos nevados.
    region[relief >= 6.0] = HILLS
    region[(relief >= 7.0) & (snow_s > 0.15)] = PEAKS
    # Bosques (Elf Forest es la zona verde lozana del oeste; los bosques pequenos son elipses).
    for item in overrides.get("forests", []):
        if item["kind"] == "green_in_ellipse":
            m = ellipse_mask(shape, item["center"], item["radii"], f) & (hue_s >= 50) & land
        else:
            m = ellipse_mask(shape, item["center"], item["radii"], f) & land
        region[m & (relief < 8.0)] = FOREST
    # Volcan.
    vol = overrides.get("volcano")
    if vol:
        m = ellipse_mask(shape, vol["center"], vol["radii"], f) & land
        # Solo la parte oscura (la roca negra), no la hierba que hay alrededor.
        dark_area = ndi.gaussian_filter(((lum < 85) & land).astype(np.float32), 12) > 0.4
        region[m & dark_area] = VOLCANO
        relief[m & dark_area] = np.maximum(relief[m & dark_area], 7.0)
    # Sitios protegidos (p. ej. Bairan: debe quedar en llanura y sin pendiente fuerte).
    for item in overrides.get("protected", []):
        m = ellipse_mask(shape, item["center"], (item["radius_px"],) * 2, f) & land
        region[m] = REGION_IDS[item["region"]]
        relief[m] = np.minimum(relief[m], float(item["max_relief"]))
    region[water] = SEA
    return Analysis(sea, lake, relief, region, work)


# ------------------------------------------------------------------------------------------------------------
# Celdas
# ------------------------------------------------------------------------------------------------------------
def block_reduce(a: np.ndarray, k: int, func) -> np.ndarray:
    """Agrupa bloques k x k (rellenando los bordes que falten con el ultimo valor) aplicando func(bloques, axis)."""
    h, w = a.shape
    ph, pw = (-h) % k, (-w) % k
    if ph or pw:
        a = np.pad(a, ((0, ph), (0, pw)), mode="edge")
    return func(a.reshape(a.shape[0] // k, k, a.shape[1] // k, k))


def cell_mode(labels: np.ndarray, k: int, n: int, valid: np.ndarray | None = None) -> np.ndarray:
    """Etiqueta mas frecuente de cada bloque k x k (solo cuentan los pixeles `valid`)."""
    best = np.zeros((math.ceil(labels.shape[0] / k), math.ceil(labels.shape[1] / k)), np.int64)
    best_n = np.full(best.shape, -1, np.int64)
    for lab in range(n):
        m = (labels == lab)
        if valid is not None:
            m &= valid
        c = block_reduce(m.astype(np.int32), k, lambda x: x.sum(axis=(1, 3)))
        better = c > best_n
        best[better] = lab
        best_n[better] = c[better]
    return best.astype(np.uint8)


def to_cells(an: Analysis, cell_px: int, overrides: dict, shallow_blocks: float = 64.0, mid_blocks: float = 160.0,
             lake_relief: int = 2, coast_ramp: bool = True):
    """Reduce el analisis a celdas de cell_px pixeles. Devuelve (region, relief), arrays uint8 de (filas, columnas)."""
    if cell_px % WORK:
        raise ValueError(f"cell_px debe ser multiplo de {WORK}: {cell_px}")
    k = cell_px // WORK
    cell_blocks = cell_px * BLOCKS_PER_PIXEL
    land_px = ~(an.sea | an.lake)
    land_frac = block_reduce(land_px.astype(np.float32), k, lambda x: x.mean(axis=(1, 3)))
    sea_frac = block_reduce(an.sea.astype(np.float32), k, lambda x: x.mean(axis=(1, 3)))
    lake_frac = block_reduce(an.lake.astype(np.float32), k, lambda x: x.mean(axis=(1, 3)))
    is_land = land_frac >= 0.5
    is_lake = ~is_land & (lake_frac > sea_frac)
    is_sea = ~is_land & ~is_lake

    region = cell_mode(an.region, k, len(REGIONS), valid=land_px)
    region[~is_land] = SEA
    # Una celda de tierra sin ningun pixel de region (no deberia pasar) cuenta como llanura.
    region[is_land & (region == SEA)] = PLAINS

    # Relieve de tierra: media de los pixeles de tierra de la celda, redondeada.
    rel_sum = block_reduce(np.where(land_px, an.relief, 0.0).astype(np.float32), k, lambda x: x.sum(axis=(1, 3)))
    rel_cnt = block_reduce(land_px.astype(np.float32), k, lambda x: x.sum(axis=(1, 3)))
    land_relief = np.clip(np.rint(rel_sum / np.maximum(rel_cnt, 1)), 4, 9)

    # Distancia (en celdas) al agua para la costa; a la tierra para la plataforma marina.
    d_water = ndi.distance_transform_cdt(np.pad(is_land, 1, mode="constant", constant_values=False), metric="chessboard")[1:-1, 1:-1]
    relief = np.zeros(is_land.shape, np.float32)
    relief[is_land] = land_relief[is_land]
    if coast_ramp:
        for dist, cap in ((2, 5), (3, 7)):
            sel = is_land & (d_water == dist)
            relief[sel] = np.minimum(relief[sel], cap)
    relief[is_land & (d_water == 1)] = 3
    # Respeta el tope de los sitios protegidos tambien en la celda (la media ya lo hace, la rampa no sube nada).
    d_land = ndi.distance_transform_edt(~is_land) * cell_blocks
    sea_relief = np.where(d_land <= shallow_blocks, 2, np.where(d_land <= mid_blocks, 1, 0))
    relief[is_sea] = sea_relief[is_sea]
    relief[is_lake] = lake_relief
    return region.astype(np.uint8), relief.astype(np.uint8)


# ------------------------------------------------------------------------------------------------------------
# Salidas
# ------------------------------------------------------------------------------------------------------------
def region_palette() -> list[int]:
    pal = [0] * 768
    for idx, (_, _, colour) in REGIONS.items():
        pal[idx * 3:idx * 3 + 3] = colour
    return pal


def save_region_png(region: np.ndarray, path: Path) -> None:
    img = Image.fromarray(region, mode="P")
    img.putpalette(region_palette())  # 256 entradas: asi se guarda a 8 bits
    img.save(path, optimize=False, bits=8)


def save_relief_png(relief: np.ndarray, path: Path) -> None:
    Image.fromarray(relief, mode="L").save(path, optimize=True)


def build_meta(region: np.ndarray, cell_px: int, source_size, origin_px, source: str) -> dict:
    cell_blocks = cell_px * BLOCKS_PER_PIXEL
    return {
        "version": VERSION,
        "source": source,
        "blocks_per_pixel": BLOCKS_PER_PIXEL,
        "cell_px": cell_px,
        "cell_blocks": cell_blocks,
        "width": int(region.shape[1]),
        "height": int(region.shape[0]),
        "pixel_width": int(source_size[0]),
        "pixel_height": int(source_size[1]),
        "origin": {
            "px": [int(origin_px[0]), int(origin_px[1])],
            "col": int(origin_px[0] // cell_px),
            "row": int(origin_px[1] // cell_px),
            "name": "Bairan",
        },
        "region_legend": {str(i): REGIONS[i][0] for i in sorted(REGIONS)},
        "region_names": {str(i): REGIONS[i][1] for i in sorted(REGIONS)},
        "relief_legend": {str(i): m for i, m in RELIEF_MEANING.items()},
    }


def render_preview(region: np.ndarray, relief: np.ndarray, scale: int) -> Image.Image:
    """Region coloreada, sombreada por relieve, con el agua segun su profundidad."""
    pal = np.array(region_palette(), np.uint8).reshape(256, 3)[:len(REGIONS)]
    img = pal[region].astype(np.float32)
    land = relief >= 3
    shade = 0.65 + 0.35 * (relief.astype(np.float32) - 3) / 6.0
    img[land] *= shade[land][:, None]
    sea = ~land & (region == SEA)
    depth = relief.astype(np.float32)  # 0 profundo .. 2 somero
    img[sea] = np.array([20, 55, 110], np.float32) + depth[sea][:, None] * np.array([25, 40, 40], np.float32)
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
    return out.resize((out.width * scale, out.height * scale), Image.NEAREST)


def render_overlay(rgb_small: Image.Image, region: np.ndarray, relief: np.ndarray, scale: int, origin_cell) -> Image.Image:
    """El mapa original reducido con el borde de costa (rojo), la frontera de regiones (blanco fino) y Bairan."""
    size = (region.shape[1] * scale, region.shape[0] * scale)
    base = np.asarray(rgb_small.resize(size, Image.LANCZOS).convert("RGB")).copy()
    big_region = np.kron(region, np.ones((scale, scale), np.uint8))
    big_land = np.kron((relief >= 3).astype(np.uint8), np.ones((scale, scale), np.uint8)).astype(bool)
    coast = big_land & ~ndi.binary_erosion(big_land, iterations=1)
    edges = (big_region != np.roll(big_region, 1, 0)) | (big_region != np.roll(big_region, 1, 1))
    base[edges & big_land & ~coast] = (255, 255, 255)
    base[coast] = (255, 0, 0)
    cx, cy = (origin_cell[0] + 0.5) * scale, (origin_cell[1] + 0.5) * scale
    yy, xx = np.mgrid[0:size[1], 0:size[0]]
    base[(xx - cx) ** 2 + (yy - cy) ** 2 <= (scale * 1.2) ** 2] = (255, 0, 255)
    return Image.fromarray(base)


def convert(source: Path, out_dir: Path, cell_px: int = 16, overrides_path: Path = DEFAULT_OVERRIDES,
            preview_dir: Path | None = None, **kw) -> dict:
    overrides = json.loads(Path(overrides_path).read_text(encoding="utf-8"))
    with Image.open(source) as im:
        rgb = np.asarray(im.convert("RGB"))
        source_size = im.size
    an = analyse(rgb, overrides, **{k: v for k, v in kw.items() if k in ("river_px", "min_island_px", "min_lake_px", "snow_belts_px", "savanna_belt_px")})
    region, relief = to_cells(an, cell_px, overrides, **{k: v for k, v in kw.items() if k in ("shallow_blocks", "mid_blocks", "lake_relief", "coast_ramp")})
    origin_px = overrides["origin_px"]
    meta = build_meta(region, cell_px, source_size, origin_px, "canon/Mapa_continente_oeste.png")
    out_dir.mkdir(parents=True, exist_ok=True)
    save_region_png(region, out_dir / "region.png")
    save_relief_png(relief, out_dir / "relief.png")
    (out_dir / "meta.json").write_text(json.dumps(meta, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if preview_dir is not None:
        preview_dir.mkdir(parents=True, exist_ok=True)
        scale = max(1024 // region.shape[1], 1)
        tag = f"west_c{cell_px}"
        render_preview(region, relief, scale).save(preview_dir / f"{tag}_regiones.png", optimize=True)
        small = Image.fromarray(rgb).resize((region.shape[1] * 4, region.shape[0] * 4), Image.LANCZOS)
        render_overlay(small, region, relief, scale, (meta["origin"]["col"], meta["origin"]["row"])).save(
            preview_dir / f"{tag}_sobre_mapa.png", optimize=True)
    return meta


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--source", type=Path, default=Path("/home/user/overgeared-docs/canon/Mapa_continente_oeste.png"),
                   help="PNG del mapa del canon (4096x3072)")
    p.add_argument("--out", type=Path, default=HERE.parents[1] / "src/main/resources/data/overgearedmod/satisfy_map/west",
                   help="carpeta de salida (region.png, relief.png, meta.json)")
    p.add_argument("--preview", type=Path, default=HERE / "preview", help="carpeta de la vista previa (no es recurso del mod)")
    p.add_argument("--no-preview", action="store_true")
    p.add_argument("--cell-px", type=int, default=16, help="pixeles del mapa por celda (16 = 32 bloques; 8 = 16 bloques)")
    p.add_argument("--overrides", type=Path, default=DEFAULT_OVERRIDES)
    p.add_argument("--river-px", type=float, default=24.0, help="anchura (px) bajo la cual un rio o canal se vuelve tierra")
    p.add_argument("--shallow-blocks", type=float, default=64.0, help="hasta aqui del suelo, mar somero (relieve 2)")
    p.add_argument("--mid-blocks", type=float, default=160.0, help="hasta aqui, relieve 1; mas lejos, mar profundo (0)")
    args = p.parse_args(argv)
    meta = convert(args.source, args.out, args.cell_px, args.overrides, None if args.no_preview else args.preview,
                   river_px=args.river_px, shallow_blocks=args.shallow_blocks, mid_blocks=args.mid_blocks)
    print(f"{meta['width']}x{meta['height']} celdas de {meta['cell_blocks']} bloques -> {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
