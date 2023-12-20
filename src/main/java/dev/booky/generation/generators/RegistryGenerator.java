package dev.booky.generation.generators;
// Created by booky10 in MinecraftSource (19:02 05.09.23)

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

public final class RegistryGenerator implements IGenerator {

    private static <T> JsonObject createRegistryDumpObj(Registry<T> registry) {
        JsonObject obj = new JsonObject();
        for (T element : registry) {
            ResourceLocation elementKey = registry.getKey(element);
            if (elementKey == null) {
                throw new IllegalStateException("Illegal element " + element + " in " + registry + " found");
            }

            String elementName = elementKey.getPath();
            int elementId = registry.getId(element);
            obj.addProperty(elementName, elementId);
        }
        return obj;
    }

    private static <T> JsonArray createRegistryDumpArr(Registry<T> registry) {
        JsonArray arr = new JsonArray();
        for (T element : registry) {
            ResourceLocation elementKey = registry.getKey(element);
            if (elementKey == null) {
                throw new IllegalStateException("Illegal element " + element + " in " + registry + " found");
            }
            arr.add(elementKey.getPath());
        }
        return arr;
    }

    @Override
    public void generate(Path outPath) {
        System.out.println(createRegistryDumpObj(BuiltInRegistries.ENTITY_TYPE));
        System.out.println(createRegistryDumpObj(BuiltInRegistries.ENCHANTMENT));
        System.out.println(createRegistryDumpObj(BuiltInRegistries.ITEM));
        System.out.println(createRegistryDumpArr(BuiltInRegistries.PARTICLE_TYPE));
        System.out.println(createRegistryDumpArr(BuiltInRegistries.ATTRIBUTE));
    }
}
