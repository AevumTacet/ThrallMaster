package com.thrallmaster;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.FollowBehavior;
import com.thrallmaster.Behavior.IdleBehavior;

import de.tr7zw.nbtapi.NBT;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class ThrallManager implements Listener {

    private HashMap<UUID, Integer> trackedTamingLevel = new HashMap<>();
    private HashMap<UUID, ThrallState> trackedEntities = new HashMap<>();

    public ThrallManager() {
        entityBehaviorTask();
    }

    public void registerAllEntities(World world)
    {
        trackedEntities.clear();
        for (Skeleton entity : world.getEntitiesByClass(Skeleton.class))
        {
            boolean isTamed = NBT.getPersistentData(entity, nbt -> nbt.getBoolean("Tamed"));
            if (!isTamed)
            {
                continue;
            }

            UUID ownerID =  NBT.getPersistentData(entity, nbt -> nbt.getUUID("OwnerUUID"));
            String currentBehavior =  NBT.getPersistentData(entity, nbt -> nbt.getString("CurrentBehavior"));
           
            ThrallState state = new ThrallState(ownerID);
            
            switch (currentBehavior) {
                case "IDLE":
                    int[] locationArray = NBT.getPersistentData(entity, nbt -> nbt.getIntArray("IdleLocation"));
                    if (locationArray != null && locationArray.length == 3)
                    {
                        Location startLocation = new Location(world, locationArray[0], locationArray[1], locationArray[2]);
                        state.setBehavior(new IdleBehavior(entity, state, startLocation));
                        break;
                    }
                    state.setBehavior(new IdleBehavior(entity, state));
                    break;
                    
                case "FOLLOW":
                    state.setBehavior(new FollowBehavior(entity, state));
                    break;
            
                default:
                    System.err.println("Thrall state is unspecified, defaulting to follow");
                    state.setBehavior(new FollowBehavior(entity, state));
                    break;
            }

            trackedEntities.put(entity.getUniqueId(), state);
        }

        System.out.println("Loading Thrall entities completed. " + trackedEntities.size() + " entities recovered.");
    }

    public void register(Skeleton entity, Player owner) {
        ThrallState state = new ThrallState(owner);

        NBT.modifyPersistentData(entity, nbt ->
        {
            nbt.setBoolean("Tamed", true);
            nbt.setUUID("OwnerUUID", owner.getUniqueId());
            nbt.setString("CurrentBehavior", "FOLLOW");
        });

        entity.setPersistent(true);
        state.setBehavior(new FollowBehavior(entity, state));
        trackedEntities.put(entity.getUniqueId(), state);
        System.out.println("Registering entity entity with UUID: " + entity.getUniqueId());
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

                    if (entity == null || state == null || state.ownerID == null)
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
            
            entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getEyeLocation(), 10, 0.1, 0.1, 0.1, 0.01);
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
            Player owner = state.getOwner();

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
                if (trackedEntities.get(entityID).ownerID.equals(player.getUniqueId())) {
                    
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

            if (trackedEntities.containsKey(entityID)) {
                Player owner = trackedEntities.get(entityID).getOwner();
                owner.sendMessage("Your Thrall has fallen.");
                unregister(entityID);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event)
    {
        var caller = event.getEntity();
        var target = event.getTarget();
        
        if (target == null)
            return;
            
        if (caller instanceof Skeleton)
        {
            if (!trackedEntities.containsKey(caller.getUniqueId()))
                return;

            var callerState = trackedEntities.get(caller.getUniqueId());
            if (callerState.ownerID.equals(target)) 
            {
                event.setCancelled(true);
            }

            if (trackedEntities.containsKey(target.getUniqueId()))
            {
                var targetState = trackedEntities.get(target.getUniqueId());
                if (callerState.ownerID.equals(targetState.ownerID))
                {
                    event.setCancelled(true);
                }
            }
        }
    }

     @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        var potion = event.getPotion();  
        var effects = potion.getEffects().stream();  

        if (effects.anyMatch(effect -> effect.getType().equals(PotionEffectType.WEAKNESS))) {
            for (LivingEntity entity : event.getAffectedEntities()) {

                if (entity instanceof PigZombie) {
                    Player thrower = (Player) event.getPotion().getShooter();
                    UUID targetID = entity.getUniqueId();
                    
                    int currentCures = trackedTamingLevel.getOrDefault(targetID, 0) + 1;
                    trackedTamingLevel.put(targetID, currentCures);
                    
                    var world = entity.getWorld();
                    world.spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 20, 0.1, 0.2, 0.1, 0.01);
                    world.playSound(entity.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1, 1);;
                    
                    // Verifica si se alcanzó el número necesario de curaciones
                    if (currentCures >= Main.config.getInt("minCures") && currentCures <= Main.config.getInt("maxCures")) {
                        
                        Random random = new Random();
                        Skeleton thrall = world.spawn(entity.getLocation(), Skeleton.class);

                        if (random.nextDouble() > 0.5)
                        {
                            var ironSword = new ItemStack(Material.IRON_SWORD);
                            thrall.getEquipment().setItemInMainHand(ironSword);
                        }

                        world.spawnParticle(Particle.SOUL, thrall.getLocation(), 50, 0.1, 0.1, 0.1, 0.02);
                        world.spawnParticle(Particle.FLAME, thrall.getLocation().add(0, 1, 0), 100, 0.1, 0.2, 0.1, 0.05);
                        world.spawnParticle(Particle.LANDING_LAVA, thrall.getLocation(), 25, 0.01, 0.01, 0.01, 0.06);
                        world.playSound(entity.getLocation(), Sound.ENTITY_PIG_DEATH, 1, 1);;
                        entity.remove();

                        Main.manager.register(thrall, thrower);
                        thrower.sendMessage("Your Thrall has risen!");
                        
                        trackedTamingLevel.remove(targetID);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityBurn(EntityCombustEvent event)
    {
        if ((event instanceof EntityCombustByBlockEvent))
        {
            return;
        }

        var caller = event.getEntity();
        if (caller instanceof Skeleton)
        {
            Skeleton entity = (Skeleton)caller;
            UUID entityID = entity.getUniqueId();
            
            if (trackedEntities.containsKey(entityID)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onWorldLoaded(WorldLoadEvent event)
    {
        this.registerAllEntities(event.getWorld());
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
        return state != null && state.ownerID.equals(owner);
    }
}
