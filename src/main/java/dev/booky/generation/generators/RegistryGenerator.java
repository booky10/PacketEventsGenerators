package dev.booky.generation.generators;
// Created by booky10 in MinecraftSource (19:02 05.09.23)

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.booky.generation.util.GenerationUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RegistryGenerator implements IGenerator {

    static <T> JsonObject generateJsonObject(Registry<T> registry) {
        JsonObject obj = new JsonObject();
        for (T element : registry) {
            ResourceLocation elementKey = registry.getKey(element);
            if (elementKey == null) {
                throw new IllegalStateException("Illegal element " + element + " in " + registry + "; no key found");
            }

            String elementName = GenerationUtil.toString(elementKey);
            int elementId = registry.getId(element);
            obj.addProperty(elementName, elementId);
        }
        return obj;
    }

    private static void generateJsonObject(Path outDir, Registry<?> registry) throws IOException {
        Path outPath = outDir.resolve(GenerationUtil.getRegistryName(registry) + ".json");
        GenerationUtil.saveJsonElement(generateJsonObject(registry), outPath);
    }

    static <T> JsonArray generateJsonArray(HolderLookup<T> registry) {
        JsonArray arr = new JsonArray();
        registry.listElementIds()
                .map(ResourceKey::location)
                .map(GenerationUtil::toString)
                .forEach(arr::add);
        return arr;
    }

    private static void generateJsonArray(Path outDir, Registry<?> registry) throws IOException {
        generateJsonArray(outDir, registry.asLookup(), GenerationUtil.getRegistryName(registry));
    }

    private static void generateJsonArray(Path outDir, ResourceKey<? extends Registry<?>> registryKey) throws IOException {
        HolderLookup.RegistryLookup<Object> lookup = GenerationUtil.getVanillaRegistries().lookupOrThrow(registryKey);
        generateJsonArray(outDir, lookup, registryKey);
    }

    private static void generateJsonArray(Path outDir, HolderLookup<?> lookup, ResourceKey<?> registryKey) throws IOException {
        generateJsonArray(outDir, lookup, GenerationUtil.toString(registryKey.location()));
    }

    private static void generateJsonArray(Path outDir, HolderLookup<?> lookup, String registryKey) throws IOException {
        Path outPath = outDir.resolve(registryKey + ".json");
        GenerationUtil.saveJsonElement(generateJsonArray(lookup), outPath);
    }

    @Override
    public void generate(Path outDir, String genName) throws IOException {
        outDir = outDir.resolve(genName);
        Files.createDirectories(outDir);

        generateJsonObject(outDir, BuiltInRegistries.ENTITY_TYPE);
        generateJsonObject(outDir, BuiltInRegistries.ENCHANTMENT);
        generateJsonObject(outDir, BuiltInRegistries.ITEM);
        generateJsonArray(outDir, BuiltInRegistries.PARTICLE_TYPE);
        generateJsonArray(outDir, BuiltInRegistries.ATTRIBUTE);
        generateJsonArray(outDir, BuiltInRegistries.BLOCK);
        generateJsonArray(outDir, Registries.CHAT_TYPE);
    }
}
