# CLAUDE.md – Übergabe-Dokumentation „Nova Client“

> Diese Datei fasst den kompletten ersten Entwicklungs-Chat zusammen (Start bis Übergabe, 02.–03.11.2026).
> Ein neuer Chat hat **nur** dieses Repo und diese Datei. Unsichere Punkte sind mit **[UNSICHER]** markiert.
> **Das Repo ist öffentlich** – hier stehen bewusst keine Passwörter, Tokens, Kartendaten oder privaten E-Mail-Adressen.

---

## 1. PROJEKTÜBERBLICK

### Was ist Nova Client?
- Ein **eigener Minecraft-Client als Fabric-Mod** für **Minecraft: Java Edition 26.3**, vom Nutzer gewünscht „mit allen
  Features, die ein Client braucht“, **NoRisk Client als Inspiration** (HUD-Module, Zoom, Freelook, Cosmetics …).
- Dazu ein eigener **Windows-Launcher „Nova Launcher“** (Electron) im Stil des NoRisk-Launchers
  (Microsoft-Login, Profile, Skin-Wechsler, automatische Installation).
- Dazu ein kleiner **Nova-Server** (`backend/`), damit Nova-Spieler gegenseitig Cosmetics, Emotes und den gerade
  gehörten Song sehen (wie das NoRisk-Netzwerk).
- Zielgruppe: der Nutzer (Minecraft-Name **EinfxchFiete**, GitHub `EinfxchFiete`) und Freunde, u. a. **Flipp1009**.
  Beide sind die „Owner“ des Clients und bekommen ein Kronen-Abzeichen.
- Name: **„Nova Client“** (bewusst **nicht** das NoRisk-Branding kopiert). Mod-ID `novaclient`, Package `dev.novaclient`.
- Aktuelle Versionen: **Nova Client 1.4.1**, **Nova Launcher 1.1.1**, Nova-Server 1.0.0.
- Repo: <https://github.com/EinfxchFiete/Nova-Client> (öffentlich, Branch `main`).

### Tech-Stack
| Teil | Technik / Versionen |
|---|---|
| Mod | Java **25** (Release 25), Minecraft **26.3** (unobfuskiert, Mojang-Namen, keine Mappings), Fabric Loader **0.20.1**, Fabric API **0.165.0+26.3**, Fabric Loom **1.19-SNAPSHOT** (löst zu 1.19.3 auf; Plugin-ID `net.fabricmc.fabric-loom`), Gradle **9.8.2** (Wrapper im Repo), Mixin (+ MixinExtras über Fabric verfügbar, aber nicht genutzt), Gson (aus Minecraft) |
| Tests Mod | Fabric **Client Gametest API** (`fabric-client-gametest-api-v1`), Xvfb, Mesa llvmpipe |
| Launcher | **Electron 38.4.0**, **@electron/packager 18.3.6**, **skinview3d 3.4.1** (3D-Skinvorschau, bringt three.js mit), **resedit 2.0.3** (exe-Icon ohne Wine), Node.js 22 (im Container v22.23.4), Playwright (im Container vorinstalliert) für UI-Tests. Kein Framework, reines HTML/CSS/JS |
| Schrift Launcher | **Monocraft** (freie Minecraft-Nachbildung, SIL OFL 1.1, `launcher/src/ui/fonts/`) |
| Server | Node.js ≥ 20, **keine Abhängigkeiten** (`http`, `crypto`, `fetch`), Hosting geplant auf **Render.com Free** (`render.yaml`) |
| Asset-Generatoren | Python 3 (nur Standardbibliothek), in `tools/assets/` |

### Bauen, starten, testen
**Einmalig in einer neuen Cloud-Session:** Java 25 holen (vorinstalliert ist nur 21):
```bash
source tools/dev-setup.sh            # lädt Temurin JDK 25 nach ~/.nova-dev, setzt JAVA_HOME/PATH/XDG_RUNTIME_DIR
```
**Mod bauen** (Ergebnis `build/libs/nova-client-<version>.jar`):
```bash
./gradlew build
```
Den Quellcode von Minecraft zum Nachschlagen erzeugen (sehr hilfreich, Minecraft 26.3 hat viele neue Namen):
```bash
./gradlew genSources   # Quellen-Jar: .gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.3/*-sources.jar
```
(Kompilierte Klassen zum `javap`: `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/…jar`.)

**Automatischer Ingame-Test** (startet echtes Minecraft, ca. 2–2,5 Minuten, Screenshots in
`build/run/clientGameTest/screenshots/`):
```bash
xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX" ./gradlew runClientGameTest
```
- Braucht `node` im PATH (der Test startet `backend/server.js` lokal mit `NOVA_DEV=1` auf Port 18765).
- Optional echte Musikerkennung unter Linux testen: `PATH=<repo>/tools/testing:$PATH NOVA_FAKE_PLAYERCTL=1`
  (dann liefert `tools/testing/fake-playerctl` einen Fake-Song).
- Erfolg = Logzeile `NOVA GAMETEST PASSED` und `BUILD SUCCESSFUL`.
- Screenshots danach **immer ansehen** (Read-Tool), viele Fehler sieht man nur im Bild.

**Server testen / lokal starten:**
```bash
cd backend && npm test        # Tests mit nachgebautem Mojang-Session-Server
node server.js                # Port 8080 (PORT=…), NOVA_DEV=1 nur lokal!
```
**Launcher:**
```bash
cd launcher && npm install
npm start                                            # Launcher starten (Linux/Container: xvfb + --no-sandbox nötig)
xvfb-run -a -s "-screen 0 1400x900x24" node test/ui-test.js              # UI-Test (Playwright), Screenshots test/output/ui/
tools/testing/build-headless-mod.sh /tmp/headless.jar                    # (im Repo-Root, nach ./gradlew build)
xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX" node test/launch-test.js /tmp/headless.jar      # Installation + Spielstart
xvfb-run -a -s "-screen 0 1400x900x24 +extension GLX" node test/ui-launch-test.js /tmp/headless.jar   # STARTEN-Klick in der UI
npm run dist:win                                     # Windows-Build nach dist/Nova Launcher-win32-x64/
node scripts/set-exe-icon.js "dist/Nova Launcher-win32-x64/Nova Launcher.exe"   # Icon + Versionsinfo (aus package.json)
```
ZIP für den Nutzer (so wurde es immer gemacht):
```bash
cd dist && python3 -c "
import zipfile,os
z=zipfile.ZipFile('Nova-Launcher-Windows.zip','w',zipfile.ZIP_DEFLATED,compresslevel=6)
root='Nova Launcher-win32-x64'
for d,_,fs in os.walk(root):
    for f in fs:
        p=os.path.join(d,f); z.write(p, os.path.join('Nova Launcher', os.path.relpath(p, root)))
z.close()"
```
**Wichtig:** Vor jedem Launcher-Build die aktuelle Mod-Jar nach `launcher/resources/nova-client.jar` kopieren
(der Launcher liefert sie mit). Der Nutzer bekommt am Ende immer **beide** Dateien: die `.jar` und die ZIP mit der `.exe`.

---

## 2. ARCHITEKTUR & STRUKTUR

