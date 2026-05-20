# Stenograph

**Talk. Glance. Send.**

Stenograph turns your phone into a voice remote for your PC keyboard. Speak into
the phone and your words are typed — in real time — wherever the cursor is on
your PC: a code editor, a terminal, a chat box, anywhere. With space and
backspace controls, it is a hands-light way to write when you would rather talk
than type (handy for dictating prompts).

Speech recognition runs on the phone. The PC just types. The two talk directly
over your own WiFi — nothing is stored, nothing leaves your network.

## How it works

```
   ┌───────────────┐      text over your WiFi      ┌───────────────────┐
   │     Phone     │ ────────────────────────────▶ │    Windows PC     │
   │  speech → text│         (WebSocket)           │  types at the     │
   │   (on-device) │                               │  active cursor    │
   └───────────────┘                               └───────────────────┘
      you speak                                       you glance at the
                                                      screen and keep going
```

The phone runs Android's on-device speech recognition and streams the resulting
text to a small tray app on the PC. The PC app types it with Windows synthetic
keystrokes.

## Requirements

- **PC:** Windows 10 or 11, Python 3.10 or newer
- **Phone:** Android 9 (API 28) or newer
- Phone and PC on the **same WiFi network**

## Known limitations

Stenograph is honest about its rough edges:

- On-device speech recognition can mishear uncommon words and names.
- There is a brief (~0.3–0.8 s) gap roughly every 60 seconds when the phone's
  recognizer restarts.
- Typing cannot reach windows running **as administrator** unless Stenograph is
  also running as administrator (a Windows security restriction).
- The PC side is **Windows only** — it uses Windows-specific typing APIs.

## Run from source — PC

```powershell
cd pc
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
python -m src.main
```

A grey circle icon appears in the system tray — Stenograph is running.

## Build from source — Android

Open the `android/` folder in Android Studio and run it on your phone, or build
an APK from the command line:

```powershell
cd android
.\gradlew.bat assembleDebug
```

The APK is written to `android\app\build\outputs\apk\debug\app-debug.apk`. Copy
it to your phone and install it (you will need to allow installation from your
file manager or browser).

## Pairing

1. Start Stenograph on the PC — a tray icon appears.
2. Right-click the tray icon and choose **Settings**. Your browser opens the
   settings page, which shows a **QR code**.
3. Open the Stenograph app on your phone and **scan the QR code**.
4. The tray icon turns **green** — connected. Start talking.

The phone can also find the PC automatically on the same network, but the QR
code is the reliable first-time pairing step — it carries the access token.

## Settings

Right-click the tray icon for:

- **Settings** — opens the settings page (pairing QR, connection status).
- **Pause Typing** — stop typing without quitting.
- **Quit**.

## Troubleshooting

**The phone shows "Disconnected" or won't connect.**

This is almost always a network mismatch. Check these, in order:

1. **The phone's Wi-Fi is on.** If the phone is on mobile data, it *cannot*
   reach your PC — Stenograph only works over a shared local network. The
   dictation screen tells you directly when Wi-Fi is off.
2. **The phone and PC are on the same Wi-Fi network.** Guest networks isolate
   devices from each other, and some routers split the 2.4 GHz and 5 GHz bands —
   make sure both devices are on the same one.
3. **The PC tray app is running** (a grey or green icon in the system tray).
4. **Still stuck?** On the phone, tap **Re-pair** and scan the QR code again.
   This refreshes both the PC's address and the access token — which fixes it
   if your PC's IP address changed.

## Privacy

Stenograph stores nothing and sends nothing to the internet. Speech is
transcribed on your phone; the text travels over your own WiFi to your own PC
and is typed there. The only data kept is a small local config file on the PC
(`~/.stenograph/config.json`) holding the pairing token and port.

## Project layout

```
pc/        Windows tray app (Python) — WebSocket server + typing
android/   Android app (Kotlin) — speech recognition + sender
docs/      protocol.md — the phone-to-PC wire protocol
```

## Contributing

Stenograph is a small, focused tool. Issues and pull requests are welcome. The
wire protocol is documented in [docs/protocol.md](docs/protocol.md) — enough to
build an alternative client (for example, for another operating system).

## Support

Stenograph is free and open source, and always will be. If it earned a spot in
your workflow, starring the repo on GitHub is a kind (and free) way to say
thanks.

## License

MIT — see [LICENSE](LICENSE).
