# Nova Client

PvP- und Quality-of-Life-Client-Mod für **Minecraft 26.3** (Fabric).

## Installation
1. Fabric Loader **0.20.1+** für Minecraft 26.3 installieren
2. **Fabric API** (26.3) in den `mods`-Ordner legen
3. `nova-client-1.2.0.jar` in den `mods`-Ordner legen
4. Java 25 (bringt der offizielle Launcher automatisch mit)

## Bedienung
| Taste / Ort | Funktion |
|---|---|
| Rechte Umschalttaste | Menü öffnen: Karte anklicken = an/aus, Rechtsklick oder ☰ = Einstellungen, einfach lostippen = Suche |
| C (halten) | Zoom, Mausrad = Zoomstärke |
| Linke Alt-Taste (halten) | Freelook – Taste im Menü änderbar (auch Maustaste 4/5) |
| Menü → HUD-Editor | Elemente ziehen, „Elemente“ = HUD-Module an/aus, „Anordnen“ = automatisch ohne Überlappung |
| Menü → Profile | Profile anlegen, laden, löschen; Vorlagen PvP / Survival / Minimal |
| Menü → Design | Akzentfarbe per Farbfeld, Farbton, Sättigung, Helligkeit, Regenbogen, Menü-Deckkraft |

Jedes Modul kann eine eigene An/Aus-Taste bekommen. Profile liegen in `config/novaclient/profiles/`.

## Module (45)
- **HUD:** FPS, Ping, CPS, Koordinaten + Richtung, Keystrokes, Rüstungsstatus, Trankeffekte, Uhrzeit, Datum, Server-IP, Geschwindigkeit, RAM, Tag-Zähler, Biom, Lichtlevel, Sättigung, Pfeile, Totem-Zähler, Item-Zähler, Spielzeit, Reichweite, Combo, Ziel-HUD, Kompassleiste, Theme
- **Visuals:** Zoom, Freelook, Custom Crosshair, Fullbright, No Hurt Cam, Low Fire, HUD-Tweaks, Kein Regen, Blockumrandung, Overlays (Kürbis/Pulverschnee/Vignette)
- **Utility:** Toggle Sprint, Toggle Sneak, Benachrichtigungen, Todes-Koordinaten, Haltbarkeitswarnung, Chat-Zeitstempel
- **Cosmetics:** Cape (10 Designs, auch auf Elytren), Nametags (eigener Name in F5 mit ✦), Partikel-Spur, Treffer-Partikel

Cosmetics sind rein clientseitig: Nur du siehst sie.

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
