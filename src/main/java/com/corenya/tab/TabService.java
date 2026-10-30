package com.corenya.tab;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the update task, the statistics session tracking and all tab related
 * events. Every player action is dispatched through {@link Scheduling} so the
 * module is Folia safe.
 */
final class TabService implements Listener {

	private final CorenyaTab plugin;
	private final TabSettings settings;
	private final StatsStore store;
	private final TabRenderer renderer;
	private final TabSender sender;

	private Scheduling.Handle updateTask;
	private Scheduling.Handle saveTask;

	TabService(CorenyaTab plugin, TabSettings settings, StatsStore store, TabRenderer renderer) {
		this.plugin = plugin;
		this.settings = settings;
		this.store = store;
		this.renderer = renderer;
		this.sender = new TabSender(plugin);
	}

	void start() {
		cancelTasks();
		store.load();
		for (Player player : online()) {
			store.startSession(player);
		}
		if (!settings.enabled()) {
			return;
		}
		updateTask = Scheduling.repeating(plugin, this::updateAll, 1L, settings.updateTicks());
		if (settings.saveMinutes() > 0L) {
			long ticks = settings.saveMinutes() * 60L * 20L;
			saveTask = Scheduling.repeating(plugin, store::saveAsync, ticks, ticks);
		}
	}

	void reload() {
		store.save();
		settings.reload();
		start();
	}

	void stop() {
		cancelTasks();
		for (Player player : online()) {
			store.stopSession(player.getUniqueId());
			try {
				sender.send(player, "", "");
			} catch (Throwable ignored) {
				// The player may already be disconnecting.
			}
		}
		store.save();
	}

	String header(Player player) {
		return renderer.header(player);
	}

	String footer(Player player) {
		return renderer.footer(player);
	}

	private void cancelTasks() {
		if (updateTask != null) {
			updateTask.cancel();
			updateTask = null;
		}
		if (saveTask != null) {
			saveTask.cancel();
			saveTask = null;
		}
	}

	private void updateAll() {
		for (Player player : online()) {
			Scheduling.runFor(player, plugin, () -> apply(player));
		}
	}

	private void apply(Player player) {
		if (!settings.enabled() || !player.isOnline()) {
			return;
		}
		sender.send(player, renderer.header(player), renderer.footer(player));
	}

	boolean senderAvailable() {
		return sender.available();
	}

	private List<Player> online() {
		return new ArrayList<>(Bukkit.getOnlinePlayers());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		store.startSession(player);
		if (settings.enabled()) {
			Scheduling.runFor(player, plugin, () -> apply(player));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		store.stopSession(event.getPlayer().getUniqueId());
		store.saveAsync();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDeath(PlayerDeathEvent event) {
		Player victim = event.getEntity();
		store.addDeath(victim);
		Player killer = victim.getKiller();
		if (killer != null && killer != victim) {
			store.addKill(killer);
		}
	}
}
