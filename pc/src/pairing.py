import json
import io
import socket
import qrcode
from PIL import Image


def get_local_ip() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def build_pairing_payload(auth_token: str, port: int) -> str:
    return json.dumps({
        "app": "stenograph",
        "version": 1,
        "token": auth_token,
        "port": port,
        "ip": get_local_ip(),
    })


def generate_qr_image(data: str) -> Image.Image:
    qr = qrcode.QRCode(version=1, box_size=10, border=4)
    qr.add_data(data)
    qr.make(fit=True)
    return qr.make_image(fill_color="black", back_color="white")


def generate_qr_bytes(data: str) -> bytes:
    img = generate_qr_image(data)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()
