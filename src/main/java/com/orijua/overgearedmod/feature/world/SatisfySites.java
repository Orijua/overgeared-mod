package com.orijua.overgearedmod.feature.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The fixed chunks of the one-per-world places of a {@link SatisfyMapData} map, read from its {@code sites.json}: Satisfy
 * is a world of the canon with a known map, so Bairan, Winston, the Northern End Cave and Earl Ashur's manor are at the
 * same chunks in every new world, whatever its seed ({@link StartingVillage#choose}). They were chosen once with the old
 * search over the map ({@link StartingVillage#search}) and checked to hold their structure in many seeds (the gametests
 * of {@code SatisfyFixedWorldTests}). Pure, so it is unit-tested.
 */
public final class SatisfySites {
    public static final String FILE = "sites.json";
    public static final int FORMAT_VERSION = 1;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    private final Map<String, ChunkPos> sites;

    public SatisfySites(Map<String, ChunkPos> sites) {
        for (String id : sites.keySet()) {
            if (!ID.matcher(id).matches()) {
                throw new IllegalArgumentException("a site id is lower case, digits and _: " + id);
            }
        }
        this.sites = Collections.unmodifiableMap(new LinkedHashMap<>(sites));
    }

    /** The sites of a map folder such as {@link SatisfyMapData#WEST}. */
    public static SatisfySites load(String folder) throws IOException {
        String path = (folder.endsWith("/") ? folder : folder + "/") + FILE;
        try (InputStream in = SatisfySites.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("missing resource " + path);
            }
            return read(in);
        }
    }

    private static volatile SatisfySites west;

    /** The West Continent's sites, read once; a missing or broken file is an error of the mod's jar. */
    public static SatisfySites west() {
        SatisfySites loaded = west;
        if (loaded == null) {
            try {
                loaded = load(SatisfyMapData.WEST);
            } catch (IOException e) {
                throw new UncheckedIOException("cannot read the satisfy_map sites", e);
            }
            west = loaded;
        }
        return loaded;
    }

    public static SatisfySites read(InputStream json) throws IOException {
        try {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(json, StandardCharsets.UTF_8)).getAsJsonObject();
            int version = root.get("version").getAsInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("unsupported sites version " + version + " (expected " + FORMAT_VERSION + ")");
            }
            Map<String, ChunkPos> sites = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("sites")) {
                JsonObject site = element.getAsJsonObject();
                JsonElement idElement = site.get("id");
                if (idElement == null || !idElement.isJsonPrimitive() || !idElement.getAsJsonPrimitive().isString()) {
                    throw new IOException("sites.json: every site needs an \"id\" string: " + site);
                }
                String id = idElement.getAsString();
                JsonArray chunk = site.getAsJsonArray("chunk");
                if (chunk == null || chunk.size() != 2 || !integer(chunk.get(0)) || !integer(chunk.get(1))) {
                    throw new IOException("sites.json: " + id + ".chunk must be [x, z] in whole chunks: " + site);
                }
                if (sites.put(id, new ChunkPos(chunk.get(0).getAsInt(), chunk.get(1).getAsInt())) != null) {
                    throw new IOException("sites.json: two sites are " + id);
                }
            }
            return new SatisfySites(sites);
        } catch (IllegalArgumentException e) {
            throw new IOException("sites.json is not valid: " + e.getMessage(), e);
        } catch (JsonParseException | NullPointerException | ClassCastException | IllegalStateException | IndexOutOfBoundsException | UnsupportedOperationException e) {
            throw new IOException("sites.json is missing or has a wrong field: " + e, e);
        }
    }

    private static boolean integer(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return false;
        }
        double number = value.getAsDouble();
        return number == Math.rint(number) && Math.abs(number) <= 1_875_000; // inside the world border, in chunks
    }

    /** Every site by id, in file order. */
    public Map<String, ChunkPos> all() {
        return sites;
    }

    public Optional<ChunkPos> get(String id) {
        return Optional.ofNullable(sites.get(id));
    }
}
