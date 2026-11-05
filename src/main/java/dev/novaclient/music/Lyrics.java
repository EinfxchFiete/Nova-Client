package dev.novaclient.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.novaclient.NovaClient;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Synced song lyrics from LRCLIB (lrclib.net: free, open, no account). Only the title, artist and length of the
 * current song are sent; results are cached in memory, nothing is stored on disk.
 */
public final class Lyrics {
	private static final int MAX_CACHE = 30;
	private static final String API = "https://lrclib.net/api/";
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(6))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.proxy(ProxySelector.getDefault())
		.build();
	private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Nova Lyrics");
		t.setDaemon(true);
		return t;
	});

	/**
	 * One line. {@code text} is empty for an instrumental break. {@code wordChars}/{@code wordMs} are the start
	 * character and start time of each word if the lyrics had word timings (enhanced LRC), otherwise null.
	 */
	public record Line(long startMs, String text, int @Nullable [] wordChars, long @Nullable [] wordMs) {
		public boolean isBreak() {
			return text.isEmpty();
		}
	}

	public record Track(List<Line> lines, boolean instrumental) {
		/** Index of the line that is current at {@code ms}, -1 before the first line. */
		public int indexAt(long ms) {
			int lo = 0;
			int hi = lines.size() - 1;
			int found = -1;
			while (lo <= hi) {
				int mid = (lo + hi) >>> 1;
				if (lines.get(mid).startMs() <= ms) {
					found = mid;
					lo = mid + 1;
				} else {
					hi = mid - 1;
				}
			}
			return found;
		}

		public @Nullable Line line(int i) {
			return i >= 0 && i < lines.size() ? lines.get(i) : null;
		}

		/** Start of the line after {@code i}, or -1 if it is the last one. */
		public long nextStart(int i) {
			return i + 1 < lines.size() ? lines.get(i + 1).startMs() : -1;
		}
	}

	public enum State {
		LOADING, READY, MISSING, PLAIN_ONLY
	}

	private static final class Entry {
		volatile State state = State.LOADING;
		volatile @Nullable Track track;
		/** After a network error the lookup is tried again from this time on (0 = never). */
		volatile long retryAt;
	}

	private static final Map<String, Entry> CACHE = new LinkedHashMap<>(16, 0.75F, true);

	private Lyrics() {
	}

	private static String key(String title, String artist) {
		return (title + "\u0000" + artist).toLowerCase(Locale.ROOT);
	}

	private static synchronized Entry entry(String title, String artist, long lengthMs) {
		String k = key(title, artist);
		Entry e = CACHE.get(k);
		if (e == null) {
			e = new Entry();
			CACHE.put(k, e);
			while (CACHE.size() > MAX_CACHE) {
				CACHE.remove(CACHE.keySet().iterator().next());
			}
			Entry entry = e;
			LOADER.execute(() -> load(entry, title, artist, lengthMs));
		} else if (e.state == State.MISSING && e.retryAt > 0 && System.currentTimeMillis() > e.retryAt) {
			// the last try failed because of the network (offline, timeout): try again
			e.retryAt = 0;
			e.state = State.LOADING;
			Entry entry = e;
			LOADER.execute(() -> load(entry, title, artist, lengthMs));
		}
		return e;
	}

	/** Loading state of the lyrics of this song; starts loading on the first call. */
	public static State state(String title, String artist, long lengthMs) {
		if (title == null || title.isBlank()) {
			return State.MISSING;
		}
		return entry(title, artist, lengthMs).state;
	}

	/** The synced lyrics, or null while loading / if there are none. */
	public static @Nullable Track get(String title, String artist, long lengthMs) {
		if (title == null || title.isBlank()) {
			return null;
		}
		Entry e = entry(title, artist, lengthMs);
		return e.state == State.READY ? e.track : null;
	}

	/** For tests: use these LRC lyrics for the song (no download). */
	public static synchronized void putForTest(String title, String artist, String lrc) {
		Entry e = new Entry();
		e.track = parse(lrc);
		e.state = State.READY;
		CACHE.put(key(title, artist), e);
	}

	// ------------------------------------------------------------------ download

	private static void load(Entry e, String title, String artist, long lengthMs) {
		try {
			String t = cleanTitle(title);
			String a = firstArtist(artist);
			long seconds = lengthMs > 0 ? Math.round(lengthMs / 1000.0) : -1;
			JsonObject best = null;
			if (!a.isEmpty() && seconds > 0) {
				best = getExact(t, a, seconds);
			}
			if (best == null && !a.isEmpty()) {
				best = getExact(t, a, -1);
			}
			if (best == null || !hasSynced(best) && !isInstrumental(best)) {
				JsonObject found = pick(search("track_name=" + enc(t) + (a.isEmpty() ? "" : "&artist_name=" + enc(a))), t, seconds);
				if (found == null) {
					found = pick(search("q=" + enc((a + " " + t).trim())), t, seconds);
				}
				if (found != null && (best == null || hasSynced(found) || isInstrumental(found))) {
					best = found;
				}
			}
			if (best == null) {
				e.state = State.MISSING;
				return;
			}
			if (isInstrumental(best)) {
				e.track = new Track(List.of(), true);
				e.state = State.READY;
				return;
			}
			if (!hasSynced(best)) {
				e.state = best.has("plainLyrics") && !best.get("plainLyrics").isJsonNull() ? State.PLAIN_ONLY : State.MISSING;
				return;
			}
			Track track = parse(best.get("syncedLyrics").getAsString());
			if (track.lines().isEmpty()) {
				e.state = State.MISSING;
				return;
			}
			e.track = track;
			e.state = State.READY;
		} catch (java.io.IOException ex) {
			e.retryAt = System.currentTimeMillis() + 60_000;
			e.state = State.MISSING;
		} catch (Exception ex) {
			e.state = State.MISSING;
		}
	}

	private static @Nullable JsonObject getExact(String title, String artist, long seconds) throws Exception {
		String q = "get?track_name=" + enc(title) + "&artist_name=" + enc(artist) + (seconds > 0 ? "&duration=" + seconds : "");
		HttpResponse<String> res = HTTP.send(request(q), HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200 || res.body().length() > 500_000) {
			return null;
		}
		JsonElement el = JsonParser.parseString(res.body());
		return el.isJsonObject() ? el.getAsJsonObject() : null;
	}

	private static @Nullable JsonArray search(String query) throws Exception {
		HttpResponse<String> res = HTTP.send(request("search?" + query), HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200 || res.body().length() > 3_000_000) {
			return null;
		}
		JsonElement el = JsonParser.parseString(res.body());
		return el.isJsonArray() ? el.getAsJsonArray() : null;
	}

	/** Best search result: synced lyrics first, then the same title, then the closest length. */
	private static @Nullable JsonObject pick(@Nullable JsonArray results, String title, long seconds) {
		if (results == null || results.isEmpty()) {
			return null;
		}
		String wanted = title.toLowerCase(Locale.ROOT);
		List<JsonObject> list = new ArrayList<>();
		for (JsonElement el : results) {
			if (el.isJsonObject()) {
				list.add(el.getAsJsonObject());
			}
		}
		list.sort(Comparator.<JsonObject>comparingInt(o -> hasSynced(o) || isInstrumental(o) ? 0 : 1)
			.thenComparingInt(o -> string(o, "trackName").toLowerCase(Locale.ROOT).equals(wanted) ? 0 : 1)
			.thenComparingDouble(o -> seconds > 0 && o.has("duration") && !o.get("duration").isJsonNull()
				? Math.abs(o.get("duration").getAsDouble() - seconds) : 0));
		JsonObject best = list.isEmpty() ? null : list.get(0);
		// a much longer/shorter recording is another version (live, remix): its timing would be wrong
		if (best != null && seconds > 0 && best.has("duration") && !best.get("duration").isJsonNull()
			&& Math.abs(best.get("duration").getAsDouble() - seconds) > 12) {
			return null;
		}
		return best;
	}

	private static HttpRequest request(String path) {
		return HttpRequest.newBuilder(URI.create(API + path))
			.timeout(Duration.ofSeconds(10))
			.header("User-Agent", "NovaClient/" + NovaClient.VERSION + " (https://github.com/EinfxchFiete/Nova-Client)")
			.GET()
			.build();
	}

	private static boolean hasSynced(JsonObject o) {
		return o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull() && !o.get("syncedLyrics").getAsString().isBlank();
	}

	private static boolean isInstrumental(JsonObject o) {
		return o.has("instrumental") && !o.get("instrumental").isJsonNull() && o.get("instrumental").getAsBoolean();
	}

	private static String string(JsonObject o, String key) {
		return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	/** "Song - Remastered 2011", "Song (feat. X)" -> "Song". */
	public static String cleanTitle(String title) {
		String t = title.replaceAll("(?i)\\s*[(\\[](feat\\.?|ft\\.?|with|prod\\.?)\\s[^)\\]]*[)\\]]", "");
		t = t.replaceAll("(?i)\\s+[-–]\\s+[^-–]*(remaster|version|edit|live|mono|stereo|radio|explicit|bonus)[^-–]*$", "");
		t = t.replaceAll("(?i)\\s*[(\\[][^)\\]]*(remaster|version|explicit)[^)\\]]*[)\\]]", "");
		t = t.strip();
		return t.isEmpty() ? title.strip() : t;
	}

	/** "A, B & C" / "A feat. B" -> "A". */
	public static String firstArtist(String artist) {
		if (artist == null) {
			return "";
		}
		String a = artist.split("(?i),|;|\\s&\\s|\\s(feat\\.?|ft\\.?|featuring)\\s|\\s/\\s")[0].strip();
		return a.isEmpty() ? artist.strip() : a;
	}

	// ------------------------------------------------------------------ LRC

	private static final Pattern TIME = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]");
	private static final Pattern WORD = Pattern.compile("<(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?>");
	private static final Pattern OFFSET = Pattern.compile("^\\[offset:\\s*([+-]?\\d+)\\s*]", Pattern.CASE_INSENSITIVE);

	private static long millis(Matcher m) {
		long min = Long.parseLong(m.group(1));
		long sec = Long.parseLong(m.group(2));
		String frac = m.group(3);
		long ms = 0;
		if (frac != null) {
			ms = switch (frac.length()) {
				case 1 -> Long.parseLong(frac) * 100;
				case 2 -> Long.parseLong(frac) * 10;
				default -> Long.parseLong(frac);
			};
		}
		return min * 60_000 + sec * 1000 + ms;
	}

	/** Parses LRC text: "[mm:ss.xx] text" (several time tags per line allowed), "[offset:ms]", word tags "<mm:ss.xx>". */
	public static Track parse(String lrc) {
		long offset = 0;
		List<Line> out = new ArrayList<>();
		for (String raw : lrc.split("\\r?\\n")) {
			String line = raw.strip();
			Matcher om = OFFSET.matcher(line);
			if (om.find()) {
				offset = Long.parseLong(om.group(1));
				continue;
			}
			List<Long> times = new ArrayList<>();
			Matcher m = TIME.matcher(line);
			int pos = 0;
			while (m.find() && m.start() == pos) {
				times.add(millis(m));
				pos = m.end();
			}
			if (times.isEmpty()) {
				continue;
			}
			String text = line.substring(pos);
			int[] wordChars = null;
			long[] wordMs = null;
			Matcher w = WORD.matcher(text);
			if (w.find()) {
				w.reset();
				StringBuilder sb = new StringBuilder();
				List<Integer> chars = new ArrayList<>();
				List<Long> ms = new ArrayList<>();
				int last = 0;
				while (w.find()) {
					sb.append(text, last, w.start());
					chars.add(sb.length());
					ms.add(millis(w));
					last = w.end();
				}
				sb.append(text.substring(last));
				// strip leading spaces without breaking the indices
				int lead = 0;
				while (lead < sb.length() && Character.isWhitespace(sb.charAt(lead))) {
					lead++;
				}
				text = sb.substring(lead).stripTrailing();
				wordChars = new int[chars.size()];
				wordMs = new long[ms.size()];
				for (int i = 0; i < chars.size(); i++) {
					wordChars[i] = Math.max(0, Math.min(text.length(), chars.get(i) - lead));
					wordMs[i] = ms.get(i);
				}
			} else {
				text = text.strip();
			}
			text = clean(text);
			if (text.isEmpty()) {
				wordChars = null;
				wordMs = null;
			}
			for (long t : times) {
				long[] shifted = null;
				if (wordMs != null) {
					shifted = new long[wordMs.length];
					for (int i = 0; i < wordMs.length; i++) {
						shifted[i] = Math.max(0, wordMs[i] - offset);
					}
				}
				out.add(new Line(Math.max(0, t - offset), text, wordChars, shifted));
			}
		}
		out.sort(Comparator.comparingLong(Line::startMs));
		// two breaks in a row are one break
		List<Line> merged = new ArrayList<>();
		for (Line l : out) {
			if (l.isBreak() && !merged.isEmpty() && merged.get(merged.size() - 1).isBreak()) {
				continue;
			}
			merged.add(l);
		}
		return new Track(List.copyOf(merged), false);
	}

	/** No formatting codes or control characters; a line that is only music notes is a break. */
	private static String clean(String s) {
		StringBuilder sb = new StringBuilder();
		s.codePoints().forEach(cp -> {
			if (cp == '§' || Character.isISOControl(cp)) {
				return;
			}
			sb.appendCodePoint(cp);
		});
		String t = sb.toString().strip();
		if (t.replaceAll("[♪♫♬♩\\s]", "").isEmpty()) {
			return "";
		}
		return t.length() > 120 ? t.substring(0, 120) : t;
	}
}
