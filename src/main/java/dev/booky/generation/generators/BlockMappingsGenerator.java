package dev.booky.generation.generators;
// Created by booky10 in MinecraftSource (19:02 05.09.23)

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.nio.file.Path;
import java.util.stream.Collectors;

public final class BlockMappingsGenerator implements IGenerator {

    @Override
    public void generate(Path outPath) {
        StringBuilder builder = new StringBuilder();
        builder.append(SharedConstants.getCurrentVersion().getId());

        for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
            builder.append('\n');
            if (state.getBlock().defaultBlockState() == state) {
                builder.append('*'); // default state
            }

            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            builder.append(blockKey.getPath());

            if (!state.getValues().isEmpty()) {
                builder.append('[');
                builder.append(state.getValues().entrySet().stream()
                        .map(entry -> {
                            Property<?> property = entry.getKey();
                            String valueName = ((Property) property).getName(entry.getValue());
                            return property.getName() + "=" + valueName;
                        })
                        .collect(Collectors.joining(",")));
                builder.append(']');
            }
        }

        System.out.println(builder);
    }
}
