package com.thrallmaster;

import java.util.Comparator;
import java.util.UUID;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.thrallmaster.States.ThrallState;

import org.bukkit.entity.Player;

public class ThrallUtils {
    private static ThrallManager manager = Main.manager;
    public static double searchRadius = 10.0;


    public static boolean isThrall(Entity entity)
    {
        return (entity instanceof Skeleton) && manager.isEntityTracked(entity.getUniqueId());
    }

    public static boolean belongsTo(Entity entity, Entity owner) {
        if (!(owner instanceof Player) || !isThrall(entity))
        {
            return false;
        }

        ThrallState thrall = manager.getThrall(entity.getUniqueId());
        return belongsTo(thrall, owner);
        
    }
    public static boolean belongsTo(ThrallState state, Entity owner) 
    {
        return state.getOwnerID().equals(owner.getUniqueId());
    }

    public static boolean haveSameOwner(ThrallState state, Entity target)
    {
        return manager.getThralls(state.getOwnerID())
            .anyMatch(x -> x.getEntityID().equals(target.getUniqueId()));
    }

    public static boolean isAlly(ThrallState state, UUID playerID)
    {
        var ownerData =  manager.getOwnerData(state.getOwnerID());
        if (ownerData == null)
        {
            return false;
        }
        return ownerData.isAlly(playerID);
    }
    public static boolean isAlly(ThrallState state, ThrallState target)
    {
        if (target == null)
        {
            return false;
        }
        return isAlly(state, target.getOwnerID());
    }
    public static boolean isAlly(ThrallState state, Entity target)
    {
        if (target instanceof Player)
        {
            Player player = (Player) target;
            return isAlly(state, player.getUniqueId());
        }

        var otherState = manager.getThrall(target.getUniqueId());
        return isAlly(state, otherState);
    }

    public static boolean isFriendly(ThrallState state, ThrallState target)
    {
        return state.isSameOwner(target) || isAlly(state, target);
    }
    public static boolean isFriendly(ThrallState state, Entity target)
    {
        if (target instanceof Player)
        {
            if (state.aggressionState == AggressionState.DEFENSIVE)
            {
                return true;
            }

            Player player = (Player) target;
            if (player.getGameMode() != GameMode.SURVIVAL)  // By default, do not attack players in creative/spectator modes
            {
                return true;
            }

            return belongsTo(state, player) || isAlly(state, player);
        }

        if (target instanceof Wolf)
        {
            Wolf wolf = (Wolf) target;
            return wolf.isTamed() && isAlly(state, wolf.getOwner().getUniqueId());
        }

        return isAlly(state, target) || haveSameOwner(state, target);
    }

    public static boolean isFriendly(Entity entity, Entity target)
    {
        if (isThrall(entity))
        {
            ThrallState state = manager.getThrall(entity.getUniqueId());
            return isFriendly(state, target);
        }
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
        double radius = searchRadius * multiplier;

        return (LivingEntity) from.getWorld()
            .getNearbyEntities(location, radius, radius, radius,
             x -> x instanceof LivingEntity && !x.equals(owner)).stream()
            .filter(x -> filterClass.isAssignableFrom(x.getClass()))
            .filter(x -> !isFriendly(state, x))
            // .filter(x -> isTargetVisibleFor(state, (LivingEntity) x))
            .min(Comparator.comparingDouble(x -> x.getLocation().distance(location) + state.selectionBias))
            .orElse(null);
    } 

    public static boolean isTargetVisibleFor(ThrallState state, LivingEntity target)
    {
        LivingEntity entity = (LivingEntity) state.getEntity();
        if (entity == null)
        {
            return false;
        }

        Location eyeLocation = entity.getEyeLocation().subtract(0, 0.5, 0);

        Vector direction = target.getEyeLocation().subtract(eyeLocation).toVector();
        RayTraceResult result = entity.getWorld()
            .rayTrace(eyeLocation, direction, searchRadius * 1.5, FluidCollisionMode.NEVER, true, 2.0, e ->
            {
                return e.getUniqueId().equals(target.getUniqueId());
            });

        if (result != null && result.getHitEntity() != null)
        {
            return true;
        }

        return false;
    }

    public static boolean equipThrall(LivingEntity entity, ItemStack item)
    {
        World world = entity.getWorld();
        Material material = item.getType();
        EntityEquipment equipment = entity.getEquipment();

        if (MaterialUtils.isWeapon(material))
        {
            world.dropItemNaturally(entity.getLocation(), equipment.getItemInMainHand());
            equipment.setItemInMainHand(item);
            return true;
        }
        else if (MaterialUtils.isArmor(material))
        {
            switch (MaterialUtils.getArmorType(material))
            {
                case HELMET:
                world.dropItemNaturally(entity.getLocation(), equipment.getHelmet());
                equipment.setHelmet(item);
                return true;

                case CHESTPLATE:
                world.dropItemNaturally(entity.getLocation(), equipment.getChestplate());
                equipment.setChestplate(item);
                return true;

                case LEGGINGS:
                world.dropItemNaturally(entity.getLocation(), equipment.getLeggings());
                equipment.setLeggings(item);
                return true;

                case BOOTS:
                world.dropItemNaturally(entity.getLocation(), equipment.getBoots());
                equipment.setBoots(item);
                return true;

                default:
                return false;
                
            }
        }
        return false;
    }
}
