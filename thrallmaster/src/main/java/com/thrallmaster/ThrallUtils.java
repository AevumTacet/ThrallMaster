package com.thrallmaster;

import java.util.Collection;

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
    public static double searchRadius = 10.0;

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
        
        ThrallState state = Main.manager.getThrall(from.getUniqueId());
        Player owner = state.getOwner();
        Location location = from.getLocation();

        LivingEntity closestEntity = null;
        double closestDistanceSquared = Double.MAX_VALUE;
        double multiplier = MaterialUtils.isRanged(((Skeleton) from).getEquipment().getItemInMainHand().getType()) ? 1.5 : 1.0;

        Collection<Entity> nearbyEntities = from.getWorld().getNearbyEntities(location, searchRadius * multiplier, searchRadius * multiplier, searchRadius * multiplier);
        for (Entity candidate : nearbyEntities) {
            if (!(candidate instanceof LivingEntity))
                continue;
            var livingEntity = (LivingEntity) candidate;

            // No atacar al dueño del Skeleton
            // Evitar que ataque a otros Piglins domesticados del mismo dueño
            if ((livingEntity instanceof Player) && (livingEntity.equals(owner))) 
            {
                continue;
            }

            if ((livingEntity instanceof Skeleton))
            {
                ThrallState otherState = Main.manager.getThrall(livingEntity.getUniqueId());
                if (state.isSameOwner(otherState))
                {
                    continue;
                }
            }

            // Verificar si es una entidad hostil (puedes personalizar esto con las condiciones que prefieras)
            if ((filterClass.isAssignableFrom(livingEntity.getClass())) || livingEntity instanceof Player) {
                // Calcular la distancia entre el Skeleton y la entidad actual
                double distanceSquared = location.distanceSquared(livingEntity.getLocation());
                
                // Si esta entidad es la más cercana, actualizar la referencia
                if (distanceSquared < closestDistanceSquared) {
                    closestDistanceSquared = distanceSquared;
                    closestEntity = livingEntity;
                }
            }
        }

        return closestEntity;
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
