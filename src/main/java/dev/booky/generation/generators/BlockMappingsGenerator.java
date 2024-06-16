package dev.booky.generation.generators;
// Created by booky10 in MinecraftSource (19:02 05.09.23)

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.booky.generation.util.GenerationUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class BlockMappingsGenerator implements IGenerator {

    @Override
    public void generate(Path outDir, String genName) throws IOException {
        JsonArray array = new JsonArray();
        for (Block block : BuiltInRegistries.BLOCK) {
            List<BlockState> states = block.getStateDefinition().getPossibleStates();
            int defaultIndex = states.indexOf(block.defaultBlockState());

            JsonArray entries = new JsonArray();
            for (BlockState state : states) {
                JsonObject object = new JsonObject();
                for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
                    String valueStr = ((Property) entry.getKey()).getName(entry.getValue());
                    JsonPrimitive value = NumberUtils.isDigits(valueStr)
                            ? new JsonPrimitive(Integer.parseInt(valueStr))
                            : "true".equals(valueStr) ? new JsonPrimitive(true)
                            : "false".equals(valueStr) ? new JsonPrimitive(false)
                            : new JsonPrimitive(valueStr);
                    object.add(entry.getKey().getName(), value);
                }
                entries.add(object);
            }

            JsonObject object = new JsonObject();
            object.addProperty("type", BuiltInRegistries.BLOCK.getKey(block).getPath());
            object.add("entries", entries);
            object.addProperty("def", defaultIndex);
            array.add(object);
        }

        GenerationUtil.saveJsonElement(array, outDir.resolve(genName + ".json"));
    }
}
