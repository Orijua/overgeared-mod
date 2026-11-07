# satisfy_map: conversor del mapa del canon a datos de terreno

Convierte `canon/Mapa_continente_oeste.png` (repo de docs, 4096x3072, **2 bloques por pixel**) en datos por celda para el
terreno de la dimension Satisfy. Vive fuera del mod empaquetado: solo los resultados van en `src/main/resources`.

## Regenerar

```bash
pip install pillow numpy scipy pytest          # una vez
cd tools/satisfy_map
python3 convert.py                              # celdas de 16 px = 32 bloques (256x192 celdas), fase de menor nitidez
python3 convert.py --cell-px 8                  # celdas de 8 px = 16 bloques (512x384 celdas), mas detalle
python3 convert.py --source RUTA/Mapa.png --out OTRA_CARPETA --no-preview
python3 -m pytest -q                            # o: python3 -m unittest
```

Tarda unos 10 s. El PNG por defecto es `/home/user/overgeared-docs/canon/Mapa_continente_oeste.png`; si el repo de docs
esta en otra ruta, usa `--source`.

### Tests

`python3 -m pytest -q` en esta carpeta. Los datos comiteados (`satisfy_map/west`) son obligatorios: si faltan, los tests
fallan. La prueba de regeneracion necesita el PNG del canon: lo busca en la variable de entorno `SATISFY_MAP_SOURCE` o, si
no existe, en `../../../overgeared-docs/canon/Mapa_continente_oeste.png` (repo de docs hermano); si no lo encuentra, solo esa
prueba se salta, con un mensaje que lo dice.

Los datos comiteados se regeneran con bytes identicos con numpy 2.5.3, scipy 1.18.1 y Pillow 12.3.0. Con otras versiones la
prueba avisa y acepta hasta un 0,5 % de celdas distintas (redondeos de `gaussian_filter`, etiquetado). Si cambias de versiones,
regenera, ejecuta los tests de Java y de Python y actualiza `REFERENCE_VERSIONS` en `test_convert.py` y esta nota.
Los tests de Java y de Python valen con cualquier `--cell-px`; tras regenerar, ejecuta ambos.

## Salidas

En `src/main/resources/data/overgearedmod/satisfy_map/west/`:

| Archivo | Contenido |
|---|---|
| `region.png` | PNG indexado de 8 bits. El indice es la region: `0` mar (y lagos), `1` llanura, `2` bosque, `3` desierto, `4` taiga, `5` llanura nevada, `6` taiga nevada, `7` colinas ventosas, `8` sabana, `9` mesas/badlands, `10` volcan, `11` picos nevados. Los 1-8 siguen la leyenda de `SatisfyMap`. |
| `relief.png` | Gris de 8 bits con valores 0-9 (se ve casi negro: es normal). Misma semantica que `SatisfyTerrain`: 0 mar profundo, 1-2 mar somero, 3 costa, 4-6 llanura/colinas, 7-9 montana. |
| `meta.json` | `version`, `blocks_per_pixel`, `cell_px`, `cell_blocks`, `width`/`height` (en celdas), `pixel_width`/`pixel_height` (del PNG), `origin` (pixel exacto de Bairan y su celda), `region_legend` (indice a bioma), `region_names`, `relief_legend`. |

La vista previa (no es recurso del mod) va en `tools/satisfy_map/preview/`: `west_cNN_regiones.png` (regiones coloreadas,
sombreadas por relieve; el mar se aclara hacia la costa) y `west_cNN_sobre_mapa.png` (el mapa original con la costa en rojo,
las fronteras de region en blanco y Bairan en magenta) para comparar a ojo.

El origen (bloque 0, 0) es el **pixel de Bairan** (1772, 458), el icono de casas de Bairan junto a Winston, no el centro de
una celda: asi el mapeo bloque-pixel es el mismo con cualquier tamano de celda. Lector en Java: `SatisfyMapData`.

## Como clasifica (por color)

Trabaja a mitad de resolucion (1 pixel de trabajo = 2x2 del mapa). Paleta real del mapa (histograma + k-means):

- **Mar**: azul grisaceo, de (79,110,116) a (153,180,183) (el halo claro de la costa); siempre `B > R` y `G > R`.
- **Tierra**: oliva (113,107,19) y pardo-oliva (133,110,45). El matiz separa dos zonas: ~56 grados (verde lozano, oeste y sur)
  y ~42 (pardo seco, norte, centro y este).
- **Desierto** (234,200,125); **mesas** naranja (187,124,48); **roca** parda (74,66,57)..(163,142,112); **nieve** blanca (225,227,223)
  o azul (117,195,227); texto e iconos casi negros; castillos azul oscuro.

Pasos: (1) se descartan rectangulos fijos (titulo, brujula, barcos, llama de la Grieta, remolino de Seiren) y se tratan como
mar; (2) el agua se separa de la tierra por color y se limpian lineas finas (texto, rutas de barco, espuma de la costa) con
una apertura morfologica y se quitan las islas diminutas; (3) los rios mas estrechos que `--river-px` (24 px = 48 bloques) se
vuelven tierra (rios y canales navegables: para despues); el agua ancha conectada al borde es mar y la que no, lago (region 0,
relieve 2); (4) iconos y texto sobre tierra se rellenan con la clase vecina mas cercana, asi no quedan agujeros; (5) el relieve
sale de la densidad de pixeles oscuros (los iconos de montana son mucho mas oscuros que el suelo; sirve tambien para las
montanas verdes); (6) las regiones salen del color suavizado y de la distancia a la nieve y al desierto; (7) cada celda toma la
region mas frecuente y el relieve medio. Costa: celda de tierra junto al agua = 3; rampa de 5 y 7 en las dos siguientes. Mar: 2
hasta `--shallow-blocks` (64) de la tierra, 1 hasta `--mid-blocks` (160), 0 mas lejos.

## Ajustes manuales (`overrides.json`)

Lo que el color no da y se pone a mano (coordenadas en pixeles del PNG): `origin_px` (Bairan), rectangulos a ignorar,
bosques (Elf Forest es la zona verde lozana dentro de una elipse; Mysterious y Crime Forest, elipses pequenas), el volcan Veldon
(elipse mas la roca oscura) y zonas protegidas (Bairan: llanura, relieve maximo 5, radio 90 px, porque el mapa la dibuja
pegada a la nieve de Frostlight).

## Limites conocidos

- Las islas del archipielago de Behen son de 1-3 celdas con celdas de 16 px; con `--cell-px 8` se ven bien.
- Los rios y canales (Canal Sahariano, Herbin) no existen aun: son tierra. Los lagos grandes (Cogro, Limit, Fullbaz) si.
- La sabana (franja alrededor del desierto y de las mesas) y las fronteras circulares del Elf Forest y de la sabana son una
  aproximacion; los biomas fieles a la novela se afinan en C3.
