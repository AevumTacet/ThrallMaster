package com.piglinenslaver;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import com.piglinenslaver.Behavior.Behavior;
import com.piglinenslaver.Behavior.FollowBehavior;
import com.piglinenslaver.Behavior.IdleBehavior;

import java.util.HashMap;
import java.util.UUID;

public class ThrallManager implements Listener {

    private HashMap<UUID, ThrallState> trackedEntities = new HashMap<>();

    public ThrallManager() {
        entityBehaviorTask();
    }

    public void register(Skeleton entity, Player owner) {
        ThrallState state = new ThrallState(owner);
        state.setBehavior(new FollowBehavior(entity, state));
        trackedEntities.put(entity.getUniqueId(), state);
    }

    public void unregister(UUID entityID) {
        System.out.println("Unregistering entity entity with UUID: " + entityID);
        trackedEntities.remove(entityID);
    }

    public ThrallState getThrall(UUID entityID)
    {
        return this.trackedEntities.getOrDefault(entityID, null);
    }
    // Tarea recurrente que actualiza el seguimiento de Piglins y el estado de ataque cada 10 ticks
    private void entityBehaviorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID entityID : trackedEntities.keySet()) {
                    Skeleton entity = (Skeleton) getEntityByUniqueId(entityID);
                    ThrallState state = trackedEntities.get(entityID);

                    if (entity == null || state == null || state.owner == null)
                    {
                        unregister(entityID);
                        continue;
                    }

                    Behavior behavior = state.getBehavior();
                    if (behavior != null)
                    {
                        behavior.onBehaviorTick();
                    }
                }
                }
        }.runTaskTimer(Main.plugin, 0, 10); // Se ejecuta cada 10 ticks
    }

    // Evento que maneja la interacción con un Skeleton para alternar entre estados FOLLOW e IDLE
    @EventHandler
    public void onPiglinInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Skeleton) {
            Skeleton entity = (Skeleton) event.getRightClicked();
            Player player = event.getPlayer();
            Material itemType = player.getInventory().getItemInMainHand().getType();
            
            UUID entityID = entity.getUniqueId();
            if (!trackedEntities.containsKey(entityID))
                return;
            ThrallState state = trackedEntities.get(entityID);
            
            // Prevenir cambios rápidos con un cooldown de medio segundo
            long currentTime = System.currentTimeMillis();
            if (currentTime - state.lastToggleTime < 500)
                return;
            state.lastToggleTime = currentTime;
            
            state.getBehavior().onBehaviorInteract(itemType);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity attacker = event.getDamager();

        // Si el dañado es un Skeleton domesticado
        if (damaged instanceof Skeleton && trackedEntities.containsKey(damaged.getUniqueId())) {
            Skeleton entity = (Skeleton) damaged;
            ThrallState state = trackedEntities.get(entity.getUniqueId());
            Player owner = state.owner;

            // Verificar que el Skeleton tiene un dueño y que el atacante no es el dueño
            if (attacker != owner) {
                // Si el atacante es otro Skeleton domesticado con el mismo dueño, no hacer nada
                if (attacker instanceof Skeleton && isSameOwner((Skeleton) attacker, owner)) {
                    return;
                }
                state.setAttackMode(attacker);
            }
        }

        // Si el dañado es el dueño del Skeleton
        if (damaged instanceof Player) {
            Player player = (Player) damaged;

            for (UUID entityID : trackedEntities.keySet()) {
                if (trackedEntities.get(entityID).owner.equals(player)) {
                    
                    Skeleton entity = (Skeleton) getEntityByUniqueId(entityID);
                    ThrallState state = trackedEntities.get(entityID);

                    if (entity != null && attacker != entity) 
                    {
                        state.setAttackMode(attacker);
                    }
                }
            }
        }
    }

    // Evento que se activa cuando un Skeleton muere U otras entidades a manos del entity
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Skeleton) 
        {
            UUID entityID = entity.getUniqueId();
            String pstr = "piglins." + entityID.toString();

            if (Main.config.contains(pstr)) {
                Player owner = Bukkit.getPlayer(UUID.fromString(Main.config.getString(pstr + ".owner")));
                if (owner != null) {
                    owner.sendMessage(entity.getName() + " has died");
                }
                Main.config.set(pstr, null);
                Main.plugin.saveConfig();

                unregister(entityID);
            }
        }
        // Make it forget
        else
        {
            Entity source = event.getDamageSource().getDirectEntity();
            if (!(source instanceof Skeleton))
                return;
          
            UUID entityID = source.getUniqueId();
            if (trackedEntities.containsKey(entityID))
            {
                ThrallState state = trackedEntities.get(entityID);
                if (state.target.equals(entity))
                {
                    state.setAttackMode(null);
                    state.target = null;
                    state.setBehavior(new IdleBehavior((Skeleton) source, state));
                }
            }

        }
    }

    @EventHandler
    public void onPiglinTarget(EntityTargetEvent event)
    {
        var caller = event.getEntity();
        var target = event.getTarget();
        
        if (target == null)
            return;
        
        if (caller instanceof Skeleton)
        {
            Skeleton entity = (Skeleton)caller;
            UUID entityID = entity.getUniqueId();
            
            if (trackedEntities.get(entityID).owner.equals(target)) {
                event.setCancelled(true);
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
    public boolean isSameOwner(Skeleton entity, Player owner) {
        ThrallState state = trackedEntities.get(entity.getUniqueId());
        return state != null && state.owner.equals(owner);
    }
}
