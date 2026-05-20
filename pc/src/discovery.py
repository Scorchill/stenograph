# src/discovery.py
import logging
import socket
from zeroconf import ServiceInfo, Zeroconf

log = logging.getLogger("stenograph.discovery")

class ServiceAdvertiser:
    def __init__(self, port: int):
        self._port = port
        self._zeroconf = None
        self._info = None

    def start(self):
        self._zeroconf = Zeroconf()
        hostname = socket.gethostname()
        local_ip = self._get_local_ip()

        self._info = ServiceInfo(
            "_stenograph._tcp.local.",
            f"Stenograph on {hostname}._stenograph._tcp.local.",
            addresses=[socket.inet_aton(local_ip)],
            port=self._port,
            properties={"version": "1"},
        )
        self._zeroconf.register_service(self._info)
        log.info(f"Advertising _stenograph._tcp on {local_ip}:{self._port}")

    def stop(self):
        if self._zeroconf and self._info:
            self._zeroconf.unregister_service(self._info)
            self._zeroconf.close()
            log.info("Stopped mDNS advertising")

    def _get_local_ip(self) -> str:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            return "127.0.0.1"
