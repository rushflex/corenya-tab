package com.corenya.tab;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Scheduler abstraction that keeps one jar working on Spigot/Paper 1.8 - 1.21.11
 * and on Folia.
 *
 * <p>Folia's scheduler classes do not exist on older servers, so they are only
 * touched through reflection after {@link #FOLIA} has been detected. All public
 * methods fall back to the classic {@code BukkitScheduler} everywhere else.</p>
 */
final class Scheduling {

	interface Handle {
		void cancel();
	}

	private static final boolean FOLIA = exists("io.papermc.paper.threadedregions.RegionizedServer");

	private static Object globalScheduler;
	private static Object asyncScheduler;
	private static Method globalRunAtFixedRate;
	private static Method asyncRunNow;
	private static Method asyncRunAtFixedRate;
	private static Method entityGetScheduler;
	private static Method entityRun;
	private static Method scheduledCancel;

	private Scheduling() {
	}

	static {
		if (FOLIA) {
			try {
				Class<?> scheduledType = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
				scheduledCancel = scheduledType.getMethod("cancel");

				Class<?> globalType = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
				globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
				globalRunAtFixedRate = globalType.getMethod("runAtFixedRate", Plugin.class, Consumer.class,
						long.class, long.class);

				Class<?> asyncType = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
				asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
				asyncRunNow = asyncType.getMethod("runNow", Plugin.class, Consumer.class);
				asyncRunAtFixedRate = asyncType.getMethod("runAtFixedRate", Plugin.class, Consumer.class,
						long.class, long.class, TimeUnit.class);

				Class<?> entityType = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
				entityGetScheduler = Entity.class.getMethod("getScheduler");
				entityRun = entityType.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
			} catch (Throwable throwable) {
				Bukkit.getLogger().warning("[CorenyaTab] Folia scheduler hazirlanamadi: " + throwable.getMessage());
			}
		}
	}

	static boolean isFolia() {
		return FOLIA;
	}

	static Handle repeating(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
		if (FOLIA && globalRunAtFixedRate != null) {
			try {
				Object task = globalRunAtFixedRate.invoke(globalScheduler, plugin, consumer(runnable),
						Math.max(1L, delayTicks), Math.max(1L, periodTicks));
				return handle(task);
			} catch (Throwable throwable) {
				Bukkit.getLogger().warning("[CorenyaTab] Folia gorevi baslatilamadi: " + throwable.getMessage());
			}
		}
		BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable,
				Math.max(0L, delayTicks), Math.max(1L, periodTicks));
		return task::cancel;
	}

	static Handle asyncRepeating(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
		if (FOLIA && asyncRunAtFixedRate != null) {
			try {
				Object task = asyncRunAtFixedRate.invoke(asyncScheduler, plugin, consumer(runnable),
						Math.max(1L, delayTicks) * 50L, Math.max(1L, periodTicks) * 50L, TimeUnit.MILLISECONDS);
				return handle(task);
			} catch (Throwable throwable) {
				Bukkit.getLogger().warning("[CorenyaTab] Folia async gorevi baslatilamadi: " + throwable.getMessage());
			}
		}
		BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable,
				Math.max(0L, delayTicks), Math.max(1L, periodTicks));
		return task::cancel;
	}

	static void runAsync(Plugin plugin, Runnable runnable) {
		if (FOLIA && asyncRunNow != null) {
			try {
				asyncRunNow.invoke(asyncScheduler, plugin, consumer(runnable));
				return;
			} catch (Throwable ignored) {
				// Fall through to the classic scheduler below.
			}
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
	}

	/**
	 * Runs the action on the thread that owns the player (its region on Folia,
	 * the main thread on Paper/Spigot).
	 */
	static void runFor(Player player, Plugin plugin, Runnable runnable) {
		if (FOLIA && entityRun != null && player != null) {
			try {
				Object scheduler = entityGetScheduler.invoke(player);
				entityRun.invoke(scheduler, plugin, consumer(runnable), (Runnable) () -> {
				});
				return;
			} catch (Throwable ignored) {
				// Fall through to the classic scheduler below.
			}
		}
		if (Bukkit.isPrimaryThread()) {
			runnable.run();
		} else {
			Bukkit.getScheduler().runTask(plugin, runnable);
		}
	}

	private static Consumer<Object> consumer(Runnable runnable) {
		return task -> runnable.run();
	}

	private static Handle handle(Object task) {
		return () -> {
			try {
				scheduledCancel.invoke(task);
			} catch (Throwable ignored) {
				// Task already finished.
			}
		};
	}

	private static boolean exists(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException exception) {
			return false;
		}
	}
}
