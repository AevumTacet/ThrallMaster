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
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTFile;
import de.tr7zw.nbtapi.NBTListCompound;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class ThrallManager implements Listener {

    private static NBTFile m_NBTFile;
    private HashMap<UUID, Integer> trackedTamingLevel = new HashMap<>();
    private HashMap<UUID, ThrallState> trackedEntities = new HashMap<>();

    public ThrallManager() {
        File worldDir = Bukkit.getWorlds().get(0).getWorldFolder();
        try 
        {
            m_NBTFile = new NBTFile(new File(worldDir, "thrall.dat"));
            m_NBTFile.addCompound("Indices");
            m_NBTFile.getCompoundList("States");
        } 
        catch (IOException e) 
        {
            System.err.println("Thrall master NBT IO could not be initialized!");
            e.printStackTrace();
        }

        entityBehaviorTask();
    }

    public static NBTListCompound getNBTCompound(UUID id)
    {
        NBTCompound indices = m_NBTFile.getCompound("Indices");
        if (indices.hasTag(id.toString()))
        {
            var index = indices.getInteger(id.toString());
            return m_NBTFile.getCompoundList("States").get(index);
        }
        
        return null;
    }
    
    public static void saveNBT()
    {
        if (m_NBTFile == null)
        {
            return;
        }

        try 
        {
            m_NBTFile.save();
        } 
        catch (IOException e) 
        {
            System.err.println("Could not save NBT settings!");
            e.printStackTrace();
        }
    }

    public void registerAllEntities(World world)
    {
        NBTCompoundList states = m_NBTFile.getCompoundList("States");
        System.out.println("Found " + states.size() + " entity compounds.");
        trackedEntities.clear();
        
        for (var iterator = states.iterator(); iterator.hasNext();)
        {
            ReadWriteNBT nbt = iterator.next();
            UUID entityID = UUID.fromString(nbt.getString("EntityID"));
            UUID ownerID =  UUID.fromString(nbt.getString("OwnerID"));
            String currentBehavior =  nbt.getString("CurrentBehavior");

            ThrallState state = new ThrallState(ownerID);
            switch (currentBehavior) {
                case "IDLE":
                    int[] locationArray = nbt.getIntArray("IdleLocation");
                    if (locationArray != null && locationArray.length == 3)
                    {
                        Location startLocation = new Location(world, locationArray[0], locationArray[1], locationArray[2]);
                        state.setBehavior(new IdleBehavior(entityID, state, startLocation));
                        break;
                    }
                    state.setBehavior(new IdleBehavior(entityID, state));
                    break;
                    
                case "FOLLOW":
                    state.setBehavior(new FollowBehavior(entityID, state));
                    break;
            
                default:
                    System.err.println("Thrall state is unspecified, defaulting to follow");
                    state.setBehavior(new FollowBehavior(entityID, state));
                    break;
            }

            trackedEntities.put(entityID, state);
        }

        System.out.println("Loading Thrall entities completed. " + trackedEntities.size());
    }

    public void register(Skeleton entity, Player owner) {
        System.out.println("Registering entity entity with UUID: " + entity.getUniqueId());
        UUID entityID = entity.getUniqueId();
        ThrallState state = new ThrallState(owner);
        
        NBTCompound indices = m_NBTFile.getCompound("Indices");
        NBTCompoundList states = m_NBTFile.getCompoundList("States");

        indices.setInteger(entityID.toString(), states.size());

        NBTCompound nbt = states.addCompound();
        nbt.setString("EntityID", entity.getUniqueId().toString());
        nbt.setString("OwnerID", owner.getUniqueId().toString());
        nbt.setString("CurrentBehavior", "FOLLOW");

        entity.setPersistent(true);
        state.setBehavior(new FollowBehavior(entity.getUniqueId(), state));
        
        trackedEntities.put(entity.getUniqueId(), state);
        
        saveNBT();

        System.out.println(m_NBTFile.toString());
    }

    public void unregister(UUID entityID) {
        if (trackedEntities.remove(entityID) != null)
        {
            System.out.println("Unregistering entity with UUID: " + entityID);

            NBTCompound indices = m_NBTFile.getCompound("Indices");
            NBTCompoundList states = m_NBTFile.getCompoundList("States");
            
            int index = indices.getInteger(entityID.toString());
            indices.removeKey(entityID.toString());
            states.remove(index);

            saveNBT();
        }
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
                for (var iterator = trackedEntities.keySet().iterator(); iterator.hasNext();)
                {
                    UUID entityID = iterator.next();
                    ThrallState state = trackedEntities.get(entityID);

                    if (state == null || state.ownerID == null)
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
        }.runTaskTimer(Main.plugin, 0, 10);
    }

    // Evento que maneja la interacción con un Skeleton para alternar entre estados FOLLOW e IDLE
    @EventHandler
    public void onPiglinInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Skeleton) {
            Skeleton entity = (Skeleton) event.getRightClicked();
            UUID entityID = entity.getUniqueId();
            if (!trackedEntities.containsKey(entityID))
            {
                return;
            }

            Player player = event.getPlayer();
            Material itemType = player.getInventory().getItemInMainHand().getType();
            
            ThrallState state = trackedEntities.get(entityID);
            if (!state.ownerID.equals(player.getUniqueId()))
            {
                return;
            }
            
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
