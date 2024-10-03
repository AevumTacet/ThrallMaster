package com.piglinenslaver;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Piglin;
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

public class PiglinManager implements Listener {

    private HashMap<UUID, PiglinState> trackedPiglins = new HashMap<>();

    public PiglinManager() {
        entityBehaviorTask();
    }

    public void register(Piglin piglin, Player owner) {
        PiglinState state = new PiglinState(owner);
        state.setBehavior(new FollowBehavior(piglin, state));
        trackedPiglins.put(piglin.getUniqueId(), state);
    }

    public void unregister(UUID piglinID) {
        System.out.println("Unregistering piglin entity with UUID: " + piglinID);
        trackedPiglins.remove(piglinID);
    }

    public PiglinState getPiglin(UUID piglinID)
    {
        return this.trackedPiglins.getOrDefault(piglinID, null);
    }
    // Tarea recurrente que actualiza el seguimiento de Piglins y el estado de ataque cada 10 ticks
    private void entityBehaviorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID piglinId : trackedPiglins.keySet()) {
                    Piglin piglin = (Piglin) getEntityByUniqueId(piglinId);
                    PiglinState state = trackedPiglins.get(piglinId);

                    if (piglin == null || state == null || state.owner == null)
                    {
                        unregister(piglinId);
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

    // Evento que maneja la interacción con un Piglin para alternar entre estados FOLLOW e IDLE
    @EventHandler
    public void onPiglinInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Piglin) {
            Piglin piglin = (Piglin) event.getRightClicked();
            Player player = event.getPlayer();
            Material itemType = player.getInventory().getItemInMainHand().getType();
            
            UUID piglinId = piglin.getUniqueId();
            if (!trackedPiglins.containsKey(piglinId))
                return;
            PiglinState state = trackedPiglins.get(piglinId);
            
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

        // Si el dañado es un Piglin domesticado
        if (damaged instanceof Piglin && trackedPiglins.containsKey(damaged.getUniqueId())) {
            Piglin piglin = (Piglin) damaged;
            PiglinState state = trackedPiglins.get(piglin.getUniqueId());
            Player owner = state.owner;

            // Verificar que el Piglin tiene un dueño y que el atacante no es el dueño
            if (attacker != owner) {
                // Si el atacante es otro Piglin domesticado con el mismo dueño, no hacer nada
                if (attacker instanceof Piglin && isSameOwner((Piglin) attacker, owner)) {
                    return;
                }
                state.setAttackMode(attacker);
            }
        }

        // Si el dañado es el dueño del Piglin
        if (damaged instanceof Player) {
            Player player = (Player) damaged;

            for (UUID piglinId : trackedPiglins.keySet()) {
                if (trackedPiglins.get(piglinId).owner.equals(player)) {
                    
                    Piglin piglin = (Piglin) getEntityByUniqueId(piglinId);
                    PiglinState state = trackedPiglins.get(piglinId);

                    if (piglin != null && attacker != piglin) 
                    {
                        state.setAttackMode(attacker);
                    }
                }
            }
        }
    }

    // Evento que se activa cuando un Piglin muere U otras entidades a manos del piglin
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Piglin) 
        {
            UUID piglinId = entity.getUniqueId();
            String pstr = "piglins." + piglinId.toString();

            if (Main.config.contains(pstr)) {
                Player owner = Bukkit.getPlayer(UUID.fromString(Main.config.getString(pstr + ".owner")));
                if (owner != null) {
                    owner.sendMessage(entity.getName() + " has died");
                }
                Main.config.set(pstr, null);
                Main.plugin.saveConfig();

                unregister(piglinId);
            }
        }
        // Make it forget
        else
        {
            Entity source = event.getDamageSource().getDirectEntity();
            if (!(source instanceof Piglin))
                return;
          
            UUID piglinId = source.getUniqueId();
            if (trackedPiglins.containsKey(piglinId))
            {
                PiglinState state = trackedPiglins.get(piglinId);
                if (state.target.equals(entity))
                {
                    state.setAttackMode(null);
                    state.target = null;
                    state.setBehavior(new IdleBehavior((Piglin) source, state));
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
        
        if (caller instanceof Piglin)
        {
            Piglin piglin = (Piglin)caller;
            UUID piglinId = piglin.getUniqueId();
            
            if (trackedPiglins.get(piglinId).owner.equals(target)) {
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
    public boolean isSameOwner(Piglin piglin, Player owner) {
        PiglinState state = trackedPiglins.get(piglin.getUniqueId());
        return state != null && state.owner.equals(owner);
    }
}
