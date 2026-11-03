package dev.novaclient.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.novaclient.NovaClient;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Album covers for songs. Looked up by title + artist with Apple's public iTunes search (no account needed),
 * so every Nova player can show the cover of everyone else without sending images over the Nova server.
 * Covers are cached in memory as 64x64 textures; nothing is stored on disk.
 */
public final class CoverArt {
	public static final int SIZE = 64;
	private static final int MAX_CACHE = 40;
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Nova Cover Art");
		t.setDaemon(true);
		return t;
	});

	private enum State { LOADING, READY, MISSING }

	private static final class Entry {
		volatile State state = State.LOADING;
		volatile @Nullable Identifier texture;
	}

	private static final Map<String, Entry> CACHE = new LinkedHashMap<>(16, 0.75F, true);
	private static int counter;

	private CoverArt() {
	}

	private static String key(String title, String artist) {
		return (title + "\u0000" + artist).toLowerCase(Locale.ROOT);
	}

	/** Texture of the cover, or null while loading / if there is none. Starts loading on the first call. */
	public static synchronized @Nullable Identifier get(String title, String artist) {
		if (title == null || title.isBlank()) {
			return null;
		}
		String k = key(title, artist);
		Entry e = CACHE.get(k);
		if (e == null) {
			e = new Entry();
			CACHE.put(k, e);
			trim();
			Entry entry = e;
			LOADER.execute(() -> load(entry, title, artist));
		}
		return e.state == State.READY ? e.texture : null;
	}

	/** For tests: use this image as the cover of the song. */
	public static synchronized void putForTest(String title, String artist, BufferedImage image) {
		Entry e = new Entry();
		CACHE.put(key(title, artist), e);
		Minecraft.getInstance().execute(() -> register(e, image));
	}

	private static void trim() {
		while (CACHE.size() > MAX_CACHE) {
			String eldest = CACHE.keySet().iterator().next();
			Entry old = CACHE.remove(eldest);
			Identifier tex = old.texture;
			if (tex != null) {
				Minecraft.getInstance().execute(() -> Minecraft.getInstance().getTextureManager().release(tex));
			}
		}
	}

	private static void load(Entry e, String title, String artist) {
		try {
			String term = URLEncoder.encode((artist + " " + title).trim(), StandardCharsets.UTF_8);
			HttpRequest search = HttpRequest.newBuilder(URI.create("https://itunes.apple.com/search?media=music&entity=song&limit=1&term=" + term))
				.timeout(Duration.ofSeconds(8)).header("User-Agent", "NovaClient/" + NovaClient.VERSION).GET().build();
			HttpResponse<String> res = HTTP.send(search, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() != 200) {
				e.state = State.MISSING;
				return;
			}
			JsonArray results = JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonArray("results");
			if (results == null || results.isEmpty()) {
				e.state = State.MISSING;
				return;
			}
			JsonObject first = results.get(0).getAsJsonObject();
			if (!first.has("artworkUrl100")) {
				e.state = State.MISSING;
				return;
			}
			String url = first.get("artworkUrl100").getAsString().replace("100x100bb", "128x128bb");
			if (!url.startsWith("https://")) {
				e.state = State.MISSING;
				return;
			}
			HttpResponse<byte[]> img = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(),
				HttpResponse.BodyHandlers.ofByteArray());
			if (img.statusCode() != 200 || img.body().length > 2_000_000) {
				e.state = State.MISSING;
				return;
			}
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(img.body()));
			if (image == null) {
				e.state = State.MISSING;
				return;
			}
			Minecraft.getInstance().execute(() -> register(e, image));
		} catch (Exception ex) {
			e.state = State.MISSING;
		}
	}

	/** Scales the image to 64x64 and uploads it (render thread). */
	private static void register(Entry e, BufferedImage source) {
		BufferedImage scaled = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(source, 0, 0, SIZE, SIZE, null);
		g.dispose();
		NativeImage ni = new NativeImage(SIZE, SIZE, false);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				ni.setPixel(x, y, scaled.getRGB(x, y) | 0xFF000000);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "dynamic/cover_" + (counter++));
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Nova cover", ni));
		e.texture = id;
		e.state = State.READY;
	}
}
