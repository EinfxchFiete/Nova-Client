# Nova Launcher

Launcher für **Nova Client** (Minecraft 26.3, Fabric) – im Stil moderner Client-Launcher.

## Funktionen
- **Microsoft-Login** (Device-Code: Code im Browser bestätigen), mehrere Konten, Tokens verschlüsselt gespeichert
- **Profile**: beliebig viele, jedes mit eigenem Spielordner, RAM, Farbe, Fenstergröße, Java-Argumenten, Nova an/aus
- **Skins**: Bibliothek mit 3D-Vorschau (skinview3d), Classic/Slim, mit einem Klick aufs Konto hochladen
- **Capes**: offizielle Capes deines Kontos auswählen oder ausblenden
- **Automatische Installation**: Minecraft 26.3, Java 25 (von Mojang), Fabric Loader, Fabric API (Modrinth) und Nova Client
- **Konsole** mit lesbarer Spielausgabe

## Microsoft-Login einrichten (einmalig)
Minecraft erlaubt Logins nur über eine registrierte und von Mojang freigeschaltete App:
1. Im [Azure-Portal](https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade) eine App registrieren
   (Kontotyp: „Nur persönliche Microsoft-Konten“ oder „…und persönliche Microsoft-Konten“).
2. Unter *Authentifizierung* → „Öffentliche Clientflows zulassen“ = **Ja**.
3. Die **Anwendungs-ID (Client-ID)** im Launcher unter *Optionen* eintragen.
4. Freischaltung für die Minecraft-API beantragen: <https://aka.ms/mce-reviewappid>. Bis Mojang zustimmt, meldet der Launcher
   „Mojang hat diese Client-ID noch nicht freigeschaltet“.

## Entwickeln
```bash
npm install
npm start                 # Launcher starten
npm run dist:win          # Windows-Build nach dist/
node scripts/set-exe-icon.js "dist/Nova Launcher-win32-x64/Nova Launcher.exe"
```
Tests (Linux, Xvfb): `node test/ui-test.js`, `node test/launch-test.js`, `node test/ui-launch-test.js`.
Die Tests benutzen ein Platzhalter-Konto; der Launcher selbst startet das Spiel nur mit echtem Microsoft-Login.

Daten liegen unter `%APPDATA%\.novalauncher` (Windows) bzw. `~/.novalauncher`.
