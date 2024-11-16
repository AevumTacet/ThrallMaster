package com.thrallmaster;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Wolf;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.FollowBehavior;
import com.thrallmaster.IO.Deserializer;
import com.thrallmaster.IO.NBTExporter;
import com.thrallmaster.States.PlayerState;
import com.thrallmaster.States.ThrallState;
import net.kyori.adventure.text.Component;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThrallManager implements Listener {

    public static Logger logger;

    private NBTExporter nbt = new NBTExporter(Main.plugin);
    private HashMap<UUID, PlayerState> playerData = new HashMap<>();
    private HashSet<UUID> trackedEntities = new HashSet<>();
    private HashMap<UUID, Integer> trackedTamingLevel = new HashMap<>();
    private HashMap<UUID, ThrallBoard> trackedBoards = new HashMap<>();

    public ThrallManager() {
        Update();
    }

    public void savePlayers(boolean verbose) {
        nbt.clear();
        playerData.values().forEach(player -> nbt.writePlayer(player));
        nbt.save();
        if (verbose == true) {
            logger.info("Saved player data for " + playerData.size() + " players.");
        }
    }

    public void restorePlayers() {
        playerData.clear();
        var container = nbt.getDataContainer();

        container.getKeys().stream()
                .map(key -> container.getCompound(key))
                .map(comp -> Deserializer.readPlayerState(comp))
                .forEach(state -> {
                    playerData.put(state.getPlayerID(), state);
                });

        logger.info("Restored data for " + container.getKeys().size() + " players.");

        trackedEntities = getThralls()
                .map(state -> state.getEntityID())
                .collect(Collectors.toCollection(HashSet::new));

        logger.info("Restored " + trackedEntities.size() + " entities.");
    }

    public void registerThrall(Skeleton entity, Player owner) {
        logger.info("Registering entity entity with UUID: " + entity.getUniqueId());
        UUID entityID = entity.getUniqueId();
        UUID ownerID = owner.getUniqueId();

        ThrallState state = new ThrallState(entityID, ownerID);
        state.setBehavior(new FollowBehavior(entityID, state));

        entity.setPersistent(true);
        entity.setRemoveWhenFarAway(false);

        getOwnerData(ownerID).addThrall(state);
        trackedEntities.add(entityID);
    }

    public ThrallState unregister(UUID entityID) {
        ThrallState state = getThrall(entityID);

        if (state == null) {
            logger.warning("Thrall state for " + entityID + " is null.");
            return null;
        }

        if (playerData.get(state.getOwnerID()).removeThrall(state)) {
            logger.info("Un-registering entity with UUID: " + entityID);
            trackedEntities.remove(entityID);
        }

        return state;
    }

    public void addAlly(UUID playerID, UUID allyID) {
        PlayerState stats = getOwnerData(playerID);
        stats.addAlly(allyID);
    }

    public void removeAlly(UUID playerID, UUID allyID) {
        PlayerState stats = getOwnerData(playerID);
        stats.removeAlly(allyID);
    }

    public void spawnThrall(Location location, Player owner) {
        World world = location.getWorld();
        Skeleton thrall = world.spawn(location, Skeleton.class);
        thrall.getEquipment().clear();
        thrall.setShouldBurnInDay(false);
        thrall.setAware(true);
        thrall.customName(Component.text("Thrall"));
        thrall.setCustomNameVisible(true);

        AttributeModifier damageModifier = new AttributeModifier(new NamespacedKey(Main.plugin, "DamageModifier"), 2,
                AttributeModifier.Operation.ADD_SCALAR);
        thrall.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addModifier(damageModifier);
        thrall.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30);

        // Bukkit.getMobGoals().removeGoal(thrall, VanillaGoal.AVOID_ENTITY);
        Bukkit.getMobGoals().removeGoal(thrall, VanillaGoal.PANIC);

        world.spawnParticle(Particle.SOUL, thrall.getLocation(), 40, 1, 1, 1, 0.02);
        world.spawnParticle(Particle.FLAME, thrall.getLocation().add(0, 1, 0), 100, 0.1, 0.2, 0.1, 0.05);
        world.spawnParticle(Particle.LANDING_LAVA, thrall.getLocation(), 40, 1, 1, 1, 0.2);
        world.playSound(thrall.getLocation(), Sound.ENTITY_DONKEY_DEATH, 1, 0.5f);
        ;

        registerThrall(thrall, owner);
        owner.sendMessage("Your Thrall rises!");

        updateBoard(owner.getUniqueId());

    }

    public ThrallState getThrall(Player owner, UUID entityID) {
        if (!playerData.containsKey(owner.getUniqueId())) {
            return null;
        }
        return playerData.get(owner.getUniqueId()).getThrall(entityID);
    }

    public ThrallState getThrall(UUID entityID) {
        if (!isEntityTracked(entityID)) {
            return null;
        }
        return getThralls()
                .filter(x -> x.getEntityID().equals(entityID)).findFirst()
                .orElse(null);
    }

    public boolean isEntityTracked(UUID entityID) {
        return trackedEntities.contains(entityID);
    }

    public PlayerState getOwnerData(UUID playerID) {
        return playerData.computeIfAbsent(playerID, PlayerState::new);
    }

    public Stream<PlayerState> getOwners() {
        return playerData.values().stream();
    }

    public Stream<ThrallState> getThralls() {
        return playerData.values().stream().flatMap(PlayerState::getThralls);
    }

    public Stream<ThrallState> getThralls(UUID playerID) {
        if (!playerData.containsKey(playerID)) {
            return Stream.empty();
        }
        return playerData.get(playerID).getThralls();
    }

    private long elapsedTicks = 1;

    private void Update() {
        new BukkitRunnable() {
            @Override
            public void run() {
                getThralls()
                        .forEach(state -> {
                            Entity entity = state.getEntity();
                            Behavior behavior = state.getBehavior();

                            if (behavior != null && entity != null && elapsedTicks % 5 == state.phaseOffset) {
                                behavior.onBehaviorTick();

                                if (entity.isUnderWater()) {
                                    behavior.onBehaviorStuck();
                                }
                            }

                            if (state.isSelected() && entity != null) {
                                entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                                        entity.getLocation().add(0, 2, 0), 5, 0.1, 0.1, 0.1, 0.01);

                                if (System.currentTimeMillis() - state.getLastSelectionTime() >= 10 * 1000) {
                                    state.setSelected(false);
                                }
                            }
                        });

                if (elapsedTicks % 10 == 0) {
                    for (UUID id : playerData.keySet()) {
                        Player player = Bukkit.getPlayer(id);
                        if (player != null && player.isOnline()) {
                            updateBoard(id);
                        }
                    }
                }

                if (elapsedTicks % 600 == 0) {
                    savePlayers(false);
                }

                elapsedTicks += 1;
            }
        }.runTaskTimer(Main.plugin, 0, 2);
    }

    public void updateBoard(UUID playerID) {
        PlayerState stats = getOwnerData(playerID);
        if (stats == null) {
            return;
        }

        if (!trackedBoards.containsKey(playerID)) {
            ThrallBoard board = new ThrallBoard(stats);
            trackedBoards.put(playerID, board);
        }

        ThrallBoard board = trackedBoards.get(playerID);
        board.clearBoard();
        board.updateBoard(stats);
    }

    // Evento que maneja la interacción con un Skeleton para alternar entre estados
    // FOLLOW e IDLE
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {

        if (!ThrallUtils.isThrall(event.getRightClicked())) {
            return;
        }

        Skeleton entity = (Skeleton) event.getRightClicked();

        World world = entity.getWorld();
        Player player = event.getPlayer();
        ItemStack playerItem = player.getInventory().getItemInMainHand();
        ThrallState state = getThrall(player, entity.getUniqueId());

        if (state == null || !ThrallUtils.belongsTo(state, player) || !state.canInteract()) {
            return;
        }

        if (player.isSneaking()) {
            boolean equipped = ThrallUtils.equipThrall(entity, playerItem);
            if (equipped) {
                player.getInventory().setItemInMainHand(null);
            }
        } else {
            if (MaterialUtils.isBone(playerItem.getType())
                    && entity.getHealth() < entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {

                playerItem.setAmount(playerItem.getAmount() - 1);
                player.getInventory().setItemInMainHand(playerItem);
                world.spawnParticle(Particle.HEART, entity.getEyeLocation(), 1);
                world.playSound(entity.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 1);
                entity.heal(1);
            } else {

                var selected = getThralls(player.getUniqueId()).filter(x -> x.isSelected())
                        .collect(Collectors.toList());
                if (selected.isEmpty()) {
                    state.getBehavior().onBehaviorInteract(playerItem.getType());
                } else {
                    selected.forEach(x -> x.getBehavior().onBehaviorInteract(playerItem.getType()));
                }
            }
        }
        world.spawnParticle(Particle.HAPPY_VILLAGER, entity.getEyeLocation(), 10, 0.1, 0.1, 0.1, 0.01);
        updateBoard(state.getOwnerID());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity attacker = event.getDamager() instanceof Arrow
                ? (Entity) ((Arrow) event.getDamager()).getShooter()
                : event.getDamager();

        if (ThrallUtils.isThrall(damaged)) {
            Skeleton entity = (Skeleton) damaged;
            ThrallState state = getThrall(entity.getUniqueId());
            Player owner = state.getOwner();

            if (attacker == owner) {
                if (MaterialUtils.isAir(owner.getInventory().getItemInMainHand().getType())) {
                    state.setSelected(!state.isSelected());
                    event.setCancelled(true);
                }
            } else if (ThrallUtils.isThrall(attacker) && ThrallUtils.isFriendly(attacker, owner)) {
                event.setCancelled(true);
            } else {
                state.setAttackMode(attacker);
            }

            updateBoard(state.getOwnerID());
        }

        // Si el dañado es el dueño del Skeleton
        if (damaged instanceof Player) {
            Player player = (Player) damaged;

            if (ThrallUtils.isThrall(attacker) && ThrallUtils.isFriendly(attacker, player)) {
                event.setCancelled(true);
            } else {
                getThralls(player.getUniqueId()).forEach(state -> {
                    state.setAttackMode(attacker);
                });
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Material material = player.getInventory().getItemInMainHand().getType();

        if (!playerData.containsKey(player.getUniqueId())) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (MaterialUtils.isAir(material)) {
                ThrallCommander.ToggleSelection(player);
                // event.setCancelled(true);
            }

            else if (MaterialUtils.isHorn(material)) {
                ThrallCommander.MultiSelect(player);
                event.setCancelled(true);
            }
        }

        else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (MaterialUtils.isMelee(material)) {
                ThrallCommander.CommandSelection(player);
            }

            else if (MaterialUtils.isHorn(material)) {
                if (player.hasCooldown(Material.GOAT_HORN))
                    return;
                ThrallCommander.HornCommand(player);
            }
        }

        updateBoard(player.getUniqueId());

    }

    // Evento que se activa cuando un Skeleton muere U otras entidades a manos del
    // entity
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (ThrallUtils.isThrall(entity)) {
            ThrallState state = unregister(entity.getUniqueId());

            if (state.getOwner() != null) {
                state.getOwner().sendMessage("Your Thrall has fallen.");
            }

            updateBoard(state.getOwnerID());
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        var caller = event.getEntity();
        var target = event.getTarget();

        if (target == null)
            return;

        if (ThrallUtils.isThrall(caller)) {
            event.setCancelled(true);
        }

        if (caller instanceof Wolf && ThrallUtils.isThrall(target)) {
            ThrallState targetState = getThrall(target.getUniqueId());

            if (ThrallUtils.isFriendly(targetState, caller)) {
                event.setCancelled(true);
            }
        }

        if (caller instanceof IronGolem && ThrallUtils.isThrall(target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        var potion = event.getPotion();
        var effects = potion.getEffects().stream();

        if (!effects.anyMatch(x -> x.getType() == PotionEffectType.WEAKNESS)) {
            return;
        }

        for (LivingEntity entity : event.getAffectedEntities()) {
            if (!(entity instanceof PigZombie)) {
                continue;
            }

            Player thrower = (Player) event.getPotion().getShooter();
            UUID targetID = entity.getUniqueId();

            int currentCures = trackedTamingLevel.getOrDefault(targetID, 0) + 1;
            trackedTamingLevel.put(targetID, currentCures);

            var world = entity.getWorld();
            world.spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 20, 0.1, 0.2, 0.1, 0.01);
            world.spawnParticle(Particle.ENCHANT, entity.getLocation(), 80, 1.5, 1.5, 1.5, 0.02);
            world.playSound(entity.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1, 1);
            ;

            // Verifica si se alcanzó el número necesario de curaciones
            if (currentCures >= Main.config.getInt("minCures") && currentCures <= Main.config.getInt("maxCures")) {
                spawnThrall(entity.getLocation(), thrower);

                entity.remove();
                trackedTamingLevel.remove(targetID);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerState stats = getOwnerData(player.getUniqueId());

        if (stats == null || stats.getCount() == 0) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                updateBoard(player.getUniqueId());
            }
        }.runTaskLater(Main.plugin, 20);
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();

        if (trackedBoards.containsKey(playerID)) {
            trackedBoards.remove(playerID);
        }
    }
}
