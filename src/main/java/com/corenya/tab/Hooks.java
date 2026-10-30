package com.corenya.tab;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Optional soft hooks. Both PlaceholderAPI and Vault are resolved through
 * reflection so the plugin keeps loading (and working) when they are absent.
 */
final class Hooks {

	private Method placeholderMethod;
	private Object economy;
	private Method balanceMethod;

	Hooks(CorenyaTab plugin) {
		reload();
	}

	void reload() {
		placeholderMethod = null;
		economy = null;
		balanceMethod = null;

		try {
			Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
			placeholderMethod = api.getMethod("setPlaceholders", Player.class, String.class);
		} catch (Throwable ignored) {
			// PlaceholderAPI is not installed.
		}

		try {
			Class<?> economyType = Class.forName("net.milkbowl.vault.economy.Economy");
			Class<?> servicesType = Class.forName("org.bukkit.plugin.ServicesManager");
			Class<?> registrationType = Class.forName("org.bukkit.plugin.RegisteredServiceProvider");
			Method getRegistration = servicesType.getMethod("getRegistration", Class.class);
			Object registration = getRegistration.invoke(Bukkit.getServicesManager(), economyType);
			if (registration != null) {
				economy = registrationType.getMethod("getProvider").invoke(registration);
				try {
					balanceMethod = economyType.getMethod("getBalance", OfflinePlayer.class);
				} catch (NoSuchMethodException exception) {
					balanceMethod = economyType.getMethod("getBalance", String.class);
				}
			}
		} catch (Throwable ignored) {
			// Vault is not installed.
		}
	}

	String placeholder(Player player, String raw) {
		if (placeholderMethod == null || player == null || raw == null || raw.isEmpty()) {
			return null;
		}
		try {
			Object value = placeholderMethod.invoke(null, player, raw);
			return value instanceof String ? (String) value : null;
		} catch (Throwable throwable) {
			return null;
		}
	}

	Double balance(Player player) {
		if (economy == null || balanceMethod == null || player == null) {
			return null;
		}
		try {
			Object value;
			if (balanceMethod.getParameterTypes()[0] == OfflinePlayer.class) {
				value = balanceMethod.invoke(economy, player);
			} else {
				value = balanceMethod.invoke(economy, player.getName());
			}
			return value instanceof Number ? ((Number) value).doubleValue() : null;
		} catch (Throwable throwable) {
			return null;
		}
	}

	boolean hasPlaceholderApi() {
		return placeholderMethod != null;
	}

	boolean hasVault() {
		return economy != null && balanceMethod != null;
	}
}
