# Nova Client

PvP- und Quality-of-Life-Client-Mod für **Minecraft 26.3** (Fabric).

## Installation
1. Fabric Loader **0.20.1+** für Minecraft 26.3 installieren
2. **Fabric API** (26.3) in den `mods`-Ordner legen
3. `nova-client-1.0.0.jar` in den `mods`-Ordner legen
4. Java 25 (bringt der offizielle Launcher automatisch mit)

## Bedienung
| Taste | Funktion |
|---|---|
| Rechte Umschalttaste | Menü öffnen (Linksklick = an/aus, Rechtsklick = Einstellungen) |
| C (halten) | Zoom, Mausrad = Zoomstärke |
| Menü → HUD-Editor | HUD-Elemente per Drag & Drop verschieben |

Jedes Modul kann eine eigene Taste bekommen. Einstellungen landen in `config/novaclient.json`.

## Module
- **HUD:** FPS, Ping, CPS, Koordinaten + Richtung, Keystrokes, Rüstungsstatus, Trankeffekte, Uhrzeit, Server-IP, Geschwindigkeit, RAM, Tag-Zähler, Theme (Akzentfarbe/Regenbogen)
- **Visuals:** Zoom, Fullbright, No Hurt Cam, Low Fire
- **Utility:** Toggle Sprint, Toggle Sneak

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
