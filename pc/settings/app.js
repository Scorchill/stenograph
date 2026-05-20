// Load config on startup and poll for connection status

let pollInterval = null;

async function loadConfig() {
    try {
        const res = await fetch("/api/config");
        const config = await res.json();
        updateUI(config);
    } catch (err) {
        console.error("Failed to load config:", err);
    }
}

function updateUI(config) {
    // "Pause Typing" is the inverse of enabled:
    // enabled=true means typing is active, so pause is unchecked
    document.getElementById("toggle-enabled").checked = !config.enabled;
    document.getElementById("toggle-terminal").checked = config.terminal_mode;
    updateConnectionStatus(config.connected);
}

function updateConnectionStatus(connected) {
    const el = document.getElementById("connection-status");
    if (connected) {
        el.textContent = "Connected";
        el.style.color = "#4ade80";
        el.style.textShadow = "0 0 8px rgba(74, 222, 128, 0.5)";
    } else {
        el.textContent = "Disconnected";
        el.style.color = "#71717a";
        el.style.textShadow = "none";
    }
}

async function pollStatus() {
    try {
        const res = await fetch("/api/config");
        const config = await res.json();
        updateConnectionStatus(config.connected);
    } catch (err) {
        console.error("Poll failed:", err);
    }
}

// Toggle: Pause Typing
document.getElementById("toggle-enabled").addEventListener("change", async (e) => {
    // Pause checked = enabled false
    const enabled = !e.target.checked;
    try {
        await fetch("/api/config", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ enabled }),
        });
        showStatus("Settings saved");
    } catch (err) {
        showStatus("Failed to save", true);
    }
});

// Toggle: Terminal Mode
document.getElementById("toggle-terminal").addEventListener("change", async (e) => {
    try {
        await fetch("/api/config", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ terminal_mode: e.target.checked }),
        });
        showStatus("Settings saved");
    } catch (err) {
        showStatus("Failed to save", true);
    }
});

// Regenerate Token
document.getElementById("btn-regenerate").addEventListener("click", async () => {
    const btn = document.getElementById("btn-regenerate");
    btn.textContent = "Regenerating...";
    btn.disabled = true;
    try {
        await fetch("/api/pairing/regenerate", { method: "POST" });
        // Reload QR image with cache-bust
        const qr = document.getElementById("qr-code");
        qr.src = "/api/pairing/qr?t=" + Date.now();
        showStatus("Token regenerated — scan the new QR code");
    } catch (err) {
        showStatus("Failed to regenerate token", true);
    } finally {
        btn.textContent = "Regenerate Token";
        btn.disabled = false;
    }
});

function showStatus(msg, isError) {
    const el = document.getElementById("status-message");
    el.textContent = msg;
    el.style.color = isError ? "#f87171" : "#4ade80";
    setTimeout(() => { el.textContent = ""; }, 3000);
}

// Init
document.addEventListener("DOMContentLoaded", () => {
    loadConfig();
    pollInterval = setInterval(pollStatus, 3000);
});
