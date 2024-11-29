package com.thrallmaster;

import java.util.ArrayList;
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
	public static double THRALL_DETECTION_RANGE;
	public static double THRALL_DETECTION_MUL;
	public static int POTION_COUNT;
	public static String SPAWN_MESSAGE;
	public static String DEATH_MESSAGE;
	public static PotionEffectType POTION_TYPE;
	public static SoundInfo RITUAL_SOUND;
	public static SoundInfo SPAWN_SOUND;
	public static SoundInfo DEATH_SOUND;
	public static ArrayList<ParticleInfo> RITUAL_PARTICLES;
	public static ArrayList<ParticleInfo> SPAWN_PARTICLES;
	public static ArrayList<ParticleInfo> DEATH_PARTICLES;

	private Settings() {
	}

	public static void loadConfig(Plugin plugin) {
		FileConfiguration config = plugin.getConfig();

		loadGeneralSettings(config);
		loadRitualSettings(config);
		loadSpawnSettings(config);
		loadDeathSettings(config);
	}

	private static void loadGeneralSettings(FileConfiguration config) {
		var general = config.getConfigurationSection("general");
		THRALL_NAME = general.getString("spawn-name", "Thrall");
		THRALL_HEALTH = general.getInt("initial-health", 20);
		THRALL_MAX_HEALTH = general.getInt("max-health", 30);
		THRALL_DETECTION_RANGE = general.getDouble("detection-range", 10);
		THRALL_DETECTION_MUL = general.getDouble("ranged-detection-multiplier", 1.5);
	}

	private static void loadSpawnSettings(FileConfiguration config) {
		SPAWN_PARTICLES = new ArrayList<>();
		var thrallSpawn = config.getConfigurationSection("thrall-spawn");

		SPAWN_MESSAGE = thrallSpawn.getString("message", "");
		var spawnParticles = thrallSpawn.getConfigurationSection("particles");
		for (String key : spawnParticles.getKeys(false)) {
			ParticleInfo particle = ParticleInfo.fromConfig(spawnParticles.getConfigurationSection(key));
			SPAWN_PARTICLES.add(particle);
		}
		SPAWN_SOUND = SoundInfo.fromConfig(thrallSpawn.getConfigurationSection("sound"));
	}

	private static void loadDeathSettings(FileConfiguration config) {
		DEATH_PARTICLES = new ArrayList<>();
		var thrallDeath = config.getConfigurationSection("thrall-death");

		DEATH_MESSAGE = thrallDeath.getString("message", "");
		var deathParticles = thrallDeath.getConfigurationSection("particles");
		for (String key : deathParticles.getKeys(false)) {
			ParticleInfo particle = ParticleInfo.fromConfig(deathParticles.getConfigurationSection(key));
			DEATH_PARTICLES.add(particle);
		}
		DEATH_SOUND = SoundInfo.fromConfig(thrallDeath.getConfigurationSection("sound"));
	}

	@SuppressWarnings("deprecation")
	private static void loadRitualSettings(FileConfiguration config) {
		RITUAL_PARTICLES = new ArrayList<>();
		var ritualAction = config.getConfigurationSection("ritual-action");

		POTION_COUNT = ritualAction.getInt("potion-count", 4);
		POTION_TYPE = validate(ritualAction.getString("potion-type", "weakness"), PotionEffectType.WEAKNESS,
				x -> PotionEffectType.getByName(x), false);

		var ritualParticles = ritualAction.getConfigurationSection("particles");
		for (String key : ritualParticles.getKeys(false)) {
			ParticleInfo particle = ParticleInfo.fromConfig(ritualParticles.getConfigurationSection(key));
			RITUAL_PARTICLES.add(particle);
		}
		RITUAL_SOUND = SoundInfo.fromConfig(ritualAction.getConfigurationSection("sound"));
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
