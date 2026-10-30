package com.corenya.tab;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /corenyatab (alias /ctab) administrative command.
 */
public final class TabCommand implements CommandExecutor, TabCompleter {

	private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "toggle", "preview", "info");

	private final CorenyaTab plugin;
	private final TabService service;
	private final TabSettings settings;

	TabCommand(CorenyaTab plugin, TabService service, TabSettings settings) {
		this.plugin = plugin;
		this.service = service;
		this.settings = settings;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
		if ("reload".equals(sub)) {
			return reload(sender);
		}
		if ("toggle".equals(sub)) {
			return toggle(sender);
		}
		if ("preview".equals(sub)) {
			return preview(sender);
		}
		if ("info".equals(sub)) {
			return info(sender);
		}
		return help(sender);
	}

	private boolean reload(CommandSender sender) {
		if (!sender.hasPermission("corenyatab.admin")) {
			send(sender, "&cBu komut icin yetkin yok.");
			return true;
		}
		plugin.reloadConfig();
		service.reload();
		send(sender, "&aTablist yeniden yuklendi.");
		return true;
	}

	private boolean toggle(CommandSender sender) {
		if (!sender.hasPermission("corenyatab.admin")) {
			send(sender, "&cBu komut icin yetkin yok.");
			return true;
		}
		boolean enabled = plugin.getConfig().getBoolean("enabled", true);
		plugin.getConfig().set("enabled", !enabled);
		plugin.saveConfig();
		service.reload();
		send(sender, !enabled ? "&aTablist acildi." : "&cTablist kapatildi.");
		return true;
	}

	private boolean preview(CommandSender sender) {
		if (!(sender instanceof Player)) {
			send(sender, "&cBu komut sadece oyun icinden kullanilabilir.");
			return true;
		}
		Player player = (Player) sender;
		send(sender, "&8&m--------&r &bCorenyaTab &7Onizleme &8&m--------");
		player.sendMessage(service.header(player));
		player.sendMessage(service.footer(player));
		return true;
	}

	private boolean info(CommandSender sender) {
		send(sender, "&8&m--------&r &bCorenyaTab &8&m--------");
		send(sender, "&7Surum: &f" + plugin.getDescription().getVersion());
		send(sender, "&7Platform: &f" + (Scheduling.isFolia() ? "Folia" : "Spigot/Paper"));
		send(sender, "&7Durum: " + (settings.enabled() ? "&aacik" : "&ckapali"));
		send(sender, "&7Onek: &r" + settings.prefix());
		send(sender, "&7Guncelleme: &f" + settings.updateTicks() + " tick");
		send(sender, "&7PlaceholderAPI: " + (plugin.hooks().hasPlaceholderApi() ? "&avar" : "&cyok"));
		send(sender, "&7Vault: " + (plugin.hooks().hasVault() ? "&a var" : "&cyok"));
		return true;
	}

	private boolean help(CommandSender sender) {
		send(sender, "&8&m--------&r &bCorenyaTab &7Komutlar &8&m--------");
		send(sender, "&b/corenyatab reload &8- &7Ayarlari yeniden yukler");
		send(sender, "&b/corenyatab toggle &8- &7Tablisti acar/kapatir");
		send(sender, "&b/corenyatab preview &8- &7Kendi tablistini onizler");
		send(sender, "&b/corenyatab info &8- &7Durum bilgisini gosterir");
		return true;
	}

	private void send(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			List<String> result = new ArrayList<>();
			String token = args[0].toLowerCase(Locale.ROOT);
			for (String sub : SUBCOMMANDS) {
				if (sub.startsWith(token)) {
					result.add(sub);
				}
			}
			return result;
		}
		return new ArrayList<>();
	}
}
