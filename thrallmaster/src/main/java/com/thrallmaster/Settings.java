package com.thrallmaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public class Settings {
	public static String THRALL_NAME;
	public static int THRALL_HEALTH;
	public static int THRALL_MAX_HEALTH;
	public static double THRALL_SELECTION_COOLDOWN;
	public static double THRALL_ACCURACY;
	public static double THRALL_DETECTION_RANGE;
	public static double THRALL_DETECTION_MUL;
	public static double THRALL_WANDER_MAX;
	public static double THRALL_FOLLOW_MIN;
	public static double THRALL_FOLLOW_MAX;
	public static double THRALL_AGGRO_COOLDOWN;
	public static double RUN_SPEED_MUL;
	public static int POTION_COUNT;
	public static double SHIELD_BLOCK_CHANCE;
	public static double ARROWFALL_MULTIPLIER;
	public static float SELECTION_BIAS;

	public static String SPAWN_MESSAGE;
	public static String DEATH_MESSAGE;
	public static PotionEffectType POTION_TYPE;
	public static SoundInfo RITUAL_SOUND;
	public static SoundInfo SPAWN_SOUND;
	public static SoundInfo DEATH_SOUND;
	public static ArrayList<ParticleInfo> RITUAL_PARTICLES;
	public static ArrayList<ParticleInfo> SPAWN_PARTICLES;
	public static ArrayList<ParticleInfo> DEATH_PARTICLES;
	public static String AGGRESSION_CHANGED_MSG;
	public static String AGGRESSION_CHANGED_MSG_MULTI;

	public static String IDLE_NAME;
	public static String FOLLOW_NAME;
	public static String ATTACK_NAME;
	public static String HEAL_NAME;
	public static String DEFENSIVE_NAME;
	public static String AGGRESSIVE_NAME;
	public static HashMap<AggressionState, String> AGGRESSION_MAP;

	private Settings() {
	}

	public static void loadConfig(Plugin plugin) {
		FileConfiguration config = plugin.getConfig();

		loadGeneralSettings(config.getConfigurationSection("general"));
		loadStateSettings(config.getConfigurationSection("states"));
		loadRitualSettings(config.getConfigurationSection("ritual-action"));
		loadSpawnSettings(config.getConfigurationSection("thrall-spawn"));
		loadDeathSettings(config.getConfigurationSection("thrall-death"));
	}

	private static void loadGeneralSettings(ConfigurationSection section) {
		THRALL_NAME = section.getString("spawn-name", "Thrall");
		THRALL_HEALTH = section.getInt("initial-health", 20);
		THRALL_MAX_HEALTH = section.getInt("max-health", 30);
		THRALL_ACCURACY = validate(section.getDouble("ranged-accuracy", -1), -1.0,
				x -> ((x >= 0) && (x <= 1)) ? x : null, false);

		THRALL_SELECTION_COOLDOWN = section.getDouble("selection-release-cooldown", 30);
		THRALL_DETECTION_RANGE = section.getDouble("detection-range", 10);
		THRALL_DETECTION_MUL = section.getDouble("ranged-detection-multiplier", 1.5);
		RUN_SPEED_MUL = section.getDouble("run-speed-multiplier", 1.5);
		AGGRESSION_CHANGED_MSG = section.getString("aggression-state-message", "");
		AGGRESSION_CHANGED_MSG_MULTI = section.getString("horn-command-message", "");

		THRALL_WANDER_MAX = section.getDouble("idle-wander-max-distance", 4);
		THRALL_FOLLOW_MIN = section.getDouble("follow-min-distance", 3);
		THRALL_FOLLOW_MAX = section.getDouble("follow-max-distance", 30);
		THRALL_AGGRO_COOLDOWN = section.getDouble("attack-demiss-cooldown", 30);
		SHIELD_BLOCK_CHANCE = section.getDouble("shield-block-chance", 0.5);
		ARROWFALL_MULTIPLIER = section.getDouble("arrow-fall-distance", 10);
		SELECTION_BIAS = (float) section.getDouble("selection-bias", 4);
	}

	private static void loadSpawnSettings(ConfigurationSection section) {
		SPAWN_PARTICLES = new ArrayList<>();

		SPAWN_MESSAGE = section.getString("message", "");
		var spawnParticles = section.getConfigurationSection("particles");
		for (String key : spawnParticles.getKeys(false)) {
			ParticleInfo particle = ParticleInfo.fromConfig(spawnParticles.getConfigurationSection(key));
			SPAWN_PARTICLES.add(particle);
		}
		SPAWN_SOUND = SoundInfo.fromConfig(section.getConfigurationSection("sound"));
	}

	private static void loadStateSettings(ConfigurationSection section) {
		IDLE_NAME = section.getString("idle", "Guarding");
		FOLLOW_NAME = section.getString("follow", "Following");
		ATTACK_NAME = section.getString("attack", "Attacking");
		HEAL_NAME = section.getString("heal", "Healing");
		DEFENSIVE_NAME = section.getString("defensive", "Defending");
		AGGRESSIVE_NAME = section.getString("aggressive", "Aggressive");

		AGGRESSION_MAP = new HashMap<AggressionState, String>() {
			{
				put(AggressionState.DEFENSIVE, DEFENSIVE_NAME);
				put(AggressionState.HOSTILE, AGGRESSIVE_NAME);
				put(AggressionState.HEALER, HEAL_NAME);
			}
		};
	}

	private static void loadDeathSettings(ConfigurationSection section) {
		DEATH_PARTICLES = new ArrayList<>();

		DEATH_MESSAGE = section.getString("message", "");
		var deathParticles = section.getConfigurationSection("particles");
		for (String key : deathParticles.getKeys(false)) {
			ParticleInfo particle = ParticleInfo.fromConfig(deathParticles.getConfigurationSection(key));
			DEATH_PARTICLES.add(particle);
		}
		DEATH_SOUND = SoundInfo.fromConfig(section.getConfigurationSection("sound"));
	}

	@SuppressWarnings("deprecation")
	private static void loadRitualSettings(ConfigurationSection section) {
		RITUAL_PARTICLES = new ArrayList<>();

		POTION_COUNT = section.getInt("potion-count", 4);
		POTION_TYPE = validate(section.getString("potion-type", "weakness"), PotionEffectType.WEAKNESS,
				x -> PotionEffectType.getByName(x), false);

		var ritualParticles = section.getConfigurationSection("particles");
		for (String key : ritualParticles.getKeys(false)) {
			ParticleInfo particle = ParticleInfo.fromConfig(ritualParticles.getConfigurationSection(key));
			RITUAL_PARTICLES.add(particle);
		}
		RITUAL_SOUND = SoundInfo.fromConfig(section.getConfigurationSection("sound"));
	}

	private static <K, T> T validate(K key, T def, Function<K, T> map, boolean canBeNull) {
		try {
			var result = map.apply(key);
			if (result == null && !canBeNull) {
				throw new Exception();
			}

			return result;
		} catch (Exception e) {
			System.err.println(
					"[Settings] Invalid parameter \"" + key.toString() + "\". Defaulting to \"" + def.toString() + "\".");
			return def;
		}
	}

	private static <K, T> T validate(K key, T def, Function<K, T> map) {
		return validate(key, def, map, true);
	}

	public static class ParticleInfo {
		public Particle type;
		public int count;
		public double speed;
		public double bx;
		public double by;
		public double bz;

		public static ParticleInfo fromConfig(ConfigurationSection section) {
			String key = section.getName().toUpperCase();
			ParticleInfo particle = new ParticleInfo();

			particle.type = validate(key, Particle.SMOKE, x -> Particle.valueOf(key));
			particle.count = section.getInt("count", 10);
			particle.speed = section.getDouble("speed", 0.01);

			var boxSize = section.getDoubleList("box-size");
			particle.bx = validate(0, 0.1, x -> boxSize.get(x));
			particle.by = validate(1, 0.1, x -> boxSize.get(x));
			particle.bz = validate(2, 0.1, x -> boxSize.get(x));

			return particle;
		}
	}

	public static class SoundInfo {
		public Sound type;
		public float volume;
		public float pitch;

		public static SoundInfo fromConfig(ConfigurationSection section) {
			SoundInfo sound = new SoundInfo();
			sound.type = validate(section.getString("type", "None"),
					Sound.BLOCK_ANVIL_HIT, x -> Sound.valueOf(x.toUpperCase()));
			sound.volume = (float) section.getDouble("volume", 1.0);
			sound.pitch = (float) section.getDouble("pitch", 1.0);

			return sound;
		}
	}

}
