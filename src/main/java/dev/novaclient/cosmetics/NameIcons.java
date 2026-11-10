package dev.novaclient.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import dev.novaclient.NovaClient;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Icons shown in front of your own name: a few built-in ones and your own PNG files from
 * {@code config/novaclient/icons/}. A file that is several times taller than wide holds several frames below each
 * other (e.g. 256x512 = two frames); the frames are shown in turns.
 */
public final class NameIcons {
	public static final String NONE = "Keins";
	/** Height of an icon in name tag / GUI pixels. */
	public static final int SIZE = 9;
	private static final int MAX_FILE_BYTES = 4 * 1024 * 1024;
	private static final int MAX_FRAMES = 16;
	private static final int FRAME_PX = 36; // frames are stored at this height (4x the drawn size)

	/** display name -> resource name of the built-in icons */
	private static final Map<String, String> BUILT_IN = new LinkedHashMap<>();

	static {
		BUILT_IN.put("Blitz", "blitz");
		BUILT_IN.put("Herz", "herz");
		BUILT_IN.put("Stern", "stern");
		BUILT_IN.put("Flamme", "flamme");
		BUILT_IN.put("Diamant", "diamant");
	}

	/** A loaded icon: all frames below each other in one texture. */
	public record Icon(Identifier texture, int frames, int frameW, int frameH) {
		/** Drawn width in name pixels (height is {@link #SIZE}). */
		public int width() {
			return Math.max(3, Math.min(SIZE * 2, Math.round(SIZE * frameW / (float) frameH)));
		}

		/** Number of spaces that make room for the icon in front of the name (a space is 4 px wide). */
		public int spaces() {
			return (width() + 2 + 3) / 4;
		}
	}

	private static final Map<String, Icon> LOADED = new HashMap<>();
	private static final Map<String, Long> FAILED = new HashMap<>();
	private static List<String> names = List.of(NONE);
	private static long scannedAt;
	private static int counter;

	private NameIcons() {
	}

	public static Path folder() {
		return FabricLoader.getInstance().getConfigDir().resolve("novaclient").resolve("icons");
	}

	/** "Keins", the built-in icons and the names of your own files (without ".png"); rescanned every 2 seconds. */
	public static List<String> names() {
		long now = System.currentTimeMillis();
		if (now - scannedAt < 2000) {
			return names;
		}
		scannedAt = now;
		List<String> out = new ArrayList<>();
		out.add(NONE);
		out.addAll(BUILT_IN.keySet());
		Path dir = folder();
		try {
			Files.createDirectories(dir);
			try (Stream<Path> files = Files.list(dir)) {
				files.map(p -> p.getFileName().toString())
					.filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".png") && n.length() > 4 && n.length() <= 44)
					.map(n -> n.substring(0, n.length() - 4))
					.filter(n -> !out.contains(n))
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.limit(200)
					.forEach(out::add);
			}
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not list the icons folder: {}", e.toString());
		}
		names = List.copyOf(out);
		return names;
	}

	/** Forces a rescan of the folder and reloads changed files (used by /icons neu). */
	public static void reload() {
		Minecraft mc = Minecraft.getInstance();
		for (Icon icon : LOADED.values()) {
			mc.getTextureManager().release(icon.texture());
		}
		LOADED.clear();
		FAILED.clear();
		scannedAt = 0;
	}

	/** The icon with this name, or null (none chosen, file missing or not a usable PNG). Render thread only. */
	public static Icon get(String name) {
		if (name == null || name.isEmpty() || name.equals(NONE)) {
			return null;
		}
		Icon icon = LOADED.get(name);
		if (icon != null) {
			return icon;
		}
		Long failed = FAILED.get(name);
		long now = System.currentTimeMillis();
		if (failed != null && now - failed < 5000) {
			return null;
		}
		try {
			BufferedImage image = read(name);
			if (image == null) {
				FAILED.put(name, now);
				return null;
			}
			icon = register(image);
			LOADED.put(name, icon);
			return icon;
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not load the name icon '{}': {}", name, e.toString());
			FAILED.put(name, now);
			return null;
		}
	}

	private static BufferedImage read(String name) throws Exception {
		String builtIn = BUILT_IN.get(name);
		if (builtIn != null) {
			Identifier id = Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "textures/icons/" + builtIn + ".png");
			try (InputStream in = Minecraft.getInstance().getResourceManager().open(id)) {
				return ImageIO.read(in);
			}
		}
		// only plain file names from the icons folder
		if (name.contains("/") || name.contains("\\") || name.contains("..")) {
			return null;
		}
		Path file = folder().resolve(name + ".png");
		if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) {
			return null;
		}
		try (InputStream in = Files.newInputStream(file)) {
			return ImageIO.read(in);
		}
	}

	/** Splits the image into frames, scales every frame down and uploads them as one texture. */
	private static Icon register(BufferedImage source) {
		int w = source.getWidth();
		int h = source.getHeight();
		int frames = h > w && h % w == 0 ? Math.min(MAX_FRAMES, h / w) : 1;
		int srcFrameH = frames > 1 ? w : h;
		// small pixel art stays as it is, big pictures are scaled down smoothly
		int frameH = Math.min(srcFrameH, FRAME_PX);
		int frameW = Math.max(1, Math.min(frameH * 2, Math.round(w * frameH / (float) srcFrameH)));
		NativeImage ni = new NativeImage(frameW, frameH * frames, false);
		for (int f = 0; f < frames; f++) {
			BufferedImage frame = scale(source.getSubimage(0, f * srcFrameH, w, srcFrameH), frameW, frameH);
			for (int y = 0; y < frameH; y++) {
				for (int x = 0; x < frameW; x++) {
					ni.setPixel(x, f * frameH + y, frame.getRGB(x, y));
				}
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "dynamic/name_icon_" + (counter++));
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Nova name icon", ni));
		return new Icon(id, frames, frameW, frameH);
	}

	/** Scales in halving steps so big pictures stay clean when they get tiny. */
	private static BufferedImage scale(BufferedImage src, int w, int h) {
		BufferedImage cur = src;
		while (cur.getWidth() / 2 >= w && cur.getHeight() / 2 >= h) {
			cur = draw(cur, cur.getWidth() / 2, cur.getHeight() / 2);
		}
		return draw(cur, w, h);
	}

	private static BufferedImage draw(BufferedImage src, int w, int h) {
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			src.getWidth() == w && src.getHeight() == h ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(src, 0, 0, w, h, null);
		g.dispose();
		return out;
	}

	/** The frame to show now. */
	public static int frame(Icon icon, long switchMs) {
		if (icon.frames() <= 1 || switchMs <= 0) {
			return 0;
		}
		return (int) (System.currentTimeMillis() / switchMs % icon.frames());
	}
}
