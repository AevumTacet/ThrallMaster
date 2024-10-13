package com.thrallmaster;

import java.util.Comparator;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

public class ThrallUtils {
    private static ThrallManager manager = Main.manager;
    public static double searchRadius = 10.0;


    public static boolean isThrall(Entity entity)
    {
        return (entity instanceof Skeleton) && manager.isEntityTracked(entity.getUniqueId());
    }

    public static boolean belongsTo(Entity entity, Entity owner) {
        if (!(owner instanceof Player))
        {
            return false;
        }
        return manager.getThrall(entity.getUniqueId()).belongsTo(owner);
    }

    public static boolean haveSameOwner(ThrallState thrall, Entity target)
    {
        return manager.getThralls(thrall.getOwnerID())
            .anyMatch(state -> state.getEntityID().equals(target.getUniqueId()));
    }

    public static boolean isAlly(ThrallState thrall, UUID playerID)
    {
        var ownerData =  manager.getOwnerData(thrall.getOwnerID());
        if (ownerData == null)
        {
            return false;
        }
        return ownerData.isAlly(playerID);
    }
    public static boolean isAlly(ThrallState thrall, ThrallState target)
    {
        if (target == null)
        {
            return false;
        }
        return isAlly(thrall, target.getOwnerID());
    }
    public static boolean isAlly(ThrallState thrall, Entity target)
    {
        if (target instanceof Player)
        {
            return isAlly(thrall, target.getUniqueId());
        }
        var state = manager.getThrall(target.getUniqueId());
        return isAlly(thrall, state);
    }

    public static boolean isFriendly(ThrallState thrall, ThrallState target)
    {
        return thrall.isSameOwner(target) || isAlly(thrall, target);
    }
    public static boolean isFriendly(ThrallState thrall, Entity target)
    {
        if (target instanceof Player)
        {
            return belongsTo(target, target) || isAlly(thrall, target.getUniqueId());
        }
        return haveSameOwner(thrall, target) || isAlly(thrall, target);
    }
    public static boolean isFriendly(Entity entity, Entity target)
    {
        if (isThrall(entity))
        {
            ThrallState state = manager.getThrall(entity.getUniqueId());
            return isFriendly(state, target);
        }
        // TODO: Add support for tameable animals (i.e. Dogs)
        return false;
    }


    public static <T extends LivingEntity> LivingEntity findNearestEntity(Entity from) 
    {
        return findNearestEntity(from, Enemy.class);
    }

    public static <T extends LivingEntity> LivingEntity findNearestEntity(Entity from, Class<T> filterClass) 
    {
        if (from == null)
        {
            return null;
        }
        
        ThrallState state = manager.getThrall(from.getUniqueId());
        Player owner = state.getOwner();
        Location location = from.getLocation();
        double multiplier = MaterialUtils.isRanged(((Skeleton) from).getEquipment().getItemInMainHand().getType()) ? 1.5 : 1.0;

        return (LivingEntity) from.getWorld().getNearbyEntities(location, searchRadius * multiplier, searchRadius * multiplier, searchRadius * multiplier).stream()
            .filter(x -> x instanceof LivingEntity && !x.equals(owner))
            .filter(x -> !(isThrall(x) && isFriendly(state, x)))
            .filter(x -> filterClass.isAssignableFrom(x.getClass()) || (x instanceof Player && ((Player)x).getGameMode() == GameMode.SURVIVAL) )
            .min(Comparator.comparingDouble(x -> x.getLocation().distance(location)))
            .orElse(null);
    } 


    public static void equipThrall(LivingEntity entity, ItemStack item)
    {
        World world = entity.getWorld();
        Material material = item.getType();
        EntityEquipment equipment = entity.getEquipment();

        if (MaterialUtils.isWeapon(material))
        {
            world.dropItemNaturally(entity.getLocation(), equipment.getItemInMainHand());
            equipment.setItemInMainHand(item);
        }
        else if (MaterialUtils.isArmor(material))
        {
            switch (MaterialUtils.getArmorType(material))
            {
                case HELMET:
                world.dropItemNaturally(entity.getLocation(), equipment.getHelmet());
                equipment.setHelmet(item);
                break;

                case CHESTPLATE:
                world.dropItemNaturally(entity.getLocation(), equipment.getChestplate());
                equipment.setChestplate(item);
                break;

                case LEGGINGS:
                world.dropItemNaturally(entity.getLocation(), equipment.getLeggings());
                equipment.setLeggings(item);
                break;

                case BOOTS:
                world.dropItemNaturally(entity.getLocation(), equipment.getBoots());
                equipment.setBoots(item);
                break;

                default:
                    break;
                
            }
        }

    }
}
