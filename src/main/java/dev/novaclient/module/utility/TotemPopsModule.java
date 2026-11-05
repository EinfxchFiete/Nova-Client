package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.util.Notifications;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Counts how many totems each player used (the totem animation everybody sees) and tells you about it. */
public class TotemPopsModule extends Module {
	private static TotemPopsModule instance;
	public final BoolSetting chat = add(new BoolSetting("Im Chat anzeigen", false));
	public final BoolSetting self = add(new BoolSetting("Auch eigene Totems", true));
	private final Map<UUID, Integer> pops = new HashMap<>();

	public TotemPopsModule() {
		super("Totem-Pops", "Zählt, wie viele Totems jeder Spieler verbraucht hat, und zeigt eine Meldung", Category.UTILITY, false);
		instance = this;
	}

	public static TotemPopsModule get() {
		return instance;
	}

	/** Called when an entity shows the totem animation. */
	public static void onTotem(Entity entity) {
		TotemPopsModule m = instance;
		if (m == null || !m.isEnabled() || !(entity instanceof Player p)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		boolean me = p == mc.player;
		if (me && !m.self.isOn()) {
			return;
		}
		int n = m.pops.merge(p.getUUID(), 1, Integer::sum);
		String text = (me ? "Du hast" : p.getName().getString() + " hat") + " ein Totem verbraucht (" + n + ")";
		Notifications.push(text, 0xFFFFD54F);
		if (m.chat.isOn() && mc.gui != null) {
			mc.gui.hud.getChat().addClientSystemMessage(Component.literal("§6[Nova] §e" + text));
		}
	}

	public int pops(UUID uuid) {
		return pops.getOrDefault(uuid, 0);
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			pops.clear();
			return;
		}
		// a player who died starts at 0 again
		pops.keySet().removeIf(uuid -> {
			Player p = mc.level.getPlayerByUUID(uuid);
			return p != null && p.isDeadOrDying();
		});
	}
}
