package com.warriorssmp.jobs.hunter.model;

import org.bukkit.Material;

/**
 * A handful of resources are tracked as their BLOCK form (e.g. CARROTS/POTATOES,
 * the growing-crop materials used to detect BlockBreakEvent) but have a separate
 * ITEM form for anything that needs to construct an ItemStack — GUI icons, giving
 * a physical item, etc. Most materials (ores, logs, wheat) double as both and need
 * no mapping; this only covers the ones that don't.
 */
public final class IconUtil {

    private IconUtil() {}

    public static Material safeIcon(Material material) {
        if (material.isItem()) return material;
        return switch (material) {
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT;
            case WHEAT -> Material.WHEAT; // shares the same ID, kept for clarity
            case SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
            case COCOA -> Material.COCOA_BEANS;
            case CAVE_VINES, CAVE_VINES_PLANT -> Material.GLOW_BERRIES;
            case TWISTING_VINES_PLANT -> Material.TWISTING_VINES;
            case WEEPING_VINES_PLANT -> Material.WEEPING_VINES;
            case CHORUS_FLOWER, CHORUS_PLANT -> Material.CHORUS_FRUIT;
            default -> Material.PAPER; // safe fallback so a future block-only entry never crashes a menu
        };
    }

    /** GUI items need a Material to render, but Hunter tracks EntityType — most
     *  mobs have a matching spawn egg, which makes a perfectly good icon. A few
     *  bosses don't (no vanilla spawn egg exists for them), so those get a
     *  curated substitute instead. */
    public static Material mobIcon(org.bukkit.entity.EntityType type) {
        return switch (type) {
            case ENDER_DRAGON -> Material.DRAGON_HEAD;
            case WITHER -> Material.WITHER_SKELETON_SKULL;
            case GIANT -> Material.ZOMBIE_HEAD;
            default -> {
                Material egg = Material.matchMaterial(type.name() + "_SPAWN_EGG");
                yield egg != null ? egg : Material.BONE;
            }
        };
    }
}
