package com.thrallmaster.Utils;

import java.util.stream.Stream;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.thrallmaster.Main;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.Settings;
import com.thrallmaster.ThrallManager;
import com.thrallmaster.States.ThrallState;

public final class TargetUtils {
	private static ThrallManager manager = Main.manager;

	public static int SCORE_THRESHOLD = 15;
	public static int PLAYER_SCORE = -5;
	public static int MOB_SCORE = 2;
	public static int PROXIMITY_FAR_SCORE = 5;
	public static int VISIBILITY_SOCRE = 10;
	public static int PLAYER_DANGER_SCORE = -5;
	public static int HEALTH_SCORE_DIV = -10;

	private TargetUtils() {
	}

	public static <T extends LivingEntity> Stream<LivingEntity> getNearby(final Entity from) {
		return getNearby(from, Enemy.class);
	}

	public static <T extends LivingEntity> Stream<LivingEntity> getNearby(final Entity from,
			final Class<T> filterClass) {
		if (from == null) {
			return null;
		}

		final ThrallState state = manager.getThrall(from.getUniqueId());
		final Player owner = state.getOwner();
		final Location location = from.getLocation();
		final Material item = ((AbstractSkeleton) from).getEquipment().getItemInMainHand().getType();
		final double multiplier = MaterialUtils.isRanged(item) ? Settings.THRALL_DETECTION_MUL : 1.0;
		final double radius = Settings.THRALL_DETECTION_RANGE * multiplier;

		return from.getWorld().getNearbyEntities(location, radius, radius, radius, x -> x instanceof LivingEntity)
				.stream()
				.map(x -> (LivingEntity) x)
				.filter(x -> !x.equals(from) && !x.equals(owner))
				.filter(x -> filterClass.isAssignableFrom(x.getClass()) || x instanceof Player);
	}

	public static boolean targetVisible(final ThrallState state, final LivingEntity target) {
		final LivingEntity entity = (LivingEntity) state.getEntity();
		if (entity == null) {
			return false;
		}

		final Location eyeLocation = entity.getEyeLocation().subtract(0, 0.1, 0);

		final Vector direction = target.getEyeLocation().subtract(eyeLocation).toVector();
		final RayTraceResult result = entity.getWorld()
				.rayTrace(eyeLocation, direction, Settings.THRALL_DETECTION_RANGE * Settings.THRALL_DETECTION_MUL,
						FluidCollisionMode.NEVER, true, 2.0, e -> {
							return e.getUniqueId().equals(target.getUniqueId());
						});

		if (result != null && result.getHitEntity() != null) {
			return true;
		}

		return false;
	}

	public static int getTargetScore(final ThrallState state, final LivingEntity target) {
		Entity thrall = state.getEntity();
		double distance = thrall.getLocation().distance(target.getLocation());

		double playerDistance = 999;
		if (state.getOwner() != null) {
			playerDistance = state.getOwner().getLocation().distance(target.getLocation());
		}

		int score = (distance > Settings.THRALL_DETECTION_RANGE ? PROXIMITY_FAR_SCORE : 0)
				+ (targetVisible(state, target) ? 0 : VISIBILITY_SOCRE)
				+ (playerDistance < Settings.THRALL_FOLLOW_MIN * 2 ? PLAYER_DANGER_SCORE : 0)
				+ (target instanceof Player ? PLAYER_SCORE : MOB_SCORE)
				+ (int) (target.getHealth() / HEALTH_SCORE_DIV);

		return score;
	}

	public static void calculateArrowTrajectory(final LivingEntity shooter, Entity arrow) {
		final ThrallState state = manager.getThrall(shooter.getUniqueId());
		final double scale = 1.0 - Settings.THRALL_ACCURACY;
		final double initialSpeed = Settings.ARROW_SPEED;
		final LivingEntity target = state.target;

		final double g = 0.05; // Blocks per Tick per Tick
		final double d = 0.99; // Atmospheric drag coefficient

		if (target != null) {
			final double travelDistance = arrow.getLocation().distance(target.getLocation());
			final Vector targetVelocity = target.getVelocity();

			double travelTime = travelDistance / initialSpeed; // Ticks
			final double drag = Math.pow(d, travelTime);
			travelTime /= drag;

			final Location targetPos = target.getLocation().add(targetVelocity.multiply(travelTime));
			final Vector relativePos = targetPos.subtract(arrow.getLocation()).toVector();

			final double x = relativePos.getX();
			final double y = relativePos.getY();
			final double z = relativePos.getZ();
			final double r = Math.sqrt(x * x + z * z);

			final double u = (initialSpeed * initialSpeed) * drag;

			final double delta = u * u - g * (g * (r * r) + 2 * y * u);
			if (delta < 0) {
				return;
			}

			final double theta = Math.atan((u - Math.sqrt(delta)) / (g * r));
			Vector finalVelocity = new Vector(Math.cos(theta) * x / r, Math.sin(theta),
					Math.cos(theta) * z / r);

			finalVelocity = finalVelocity.add(Vector.getRandom().multiply(scale));
			arrow.setVelocity(finalVelocity.multiply(initialSpeed));
		}
	}

}
