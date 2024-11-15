package com.thrallmaster;

import java.util.stream.Stream;
import org.bukkit.Material;
import com.google.common.base.Predicate;

public class MaterialUtils 
{

    public enum ArmorType 
    {
        HELMET(material -> isHelmet(material)),
        CHESTPLATE(material -> isChestplate(material)),
        LEGGINGS(material -> isLeggings(material)),
        BOOTS(material -> isBoots(material));

        private final Predicate<Material> matchCondition;

        ArmorType(Predicate<Material> matchCondition) {
            this.matchCondition = matchCondition;
        }

        public boolean matches(Material material) {
            return matchCondition.test(material);
        }
    }

    public static boolean isAir(Material material)
    {
        return material == Material.AIR;
    }

    public static boolean isWeapon(Material material)
    {
        return isMelee(material) || isRanged(material);
    }
    
    public static boolean isArmor(Material material)
    {
        return isHelmet(material) || isChestplate(material) || isLeggings(material) || isBoots(material);
    }

    public static ArmorType getArmorType(Material material) {
        return Stream.of(ArmorType.values())
                .filter(armorType -> armorType.matches(material))
                .findFirst()
                .orElse(null);
    }

    public static boolean isMelee(Material material)
    {
        return isSword(material) || isAxe(material) || isPike(material);
    }

    public static boolean isRanged(Material material)
    {
        return isBow(material);// || isCrossbow(material);
    }


    public static boolean isSword(Material material)
    {
        return (material == Material.IRON_SWORD ||
                material == Material.WOODEN_SWORD ||
                material == Material.STONE_SWORD ||
                material == Material.GOLDEN_SWORD ||
                material == Material.DIAMOND_SWORD ||
                material == Material.NETHERITE_SWORD) ;
    }

    public static boolean isAxe(Material material)
    {
        return (material == Material.IRON_AXE ||
                material == Material.WOODEN_AXE ||
                material == Material.STONE_AXE ||
                material == Material.GOLDEN_AXE ||
                material == Material.DIAMOND_AXE ||
                material == Material.NETHERITE_AXE) ;
    }

    public static boolean isPike(Material material)
    {
        return material.toString().endsWith("pike");
    }

    public static boolean isBow(Material material)
    {
        return material == Material.BOW;
    }

    public static boolean isCrossbow(Material material)
    {
        return material == Material.CROSSBOW;
    }

    public static boolean isBone(Material material)
    {
        return material == Material.BONE;
    }

    public static boolean isHelmet(Material material)
    {
        return (material == Material.IRON_HELMET ||
                material == Material.TURTLE_HELMET ||
                material == Material.LEATHER_HELMET ||
                material == Material.GOLDEN_HELMET ||
                material == Material.CHAINMAIL_HELMET ||
                material == Material.DIAMOND_HELMET ||
                material == Material.NETHERITE_HELMET) ;
    }

    public static boolean isChestplate(Material material)
    {
        return (material == Material.IRON_CHESTPLATE ||
                material == Material.LEATHER_CHESTPLATE ||
                material == Material.GOLDEN_CHESTPLATE ||
                material == Material.CHAINMAIL_CHESTPLATE ||
                material == Material.DIAMOND_CHESTPLATE ||
                material == Material.NETHERITE_CHESTPLATE) ;
    }

    public static boolean isLeggings(Material material)
    {
        return (material == Material.IRON_LEGGINGS ||
                material == Material.LEATHER_LEGGINGS ||
                material == Material.GOLDEN_LEGGINGS ||
                material == Material.CHAINMAIL_LEGGINGS ||
                material == Material.DIAMOND_LEGGINGS ||
                material == Material.NETHERITE_LEGGINGS) ;
    }

    public static boolean isBoots(Material material)
    {
        return (material == Material.IRON_BOOTS ||
                material == Material.LEATHER_BOOTS ||
                material == Material.GOLDEN_BOOTS ||
                material == Material.CHAINMAIL_BOOTS ||
                material == Material.DIAMOND_BOOTS ||
                material == Material.NETHERITE_BOOTS) ;
    }

    public static boolean isHorn(Material material)
    {
        return material == Material.GOAT_HORN;
    }


}
