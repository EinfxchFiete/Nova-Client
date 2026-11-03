package dev.novaclient.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.world.Playtime;
import dev.novaclient.world.Waypoints;
import dev.novaclient.world.WorldId;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Client commands of Nova Client: /playtime and /wegpunkt. They run only on your PC, the server never sees them. */
public final class NovaCommands {
	private NovaCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> registerAll(dispatcher));
	}

	private static MutableComponent prefix() {
		return Component.literal("[Nova] ").withColor(ThemeModule.accent() & 0xFFFFFF);
	}

	private static void info(FabricClientCommandSource src, String text) {
		src.sendFeedback(prefix().append(Component.literal(text).withColor(0xFFFFFF)));
	}

	private static void registerAll(CommandDispatcher<FabricClientCommandSource> d) {
		d.register(ClientCommands.literal("playtime")
			.executes(c -> {
				Minecraft mc = c.getSource().getClient();
				info(c.getSource(), "Spielzeit auf " + WorldId.displayName(mc) + ": " + Playtime.format(Playtime.currentTotal())
					+ " (diese Sitzung: " + Playtime.format(Playtime.currentSession()) + ")");
				return 1;
			})
			.then(ClientCommands.literal("alle").executes(c -> {
				List<Map.Entry<String, Long>> all = Playtime.all();
				if (all.isEmpty()) {
					info(c.getSource(), "Noch keine Spielzeit gespeichert.");
					return 1;
				}
				info(c.getSource(), "Spielzeit in allen Welten und auf allen Servern:");
				int shown = 0;
				for (Map.Entry<String, Long> e : all) {
					String type = e.getKey().startsWith("sp:") ? "Welt" : "Server";
					c.getSource().sendFeedback(Component.literal("  " + type + " " + WorldId.nameOfKey(e.getKey()) + ": " + Playtime.format(e.getValue())));
					if (++shown >= 15) {
						break;
					}
				}
				return 1;
			})));

		d.register(ClientCommands.literal("wegpunkt")
			.executes(c -> list(c.getSource()))
			.then(ClientCommands.literal("liste").executes(c -> list(c.getSource())))
			.then(ClientCommands.literal("setzen")
				.executes(c -> add(c.getSource(), null))
				.then(ClientCommands.argument("name", StringArgumentType.greedyString())
					.executes(c -> add(c.getSource(), StringArgumentType.getString(c, "name")))))
			.then(ClientCommands.literal("entfernen")
				.executes(c -> {
					Minecraft mc = c.getSource().getClient();
					var w = Waypoints.nearest(mc);
					if (w == null) {
						c.getSource().sendError(Component.literal("Hier gibt es keinen Wegpunkt."));
						return 0;
					}
					Waypoints.remove(mc, w.name());
					info(c.getSource(), w.name() + " entfernt.");
					return 1;
				})
				.then(ClientCommands.argument("name", StringArgumentType.greedyString())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(Waypoints.inWorld(c.getSource().getClient()).stream().map(Waypoints.Waypoint::name), b))
					.executes(c -> {
						String name = StringArgumentType.getString(c, "name");
						if (Waypoints.remove(c.getSource().getClient(), name)) {
							info(c.getSource(), name + " entfernt.");
							return 1;
						}
						c.getSource().sendError(Component.literal("Kein Wegpunkt mit dem Namen \"" + name + "\"."));
						return 0;
					})))
			.then(ClientCommands.literal("alleentfernen").executes(c -> {
				int n = Waypoints.clear(c.getSource().getClient());
				info(c.getSource(), n + " Wegpunkt(e) in dieser Welt entfernt.");
				return 1;
			})));
	}

	private static int add(FabricClientCommandSource src, String name) {
		var w = Waypoints.add(src.getClient(), name);
		if (w == null) {
			src.sendError(Component.literal("Du bist in keiner Welt."));
			return 0;
		}
		info(src, w.name() + " gesetzt: " + w.x() + ", " + w.y() + ", " + w.z());
		return 1;
	}

	private static int list(FabricClientCommandSource src) {
		Minecraft mc = src.getClient();
		var all = Waypoints.inWorld(mc);
		if (all.isEmpty()) {
			info(src, "Keine Wegpunkte in dieser Welt. Setzen: Taste B oder /wegpunkt setzen <Name>");
			return 1;
		}
		info(src, all.size() + " Wegpunkt(e) in " + WorldId.displayName(mc) + ":");
		for (var w : all) {
			String dim = w.dimension().replace("minecraft:", "");
			String dist = mc.player != null && w.dimension().equals(mc.player.level().dimension().identifier().toString())
				? " · " + Math.round(w.distanceTo(mc.player)) + " m" : "";
			src.sendFeedback(Component.literal("  " + w.name() + ": " + w.x() + ", " + w.y() + ", " + w.z() + " (" + dim + ")" + dist).withColor(w.color() & 0xFFFFFF));
		}
		info(src, "Entfernen: /wegpunkt entfernen <Name>");
		return 1;
	}
}
