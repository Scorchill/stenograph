import logging
import os
from flask import Flask, jsonify, request, send_from_directory, Response
from src.pairing import build_pairing_payload, generate_qr_bytes

log = logging.getLogger("stenograph.settings")


class SettingsServer:
    def __init__(self, config, server_ref=None):
        self._config = config
        self._server_ref = server_ref
        self._app = Flask(__name__)
        self._setup_routes()

    def _setup_routes(self):
        settings_dir = os.path.join(
            os.path.dirname(os.path.dirname(__file__)), "settings"
        )

        @self._app.route("/settings")
        def settings_page():
            return send_from_directory(settings_dir, "index.html")

        @self._app.route("/settings/<path:filename>")
        def settings_static(filename):
            return send_from_directory(settings_dir, filename)

        @self._app.route("/api/config", methods=["GET"])
        def get_config():
            return jsonify({
                "port": self._config.port,
                "enabled": self._config.enabled,
                "terminal_mode": self._config.terminal_mode,
                "connected": self._server_ref.connected if self._server_ref else False,
            })

        @self._app.route("/api/config", methods=["POST"])
        def update_config():
            data = request.json
            if "enabled" in data:
                self._config.enabled = bool(data["enabled"])
                if self._server_ref:
                    self._server_ref._enabled = self._config.enabled
            if "terminal_mode" in data:
                self._config.terminal_mode = bool(data["terminal_mode"])
                if self._server_ref and hasattr(self._server_ref, "_typer"):
                    self._server_ref._typer._terminal_mode = self._config.terminal_mode
            self._config.save()
            return jsonify({"status": "ok"})

        @self._app.route("/api/pairing/qr")
        def pairing_qr():
            payload = build_pairing_payload(
                self._config.auth_token, self._config.port
            )
            qr_bytes = generate_qr_bytes(payload)
            return Response(qr_bytes, mimetype="image/png")

        @self._app.route("/api/pairing/regenerate", methods=["POST"])
        def regenerate_token():
            import secrets
            new_token = secrets.token_hex(16)
            self._config.auth_token = new_token
            self._config.save()
            # Propagate to running server so it accepts the new token immediately
            if self._server_ref:
                self._server_ref._auth_token = new_token
            log.info("Auth token regenerated and pushed to server")
            return jsonify({"status": "ok"})

    def run(self):
        self._app.run(
            host="127.0.0.1",
            port=self._config.settings_port,
            debug=False,
            use_reloader=False,
        )
