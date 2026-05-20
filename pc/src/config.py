import json
import os
import secrets
from dataclasses import dataclass

DEFAULT_DATA_DIR = os.path.join(os.path.expanduser("~"), ".stenograph")

@dataclass
class Config:
    data_dir: str = DEFAULT_DATA_DIR
    auth_token: str = ""
    port: int = 9476
    settings_port: int = 9477
    enabled: bool = True
    terminal_mode: bool = False

    def __post_init__(self):
        os.makedirs(self.data_dir, exist_ok=True)
        config_path = os.path.join(self.data_dir, "config.json")
        if os.path.exists(config_path):
            with open(config_path, "r") as f:
                data = json.load(f)
            self.auth_token = data.get("auth_token", self.auth_token)
            self.port = data.get("port", self.port)
            self.settings_port = data.get("settings_port", self.settings_port)
            self.enabled = data.get("enabled", self.enabled)
            self.terminal_mode = data.get("terminal_mode", self.terminal_mode)
        if not self.auth_token:
            self.auth_token = secrets.token_hex(16)
            self.save()

    def save(self):
        config_path = os.path.join(self.data_dir, "config.json")
        data = {
            "auth_token": self.auth_token,
            "port": self.port,
            "settings_port": self.settings_port,
            "enabled": self.enabled,
            "terminal_mode": self.terminal_mode,
        }
        with open(config_path, "w") as f:
            json.dump(data, f, indent=2)
