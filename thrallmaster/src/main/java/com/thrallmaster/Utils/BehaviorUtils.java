package com.thrallmaster.Utils;

import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

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

}
