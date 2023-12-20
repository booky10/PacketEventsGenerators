package dev.booky.generation;
// Created by booky10 in PacketEventsUtils (16:56 20.12.23)

import com.mojang.logging.LogUtils;
import dev.booky.generation.generators.BlockMappingsGenerator;
import dev.booky.generation.generators.IGenerator;
import dev.booky.generation.generators.ItemTypesGenerator;
import dev.booky.generation.generators.RegistryGenerator;
import dev.booky.generation.generators.TagsGenerator;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GenerationMain {

    private static final Logger LOGGER = LogUtils.getLogger();

    private GenerationMain() {
    }

    public static void main(String[] args) {
        try {
            Path outDir = Path.of(args[0]); // passed by gradle ":generate" task
            run(outDir);
        } finally {
            // safely shutdown - sometimes causes issues where log4j just
            // deletes a few of the last log messages
            LogManager.shutdown();
        }
    }

    public static void run(Path outDir) {
        try {
            Files.createDirectories(outDir);
        } catch (IOException exception) {
            throw new RuntimeException("Error while creating output directory at " + outDir, exception);
        }

        LOGGER.info("Initializing minecraft constants...");
        long start = System.currentTimeMillis();
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        LOGGER.info("Took {}ms to initialize minecraft constants",
                System.currentTimeMillis() - start);

        LOGGER.info("Initializing generators...");
        List<IGenerator> generators = List.of(
                new RegistryGenerator(),
                new BlockMappingsGenerator(),
                new TagsGenerator(),
                new ItemTypesGenerator()
        );

        LOGGER.info("Running {} generators...", generators.size());
        for (IGenerator generator : generators) {
            String genName = generator.getClass().getSimpleName();
            LOGGER.info(" Running {}...", genName);

            try {
                generator.generate(outDir, genName);
            } catch (IOException exception) {
                LOGGER.error(" Error while running {}", genName, exception);
            }
        }
    }
}
