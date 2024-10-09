package com.thrallmaster;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.FollowBehavior;
import com.thrallmaster.Behavior.IdleBehavior;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTFile;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThrallManager implements Listener {

    public static Logger logger;
    private static NBTFile m_NBTFile;
    private HashMap<UUID, Integer> trackedTamingLevel = new HashMap<>();
    private HashMap<UUID, HashSet<ThrallState>> trackedEntities = new HashMap<>();

    public ThrallManager() {
        File worldDir = Bukkit.getWorlds().get(0).getWorldFolder();
        try 
        {
            m_NBTFile = new NBTFile(new File(worldDir, "thrall.dat"));
            m_NBTFile.addCompound("ThrallStates");
        } 
        catch (IOException e) 
        {
            logger.warning("Thrall master NBT IO could not be initialized!");
            e.printStackTrace();
        }

        Update();
    }

    public static NBTCompound getNBTCompound(UUID id)
    {
        NBTCompound states = m_NBTFile.getCompound("ThrallStates");
        return states.getCompound(id.toString());
    }
    
    public static void saveNBT()
    {
        if (m_NBTFile == null)
        {
            return;
        }

        try 
        {
            var count = m_NBTFile.getCompound("ThrallStates").getKeys().size();
            logger.info("Saving Thrall NBT state.");
            m_NBTFile.save();
            logger.info(count + " Entitites saved.");
            logger.info(m_NBTFile.toString());
        } 
        catch (IOException e) 
        {
            logger.warning("Could not save NBT settings!");
            e.printStackTrace();
        }
    }

    public void registerAllEntities(World world)
    {
        NBTCompound states = m_NBTFile.getCompound("ThrallStates");
        Set<String> dataKeys = states.getKeys();

        logger.info("Found " + dataKeys.size() + " entity compounds.");
        trackedEntities.clear();
        int thrallCount = 0;
        
        for (String key : dataKeys)
        {
            ReadWriteNBT nbt = states.getCompound(key);

            UUID entityID = UUID.fromString(key);
            UUID ownerID =  UUID.fromString(nbt.getString("OwnerID"));
            String currentBehavior =  nbt.getString("CurrentBehavior");

            ThrallState state = new ThrallState(entityID, ownerID);
            switch (currentBehavior) {
                case "IDLE":
                    if (nbt.hasTag("IdleLocationW") && nbt.hasTag("IdleLocationX") && 
                        nbt.hasTag("IdleLocationY") && nbt.hasTag("IdleLocationZ"))
                    {
                        String locationW = nbt.getString("IdleLocationW");
                        double locationX = nbt.getDouble("IdleLocationX");
                        double locationY = nbt.getDouble("IdleLocationY");
                        double locationZ = nbt.getDouble("IdleLocationZ");

                        Location startLocation = new Location(Bukkit.getWorld(locationW), locationX, locationY, locationZ);
                        state.setBehavior(new IdleBehavior(entityID, state, startLocation));
                    }
                    else
                    {
                        logger.warning("Warning: Idle state with no IdleLocation tag found.");
                        state.setBehavior(new IdleBehavior(entityID, state, null));
                    }

                    break;
                    
                case "FOLLOW":
                    state.setBehavior(new FollowBehavior(entityID, state));
                    break;
            
                default:
                    logger.warning("Thrall state is unspecified, defaulting to follow");
                    state.setBehavior(new FollowBehavior(entityID, state));
                    break;
            }
            
            trackedEntities.computeIfAbsent(ownerID, x -> new HashSet<>()).add(state);
            thrallCount ++;
        }

        logger.info("Loading Thrall entities completed. " + thrallCount + " entities in total.");
        logger.info(trackedEntities.toString());
    }

    public void register(Skeleton entity, Player owner) {
        logger.info("Registering entity entity with UUID: " + entity.getUniqueId());
        UUID entityID = entity.getUniqueId();
        UUID ownerID = owner.getUniqueId();

        NBTCompound states = m_NBTFile.getCompound("ThrallStates");
        ThrallState state = new ThrallState(entityID, ownerID);
        NBTCompound nbt = states.addCompound(entityID.toString());

        nbt.setString("OwnerID", ownerID.toString());
        nbt.setString("CurrentBehavior", "FOLLOW");

        entity.setPersistent(true);
        state.setBehavior(new FollowBehavior(entityID, state));
        entity.setRemoveWhenFarAway(false);

        trackedEntities.computeIfAbsent(ownerID, x -> new HashSet<>()).add(state);
    }

    public ThrallState unregister(UUID entityID) {
        ThrallState state = getThrall(entityID);

        if (state == null)
        {
            logger.warning("Thrall state for " + entityID + " is null.");
            return null;
        }

        if (trackedEntities.get(state.getOwnerID()).remove(state))
        {
            logger.info("Unregistering entity with UUID: " + entityID);

            NBTCompound states = m_NBTFile.getCompound("ThrallStates");
            states.removeKey(entityID.toString());
        }

        return state;
    }

    public ThrallState getThrall(Player owner, UUID entityID)
    {
        return trackedEntities.getOrDefault(owner.getUniqueId(), new HashSet<>()).stream()
                .filter(x -> x.getEntityID().equals(entityID)).findFirst()
                .orElse(null);
    }
    public ThrallState getThrall(UUID entityID)
    {
        return trackedEntities.values().stream()
                .flatMap(HashSet<ThrallState>::stream)
                .filter(x -> x.getEntityID().equals(entityID)).findFirst()
                .orElse(null);
    }

    public boolean isEntityTracked(UUID entityID)
    {
        return trackedEntities.values().stream()
                .flatMap(HashSet<ThrallState> :: stream)
                .anyMatch(x -> x.getEntityID().equals(entityID));
    }

    public boolean isThrall(Entity entity)
    {
        return (entity instanceof Skeleton) && isEntityTracked(entity.getUniqueId());
    }

    public boolean belongsTo(Entity entity, Entity owner) {
        return getThrall(entity.getUniqueId()).getOwnerID().equals(owner.getUniqueId());
    }
    public boolean belongsTo(ThrallState state, Entity owner) {
        return state.getOwnerID().equals(owner.getUniqueId());
    }

    public boolean haveSameOwner(ThrallState entity, ThrallState target)
    {
        return entity.isSameOwner(target);
    }

    public Stream<ThrallState> getThralls()
    {
        return trackedEntities.values().stream().flatMap(HashSet<ThrallState>::stream);
    }
    public Stream<ThrallState> getThralls(UUID playerID)
    {
        return trackedEntities.getOrDefault(playerID, new HashSet<>()).stream();
    }

    
    // Tarea recurrente que actualiza el seguimiento de Skeletons y el estado de ataque cada 10 ticks
    private long elapsedTicks = 1;

    private void Update()
     {
        new BukkitRunnable() {
            @Override
            public void run()
             {  
                trackedEntities.values().stream().flatMap(HashSet<ThrallState>::stream)
                .forEach(state ->
                {
                    Entity entity = state.getEntity();
                    Behavior behavior = state.getBehavior();

                    if (behavior != null && entity != null)
                    {
                        behavior.onBehaviorTick();
                    }

                    if (state.isSelected() && entity != null)
                    {
                        entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation(), 5, 0.1, 0.1, 0.1, 0.01);
                        
                        if (System.currentTimeMillis() - state.getLastSelectionTime() >= 20 * 1000)
                        {
                            state.setSelected(false);
                        }
                    }
                });

                if (elapsedTicks % 600 == 0)
                {
                    saveNBT();
                }

                elapsedTicks += 1;
            }
        }.runTaskTimer(Main.plugin, 0, 10);
    }

    // Evento que maneja la interacción con un Skeleton para alternar entre estados FOLLOW e IDLE
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {

        if (!isThrall(event.getRightClicked())) {
            return;
        }

        Skeleton entity = (Skeleton) event.getRightClicked();

        World world = entity.getWorld();
        Player player = event.getPlayer();
        ItemStack playerItem = player.getInventory().getItemInMainHand();
        ThrallState state = getThrall(player, entity.getUniqueId());

        if (state == null || !belongsTo(state, player) || !state.canInteract()) {
            return;
        }
        
        if (player.isSneaking()) {
            ThrallUtils.equipThrall(entity, playerItem);
            player.getInventory().setItemInMainHand(null);
        } 
        else {
            if (playerItem.getType() == Material.BONE
                    && entity.getHealth() < entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
                playerItem.setAmount(playerItem.getAmount() - 1);
                player.getInventory().setItemInMainHand(playerItem);
                world.spawnParticle(Particle.HEART, entity.getEyeLocation(), 1);
                world.playSound(entity.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 1);
                entity.heal(1);
            }
            else {

                var selected = getThralls(player.getUniqueId()).filter(x -> x.isSelected()).collect(Collectors.toList());
                if (selected.isEmpty())
                {
                    state.getBehavior().onBehaviorInteract(playerItem.getType());
                }
                else {
                    selected.forEach(x -> x.getBehavior().onBehaviorInteract(playerItem.getType()));
                }
            }
        }

        world.spawnParticle(Particle.HAPPY_VILLAGER, entity.getEyeLocation(), 10, 0.1, 0.1, 0.1, 0.01);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity attacker = event.getDamager() instanceof Arrow 
                            ? (Entity)((Arrow)event.getDamager()).getShooter() 
                            : event.getDamager();

        if (isThrall(damaged)) 
        {
            Skeleton entity = (Skeleton) damaged;
            ThrallState state = getThrall(entity.getUniqueId());
            Player owner = state.getOwner();

            
            if (attacker == owner)
            {
                if (owner.getInventory().getItemInMainHand().getType() == Material.AIR)
                {
                    state.setSelected(!state.isSelected());
                    event.setCancelled(true);
                }
            }
            else if (isThrall(attacker) && belongsTo(attacker, owner))
            {
                event.setCancelled(true);
            }
            else
            {
                state.setAttackMode(attacker);
            }
        }

        // Si el dañado es el dueño del Skeleton
        if (damaged instanceof Player) {
            Player player = (Player) damaged;

            if (isThrall(attacker) && belongsTo(attacker, player))
            {
                event.setCancelled(true);
            }
            else
            {
                getThralls(player.getUniqueId()).forEach(state -> 
                {
                    state.setAttackMode(attacker);
                });
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();
        if (event.getAction() == Action.LEFT_CLICK_AIR && player.getInventory().getItemInMainHand().getType() == Material.AIR)
        {
            RayTraceResult rayTraceResult = player.rayTraceEntities(20);
            
            if (rayTraceResult != null)
            {
                Entity entity = rayTraceResult.getHitEntity();
                ThrallState state = getThrall(player, entity.getUniqueId());

                if (entity != null && state != null)
                {
                    state.setSelected(!state.isSelected());
                    return;
                }
            }

            // Otherwise..
            getThralls(player.getUniqueId()).forEach(x ->
                {
                    x.setSelected(false);
                });

        }
    }

    // Evento que se activa cuando un Skeleton muere U otras entidades a manos del entity
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (isThrall(entity))
        {
            ThrallState state = unregister(entity.getUniqueId());
            
            if (state.getOwner() != null)
            {
                state.getOwner().sendMessage("Your Thrall has fallen.");
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

        if (isThrall(caller))
        {
            ThrallState callerState = getThrall(caller.getUniqueId());
            if (belongsTo(callerState, target))
            {
                event.setCancelled(true);
            }

            if (isThrall(target))
            {
                ThrallState targetState = getThrall(target.getUniqueId());
                if (haveSameOwner(callerState, targetState))
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

        if (!effects.anyMatch(x -> x.getType() == PotionEffectType.WEAKNESS)) 
        {
            return;
        }

        for (LivingEntity entity : event.getAffectedEntities()) 
        {
            if (!(entity instanceof PigZombie)) 
            {
                continue;
            }

            Player thrower = (Player) event.getPotion().getShooter();
            UUID targetID = entity.getUniqueId();
            
            int currentCures = trackedTamingLevel.getOrDefault(targetID, 0) + 1;
            trackedTamingLevel.put(targetID, currentCures);
            
            var world = entity.getWorld();
            world.spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 20, 0.1, 0.2, 0.1, 0.01);
            world.spawnParticle(Particle.ENCHANT, entity.getLocation(), 80, 1.5, 1.5, 1.5, 0.02);
            world.playSound(entity.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1, 1);;
            
            // Verifica si se alcanzó el número necesario de curaciones
            if (currentCures >= Main.config.getInt("minCures") && currentCures <= Main.config.getInt("maxCures")) 
            {
                Skeleton thrall = world.spawn(entity.getLocation(), Skeleton.class);
                thrall.getEquipment().clear();

                world.spawnParticle(Particle.SOUL, thrall.getLocation(), 40, 1, 1, 1, 0.02);
                world.spawnParticle(Particle.FLAME, thrall.getLocation().add(0, 1, 0), 100, 0.1, 0.2, 0.1, 0.05);
                world.spawnParticle(Particle.LANDING_LAVA, thrall.getLocation(), 40, 1, 1, 1, 0.2);
                world.playSound(entity.getLocation(), Sound.ENTITY_DONKEY_DEATH, 1, 0.5f);;

                entity.remove();

                register(thrall, thrower);
                thrower.sendMessage("Your Thrall rises!");
                
                trackedTamingLevel.remove(targetID);
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
        
        Entity caller = event.getEntity();
        if (isThrall(caller))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWorldLoaded(WorldLoadEvent event)
    {
        this.registerAllEntities(event.getWorld());
    }

}
