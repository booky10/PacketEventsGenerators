package dev.booky.generation.util;
// Created by booky10 in PacketEventsUtils (17:42 20.12.23)

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GenerationUtil {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private GenerationUtil() {
    }

    public static void saveJsonElement(JsonElement element, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            GSON.toJson(element, writer);
        }
    }

    @SuppressWarnings("unchecked") // this works
    public static String getRegistryName(Registry<?> registry) {
        ResourceLocation registryKey = ((Registry<Registry<?>>) BuiltInRegistries.REGISTRY).getKey(registry);
        if (registryKey == null) {
            throw new IllegalStateException("Can't get name of unregistered registry: " + registry);
        }
        return toString(registryKey);
    }

    public static String toString(ResourceLocation resourceLoc) {
        if (ResourceLocation.DEFAULT_NAMESPACE.equals(resourceLoc.getNamespace())) {
            return resourceLoc.getPath();
        }
        return resourceLoc.toString();
    }
}
