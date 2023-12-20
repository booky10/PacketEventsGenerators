package dev.booky.generation.generators;
// Created by booky10 in MinecraftSource (19:02 05.09.23)

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.booky.generation.util.GenerationUtil;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ItemTypesGenerator implements IGenerator {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void generate(Path outDir, String genName) throws IOException {
        Path genDir = outDir.resolve(genName);
        Files.createDirectories(genDir);

        Path inputPath = genDir.resolve("input.json");
        Path outputPath = genDir.resolve("output.txt");
        if (!Files.exists(inputPath)) {
            LOGGER.warn("Skipping generator, input path {} doesn't exist", inputPath);
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // read inputs
            Set<ResourceLocation> prevItems = GenerationUtil.loadJsonElement(inputPath, JsonObject.class).keySet().stream()
                    .map(ResourceLocation::new).collect(Collectors.toCollection(LinkedHashSet::new));
            Set<ResourceLocation> items = BuiltInRegistries.ITEM.stream()
                    .map(BuiltInRegistries.ITEM::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // determine which items have been removed compared to input
            Set<ResourceLocation> removedItems = new LinkedHashSet<>(prevItems);
            removedItems.removeAll(items);
            writer.write("// Removed items (");
            writer.write(SharedConstants.getCurrentVersion().getName());
            writer.write("): ");
            writer.write(removedItems.toString());
            writer.newLine();

            // determine which items have been added compared to input
            Set<ResourceLocation> addedItems = new LinkedHashSet<>(items);
            addedItems.removeAll(prevItems);
            writer.write("// Added items (");
            writer.write(SharedConstants.getCurrentVersion().getName());
            writer.write("): ");
            writer.write(addedItems.toString());
            writer.newLine();

            // generate code for each added item
            for (ResourceLocation addedItem : addedItems) {
                writer.newLine();
                writer.write("public static final ItemType ");
                writer.write(GenerationUtil.asFieldName(addedItem));
                writer.write(" = builder(\"");
                writer.write(GenerationUtil.toString(addedItem));
                writer.write("\")");

                Item item = BuiltInRegistries.ITEM.get(addedItem);
                writer.write(".setMaxAmount(");
                writer.write(Integer.toString(item.getMaxStackSize()));
                writer.write(')');
                if (item.getMaxDamage() > 0) {
                    writer.write(".setMaxDurability(");
                    writer.write(Integer.toString(item.getMaxDamage()));
                    writer.write(')');
                }
                if (item instanceof BlockItem blockItem) {
                    ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
                    String blockName = blockKey.getPath().toUpperCase(Locale.ROOT);
                    writer.write(".setPlacedType(StateTypes.");
                    writer.write(blockName);
                    writer.write(')');
                }

                String attributesStr = Arrays.stream(ItemAttribute.values())
                        .filter(attribute -> attribute.predicate.test(item))
                        .map(attribute -> "ItemAttribute." + attribute.name())
                        .collect(Collectors.joining(", "));
                if (!attributesStr.isBlank()) {
                    writer.write(".setAttributes(");
                    writer.write(attributesStr);
                    writer.write(')');
                }

                writer.write(".build();");
            }
        }
    }

    public enum ItemAttribute {

        MUSIC_DISC(item -> item instanceof RecordItem),
        EDIBLE(item -> item.getFoodProperties() != null),
        FIRE_RESISTANT(Item::isFireResistant),
        WOOD_TIER(item -> item instanceof TieredItem tiered && tiered.getTier() == Tiers.WOOD),
        STONE_TIER(item -> item instanceof TieredItem tiered && tiered.getTier() == Tiers.STONE),
        IRON_TIER(item -> item instanceof TieredItem tiered && tiered.getTier() == Tiers.IRON),
        DIAMOND_TIER(item -> item instanceof TieredItem tiered && tiered.getTier() == Tiers.DIAMOND),
        GOLD_TIER(item -> item instanceof TieredItem tiered && tiered.getTier() == Tiers.GOLD),
        NETHERITE_TIER(item -> item instanceof TieredItem tiered && tiered.getTier() == Tiers.NETHERITE),
        FUEL(item -> AbstractFurnaceBlockEntity.getFuel().containsKey(item)),
        SWORD(item -> item instanceof SwordItem),
        SHOVEL(item -> item instanceof ShovelItem),
        AXE(item -> item instanceof AxeItem),
        PICKAXE(item -> item instanceof PickaxeItem),
        HOE(item -> item instanceof HoeItem);

        private final Predicate<Item> predicate;

        ItemAttribute(Predicate<Item> predicate) {
            this.predicate = predicate;
        }
    }
}
