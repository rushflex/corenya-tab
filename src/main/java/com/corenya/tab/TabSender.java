package com.corenya.tab;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Sends the tab list header/footer on every server version.
 *
 * <ul>
 *   <li>1.13+ &ndash; {@code Player#setPlayerListHeaderFooter(String, String)}</li>
 *   <li>1.8 - 1.12 &ndash; the header/footer packet through a tiny NMS hook</li>
 * </ul>
 *
 * Everything is resolved through reflection, which is why the jar itself can be
 * compiled against the 1.8.8 API and still run on 1.21.11.
 */
final class TabSender {

	private final CorenyaTab plugin;

	private Method stringMethod;
	private Method headerMethod;
	private Method footerMethod;

	private Constructor<?> packetConstructor;
	private Constructor<?> textConstructor;
	private Field packetHeader;
	private Field packetFooter;
	private Method getHandle;
	private Field connection;
	private Method sendPacket;

	TabSender(CorenyaTab plugin) {
		this.plugin = plugin;
		setupApi();
		if (stringMethod == null) {
			setupLegacy();
		}
	}

	boolean available() {
		return stringMethod != null || (headerMethod != null && footerMethod != null) || packetConstructor != null;
	}

	void send(Player player, String header, String footer) {
		if (stringMethod != null) {
			try {
				stringMethod.invoke(player, header, footer);
				return;
			} catch (Throwable ignored) {
				// Fall through to the next strategy.
			}
		}
		if (headerMethod != null && footerMethod != null) {
			try {
				headerMethod.invoke(player, header);
				footerMethod.invoke(player, footer);
				return;
			} catch (Throwable ignored) {
				// Fall through to the next strategy.
			}
		}
		if (packetConstructor != null) {
			sendLegacy(player, header, footer);
		}
	}

	private void setupApi() {
		try {
			stringMethod = Player.class.getMethod("setPlayerListHeaderFooter", String.class, String.class);
		} catch (Throwable ignored) {
			// Pre-1.13 server.
		}
		if (stringMethod == null) {
			try {
				headerMethod = Player.class.getMethod("setPlayerListHeader", String.class);
				footerMethod = Player.class.getMethod("setPlayerListFooter", String.class);
			} catch (Throwable ignored) {
				// Pre-1.13 server.
			}
		}
	}

	private void setupLegacy() {
		try {
			String serverPackage = Bukkit.getServer().getClass().getPackage().getName();
			String version = serverPackage.substring(serverPackage.lastIndexOf('.') + 1);
			if (!version.startsWith("v")) {
				return;
			}
			String nms = "net.minecraft.server." + version;

			Class<?> packetType = Class.forName(nms + ".PacketPlayOutPlayerListHeaderFooter");
			Class<?> chatType = Class.forName(nms + ".ChatComponentText");
			Class<?> packetBase = Class.forName(nms + ".Packet");
			Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");

			packetConstructor = packetType.getConstructor();
			textConstructor = chatType.getConstructor(String.class);
			packetHeader = field(packetType, "a", "header");
			packetFooter = field(packetType, "b", "footer");

			getHandle = craftPlayer.getMethod("getHandle");
			Class<?> handleType = getHandle.getReturnType();
			connection = field(handleType, "playerConnection", "connection");
			sendPacket = connection.getType().getMethod("sendPacket", packetBase);
		} catch (Throwable throwable) {
			plugin.getLogger().info("Eski surum tablist destegi kapali: " + throwable.getMessage());
			packetConstructor = null;
		}
	}

	private void sendLegacy(Player player, String header, String footer) {
		try {
			Object packet = packetConstructor.newInstance();
			packetHeader.set(packet, textConstructor.newInstance(header));
			packetFooter.set(packet, textConstructor.newInstance(footer));
			Object handle = getHandle.invoke(player);
			Object playerConnection = connection.get(handle);
			sendPacket.invoke(playerConnection, packet);
		} catch (Throwable throwable) {
			plugin.getLogger().warning("Tablist paketi gonderilemedi: " + throwable.getMessage());
		}
	}

	private static Field field(Class<?> type, String... names) throws NoSuchFieldException {
		for (String name : names) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException ignored) {
				// Try the next known name.
			}
		}
		throw new NoSuchFieldException(type.getName());
	}
}
