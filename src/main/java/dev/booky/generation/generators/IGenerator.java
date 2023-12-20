package dev.booky.generation.generators;
// Created by booky10 in PacketEventsUtils (16:55 20.12.23)

import java.nio.file.Path;

public interface IGenerator {

    void generate(Path outPath);
}
