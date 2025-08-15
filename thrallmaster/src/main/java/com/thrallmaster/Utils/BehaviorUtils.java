package com.thrallmaster.Utils;

import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import com.thrallmaster.Main;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.Settings;
import com.thrallmaster.ThrallManager;
import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.HealBehavior;
import com.thrallmaster.Behavior.HostileBehavior;
import com.thrallmaster.States.ThrallState;

public final class BehaviorUtils {
	private static ThrallManager manager = Main.manager;

	private static final BlockFace[] DIRECTIONS = {
			BlockFace.NORTH,
			BlockFace.EAST,
			BlockFace.SOUTH,
			BlockFace.WEST,
	};
	private static Random random = new Random();

	private BehaviorUtils() {
	}

	public static double distance(Entity entity, Location location) {
		return entity.getLocation().distance(location);
	}

	public static void findClosestAlly(ThrallState state, Behavior behavior) {
		LivingEntity entity = (LivingEntity) state.getEntity();

		LivingEntity nearestEntity = TargetUtils.getNearby(entity, AbstractSkeleton.class)
				.filter(x -> ThrallUtils.isFriendly(state, x))
				.filter(x -> x.getHealth() < Settings.THRALL_HEALTH)
				.filter(x -> !(manager.getThrall(x.getUniqueId()).getBehavior() instanceof HostileBehavior))
				.min(Comparator
						.comparingDouble(x -> (x.getLocation().distance(entity.getLocation()) + state.selectionBias)
								* x.getHealth()))
				.orElse(null);
		;
		if (nearestEntity != null) {
			state.target = nearestEntity;
			state.setBehavior(new HealBehavior(state.getEntityID(), state, behavior));
		}
	}

	public static void findClosestEnemy(ThrallState state, Behavior behavior) {
		LivingEntity entity = (LivingEntity) state.getEntity();

		Stream<LivingEntity> entities = TargetUtils.getNearby(entity, Enemy.class)
				.filter(x -> !ThrallUtils.isFriendly(state, x));

		if (!MaterialUtils.isRanged(entity.getEquipment().getItemInMainHand().getType())) {
			entities = entities.filter(x -> !(x instanceof Creeper));
		}
		LivingEntity nearestEntity = entities
				.map(x -> new SimpleEntry<>(x, TargetUtils.getTargetScore(state, x)))
				.filter(entry -> entry.getValue() < Settings.SCORE_THRESHOLD)
				.min(Comparator.comparingDouble(entry -> entry.getValue() + state.selectionBias))
				.map(Map.Entry::getKey)
				.orElse(null);

		if (nearestEntity != null) {
			state.setAttackMode(nearestEntity);
		}
	}

	public static void randomWalk(Mob entity, Location startLocation) {
		Block block = startLocation.getBlock();

		for (int i = 0; i < Settings.THRALL_WANDER_MAX; i++) {

			BlockFace face = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
			Block relative = block.getRelative(face);

			if (relative.getType() == Material.AIR) {
				Block relativeDown = relative.getRelative(BlockFace.DOWN);
				if (relativeDown.isSolid()) {
					block = relativeDown;

					if (Settings.DEBUG_ENABLED) {
						entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
								block.getLocation().add(0.5, 1, 0.5),
								1, 0, 0, 0, 0);
					}
				} else {
					break;
				}
			} else if (!relative.isSolid()) {
				break;
			}

			Block relativeUp = relative.getRelative(BlockFace.UP);
			if (relativeUp.getType() == Material.AIR) {
				block = relative;

				if (Settings.DEBUG_ENABLED) {
					entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
							block.getLocation().add(0.5, 1, 0.5), 0);
				}
			} else {
				block = relativeUp;
			}
		}
		if (Settings.DEBUG_ENABLED) {
			entity.getWorld().spawnParticle(Particle.SOUL, block.getLocation().add(0.5, 1, 0.5), 1, 0, 0, 0, 0);
		}
		entity.getPathfinder().moveTo(block.getLocation().add(0, 1, 0), 1);
	}

}
