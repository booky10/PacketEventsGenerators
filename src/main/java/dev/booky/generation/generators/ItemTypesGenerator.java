package dev.booky.generation.generators;
// Created by booky10 in MinecraftSource (19:02 05.09.23)

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.booky.generation.util.GenerationUtil;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.booky.generation.util.GenerationUtil.asFieldName;
import static net.minecraft.core.component.DataComponents.COMMON_ITEM_COMPONENTS;
import static net.minecraft.core.component.DataComponents.CONTAINER_LOOT;
import static net.minecraft.core.component.DataComponents.CUSTOM_DATA;
import static net.minecraft.core.component.DataComponents.DEBUG_STICK_STATE;
import static net.minecraft.core.component.DataComponents.FOOD;
import static net.minecraft.core.component.DataComponents.INTANGIBLE_PROJECTILE;
import static net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE;
import static net.minecraft.core.component.DataComponents.LOCK;
import static net.minecraft.core.component.DataComponents.MAP_DECORATIONS;
import static net.minecraft.core.component.DataComponents.MAX_DAMAGE;
import static net.minecraft.core.component.DataComponents.MAX_STACK_SIZE;
import static net.minecraft.core.component.DataComponents.RECIPES;

public final class ItemTypesGenerator implements IGenerator {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<DataComponentType<?>> UNSYNCHRONIZED_TYPES = Set.of(
            CUSTOM_DATA, INTANGIBLE_PROJECTILE, MAP_DECORATIONS, DEBUG_STICK_STATE,
            RECIPES, LOCK, CONTAINER_LOOT
    );

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
                    .map(String::toLowerCase).map(ResourceLocation::parse).collect(Collectors.toCollection(LinkedHashSet::new));
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
            Set<ResourceLocation> addedItems = new LinkedHashSet<>(prevItems);
//            addedItems.removeAll(prevItems);
            writer.write("// Added items (");
            writer.write(SharedConstants.getCurrentVersion().getName());
            writer.write("): ");
            writer.write(addedItems.toString());
            writer.newLine();

