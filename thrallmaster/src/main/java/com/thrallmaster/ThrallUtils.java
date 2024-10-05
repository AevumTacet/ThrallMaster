package com.thrallmaster;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Player;

public class ThrallUtils {
    public static double searchRadius = 10.0;

    public static <T extends LivingEntity> LivingEntity findNearestEntity(Entity entity) 
    {
        return findNearestEntity(entity, Enemy.class);
    }

    public static <T extends LivingEntity> LivingEntity findNearestEntity(Entity entity, Class<T> filterClass) 
    {
        if (entity == null)
        {
            return null;
        }
        
        ThrallState state = Main.manager.getThrall(entity.getUniqueId());
        Player owner = state.getOwner();
        Location location = entity.getLocation();

        LivingEntity closestEntity = null;
        double closestDistanceSquared = Double.MAX_VALUE;

        Collection<Entity> nearbyEntities = entity.getWorld().getNearbyEntities(location, searchRadius, searchRadius, searchRadius);
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
}
