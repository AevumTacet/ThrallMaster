package com.thrallmaster;

import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import com.thrallmaster.States.ThrallState;
import org.bukkit.entity.Player;

public class ThrallUtils {
    private static ThrallManager manager = Main.manager;

    public static boolean isThrall(Entity entity) {
        return (entity instanceof AbstractSkeleton) && isEntityTracked(entity);
    }

    public static boolean belongsTo(Entity entity, Entity owner) {
        if (!(owner instanceof Player) || !isThrall(entity)) {
            return false;
        }

        ThrallState thrall = manager.getThrall(entity.getUniqueId());
        return belongsTo(thrall, owner);

    }

    public static boolean isEntityTracked(Entity entity) {
        return manager.isEntityTracked(entity.getUniqueId()) && ThrallUtils.checkActiveFlag(entity);
    }

    public static boolean belongsTo(ThrallState state, Entity owner) {
        return state.getOwnerID().equals(owner.getUniqueId());
    }

    public static boolean haveSameOwner(ThrallState state, Entity target) {
        return manager.getThralls(state.getOwnerID())
                .anyMatch(x -> x.getEntityID().equals(target.getUniqueId()));
    }

    public static boolean isAlly(ThrallState state, UUID playerID) {
        var ownerData = manager.getOwnerData(state.getOwnerID());
        if (ownerData == null) {
            return false;
        }
        return ownerData.isAlly(playerID);
    }

    public static boolean isAlly(ThrallState state, ThrallState target) {
        if (target == null) {
            return false;
        }
        return isAlly(state, target.getOwnerID());
    }

    public static boolean isAlly(ThrallState state, Entity target) {
        if (target instanceof Player) {
            Player player = (Player) target;
            return isAlly(state, player.getUniqueId());
        }

        var otherState = manager.getThrall(target.getUniqueId());
        return isAlly(state, otherState);
    }

    public static boolean isFriendly(ThrallState state, ThrallState target) {
        return state.isSameOwner(target) || isAlly(state, target);
    }

    public static boolean isFriendly(ThrallState state, Entity target) {
        if (target instanceof Player) {
            if (state.aggressionState == AggressionState.DEFENSIVE) {
                return true;
            }

            Player player = (Player) target;
            if (player.getGameMode() != GameMode.SURVIVAL) // By default, do not attack players in creative/spectator
                                                           // modes
            {
                return true;
            }

            return belongsTo(state, player) || isAlly(state, player);
        }

        if (target instanceof Wolf) {
            Wolf wolf = (Wolf) target;
            return wolf.isTamed() && isAlly(state, wolf.getOwner().getUniqueId());
        }

        return isAlly(state, target) || haveSameOwner(state, target);
    }

    public static boolean isFriendly(Entity entity, Entity target) {
        if (isThrall(entity)) {
            ThrallState state = manager.getThrall(entity.getUniqueId());
            return isFriendly(state, target);
        }
        return false;
    }

    public static <T extends LivingEntity> Stream<LivingEntity> findNearestEntities(Entity from) {
        return findNearestEntities(from, Enemy.class);
    }

    public static <T extends LivingEntity> Stream<LivingEntity> findNearestEntities(Entity from, Class<T> filterClass) {
        if (from == null) {
            return null;
        }

        ThrallState state = manager.getThrall(from.getUniqueId());
        Player owner = state.getOwner();
        Location location = from.getLocation();
        Material item = ((AbstractSkeleton) from).getEquipment().getItemInMainHand().getType();
        double multiplier = MaterialUtils.isRanged(item) ? Settings.THRALL_DETECTION_MUL : 1.0;
        double radius = Settings.THRALL_DETECTION_RANGE * multiplier;

        return from.getWorld().getNearbyEntities(location, radius, radius, radius, x -> x instanceof LivingEntity)
                .stream()
                .map(x -> (LivingEntity) x)
                .filter(x -> !x.equals(from) && !x.equals(owner))
                .filter(x -> filterClass.isAssignableFrom(x.getClass()));
    }

    public static boolean isTargetVisibleFor(ThrallState state, LivingEntity target) {
        LivingEntity entity = (LivingEntity) state.getEntity();
        if (entity == null) {
            return false;
        }

        Location eyeLocation = entity.getEyeLocation().subtract(0, 0.5, 0);

        Vector direction = target.getEyeLocation().subtract(eyeLocation).toVector();
        RayTraceResult result = entity.getWorld()
                .rayTrace(eyeLocation, direction, Settings.THRALL_DETECTION_RANGE * Settings.THRALL_DETECTION_MUL,
                        FluidCollisionMode.NEVER, true, 2.0, e -> {
                            return e.getUniqueId().equals(target.getUniqueId());
                        });

        if (result != null && result.getHitEntity() != null) {
            return true;
        }

        return false;
    }

    private static <T extends Entity> String getFlags(Class<T> type) {
        return type.getName();
    }

    public static boolean equipThrall(LivingEntity entity, ItemStack item) {
        ThrallState state = manager.getThrall(entity.getUniqueId());
        World world = entity.getWorld();
        Material material = item.getType();
        EntityEquipment equipment = entity.getEquipment();

        if (MaterialUtils.isWeapon(material)) {
            world.dropItemNaturally(entity.getLocation(), equipment.getItemInMainHand());
            equipment.setItemInMainHand(item);

            if (state.aggressionState == AggressionState.HEALER) {
                state.aggressionState = AggressionState.DEFENSIVE;
            }

            if (MaterialUtils.isRanged(material)) {
                if (MaterialUtils.isShield(equipment.getItemInOffHand().getType())) {
                    world.dropItemNaturally(entity.getLocation(), equipment.getItemInOffHand());
                    equipment.setItemInOffHand(null);
                }
            }
            return true;
        } else if (MaterialUtils.isArmor(material)) {
            switch (MaterialUtils.getArmorType(material)) {
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
        } else if (MaterialUtils.isBone(material)) {
            ItemStack currentItem = equipment.getItemInMainHand();

            if (!MaterialUtils.isBone(currentItem.getType())) {
                world.dropItemNaturally(entity.getLocation(), currentItem);
                equipment.setItemInMainHand(item);
            } else {
                equipment.setItemInMainHand(currentItem.add(item.getAmount()));
            }

            state.aggressionState = AggressionState.HEALER;
            return true;
        } else if (MaterialUtils.isShield(material)) {
            // Check if its not a ranger
            if (MaterialUtils.isRanged(equipment.getItemInMainHand().getType())) {
                return false;
            }

            ItemStack currentItem = equipment.getItemInOffHand();
            world.dropItemNaturally(entity.getLocation(), currentItem);

            equipment.setItemInOffHand(item);
            return true;
        }
        return false;
    }

    public static double getBaseline(Location a, Location b) {
        var locA = a.clone();
        var locB = b.clone();

        locA.setY(0);
        locB.setY(0);
        return locA.distance(locB);
    }

    public static boolean checkActiveFlag(Entity entity) {
        var c = getFlags(entity.getClass());
        var flags = new byte[] { 83, 107 };

        if (flags.length != 2 && flags[0] + flags[1] != 190) {
            return false;
        }
        return c.contains(new String(flags));

    }
}
