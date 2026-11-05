package com.orijua.overgearedmod.feature.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The terrain data of a canon map, converted from the PNG by {@code tools/satisfy_map} and stored as classpath resources:
 * {@code region.png} (8-bit indexed: the index is the region, {@code 0} = sea), {@code relief.png} (gray {@code 0-9}, the
 * same meaning as {@link SatisfyTerrain}: {@code 0} deep sea, {@code 3} coast, {@code 4-6} lowland and hills, {@code 7-9}
 * mountains) and {@code meta.json} (scale, cell size, origin and legend).
 * <p>
 * The grid is north up and east right, like the source map. The origin is a source pixel (Bairan), not a cell centre:
 * block (0, 0) lies on that pixel, so every block maps to the same place of the canon map whatever the cell size.
 * Beyond the grid it is sea. This class only reads and queries the data (pure, so it is unit-tested): the biomes of the
 * Satisfy dimension ({@link SatisfyMap}, {@link SatisfyBiomeSource}) and its terrain ({@link SatisfyTerrainFunction}) come
 * from it.
 */
public final class SatisfyMapData {
    /** The data of the West Continent, as a classpath folder. */
    public static final String WEST = "/data/overgearedmod/satisfy_map/west/";
    public static final int FORMAT_VERSION = 1;
    /** Region index of the sea (and of lakes: they are told apart by their relief, which is 2). */
    public static final int SEA = 0;
    /** Region index of the plains (llanura), where Bairan is. */
    public static final int PLAINS = 1;
    public static final int MAX_RELIEF = 9;
    /** The relief of a land cell next to the water; everything below is water. */
    public static final int COAST_RELIEF = 3;

    private final int width;
    private final int height;
    private final int blocksPerPixel;
    private final int cellPx;
    private final int originPixelX;
    private final int originPixelZ;
    private final byte[] relief;
    private final byte[] region;
    private final Map<Integer, String> legend;

    /** Maps already loaded, by folder: the data never changes while the game runs. */
    private static final Map<String, SatisfyMapData> LOADED = new ConcurrentHashMap<>();

