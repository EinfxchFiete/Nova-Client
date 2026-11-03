package dev.novaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.friends.Friends;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.module.utility.FriendsModule;
import dev.novaclient.module.utility.NetworkModule;
import dev.novaclient.network.NovaNetwork;
import dev.novaclient.util.Notifications;
import dev.novaclient.util.Render;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Friends: add by name or from the players on your server, requests, online status and joining a friend's server. */
public class FriendsScreen extends Screen {
	private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	private static final int ROW = 22;

	private final @Nullable Screen parent;
	private String input = "";
	private boolean inputFocused = true;
	private String message = "";
	private int messageColor = 0xFF9A9AAE;
	private boolean busy;
	private double scroll;

	private int left;
	private int top;
	private int winW;
	private int winH;

	private enum Kind { HEADER, FRIEND, REQUEST, NEARBY, HINT }

	/** One line of the list with up to two buttons on the right. */
	private record Row(Kind kind, @Nullable UUID uuid, String name, String status, int statusColor, String button1, String button2) {
	}

	public FriendsScreen(@Nullable Screen parent) {
		super(Component.literal("Freunde"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public void added() {
		super.added();
		minecraft.textInputManager().startTextInput(this);
		NovaNetwork.requestSync();
	}

	@Override
	public void removed() {
		minecraft.textInputManager().stopTextInput(this);
		super.removed();
	}

	@Override
	public void tick() {
		super.tick();
		minecraft.textInputManager().startTextInput(this);
	}

	private void layout() {
		winW = Math.min(420, width - 20);
		winH = Math.min(300, height - 20);
		left = (width - winW) / 2;
		top = (height - winH) / 2;
	}

	private int listTop() {
		return top + 52;
	}

	private int listBottom() {
		return top + winH - 18;
	}

	private int[] addButton() {
		int w = font.width("Hinzufügen") + 14;
		return new int[] {left + winW - 10 - w, top + 26, left + winW - 10, top + 42};
	}

	// ------------------------------------------------------------------ content

	private static String ago(long ms) {
		long min = ms / 60_000;
		if (min < 1) {
			return "gerade eben";
		}
		if (min < 60) {
			return "vor " + min + " Min.";
		}
		return "vor " + (min / 60) + " Std.";
	}

	private List<Row> rows() {
		List<Row> out = new ArrayList<>();
		List<Friends.Request> requests = Friends.requests();
		if (!requests.isEmpty()) {
			out.add(new Row(Kind.HEADER, null, "Anfragen (" + requests.size() + ")", "", 0, "", ""));
			for (Friends.Request r : requests) {
				out.add(new Row(Kind.REQUEST, r.uuid(), r.name(), "möchte dein Freund sein", 0xFFFFDD66, "Annehmen", "Ablehnen"));
			}
		}
		List<Friends.Friend> list = new ArrayList<>(Friends.list());
		// online first
		list.sort((a, b) -> Boolean.compare(online(b.uuid()), online(a.uuid())));
		out.add(new Row(Kind.HEADER, null, "Freunde (" + Friends.onlineCount() + " online / " + list.size() + ")", "", 0, "", ""));
		if (list.isEmpty()) {
			out.add(new Row(Kind.HINT, null, "Noch keine Freunde. Gib oben einen Namen ein oder füge unten jemanden hinzu.", "", 0, "", ""));
		}
		for (Friends.Friend f : list) {
			Friends.Status s = Friends.status(f.uuid());
			String status;
			int color;
			String join = "";
			if (s == null) {
				status = "Status unbekannt (Nova-Netzwerk)";
				color = 0xFF7C7C8E;
			} else if (!s.mutual()) {
				status = "Anfrage gesendet – wartet auf Bestätigung";
				color = 0xFF9A9AAE;
			} else if (s.online()) {
				status = "Online" + (s.server().isEmpty() ? "" : " · " + s.server());
				color = 0xFF55FF55;
				if (!s.server().isEmpty() && !s.server().equals("Einzelspieler") && !sameServer(s.server())) {
					join = "Beitreten";
				}
			} else {
				status = "Offline" + (s.lastSeenMs() >= 0 ? " · zuletzt " + ago(s.lastSeenMs()) : "");
				color = 0xFF9A9AAE;
			}
			out.add(new Row(Kind.FRIEND, f.uuid(), f.name(), status, color, join, "Entfernen"));
		}
		// quick add: players on this server
		List<Row> nearby = new ArrayList<>();
		if (minecraft.getConnection() != null && minecraft.player != null) {
			for (PlayerInfo info : minecraft.getConnection().getListedOnlinePlayers()) {
				UUID id = info.getProfile().id();
				if (!id.equals(minecraft.player.getUUID()) && !Friends.isFriend(id) && nearby.size() < 30) {
					boolean nova = NovaNetwork.isNovaUser(id);
					nearby.add(new Row(Kind.NEARBY, id, info.getProfile().name(), nova ? "spielt mit Nova Client" : "auf diesem Server",
						nova ? 0xFFB89CFF : 0xFF7C7C8E, "Hinzufügen", ""));
				}
			}
		}
		if (!nearby.isEmpty()) {
			out.add(new Row(Kind.HEADER, null, "Spieler auf diesem Server", "", 0, "", ""));
			out.addAll(nearby);
		}
		return out;
	}

	private static boolean online(UUID id) {
		Friends.Status s = Friends.status(id);
		return s != null && s.mutual() && s.online();
	}

	private boolean sameServer(String server) {
		ServerData current = minecraft.getCurrentServer();
		return current != null && current.ip.trim().equalsIgnoreCase(server);
	}

	private int rowHeight(Row r) {
		return r.kind() == Kind.HEADER ? 16 : r.kind() == Kind.HINT ? 14 : ROW;
	}

	private int contentHeight(List<Row> rows) {
		int h = 0;
		for (Row r : rows) {
			h += rowHeight(r) + 2;
		}
		return h;
	}

	private double maxScroll(List<Row> rows) {
		return Math.max(0, contentHeight(rows) - (listBottom() - listTop()));
	}

	/** Button rectangles of a row: {x0, y0, x1, y1} for button 1 and 2 (null if absent). */
	private int[][] buttons(Row r, int y) {
		int[][] out = new int[2][];
		int x = left + winW - 12;
		String[] labels = {r.button2(), r.button1()};
		for (int i = 0; i < 2; i++) {
			if (labels[i].isEmpty()) {
				continue;
			}
			int w = font.width(labels[i]) + 10;
			out[1 - i] = new int[] {x - w, y + 4, x, y + ROW - 4};
			x -= w + 4;
		}
		return out;
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		layout();
		int accent = ThemeModule.accent();
		Render.rounded(g, left, top, left + winW, top + winH, Render.alpha(0xFF0E0E14, ThemeModule.menuAlpha()));
		Render.roundedOutline(g, left, top, left + winW, top + winH, Render.alpha(accent, 0x70));
		g.text(font, "FREUNDE", left + 10, top + 8, 0xFFFFFFFF, false);
		g.text(font, "Freund hinzufügen: Name eingeben", left + 70, top + 8, 0xFF6C6C7E, false);

		// name field + add button
		int fx0 = left + 10;
		int[] ab = addButton();
		Render.rounded(g, fx0, top + 26, ab[0] - 6, top + 42, inputFocused ? 0xFF22222D : 0xFF16161E);
		if (inputFocused) {
			Render.roundedOutline(g, fx0, top + 26, ab[0] - 6, top + 42, Render.alpha(accent, 0xA0));
		}
		boolean blink = inputFocused && (System.currentTimeMillis() / 500) % 2 == 0;
		String shown = input.isEmpty() && !inputFocused ? "Minecraft-Name …" : input + (blink ? "_" : "");
		g.text(font, shown, fx0 + 6, top + 30, input.isEmpty() && !inputFocused ? 0xFF6C6C7E : 0xFFFFFFFF, false);
		boolean hoverAdd = Render.inside(mouseX, mouseY, ab[0], ab[1], ab[2], ab[3]);
		Render.rounded(g, ab[0], ab[1], ab[2], ab[3], busy ? 0xFF33333F : hoverAdd ? Render.mix(accent, 0xFFFFFFFF, 0.15F) : accent);
		g.centeredText(font, busy ? "…" : "Hinzufügen", (ab[0] + ab[2]) / 2, ab[1] + 4, 0xFFFFFFFF);

		List<Row> rows = rows();
		scroll = Math.max(0, Math.min(scroll, maxScroll(rows)));
		g.enableScissor(left + 4, listTop() - 2, left + winW - 4, listBottom());
		int y = listTop() - (int) scroll;
		for (Row r : rows) {
			int h = rowHeight(r);
			if (y + h >= listTop() - 2 && y <= listBottom()) {
				renderRow(g, r, y, mouseX, mouseY, accent);
			}
			y += h + 2;
		}
		g.disableScissor();

		String footer = message.isEmpty() ? networkHint() : message;
		g.text(font, font.plainSubstrByWidth(footer, winW - 20), left + 10, top + winH - 13, message.isEmpty() ? 0xFF6C6C7E : messageColor, false);
	}

	private String networkHint() {
		NetworkModule net = NetworkModule.get();
		FriendsModule fm = FriendsModule.get();
		if (fm == null || !fm.isEnabled()) {
			return "Das Modul „Freunde“ ist aus – schalte es unter Module → Utility ein.";
		}
		if (net == null || !net.isEnabled() || net.serverUrl().isEmpty()) {
			return "Nova-Netzwerk aus: Online-Status geht nur mit dem Nova-Netzwerk.";
		}
		return NovaNetwork.isConnected() ? "Freunde sehen sich, wenn beide sich hinzugefügt haben."
			: "Verbinde mit dem Nova-Netzwerk …";
	}

	private void renderRow(GuiGraphicsExtractor g, Row r, int y, int mouseX, int mouseY, int accent) {
		int x0 = left + 10;
		int x1 = left + winW - 10;
		switch (r.kind()) {
			case HEADER -> {
				g.text(font, r.name().toUpperCase(java.util.Locale.ROOT), x0, y + 5, 0xFF8E8EA4, false);
				g.fill(x0 + font.width(r.name().toUpperCase(java.util.Locale.ROOT)) + 6, y + 9, x1, y + 10, 0x22FFFFFF);
			}
			case HINT -> g.text(font, font.plainSubstrByWidth(r.name(), x1 - x0), x0, y + 3, 0xFF7C7C8E, false);
			default -> {
				boolean hover = Render.inside(mouseX, mouseY, x0, y, x1, y + ROW) && mouseY >= listTop() && mouseY < listBottom();
				Render.rounded(g, x0, y, x1, y + ROW, hover ? 0xFF20202A : 0xFF16161E);
				int dot = r.kind() == Kind.FRIEND ? (r.statusColor() == 0xFF55FF55 ? 0xFF55FF55 : 0xFF55556A) : r.kind() == Kind.REQUEST ? 0xFFFFDD66 : 0xFF3A3A48;
				Render.rounded(g, x0 + 6, y + 8, x0 + 12, y + 14, dot);
				g.text(font, r.name(), x0 + 18, y + 3, 0xFFFFFFFF, false);
				int[][] b = buttons(r, y);
				int textMax = (b[0] != null ? b[0][0] : x1) - x0 - 24;
				g.pose().pushMatrix();
				g.pose().translate(x0 + 18, y + 13);
				g.pose().scale(0.75F, 0.75F);
				g.text(font, font.plainSubstrByWidth(r.status(), (int) (textMax / 0.75F)), 0, 0, r.statusColor(), false);
				g.pose().popMatrix();
				String[] labels = {r.button1(), r.button2()};
				for (int i = 0; i < 2; i++) {
					if (b[i] == null) {
						continue;
					}
					boolean over = Render.inside(mouseX, mouseY, b[i][0], b[i][1], b[i][2], b[i][3]);
					boolean danger = labels[i].equals("Entfernen") || labels[i].equals("Ablehnen");
					int bg = danger ? (over ? 0xFF6A2A33 : 0xFF3A2026) : over ? Render.mix(accent, 0xFFFFFFFF, 0.15F) : Render.alpha(accent, 0xC0);
					Render.rounded(g, b[i][0], b[i][1], b[i][2], b[i][3], bg);
					g.centeredText(font, labels[i], (b[i][0] + b[i][2]) / 2, b[i][1] + 3, 0xFFFFFFFF);
				}
			}
		}
	}

	// ------------------------------------------------------------------ actions

	private void info(String text, boolean error) {
		message = text;
		messageColor = error ? 0xFFFF7777 : 0xFF77FF99;
	}

	private void addTyped() {
		String name = input.trim();
		if (name.isEmpty() || busy) {
			return;
		}
		if (!name.matches("[A-Za-z0-9_]{1,16}")) {
			info("Ungültiger Name: nur Buchstaben, Zahlen und _ (max. 16).", true);
			return;
		}
		busy = true;
		info("Suche " + name + " …", false);
		Friends.lookup(name, f -> {
			busy = false;
			if (f == null) {
				info("Spieler „" + name + "“ nicht gefunden (oder Mojang gerade nicht erreichbar).", true);
				return;
			}
			add(f.uuid(), f.name());
			input = "";
		});
	}

	private void add(UUID uuid, String name) {
		if (Friends.isFriend(uuid)) {
			info(name + " ist schon in deiner Liste.", true);
			return;
		}
		if (!Friends.add(uuid, name)) {
			info("Das geht nicht (du selbst oder Liste voll).", true);
			return;
		}
		info(name + " hinzugefügt – ihr seid Freunde, sobald " + name + " dich auch hinzufügt.", false);
		NovaNetwork.requestSync();
	}

	private void join(String server, String name) {
		var mc = minecraft;
		if (!ServerAddress.isValidAddress(server)) {
			info("Ungültige Server-Adresse: " + server, true);
			return;
		}
		Notifications.push("Verbinde mit " + server + " …", 0);
		boolean local = mc.isLocalServer();
		if (mc.level != null) {
			mc.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
			if (local) {
				mc.disconnectWithSavingScreen();
			} else {
				mc.disconnectWithProgressScreen();
			}
		}
		ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(server), new ServerData(name, server, ServerData.Type.OTHER), false, null);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		layout();
		double mx = event.x();
		double my = event.y();
		if (event.button() != LEFT) {
			return super.mouseClicked(event, doubleClick);
		}
		int[] ab = addButton();
		if (Render.inside(mx, my, ab[0], ab[1], ab[2], ab[3])) {
			addTyped();
			return true;
		}
		inputFocused = Render.inside(mx, my, left + 10, top + 26, ab[0] - 6, top + 42);
		if (my < listTop() || my >= listBottom()) {
			return super.mouseClicked(event, doubleClick);
		}
		int y = listTop() - (int) scroll;
		for (Row r : rows()) {
			int h = rowHeight(r);
			if (r.uuid() != null && my >= y && my < y + h) {
				int[][] b = buttons(r, y);
				for (int i = 0; i < 2; i++) {
					if (b[i] != null && Render.inside(mx, my, b[i][0], b[i][1], b[i][2], b[i][3])) {
						click(r, i == 0 ? r.button1() : r.button2());
						return true;
					}
				}
			}
			y += h + 2;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void click(Row r, String button) {
		switch (button) {
			case "Annehmen", "Hinzufügen" -> add(r.uuid(), r.name());
			case "Ablehnen" -> {
				Friends.ignore(r.uuid());
				info("Anfrage von " + r.name() + " abgelehnt.", false);
			}
			case "Entfernen" -> {
				Friends.remove(r.uuid());
				info(r.name() + " entfernt.", false);
				NovaNetwork.requestSync();
			}
			case "Beitreten" -> {
				Friends.Status s = Friends.status(r.uuid());
				if (s != null && !s.server().isEmpty()) {
					join(s.server(), r.name());
				}
			}
			default -> {
			}
		}
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!inputFocused || !event.isAllowedChatCharacter()) {
			return false;
		}
		String c = event.codepointAsString();
		if (c.matches("[A-Za-z0-9_]") && input.length() < 16) {
			input += c;
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (inputFocused) {
			if (key == InputConstants.KEY_BACKSPACE && !input.isEmpty()) {
				input = input.substring(0, input.length() - 1);
				return true;
			}
			if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
				addTyped();
				return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		layout();
		scroll = Math.max(0, Math.min(maxScroll(rows()), scroll - scrollY * 20));
		return true;
	}

	// ------------------------------------------------------------------ test helpers

	public int[] inputCenter() {
		layout();
		return new int[] {left + 40, top + 34};
	}

	public int[] addButtonCenter() {
		layout();
		int[] ab = addButton();
		return new int[] {(ab[0] + ab[2]) / 2, (ab[1] + ab[3]) / 2};
	}

	/** Centre of a button ("Annehmen", "Entfernen", …) in the row of this player, or null. */
	public int @Nullable [] buttonCenter(String name, String button) {
		layout();
		int y = listTop() - (int) scroll;
		for (Row r : rows()) {
			int h = rowHeight(r);
			if (r.name().equalsIgnoreCase(name) && r.uuid() != null) {
				int[][] b = buttons(r, y);
				String[] labels = {r.button1(), r.button2()};
				for (int i = 0; i < 2; i++) {
					if (b[i] != null && labels[i].equals(button)) {
						return new int[] {(b[i][0] + b[i][2]) / 2, (b[i][1] + b[i][3]) / 2};
					}
				}
			}
			y += h + 2;
		}
		return null;
	}

	public String getMessage() {
		return message;
	}
}
