package com.thrallmaster;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.FollowBehavior;
import com.thrallmaster.Behavior.IdleBehavior;
import com.thrallmaster.IO.Deserializer;
import com.thrallmaster.IO.NBTExporter;
import com.thrallmaster.States.PlayerState;
import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.InteractionUtils;
import com.thrallmaster.Utils.TargetUtils;
import com.thrallmaster.Utils.ThrallUtils;

import net.kyori.adventure.text.Component;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
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

    public void registerThrall(AbstractSkeleton entity, Player owner) {
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
        AbstractSkeleton thrall = world.spawn(location, WitherSkeleton.class);
        thrall.getEquipment().clear();
        thrall.setShouldBurnInDay(false);
        thrall.setAware(true);
        thrall.customName(Component.text(Settings.THRALL_NAME));
        thrall.setCustomNameVisible(true);

        thrall.getAttribute(Attribute.SCALE).setBaseValue(0.8);
        thrall.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Settings.THRALL_MAX_HEALTH);
        thrall.setHealth(Settings.THRALL_HEALTH);
        clearMobGoals(thrall);

        for (var particle : Settings.SPAWN_PARTICLES) {
            world.spawnParticle(particle.type, thrall.getLocation(), particle.count, particle.bx, particle.by,
                    particle.bz, particle.speed);
        }
        world.playSound(thrall.getLocation(), Settings.SPAWN_SOUND.type, Settings.SPAWN_SOUND.volume,
                Settings.SPAWN_SOUND.pitch);

        registerThrall(thrall, owner);
        owner.sendMessage(String.format(Settings.SPAWN_MESSAGE, thrall.getName()));

        updateBoard(owner.getUniqueId());

    }

    private void clearMobGoals(AbstractSkeleton thrall) {
        if (Settings.DEBUG_ENABLED) {
            logger.info("Attempting to remove selected PathFinderGoals from thrall: " + thrall.getUniqueId());
        }

        Bukkit.getMobGoals().removeGoal(thrall, VanillaGoal.PANIC);
        Bukkit.getMobGoals().removeGoal(thrall, VanillaGoal.RANDOM_STROLL);
        Bukkit.getMobGoals().removeGoal(thrall, VanillaGoal.WATER_AVOIDING_RANDOM_STROLL);
        Bukkit.getMobGoals().removeGoal(thrall, VanillaGoal.FLEE_SUN);
        Bukkit.getMobGoals().removeGoal(thrall, VanillaGoal.RESTRICT_SUN);
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

                            if (behavior != null && entity != null
                                    && elapsedTicks % 5 == state.phaseOffset) {

                                behavior.onBehaviorTick();

                                if (entity.isUnderWater() || entity.isInPowderedSnow()) {
                                    behavior.onBehaviorStuck();
                                }
                            }

                            if (state.isSelected() && entity != null) {
                                entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                                        entity.getLocation().add(0, 2, 0), 5, 0.1, 0.1, 0.1, 0.01);

                                if (System.currentTimeMillis()
                                        - state.getLastSelectionTime() >= Settings.THRALL_SELECTION_COOLDOWN * 1000) {
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
        }.runTaskTimer(Main.plugin, 0, 1);
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

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {

        if (!ThrallUtils.isThrall(event.getRightClicked())) {
            return;
        }

        AbstractSkeleton entity = (AbstractSkeleton) event.getRightClicked();

        World world = entity.getWorld();
        Player player = event.getPlayer();
        ItemStack playerItem = player.getInventory().getItemInMainHand();
        ThrallState state = getThrall(player, entity.getUniqueId());

        if (state == null || !ThrallUtils.belongsTo(state, player) || !state.canInteract()) {
            return;
        }

        if (player.isSneaking()) {
            boolean equipped = InteractionUtils.equipThrall(entity, playerItem);
            if (equipped) {
                player.getInventory().setItemInMainHand(null);
            }
        } else {
            if (MaterialUtils.isBone(playerItem.getType())
                    && entity.getHealth() < entity.getAttribute(Attribute.MAX_HEALTH).getValue()) {

                playerItem.setAmount(playerItem.getAmount() - 1);
                player.getInventory().setItemInMainHand(playerItem);
                player.swingMainHand();
                world.spawnParticle(Particle.HEART, entity.getEyeLocation(), 1);
                world.playSound(entity.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 1);
                entity.heal(1);
            } else if (MaterialUtils.isStick(playerItem.getType())) {
                var selection = getThralls(state.getOwnerID())
                        .filter(x -> x.getEntityID() != state.getEntityID())
                        .filter(x -> x.isSelected())
                        .collect(Collectors.toList());

                if (!selection.isEmpty()) {
                    selection.forEach(x -> x.setBehavior(new FollowBehavior(x.getEntityID(), x, state.getEntity())));
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 0.9f);
                    ThrallUtils.notifyPlayer(state.getOwner(),
                            String.format(Settings.BEHAVIOR_CHANGED_MSG_MULTI, selection.size(),
                                    Settings.FOLLOW_NAME + " " + state.getEntity().getName()));
                }
                // Empty hand, etc
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

        // Thrall owner is damaged
        if (damaged instanceof Player) {
            Player player = (Player) damaged;

            if (ThrallUtils.isThrall(attacker) && ThrallUtils.isFriendly(attacker, player)) {
                event.setCancelled(true);
            } else {
                getThralls(player.getUniqueId())
                        .filter(state -> state.isValidEntity())
                        .forEach(state -> {
                            state.setAttackMode(attacker);
                        });
            }
        }

        // Damaged is a Thrall
        if (ThrallUtils.isThrall(damaged)) {
            AbstractSkeleton entity = (AbstractSkeleton) damaged;
            ThrallState state = getThrall(entity.getUniqueId());
            Player owner = state.getOwner();

            if (attacker.equals(owner)) {
                if (MaterialUtils.isAir(owner.getInventory().getItemInMainHand().getType())) {
                    state.setSelected(!state.isSelected());
                    event.setCancelled(true);

                    if (Settings.DEBUG_ENABLED) {
                        logger.info("Thrall PathFinderGoals: ");
                        Bukkit.getMobGoals().getAllGoals(entity).forEach(x -> logger.info(x.getKey().toString()));
                    }
                }
            } else if (ThrallUtils.isThrall(attacker) && ThrallUtils.isFriendly(attacker, owner)) {
                event.setCancelled(true);
            } else {
                state.setAttackMode(attacker);

                // Block attacks when a shield is equipped
                Random random = new Random();
                ItemStack offHand = entity.getEquipment().getItemInOffHand();
                if (MaterialUtils.isShield(offHand.getType())
                        && (state.target != null && state.target.equals(attacker))) {
                    if (random.nextDouble() < Settings.SHIELD_BLOCK_CHANCE) {
                        entity.swingOffHand();
                        entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                        MaterialUtils.applyDamage(offHand, (int) event.getDamage());
                        event.setCancelled(true);
                    }
                }
            }

            updateBoard(state.getOwnerID());
        }

        // Attacker is a Thrall
        if (ThrallUtils.isThrall(attacker)) {
            LivingEntity livingEntity = (LivingEntity) damaged;

            // Wither Skeletons ranged attacks are considered "unarmed", so the damage needs
            // to be modified
            if (attacker instanceof WitherSkeleton) {
                // if (event.getDamager() instanceof Arrow) {
                // var damage = event.getDamage();
                // event.setDamage(damage);
                // }

                Bukkit.getScheduler().runTaskLater(Main.plugin, new Runnable() {
                    @Override
                    public void run() {
                        livingEntity.removePotionEffect(PotionEffectType.WITHER);
                    }
                }, 1);
            }

            // Apply damage to the current item
            LivingEntity entity = (LivingEntity) attacker;
            ItemStack mainHand = entity.getEquipment().getItemInMainHand();
            MaterialUtils.applyDamage(mainHand, (int) (event.getDamage() / 2));
        }
    }

    @EventHandler
    public void onArrowFired(EntityShootBowEvent event) {
        LivingEntity shooter = event.getEntity();
        Entity arrow = event.getProjectile();
        ItemStack bow = event.getBow();

        if (shooter == null || arrow == null || bow == null) {
            return;
        }

        if (ThrallUtils.isThrall(shooter)) {
            // Remove fire effects from Thralls if they are not carrying a bow with
            // enchanments
            if (!bow.containsEnchantment(Enchantment.FLAME)) {
                if (arrow.getFireTicks() != 0) {
                    arrow.setFireTicks(0);
                }
            }

            // use custom accuracy algorith if enabled.
            if (Settings.THRALL_ACCURACY != -1) {
                TargetUtils.calculateArrowTrajectory(shooter, arrow);
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
                updateBoard(player.getUniqueId());
                // event.setCancelled(true);
            }

            else if (MaterialUtils.isHorn(material)) {
                ThrallCommander.MultiSelect(player);
                updateBoard(player.getUniqueId());
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
                updateBoard(player.getUniqueId());
            }
        }

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        World world = entity.getWorld();

        if (ThrallUtils.isThrall(entity)) {
            ThrallState state = unregister(entity.getUniqueId());

            for (var particle : Settings.DEATH_PARTICLES) {
                world.spawnParticle(particle.type, entity.getLocation(), particle.count, particle.bx, particle.by,
                        particle.bz, particle.speed);
            }
            world.playSound(entity.getLocation(), Settings.DEATH_SOUND.type, Settings.DEATH_SOUND.volume,
                    Settings.DEATH_SOUND.pitch);

            if (state.getOwner() != null) {
                state.getOwner().sendMessage(String.format(Settings.DEATH_MESSAGE, entity.getName()));
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

        if (!effects.anyMatch(x -> x.getType() == Settings.POTION_TYPE)) {
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

            for (var particle : Settings.RITUAL_PARTICLES) {
                world.spawnParticle(particle.type, entity.getLocation(), particle.count, particle.bx, particle.by,
                        particle.bz, particle.speed);
            }
            world.playSound(entity.getLocation(), Settings.RITUAL_SOUND.type, Settings.RITUAL_SOUND.volume,
                    Settings.RITUAL_SOUND.pitch);

            if (currentCures >= Settings.POTION_COUNT) {
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

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (ThrallUtils.isThrall(entity)) {
                AbstractSkeleton thrall = (AbstractSkeleton) entity;
                ThrallState state = getThrall(thrall.getUniqueId());

                if (state.getBehavior() instanceof IdleBehavior) {
                    IdleBehavior behavior = (IdleBehavior) state.getBehavior();
                    thrall.teleport(behavior.startLocation);
                }

                if (entity != null) {
                    clearMobGoals(thrall);
                }
            }
        }
    }
}
