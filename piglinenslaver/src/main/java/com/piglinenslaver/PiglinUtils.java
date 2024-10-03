package com.piglinenslaver;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;

public class PiglinUtils {
    public static double searchRadius = 10.0;

    public static <T extends LivingEntity> LivingEntity findNearestEntity(Piglin piglin) 
    {
        return findNearestEntity(piglin, Monster.class);
    }

    public static <T extends LivingEntity> LivingEntity findNearestEntity(Piglin piglin, Class<T> filterClass) 
    {
        PiglinState state = Main.manager.getPiglin(piglin.getUniqueId());
        Player owner = state.owner;
        Location location = piglin.getLocation();

        LivingEntity closestEntity = null;
        double closestDistanceSquared = Double.MAX_VALUE;

        Collection<Entity> nearbyEntities = piglin.getWorld().getNearbyEntities(location, searchRadius, searchRadius, searchRadius);
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity))
                continue;
            var livingEntity = (LivingEntity) entity;

            // No atacar al dueño del Piglin
            // Evitar que ataque a otros Piglins domesticados del mismo dueño
            if ((livingEntity instanceof Player) && (livingEntity.equals(owner))) 
            {
                continue;
            }

            if ((livingEntity instanceof Piglin))
            {
                PiglinState otherState = Main.manager.getPiglin(livingEntity.getUniqueId());
                if (state.isSameOwner(otherState))
                {
                    continue;
                }
            }

            // Verificar si es una entidad hostil (puedes personalizar esto con las condiciones que prefieras)
            if ((filterClass.isAssignableFrom(livingEntity.getClass())) || livingEntity instanceof Player) {
                // Calcular la distancia entre el Piglin y la entidad actual
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
