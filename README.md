# Nova Client

PvP- und Quality-of-Life-Client-Mod für **Minecraft 26.3** (Fabric).

## Installation
1. Fabric Loader **0.20.1+** für Minecraft 26.3 installieren
2. **Fabric API** (26.3) in den `mods`-Ordner legen
3. `nova-client-1.1.0.jar` in den `mods`-Ordner legen
4. Java 25 (bringt der offizielle Launcher automatisch mit)

## Bedienung
| Taste | Funktion |
|---|---|
| Rechte Umschalttaste | Menü öffnen: Karte anklicken = an/aus, Rechtsklick oder ☰ = Einstellungen, einfach lostippen = Suche |
| C (halten) | Zoom, Mausrad = Zoomstärke |
| Linke Alt-Taste (halten) | Freelook: umsehen, ohne dich zu drehen |
| Menü → HUD-Editor | HUD-Elemente ziehen, „Elemente“ = alle HUD-Module an/aus, Rechtsklick = Einstellungen |

Jedes Modul kann eine eigene Taste bekommen. Einstellungen landen in `config/novaclient.json`.

## Module
- **HUD:** FPS, Ping, CPS, Koordinaten + Richtung, Keystrokes (mit CPS), Rüstungsstatus, Trankeffekte, Uhrzeit, Server-IP, Geschwindigkeit, RAM, Tag-Zähler, Biom, Pfeile, Spielzeit, Reichweite, Combo, Theme (Akzentfarbe/Regenbogen)
- **Visuals:** Zoom, Freelook, Custom Crosshair (6 Stile, 9 Farben), Fullbright, No Hurt Cam, Low Fire, HUD-Tweaks (Scoreboard/Bossleiste/Titel ausblenden), Kein Regen
- **Utility:** Toggle Sprint, Toggle Sneak

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
