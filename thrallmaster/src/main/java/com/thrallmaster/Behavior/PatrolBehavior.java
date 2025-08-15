package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Location;

import com.thrallmaster.States.ThrallState;

public class PatrolBehavior extends Behavior {
	public static int WAIT_TIME = 5;
	private int index;
	private Location startLocation;
	private Location EndLocation;
	private double timer;

	public PatrolBehavior(UUID entityID, ThrallState state, Location starLocation, Location endLocation) {
		super(entityID, state);
		this.index = 0;
		this.startLocation = starLocation;
		this.EndLocation = endLocation;
		this.timer = 0;
	}

	@Override
	public String getBehaviorName() {
		return "Patrolling";
	}

	@Override
	public void onBehaviorStart() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'onBehaviorStart'");
	}

	@Override
	public void onBehaviorTick() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'onBehaviorTick'");
	}

}
