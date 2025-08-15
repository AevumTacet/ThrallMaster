package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Player;

import com.thrallmaster.AggressionState;
import com.thrallmaster.Settings;
import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.BehaviorUtils;

public class PatrolBehavior extends Behavior {
	public static int WAIT_TIME = 5;
	private int index;
	private Location startLocation;
	private Location endLocation;
	private double startTime;
	private int elapsedTicks;

	public PatrolBehavior(UUID entityID, ThrallState state, Location starLocation, Location endLocation) {
		super(entityID, state);
		this.index = 0;
		this.startLocation = starLocation;
		this.endLocation = endLocation;
	}

	@Override
	public String getBehaviorName() {
		return "Patrolling";
	}

	@Override
	public void onBehaviorStart() {
		var entity = this.getEntity();

		if (entity != null) {
			entity.setTarget(null);
		}
		this.startTime = 0;
	}

	@Override
	public void onBehaviorTick() {
		AbstractSkeleton entity = this.getEntity();
		if (entity == null) {
			return;
		}

		Location target = this.index == 0 ? startLocation : endLocation;
		double distance = BehaviorUtils.distance(entity, target);

		if (distance > Settings.THRALL_FOLLOW_MIN) {
			entity.getPathfinder().moveTo(target);
		} else {
			if (elapsedTicks % 100 == 0) {
				BehaviorUtils.randomWalk(entity, startLocation);
			}
			startTime = System.currentTimeMillis();
		}

		if (elapsedTicks % 10 == 0) {
			if (state.aggressionState == AggressionState.HOSTILE) {
				BehaviorUtils.findClosestEnemy(state, this);
			} else if (state.aggressionState == AggressionState.HEALER) {
				BehaviorUtils.findClosestAlly(state, this);
			}
		}

		if (System.currentTimeMillis() - startTime > WAIT_TIME * 1000) {
			index = index == 0 ? 1 : 0;
		}

		Player owner = state.getOwner();
		if (owner != null) {
			double distancePlayer = BehaviorUtils.distance(entity, owner.getLocation());
			if (distancePlayer < Settings.THRALL_FOLLOW_MIN / 2) {
				entity.getPathfinder().stopPathfinding();
				entity.lookAt(state.getOwner().getEyeLocation());
				return;
			}
		}

		elapsedTicks++;
	}

}
