package com.corenya.tab;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight per-player statistics used by the tab list. Only values that
 * cannot be read from the server API are stored here: playtime, kills and
 * deaths. Money/elo/gems/votes come from PlaceholderAPI or the config.
 */
final class StatsStore {

	private static final class Entry {
		private String name;
		private long playtime;
		private int kills;
		private int deaths;
	}

	private final CorenyaTab plugin;
	private final File file;
	private final Map<UUID, Entry> entries = new HashMap<>();
	private final Map<UUID, Long> sessions = new HashMap<>();
	private volatile boolean saving;

	StatsStore(CorenyaTab plugin) {
		this.plugin = plugin;
		this.file = new File(plugin.getDataFolder(), "stats.yml");
	}

	synchronized void load() {
		entries.clear();
		YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
		ConfigurationSection section = data.getConfigurationSection("players");
		if (section == null) {
			return;
		}
		for (String key : section.getKeys(false)) {
			try {
				UUID uniqueId = UUID.fromString(key);
				ConfigurationSection player = section.getConfigurationSection(key);
				if (player == null) {
					continue;
				}
				Entry entry = new Entry();
				entry.name = player.getString("name", key);
				entry.playtime = Math.max(0L, player.getLong("playtime", 0L));
				entry.kills = Math.max(0, player.getInt("kills", 0));
				entry.deaths = Math.max(0, player.getInt("deaths", 0));
				entries.put(uniqueId, entry);
			} catch (IllegalArgumentException ignored) {
				// Malformed UUID, skip the record.
			}
		}
	}

	synchronized void startSession(Player player) {
		Entry entry = entry(player);
		entry.name = player.getName();
		if (!sessions.containsKey(player.getUniqueId())) {
			sessions.put(player.getUniqueId(), System.currentTimeMillis());
		}
	}

	synchronized void stopSession(UUID uniqueId) {
		Long started = sessions.remove(uniqueId);
		if (started != null) {
			Entry entry = entries.get(uniqueId);
			if (entry != null) {
				entry.playtime += Math.max(0L, System.currentTimeMillis() - started);
			}
		}
	}

	synchronized long playtime(UUID uniqueId) {
		Entry entry = entries.get(uniqueId);
		long total = entry == null ? 0L : entry.playtime;
		Long started = sessions.get(uniqueId);
		if (started != null) {
			total += Math.max(0L, System.currentTimeMillis() - started);
		}
		return total;
	}

	synchronized int kills(UUID uniqueId) {
		Entry entry = entries.get(uniqueId);
		return entry == null ? 0 : entry.kills;
	}

	synchronized int deaths(UUID uniqueId) {
		Entry entry = entries.get(uniqueId);
		return entry == null ? 0 : entry.deaths;
	}

	synchronized void addKill(Player player) {
		entry(player).kills++;
	}

	synchronized void addDeath(Player player) {
		entry(player).deaths++;
	}

	synchronized void save() {
		if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
			plugin.getLogger().warning("Veri klasoru olusturulamadi: " + plugin.getDataFolder().getAbsolutePath());
			return;
		}
		YamlConfiguration data = new YamlConfiguration();
		long now = System.currentTimeMillis();
		for (Map.Entry<UUID, Entry> item : entries.entrySet()) {
			Entry entry = item.getValue();
			String base = "players." + item.getKey() + ".";
			Long started = sessions.get(item.getKey());
			long total = entry.playtime + (started == null ? 0L : Math.max(0L, now - started));
			data.set(base + "name", entry.name);
			data.set(base + "playtime", total);
			data.set(base + "kills", entry.kills);
			data.set(base + "deaths", entry.deaths);
		}
		try {
			data.save(file);
		} catch (Exception exception) {
			plugin.getLogger().warning("stats.yml yazilamadi: " + exception.getMessage());
		}
	}

	void saveAsync() {
		if (saving) {
			return;
		}
		saving = true;
		Scheduling.runAsync(plugin, () -> {
			try {
				save();
			} finally {
				saving = false;
			}
		});
	}

	private Entry entry(Player player) {
		Entry entry = entries.get(player.getUniqueId());
		if (entry == null) {
			entry = new Entry();
			entry.name = player.getName();
			entries.put(player.getUniqueId(), entry);
		}
		return entry;
	}
}
