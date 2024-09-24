package com.piglinenslaver;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PiglinManager implements Listener {

    private HashMap<UUID, BehaviorState> piglinStates = new HashMap<>();
    private HashMap<UUID, BehaviorState> previousStates = new HashMap<>(); // Para almacenar el estado anterior
    private HashMap<UUID, Long> lastToggleTimes = new HashMap<>();
    private Map<UUID, AggressionState> aggressionMap = new HashMap<>(); // Definir un mapa para los subestados de combate
    private HashMap<UUID, Player> owners = new HashMap<>();

    // Estados posibles
    private enum BehaviorState {
        FOLLOW,
        IDLE,
        ATTACK // Nuevo estado ATTACK para cuando el Piglin ataca
    }

    public enum AggressionState {
        DEFENSIVE,
        HOSTILE
    }

    // Constructor que inicia las tareas de seguimiento
    public PiglinManager() {
        startBehaviorStateTask();
    }

    // Método para establecer el dueño de un Piglin
    public void setOwner(Piglin piglin, Player owner) {
        owners.put(piglin.getUniqueId(), owner);
    }

    // Método que actualiza el estado de seguimiento del Piglin
    public void updatePiglinFollow(Mob piglin) {
        Player owner = owners.get(piglin.getUniqueId());
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

    public LivingEntity updatePiglinTargetSearch(Piglin piglin) {
        double searchRadius = 20.0;
        Player owner = owners.get(piglin.getUniqueId());
        Location location = piglin.getLocation();

        // Variables para almacenar la entidad más cercana
        LivingEntity closestEntity = null;
        double closestDistanceSquared = Double.MAX_VALUE;

        // Buscar entidades dentro del radio
        var nearbyEntities = piglin.getWorld().getNearbyEntities(location, searchRadius, searchRadius, searchRadius);
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity))
                continue;
            var livingEntity = (LivingEntity) entity;

            // No atacar al dueño del Piglin
            // Evitar que ataque a otros Piglins domesticados del mismo dueño
            if (((livingEntity instanceof Player) && (livingEntity.equals(owner))) ||
                ((livingEntity instanceof Piglin) && isSameOwner((Piglin)livingEntity, owner))) {
                continue;
            }

            // Verificar si es una entidad hostil (puedes personalizar esto con las condiciones que prefieras)
            if ((livingEntity instanceof Monster) || livingEntity instanceof Player) {
                // Calcular la distancia entre el Piglin y la entidad actual
                double distanceSquared = location.distanceSquared(livingEntity.getLocation());
                
                // Si esta entidad es la más cercana, actualizar la referencia
                if (distanceSquared < closestDistanceSquared) {
                    closestDistanceSquared = distanceSquared;
                    closestEntity = livingEntity;
                }
            }
        }

        return closestEntity;  // Retorna la entidad más cercana (puede ser null si no encontró ninguna)
}


    // Tarea recurrente que actualiza el seguimiento de Piglins y el estado de ataque cada 10 ticks
    private void startBehaviorStateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {

                for (UUID piglinId : owners.keySet()) {
                    Piglin piglin = (Piglin) getEntityByUniqueId(piglinId);
                    if (piglin == null)
                        continue;
                        
                    BehaviorState behaviorState = piglinStates.getOrDefault(piglinId, BehaviorState.FOLLOW);
                    switch (behaviorState)
                    {
                        case IDLE:
                        default:
                            piglin.setAI(true);
                            break;

                        case FOLLOW:
                            updatePiglinFollow(piglin);
                            break;

                        case ATTACK:
                                var aggressionState = aggressionMap.getOrDefault(piglin, AggressionState.DEFENSIVE);
                                @Nullable LivingEntity target;
                                switch (aggressionState)
                                {
                                    case DEFENSIVE:
                                    default:
                                        target = piglin.getTarget();
                                        if (target == null)
                                        {
                                            BehaviorState prevState = previousStates.getOrDefault(piglinId, BehaviorState.IDLE);
                                            piglinStates.put(piglinId, prevState);
                                            break;
                                        }
                                        break;

                                    case HOSTILE:
                                        target = piglin.getTarget();
                                        if (target == null)
                                        {
                                            target = updatePiglinTargetSearch(piglin);
                                            piglin.setTarget(target);
                                        }
                                        break;
                                    }
                                piglin.attack(target);
                                break;
                            }
                    }
                }
        }.runTaskTimer(Main.plugin, 0, 10); // Se ejecuta cada 10 ticks
    }

    // Evento que maneja la interacción con un Piglin para alternar entre estados FOLLOW e IDLE
    @EventHandler
    public void onPiglinInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Piglin) {
            Piglin piglin = (Piglin) event.getRightClicked();
            Player player = event.getPlayer();
            UUID piglinId = piglin.getUniqueId();
            Material eventItemType = player.getInventory().getItemInMainHand().getType();

            // Prevenir cambios rápidos con un cooldown de 1 segundo
            long currentTime = System.currentTimeMillis();
            if (lastToggleTimes.containsKey(piglinId) && (currentTime - lastToggleTimes.get(piglinId)) < 1000) {
                return;
            }
            lastToggleTimes.put(piglinId, currentTime);

            if (eventItemType.toString().endsWith("_SWORD")) {
                AggressionState aggressionState = aggressionMap.getOrDefault(piglinId, AggressionState.DEFENSIVE);
                switch (aggressionState)
                {
                    case DEFENSIVE:
                    default:
                        aggressionMap.put(piglinId, AggressionState.HOSTILE);
                        break;
                        
                    case HOSTILE:
                    aggressionMap.put(piglinId, AggressionState.DEFENSIVE);
                    break;
                }
                player.sendMessage("El Piglin está en estado: " + aggressionMap.get(piglinId));
            }

            else if (eventItemType == Material.AIR)
            {
                BehaviorState behaviorState = piglinStates.getOrDefault(piglinId, BehaviorState.FOLLOW);
                switch (behaviorState) {
                    case FOLLOW:
                    default:
                        piglinStates.put(piglinId, BehaviorState.IDLE);
                        break;
                
                    case IDLE:
                        piglinStates.put(piglinId, BehaviorState.FOLLOW);
                        break;
                }
                player.sendMessage("El Piglin está en estado: " + piglinStates.get(piglinId));
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity attacker = event.getDamager();

        // Si el dañado es un Piglin domesticado
        if (damaged instanceof Piglin) {
            Piglin piglin = (Piglin) damaged;
            Player owner = owners.getOrDefault(piglin.getUniqueId(), null);

            // Verificar que el Piglin tiene un dueño y que el atacante no es el dueño
            if (owner != null && attacker != owner) {
                // Si el atacante es otro Piglin domesticado con el mismo dueño, no hacer nada
                if (attacker instanceof Piglin && isSameOwner((Piglin) attacker, owner)) {
                    return;
                }

                // Guardar el estado previo antes de entrar en ATTACK
                previousStates.put(piglin.getUniqueId(), piglinStates.getOrDefault(piglin.getUniqueId(), BehaviorState.IDLE));
                // Cambiar al estado ATTACK
                piglinStates.put(piglin.getUniqueId(), BehaviorState.ATTACK);
                piglin.setTarget((LivingEntity) attacker);
            }
        }

        // Si el dañado es el dueño del Piglin
        if (damaged instanceof Player) {
            Player player = (Player) damaged;

            for (UUID piglinId : owners.keySet()) {
                if (owners.get(piglinId).equals(player)) {
                    
                    Piglin piglin = (Piglin) getEntityByUniqueId(piglinId);
                    if (piglin != null && attacker != piglin) {
                        // Guardar el estado previo antes de entrar en ATTACK
                        previousStates.put(piglin.getUniqueId(), piglinStates.getOrDefault(piglin.getUniqueId(), BehaviorState.IDLE));
                        // Cambiar al estado ATTACK
                        piglinStates.put(piglin.getUniqueId(), BehaviorState.ATTACK);
                        piglin.setTarget((LivingEntity) attacker);
                    }
                }
            }
        }
    }

    // Obtener la entidad usando su UUID
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

    // Verificar si dos Piglins tienen el mismo dueño
    private boolean isSameOwner(Piglin piglin, Player owner) {
        return owners.get(piglin.getUniqueId()) != null && owners.get(piglin.getUniqueId()).equals(owner);
    }
}
