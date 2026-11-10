# Nova Client

PvP- und Quality-of-Life-Client-Mod für **Minecraft 26.3** (Fabric).

## Installation
1. Fabric Loader **0.20.1+** für Minecraft 26.3 installieren
2. **Fabric API** (26.3) in den `mods`-Ordner legen
3. `nova-client-1.8.8.jar` in den `mods`-Ordner legen
4. Java 25 (bringt der offizielle Launcher automatisch mit)

## Bedienung
| Taste / Ort | Funktion |
|---|---|
| Rechte Umschalttaste | Startmenü: **1 Module** (Karte anklicken = an/aus, Rechtsklick oder ☰ = Einstellungen, lostippen = Suche), **2 Cosmetics**, **3 Freunde**, HUD-Editor |
| Startmenü → Freunde | Freunde per Name oder vom Server hinzufügen, Anfragen annehmen, sehen wer online ist und wo, „Beitreten“ |
| C (halten) | Zoom, Mausrad = Zoomstärke |
| V | Emote-Rad mit Seiten (26 Emotes: Winken, Jubeln, Tanzen, Klatschen, Facepalm, Zeigen, Verbeugen, Salutieren, Zombie, Schulterzucken, Nachdenken, Headbangen, Luftgitarre, Muskeln zeigen, Panik, Dab, Hinsetzen, Meditieren, Schlafen, Hampelmann, T-Pose, Roboter, Lachen, Weinen, Disco, Pirouette) |
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

## Module (96)
- **HUD:** FPS, Ping, CPS, Koordinaten (+ Richtung, Nether-Umrechnung, Chunk), Keystrokes, Rüstungsstatus, Trankeffekte, Uhrzeit, Datum, Server-IP, Spieler online, Wetter, Geschwindigkeit, RAM, Tag-Zähler, Biom, Lichtlevel, Sättigung, Pfeile, Totem-Zähler, Item-Zähler, Spielzeit, Reichweite, Combo, Ziel-HUD, Kompassleiste, Block-Info, Inventar-Vorschau, Stoppuhr, Wegpunkt, **Musik** (Song mit Cover, Zeit und Fortschritt), **Ingame-Zeit**, **Mondphase**, **Blickrichtung**, **Erfahrung**, **Item-Haltbarkeit**, **Minimap**, **Bewegungsstatus**, **Hotbar-Nummern**, **Songtext** (Karaoke am Himmel), **TPS**, **Sitzungsstatistik**, Theme
- **Visuals:** Zoom, Freelook, Custom Crosshair, Fullbright, No Hurt Cam, Low Fire, HUD-Tweaks, Kein Regen, Blockumrandung, Overlays (Kürbis/Pulverschnee/Unterwasser/Vignette), **Hand-Größe**, **Schadenszahlen**, **Leben über Spielern**, **Rückblick**, **Item-Physik**, **Scoreboard-Zahlen aus**, **Trefferanzeige**, **FPS-Boost**, **Kein Nebel**
- **Utility:** Toggle Sprint, Toggle Sneak, Benachrichtigungen, Todes-Koordinaten, Haltbarkeitswarnung, Chat-Zeitstempel, Chat-Makros, Hintergrund-FPS, **Nova-Netzwerk**, **Tab-Ping in ms**, **Koordinaten teilen**, **Auto-GG**, **Fenstertitel**, **Chat-Erwähnungen**, **Kompakter Chat**, **Langer Chat-Verlauf**, **Freunde**, **Pausen-Erinnerung**, **Totem-Pops**, **Chat-Filter**, **Auto-Respawn**, **Hunger-Warnung**
- **Cosmetics:** Cape (14 Designs + **24 animierte** + wechselnd), **Kopfschmuck in 3D** (Bandana, Krone mit einfärbbaren Steinen, Zylinder, Teufelshörner, Katzenohren, Heiligenschein, Partyhut, Cowboyhut, Hasenohren, Kopfhörer, Sonnenbrille, Wikingerhelm, Blumenkranz, Mütze, Hexenhut, Weihnachtsmütze, Kochmütze, Fuchsohren, Einhorn-Horn, Propellermütze, Pilzhut, Piratenhut, Alien-Antennen, Doktorhut, Irokese, Cyber-Visier, Quietsche-Ente, Maske – in 11 Farben), **animierte Flügel** (Engel, Drache, Schmetterling, Fledermaus, Fee, Phönix mit Flammen, Eis, Libelle – einfärbbar), **Extras** (Rucksack, Katzen-, Fuchs- und Teufelsschwanz, Schal, Jetpack mit Flammen, Gitarre, Rückenschwert, Rückenschild, Köcher, Luftballon), **Minimee** (Mini-Spieler mit beliebigem Skin: Kette, Hosentasche, auf dem Kopf, auf der Schulter, Huckepack, in der Hand oder schwebend – mit 5 Animationen und 3 Größen), **Emotes**, Nametags (Namensfarbe + 11 animierte Effekte: Farbverlauf, Regenbogen-Welle, Metallic, Pulsieren, Schimmer, Blinken, Chromatisch, Feuer, Leuchten, Buchstabe für Buchstabe, Gold-Stern; fett/schräg; **eigene Icons** vor dem Namen: PNGs in `config/novaclient/icons/` legen – Befehl `/icons` zeigt den Ordner, mehrere Bilder untereinander wechseln sich ab), **50 Begleiter** (Laufwesen laufen neben dir, Flugtiere fliegen, Blobfisch, Otter und **24 CUBEES** mit eigenen Animationen), Partikel-Ring, Partikel-Spur, Treffer-Partikel, **Kill-Effekt**