            // generate code for each added item
            for (ResourceLocation addedItem : addedItems) {
                writer.newLine();
                writer.write("public static final ItemType ");
                writer.write(asFieldName(addedItem));
                writer.write(" = builder(\"");
                writer.write(GenerationUtil.toString(addedItem));
                writer.write("\")");

                Optional<Holder.Reference<Item>> itemHolder = BuiltInRegistries.ITEM.getHolder(addedItem);
                if (itemHolder.isEmpty()) {
                    writer.write(".build(); // TODO: MISSING FROM REGISTRY");
                    continue;
                }

                Item item = itemHolder.get().value();
                DataComponentMap components = item.components(); // default components
                writer.write(".setMaxAmount(");
                writer.write(Integer.toString(item.getDefaultMaxStackSize()));
                writer.write(')');
                Integer maxDamage = components.get(MAX_DAMAGE);
                if (maxDamage != null) {
                    writer.write(".setMaxDurability(");
                    writer.write(Integer.toString(maxDamage));
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

                DataComponentMap defItemComps = COMMON_ITEM_COMPONENTS;
                for (TypedDataComponent<?> comp : components.stream()
                        .filter(comp -> comp.type() != MAX_DAMAGE && comp.type() != MAX_STACK_SIZE)
                        .filter(comp -> !defItemComps.has(comp.type())
                                || !Objects.equals(comp.value(), defItemComps.get(comp.type())))
                        .filter(comp -> !comp.type().isTransient())
                        .filter(comp -> !UNSYNCHRONIZED_TYPES.contains(comp.type()))
                        .sorted(Comparator.comparingInt(c ->
                                BuiltInRegistries.DATA_COMPONENT_TYPE.getId(c.type())))
                        .toList()) {
                    writer.write(".setComponent(");
                    writer.write(asFieldName(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(comp.type())));
                    writer.write(", ");
                    writer.write(c(comp.value()));
                    writer.write(')');
                }

                writer.write(".build();");
            }
        }
    }

    private record Rgb(int rgb) {
        @Override
        public String toString() {
            String hexStr = Integer.toHexString(this.rgb());
            return "0x" + hexStr.toUpperCase(Locale.ROOT);
        }
    }

    private static final Map<UUID, String> UUID_FIELD_NAMES = Map.of(
            UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), "ARMOR_MODIFIER_BOOTS_UUID",
            UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), "ARMOR_MODIFIER_LEGGINGS_UUID",
            UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), "ARMOR_MODIFIER_CHESTPLATE_UUID",
            UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"), "ARMOR_MODIFIER_HELMET_UUID",
            UUID.fromString("C1C72771-8B8E-BA4A-ACE0-81A93C8928B2"), "ARMOR_MODIFIER_BODY_UUID",
            UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF"), "TOOL_MODIFIER_ATTACK_DAMAGE_UUID",
            UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"), "TOOL_MODIFIER_ATTACK_SPEED_UUID"
    );

    private static String c(@Nullable Object val) {
        return switch (val) {
            case Unit ignored -> c(null);
            case null -> "null";
            case String ignored -> "\"" + StringEscapeUtils.escapeJava(val.toString()) + "\"";
            case Integer ignored -> val.toString();
            case Boolean ignored -> val.toString();
            case Float num -> (Math.round(num * 1000d) / 1000d) + "f";
            case Double num -> (Math.round(num * 1000d) / 1000d) + "d";
            case UUID id -> UUID_FIELD_NAMES.getOrDefault(id, "UUID.fromString(\"" + id + "\")");
            case Rarity rarity -> "ItemRarity." + rarity.name();
            case List<?> list -> list.isEmpty() ? "Collections.emptyList()" :
                    "Arrays.asList(" + list.stream().map(ItemTypesGenerator::c)
                            .collect(Collectors.joining(", ")) + ")";
            case Stream<?> stream -> c(stream.toList());
            case Holder<?> holder -> c(holder.value());
            case Optional<?> optional -> c(optional.orElse(null));
            case Rgb rgb -> rgb.toString();
            case ResourceLocation loc -> ResourceLocation.DEFAULT_NAMESPACE.equals(loc.getNamespace())
                    ? "ResourceLocation.minecraft(" + c(loc.getPath()) + ")"
                    : "new ResourceLocation(" + c(loc.getNamespace()) + ", " + c(loc.getPath()) + ")";
            case TagKey<?> tagKey -> c(tagKey.location());
            case ResourceKey<?> resKey -> c(resKey.location());
            case HolderSet<?> holders -> "new MappedEntitySet<>(" + holders.unwrap()
                    .map(ItemTypesGenerator::c, ItemTypesGenerator::c) + ")";
            case ItemEnchantments enchantments -> {
                boolean showInTooltip = ((CompoundTag) ItemEnchantments.CODEC.encodeStart(NbtOps.INSTANCE, enchantments)
                        .getOrThrow()).getBoolean("show_in_tooltip");
                yield "new ItemEnchantments(Collections.emptyMap()" + (enchantments.isEmpty() ? "" : "/*FIXME*/") + ", " + showInTooltip + ")";
            }
            case ItemAttributeModifiers mods -> "new ItemAttributeModifiers(" + c(mods.modifiers())
                    + ", " + mods.showInTooltip() + ")";
            case ItemAttributeModifiers.Entry entry -> "new ItemAttributeModifiers.ModifierEntry("
                    + c(entry.attribute()) + ", " + c(entry.modifier()) + ", " + c(entry.slot()) + ")";
            case Attribute attribute -> "Attributes." + asFieldName(BuiltInRegistries.ATTRIBUTE.getKey(attribute));
            case EquipmentSlotGroup esg -> "ItemAttributeModifiers.EquipmentSlotGroup." + esg.name();
            case AttributeModifier.Operation op -> "AttributeOperation." + switch (op) {
                case ADD_VALUE -> "ADDITION";
                case ADD_MULTIPLIED_BASE -> "MULTIPLY_BASE";
                case ADD_MULTIPLIED_TOTAL -> "MULTIPLY_TOTAL";
            };
            case AttributeModifier mod -> "new ItemAttributeModifiers.Modifier(" + c(mod.id()) + ", " + c(mod.id())
                    + ", " + c(mod.amount()) + ", " + c(mod.operation()) + ")";
            case FoodProperties props -> "new FoodProperties(" + props.nutrition() + ", " + c(props.saturation()) + ", "
                    + props.canAlwaysEat() + ", " + c(props.eatSeconds()) + ", " + c(props.effects()) + ")";
            case FoodProperties.PossibleEffect eff -> "new FoodProperties.PossibleEffect(" + c(eff.effect())
                    + ", " + c(eff.probability()) + ")";
            case MobEffectInstance eff -> "new PotionEffect("
                    + c(eff.getEffect()) + ", " + c(eff.asDetails()) + ")";
            case MobEffectInstance.Details eff -> "new PotionEffect.Properties(" + eff.amplifier()
                    + ", " + eff.duration() + ", " + eff.ambient() + ", " + eff.showParticles()
                    + ", " + eff.showIcon() + ", " + c(eff.hiddenEffect()) + ")";
            case MobEffect eff -> "PotionTypes." + asFieldName(BuiltInRegistries.MOB_EFFECT.getKey(eff));
            case Tool t -> "new ItemTool(" + c(t.rules()) + ", "
                    + c(t.defaultMiningSpeed()) + ", " + t.damagePerBlock() + ")";
            case Block block -> "StateTypes." + asFieldName(
                    BuiltInRegistries.BLOCK.getKey(block)) + ".getMapped()";
            case Tool.Rule rule -> "new ItemTool.Rule(" + c(rule.blocks())
                    + ", " + c(rule.speed()) + ", " + c(rule.correctForDrops()) + ")";
            case DyedItemColor color -> "new ItemDyeColor(" + c(new Rgb(color.rgb()))
                    + ", " + color.showInTooltip() + ")";
            case DyeColor color -> "DyeColor." + color.name();
            case MapPostProcessing mpp -> "ItemMapPostProcessingState." + switch (mpp) {
                case LOCK -> "LOCKED";
                case SCALE -> "SCALED";
            };
            case SuspiciousStewEffects e -> "new SuspiciousStewEffects(" + c(e.effects()) + ")";
            case WritableBookContent c -> "new WritableBookContent(" + c(c.pages()) + ")";
            case BannerPatternLayers l -> "new BannerLayers(" + c(l.layers()) + ")";
            case PotionContents p -> "new ItemPotionContents(" + c(p.potion()) + ", "
                    + c(p.customColor().map(Rgb::new)) + ", " + c(p.customEffects()) + ")";
            case Potion p -> "Potions." + asFieldName(BuiltInRegistries.POTION.getKey(p));
            case ItemContainerContents c -> "new ItemContainerContents(" + c(c.stream()) + ")";
            case CustomData d -> d.isEmpty() ? "new NBTCompound()" : "\"" + d + "\"/*FIXME*/";
            case BundleContents b -> "new BundleContents(" + c(b.itemCopyStream()) + ")";
            case Fireworks f -> "new ItemFireworks(" + f.flightDuration() + ", " + c(f.explosions()) + ")";
            case MapItemColor c -> c(new Rgb(c.rgb()));
            case ChargedProjectiles p -> "new ChargedProjectiles(" + c(p.getItems()) + ")";
            case PotDecorations d -> "new PotDecorations(" + c(d.back()) + ", " + c(d.left())
                    + ", " + c(d.right()) + ", " + c(d.front()) + ")";
            default -> "/*FIXME: " + val + "*/";
        };
    }

    public enum ItemAttribute {

        MUSIC_DISC(item -> item.components().has(JUKEBOX_PLAYABLE)),
        EDIBLE(item -> item.components().has(FOOD)),
        FIRE_RESISTANT(item -> item.components().has(DataComponents.FIRE_RESISTANT)),
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