### Ordnerstruktur (wichtige Dateien)
```
build.gradle, settings.gradle, gradle.properties   Gradle/Loom; Versionen in gradle.properties; fabricApi.configureTests (Gametest-SourceSet)
gradlew, gradle/wrapper/                           Gradle 9.8.2 Wrapper
render.yaml                                        Render-Blueprint für den Nova-Server (rootDir backend, plan free)
README.md                                          Nutzer-Doku (Deutsch): Installation, Tasten, Modulliste
CLAUDE.md                                          diese Übergabe
src/main/java/dev/novaclient/
  NovaClient.java            Einstieg (ClientModInitializer): ModuleManager, Config, Menü-Taste, Tick, HUD-Elemente,
                             AttackEntityCallback, Cosmetics-Render-Layer, NovaNetwork.start()
  module/Module.java         Basis: Name (=Config-Schlüssel!), Beschreibung, Kategorie, enabled, Keybind, Settings, tick()
  module/Category.java       HUD, VISUAL("Visuals"), UTILITY, COSMETICS
  module/ModuleManager.java  registriert alle 63 Module, tick(), onKeyPressed() (Modul-Tasten + Benachrichtigung)
  module/hud/                HudModule (Anker-Positionen), TextHudModule, alle HUD-Module, ThemeModule, MusicModule
  module/visual/             Zoom, Freelook, Crosshair, Fullbright, NoHurtCam, LowFire, HudTweaks, NoWeather, BlockOutline, Overlays
  module/utility/            ToggleSprint/Sneak, Notifications, DeathCoords, DurabilityWarning, ChatTimestamps, ChatMacros,
                             BackgroundFps, NetworkModule (Server-Adresse!), TabPingModule, CoordsShareModule
  module/cosmetics/          CapeModule, HeadCosmeticModule, WingsModule, EmotesModule, NametagModule, TrailModule,
                             HitParticlesModule, CompanionModule, AuraModule (2×: "Aura" und "Partikel-Ring")
  setting/                   Setting<T>, BoolSetting, NumberSetting, ModeSetting, KeySetting, TextSetting
  config/ConfigManager.java  Profile: config/novaclient/profiles/<Name>.json + client.json, Vorlagen PvP/Survival/Minimal
  gui/ClickGuiScreen.java    Hauptmenü (Rechte Umschalttaste): Tabs, Suche, Karten, Seiten SETTINGS/PROFILES/DESIGN
  gui/HudEditorScreen.java   HUD verschieben, „Elemente“-Liste, „Anordnen“, Rechtsklick = Einstellungen
  gui/CosmeticsScreen.java   Garderobe mit drehbarer 3D-Spielervorschau
  cosmetics/                 Loadout (was jemand trägt), CosmeticRegistry (IDs/Namen/Farben), CosmeticModels (3D-Modelle),
                             CosmeticsLayer (Render-Layer am Spieler), Cosmetics (lokal vs. Netzwerk), Badges (Owner/Nova-Icons)
  emote/                     Emotes (Animationen, lokal + remote), EmoteWheelScreen
  music/NowPlaying.java      Song-Erkennung (Windows PowerShell/SMTC, Linux playerctl, macOS osascript)
  network/NovaNetwork.java   Client für den Nova-Server (Auth über Mojang joinServer, Sync alle 3 s)
  util/                      KeyState (Tasten/Maus-Zustand + Druckzähler), CpsTracker, CombatTracker, Notifications, Render
  mixin/                     18 Mixins (Liste unten)
src/main/resources/
  fabric.mod.json, novaclient.mixins.json, assets/novaclient/{icon.png, lang/de_de.json, en_us.json}
  assets/novaclient/textures/cape/*.png (14 statische Capes), cape/anim/<name>_<0..15>.png (6×16 Frames)
  assets/novaclient/textures/cosmetics/*.png (bandana, crown, tophat, horns, catears, halo, wings_angel/dragon/butterfly)
  assets/novaclient/textures/gui/sprites/badge/{owner,nova}.png  (9×9, landen automatisch im GUI-Atlas)
src/gametest/                NovaClientGameTest (großer Ablauftest) + mixin/HeadlessGlBackendMixin (nur Test!)
backend/                     server.js, package.json, README.md (Deploy-Anleitung), test/server-test.js
launcher/
  src/main.js                Electron-Hauptprozess: Fenster (frameless), IPC, Kontoverwaltung, Steve-Skin, Spielstart
  src/preload.js             window.nova = { invoke(channel,…), on(event, fn) } (contextIsolation, sandbox)
  src/core/                  paths, net (Downloads+SHA1), minecraft (Version/Libraries/Assets/Rules), fabric, java
                             (Mojang-Java-Runtime), mods (Fabric API von Modrinth + Nova-Jar), launch (Klassenpfad,
                             Argumente, Spawn), auth (Microsoft Device Code → Xbox → XSTS → Minecraft), skins (Upload/Cape), store
  src/ui/                    index.html, styles.css, app.js, fonts/Monocraft.ttf (+ Lizenz)
  resources/                 nova-client.jar (mitgeliefert), icon.png/.ico, default-skin.png (eigener Offline-Ersatz)
  scripts/set-exe-icon.js    Icon+Versionsinfo in die exe (resedit)
  test/                      ui-test.js, launch-test.js, ui-launch-test.js
tools/                       Generatoren & Test-Helfer aus dem Chat (siehe Abschnitt 9)
```

### Zusammenspiel
- **Module** halten Zustand + Settings. `ModuleManager.tick()` ruft jeden Client-Tick (END_CLIENT_TICK) `tick()` aller Module.
- **Tasten**: `KeyboardHandlerMixin` / `MouseHandlerMixin` füttern `KeyState` (gedrückt + Druckzähler). Modul-An/Aus-Tasten
  laufen über `ModuleManager.onKeyPressed` (Event). `KeySetting.consumePress()` vergleicht Druckzähler → keine verpassten
  kurzen Tastendrücke.
- **HUD**: `NovaClient` registriert `HudElementRegistry.addLast("novaclient:hud")`, das alle aktiven `HudModule`s zeichnet
  (nicht bei F1, nicht bei F3, nicht im HUD-Editor). Positionen = Anker (START/CENTER/END) + Pixel-Offset.
  Ersetzt werden Vanilla-Elemente: MOB_EFFECTS (Trankeffekte ausblenden), CROSSHAIR (Custom Crosshair), SCOREBOARD,
  BOSS_BAR, TITLE_AND_SUBTITLE (HUD-Tweaks).
- **Config**: `ConfigManager` speichert pro Profil alle Module (Schlüssel = Modulname, Settings = Settingname).
  **Namen von Modulen/Settings umbenennen ⇒ alte Einstellungen gehen verloren.**
- **Cosmetics**: `Cosmetics.loadoutFor(entityId)` → lokaler Spieler aus den Modulen, andere aus `NovaNetwork`.
  `CosmeticsLayer` (registriert per `LivingEntityRenderLayerRegistrationCallback` für `AvatarRenderer`) zeichnet Kopfschmuck
  + Flügel. Capes laufen über `AbstractClientPlayerMixin.getSkin()` (Cape-Textur im `PlayerSkin`).
- **Netzwerk**: `NovaNetwork.clientTick()` baut auf dem Spielthread einen Snapshot (eigene Cosmetics, Song, Emote, UUIDs der
  Mitspieler), ein Daemon-Thread synchronisiert alle 3 s mit dem Server. Ergebnisse (`RemoteStatus`) werden von
  Cosmetics/Emotes/Badges/Song-Zeile gelesen. Ohne Server-Adresse ist alles aus (nur lokal sichtbar).
- **Menüs**: `ClickGuiScreen` → Buttons „HUD-Editor“, „Cosmetics“, „Profile“, „Design“; `CosmeticsScreen` schreibt direkt
  in die Cosmetic-Module.

### Mixins (Ziel → Zweck)
| Mixin | Ziel | Zweck |
|---|---|---|
| AbstractClientPlayerMixin | `getSkin` RETURN | eigenes Cape / Capes anderer Nova-Spieler |
| AvatarRendererMixin | `submitNameDisplay(AvatarRenderState,PoseStack,SubmitNodeCollector,CameraRenderState)` TAIL | Song-Zeile über dem Namen |
| CameraMixin | `calculateFov` RETURN; `alignWithEntity` vor `Options.getCameraType` (ordinal 0) | Zoom; Freelook-Kamerawinkel |
| ChatComponentMixin | `addMessage(Component,MessageSignature,GuiMessageSource,GuiMessageTag)` ModifyVariable | Chat-Zeitstempel |
| ChatListenerMixin | `handlePlayerChatMessage` HEAD, ModifyVariable auf `ChatType.Bound` | Owner-/Nova-Abzeichen vor dem Absender im Chat (nur echter Spieler-Chat; Plugin-Server, die Chat als Systemnachricht schicken, bekommen kein Abzeichen) |
| EntityMixin | `turn` HEAD | Freelook: Maus dreht Kamera statt Spieler |
| EntityRendererMixin | `getNameTag(Entity)` RETURN | Owner-/Nova-Abzeichen, eigene Namensfarbe |
| GameRendererMixin | `bobHurt` HEAD | No Hurt Cam (Todesanimation bleibt) |
| HudMixin | `extractTextureOverlay`, `extractVignette` HEAD | Kürbis/Pulverschnee/Vignette ausblenden |
| KeyboardHandlerMixin | `keyPress` HEAD | KeyState + Modul-Tasten (action 1=Druck, 0=loslassen, -1=Wiederholung) |
| LevelMixin | `getRainLevel` HEAD | „Kein Regen“ (nur clientseitige Welt) |
| LevelRendererMixin | `submitBlockOutline` → `submitHitOutline` ordinal 1, ModifyArg index 4/5 | Blockumrandung Farbe/Dicke |
| LightmapRenderStateExtractorMixin | `extract` TAIL | Fullbright (nightVisionEffectIntensity=1, brightness=1) |
| LivingEntityRendererMixin | `shouldShowName(LivingEntity,double)` RETURN | eigener Nametag in F5 |
| MouseHandlerMixin | `onButton` HEAD, `onScroll` HEAD | KeyState/CPS; Mausrad beim Zoomen |
| PlayerModelMixin | `setupAnim(AvatarRenderState)` TAIL | Emotes (Arme/Kopf/Beine) |
| PlayerTabOverlayMixin | `getNameForDisplay` RETURN, `extractPingIcon` HEAD | Abzeichen in Tab-Liste, Ping in ms |
| ScreenEffectRendererMixin | `submitFire` HEAD/RETURN, `submitWater` HEAD | Low Fire (Pose absenken), Unterwasser-Overlay aus |
| *(Test)* HeadlessGlBackendMixin | `GlBackend.createWindow` vor `SDL_CreateWindow` | setzt SDL-GL-Attribut 22 (sRGB) auf 0, weil Xvfb kein sRGB kann – **nur im Gametest** |

