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
	private int index;
	private Location startLocation;
	private Location endLocation;
	private long startTime;
	private int elapsedTicks;

	public PatrolBehavior(UUID entityID, ThrallState state, Location startLocation, Location endLocation) {
		super(entityID, state);
		this.index = 0;
		this.startLocation = startLocation;
		this.endLocation = endLocation;
	}

	@Override
	public String getBehaviorName() {
		return Settings.PATROL_NAME;
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
			if (startTime == 0) {
				startTime = System.currentTimeMillis();
			}
		}

		if (elapsedTicks % 10 == 0) {
			if (state.aggressionState == AggressionState.HOSTILE) {
				BehaviorUtils.findClosestEnemy(state, this);
			} else if (state.aggressionState == AggressionState.HEALER) {
				BehaviorUtils.findClosestAlly(state, this);
			}
		}

		if (startTime > 0 && System.currentTimeMillis() - startTime > Settings.THRALL_PATROL_WAIT * 1000) {
			this.index = this.index == 0 ? 1 : 0;
			this.startTime = 0;
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
