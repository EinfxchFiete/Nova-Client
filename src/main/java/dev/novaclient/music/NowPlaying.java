package dev.novaclient.music;

import dev.novaclient.NovaClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Detects the song that is currently playing on this computer.
 * Windows: Windows media controls (Spotify, browsers, most music apps) via a hidden PowerShell process,
 * with Spotify's window title as fallback. Linux: playerctl (MPRIS). macOS: Spotify / Music via osascript.
 */
public final class NowPlaying {
	/**
	 * A song. {@code positionMs}/{@code lengthMs} are -1 if unknown; the position was measured at {@code sampledAt}
	 * (System.currentTimeMillis) and moves on while the song is playing, see {@link #positionNow()}.
	 */
	public record Song(String title, String artist, boolean playing, String source, long positionMs, long lengthMs, long sampledAt) {
		public Song(String title, String artist, boolean playing, String source) {
			this(title, artist, playing, source, -1, -1, System.currentTimeMillis());
		}

		public boolean hasTimeline() {
			return lengthMs > 0 && positionMs >= 0;
		}

		/** Current position in ms (extrapolated), -1 if unknown. */
		public long positionNow() {
			if (!hasTimeline()) {
				return -1;
			}
			long p = positionMs + (playing ? System.currentTimeMillis() - sampledAt : 0);
			return Math.max(0, Math.min(lengthMs, p));
		}

		/** "1:23 / 3:45", or "" if unknown. */
		public String timestamp() {
			return hasTimeline() ? time(positionNow()) + " / " + time(lengthMs) : "";
		}

		public static String time(long ms) {
			long s = ms / 1000;
			return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
		}
	}

	private static volatile Song current;
	private static volatile Song override;
	private static volatile boolean running;
	private static volatile String status = "Aus";
	private static Thread thread;
	private static Process process;

	/** Windows PowerShell 5.1 script: prints "title<TAB>artist<TAB>status<TAB>source<TAB>positionMs<TAB>lengthMs" every 2 seconds. */
	private static final String WINDOWS_SCRIPT = String.join("\n",
		"$ErrorActionPreference = 'SilentlyContinue'",
		"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8",
		"$mgr = $null",
		"try {",
		"  Add-Type -AssemblyName System.Runtime.WindowsRuntime",
		"  $asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]",
		"  function Await($op, [Type]$type) { $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op)); $t.Wait(-1) | Out-Null; $t.Result }",
		"  [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]",
		"  $mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
		"} catch { $mgr = $null }",
		"while ($true) {",
		"  $line = ''",
		"  try {",
		"    if ($mgr -ne $null) {",
		"      $s = $mgr.GetCurrentSession()",
		"      if ($s -ne $null) {",
		"        $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
		"        $st = $s.GetPlaybackInfo().PlaybackStatus",
		"        $pos = -1; $len = -1",
		"        try {",
		"          $tl = $s.GetTimelineProperties()",
		"          $len = [long]($tl.EndTime - $tl.StartTime).TotalMilliseconds",
		"          $pos = [long]$tl.Position.TotalMilliseconds",
		"          if ($st -eq 'Playing') { $pos += [long]([DateTimeOffset]::Now - $tl.LastUpdatedTime).TotalMilliseconds }",
		"          if ($len -le 0) { $pos = -1; $len = -1 } elseif ($pos -gt $len) { $pos = $len }",
		"        } catch { $pos = -1; $len = -1 }",
		"        if ($p -ne $null -and $p.Title) { $line = $p.Title + \"`t\" + $p.Artist + \"`t\" + $st + \"`t\" + $s.SourceAppUserModelId + \"`t\" + $pos + \"`t\" + $len }",
		"      }",
		"    }",
		"  } catch { $line = '' }",
		"  if ($line -eq '') {",
		"    $w = Get-Process -Name Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like '* - *' } | Select-Object -First 1",
		"    if ($w -ne $null) { $parts = $w.MainWindowTitle -split ' - ', 2; $line = $parts[1] + \"`t\" + $parts[0] + \"`tPlaying`tSpotify\" }",
		"  }",
		"  # stop as soon as Minecraft no longer reads our output (game closed)",
		"  try { [Console]::Out.WriteLine($line); [Console]::Out.Flush() } catch { break }",
		"  Start-Sleep -Seconds 2",
		"}");

	private NowPlaying() {
	}

	/** The current song, or null if nothing is playing / detection is off. */
	public static Song current() {
		Song o = override;
		return o != null ? o : current;
	}

	public static String status() {
		return status;
	}

	/** For tests and previews: pretend this song is playing (null = real detection). */
	public static void setOverride(Song song) {
		override = song;
	}

	private static boolean hookInstalled;