### Alle 63 Module (Name = Config-Schlüssel, Standard an/aus)
- **HUD:** Theme (an), FPS (an), Ping (an), CPS (an), Koordinaten (an; Settings: Richtung, Nether/Oberwelt umrechnen, Chunk),
  Keystrokes (an), Rüstungsstatus (an), Trankeffekte (an; „Vanilla-Anzeige ausblenden“), Uhrzeit, Server-IP, Geschwindigkeit,
  Arbeitsspeicher, Tag-Zähler, Biom, Pfeile, Spielzeit, Reichweite, Combo, Ziel-HUD, Kompassleiste, Lichtlevel, Sättigung,
  Datum, Totem-Zähler, Block-Info, Inventar-Vorschau, Spieler online, Wetter, Stoppuhr (Num 0 / Num 1), Wegpunkt (B),
  Musik, Item-Zähler
- **Visuals:** Zoom (an, C), Freelook (an, Linke Alt), Custom Crosshair, Fullbright, No Hurt Cam, Low Fire, HUD-Tweaks,
  Kein Regen, Blockumrandung, Overlays
- **Utility:** Toggle Sprint, Toggle Sneak, Benachrichtigungen (an), Todes-Koordinaten (an), Haltbarkeitswarnung (an),
  Chat-Zeitstempel, Chat-Makros, Hintergrund-FPS, Nova-Netzwerk (an), Tab-Ping in ms (an), Koordinaten teilen
- **Cosmetics:** Cape, Kopfschmuck, Flügel, Emotes (an, V), Nametags, Partikel-Spur, Treffer-Partikel, Begleiter, Aura,
  Partikel-Ring
- Menü-Taste: **Rechte Umschalttaste** (einzige Vanilla-KeyMapping `key.novaclient.menu`, Kategorie `key.category.novaclient.main`).

### Wichtige Klassen / Schnittstellen
- `Module`: `getName()`, `isEnabled()/setEnabled()/toggle()`, `loadEnabled()` (ohne Seiteneffekte, Start), `resetToDefaults()`,
  `getKeybind()/setKeybind()`, `tick()`, `onEnable()/onDisable()`.
- `HudModule`: `getWidth(editor)`, `getHeight(editor)`, `renderContent(g,x,y,editor)`, `getScreenX/Y`, `setScreenPos`
  (wählt Anker automatisch nach Bildschirmdrittel), `setPosition(anchorX, x, anchorY, y)`, `resetPosition()`.
  Breite/Höhe 0 ⇒ unsichtbar (z. B. Rüstung leer, keine Effekte).
- `KeyState`: Tastatur = SDL-Scancode, Maus = `1000 + Button` (`MOUSE_OFFSET`); `isDown`, `presses`, `name`.
- `KeySetting.consumePress(inGame)`: true einmal pro neuem Druck.
- `ConfigManager`: `load()` (Start), `save()`, `reload()`, `switchTo(name)`, `createCopy(name)`, `createFromPreset(preset)`,
  `delete(name)`, `listProfiles()`, `sanitize(name)`.
- `Loadout(cape, head, headColor, wings, wingsColor)` + `toJson/fromJson` (validiert über `CosmeticRegistry`).
- `CosmeticRegistry`: `HEADS` (bandana, crown, tophat, horns, catears, halo), `WINGS` (angel, dragon, butterfly),
  `COLORS` (Normal, Rot, Orange, Gelb, Grün, Türkis, Blau, Lila, Pink, Schwarz, Regenbogen=animiert),
  `ANIMATED_CAPES` (anim:nova, anim:galaxy, anim:fire, anim:ocean, anim:rainbow, anim:matrix), `CAPE_FRAMES = 16`.
- `CapeModule`: `idFor(design)`, `texture(id)` (Frame bei animierten Capes: `millis/100 % 16`), `texturePath(id)` (für GUI-blit),
  `withCape(skin, id, elytra)`, `apply(skin)`.
- `Badges`: `OWNERS` (UUIDs), `decorate(uuid, local, name, forNametag)`, `ownerIcon()`, `novaIcon()`.
- `Emotes`: `ALL` (wave 3 s, cheer 3 s, dance 6 s, clap 3 s, facepalm 2,5 s, point 2,5 s), `play(id)`, `stop()`, `tick()`,
  `localActive()`, `apply(PlayerModel, AvatarRenderState)`.
- `NovaNetwork`: `start()`, `clientTick(mc)`, `requestSync()`, `isNovaUser(uuid)`, `loadout(uuid)`, `song(uuid)`, `status(uuid)`,
  `isConnected()`, `lastError()`, `knownPlayers()`, Test-Hook `setSessionForTest(server, uuid, token)`.
- `NowPlaying`: `start()/stop()`, `current()` → `Song(title, artist, playing, source)`, Test-Hook `setOverride(song)`.
- `NetworkModule.DEFAULT_SERVER` = **""** (Platzhalter – nach dem Deploy die Render-URL eintragen, siehe Abschnitt 7).

### Nova-Server-API (`backend/server.js`)
- `GET /`, `GET /health`
- `POST /v1/auth/start {name}` → `{serverId}` (Zufalls-Nonce, 2 min gültig)
- Client: `Minecraft.services().sessionService().joinServer(uuid, accessToken, serverId)` (Token geht **nur an Mojang**)
- `POST /v1/auth/finish {name, serverId}` → Server fragt `sessionserver.mojang.com/session/minecraft/hasJoined` → `{token, uuid}`
- `POST /v1/sync` (Header `Authorization: Bearer <64 hex>`) Body `{status:{cosmetics, song?, emote?{id, age}}, players:[uuid…]}`
  → `{players:{uuid:{cosmetics, song?, emote?{id, age}}}}`
- `POST /v1/auth/dev {uuid, name}` nur bei `NOVA_DEV=1` (lokale Tests!).
- Limits: Body 32 KB, 200 Spieler/Sync, Status-TTL 30 s, Token 24 h, start/finish 20/min/IP, sync 10/5 s/UUID.
  Werte werden gefiltert (nur bekannte IDs, Songtitel 80 / Interpret 60 Zeichen, keine Steuer- und §-Zeichen).
- Env: `PORT`, `NOVA_SESSION_SERVER` (Test), `NOVA_DEV`. Alles im RAM, keine Datenbank.
- Client-Seite: Sync alle 3 s, Daten älter als 20 s gelten als veraltet, Auth-Backoff 30 s verdoppelnd bis 15 min,
  nur `https://` (außer `http://127.0.0.1`/`localhost`).

### Launcher intern
- Datenordner: Windows `%APPDATA%\.novalauncher`, macOS `~/Library/Application Support/novalauncher`, Linux `~/.novalauncher`
  (Override `NOVA_LAUNCHER_HOME` für Tests). Unterordner: `versions/ libraries/ assets/ runtime/ instances/<profilId>/ skins/ cache/steve.png`,
  Datei `launcher.json`.
- `launcher.json`: `settings {clientId, closeOnLaunch, javaPath, defaultRamMb}`, `accounts [{uuid, name, skins, capes,
  refreshToken{enc,data}, mcToken{enc,data}, mcExpiresAt}]`, `activeAccount`, `profiles [{id, name, ramMb, novaEnabled,
  jvmArgs, width, height, color, lastPlayed}]`, `activeProfile`, `skins [{id, name, variant}]`. Tokens über Electron
  `safeStorage` (Windows DPAPI) verschlüsselt.
- IPC (`window.nova.invoke`): `state, window, open-external, settings:set, auth:start, auth:cancel, accounts:select,
  accounts:remove, accounts:refresh, accounts:skin, capes:texture, capes:set, skins:add, skins:addBuffer, skins:texture,
  skins:update, skins:remove, skins:apply, profiles:create, profiles:update, profiles:select, profiles:delete,
  profiles:open, game:launch, game:kill`. Events: `auth:status, game:progress, game:log, game:state`.
- Installation: Mojang-Manifest → Version-JSON 26.3 → Client-Jar, Libraries (OS-Regeln), Assets, Log-Config; Fabric-Profil von
  `meta.fabricmc.net` (neuester stabiler Loader); Java-Runtime `java-runtime-epsilon` aus Mojangs Runtime-Manifest
  (`…/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json`); Fabric API neueste für 26.3 von Modrinth;
  Nova-Jar aus `resources/`. Doppelte Libraries (z. B. ASM) werden per `group:artifact[:classifier]` entfernt, Fabric gewinnt.
- Spiel-Log ist XML (Mojangs log4j-Config) → wird in `app.js` (`parseLogLine`) lesbar gemacht.
- Microsoft-Login: Device Code Flow, Tenant `consumers`, Scope `XboxLive.signin offline_access` → XBL → XSTS
  (`rp://api.minecraftservices.com/`) → `login_with_xbox` → Profil. HTTP 401/403 bei `login_with_xbox` = „Client-ID von Mojang
  noch nicht freigeschaltet“. Skin-Upload `POST /minecraft/profile/skins` (multipart), Cape `PUT/DELETE /minecraft/profile/capes/active`.

---

## 3. ALLE ENTSCHEIDUNGEN (mit Begründung)

