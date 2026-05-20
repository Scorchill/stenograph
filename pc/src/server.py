import asyncio
import json
import logging
from websockets.asyncio.server import serve
from websockets.http11 import Request, Response
from websockets.exceptions import ConnectionClosed

log = logging.getLogger("stenograph.server")


class StenographServer:
    def __init__(self, port: int, auth_token: str, typer, on_connect=None, on_disconnect=None, enabled=True):
        self._port = port
        self._auth_token = auth_token
        self._typer = typer
        self._on_connect = on_connect
        self._on_disconnect = on_disconnect
        self._server = None
        self._connected = False
        self._enabled = enabled

    @property
    def port(self):
        if self._server:
            return self._server.sockets[0].getsockname()[1]
        return self._port

    @property
    def connected(self):
        return self._connected

    async def start(self):
        self._server = await serve(
            self._handler,
            "0.0.0.0",
            self._port,
            process_request=self._check_auth,
        )
        log.info(f"WebSocket server listening on port {self.port}")

    async def stop(self):
        if self._server:
            self._server.close()
            await self._server.wait_closed()
            self._server = None

    async def _check_auth(self, connection, request: Request):
        auth = request.headers.get("Authorization", "")
        if auth != f"Bearer {self._auth_token}":
            log.warning(f"Rejected connection: invalid token (got '{auth[:20]}...')")
            return connection.respond(403, "Forbidden")

    async def _handler(self, websocket):
        log.info("Phone connected")
        self._connected = True
        if self._on_connect:
            self._on_connect()
        try:
            async for message in websocket:
                try:
                    data = json.loads(message)
                    msg_type = data.get("type")
                    text = data.get("text", "")
                    if msg_type == "partial":
                        log.debug(f"Received: partial -> {text[:50]}")
                    else:
                        log.info(f"Received: {msg_type} -> {text[:50]}")
                    if not self._enabled:
                        continue
                    if msg_type == "partial":
                        self._typer.handle_partial(text)
                    elif msg_type == "final":
                        self._typer.handle_final(text)
                    elif msg_type == "undo":
                        self._typer.handle_undo()
                    elif msg_type == "stop":
                        self._typer.handle_stop()
                    elif msg_type == "space":
                        self._typer.handle_space()
                    elif msg_type == "backspace":
                        self._typer.handle_backspace()
                    else:
                        log.warning(f"Unknown message type: {msg_type}")
                except json.JSONDecodeError:
                    log.warning(f"Invalid JSON: {message}")
        except ConnectionClosed:
            log.info("Phone disconnected (connection closed)")
        except Exception as e:
            log.warning(f"Handler error: {e}")
        finally:
            log.info("Phone disconnected")
            self._connected = False
            self._typer.reset()  # Full reset on disconnect
            if self._on_disconnect:
                self._on_disconnect()