	public static synchronized void start() {
		if (running) {
			return;
		}
		if (!hookInstalled) {
			hookInstalled = true;
			// make sure the helper process never outlives the game
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				Process p = process;
				if (p != null) {
					p.destroyForcibly();
				}
			}, "Nova Now Playing cleanup"));
		}
		running = true;
		thread = new Thread(NowPlaying::run, "Nova Now Playing");
		thread.setDaemon(true);
		thread.start();
	}

	public static synchronized void stop() {
		running = false;
		current = null;
		status = "Aus";
		if (process != null) {
			process.destroy();
			process = null;
		}
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}

	private static void run() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		while (running) {
			try {
				if (os.contains("win")) {
					runWindows();
				} else if (os.contains("mac")) {
					poll(new String[] {"osascript", "-e", MAC_SCRIPT}, 1);
				} else {
					// position and mpris:length are in microseconds
					poll(new String[] {"playerctl", "metadata", "--format",
						"{{title}}\t{{artist}}\t{{status}}\t{{playerName}}\t{{position}}\t{{mpris:length}}"}, 1000);
				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				status = "Nicht verfügbar: " + e.getMessage();
				current = null;
			}
			sleep(10_000L);
		}
	}

	private static final String MAC_SCRIPT = "if application \"Spotify\" is running then\n"
		+ "tell application \"Spotify\" to return (name of current track) & tab & (artist of current track) & tab & (player state as string) & tab & \"Spotify\" & tab & ((player position * 1000) as integer) & tab & (duration of current track)\n"
		+ "else if application \"Music\" is running then\n"
		+ "tell application \"Music\" to return (name of current track) & tab & (artist of current track) & tab & (player state as string) & tab & \"Music\" & tab & ((player position * 1000) as integer) & tab & ((duration of current track * 1000) as integer)\n"
		+ "end if";

	private static void runWindows() throws Exception {
		String encoded = Base64.getEncoder().encodeToString(WINDOWS_SCRIPT.getBytes(StandardCharsets.UTF_16LE));
		ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
			"-WindowStyle", "Hidden", "-EncodedCommand", encoded);
		pb.redirectErrorStream(false);
		pb.redirectError(ProcessBuilder.Redirect.DISCARD);
		Process p = pb.start();
		process = p;
		status = "Aktiv (Windows-Mediensteuerung)";
		try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while (running && (line = r.readLine()) != null) {
				current = parse(line, 1);
			}
		} finally {
			p.destroy();
			if (process == p) {
				process = null;
			}
		}
		if (running) {
			status = "PowerShell beendet – neuer Versuch in 10 s";
		}
	}

	/** {@code unitsPerMs}: how many units of the position/length fields are one millisecond (playerctl: microseconds). */
	private static void poll(String[] command, long unitsPerMs) throws Exception {
		status = "Aktiv";
		while (running) {
			Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
			String line;
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
				line = r.readLine();
			}
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
			}
			current = line == null ? null : parse(line, unitsPerMs);
			Thread.sleep(2000L);
		}
	}

	/** Parses "title<TAB>artist<TAB>status<TAB>source[<TAB>position<TAB>length]". */
	static Song parse(String line, long unitsPerMs) {
		if (line == null || line.isBlank()) {
			return null;
		}
		String[] p = line.split("\t", -1);
		String title = clean(p[0]);
		if (title.isEmpty()) {
			return null;
		}
		String artist = p.length > 1 ? clean(p[1]) : "";
		String state = p.length > 2 ? p[2].trim().toLowerCase(Locale.ROOT) : "playing";
		boolean playing = state.startsWith("play") || state.equals("4");
		String source = p.length > 3 ? sourceName(p[3]) : "";
		long pos = p.length > 5 ? number(p[4]) : -1;
		long len = p.length > 5 ? number(p[5]) : -1;
		if (pos < 0 || len <= 0) {
			pos = -1;
			len = -1;
		} else {
			pos /= unitsPerMs;
			len /= unitsPerMs;
		}
		return new Song(title, artist, playing, source, Math.min(pos, len), len, System.currentTimeMillis());
	}

	private static long number(String s) {
		try {
			return (long) Double.parseDouble(s.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static String sourceName(String id) {
		String s = id.toLowerCase(Locale.ROOT);
		if (s.contains("spotify")) {
			return "Spotify";
		}
		if (s.contains("chrome") || s.contains("msedge") || s.contains("firefox") || s.contains("opera") || s.contains("brave")) {
			return "Browser";
		}
		if (s.contains("music") || s.contains("zune")) {
			return "Musik-App";
		}
		return clean(id.length() > 20 ? id.substring(0, 20) : id);
	}

	private static String clean(String s) {
		String t = s.replaceAll("[\\p{Cntrl}\\u00a7]", "").trim();
		return t.length() > 80 ? t.substring(0, 80) : t;
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	static {
		NovaClient.LOGGER.debug("NowPlaying ready");
	}
}
