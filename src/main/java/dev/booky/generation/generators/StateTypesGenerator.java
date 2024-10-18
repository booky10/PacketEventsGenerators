package dev.booky.generation.generators;
// Created by booky10 in PacketEventsUtils (22:44 20.12.23)

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import dev.booky.generation.util.GenerationUtil;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class StateTypesGenerator implements IGenerator {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("deprecation") // solid state is deprecated
    @Override
    public void generate(Path outDir, String genName) throws IOException {
        Path genDir = outDir.resolve(genName);
        Files.createDirectories(genDir);

        Path inputPath = genDir.resolve("input.json");
        Path outputPath = genDir.resolve("output.txt");
        if (!Files.exists(inputPath)) {
            LOGGER.warn("Skipping generator, input path {} doesn't exist", inputPath);
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // read inputs
            Set<ResourceLocation> prevBlocks = GenerationUtil.loadJsonElement(inputPath, JsonArray.class)
                    .asList().stream()
                    .map(JsonElement::getAsString).map(ResourceLocation::parse)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<ResourceLocation> blocks = BuiltInRegistries.BLOCK.stream()
                    .map(BuiltInRegistries.BLOCK::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // determine which blocks have been removed compared to input
            Set<ResourceLocation> removedBlocks = new LinkedHashSet<>(prevBlocks);
            removedBlocks.removeAll(blocks);
            writer.write("// Removed blocks (");
            writer.write(SharedConstants.getCurrentVersion().getName());
            writer.write("): ");
            writer.write(removedBlocks.toString());
            writer.newLine();

            // determine which blocks have been added compared to input
            Set<ResourceLocation> addedBlocks = new LinkedHashSet<>(blocks);
            addedBlocks.removeAll(prevBlocks);
            writer.write("// Added blocks (");
            writer.write(SharedConstants.getCurrentVersion().getName());
            writer.write("): ");
            writer.write(addedBlocks.toString());
            writer.newLine();

            // generate code for each added block
            for (ResourceLocation addedBlock : addedBlocks) {
                writer.newLine();
                writer.write("public static StateType ");
                writer.write(GenerationUtil.asFieldName(addedBlock));
                writer.write(" = StateTypes.builder().name(\"");
                writer.write(GenerationUtil.toString(addedBlock));
                writer.write("\")");

                Block block = BuiltInRegistries.BLOCK.get(addedBlock).orElseThrow().value();
                writer.write(".blastResistance(" + block.getExplosionResistance() + "f)");
                writer.write(".hardness(" + block.defaultDestroyTime() + "f)");
                writer.write(".isBlocking(" + block.properties().hasCollision + ")");
                writer.write(".requiresCorrectTool(" + block.properties().requiresCorrectToolForDrops + ")");
                writer.write(".isSolid(" + block.defaultBlockState().isSolid() + ")");
                // bob.append(".setMaterial(").append().append(")"); // TODO how?

                writer.write(".build();");
            }
        }
    }
}
