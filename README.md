# Nova Client

PvP- und Quality-of-Life-Client-Mod für **Minecraft 26.3** (Fabric).

## Installation
1. Fabric Loader **0.20.1+** für Minecraft 26.3 installieren
2. **Fabric API** (26.3) in den `mods`-Ordner legen
3. `nova-client-1.6.0.jar` in den `mods`-Ordner legen
4. Java 25 (bringt der offizielle Launcher automatisch mit)

## Bedienung
| Taste / Ort | Funktion |
|---|---|
| Rechte Umschalttaste | Menü öffnen: Karte anklicken = an/aus, Rechtsklick oder ☰ = Einstellungen, einfach lostippen = Suche |
| C (halten) | Zoom, Mausrad = Zoomstärke |
| V | Emote-Rad (11 Emotes: Winken, Jubeln, Tanzen, Klatschen, Facepalm, Zeigen, Verbeugen, Salutieren, Zombie, Schulterzucken, Nachdenken) |
| H (halten, Modul „Rückblick“) | hinter dich schauen |
| Linke Alt-Taste (halten) | Freelook – Taste im Menü änderbar (auch Maustaste 4/5) |
| Menü → HUD-Editor | Elemente ziehen, „Elemente“ = HUD-Module an/aus, „Anordnen“ = automatisch ohne Überlappung |
| Menü → Cosmetics (oder Tab „Cosmetics“) | Alles an einem Ort mit drehbarer 3D-Vorschau: Capes, Kopfschmuck, Flügel, Minimee, Begleiter, Effekte, Emotes, Extras |
| B | Wegpunkt setzen (beliebig viele, pro Welt/Server gespeichert, Markierung in der Welt) |
| `/wegpunkt` | Wegpunkte auflisten, `setzen <Name>`, `entfernen <Name>`, `alleentfernen` |
| `/playtime` | Spielzeit in dieser Welt / auf diesem Server (wird gespeichert), `/playtime alle` = alle Welten |
| Menü → Profile | Profile anlegen, laden, löschen; Vorlagen PvP / Survival / Minimal |
| Menü → Design | Akzentfarbe per Farbfeld, Farbton, Sättigung, Helligkeit, Regenbogen, Menü-Deckkraft |

Jedes Modul kann eine eigene An/Aus-Taste bekommen. Profile liegen in `config/novaclient/profiles/`.

## Module (80)
- **HUD:** FPS, Ping, CPS, Koordinaten (+ Richtung, Nether-Umrechnung, Chunk), Keystrokes, Rüstungsstatus, Trankeffekte, Uhrzeit, Datum, Server-IP, Spieler online, Wetter, Geschwindigkeit, RAM, Tag-Zähler, Biom, Lichtlevel, Sättigung, Pfeile, Totem-Zähler, Item-Zähler, Spielzeit, Reichweite, Combo, Ziel-HUD, Kompassleiste, Block-Info, Inventar-Vorschau, Stoppuhr, Wegpunkt, **Musik** (Song mit Cover, Zeit und Fortschritt), **Ingame-Zeit**, **Mondphase**, **Blickrichtung**, **Erfahrung**, **Item-Haltbarkeit**, **Minimap**, **Bewegungsstatus**, **Hotbar-Nummern**, Theme
- **Visuals:** Zoom, Freelook, Custom Crosshair, Fullbright, No Hurt Cam, Low Fire, HUD-Tweaks, Kein Regen, Blockumrandung, Overlays (Kürbis/Pulverschnee/Unterwasser/Vignette), **Hand-Größe**, **Schadenszahlen**, **Leben über Spielern**, **Rückblick**
- **Utility:** Toggle Sprint, Toggle Sneak, Benachrichtigungen, Todes-Koordinaten, Haltbarkeitswarnung, Chat-Zeitstempel, Chat-Makros, Hintergrund-FPS, **Nova-Netzwerk**, **Tab-Ping in ms**, **Koordinaten teilen**, **Auto-GG**, **Fenstertitel**, **Chat-Erwähnungen**
- **Cosmetics:** Cape (14 Designs + **16 animierte** + wechselnd), **Kopfschmuck in 3D** (Bandana, Krone mit einfärbbaren Steinen, Zylinder, Teufelshörner, Katzenohren, Heiligenschein, Partyhut, Cowboyhut, Hasenohren, Kopfhörer, Sonnenbrille, Wikingerhelm, Blumenkranz, Mütze – in 11 Farben), **animierte Flügel** (Engel, Drache, Schmetterling, Fledermaus, Fee, Phönix – einfärbbar), **Extras** (Rucksack, Katzen-, Fuchs- und Teufelsschwanz, Schal), **Minimee** (Mini-Spieler mit beliebigem Skin als Kette, an der Hosentasche oder auf dem Kopf), **Emotes**, Nametags, **19 Begleiter**, Partikel-Ring, Partikel-Spur, Treffer-Partikel, **Kill-Effekt**

## Nova-Netzwerk (Cosmetics, Songs und Emotes für alle Nova-Spieler)
Damit andere Nova-Spieler deine Cosmetics, Minimees, Emotes und deinen aktuellen Song (mit Cover und Zeit) **über deinem Namen** sehen, braucht es den
kleinen **Nova-Server** aus dem Ordner [`backend/`](backend/README.md) (kostenlos auf Render.com, Anleitung dort).
Seit 1.4.1 ist der offizielle Nova-Server (`https://nova-server-ifqq.onrender.com`) fest eingetragen – du musst nichts einstellen.
Eine andere Adresse kannst du im Spiel unter *Utility → Nova-Netzwerk* eintragen.
Die Owner **Flipp1009** und **EinfxchFiete** haben eine Krone vor dem Namen, andere Nova-Spieler einen Nova-Stern.

Ohne Nova-Server siehst nur du deine Cosmetics. Spieler ohne Nova Client sehen sie nie.
Song-Cover werden über die öffentliche iTunes-Suche von Apple geladen (nur Titel + Interpret, abschaltbar im Musik-Modul).

> Hinweis: Manche Server (z. B. Hypixel) verbieten Freelook oder Fullbright. Prüfe vorher die Serverregeln.

## Bauen
```bash
./gradlew build
```
Die fertige Jar liegt danach in `build/libs/`.

## Automatischer Ingame-Test
```bash
./gradlew runClientGameTest
```
Startet Minecraft, erstellt eine Testwelt, prüft alle Module und speichert Screenshots in `build/run/clientGameTest/screenshots/`. Der Testcode (`src/gametest`) ist nicht Teil der veröffentlichten Jar.
