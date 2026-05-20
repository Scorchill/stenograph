import json
import os
import tempfile
import pytest
from src.config import Config

def test_default_config():
    with tempfile.TemporaryDirectory() as tmpdir:
        cfg = Config(data_dir=tmpdir)
        assert cfg.port == 9476
        assert cfg.settings_port == 9477
        assert cfg.enabled is True
        assert cfg.terminal_mode is False
        assert len(cfg.auth_token) == 32

def test_config_saves_and_loads():
    with tempfile.TemporaryDirectory() as tmpdir:
        cfg = Config(data_dir=tmpdir)
        token = cfg.auth_token
        cfg.save()

        cfg2 = Config(data_dir=tmpdir)
        assert cfg2.auth_token == token
        assert cfg2.port == 9476

def test_config_file_path():
    with tempfile.TemporaryDirectory() as tmpdir:
        cfg = Config(data_dir=tmpdir)
        cfg.save()
        assert os.path.exists(os.path.join(tmpdir, "config.json"))
