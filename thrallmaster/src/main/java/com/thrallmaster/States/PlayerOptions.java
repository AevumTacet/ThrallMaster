package com.thrallmaster.States;

import java.util.HashMap;
import java.util.UUID;
import com.thrallmaster.IO.Serializable;

import de.tr7zw.nbtapi.NBTCompound;

public class PlayerOptions implements Serializable {
	public UUID playerID;
	public HashMap<String, Object> options;

	public static HashMap<String, Object> defaultOptions = new HashMap<>() {
		{
			put("SCOREBOARD_MODE", 0);
			put("SELECTION_COOLDOWN", 20);
		}
	};

	public PlayerOptions(UUID playerID) {
		this.playerID = playerID;
		this.options = new HashMap<>(defaultOptions);
	}

	@Override
	public void export(NBTCompound nbt) {
		options.forEach((key, value) -> nbt.setString(key, value.toString()));
	}

	public void setConfig(String key, String value) {
		if (!defaultOptions.containsKey(key)) {
			System.err.println("Unknown player setting: " + key);
			return;
		}

		Object defaultValue = defaultOptions.get(key);
		if (defaultValue instanceof Integer) {
			this.options.put(key, Integer.parseInt(value));
		} else {
			this.options.put(key, value);
		}
	}

}
