package com.corenya.tab;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/**
 * Builds the per-player header/footer text from config.yml.
 */
final class TabRenderer {

	private static final Method PING = pingMethod();

	private final CorenyaTab plugin;
	private final TabSettings settings;
	private final Hooks hooks;
	private final StatsStore store;

	TabRenderer(CorenyaTab plugin, TabSettings settings, Hooks hooks, StatsStore store) {
		this.plugin = plugin;
		this.settings = settings;
		this.hooks = hooks;
		this.store = store;
	}

	String header(Player player) {
		return render(player, settings.header());
	}

	String footer(Player player) {
		return render(player, settings.footer());
	}

	private String render(Player player, List<String> lines) {
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < lines.size(); index++) {
			if (index > 0) {
				builder.append('\n');
			}
			builder.append(apply(player, lines.get(index)));
		}
		return ChatColor.translateAlternateColorCodes('&', builder.toString());
	}

	private String apply(Player player, String line) {
		if (line == null) {
			return "";
		}
		String result = expandPlaceholderApi(player, line);
		result = result.replace("{prefix}", settings.prefix());
		result = result.replace("{player}", player.getName());
		result = result.replace("{displayname}", player.getDisplayName());
		result = result.replace("{world}", player.getWorld().getName());
		result = result.replace("{ping}", ping(player));
		result = result.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
		result = result.replace("{max}", String.valueOf(Bukkit.getMaxPlayers()));
		result = result.replace("{date}", LocalDate.now().format(settings.dateFormatter()));
		result = result.replace("{time}", LocalTime.now().format(settings.timeFormatter()));
		result = result.replace("{uptime}", formatDuration(plugin.uptime()));
		result = result.replace("{location}", settings.location());
		result = result.replace("{website}", settings.website());
		result = result.replace("{money}", money(player));
		result = result.replace("{elo}", value(player, "elo"));
		result = result.replace("{gems}", value(player, "gems"));
		result = result.replace("{votes}", votes(player));
		result = result.replace("{team}", team(player));
		result = result.replace("{kills}", statValue(player, "kills"));
		result = result.replace("{deaths}", statValue(player, "deaths"));
		result = result.replace("{playtime}", formatDuration(store.playtime(player.getUniqueId())));
		return result;
	}

	private String expandPlaceholderApi(Player player, String input) {
		String result = input;
		int start;
		while ((start = result.indexOf("{papi:")) >= 0) {
			int end = result.indexOf('}', start);
			if (end < 0) {
				break;
			}
			String raw = result.substring(start + 6, end);
			String value = hooks.placeholder(player, raw);
			result = result.substring(0, start) + (value == null ? "" : value) + result.substring(end + 1);
		}
		return result;
	}

	private String value(Player player, String key) {
		String placeholder = settings.placeholder(key);
		if (!placeholder.isEmpty()) {
			String resolved = hooks.placeholder(player, placeholder);
			if (resolved != null) {
				return resolved;
			}
		}
		return settings.fallback(key);
	}

	private String statValue(Player player, String key) {
		String placeholder = settings.placeholder(key);
		if (!placeholder.isEmpty()) {
			String resolved = hooks.placeholder(player, placeholder);
			if (resolved != null) {
				return resolved;
			}
		}
		if ("kills".equals(key)) {
			return String.valueOf(store.kills(player.getUniqueId()));
		}
		return String.valueOf(store.deaths(player.getUniqueId()));
	}

	private String money(Player player) {
		String placeholder = settings.placeholder("money");
		if (!placeholder.isEmpty()) {
			String resolved = hooks.placeholder(player, placeholder);
			if (resolved != null) {
				return resolved;
			}
		}
		Double balance = hooks.balance(player);
		if (balance != null) {
			return formatMoney(balance);
		}
		return settings.fallback("money");
	}

	private String votes(Player player) {
		String value = value(player, "votes");
		if (settings.votesMax() > 0 && value.matches("\\d+")) {
			return value + "/" + settings.votesMax();
		}
		return value;
	}

	private String team(Player player) {
		String placeholder = settings.placeholder("team");
		if (!placeholder.isEmpty()) {
			String resolved = hooks.placeholder(player, placeholder);
			if (resolved != null) {
				return resolved;
			}
		}
		try {
			Team team = player.getScoreboard().getEntryTeam(player.getName());
			if (team != null) {
				return team.getDisplayName();
			}
		} catch (Throwable ignored) {
			// Custom scoreboards may not support entry lookup.
		}
		return settings.fallback("team");
	}

	private String ping(Player player) {
		if (PING == null) {
			return "-";
		}
		try {
			Object value = PING.invoke(player);
			return value instanceof Number ? ((Number) value).intValue() + "ms" : "-";
		} catch (Throwable throwable) {
			return "-";
		}
	}

	private String formatMoney(double amount) {
		String symbol = settings.moneySymbol();
		if (!settings.compactNumbers()) {
			return symbol + String.format(Locale.US, "%.2f", amount);
		}
		double absolute = Math.abs(amount);
		if (absolute >= 1_000_000_000D) {
			return symbol + trim(amount / 1_000_000_000D) + "B";
		}
		if (absolute >= 1_000_000D) {
			return symbol + trim(amount / 1_000_000D) + "M";
		}
		if (absolute >= 1_000D) {
			return symbol + trim(amount / 1_000D) + "K";
		}
		return symbol + trim(amount);
	}

	private String trim(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.05D) {
			return String.format(Locale.US, "%.0f", value);
		}
		return String.format(Locale.US, "%.1f", value);
	}

	private String formatDuration(long millis) {
		long minutes = Math.max(0L, millis) / 60000L;
		if (minutes <= 0L) {
			return "0m";
		}
		long days = minutes / 1440L;
		long hours = (minutes % 1440L) / 60L;
		long remaining = minutes % 60L;
		StringBuilder builder = new StringBuilder();
		if (days > 0L) {
			builder.append(days).append("d ");
		}
		if (hours > 0L) {
			builder.append(hours).append("h ");
		}
		if (days == 0L) {
			builder.append(remaining).append("m");
		}
		return builder.toString().trim();
	}

	private static Method pingMethod() {
		try {
			return Player.class.getMethod("getPing");
		} catch (Throwable throwable) {
			return null;
		}
	}
}