1. **Fabric statt Forge**: vom Nutzer gewählt (auf Empfehlung), Fabric API ist für 26.3 sofort verfügbar.
2. **Name „Nova Client“, eigenes Branding**: NoRisk ist nur Inspiration; Logos/Namen anderer Clients werden nicht kopiert.
3. **Keine Cheat-Module** (Killaura, Anticheat-Bypässe) für öffentliche Server – gleich zu Beginn so festgelegt.
4. **Eigenes Gradle-Projekt statt das FabricMC-Beispielrepo auszuführen**: Das Ausführen von `gradlew` aus einem geklonten
   fremden Repo wurde von der Sicherheitsprüfung blockiert („Code from External“). Beispielrepo nur als Vorlage gelesen.
5. **Gradle 9.8.2 + Wrapper im Repo**, **Temurin JDK 25** (Minecraft 26.3 verlangt Java 25).
6. **Tests im echten Spiel** mit Fabric Client Gametest + Xvfb; Grafik-Workaround nur im Test-SourceSet (nie in der Release-Jar).
   Der Gametest bedient alles mit echten Tasten/Klicks und prüft Messwerte (z. B. Zoom-FOV 84°→21°).
7. **HUD-Positionen als Anker + Pixel-Offset** statt Bruchteilen des Bildschirms (Bruchteile führten zu Überlappungen).
8. **Tasten event-basiert** (KeyboardHandler-Mixin, KeyState-Druckzähler) statt Abfrage pro Tick (verpasste kurze Drücke).
9. **Maus-Buttons als Konstanten** `InputConstants.MOUSE_BUTTON_LEFT/RIGHT` (26.x/SDL: links=1, mitte=2, rechts=3).
10. **Zoom/Freelook-Tasten als Nova-`KeySetting`** (im Nova-Menü änderbar, auch Maustasten 4/5) statt Vanilla-KeyMapping –
    nachdem der Nutzer meldete, Freelook gehe nicht. Nur die Menü-Taste ist noch eine Vanilla-KeyMapping.
11. **Freelook**: Kamerarotation in `alignWithEntity` ersetzen + `Entity.turn` abfangen, automatisch 3. Person,
    Bildschirm-Hinweis „Freelook“.
12. **Custom Crosshair** ersetzt das Vanilla-Crosshair-Element komplett (dabei verschwindet auch die Vanilla-Angriffsanzeige
    – bewusst in Kauf genommen [UNSICHER ob dem Nutzer wichtig]).
13. **Fullbright über Nachtsicht-Intensität in der Lightmap** statt Gamma-Wert (Gamma ist in neuen Versionen begrenzt).
14. **Kein Zeit-Wechsler**: Sonne/Zeit laufen in 26.x über Umgebungsattribute/Welt-Uhren, nicht sauber clientseitig änderbar.
15. **Low Fire Standard 0.2** (0.3 schob das Feuer in 26.3 komplett aus dem Bild), Bereich 0.05–0.4.
16. **Benachrichtigungen oben mittig** (unter der Kompassleiste), max. 3 – vorher verdeckten sie die Bildmitte.
17. **Profile** als einzelne JSON-Dateien pro Profil, Vorlagen PvP/Survival/Minimal, alte `config/novaclient.json`
    wird automatisch zu Profil „Standard“ migriert.
18. **Design-Seite** mit 12 Farbvorlagen + Regler (Farbton, Sättigung, Helligkeit, Regenbogen, Menü-Deckkraft).
19. **Cosmetics clientseitig**; andere sehen sie nur über den Nova-Server. **Owner-Abzeichen per UUID** (nicht per Name, Namen
    können sich ändern).
20. **Abzeichen als Grafik im Text** über `Component.object(new AtlasSprite(gui-Atlas, sprite))` – Sprites unter
    `textures/gui/sprites/` landen automatisch im GUI-Atlas; Fallback-Text ♛/✦.
21. **Animierte Capes über Einzelbild-Texturen**, die `getSkin()` pro Bild wechselt (10 fps). `.mcmeta`-Animation geht nur in Atlanten.
22. **Cosmetic-Animationen nur über den PoseStack**, nicht über ModelPart-Rotationen: Rendering ist verzögert (Submit-System),
    geteilte ModelParts hätten bei allen Spielern dieselbe (letzte) Pose. Bewegliche Teile sind eigene ModelParts.
23. **Kopfschmuck wird bei getragenem Helm ausgeblendet** (außer Heiligenschein und Hörner), Flügel bei Elytra.
24. **Begleiter als clientseitige Entities** mit negativer ID (-4 242 042), werden nie an den Server gesendet.
25. **Emote-Taste V**, weil **G in 26.3 „Schnellaktionen“** ist (und **O die Freundesliste**). Emotes drehen die Kamera
    automatisch nach vorne und enden bei Bewegung.
26. **Nova-Netzwerk mit Mojang-`hasJoined`-Prüfung** (wie Minecraft-Server): niemand kann sich als anderer Spieler ausgeben;
    Microsoft-Token verlässt den PC nur Richtung Mojang.
27. **Server ohne Abhängigkeiten, alles im RAM**, Hosting **Render.com Free** per `render.yaml` (Deploy per Klick).
28. **Musikerkennung über Windows-Mediensteuerung (SMTC) per PowerShell** + Spotify-Fenstertitel als Fallback, statt Spotify-Web-API
    (die bräuchte eine weitere OAuth-App).
29. **Launcher mit Electron** (HTML/CSS sieht dem NoRisk-Launcher am ähnlichsten, skinview3d für 3D-Skins), Windows-Build
    unter Linux per @electron/packager, exe-Icon per resedit (kein Wine nötig). Auslieferung als **ZIP** (portable), kein Installer.
30. **Microsoft-Login nur mit eigener Azure-App** des Nutzers + Mojang-Freischaltung. Abgelehnt: fremde Client-IDs anderer
    Launcher nutzen, Login umgehen, Offline-/Cracked-Modus.
31. **Steve als Standard-Skin wird zur Laufzeit von `textures.minecraft.net` geladen** und gecacht (nicht mitgeliefert,
    Mojang-Asset). Offline-Ersatz: eigener „Nova Hoodie“-Skin.
32. **Launcher-Schrift Monocraft** (OFL-lizenzierte Nachbildung); die echte Minecraft-Schrift gehört Mojang. Kein künstliches
    Fett (`font-synthesis: none`), Log-Konsole und Login-Code bleiben in Monospace-Systemschrift.
33. **Repo öffentlich gemacht** (für das Mojang-Formular als Website-Angabe).
34. **Keine Tokens/Keys im Chat**: Der Nutzer bot einen GitHub-Key an – abgelehnt, stattdessen GitHub-App-Freigabe erklärt.

### Verworfene Ansätze (nicht erneut versuchen)
- `gradlew` aus einem fremden geklonten Repo ausführen → blockiert.
- Tastenabfrage pro Tick (`InputConstants.isKeyDown`) für Modul-Tasten → verpasst kurze Drücke.
- HUD-Positionen als Bruchteile → Überlappungen.
- Maus-Button-Nummern 0/1 (alte GLFW-Logik) → in 26.x falsch.
- Vanilla-KeyMappings für Zoom/Freelook → durch Nova-KeySettings ersetzt.
- ModelPart-Rotationen für Animationen → falsch bei mehreren Spielern (verzögertes Rendering).
- `.mcmeta`-animierte Cape-Texturen → funktioniert nur in Atlanten.
- Zeit-Wechsler → in 26.x nicht sauber machbar.
- Cloudflare Workers/KV/Durable Objects als Server → KV-Schreiblimits bzw. kompliziertes Deployment; daher Node auf Render.
- Spotify-Web-API für den Song → weitere App-Registrierung nötig.
- Launcher-Variante 1 „Installer für den offiziellen Launcher“ → Nutzer wählte Variante 2 (eigener Launcher).
- Monocraft über jsDelivr laden → Paket > 50 MB, jsDelivr verweigert; stattdessen `raw.githubusercontent.com`.
- Mojang-Dateien (Steve, Minecraft-Schrift) mitliefern → Urheberrecht.
- Emote-Taste G (Konflikt Schnellaktionen), Test-Taste O (Freundesliste).

---

## 4. VORGABEN & REGELN DES NUTZERS

- **Sprache: Deutsch**, locker („Bro“, „mein bester Freund“). Antworten verständlich, Schritt für Schritt bei Klick-Anleitungen
  (Azure, GitHub, Render) – der Nutzer ist kein Profi-Entwickler.
- **„MACHE KEINE FEHLER!!!“** → gründlich testen (im echten Spiel), Screenshots prüfen, ehrlich sagen, was nicht getestet werden konnte.
- **Immer fertige Dateien liefern**: Der Nutzer fragt oft „gebe mir die jar (und exe)“ → beide Dateien erneut per SendUserFile
  schicken (`nova-client-<version>.jar` aus dem Repo-Root bzw. `build/libs/`, `launcher/dist/Nova-Launcher-Windows.zip`).
- **NoRisk Client als Inspiration** (Funktionen, Optik), aber eigener Name/Branding.
- **Viele Features** („füge noch VIEL mehr hinzu“): Module, Cosmetics, Funktionen wie bei NoRisk.
- Ausdrücklich gewünscht: Freelook, Custom Crosshair, cleanere UI, Anklicken zum Aktivieren, Suche, Capes, Nametags, Cosmetics,
  Profile, änderbare Akzentfarben, animierte Cosmetics (Bandanas [UNSICHER: Nutzer schrieb „Bahndamms“ – als Bandanas
  verstanden]), animierte Capes, Cosmetics-Menü mit Vorschau, aktueller Song über dem Namen **für alle sichtbar**,
  **Owner-Symbol für Flipp1009 und EinfxchFiete**, eigener .exe-Launcher mit Skin-Wechsler und Profilen wie NoRisk,
  im Launcher **Steve als Standard-Skin**, darüber „nicht eingeloggt“, **Minecraft-Schrift im Launcher**.
