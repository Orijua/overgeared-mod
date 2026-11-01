package com.orijua.overgearedmod.datagen;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.world.StructurePieces;
import com.orijua.overgearedmod.feature.world.TemplateBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Structure pieces built from code (structure/): writes each one as data/overgearedmod/structure/&lt;path&gt;.nbt, the
 * same file a structure block saves. The structures, pools and sets that use them stay hand-written JSON.
 */
final class ModStructurePieceProvider implements DataProvider {
    private final PackOutput.PathProvider templates;

    ModStructurePieceProvider(PackOutput output) {
        this.templates = output.createPathProvider(PackOutput.Target.DATA_PACK, "structure");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        int dataVersion = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        return CompletableFuture.allOf(StructurePieces.all().entrySet().stream()
                .map(piece -> CompletableFuture.runAsync(() -> write(output, piece, dataVersion)))
                .toArray(CompletableFuture[]::new));
    }

    @SuppressWarnings("deprecation") // sha1 is what CachedOutput expects, as in vanilla's SnbtToNbt
    private void write(CachedOutput output, Map.Entry<String, TemplateBuilder> piece, int dataVersion) {
        Path path = templates.file(Overgearedmod.id(piece.getKey()), "nbt");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(piece.getValue().build(dataVersion), bytes);
            byte[] payload = bytes.toByteArray();
            HashCode hash = Hashing.sha1().hashBytes(payload);
            output.writeIfNeeded(path, payload, hash);
        } catch (IOException e) {
            throw new UncheckedIOException("couldn't write structure piece " + path, e);
        }
    }

    @Override
    public String getName() {
        return "Structure pieces";
    }
}
