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
		this.options = new HashMap<>();
	}

	@Override
	public void export(NBTCompound nbt) {
		var comp = nbt.addCompound("Settings");

		options.forEach((key, value) -> comp.setString(key, value.toString()));
	}

	public void load(NBTCompound nbt) {
		var comp = nbt.getCompound("Settings");
		if (comp == null) {
			this.options = defaultOptions;
			return;
		}

		for (String key : defaultOptions.keySet()) {
			if (comp.hasTag(key)) {
				String value = comp.getString(key);

				Object defaultValue = defaultOptions.get(key);
				if (defaultValue instanceof Integer) {
					this.options.put(key, Integer.parseInt(value));
				} else {
					this.options.put(key, value);
				}

			} else {
				this.options.put(key, defaultOptions.get(key));
			}
		}
	}
}
