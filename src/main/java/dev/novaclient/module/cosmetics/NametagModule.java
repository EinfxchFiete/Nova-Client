package dev.novaclient.module.cosmetics;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.setting.BoolSetting;
import net.minecraft.network.chat.Component;

/** Shows your own nametag in third person and marks you with a Nova symbol. */
public class NametagModule extends Module {
	private static NametagModule instance;

	public final BoolSetting ownNametag = add(new BoolSetting("Eigener Nametag (F5)", true));
	public final BoolSetting symbol = add(new BoolSetting("Nova-Symbol vor dem Namen", true));
	public final dev.novaclient.setting.ModeSetting color = add(new dev.novaclient.setting.ModeSetting("Namensfarbe", "Normal",
		"Normal", "Theme", "Regenbogen", "Gold", "Rot", "Grün", "Blau", "Pink"));
	/** Animated name styles (own implementation; every letter gets its own colour each frame). */
	public final dev.novaclient.setting.ModeSetting effect = add(new dev.novaclient.setting.ModeSetting("Effekt", "Keiner",
		"Keiner", "Farbverlauf", "Regenbogen-Welle", "Metallic", "Pulsieren", "Schimmer", "Blinken", "Chromatisch", "Feuer", "Leuchten",
		"Buchstabe für Buchstabe", "Gold-Stern"));
	public final dev.novaclient.setting.ModeSetting second = add(new dev.novaclient.setting.ModeSetting("Zweite Farbe (Farbverlauf)", "Blau",
		"Weiß", "Gold", "Rot", "Grün", "Blau", "Pink", "Lila", "Türkis"));
	public final BoolSetting bold = add(new BoolSetting("Fett", false));
	public final BoolSetting italic = add(new BoolSetting("Schräg", false));
	public final BoolSetting inTab = add(new BoolSetting("Auch in der Tab-Liste", true));

	public static boolean tabOn() {
		return instance != null && instance.inTab.isOn();
	}

	public NametagModule() {
		super("Nametags", "Eigener Name in der 3. Person, Nova-Symbol, Namensfarbe und animierte Effekte", Category.COSMETICS, false);
		instance = this;
	}

	public static boolean showOwn() {
		return instance != null && instance.isEnabled() && instance.ownNametag.isOn();
	}

