# Nova Client

PvP- und Quality-of-Life-Client-Mod für **Minecraft 26.3** (Fabric).

## Installation
1. Fabric Loader **0.20.1+** für Minecraft 26.3 installieren
2. **Fabric API** (26.3) in den `mods`-Ordner legen
3. `nova-client-1.4.1.jar` in den `mods`-Ordner legen
4. Java 25 (bringt der offizielle Launcher automatisch mit)

## Bedienung
| Taste / Ort | Funktion |
|---|---|
| Rechte Umschalttaste | Menü öffnen: Karte anklicken = an/aus, Rechtsklick oder ☰ = Einstellungen, einfach lostippen = Suche |
| C (halten) | Zoom, Mausrad = Zoomstärke |
| V | Emote-Rad (Winken, Jubeln, Tanzen, Klatschen, Facepalm, Zeigen) |
| Linke Alt-Taste (halten) | Freelook – Taste im Menü änderbar (auch Maustaste 4/5) |
| Menü → HUD-Editor | Elemente ziehen, „Elemente“ = HUD-Module an/aus, „Anordnen“ = automatisch ohne Überlappung |
| Menü → Cosmetics | Garderobe mit drehbarer 3D-Vorschau: Capes, Kopfschmuck, Flügel, Emotes, Effekte |
| Menü → Profile | Profile anlegen, laden, löschen; Vorlagen PvP / Survival / Minimal |
| Menü → Design | Akzentfarbe per Farbfeld, Farbton, Sättigung, Helligkeit, Regenbogen, Menü-Deckkraft |

Jedes Modul kann eine eigene An/Aus-Taste bekommen. Profile liegen in `config/novaclient/profiles/`.

## Module (63)
- **HUD:** FPS, Ping, CPS, Koordinaten (+ Richtung, Nether-Umrechnung, Chunk), Keystrokes, Rüstungsstatus, Trankeffekte, Uhrzeit, Datum, Server-IP, Spieler online, Wetter, Geschwindigkeit, RAM, Tag-Zähler, Biom, Lichtlevel, Sättigung, Pfeile, Totem-Zähler, Item-Zähler, Spielzeit, Reichweite, Combo, Ziel-HUD, Kompassleiste, Block-Info, Inventar-Vorschau, Stoppuhr, Wegpunkt, **Musik** (aktueller Song), Theme
- **Visuals:** Zoom, Freelook, Custom Crosshair, Fullbright, No Hurt Cam, Low Fire, HUD-Tweaks, Kein Regen, Blockumrandung, Overlays (Kürbis/Pulverschnee/Unterwasser/Vignette)
- **Utility:** Toggle Sprint, Toggle Sneak, Benachrichtigungen, Todes-Koordinaten, Haltbarkeitswarnung, Chat-Zeitstempel, Chat-Makros, Hintergrund-FPS, **Nova-Netzwerk**, **Tab-Ping in ms**, **Koordinaten teilen**
- **Cosmetics:** Cape (14 Designs + **6 animierte** + wechselnd), **Kopfschmuck in 3D** (Bandana, Krone, Zylinder, Teufelshörner, Katzenohren, Heiligenschein – in 11 Farben), **animierte Flügel** (Engel, Drache, Schmetterling), **Emotes**, Nametags, Begleiter, Aura, Partikel-Ring, Partikel-Spur, Treffer-Partikel

## Nova-Netzwerk (Cosmetics, Songs und Emotes für alle Nova-Spieler)
Damit andere Nova-Spieler deine Cosmetics, Emotes und deinen aktuellen Song **über deinem Namen** sehen, braucht es den
kleinen **Nova-Server** aus dem Ordner [`backend/`](backend/README.md) (kostenlos auf Render.com, Anleitung dort).
Seit 1.4.1 ist der offizielle Nova-Server (`https://nova-server-ifqq.onrender.com`) fest eingetragen – du musst nichts einstellen.
Eine andere Adresse kannst du im Spiel unter *Utility → Nova-Netzwerk* eintragen.
Die Owner **Flipp1009** und **EinfxchFiete** haben eine Krone vor dem Namen, andere Nova-Spieler einen Nova-Stern.

Ohne Nova-Server siehst nur du deine Cosmetics. Spieler ohne Nova Client sehen sie nie.

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
