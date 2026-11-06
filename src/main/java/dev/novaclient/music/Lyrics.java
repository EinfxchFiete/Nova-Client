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
		/** All synced lyrics that fit the song, best first, and the one in use. */
		volatile List<Candidate> candidates = List.of();
		volatile int index;
	}

	private static final Map<String, Entry> CACHE = new LinkedHashMap<>(16, 0.75F, true);

	private Lyrics() {
	}

	private static String key(String title, String artist) {
		return MusicData.key(title, artist);
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

	// ------------------------------------------------------------------ songs

	/** {title, artist} used for the lyrics of this song (see {@link #query}). */
	public static String[] query(NowPlaying.Song s) {
		return query(s.title(), s.artist(), s.source());
	}

	/** Key of the song's lyrics (for offsets and choices in {@link MusicData}). */
	public static String key(NowPlaying.Song s) {
		String[] q = query(s);
		return key(q[0], q[1]);
	}

	public static State state(NowPlaying.Song s) {
		String[] q = query(s);
		return state(q[0], q[1], s.lengthMs());
	}

	public static @Nullable Track get(NowPlaying.Song s) {
		String[] q = query(s);
		return get(q[0], q[1], s.lengthMs());
	}

	/** Number of synced lyrics found for this song (you can switch between them), 0 if none / still loading. */
	public static synchronized int choices(NowPlaying.Song s) {
		Entry e = CACHE.get(key(s));
		return e == null ? 0 : e.candidates.size();
	}

	/** "2/3: Title – Artist (Album)" of the lyrics in use, or "" if none. */
	public static synchronized String info(NowPlaying.Song s) {
		Entry e = CACHE.get(key(s));
		if (e == null || e.candidates.isEmpty() || e.state != State.READY) {
			return "";
		}
		return (e.index + 1) + "/" + e.candidates.size() + ": " + e.candidates.get(e.index).label();
	}

	/** Uses the next lyrics found for this song (if the first ones belong to another version); remembers the choice. */
	public static synchronized String next(NowPlaying.Song s) {
		String k = key(s);
		Entry e = CACHE.get(k);
		if (e == null || e.candidates.size() < 2) {
			return e == null || e.state == State.LOADING ? "Der Songtext wird noch geladen." : "Für diesen Song gibt es keine anderen Songtexte.";
		}
		use(e, (e.index + 1) % e.candidates.size());
		MusicData.setChoice(k, e.candidates.get(e.index).id());
		return "Songtext " + info(s);
	}

	/** Forgets the lyrics of this song and loads them again. */
	public static synchronized void reload(NowPlaying.Song s) {
		CACHE.remove(key(s));
	}

	// ------------------------------------------------------------------ download

	/** A search result with its match score; only results whose title, artist and length fit are kept. */
	private record Candidate(JsonObject json, double score) {
		long id() {
			return json.has("id") && !json.get("id").isJsonNull() ? json.get("id").getAsLong() : -1;
		}

		String label() {
			String a = string(json, "artistName");
			String al = string(json, "albumName");
			return string(json, "trackName") + (a.isEmpty() ? "" : " – " + a) + (al.isEmpty() ? "" : " (" + al + ")");
		}
	}

	private static void load(Entry e, String title, String artist, long lengthMs) {
		try {
			String t = cleanTitle(title);
			String a = firstArtist(artist);
			long seconds = lengthMs > 0 ? Math.round(lengthMs / 1000.0) : -1;
			Map<Object, JsonObject> found = new LinkedHashMap<>();
			if (!a.isEmpty() && seconds > 0) {
				JsonObject exact = getExact(t, a, seconds);
				if (exact != null) {
					// LRCLIB's own match for title, artist and length: preferred when the scores are close
					exact.addProperty("_exact", true);
				}
				add(found, exact);
			}
			add(found, search("track_name=" + enc(t) + (a.isEmpty() ? "" : "&artist_name=" + enc(a))));
			List<Candidate> ranked = rank(found.values(), t, artist, seconds);
			if (ranked.stream().noneMatch(c -> hasSynced(c.json()))) {
				// nothing synced yet: a free search ("artist title") and, without a known length, the exact lookup
				add(found, search("q=" + enc((a + " " + t).trim())));
				if (!a.isEmpty() && seconds <= 0) {
					add(found, getExact(t, a, -1));
				}
				ranked = rank(found.values(), t, artist, seconds);
			}
			apply(e, ranked, key(title, artist));
		} catch (java.io.IOException ex) {
			e.retryAt = System.currentTimeMillis() + 60_000;
			e.state = State.MISSING;
		} catch (Exception ex) {
			NovaClient.LOGGER.debug("Lyrics lookup failed", ex);
			e.state = State.MISSING;
		}
	}

	private static void add(Map<Object, JsonObject> found, @Nullable JsonElement el) {
		if (el == null) {
			return;
		}
		if (el.isJsonArray()) {
			for (JsonElement x : el.getAsJsonArray()) {
				add(found, x);
			}
		} else if (el.isJsonObject()) {
			JsonObject o = el.getAsJsonObject();
			Object id = o.has("id") && !o.get("id").isJsonNull() ? (Object) o.get("id").getAsLong() : (Object) o;
			found.putIfAbsent(id, o);
		}
	}

	/** Usable results, best first: synced lyrics before instrumental before plain ones, then by score. */
	static List<Candidate> rank(java.util.Collection<JsonObject> results, String title, String artist, long seconds) {
		List<Candidate> out = new ArrayList<>();
		for (JsonObject o : results) {
			double s = score(o, title, artist, seconds);
			if (!Double.isNaN(s)) {
				out.add(new Candidate(o, s));
			}
		}
		out.sort(Comparator.<Candidate>comparingInt(c -> hasSynced(c.json()) ? 0 : isInstrumental(c.json()) ? 1 : 2)
			.thenComparing(Comparator.comparingDouble(Candidate::score).reversed()));
		return out;
	}

	/**
	 * How well a result fits the song (higher = better), NaN if it is another song: the title must be (nearly) the same,
	 * the artist must match if we know it, and the recording must not be more than 8 s longer or shorter (another version
	 * like live or sped up has other timings).
	 */
	static double score(JsonObject o, String title, String artist, long seconds) {
		double ts = similarity(cleanTitle(string(o, "trackName")), cleanTitle(title));
		if (ts < 0.72) {
			return Double.NaN;
		}
		double as = 1;
		if (artist != null && !artist.isBlank()) {
			as = artistSimilarity(string(o, "artistName"), artist);
			if (as < 0.5) {
				return Double.NaN;
			}
		}
		double penalty = 0;
		if (seconds > 0 && o.has("duration") && !o.get("duration").isJsonNull()) {
			double d = Math.abs(o.get("duration").getAsDouble() - seconds);
			if (d > 8) {
				return Double.NaN;
			}
			penalty = d * 0.3;
		}
		double exactBonus = o.has("_exact") ? 0.5 : 0;
		return 2 * ts + 1.5 * as - penalty + exactBonus;
	}

	/** Test hook for {@link #score}. */
	public static double scoreForTest(JsonObject o, String title, String artist, long seconds) {
		return score(o, title, artist, seconds);
	}

	private static void apply(Entry e, List<Candidate> ranked, String songKey) {
		List<Candidate> synced = new ArrayList<>();
		for (Candidate c : ranked) {
			if (hasSynced(c.json())) {
				synced.add(c);
			}
		}
		e.candidates = List.copyOf(synced);
		if (!synced.isEmpty()) {
			int index = 0;
			long chosen = MusicData.choice(songKey);
			for (int i = 0; i < synced.size(); i++) {
				if (synced.get(i).id() == chosen) {
					index = i;
				}
			}
			use(e, index);
			return;
		}
		if (ranked.stream().anyMatch(c -> isInstrumental(c.json()))) {
			e.track = new Track(List.of(), true);
			e.state = State.READY;
			return;
		}
		boolean plain = ranked.stream().anyMatch(c -> c.json().has("plainLyrics") && !c.json().get("plainLyrics").isJsonNull());
		e.state = plain ? State.PLAIN_ONLY : State.MISSING;
	}

	private static void use(Entry e, int index) {
		Track track = parse(e.candidates.get(index).json().get("syncedLyrics").getAsString());
		e.index = index;
		if (track.lines().isEmpty()) {
			e.state = State.MISSING;
			return;
		}
		e.track = track;
		e.state = State.READY;
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

	// ------------------------------------------------------------------ matching

	/** Lower case, no accents, "&" = "and", only letters and digits separated by single spaces. */
	public static String normalize(String s) {
		if (s == null) {
			return "";
		}
		String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		n = n.toLowerCase(Locale.ROOT).replace("&", " and ").replace("'", "").replace("’", "");
		n = n.replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
		return n;
	}

	private static int levenshtein(String a, String b) {
		int[] prev = new int[b.length() + 1];
		int[] cur = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			prev[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			cur[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
			}
			int[] t = prev;
			prev = cur;
			cur = t;
		}
		return prev[b.length()];
	}

	/** 0..1: 1 = same text (after {@link #normalize}). */
	public static double similarity(String a, String b) {
		String x = normalize(a);
		String y = normalize(b);
		if (x.isEmpty() || y.isEmpty()) {
			return 0;
		}
		if (x.equals(y)) {
			return 1;
		}
		if (x.replace(" ", "").equals(y.replace(" ", ""))) {
			return 0.98;
		}
		double lev = 1 - levenshtein(x, y) / (double) Math.max(x.length(), y.length());
		// same words in another order ("the" and "a" do not count)
		java.util.Set<String> wx = new java.util.HashSet<>(List.of(x.split(" ")));
		java.util.Set<String> wy = new java.util.HashSet<>(List.of(y.split(" ")));
		wx.removeAll(List.of("the", "a"));
		wy.removeAll(List.of("the", "a"));
		double dice = 0;
		if (!wx.isEmpty() && !wy.isEmpty()) {
			java.util.Set<String> both = new java.util.HashSet<>(wx);
			both.retainAll(wy);
			dice = 2.0 * both.size() / (wx.size() + wy.size());
		}
		return Math.max(lev, dice);
	}

	/** Best similarity between any artist of {@code a} and any artist of {@code b} ("A, B & C", "A feat. B"). */
	public static double artistSimilarity(String a, String b) {
		double best = similarity(a, b);
		for (String x : a.split("(?i),|;|\\s&\\s|\\s(feat\\.?|ft\\.?|featuring|x|with|and|und)\\s|\\s/\\s")) {
			for (String y : b.split("(?i),|;|\\s&\\s|\\s(feat\\.?|ft\\.?|featuring|x|with|and|und)\\s|\\s/\\s")) {
				best = Math.max(best, similarity(x, y));
			}
		}
		return best;
	}

	/**
	 * Title and artist to search lyrics for. Video sites send things like "Artist - Song (Official Video)" with the
	 * channel ("ArtistVEVO", "Artist - Topic") as artist; this turns them into the song and the real artist.
	 */
	public static String[] query(String title, String artist, String source) {
		String t = title == null ? "" : title;
		String a = artist == null ? "" : artist;
		t = t.replaceAll("(?i)\\s*[(\\[][^)\\]]*(official|video|audio|lyrics?|visuali[sz]er|clip)[^)\\]]*[)\\]]", "");
		t = t.replaceAll("(?i)\\s*[(\\[]\\s*(hd|hq|4k|mv|m/v)\\s*[)\\]]", "");
		t = t.replaceAll("\\s+[|｜].*$", "").strip();
		a = a.replaceAll("(?i)\\s*-\\s*topic$", "").replaceAll("(?i)\\s*vevo$", "").replaceAll("(?i)\\s+official$", "").strip();
		java.util.regex.Matcher dash = Pattern.compile("\\s[-–—]\\s").matcher(t);
		if (dash.find()) {
			String left = t.substring(0, dash.start()).strip();
			String right = t.substring(dash.end()).strip();
			boolean video = "Browser".equals(source) || a.isEmpty() || artistSimilarity(left, a) >= 0.6;
			if (video && !left.isEmpty() && !right.isEmpty()) {
				a = left;
				t = right;
			}
		}
		if (t.isEmpty()) {
			t = title == null ? "" : title.strip();
		}
		return new String[] {t, a};
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