	private int nameColor() {
		return switch (color.get()) {
			case "Theme" -> ThemeModule.accent() & 0xFFFFFF;
			case "Regenbogen" -> java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000F, 0.7F, 1F) & 0xFFFFFF;
			case "Gold" -> 0xFFC830;
			case "Rot" -> 0xFF5050;
			case "Grün" -> 0x55FF77;
			case "Blau" -> 0x5599FF;
			case "Pink" -> 0xFF77DD;
			default -> -1;
		};
	}

	/** Own name in the chosen colour (when the module is on). */
	public static Component colorize(Component name) {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		return colorize(name, mc.getUser() == null ? "" : mc.getUser().getName());
	}

	/**
	 * Colours and animates only the user name inside the display name; everything around it (rank prefixes, team
	 * colours, suffixes) keeps its own style. If the user name is not part of the text, the whole text is styled.
	 */
	public static Component colorize(Component name, String username) {
		NametagModule n = instance;
		if (n == null || !n.isEnabled() || name == null) {
			return name;
		}
		int c = n.nameColor();
		String fx = n.effect.get();
		boolean styled = n.bold.isOn() || n.italic.isOn();
		if (fx.equals("Keiner") && !styled && c < 0) {
			return name;
		}
		String s = name.getString();
		int from = username == null || username.isEmpty() ? -1 : s.indexOf(username);
		int to = from < 0 ? s.length() : from + username.length();
		if (from < 0) {
			from = 0;
		}
		int base = c < 0 ? 0xFFFFFF : c;
		if (fx.equals("Gold-Stern")) {
			base = 0xFFD54A;
		}
		float t = (System.currentTimeMillis() % 3_600_000L) / 1000.0F;
		int len = to - from;
		final int start = from;
		final int end = to;
		final int baseColor = base;
		net.minecraft.network.chat.MutableComponent out = Component.empty();
		int[] pos = {0};
		name.visit((style, text) -> {
			for (int k = 0; k < text.length(); k++) {
				int i = pos[0]++;
				String ch = String.valueOf(text.charAt(k));
				if (i < start || i >= end) {
					out.append(Component.literal(ch).setStyle(style));
					continue;
				}
				if (i == start && fx.equals("Gold-Stern")) {
					out.append(Component.literal("★ ").withColor(mix(0xFFB300, 0xFFF59D, 0.5F + 0.5F * (float) Math.sin(t * 4.0F))));
				}
				net.minecraft.network.chat.Style st = style.withColor(n.letterColor(fx, baseColor, i - start, len, t));
				if (n.bold.isOn()) {
					st = st.withBold(true);
				}
				if (n.italic.isOn()) {
					st = st.withItalic(true);
				}
				out.append(Component.literal(ch).setStyle(st));
			}
			return java.util.Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		return out;
	}

	private int secondColor() {
		return switch (second.get()) {
			case "Gold" -> 0xFFC830;
			case "Rot" -> 0xFF5050;
			case "Grün" -> 0x55FF77;
			case "Blau" -> 0x5599FF;
			case "Pink" -> 0xFF77DD;
			case "Lila" -> 0xB388FF;
			case "Türkis" -> 0x40E0D0;
			default -> 0xFFFFFF;
		};
	}

	/** Colour of letter i (of len) at time t for the chosen effect. */
	int letterColor(String fx, int base, int i, int len, float t) {
		float p = len <= 1 ? 0 : i / (float) (len - 1);
		// a highlight that sweeps over the name from left to right, then pauses
		float sweep = (t * 0.7F) % 1.8F - 0.3F;
		float shine = Math.max(0, 1 - Math.abs(p - sweep) * 4.0F);
		return switch (fx) {
			case "Farbverlauf" -> mix(base, secondColor(), 0.5F + 0.5F * (float) Math.sin((p * 0.8F - t * 0.35F) * Math.PI * 2));
			case "Regenbogen-Welle" -> java.awt.Color.HSBtoRGB(frac(t * 0.4F - i * 0.07F), 0.7F, 1F) & 0xFFFFFF;
			case "Metallic" -> mix(mix(base, 0x8A909C, 0.6F), 0xFFFFFF, shine);
			case "Pulsieren" -> mix(base, scale(base, 0.3F), 0.5F + 0.5F * (float) Math.sin(t * 3.0F));
			case "Schimmer" -> mix(base, 0xFFFFFF, shine * 0.75F);
			case "Blinken" -> (long) (t * 2.0F) % 2 == 0 ? base : scale(base, 0.35F);
			case "Chromatisch" -> mix(0xFF3B5C, 0x3BE8FF, 0.5F + 0.5F * (float) Math.sin(t * 5.0F + i * 1.3F));
			case "Feuer" -> mix(0xFF3D00, 0xFFE24A, 0.5F + 0.5F * (float) (Math.sin(t * 9.0F + i * 2.1F) * Math.sin(t * 5.3F + i)));
			case "Leuchten" -> mix(base, 0xFFFFFF, 0.35F + 0.35F * (float) Math.sin(t * 4.0F));
			case "Buchstabe für Buchstabe" -> (int) (t * 8.0F) % (len + 3) == i ? 0xFFFFFF : scale(base, 0.6F);
			default -> base;
		};
	}

	private static float frac(float v) {
		return v - (float) Math.floor(v);
	}

	private static int scale(int rgb, float f) {
		return mix(0x000000, rgb, f);
	}

	private static int mix(int a, int b, float f) {
		f = Math.max(0, Math.min(1, f));
		int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * f);
		int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * f);
		int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * f);
		return r << 16 | g << 8 | bl;
	}

	/** Whether the Nova symbol should be shown in front of your own name. */
	public static boolean symbolOn() {
		return instance != null && instance.isEnabled() && instance.symbol.isOn();
	}

	/** Kept for compatibility: own name with colour (badges are added by {@link dev.novaclient.cosmetics.Badges}). */
	public static Component decorate(Component name) {
		return colorize(name);
	}
}
