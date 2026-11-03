# Nova Server

Kleiner Server, über den sich Nova-Client-Spieler gegenseitig sehen:
**Cosmetics** (Capes, Kopfschmuck, Flügel), **Emotes** und den **Song**, den jemand gerade hört.

- Keine Datenbank, keine Abhängigkeiten – alles liegt nur im Arbeitsspeicher und verfällt nach kurzer Zeit.
- Anmeldung wie beim Betreten eines Minecraft-Servers: Mojang bestätigt, wer du bist.
  Dein Microsoft-/Minecraft-Token geht dabei **nur an Mojang**, nie an diesen Server.
- Alle Werte werden geprüft (nur bekannte Cosmetics, Songtitel gekürzt, keine Farbcodes).

## Kostenlos online stellen (Render.com)
1. Konto auf <https://render.com> anlegen (mit GitHub anmelden).
2. **New → Blueprint** wählen und das Repository `EinfxchFiete/Nova-Client` auswählen.
   Render liest die Datei `render.yaml` und richtet den Dienst `nova-server` (Free) ein.
3. Nach dem Deploy steht oben die Adresse, z. B. `https://nova-server-xxxx.onrender.com`.
4. Die Adresse im Spiel eintragen: **Rechte Umschalttaste → Utility → Nova-Netzwerk → Server-Adresse**
   (oder fest in den Client einbauen lassen, dann müssen deine Freunde nichts eintragen).

Hinweis: Kostenlose Render-Dienste schlafen nach ca. 15 Minuten ohne Anfragen ein und brauchen
beim Aufwachen etwa eine Minute. Danach läuft alles normal.

## Lokal starten / testen
```bash
node server.js            # Port 8080
npm test                  # Tests mit einem nachgebauten Mojang-Server
```
`NOVA_DEV=1` schaltet einen Test-Login ohne Mojang frei – **nur für lokale Tests**, nie online verwenden.