## Songtext am Himmel
Das Modul **Songtext** (HUD) zeigt den Text deines aktuellen Songs (Spotify, Browser, … – wie beim Musik-Modul) wie Karaoke groß am Himmel
vor dir: die gesungenen Buchstaben füllen sich mit Farbe, die nächste Zeile steht klein darunter, bei Pausen zählen drei Punkte herunter.
Einstellbar: Farbe (Theme, Cover, Regenbogen, Weiß), Größe, Höhe, ob der Text dir folgt, durch Blöcke sichtbar, Zeitversatz und
zusätzlich im HUD. Die Texte kommen von [LRCLIB](https://lrclib.net) (kostenlos, ohne Konto; gesendet werden nur Titel, Interpret und Länge).
Stile: Karaoke, Wort für Wort, Ganze Zeile. Neue Zeilen erscheinen kurz bevor sie gesungen werden.

**Passt der Songtext nicht genau?** Pro Song einstellbar und gespeichert:
- Im **Musik-Menü** (Startmenü → 4) auf die Zeile klicken, die gerade gesungen wird – fertig.
- Tasten „Songtext früher/später“ (je 0,1 s, im Modul Songtext belegen) oder `/songtext früher|später [Sekunden]`.
- Falscher Text oder andere Version: „Anderer Songtext“ bzw. `/songtext anders`; `/songtext aus|an|neu|zurücksetzen`.
Gefunden wird nur ein Text, bei dem Titel, Interpret und Länge (±8 s) passen. Videotitel wie „Interpret - Song (Official Video)“ werden erkannt.

## Musik-Menü
Startmenü (Rechte Umschalttaste) → **Musik** (Taste 4): aktueller Song mit Cover und Zeit, **Play/Pause, Weiter, Zurück**
(Windows-Mediensteuerung, Linux playerctl, macOS Spotify/Musik), der ganze Songtext mit der aktuellen Zeile, **Verlauf**,
**Top-Songs** mit Hörzeit und was **Nova-Spieler in deiner Nähe** gerade hören. Dazu `/song`, eine Meldung bei jedem neuen Song und
Tasten für Play/Pause, Weiter und Zurück (im Modul Musik belegen). Gespeichert wird in `config/novaclient/music.json`.

## Mehr FPS
- **FPS-Boost** (standardmäßig an): weit entfernte Entities und Block-Entities (Kisten, Schilder, …) werden nicht gezeichnet, Partikel begrenzt.
- **Nova Launcher**: Schalter „Performance-Mods“ pro Profil (standardmäßig an) – installiert und aktualisiert automatisch
  Sodium, Lithium, FerriteCore, EntityCulling und ImmediatelyFast von Modrinth. Eigene Kopien dieser Mods im `mods`-Ordner werden erkannt.

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

## Credits
- Blobfisch-Modell: **QueenOfAnts** (qpack_blobfish, „free to use/edit … on your projects“), für Nova umgewandelt mit
  `tools/assets/bbmodel_pet.py`.
- CUBEES-Begleiter (Starter, Biomes 1, Currets – 24 Modelle mit Texturen und Animationen): **Cubes Studio**
  ([MCModels](https://mcmodels.net)). Die Lizenz der Pakete verbietet eigentlich die Nutzung in eigenen Produkten und die
  Weitergabe; Cubes Studio hat Nova die Nutzung und die Veröffentlichung in diesem öffentlichen Repo am 06.11.2026 auf Discord
  ausdrücklich erlaubt („Yes its Okay Claude can put it in a Public Reposity. Have Fun with your Client -Cubes Studio“).
  Die Erlaubnis gilt nur für Nova Client – bitte die Modelle nicht aus diesem Repo für andere Projekte übernehmen.
  Siehe auch `src/main/resources/assets/novaclient/models/pets/NOTICE.txt`.
- Otter-Begleiter („Sleepy the Otter“ 3.0.0): **Nocsy** ([MCModels](https://mcmodels.net)). Nocsy hat Nova die Nutzung und die
  Veröffentlichung in diesem öffentlichen Repo am 06.11.2026 auf Discord erlaubt („Sure, you can use the models, and Claude can add them
  to the repo“). Auch diese Erlaubnis gilt nur für Nova Client.
- Launcher-Schrift: [Monocraft](https://github.com/IdreesInc/Monocraft) (SIL OFL 1.1).
