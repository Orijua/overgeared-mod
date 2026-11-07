package com.orijua.overgearedmod.feature.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The canon places of a {@link SatisfyMapData} map, read from its {@code places.json} (derived by hand from the docs'
 * {@code canon/Localizaciones.md}: only the clear, not the doubtful readings). Each place is a pixel of the source map,
 * so its block is fixed by the same origin and scale as the terrain. They are only coordinates: no structure is built
 * for them; {@code /satisfy debug goto} uses them. Pure, so it is unit-tested.
 */
public final class SatisfyPlaces {
    public static final String FILE = "places.json";
    public static final int FORMAT_VERSION = 1;
    /** How many cells around a place a landing may be looked for (labels sit beside their icon, not on it). */
    public static final int SEARCH_CELLS = 4;
    /** The relief of firm land: above the coast. */
    public static final int FIRM_RELIEF = SatisfyMapData.COAST_RELIEF + 1;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");
    private static final Set<String> CERTAINTIES = Set.of("alta", "media", "mapa");

    /** A place: its pixel on the source map, what it is and how sure the canon is of it ({@code alta}, {@code media}, {@code mapa}). */
    public record Place(String id, String name, String type, int pixelX, int pixelZ, String certainty, boolean land) {
    }

    private final Map<String, Place> places;

    public SatisfyPlaces(List<Place> places) {
        Map<String, Place> byId = new LinkedHashMap<>();
        for (Place place : places) {
            if (!ID.matcher(place.id()).matches()) {
                throw new IllegalArgumentException("a place id is lower case, digits and _: " + place.id());
            }
            if (!CERTAINTIES.contains(place.certainty())) {
                throw new IllegalArgumentException(place.id() + ": unknown certainty " + place.certainty());
            }
            if (byId.put(place.id(), place) != null) {
                throw new IllegalArgumentException("two places are " + place.id());
            }
        }
        this.places = Collections.unmodifiableMap(byId);
    }

