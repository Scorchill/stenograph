# src/main.py
import asyncio
import logging
import sys
import threading

from src.config import Config
from src.typer import Typer
from src.server import StenographServer
from src.discovery import ServiceAdvertiser
from src.settings_server import SettingsServer
from src.tray import StenographTray

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
)
log = logging.getLogger("stenograph")


class Stenograph:
    def __init__(self):
        self.config = Config()
        self.typer = Typer(terminal_mode=self.config.terminal_mode)
        self.server = None
        self.advertiser = None
        self.tray = None
        self._loop = None

    def run(self):
        log.info("Starting Stenograph")

        # Create WebSocket server on main thread so other components can reference it
        self._loop = asyncio.new_event_loop()
        self.server = StenographServer(
            port=self.config.port,
            auth_token=self.config.auth_token,
            typer=self.typer,
            enabled=self.config.enabled,
            on_connect=lambda: self.tray.set_connected(True) if self.tray else None,
            on_disconnect=lambda: self.tray.set_connected(False) if self.tray else None,
        )

        # Run the server's event loop in a background thread
        server_thread = threading.Thread(target=self._run_server, daemon=True)
        server_thread.start()

        # Start mDNS advertising
        self.advertiser = ServiceAdvertiser(self.config.port)
        try:
            self.advertiser.start()
        except Exception as e:
            log.warning(f"mDNS advertising failed: {e}")

        # Start settings server in background thread
        settings = SettingsServer(self.config, server_ref=self.server)
        settings_thread = threading.Thread(target=settings.run, daemon=True)
        settings_thread.start()
        log.info(f"Settings: http://localhost:{self.config.settings_port}/settings")

        # Start tray (blocks main thread)
        self.tray = StenographTray(self.config, on_quit=self._shutdown)
        self.tray.start()

    def _run_server(self):
        asyncio.set_event_loop(self._loop)
        self._loop.run_until_complete(self.server.start())
        log.info(f"WebSocket server on port {self.config.port}")
        self._loop.run_forever()

    def _shutdown(self):
        log.info("Shutting down Stenograph")
        if self.advertiser:
            self.advertiser.stop()
        if self._loop and self.server:
            asyncio.run_coroutine_threadsafe(self.server.stop(), self._loop)
            self._loop.call_soon_threadsafe(self._loop.stop)
        sys.exit(0)


def main():
    app = Stenograph()
    app.run()


if __name__ == "__main__":
    main()
