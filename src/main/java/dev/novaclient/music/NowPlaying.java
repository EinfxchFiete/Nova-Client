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
	public record Song(String title, String artist, boolean playing, String source) {
	}

	private static volatile Song current;
	private static volatile Song override;
	private static volatile boolean running;
	private static volatile String status = "Aus";
	private static Thread thread;
	private static Process process;

	/** Windows PowerShell 5.1 script: prints "title<TAB>artist<TAB>status<TAB>source" every 3 seconds. */
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
		"        if ($p -ne $null -and $p.Title) { $line = $p.Title + \"`t\" + $p.Artist + \"`t\" + $st + \"`t\" + $s.SourceAppUserModelId }",
		"      }",
		"    }",
		"  } catch { $line = '' }",
		"  if ($line -eq '') {",
		"    $w = Get-Process -Name Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like '* - *' } | Select-Object -First 1",
		"    if ($w -ne $null) { $parts = $w.MainWindowTitle -split ' - ', 2; $line = $parts[1] + \"`t\" + $parts[0] + \"`tPlaying`tSpotify\" }",
		"  }",
		"  # stop as soon as Minecraft no longer reads our output (game closed)",
		"  try { [Console]::Out.WriteLine($line); [Console]::Out.Flush() } catch { break }",
		"  Start-Sleep -Seconds 3",
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
					poll(new String[] {"osascript", "-e", MAC_SCRIPT});
				} else {
					poll(new String[] {"playerctl", "metadata", "--format", "{{title}}\t{{artist}}\t{{status}}\t{{playerName}}"});
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
		+ "tell application \"Spotify\" to return (name of current track) & tab & (artist of current track) & tab & (player state as string) & tab & \"Spotify\"\n"
		+ "else if application \"Music\" is running then\n"
		+ "tell application \"Music\" to return (name of current track) & tab & (artist of current track) & tab & (player state as string) & tab & \"Music\"\n"
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
				current = parse(line);
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

	private static void poll(String[] command) throws Exception {
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
			current = line == null ? null : parse(line);
			Thread.sleep(3000L);
		}
	}

	/** Parses "title<TAB>artist<TAB>status<TAB>source". */
	static Song parse(String line) {
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
		return new Song(title, artist, playing, source);
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
