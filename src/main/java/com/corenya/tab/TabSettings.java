package com.corenya.tab;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable-by-reload snapshot of config.yml.
 */
final class TabSettings {

	private static final DateTimeFormatter DEFAULT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter DEFAULT_TIME = DateTimeFormatter.ofPattern("HH:mm");

	private final CorenyaTab plugin;
	private final Map<String, String> placeholders = new HashMap<>();
	private final Map<String, String> fallbacks = new HashMap<>();

	private boolean enabled;
	private String prefix;
	private long updateTicks;
	private long saveMinutes;
	private String location;
	private String website;
	private String moneySymbol;
	private boolean compactNumbers;
	private int votesMax;
	private DateTimeFormatter dateFormatter = DEFAULT_DATE;
	private DateTimeFormatter timeFormatter = DEFAULT_TIME;
	private List<String> header = Collections.emptyList();
	private List<String> footer = Collections.emptyList();

	TabSettings(CorenyaTab plugin) {
		this.plugin = plugin;
		reload();
	}

	void reload() {
		FileConfiguration config = plugin.getConfig();

		enabled = config.getBoolean("enabled", true);
		prefix = config.getString("prefix", "&b&lCorenyaVanilla");
		updateTicks = Math.max(5L, config.getLong("update-ticks", 20L));
		saveMinutes = Math.max(0L, config.getLong("save-minutes", 5L));

		location = config.getString("display.location", "ISTANBUL");
		website = config.getString("display.website", "corenyavanilla.com");
		moneySymbol = config.getString("display.money-symbol", "$");
		compactNumbers = config.getBoolean("display.compact-numbers", true);
		votesMax = Math.max(0, config.getInt("display.votes-max", 50));

		dateFormatter = formatter(config.getString("display.date-format", "dd/MM/yyyy"), DEFAULT_DATE);
		timeFormatter = formatter(config.getString("display.time-format", "HH:mm"), DEFAULT_TIME);

		placeholders.clear();
		fallbacks.clear();
		for (String key : new String[]{"money", "elo", "gems", "votes", "team", "kills", "deaths"}) {
			placeholders.put(key, text(config, "placeholders." + key));
			fallbacks.put(key, text(config, "fallback." + key));
		}

		header = new ArrayList<>(config.getStringList("header"));
		footer = new ArrayList<>(config.getStringList("footer"));
		if (header.isEmpty()) {
			header = Collections.singletonList("&b&l{prefix}");
		}
		if (footer.isEmpty()) {
			footer = Collections.singletonList("&7&o{website}");
		}
	}

	boolean enabled() {
		return enabled;
	}

	String prefix() {
		return prefix;
	}

	long updateTicks() {
		return updateTicks;
	}

	long saveMinutes() {
		return saveMinutes;
	}

	String location() {
		return location;
	}

	String website() {
		return website;
	}

	String moneySymbol() {
		return moneySymbol;
	}

	boolean compactNumbers() {
		return compactNumbers;
	}

	int votesMax() {
		return votesMax;
	}

	DateTimeFormatter dateFormatter() {
		return dateFormatter;
	}

	DateTimeFormatter timeFormatter() {
		return timeFormatter;
	}

	List<String> header() {
		return header;
	}

	List<String> footer() {
		return footer;
	}

	String placeholder(String key) {
		String value = placeholders.get(key);
		return value == null ? "" : value;
	}

	String fallback(String key) {
		String value = fallbacks.get(key);
		return value == null ? "" : value;
	}

	private static String text(FileConfiguration config, String path) {
		String value = config.getString(path);
		return value == null ? "" : value.trim();
	}

	private static DateTimeFormatter formatter(String pattern, DateTimeFormatter fallback) {
		try {
			return DateTimeFormatter.ofPattern(pattern);
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}
}