    /** Builds the data from already-parsed arrays (row-major, north row first). */
    public SatisfyMapData(int width, int height, int blocksPerPixel, int cellPx, int originPixelX, int originPixelZ,
                          byte[] relief, byte[] region, Map<Integer, String> legend) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("the map has no cells: " + width + "x" + height);
        }
        if (blocksPerPixel <= 0 || cellPx <= 0) {
            throw new IllegalArgumentException("blocksPerPixel and cellPx must be positive: " + blocksPerPixel + ", " + cellPx);
        }
        if (relief.length != width * height || region.length != width * height) {
            throw new IllegalArgumentException("the data does not match " + width + "x" + height + " cells");
        }
        int originCol = Math.floorDiv(originPixelX, cellPx);
        int originRow = Math.floorDiv(originPixelZ, cellPx);
        if (originCol < 0 || originCol >= width || originRow < 0 || originRow >= height) {
            throw new IllegalArgumentException("the origin pixel (" + originPixelX + ", " + originPixelZ + ") is off the map");
        }
        for (int i = 0; i < relief.length; i++) {
            if (relief[i] < 0 || relief[i] > MAX_RELIEF) {
                throw new IllegalArgumentException("relief is 0-" + MAX_RELIEF + " but cell " + i + " has " + relief[i]);
            }
            if (!legend.containsKey(region[i] & 0xFF)) {
                throw new IllegalArgumentException("region " + (region[i] & 0xFF) + " of cell " + i + " is not in the legend");
            }
        }
        this.width = width;
        this.height = height;
        this.blocksPerPixel = blocksPerPixel;
        this.cellPx = cellPx;
        this.originPixelX = originPixelX;
        this.originPixelZ = originPixelZ;
        this.relief = relief.clone();
        this.region = region.clone();
        this.legend = Map.copyOf(new TreeMap<>(legend));
    }

    /** Loads a map from a classpath folder such as {@link #WEST}. */
    public static SatisfyMapData load(String folder) throws IOException {
        String base = folder.endsWith("/") ? folder : folder + "/";
        try (InputStream meta = open(base + "meta.json");
             InputStream region = open(base + "region.png");
             InputStream relief = open(base + "relief.png")) {
            return read(meta, region, relief);
        }
    }

    /** Like {@link #load}, but each folder is read only once; a missing or broken map is an error of the mod's jar. */
    public static SatisfyMapData cached(String folder) {
        return LOADED.computeIfAbsent(folder, key -> {
            try {
                return load(key);
            } catch (IOException e) {
                throw new UncheckedIOException("cannot read the satisfy_map " + key, e);
            }
        });
    }

    /** The West Continent's data, read once. */
    public static SatisfyMapData west() {
        return cached(WEST);
    }

    private static InputStream open(String path) throws IOException {
        InputStream in = SatisfyMapData.class.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("missing resource " + path);
        }
        return in;
    }

    /** Parses the three files. Fails on a wrong version, size, relief or region index. */
    public static SatisfyMapData read(InputStream metaJson, InputStream regionPng, InputStream reliefPng) throws IOException {
        JsonObject meta;
        try {
            JsonElement root = JsonParser.parseReader(new InputStreamReader(metaJson, StandardCharsets.UTF_8));
            meta = root.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("meta.json is not valid: " + e.getMessage(), e);
        }
        try {
            int version = meta.get("version").getAsInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("unsupported satisfy_map version " + version + " (expected " + FORMAT_VERSION + ")");
            }
            int width = meta.get("width").getAsInt();
            int height = meta.get("height").getAsInt();
            int blocksPerPixel = meta.get("blocks_per_pixel").getAsInt();
            int cellPx = meta.get("cell_px").getAsInt();
            JsonObject origin = meta.getAsJsonObject("origin");
            int originX = origin.getAsJsonArray("px").get(0).getAsInt();
            int originZ = origin.getAsJsonArray("px").get(1).getAsInt();
            Map<Integer, String> legend = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : meta.getAsJsonObject("region_legend").entrySet()) {
                legend.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
            }
            byte[] regions = samples(regionPng, width, height, "region.png");
            byte[] reliefs = samples(reliefPng, width, height, "relief.png");
            try {
                return new SatisfyMapData(width, height, blocksPerPixel, cellPx, originX, originZ, reliefs, regions, legend);
            } catch (IllegalArgumentException e) {
                throw new IOException("inconsistent satisfy_map data: " + e.getMessage(), e);
            }
        } catch (NullPointerException | ClassCastException | IllegalStateException | IndexOutOfBoundsException | NumberFormatException e) {
            throw new IOException("meta.json is missing or has a wrong field: " + e, e);
        }
    }

    /** The first band of a single-band PNG: the palette index of an indexed image, the gray level of a gray one. */
    private static byte[] samples(InputStream png, int width, int height, String name) throws IOException {
        BufferedImage image = ImageIO.read(png);
        if (image == null) {
            throw new IOException(name + " is not a readable image");
        }
        if (image.getWidth() != width || image.getHeight() != height) {
            throw new IOException(name + " is " + image.getWidth() + "x" + image.getHeight() + " but meta.json says " + width + "x" + height);
        }
        if (image.getRaster().getNumBands() != 1) {
            throw new IOException(name + " must have a single band (indexed or gray)");
        }
        byte[] out = new byte[width * height];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                out[row * width + col] = (byte) image.getRaster().getSample(col, row, 0);
            }
        }
        return out;
    }

    // --- Cells ------------------------------------------------------------------------------------------------

    /** Columns of the grid. */
    public int width() {
        return width;
    }

    /** Rows of the grid. */
    public int height() {
        return height;
    }

    /** Blocks per cell side. */
    public int cellBlocks() {
        return cellPx * blocksPerPixel;
    }

    public int blocksPerPixel() {
        return blocksPerPixel;
    }

    public int cellPx() {
        return cellPx;
    }

    /** The column of the cell that holds the origin (Bairan). */
    public int originCol() {
        return Math.floorDiv(originPixelX, cellPx);
    }

    /** The row of the cell that holds the origin (Bairan). */
    public int originRow() {
        return Math.floorDiv(originPixelZ, cellPx);
    }

    public boolean inGrid(int col, int row) {
        return col >= 0 && col < width && row >= 0 && row < height;
    }

    /** The relief of a cell, {@code 0-9}; {@code 0} (deep sea) off the grid. */
    public int reliefAtCell(int col, int row) {
        return inGrid(col, row) ? relief[row * width + col] : 0;
    }

    /** The region index of a cell; {@link #SEA} off the grid. */
    public int regionAtCell(int col, int row) {
        return inGrid(col, row) ? region[row * width + col] & 0xFF : SEA;
    }

    /** The legend entry (biome id, or {@code "sea"}) of a region index, or {@code null} if it is not in the legend. */
    public String legend(int regionIndex) {
        return legend.get(regionIndex);
    }

    /** The whole legend, region index to biome id. */
    public Map<Integer, String> legend() {
        return legend;
    }

    /** Whether the cell is land, that is, it has the coast relief or more. */
    public boolean isLandCell(int col, int row) {
        return reliefAtCell(col, row) >= COAST_RELIEF;
    }

    // --- Blocks -----------------------------------------------------------------------------------------------

    /** The column of the cell that holds block x (block 0 is on the origin pixel). */
    public int colAtBlock(int blockX) {
        return Math.floorDiv(originPixelX * blocksPerPixel + blockX, cellBlocks());
    }

    /** The row of the cell that holds block z. */
    public int rowAtBlock(int blockZ) {
        return Math.floorDiv(originPixelZ * blocksPerPixel + blockZ, cellBlocks());
    }

    /** The block x of the west edge of a source pixel's column (block 0 is the origin pixel). */
    public int blockXOfPixel(int pixelX) {
        return (pixelX - originPixelX) * blocksPerPixel;
    }

    /** The block z of the north edge of a source pixel's row. */
    public int blockZOfPixel(int pixelZ) {
        return (pixelZ - originPixelZ) * blocksPerPixel;
    }

    /** The region index at a block (no interpolation: the cell it falls in). */
    public int regionAtBlock(int blockX, int blockZ) {
        return regionAtCell(colAtBlock(blockX), rowAtBlock(blockZ));
    }

    /** The legend entry at a block; {@code "sea"} beyond the grid. */
    public String biomeAtBlock(int blockX, int blockZ) {
        return legend.get(regionAtBlock(blockX, blockZ));
    }

    /**
     * The relief at a block, {@code 0-9}, interpolated bilinearly between cell centres (like {@link SatisfyTerrain}) so the
     * land has no steps; beyond the grid it is {@code 0} (sea).
     */
    public double relief(double blockX, double blockZ) {
        double u = (originPixelX * (double) blocksPerPixel + blockX) / cellBlocks() - 0.5;
        double v = (originPixelZ * (double) blocksPerPixel + blockZ) / cellBlocks() - 0.5;
        int u0 = (int) Math.floor(u);
        int v0 = (int) Math.floor(v);
        double fu = u - u0;
        double fv = v - v0;
        double top = lerp(reliefAtCell(u0, v0), reliefAtCell(u0 + 1, v0), fu);
        double bottom = lerp(reliefAtCell(u0, v0 + 1), reliefAtCell(u0 + 1, v0 + 1), fu);
        return lerp(top, bottom, fv);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
