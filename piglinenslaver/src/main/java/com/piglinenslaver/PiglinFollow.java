package com.piglinenslaver;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.World;

import java.util.HashMap;
import java.util.UUID;

public class PiglinFollow implements Listener {
    private HashMap<UUID, Player> following = new HashMap<>();
    private HashMap<UUID, Estado> piglinStates = new HashMap<>();
    private HashMap<UUID, Long> lastToggleTimes = new HashMap<>(); // Para evitar cambios rápidos

    private enum Estado {
        FOLLOW,
        IDLE
    }

    public PiglinFollow() {
        startFollowUpdateTask();
    }

    public void followPiglin(Piglin piglin, Player owner) {
        following.put(piglin.getUniqueId(), owner);
        piglin.setTarget(owner); // Hacer que el Piglin siga al jugador
    }

    public void updatePiglinFollow(Mob piglin) {
        Player owner = following.get(piglin.getUniqueId());
        if (owner == null) return;

        double minDistance = Main.config.getDouble("minFollowDistance", 5.0);
        double maxDistance = Main.config.getDouble("maxFollowDistance", 20.0);
        double distance = piglin.getLocation().distance(owner.getLocation());

        if (distance < minDistance) {
            return;
        } else if (distance > maxDistance) {
            piglin.teleport(owner.getLocation()); // Teletransportar si está muy lejos
        } else {
            piglin.getPathfinder().moveTo(owner, 1.0); // Hacer que se acerque al jugador
        }
    }

    private void startFollowUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID piglinId : following.keySet()) {
                    Mob piglin = (Mob) getEntityByUniqueId(piglinId);
                    if (piglin != null) {
                        Estado estado = piglinStates.getOrDefault(piglinId, Estado.FOLLOW);
                        if (estado == Estado.FOLLOW) {
                            updatePiglinFollow(piglin); // Solo actualiza si está en FOLLOW
                        }
                    }
                }
            }
        }.runTaskTimer(Main.plugin, 0, 10); // Se ejecuta cada 10 ticks
    }

    @EventHandler
    public void onPiglinInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Piglin) {
            Piglin piglin = (Piglin) event.getRightClicked();
            Player player = event.getPlayer();
            UUID piglinId = piglin.getUniqueId();

            // Prevenir cambios rápidos con un cooldown de 1 segundo
            long currentTime = System.currentTimeMillis();
            if (lastToggleTimes.containsKey(piglinId) && (currentTime - lastToggleTimes.get(piglinId)) < 1000) {
                return;
            }
            lastToggleTimes.put(piglinId, currentTime);

            // Alternar estados
            Estado estadoActual = piglinStates.getOrDefault(piglinId, Estado.FOLLOW);
            if (estadoActual == Estado.FOLLOW) {
                piglinStates.put(piglinId, Estado.IDLE);
                following.remove(piglinId); // Dejar de seguir
                piglin.setAI(true); // Permitir que el piglin use su IA normal
                player.sendMessage("El Piglin está en estado IDLE.");
            } else {
                piglinStates.put(piglinId, Estado.FOLLOW);
                followPiglin(piglin, player); // Hacer que siga nuevamente
                player.sendMessage("El Piglin ahora te sigue (estado FOLLOW).");
            }
        }
    }

    private Entity getEntityByUniqueId(UUID uniqueId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uniqueId)) {
                    return entity;
                }
            }
        }
        return null;
    }
}