    /** The places of a map folder such as {@link SatisfyMapData#WEST}. */
    public static SatisfyPlaces load(String folder) throws IOException {
        String path = (folder.endsWith("/") ? folder : folder + "/") + FILE;
        try (InputStream in = SatisfyPlaces.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("missing resource " + path);
            }
            return read(in);
        }
    }

    private static volatile SatisfyPlaces west;

    /** The West Continent's places, read once; a missing or broken file is an error of the mod's jar. */
    public static SatisfyPlaces west() {
        SatisfyPlaces loaded = west;
        if (loaded == null) {
            try {
                loaded = load(SatisfyMapData.WEST);
            } catch (IOException e) {
                throw new UncheckedIOException("cannot read the satisfy_map places", e);
            }
            west = loaded;
        }
        return loaded;
    }

    public static SatisfyPlaces read(InputStream json) throws IOException {
        try {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(json, StandardCharsets.UTF_8)).getAsJsonObject();
            int version = root.get("version").getAsInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("unsupported places version " + version + " (expected " + FORMAT_VERSION + ")");
            }
            List<Place> places = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("places")) {
                JsonObject place = element.getAsJsonObject();
                String id = string(place, "id", "?");
                JsonArray px = place.getAsJsonArray("px");
                if (px == null || px.size() != 2) {
                    throw new IOException("places.json: " + id + ".px must be [x, y]: " + place);
                }
                places.add(new Place(id, string(place, "name", id), string(place, "type", id),
                        pixel(px.get(0), id), pixel(px.get(1), id), string(place, "certainty", id), bool(place, "land", id)));
            }
            return new SatisfyPlaces(places);
        } catch (IllegalArgumentException e) {
            throw new IOException("places.json is not valid: " + e.getMessage(), e);
        } catch (NullPointerException | ClassCastException | IllegalStateException | IndexOutOfBoundsException | UnsupportedOperationException e) {
            throw new IOException("places.json is missing or has a wrong field: " + e, e);
        }
    }

    private static String string(JsonObject place, String field, String id) throws IOException {
        JsonElement value = place.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IOException("places.json: " + id + "." + field + " must be a string, not " + value);
        }
        return value.getAsString();
    }

    private static boolean bool(JsonObject place, String field, String id) throws IOException {
        JsonElement value = place.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IOException("places.json: " + id + "." + field + " must be true or false, not " + value);
        }
        return value.getAsBoolean();
    }

    private static int pixel(JsonElement value, String id) throws IOException {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IOException("places.json: " + id + ".px must hold integers >= 0, not " + value);
        }
        double number = value.getAsDouble();
        if (number < 0 || number != Math.rint(number) || number > Integer.MAX_VALUE) {
            throw new IOException("places.json: " + id + ".px must hold integers >= 0, not " + value);
        }
        return (int) number;
    }

    /** In file order. */
    public List<Place> all() {
        return List.copyOf(places.values());
    }

    public List<String> ids() {
        return List.copyOf(places.keySet());
    }

    public Optional<Place> get(String id) {
        return Optional.ofNullable(places.get(id));
    }

    // --- On the map ------------------------------------------------------------------------------------------

    /** Whether the place's pixel is on the map's grid. */
    public static boolean onMap(SatisfyMapData map, Place place) {
        return map.inGrid(Math.floorDiv(place.pixelX(), map.cellPx()), Math.floorDiv(place.pixelZ(), map.cellPx()));
    }

    /** The block (x, z) of the place's pixel (its north-west corner). */
    public static int[] block(SatisfyMapData map, Place place) {
        return new int[]{map.blockXOfPixel(place.pixelX()), map.blockZOfPixel(place.pixelZ())};
    }

    /** A pixel of the source map as chunks from Bairan (Bairan's chunk is the map's origin). */
    public static ChunkPos chunkOffset(SatisfyMapData map, int pixelX, int pixelZ) {
        return new ChunkPos(map.blockXOfPixel(pixelX) >> 4, map.blockZOfPixel(pixelZ) >> 4);
    }

    /** A place as chunks from Bairan. */
    public static ChunkPos chunkOffset(SatisfyMapData map, Place place) {
        return chunkOffset(map, place.pixelX(), place.pixelZ());
    }

    /**
     * Where to land for a place, as the block (x, z) at the middle of a land cell: the nearest cell with firm land within
     * {@link #SEARCH_CELLS} of the place, or else the nearest coast cell (small islands are all coast). Empty for a sea
     * place, or one with no land near it.
     */
    public static Optional<int[]> landing(SatisfyMapData map, Place place) {
        if (!place.land()) {
            return Optional.empty();
        }
        int col = Math.floorDiv(place.pixelX(), map.cellPx());
        int row = Math.floorDiv(place.pixelZ(), map.cellPx());
        Optional<int[]> cell = nearest(map, col, row, FIRM_RELIEF).or(() -> nearest(map, col, row, SatisfyMapData.COAST_RELIEF));
        return cell.map(found -> {
            int half = map.cellPx() / 2;
            return new int[]{map.blockXOfPixel(found[0] * map.cellPx() + half), map.blockZOfPixel(found[1] * map.cellPx() + half)};
        });
    }

    /** The nearest cell (col, row) to (col, row) with at least {@code relief}, ties to the first found north-west. */
    private static Optional<int[]> nearest(SatisfyMapData map, int col, int row, int relief) {
        int[] best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dr = -SEARCH_CELLS; dr <= SEARCH_CELLS; dr++) {
            for (int dc = -SEARCH_CELLS; dc <= SEARCH_CELLS; dc++) {
                int distance = dc * dc + dr * dr;
                if (distance < bestDistance && map.reliefAtCell(col + dc, row + dr) >= relief) {
                    best = new int[]{col + dc, row + dr};
                    bestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
