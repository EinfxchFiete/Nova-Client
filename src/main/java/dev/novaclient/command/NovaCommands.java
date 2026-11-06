package dev.novaclient.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.novaclient.module.hud.LyricsModule;
import dev.novaclient.module.hud.MusicModule;
import dev.novaclient.music.Lyrics;
import dev.novaclient.music.MusicData;
import dev.novaclient.music.NowPlaying;
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

/** Client commands of Nova Client: /playtime, /wegpunkt, /song and /songtext. They run only on your PC, the server never sees them. */
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

	private static NowPlaying.@org.jspecify.annotations.Nullable Song song(FabricClientCommandSource src) {
		NowPlaying.Song s = NowPlaying.current();
		if (s == null) {
			src.sendError(Component.literal("Gerade läuft kein Song (Modul \"Musik\" oder \"Songtext\" an?)."));
		}
		return s;
	}

	private static int songtextInfo(FabricClientCommandSource src) {
		NowPlaying.Song s = song(src);
		if (s == null) {
			return 1;
		}
		String info = Lyrics.info(s);
		info(src, "Songtext zu \"" + s.title() + "\": " + switch (Lyrics.state(s)) {
			case LOADING -> "wird geladen";
			case READY -> info.isEmpty() ? "da" : info;
			case PLAIN_ONLY -> "nur ohne Zeiten gefunden";
			case MISSING -> "keiner gefunden";
		});
		info(src, "Versatz: " + LyricsModule.describeOffset(MusicData.offset(Lyrics.key(s)))
			+ ". /songtext früher|später [Sekunden], anders, neu, aus, an, zurücksetzen");
		return 1;
	}

	private static int nudge(FabricClientCommandSource src, double seconds) {
		NowPlaying.Song s = song(src);
		if (s != null) {
			LyricsModule.nudge(s, Math.round(seconds * 1000));
			info(src, "Songtext jetzt " + LyricsModule.describeOffset(MusicData.offset(Lyrics.key(s))) + " (nur dieser Song).");
		}
		return 1;
	}

	private static int hide(FabricClientCommandSource src, boolean hidden) {
		NowPlaying.Song s = song(src);
		if (s != null) {
			MusicData.setHidden(Lyrics.key(s), hidden);
			info(src, hidden ? "Songtext für \"" + s.title() + "\" ausgeblendet." : "Songtext für \"" + s.title() + "\" wieder an.");
		}
		return 1;
	}

	private static void registerAll(CommandDispatcher<FabricClientCommandSource> d) {
		d.register(ClientCommands.literal("song").executes(c -> {
			NowPlaying.Song s = NowPlaying.current();
			if (s == null) {
				info(c.getSource(), "Gerade läuft kein Song" + (MusicData.recent().isEmpty() ? "." : " (zuletzt: "
					+ MusicData.recent().get(0).title() + ")."));
				return 1;
			}
			info(c.getSource(), "♪ " + MusicModule.format(s) + (s.hasTimeline() ? " · " + s.timestamp() : "")
				+ (s.playing() ? "" : " (pausiert)") + (s.source().isEmpty() ? "" : " · " + s.source()));
			return 1;
		}));
		d.register(ClientCommands.literal("songtext")
			.executes(c -> songtextInfo(c.getSource()))
			.then(ClientCommands.literal("früher").executes(c -> nudge(c.getSource(), 0.5))
				.then(ClientCommands.argument("sekunden", DoubleArgumentType.doubleArg(0, 30))
					.executes(c -> nudge(c.getSource(), DoubleArgumentType.getDouble(c, "sekunden")))))
			.then(ClientCommands.literal("später").executes(c -> nudge(c.getSource(), -0.5))
				.then(ClientCommands.argument("sekunden", DoubleArgumentType.doubleArg(0, 30))
					.executes(c -> nudge(c.getSource(), -DoubleArgumentType.getDouble(c, "sekunden")))))
			.then(ClientCommands.literal("zurücksetzen").executes(c -> {
				NowPlaying.Song s = song(c.getSource());
				if (s != null) {
					MusicData.setOffset(Lyrics.key(s), 0);
					info(c.getSource(), "Versatz für \"" + s.title() + "\" zurückgesetzt.");
				}
				return 1;
			}))
			.then(ClientCommands.literal("anders").executes(c -> {
				NowPlaying.Song s = song(c.getSource());
				if (s != null) {
					info(c.getSource(), Lyrics.next(s));
				}
				return 1;
			}))
			.then(ClientCommands.literal("neu").executes(c -> {
				NowPlaying.Song s = song(c.getSource());
				if (s != null) {
					Lyrics.reload(s);
					info(c.getSource(), "Songtext für \"" + s.title() + "\" wird neu gesucht.");
				}
				return 1;
			}))
			.then(ClientCommands.literal("aus").executes(c -> hide(c.getSource(), true)))
			.then(ClientCommands.literal("an").executes(c -> hide(c.getSource(), false))));
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
