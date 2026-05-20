import ctypes
import ctypes.wintypes
import logging

log = logging.getLogger("stenograph.typer")

# Windows constants
INPUT_KEYBOARD = 1
KEYEVENTF_UNICODE = 0x0004
KEYEVENTF_KEYUP = 0x0002
VK_BACK = 0x08


class KEYBDINPUT(ctypes.Structure):
    _fields_ = [
        ("wVk", ctypes.wintypes.WORD),
        ("wScan", ctypes.wintypes.WORD),
        ("dwFlags", ctypes.wintypes.DWORD),
        ("time", ctypes.wintypes.DWORD),
        ("dwExtraInfo", ctypes.POINTER(ctypes.c_ulong)),
    ]


class MOUSEINPUT(ctypes.Structure):
    _fields_ = [
        ("dx", ctypes.c_long),
        ("dy", ctypes.c_long),
        ("mouseData", ctypes.wintypes.DWORD),
        ("dwFlags", ctypes.wintypes.DWORD),
        ("time", ctypes.wintypes.DWORD),
        ("dwExtraInfo", ctypes.POINTER(ctypes.c_ulong)),
    ]


class HARDWAREINPUT(ctypes.Structure):
    _fields_ = [
        ("uMsg", ctypes.wintypes.DWORD),
        ("wParamL", ctypes.wintypes.WORD),
        ("wParamH", ctypes.wintypes.WORD),
    ]


class INPUT(ctypes.Structure):
    class _INPUT(ctypes.Union):
        _fields_ = [
            ("ki", KEYBDINPUT),
            ("mi", MOUSEINPUT),
            ("hi", HARDWAREINPUT),
        ]

    _fields_ = [
        ("type", ctypes.wintypes.DWORD),
        ("_input", _INPUT),
    ]


class Typer:
    def __init__(self, dry_run=False, terminal_mode=False):
        self._dry_run = dry_run
        self._terminal_mode = terminal_mode
        self._current_text = ""       # What we've typed for the current partial
        self._last_final = ""         # The last completed final text
        self._has_prior_final = False  # Whether a previous utterance exists (for space prepend)

    def _common_prefix_len(self, a: str, b: str) -> int:
        i = 0
        while i < len(a) and i < len(b) and a[i] == b[i]:
            i += 1
        return i

    def handle_partial(self, text: str) -> list:
        if self._terminal_mode:
            return []

        ops = []
        # Prepend space if this is the first partial after a prior final
        display_text = (" " + text) if self._has_prior_final and not self._current_text else text

        prefix_len = self._common_prefix_len(self._current_text, display_text)
        chars_to_delete = len(self._current_text) - prefix_len
        chars_to_type = display_text[prefix_len:]

        if chars_to_delete > 0:
            ops.append(("backspace", chars_to_delete))
            if not self._dry_run:
                self._send_backspace(chars_to_delete)
        if chars_to_type:
            ops.append(("type", chars_to_type))
            if not self._dry_run:
                self._send_unicode(chars_to_type)

        self._current_text = display_text
        return ops

    def handle_final(self, text: str) -> list:
        ops = []

        if self._terminal_mode:
            display_text = (" " + text) if self._has_prior_final else text
            ops.append(("type", display_text))
            if not self._dry_run:
                self._send_unicode(display_text)
            self._last_final = display_text
            self._has_prior_final = True
            self._current_text = ""
            return ops

        # Backspace all current partial text
        if self._current_text:
            ops.append(("backspace", len(self._current_text)))
            if not self._dry_run:
                self._send_backspace(len(self._current_text))

        # Type the final version (with space if needed)
        display_text = (" " + text) if self._has_prior_final else text
        ops.append(("type", display_text))
        if not self._dry_run:
            self._send_unicode(display_text)

        self._last_final = display_text
        self._current_text = ""
        self._has_prior_final = True
        return ops

    def handle_undo(self) -> list:
        ops = []
        if self._last_final:
            ops.append(("backspace", len(self._last_final)))
            if not self._dry_run:
                self._send_backspace(len(self._last_final))
            self._last_final = ""
            self._has_prior_final = False
        return ops

    def handle_stop(self):
        self._current_text = ""
        self._last_final = ""
        # Keep _has_prior_final True so next session gets a space

    def handle_space(self):
        if not self._dry_run:
            self._send_unicode(" ")

    def handle_backspace(self):
        if not self._dry_run:
            self._send_backspace(1)

    def reset(self):
        """Full reset on phone disconnect — next session starts clean."""
        self._current_text = ""
        self._last_final = ""
        self._has_prior_final = False

    def _send_unicode(self, text: str):
        log.info(f"Typing {len(text)} chars: {text[:30]}")
        for char in text:
            inputs = (INPUT * 2)()
            # Key down
            inputs[0].type = INPUT_KEYBOARD
            inputs[0]._input.ki.wVk = 0
            inputs[0]._input.ki.wScan = ord(char)
            inputs[0]._input.ki.dwFlags = KEYEVENTF_UNICODE
            # Key up
            inputs[1].type = INPUT_KEYBOARD
            inputs[1]._input.ki.wVk = 0
            inputs[1]._input.ki.wScan = ord(char)
            inputs[1]._input.ki.dwFlags = KEYEVENTF_UNICODE | KEYEVENTF_KEYUP

            result = ctypes.windll.user32.SendInput(
                2, ctypes.byref(inputs), ctypes.sizeof(INPUT)
            )
            if result == 0:
                err = ctypes.get_last_error()
                log.warning(f"SendInput failed for '{char}': error {err}")

    def _send_backspace(self, count: int):
        for _ in range(count):
            inputs = (INPUT * 2)()
            # Key down
            inputs[0].type = INPUT_KEYBOARD
            inputs[0]._input.ki.wVk = VK_BACK
            inputs[0]._input.ki.dwFlags = 0
            # Key up
            inputs[1].type = INPUT_KEYBOARD
            inputs[1]._input.ki.wVk = VK_BACK
            inputs[1]._input.ki.dwFlags = KEYEVENTF_KEYUP

            ctypes.windll.user32.SendInput(
                2, ctypes.byref(inputs), ctypes.sizeof(INPUT)
            )
