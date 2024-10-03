package com.piglinenslaver;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Skeleton; // Cambiado de PigZombie a Skeleton
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffectType;

public class Events implements Listener {
    // Mapa que asocia los UUID de los Piglins a sus respectivos jugadores
    private HashMap<UUID, Integer> cureMap = new HashMap<>(); // Mapa de curaciones por PigZombie

    // Constructor
    public Events() {
        // No se necesita la tarea de seguimiento si se elimina el seguimiento del Skeleton
    }

    // Evento que se activa al lanzar una poción a un PigZombie
    @EventHandler
    public void onPotionSplash(PotionSplashEvent e) {
        // Filtra para pociones de curación
        if (e.getPotion().getEffects().stream().anyMatch(effect -> effect.getType().equals(PotionEffectType.WEAKNESS))) {
            for (LivingEntity entity : e.getAffectedEntities()) {
                if (entity instanceof PigZombie) { // Verifica si es un PigZombie
                    PigZombie pigZombie = (PigZombie) entity;
                    Player thrower = (Player) e.getPotion().getShooter();
                    UUID pigZombieUUID = pigZombie.getUniqueId();
                    
                    // Actualiza el número de curaciones
                    int currentCures = cureMap.getOrDefault(pigZombieUUID, 0) + 1;
                    cureMap.put(pigZombieUUID, currentCures);
                    
                    // Verifica si se alcanzó el número necesario de curaciones
                    if (currentCures >= Main.config.getInt("minCures") && currentCures <= Main.config.getInt("maxCures")) {
                        // Transformar al PigZombie en Skeleton adulto y domesticado
                        pigZombie.remove();
                        Skeleton thrall = pigZombie.getWorld().spawn(pigZombie.getLocation(), Skeleton.class); // Spawnea a un Skeleton curado

                        thrall.getWorld().spawnParticle(Particle.FLAME, thrall.getLocation().add(0, 1, 0), 20);

                        // Asigna el Skeleton al jugador que lanzó las pociones
                        String piglinPath = "piglins." + thrall.getUniqueId().toString();
                        Main.config.set(piglinPath + ".owner", thrower.getUniqueId().toString());
                        Main.plugin.saveConfig(); // Guarda en la configuración

                        Main.manager.register(thrall, thrower);
                        thrower.sendMessage("Your Thrall has been tamed!");
                        
                        cureMap.remove(pigZombieUUID);
                    }
                }
            }
        }
    }

}