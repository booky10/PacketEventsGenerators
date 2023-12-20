package dev.booky.generation.generators;
// Created by booky10 in MinecraftSource (19:02 05.09.23)

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.data.Main;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.file.PathUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TagsGenerator implements IGenerator {

    @Override
    public void generate(Path outPath) {
        if (true) return; // TODO

        Path outDir = Path.of("generated");
        if (true) {
            if (Files.isDirectory(outDir)) {
                PathUtils.deleteDirectory(outDir);
            }
            Main.main(new String[]{"--all"});
        }

        List<Path> tagsDirs = List.of(
                outDir.resolve(Path.of("data", "minecraft", "tags")),
                outDir.resolve(Path.of("data", "minecraft", "datapacks", "update_1_21", "data", "minecraft", "tags")),
                outDir.resolve(Path.of("data", "minecraft", "datapacks", "trade_rebalance", "data", "minecraft", "tags")),
                outDir.resolve(Path.of("data", "minecraft", "datapacks", "bundle", "data", "minecraft", "tags"))
        );
        List<Data> data = List.of(
                new Data("blocks", "BlockTags", "StateTypes", BlockTags.class, true),
                new Data("items", "ItemTags", "ItemTypes", ItemTags.class, false)
        );

        Map<Map.Entry<List<String>, List<String>>, String> copyableStuff = new HashMap<>();

        for (Data datum : data) {
            // required for correct order
            Map<String, List<Path>> tagsLocs = new LinkedHashMap<>();
            for (Field field : datum.clazz().getFields()) {
                if (!Modifier.isPublic(field.getModifiers())
                        || !Modifier.isStatic(field.getModifiers())
                        || !Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                TagKey<?> key = (TagKey<?>) field.get(null);
                tagsLocs.put(key.location().getPath(), new ArrayList<>());
            }

            for (Path tagsDir : tagsDirs) {
                Path path = tagsDir.resolve(datum.regName());
                if (!Files.isDirectory(path)) {
                    continue;
                }

                try (Stream<Path> tree = Files.walk(path)) {
                    tree
                            .filter(file -> !file.equals(path) && Files.isRegularFile(file))
                            .forEach(file -> {
                                String tagName = path.relativize(file).toString();
                                tagName = tagName.substring(0, tagName.length() - ".json".length());
                                tagsLocs.get(tagName).add(file);
                            });
                }
            }

            Map<String, Tag> tagObjs = new LinkedHashMap<>();
            for (Map.Entry<String, List<Path>> entry : tagsLocs.entrySet()) {
                JsonArray values = new JsonArray();
                for (Path path : entry.getValue()) {
                    JsonObject tagContents = GsonHelper.parse(Files.readString(path));
                    values.addAll(tagContents.remove("values").getAsJsonArray());
                    Preconditions.checkState(tagContents.isEmpty(), "%s != empty", tagContents);
                }

                List<String> types = new ArrayList<>();
                List<String> tags = new ArrayList<>();
                for (JsonElement elem : values) {
                    String elemStr = elem.getAsString();
                    if (elemStr.indexOf('#') == 0) {
                        tags.add(elemStr.substring(1)
                                .replace("minecraft:", ""));
                    } else {
                        types.add(elemStr.replace("minecraft:", ""));
                    }

                }

                Tag tag = new Tag(datum.tagsClass, datum.typesClass, entry.getKey(), tags, types);
                tagObjs.put(tag.name, tag);
            }

            for (Map.Entry<String, Tag> entry : tagObjs.entrySet()) {
                for (String tag : entry.getValue().tags) {
                    entry.getValue().parents.add(tagObjs.get(tag));
                }
            }
            while (!tagObjs.isEmpty()) {
                List<Tag> roots = tagObjs.values().stream()
                        .filter(tag -> tag.parents.isEmpty())
                        .toList();
                for (Tag root : roots) {
                    System.out.println(root.toString(datum.allowCopy, copyableStuff));
                }
                tagObjs.values().removeAll(roots);
                for (Tag tag : tagObjs.values()) {
                    tag.parents.removeAll(roots);
                }
            }
        }
    }

    private record Data(String regName, String tagsClass, String typesClass, Class<?> clazz, boolean allowCopy) {}

    private static final class Tag {

        private final String tagsClass;
        private final String typesClass;
        private final String name;
        private final List<String> tags;
        private final List<String> types;

        private final List<Tag> parents = new ArrayList<>();

        private Tag(String tagsClass, String typesClass, String name, List<String> tags, List<String> types) {
            this.tagsClass = tagsClass;
            this.typesClass = typesClass;
            this.name = name;
            this.tags = tags;
            this.types = types;
        }

        public String toString(boolean allowCopy, Map<Map.Entry<List<String>, List<String>>, String> copyableStuff) {
            StringBuilder bob = new StringBuilder();
            Runnable applyId = () -> bob
                    .append(this.tagsClass)
                    .append('.')
                    .append(this.name
                            .toUpperCase(Locale.ROOT)
                            .replace(File.separator, "_")
                            .replaceAll("__+", "_"));

            if (allowCopy) {
                applyId.run();
                copyableStuff.put(Map.entry(types, tags), bob.toString());
            } else {
                String copyId = copyableStuff.get(Map.entry(types, tags));
                if (copyId != null) {
                    bob.append("copy(").append(copyId).append(", ");
                    applyId.run();
                    bob.append(");");
                    return bob.toString();
                }
                applyId.run();
            }

            for (String tag : tags) {
                bob.append(".addTag(")
                        .append(this.tagsClass)
                        .append('.')
                        .append(tag.toUpperCase(Locale.ROOT))
                        .append(')');
            }
            if (!types.isEmpty()) {
                String joined = types.stream()
                        .map(type -> type.toUpperCase(Locale.ROOT))
                        .map(type -> this.typesClass + "." + type)
                        .collect(Collectors.joining(", "));
                bob.append(".add(").append(joined).append(')');
            }
            return bob.append(';').toString();
        }
    }
}