- Namenskonventionen im Code: Package `dev.novaclient.*`, Modul- und Settingnamen **auf Deutsch** (sind Config-Schlüssel),
  Code-Kommentare/Commit-Messages Englisch, UI-Texte Deutsch.
- Commits: Autor „Jonte“ mit der E-Mail des Nutzers (wie bisher), Push direkt auf `main`; Commit-Messages mit den
  Attributionszeilen, die die Umgebung vorgibt. Keine Modell-IDs in Commits.
- Nutzer arbeitet auf **Windows** (Screenshots), nutzt Chrome, Spotify, Discord und aktuell den NoRisk-Launcher.

---

## 5. VERLAUF / CHANGELOG (chronologisch)

**02.11.2026**
1. Frage „Kannst du Minecraft Clients coden?“ → Fabric-Client-Mod vorgeschlagen, Cheats ausgeschlossen.
2. Auftrag: 26.3, Fabric, alle Features, NoRisk als Inspiration, .jar für den mods-Ordner.
   - Umgebung: JDK 25 + Gradle 9.8.2 geladen, Projekt angelegt, Minecraft-Quellen dekompiliert.
   - **v1.0.0** (Commit a8a0c90): 19 Module (HUD: FPS, Ping, CPS, Koordinaten, Keystrokes, Rüstung, Tränke, Uhr, Server-IP,
     Geschwindigkeit, RAM, Tag; Theme; Zoom, Fullbright, No Hurt Cam, Low Fire; Toggle Sprint/Sneak), ClickGUI (Rechte Umschalt),
     HUD-Editor, JSON-Config. Gametest eingerichtet. Bugs: Tasten-Polling, HUD-Überlappung, Low Fire 0.3 → behoben.
3. GitHub-Repo: Nutzer bot Key an → abgelehnt; Repo `EinfxchFiete/Nova-Client` gefunden, Code auf GitHubs „Initial commit“ gerebased
   und gepusht; Gradle-Wrapper, README, .gitignore ergänzt.
4. **v1.1.0** (6b9effe): Maus-Button-Bug (SDL-Nummern) gefunden und überall behoben (CPS, Klicks im Menü, HUD-Editor);
   neues Karten-Menü mit Tabs/Suche/Einstellungsseite; Freelook, Custom Crosshair (6 Stile), HUD-Tweaks, Kein Regen,
   Reichweite, Combo, Biom, Pfeile, Spielzeit; Keystrokes neu; ModeSetting; Absturz durch statisches ItemStack behoben.
5. **v1.2.0** (3785ac7): Freelook robuster (KeyState, KeySetting, Anzeige), Cosmetics-Tab (Cape 10 Designs, Nametags,
   Partikel-Spur, Treffer-Partikel), Profile + Vorlagen, Design-Seite, Ziel-HUD, Kompass, Lichtlevel, Sättigung, Datum,
   Totem/Item-Zähler, Blockumrandung, Overlays, Benachrichtigungen, Todes-Koordinaten, Haltbarkeitswarnung,
   Chat-Zeitstempel, HUD-„Anordnen“, Suche in Settings, Zurücksetzen pro Modul.
6. **v1.3.0** (7353f8a): **Suche repariert** (SDL-Texteingabe), Begleiter, Aura, Heiligenschein (Partikel, später
   „Partikel-Ring“), 4 weitere Capes + „Wechselnd“, Namensfarbe, Block-Info, Inventar-Vorschau, Spieler online, Wetter,
   Stoppuhr, Wegpunkt, Chat-Makros (TextSetting), Hintergrund-FPS, Nether-Koordinaten/Chunk, Unterwasser-Overlay.
   Launcher-Hindernis (Microsoft-Login) erklärt, Optionen 1/2 angeboten.
7. Nutzer wählte **Option 2** → **Nova Launcher 1.0.0** (617e0bb): Electron, Login, Profile, Skins (3D, Classic/Slim, Upload),
   Capes, Konsole, Optionen mit Azure-Anleitung, automatische Installation, Windows-Build + Icon. Tests: UI, Installation+Start,
   STARTEN-Klick.
8. Azure-Einrichtung mit dem Nutzer (siehe Abschnitt 10): Azure-Free-Konto, App „Nova Launcher“, Client-ID, Mojang-Formular
   („New AppID for Approval“), Repo öffentlich gemacht, Repo auf Geheimnisse geprüft (keine; nur Autor-E-Mail in der Historie).

**03.11.2026**
9. **v1.4.0** (bdbb653): 3D-Kopfschmuck (Bandana, Krone, Zylinder, Hörner, Katzenohren, Heiligenschein; 11 Farben),
   animierte Flügel (Engel/Drache/Schmetterling), 6 animierte Capes, Cosmetics-Menü mit 3D-Vorschau, Owner-/Nova-Abzeichen
   (Nametag + Tab), Tab-Ping in ms, Musik-Modul (Song über dem Namen + HUD), Emotes + Emote-Rad (V), Koordinaten teilen,
   Nova-Netzwerk (Client) + Nova-Server (`backend/`, Tests, `render.yaml`). Launcher **1.1.0** mit neuer Jar und Neuigkeiten.
10. Nutzer will die Server-Adresse später schicken (wenn zu Hause / wieder Tokens).
11. Launcher (ee84bb2): Steve als Standard-Skin (von Mojang geladen), Nametag „Nicht eingeloggt“, Monocraft-Schrift.
12. Übergabe: diese CLAUDE.md + `tools/` (Generatoren/Helfer aus dem Chat).
13. Neuer Chat (03.11.2026): **v1.4.1** – Owner-/Nova-Abzeichen im Chat (`ChatListenerMixin`, Setting „Abzeichen im Chat“ im Modul
    Nova-Netzwerk, Standard an), animierte Capes im Cosmetics-Menü „… Live“ (nur Anzeige, Config-Werte unverändert).
    Gametest prüft echten Spieler-Chat (`sendChat`) mit/ohne Abzeichen. Launcher **1.1.1** (neue Jar + Neuigkeiten-Karte).
    Gepusht auf den Session-Branch `claude/intelligent-babbage-0h0ma0` (Vorgabe der Umgebung), nicht direkt auf `main`.
    Server-URL weiterhin offen.

---

## 6. BEKANNTE PROBLEME & BUGS

