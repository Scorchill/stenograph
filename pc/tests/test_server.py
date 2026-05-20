import asyncio
import json
import pytest
import websockets
from unittest.mock import MagicMock
from src.server import StenographServer


@pytest.fixture
def mock_typer():
    typer = MagicMock()
    typer.handle_partial.return_value = []
    typer.handle_final.return_value = []
    typer.handle_undo.return_value = []
    return typer


@pytest.mark.asyncio
async def test_server_starts_and_stops(mock_typer):
    server = StenographServer(port=0, auth_token="test-token", typer=mock_typer)
    await server.start()
    assert server.port > 0
    await server.stop()


@pytest.mark.asyncio
async def test_rejects_bad_token(mock_typer):
    server = StenographServer(port=0, auth_token="correct-token", typer=mock_typer)
    await server.start()
    try:
        async with websockets.connect(
            f"ws://localhost:{server.port}",
            additional_headers={"Authorization": "Bearer wrong-token"},
        ) as ws:
            await ws.recv()
    except (websockets.exceptions.InvalidStatus, websockets.exceptions.ConnectionClosed):
        pass  # Expected — server rejected us
    await server.stop()


@pytest.mark.asyncio
async def test_accepts_valid_token(mock_typer):
    server = StenographServer(port=0, auth_token="correct-token", typer=mock_typer)
    await server.start()
    async with websockets.connect(
        f"ws://localhost:{server.port}",
        additional_headers={"Authorization": "Bearer correct-token"},
    ) as ws:
        await ws.send(json.dumps({"type": "partial", "text": "hello"}))
        await asyncio.sleep(0.1)
    mock_typer.handle_partial.assert_called_with("hello")
    await server.stop()


@pytest.mark.asyncio
async def test_dispatches_final(mock_typer):
    server = StenographServer(port=0, auth_token="tok", typer=mock_typer)
    await server.start()
    async with websockets.connect(
        f"ws://localhost:{server.port}",
        additional_headers={"Authorization": "Bearer tok"},
    ) as ws:
        await ws.send(json.dumps({"type": "final", "text": "Hello."}))
        await asyncio.sleep(0.1)
    mock_typer.handle_final.assert_called_with("Hello.")
    await server.stop()


@pytest.mark.asyncio
async def test_dispatches_undo(mock_typer):
    server = StenographServer(port=0, auth_token="tok", typer=mock_typer)
    await server.start()
    async with websockets.connect(
        f"ws://localhost:{server.port}",
        additional_headers={"Authorization": "Bearer tok"},
    ) as ws:
        await ws.send(json.dumps({"type": "undo"}))
        await asyncio.sleep(0.1)
    mock_typer.handle_undo.assert_called_once()
    await server.stop()
