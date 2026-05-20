# src/tray.py
import logging
import webbrowser
from PIL import Image, ImageDraw
import pystray

log = logging.getLogger("stenograph.tray")


class StenographTray:
    def __init__(self, config, on_quit=None):
        self._config = config
        self._on_quit = on_quit
        self._connected = False
        self._icon = None

    def start(self):
        menu = pystray.Menu(
            pystray.MenuItem("Settings", self._open_settings),
            pystray.MenuItem(
                "Pause Typing",
                self._toggle_enabled,
                checked=lambda item: not self._config.enabled,
            ),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem("Quit", self._quit),
        )
        self._icon = pystray.Icon(
            "Stenograph",
            self._create_icon(connected=False),
            "Stenograph — Disconnected",
            menu,
        )
        self._icon.run()

    def set_connected(self, connected: bool):
        self._connected = connected
        if self._icon:
            self._icon.icon = self._create_icon(connected)
            self._icon.title = f"Stenograph — {'Connected' if connected else 'Disconnected'}"

    def _create_icon(self, connected: bool) -> Image.Image:
        size = 64
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        color = (100, 220, 100, 255) if connected else (80, 80, 80, 255)
        draw.ellipse([8, 8, size - 8, size - 8], fill=color)
        return img

    def _open_settings(self):
        webbrowser.open(f"http://localhost:{self._config.settings_port}/settings")

    def _toggle_enabled(self):
        self._config.enabled = not self._config.enabled
        self._config.save()

    def _quit(self):
        if self._icon:
            self._icon.stop()
        if self._on_quit:
            self._on_quit()
