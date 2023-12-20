package dev.booky.generation;
// Created by booky10 in PacketEventsUtils (16:56 20.12.23)

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.slf4j.Logger;

public final class GenerationMain {

    private static final Logger LOGGER = LogUtils.getLogger();

    static {
        LOGGER.info("Initializing minecraft constants...");
        long start = System.currentTimeMillis();
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        LOGGER.info("Took {}ms to initialize minecraft constants",
                System.currentTimeMillis() - start);
    }

    private GenerationMain() {
    }

    public static void main(String[] args) {
        LOGGER.info("Hello World!");
    }
}
