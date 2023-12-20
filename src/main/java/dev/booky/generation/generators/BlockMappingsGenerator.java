package dev.booky.generation.generators;
// Created by booky10 in MinecraftSource (19:02 05.09.23)

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class BlockMappingsGenerator implements IGenerator {

    @SuppressWarnings({"rawtypes", "unchecked"}) // I don't care
    private static void stringifyStateValues(Writer writer, Map<Property<?>, Comparable<?>> values) throws IOException{
        if (values.isEmpty()) {
            return; // no values present, nothing to stringify
        }

        writer.write('[');
        boolean first = true;
        for (Map.Entry<Property<?>, Comparable<?>> entry : values.entrySet()) {
            if (first) {
                first = false;
            } else {
                writer.write(',');
            }

            Property<?> property = entry.getKey();
            writer.write(property.getName());
            writer.write('=');

            String valueName = ((Property) property).getName(entry.getValue());
            writer.write(valueName);
        }
        writer.write(']');
    }

    @Override
    public void generate(Path outDir, String genName) throws IOException {
        Path outPath = outDir.resolve(genName + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(outPath)) {
            writer.write(SharedConstants.getCurrentVersion().getId());

            for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
                writer.newLine();
                if (state.getBlock().defaultBlockState() == state) {
                    writer.append('*'); // default state
                }

                ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                writer.append(blockKey.getPath());
                stringifyStateValues(writer, state.getValues());
            }
        }
    }
}