### Gelöste Bugs (Ursache → Lösung)
| Problem | Ursache | Lösung |
|---|---|---|
| `gradlew` blockiert | fremdes geklontes Repo | eigenes Projekt, Gradle-Distribution |
| Maven Central HTTP 429 | Rate-Limit | Build mit Wiederholungen (Backoff) |
| `genSources` scheiterte beim 1. Mal | unklar [UNSICHER] | einfach erneut ausführen |
| Spiel startet nicht unter Xvfb („Couldn't find matching GLX visual“) | Minecraft fordert sRGB-Framebuffer (SDL-Attr. 22) | Test-Mixin setzt 22=0 (nur Gametest) |
| Modul-Tasten reagieren manchmal nicht | Abfrage pro Tick verpasst kurze Drücke | KeyboardHandler-Mixin + Druckzähler |
| HUD-Elemente überlappen | Positionen als Bildschirm-Bruchteile | Anker + Pixel-Offset |
| Low Fire versteckt Feuer komplett | 0.3 zu viel für 26.3 | Standard 0.2, max 0.4 |
| CPS falsch, Linksklick öffnet Einstellungen, HUD-Editor-Drag kaputt | 26.x/SDL: links=1, rechts=3 (nicht 0/1); Test nutzte dieselben falschen Zahlen | `InputConstants.MOUSE_BUTTON_*` überall |
| Absturz beim Start „Components not bound yet“ | `static final ItemStack` beim Mod-Init | ItemStacks erst beim Zeichnen erzeugen |
| Leerer grauer Kasten unten rechts | Rüstungs-HUD ohne Rüstung | Breite/Höhe 0 wenn leer |
| Einstellungsseite: Zeilen scrollen unter den Kopf | gemeinsamer Scissor | eigener Scissor ab Listenbeginn |
| Profil „presets“ würde Zeilen-IDs kollidieren | Strings als Zeilentyp | `record ProfileRow` |
| Benachrichtigungen verdecken Bildmitte | Position über der Hotbar | oben mittig, max. 3 |
| **Suche: nichts eintippbar** | 26.x/SDL liefert Zeichen nur nach `textInputManager().startTextInput(owner)`; `typeChars` im Test umgeht das | `ClickGuiScreen.added()/tick()` startet, `removed()` stoppt; Test prüft das Feld `textInputEnabled` per Reflection |
| Launcher: neuer Skin erscheint nicht | UI-State nicht neu geladen | Skins-Seite holt `state` neu |
| Launcher: „unhandled rejection“-Fehler | bereits angezeigte Fehler weitergeworfen | `e.shown` + `unhandledrejection`-Handler |
| Cape-Vorschau im Menü schwarz | GUI-`blit` braucht Dateipfad `textures/…png`, nicht Textur-ID | `CapeModule.texturePath(id)` |
| Gametest: Screenshot mit „ö“ im Namen | Dateisystem-Kodierung der Test-JVM | nur ASCII in Screenshot-Namen |
| Koordinaten-Taste O ohne Wirkung | **O = Freundesliste** in 26.3 | Standard „keine Taste“, Test nutzt U |
| Emote-Taste G | **G = Schnellaktionen** in 26.3 | Emote-Rad auf **V** |
| exe-Versionsinfo immer 1.0.0 | hartcodiert | aus `package.json` gelesen |
| Monocraft ohne ✕/✦ | Glyphen fehlen | ✕→× ersetzt, ✦ fällt auf Systemschrift zurück |
| PowerShell-Musikprozess könnte Spiel überleben | Kindprozess ohne Ende | Script bricht bei geschlossenem Stdout ab + Shutdown-Hook |

### Offene Punkte / Risiken
- **Nicht unter Windows getestet**: Musikerkennung (PowerShell/SMTC), Launcher-Start unter Windows, `javaw.exe`, Klassenpfad mit `;`.
  [UNSICHER] ob beim PowerShell-Start kurz ein Konsolenfenster aufblitzt (Java nutzt vermutlich CREATE_NO_WINDOW).
- **Echter Microsoft-Login** im Launcher ungetestet (geht erst nach Mojang-Freischaltung); ebenso Skin-Upload/Cape-Wechsel.
- **Nova-Server-Anmeldung mit echtem Konto** ungetestet (im Test über `NOVA_DEV`-Login).
- **Tab-Liste** (Abzeichen, Ping in ms) nicht visuell geprüft: Im Einzelspieler zeigt Minecraft die Tab-Liste nicht, wenn nur ein
  Spieler gelistet ist. Mixins laden aber fehlerfrei (`defaultRequire 1`).
- Render-Free schläft nach ~15 min ohne Anfragen (erste Anfrage ~1 min langsam).
- Viele HUD-Module gleichzeitig an + kleine GUI-Skala ⇒ Platzmangel/Überlappung (HUD-Editor „Anordnen“ hilft nur teilweise).
- Custom Crosshair blendet die Vanilla-Angriffsanzeige mit aus.
- Gametest-Szene: In der Frontansicht war die Kamera sehr nah, solange ein Rüstungsständer im Weg stand (nur Test).
- E-Mail-Adresse des Nutzers steht als Commit-Autor in der (öffentlichen) Git-Historie; Umschreiben angeboten, **keine Antwort** [OFFEN].

---

## 7. AKTUELLER STAND

- Alles Umgesetzte ist **committet und gepusht** (v1.4.1 auf Branch `claude/intelligent-babbage-0h0ma0`; `main` steht noch auf der Übergabe).
- Zuletzt bearbeitet: Chat-Abzeichen, „… Live“-Capes, Launcher 1.1.1.
- Ausgeliefert an den Nutzer: `nova-client-1.4.1.jar`, `Nova-Launcher-Windows.zip` (Launcher 1.1.1 inkl. Nova 1.4.1).
- **Wartet auf den Nutzer:**
  1. **Nova-Server auf Render deployen** und die URL schicken (Anleitung in `backend/README.md`).
  2. **Mojang-Freischaltung** der Azure-Client-ID (Formular wurde ausgefüllt; [UNSICHER] ob abgesendet und ob schon geantwortet).
  3. [UNSICHER] ob in Azure „Öffentliche Clientflows zulassen“ wirklich auf „Ja“ gestellt wurde (nie bestätigt).
- Halbfertig im Code: nichts bekannt. `NetworkModule.DEFAULT_SERVER = ""` ist bewusst leer (wartet auf URL).
- Besprochen, aber nicht im Repo: Standard-Client-ID im Launcher vorbelegen (Idee, siehe 8), E-Mail aus Git-Historie entfernen (Angebot).

---

## 8. OFFENE AUFGABEN & IDEEN

### To-dos nach Priorität
1. **Server-URL einbauen**, sobald der Nutzer sie schickt: `NetworkModule.DEFAULT_SERVER = "https://…onrender.com"`,
   Version erhöhen (1.4.2), `./gradlew build` + Gametest, Jar nach `launcher/resources/nova-client.jar`, Launcher neu bauen
   (dist:win, Icon, ZIP), committen, pushen, **Jar + ZIP schicken**. Danach mit echtem Server testen
   (`curl https://…/health`).
2. Nach Mojang-Freischaltung: echten Login/Skin-Upload/Spielstart unter Windows mit dem Nutzer testen; Idee: die Client-ID
   des Nutzers als Standard in `launcher/src/core/store.js` (`settings.clientId`) vorbelegen, damit Freunde nichts eintragen
   müssen. Die ID steht bewusst **nicht** im Repo → beim Nutzer erfragen und **vorher fragen**, ob sie ins öffentliche Repo darf.
3. Feedback des Nutzers zu Windows einholen: Song-Anzeige, Freelook, Launcher.
4. ~~Abzeichen im Chat~~ (erledigt in 1.4.1). Offen: auch für Plugin-Server-Chat (Systemnachrichten, Name per Text suchen) – riskant, nicht gemacht.
5. ~~Doppelte Cape-Namen~~ (erledigt in 1.4.1: animierte Capes mit gleichnamigem statischem Design heißen „… Live“).
6. Frage an den Nutzer: E-Mail aus der Git-Historie entfernen (Force-Push nötig)?

### Ideen (erwähnt/angedacht, nicht umgesetzt)
- Aus dem allerersten Angebot, nie umgesetzt: **Performance-Optimierungen über Mixins**, eigene Render-/Shader-Effekte.
- Discord Rich Presence (braucht Discord-App-ID), Zeit-Wechsler (in 26.x schwierig), Minimap, Item-Physik, Motion Blur,
  Treffer-Farbe, Schadenszahlen, Auto-GG, kompakter Chat / längerer Chat-Verlauf, Gegenstand-in-der-Hand-Größe,
  Scoreboard-Zahlen ausblenden, mehrere Wegpunkte mit Markierung in der Welt, Song-Fortschrittsbalken (Spotify-HUD wie NoRisk),
  Freundesliste/Nova-Freunde, mehr Emotes, Rucksack/Hüte weitere Cosmetics, Begleiter/Aura/Spur auch für andere Spieler
  synchronisieren (bisher nur Cape/Kopf/Flügel/Song/Emote), Nova-Cosmetics in der Launcher-Vorschau,
  Launcher: Auto-Update, Code-Signing der exe (SmartScreen-Warnung), macOS/Linux-Builds, Skin per Name/URL importieren,
  Launcher-Variante 1 (Installer für den offiziellen Launcher) als Alternative.

---

## 9. WICHTIGE CODE-SCHNIPSEL (aus dem Chat, die nicht Teil des Mod-/Launcher-Codes sind)

Alle diese Skripte liegen jetzt im Repo unter `tools/` (vorher nur im temporären Arbeitsordner des Chats), **inhaltlich unverändert**.
Einzige Ausnahme: `SdlGlTest.java` hieß im Chat `T.java` (Klasse `T`), nur der Klassenname wurde angepasst.

| Datei | Zweck | Aufruf |
|---|---|---|
| `tools/assets/capes.py` | 14 statische Cape-Texturen (64×32) | `python3 tools/assets/capes.py src/main/resources/assets/novaclient/textures/cape` |
| `tools/assets/cosmetic_assets.py` | Abzeichen, Kopfschmuck-/Flügel-Texturen, 6×16 animierte Cape-Frames | `python3 tools/assets/cosmetic_assets.py src/main/resources/assets/novaclient/textures` |
| `tools/assets/mod_icon.py` | Mod-Icon 128×128 | im Repo-Root: `python3 tools/assets/mod_icon.py` |
| `tools/assets/launcher_assets.py` | Launcher `default-skin.png` + `icon.png` | in `launcher/`: `python3 ../tools/assets/launcher_assets.py` |
| `tools/assets/make_ico.py` | `icon.ico` aus `icon.png` | in `launcher/`: `python3 ../tools/assets/make_ico.py` |
| `tools/testing/build-headless-mod.sh` | Test-Hilfsmod für Launcher-Starttests | `tools/testing/build-headless-mod.sh /tmp/headless.jar` |
| `tools/testing/fake-playerctl` | Fake-Song für den Linux-Musiktest | `PATH=$PWD/tools/testing:$PATH NOVA_FAKE_PLAYERCTL=1 …` |
| `tools/testing/SdlGlTest.java` | Mini-Programm zur Diagnose von SDL/OpenGL unter Xvfb (zeigte das sRGB-Problem) | `java -cp "<lwjgl+lwjgl-sdl jars>" SdlGlTest.java` |
| `tools/testing/glx.py` | Mini-Diagnose per ctypes: X-Display öffnen, GLX-Erweiterung und `glXChooseVisual` prüfen (erster Schritt der Xvfb-Fehlersuche) | `LIBGL_DEBUG=verbose xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX" python3 tools/testing/glx.py` |
| `tools/dev-setup.sh` | Java 25 + XDG_RUNTIME_DIR für Cloud-Sessions | `source tools/dev-setup.sh` |

Geprüft: `mod_icon.py`, `launcher_assets.py` und `make_ico.py` erzeugen **bytegleich** die Dateien im Repo. `capes.py`,
`cosmetic_assets.py`, `fake-playerctl` und `glx.py` sind bytegleich mit den Originalen aus dem Arbeitsordner des Chats.
`cosmetic_assets.py` nutzt bei Cape-Frames `hash(name)` für einen (ungenutzten) Zufallsgenerator – Ausgabe trotzdem deterministisch [UNSICHER, nicht bytegenau verglichen].

**Test-Hilfsmod (Inhalt von `build-headless-mod.sh`, Kern):**
```json
fabric.mod.json:           {"schemaVersion":1,"id":"nova-headless-test","version":"1.0.0","environment":"client","mixins":["nova-headless.mixins.json"]}
nova-headless.mixins.json: {"required":true,"package":"dev.novaclient.test.mixin","compatibilityLevel":"JAVA_25","client":["HeadlessGlBackendMixin"],"injectors":{"defaultRequire":1}}
```
+ `build/classes/java/gametest/dev/novaclient/test/mixin/HeadlessGlBackendMixin.class`, gepackt mit `jar cf`.

**Texte für das Mojang-Formular (aka.ms/mce-reviewappid), wie dem Nutzer vorgeschlagen:**
- Typ: **New AppID for Approval**; Website: `https://github.com/EinfxchFiete/Nova-Client`
- Justification:
  > Nova Launcher is a free, non-commercial launcher for Minecraft: Java Edition that I am building for my own client mod "Nova Client" (Fabric, Minecraft 26.3).
  > I need access to the Minecraft Services API so players can sign in with their own Microsoft account via the device code flow, verify that they own Minecraft: Java Edition, and start the official game with a valid session. The launcher also lets players change their own skin and choose their own capes through the official profile endpoints.
  > The game files are downloaded only from Mojang's official servers. Tokens are stored locally on the user's PC and encrypted with the operating system's protection (Windows DPAPI). The launcher does not allow playing without owning the game, has no ads or payments, and does not collect or send any user data to third parties.
- Other information:
  > This is a personal hobby project, currently used by me and a few friends. The Azure app is registered for personal Microsoft accounts only, with public client flows enabled for the device code login. No redirect URIs are used. The source code is available at the URL above.
  (Der Nutzer ergänzte „The Client and Texts are fully AI Generated :D“; vorgeschlagen war sachlicher: „The client, the launcher and these texts were created with the help of an AI coding assistant (Claude).“ [UNSICHER welche Fassung abgeschickt wurde])

**Erste Formular-Hilfe** (allgemein, bevor der Nutzer die echten Felder per Screenshot zeigte):

| Feld | Vorschlag |
|---|---|
| Name / E-Mail | Name + E-Mail des Microsoft-Kontos des Nutzers |
| Company / Organization | eigener Name oder `Nova Client (private project)` |
| Application name | `Nova Launcher` |
| Application (Client) ID | Client-ID der Azure-App (hier bewusst nicht notiert) |
| Tenant ID / Directory ID (falls gefragt) | „Verzeichnis-ID (Mandant)“ aus Azure (hier bewusst nicht notiert) |
| Website | nur Echtes. Der Nutzer hatte zuerst „none“ → empfohlen: Repo öffentlich machen und Repo-URL eintragen (so gemacht) |

- Erste Beschreibung („How will you use the API?“), später durch die Justification oben ersetzt:
  > Nova Launcher is a small, non-commercial Minecraft: Java Edition launcher for my own client mod "Nova Client" (Fabric, Minecraft 26.3). It uses the Microsoft device code login to authenticate players with their own Microsoft account, checks that they own Minecraft, launches the official game files from Mojang's servers, and lets players change their own skin and select their own capes via the Minecraft Services API. Tokens are stored only locally and encrypted on the user's PC. The launcher is free, contains no ads or payments, and does not let anyone play without owning the game.
- Ja/Nein-Fragen: kommerziell → **Nein**; öffentlich verfügbar → ehrlich („privat / kleine Nutzergruppe“); Usage Guidelines/EULA → **Ja**.
- Mitgegebene Regeln: nur Wahres schreiben (keine erfundenen Nutzerzahlen, Firma oder Website), sich nicht als NoRisk oder Mojang
  ausgeben; nutzt nur der Nutzer selbst den Launcher, „currently only used by me“ statt „used by me and a few friends“ schreiben.
  Mojang antwortet per E-Mail nach Tagen bis Wochen; Ablehnung oder späterer Entzug der Freischaltung ist möglich.
- Letzter Stand im Chat: „Das Formular passt so, du kannst es absenden.“ Dazu der Tipp, im privaten Browserfenster zu prüfen, ob das
  Repo ohne Login erreichbar ist.

---

## 10. SONSTIGES

### Konten & Dienste (ohne Geheimnisse)
- **GitHub**: Repo `EinfxchFiete/Nova-Client` (öffentlich). Weitere Repos des Nutzerumfelds (z. B. `flipp1009/*`) sind für dieses
  Projekt irrelevant. Zugriff des Assistenten lief über die Claude-GitHub-App.
- **Owner-UUIDs** (Mojang): Flipp1009 = `0c68d1f1-ba5c-4356-833b-5f1cb4e4530c`, EinfxchFiete = `38f96e1e-970c-4c9f-bb7d-a15c4b07bfcb`
  (per `api.mojang.com/users/profiles/minecraft/<name>` ermittelt).
- **Azure** (privates Microsoft-Konto des Nutzers, Azure-Free-Konto mit „Default Directory“): App-Registrierung **„Nova Launcher“**,
  Kontotyp „Nur persönliche Microsoft-Konten“. **Client-ID und Verzeichnis-ID (Mandant) bewusst nicht notiert**: Sie stehen im
  Azure-Portal (App-Registrierungen → „Nova Launcher“ → Übersicht) und in den Launcher-Optionen des Nutzers. Die Client-ID ist
  technisch kein Geheimnis, aber der Nutzer fragte ausdrücklich, ob „die Azure-ID“ im Repo sichtbar ist (Antwort damals: nein)
  → nur nach Rückfrage ins Repo schreiben. Keine Umleitungs-URIs, kein Client-Geheimnis.
- **Azure-Einrichtung (Ablauf aus dem Chat, falls der Nutzer nochmal Hilfe braucht):** Private Microsoft-Konten haben kein eigenes
  Azure-Verzeichnis mehr, ohne Verzeichnis gibt es keine App-Registrierung. Lösung: kostenloses Azure-Konto („sich für Azure
  registrieren“; verlangt Handynummer und Kreditkarte nur zur Identitätsprüfung, evtl. kurze Reservierung von ca. 1 €).
  Ausgabenschutz aktiv, **nie auf „Pay-as-you-go“ upgraden**; nach 30 Tagen verfällt nur das Startguthaben, Verzeichnis und App bleiben.
  Das M365-Entwicklerprogramm hilft nicht (Testumgebung nur noch mit bezahltem Visual-Studio-Abo).
  Klickweg: Suchleiste „App-Registrierungen“ → „+ Neue Registrierung“ (Name `Nova Launcher`, „Nur persönliche Microsoft-Konten“,
  Umleitungs-URI leer) → „Registrieren“ → Verwalten → „Authentifizierung“ (im neuen Design ggf. Registerkarte „Einstellungen“) →
  „Öffentliche Clientflows zulassen“ = Ja/Aktiviert → „Speichern“ → „Übersicht“ → „Anwendungs-ID (Client)“ kopieren →
  Nova Launcher → Optionen → „Azure Client-ID“. Danach das Mojang-Formular (Abschnitt 9). Bis zur Freischaltung meldet der Launcher
  „Mojang hat diese Client-ID noch nicht freigeschaltet“, das ist normal.
- **Render.com**: noch kein Konto/Deploy [Stand Übergabe].
- **Monocraft**: <https://github.com/IdreesInc/Monocraft> (OFL 1.1), Datei `dist/Monocraft-ttf/Monocraft.ttf`.
- **Steve-Textur**: `https://textures.minecraft.net/texture/31f477eb1a7beee631c2ca64d06f8f68fa93a3386d04452ab27f43acdf1b60cb`.

### Hinweise, die dem Nutzer gegeben wurden
- **Installation der Jar:** Fabric Loader ≥ 0.20.1 für 26.3, **Fabric API** (für 26.3) und `nova-client-<version>.jar` in den
  `mods`-Ordner, alte Nova-Jar löschen, mit dem Fabric-Profil starten. Menü: Rechte Umschalttaste, Zoom: C.
- **Spielen bis zur Mojang-Freischaltung:** Nova-Jar + Fabric API in ein Fabric-26.3-Profil im offiziellen Launcher, im **Prism
  Launcher** oder in der **Modrinth App** legen, dort geht der normale Microsoft-Login sofort. Ob der NoRisk-Launcher eigene Mods
  neben seinem Client erlaubt, sollte der Nutzer selbst prüfen [UNSICHER].
- **Freelook** ist auf **Hypixel** und manchen anderen Servern verboten → vorher die Serverregeln prüfen.
- **Windows SmartScreen:** Beim ersten Start der unsignierten `.exe` erscheint „Der Computer wurde durch Windows geschützt“ →
  „Weitere Informationen“ → „Trotzdem ausführen“.
- Bei Problemen unter Windows soll der Nutzer einen Screenshot oder den Text aus der Launcher-Konsole schicken.

### Nützliche Links
- Azure App-Registrierungen: <https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade>
- Mojang-Freischaltung: <https://aka.ms/mce-reviewappid>
- Fabric Meta: `https://meta.fabricmc.net/v2/versions/loader/26.3`, Modrinth Fabric API: `https://api.modrinth.com/v2/project/fabric-api/version`
- Mojang Versionsmanifest: `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`

### Cloud-Umgebung (Erfahrungen)
- Vorinstalliert: Java 21 (nicht 25!), Node 22, Python 3, Xvfb, Mesa (llvmpipe), Chromium + Playwright (`require('playwright')`).
- Erreichbar: Maven Central (manchmal 429), maven.fabricmc.net, Mojang-Server, Modrinth, npm, GitHub-Downloads
  (Release-*Seiten* gaben 403, direkte Downloads gingen), raw.githubusercontent.com, Adoptium-API.
- Xvfb-Start immer mit `+extension GLX`; `XDG_RUNTIME_DIR` setzen; Electron in Tests mit `--no-sandbox`.
- Sound-/Realms-/Session-Fehler im Spiel-Log sind in der Testumgebung normal (keine Soundkarte, Fake-Konto).

### Minecraft-26.3-Spickzettel (viel Zeit gekostet, unbedingt beachten)
- **Unobfuskiert**: Mojang-Namen direkt, keine Yarn-Mappings. Viele Umbenennungen: GUI zeichnet über `GuiGraphicsExtractor`
  (`extractRenderState` statt `render`, `extractBackground`), `g.text(font, s, x, y, ARGB, shadow)` – Farben **mit Alpha** (0xFF…).
- HUD: `HudElementRegistry` + `VanillaHudElements`, `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)`.
- Screens: `minecraft.gui.screen()` / `minecraft.gui.setScreen(…)`; HUD-Objekt `minecraft.gui.hud` (`isHidden()`, `getDebugOverlay()`, `getChat()`).
- **Eingabe = SDL3**: `InputConstants.KEY_*` sind **Scancodes** (A=4, B=5, C=6, G=10, O=18, V=25, LSHIFT=225, RSHIFT=229,
  LALT=226, ESC=41, BACKSPACE=42, RETURN=40, NUMPAD0=98, NUMPAD1=89). Maus: links=1, mitte=2, rechts=3.
  `KeyEvent.key()`, `MouseButtonEvent.button()`, `CharacterEvent` (Zeichen) – **Texteingabe nur nach `textInputManager().startTextInput(owner)`**.
- Vanilla-Standardtasten 26.3 (Auszug): G = Schnellaktionen, O = Freundesliste, P = Soziale Interaktionen, L = Fortschritte,
  C/X = Hotbar speichern/laden, F5 Perspektive. Frei: B, H, J, K, M, N, U, V, Numpad.
- Rendering ist **Submit-basiert und verzögert** (`SubmitNodeCollector`): `model.setupAnim(state)` läuft beim echten Zeichnen;
  PoseStack wird beim Submit kopiert. RenderTypes: `entityCutout` (ohne Culling), `entityCutoutCull`, `entityTranslucent`,
  `entityTranslucentEmissive`. Spieler = `AvatarRenderer`/`AvatarRenderState` (`state.id` = Entity-ID, `skin`, `nameTag`,
  `scoreText`, `nameTagAttachment`).
- `PlayerSkin(body, cape, elytra, model, secure)`; `ClientAsset.ResourceTexture(id)` → `textures/<pfad>.png`.
- Entity-Typen liegen in `EntityTypes` (nicht mehr `EntityType`), `EntitySpawnReason`.
- Zeit: Welt-Uhren (`Level.getOverworldClockTime()`), Himmel über Umgebungsattribute.
- `ItemStack` nicht in statischen Feldern beim Mod-Start erzeugen.
- Gametest: `TestInput.pressMouse(int)` nimmt **rohe SDL-Buttonnummern**; `typeChars` umgeht die SDL-Texteingabe;
  Screenshot-Namen nur ASCII; Einzelspieler zeigt Tab-Liste nur mit >1 gelisteten Spielern.

---

## NACHTRAG (zweiter Durchgang durch den Verlauf)

Beim zweiten Durchgehen ergänzt:
- **Erste Antwort** des Assistenten nannte als Grenze: keine Anticheat-Bypässe für öffentliche Server, Singleplayer/eigener Server
  wäre verhandelbar – so wurde es auch gehandhabt (es gibt keine Cheat-Module).
- **Toggle Sneak** nutzt die Vanilla-Option „Schleichen umschalten“ (`toggleCrouch`) und stellt sie beim Ausschalten zurück;
  **Toggle Sprint** hält die Sprinttaste gedrückt und beachtet Vanillas `ToggleKeyMapping`-Logik.
- **Zoom**: Stärke 1,5–50 (Standard 4), Mausrad ändert Stärke ohne Hotbar zu wechseln, weicher Zoom, filmische Kamera
  (`options.smoothCamera`) während des Zooms.
- **ConfigManager** schreibt atomar über `.tmp`-Datei; Profilnamen werden auf `[A-Za-z0-9ÄÖÜäöüß _-]` und 24 Zeichen bereinigt.
- **Theme-Farbvorlagen**: Lila, Blau, Cyan, Mint, Grün, Gelb, Orange, Rot, Pink, Rosa, Gold, Weiß.
- **Crosshair-Stile**: Kreuz, Kreuz + Punkt, Punkt, Kreis, T-Form, Quadrat; Farben Weiß/Theme/Rot/Grün/Blau/Gelb/Pink/Cyan/Schwarz.
- **Partikel-Spur**: Herzen, Flammen, Seelenflammen, Noten, Endstab, Kirschblüten, Glitzer, Totem, Funken.
- **Begleiter**: Allay, Biene, Papagei, Vex; Seite links/rechts.
- **Chat-Makros**: 3 Slots, Standardtexte „/spawn“ und „gg“, ohne Taste nichts ausgelöst; Text mit „/“ = Befehl.
- **Stoppuhr** Standard Num 0 (Start/Stopp) und Num 1 (Reset); **Wegpunkt** Standard B (merkt Dimension).
- **HUD-Editor** rastet an Rändern/Mitte ein (4 px); „Fertig“-Button; Liste scrollbar.
- **Launcher-Fenster** ist rahmenlos mit eigenen Fensterknöpfen; Konto-Menü mit mehreren Konten; Profil-Popover am STARTEN-Knopf;
  Neuigkeiten-Karten (Texte in `app.js`, Konstante `NEWS`) – bei neuen Versionen aktualisieren.
- **Launcher-Versionsnummer** steht an zwei Stellen: `launcher/package.json` (`version`) und `launcher/src/core/launch.js`
  (`LAUNCHER_VERSION`). Mod-Version: `gradle.properties` (`version`) **und** `NovaClient.VERSION` – beide anpassen.
- **Datenschutz-Grundsatz für künftige Chats:** Nie nach Passwörtern, Tokens oder Zahlungsdaten fragen. Zeigt ein Screenshot
  solche Daten, sofort darauf hinweisen und nichts davon ins (öffentliche) Repo schreiben. Vor dem Eintragen von Konto-IDs o. Ä. ins
  Repo den Nutzer fragen.
- Der Nutzer will bei Gelegenheit **„den Server-Dings“ (Render-URL) schicken**, wenn er wieder Tokens hat und zu Hause ist –
  das ist der nächste konkrete Schritt (To-do 1).
- `tools/` und diese Datei wurden im Übergabe-Commit ergänzt; die Release-Jar enthält davon nichts.

### Dritter Durchgang (nach dem Neustart der Sitzung)
Der Verlauf wurde noch einmal automatisch durchsucht (alle Antworten, alle im Chat erstellten Dateien). Ergänzt wurden:
- `tools/testing/glx.py` (GLX-Diagnose aus der Xvfb-Fehlersuche), das noch fehlte, und der Hinweis, dass `SdlGlTest.java` im Chat `T.java` hieß.
- Die erste, allgemeine Formular-Hilfe für Mojang mit Feldern, Ja/Nein-Antworten und Regeln (Abschnitt 9).
- Der Azure-Ablauf mit Kosten-Hinweisen und Klickweg (Abschnitt 10).
- Hinweise an den Nutzer (Abschnitt 10): Installation, Spielen bis zur Freischaltung (Prism/Modrinth App), Freelook-Verbot auf
  Hypixel, SmartScreen.
- Ideen aus dem ersten Angebot (Performance-Mixins, Render-/Shader-Effekte).
- Die Azure-Client-ID wurde aus dieser Datei entfernt, weil das Repo öffentlich ist und der Nutzer danach gefragt hatte (siehe Abschnitt 10).
