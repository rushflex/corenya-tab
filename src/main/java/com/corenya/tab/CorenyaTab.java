package com.corenya.tab;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CorenyaTab - a tab list design for every Spigot/Paper/Folia version from
 * 1.8 to 1.21.11.
 */
public final class CorenyaTab extends JavaPlugin {

	private long startedAt;
	private TabSettings settings;
	private StatsStore store;
	private Hooks hooks;
	private TabRenderer renderer;
	private TabService service;

	@Override
	public void onEnable() {
		startedAt = System.currentTimeMillis();
		if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
			getLogger().warning("Veri klasoru olusturulamadi: " + getDataFolder().getAbsolutePath());
		}
		saveDefaultConfig();

		settings = new TabSettings(this);
		hooks = new Hooks(this);
		store = new StatsStore(this);
		renderer = new TabRenderer(this, settings, hooks, store);
		service = new TabService(this, settings, store, renderer);

		PluginCommand command = getCommand("corenyatab");
		if (command != null) {
			TabCommand executor = new TabCommand(this, service, settings);
			command.setExecutor(executor);
			command.setTabCompleter(executor);
		}

		getServer().getPluginManager().registerEvents(service, this);
		service.start();

		getLogger().info("========================================");
		getLogger().info(" CorenyaTab v" + getDescription().getVersion());
		getLogger().info(" Platform: " + (Scheduling.isFolia() ? "Folia" : "Spigot/Paper"));
		getLogger().info(" Onek: " + ChatColor.stripColor(
				ChatColor.translateAlternateColorCodes('&', settings.prefix())));
		if (!service.senderAvailable()) {
			getLogger().warning(" Bu sunucu surumunde tablist basligi gonderilemiyor.");
		}
		getLogger().info("========================================");
	}

	@Override
	public void onDisable() {
		if (service != null) {
			service.stop();
		}
	}

	public long uptime() {
		return System.currentTimeMillis() - startedAt;
	}

	public TabSettings settings() {
		return settings;
	}

	public StatsStore store() {
		return store;
	}

	public Hooks hooks() {
		return hooks;
	}

	public TabRenderer renderer() {
		return renderer;
	}
}
