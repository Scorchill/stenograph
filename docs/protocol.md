# Stenograph Wire Protocol

How the Stenograph phone app and PC app communicate. This is complete enough to
build an alternative client (for example, a client for another phone OS, or an
alternative PC server).

Protocol version: **1**

## 1. Discovery (optional)

The PC app advertises itself on the local network over mDNS / DNS-SD:

- Service type: `_stenograph._tcp.local.`
- Service name: `Stenograph on <hostname>._stenograph._tcp.local.`
- Port: the WebSocket port (default `9476`)
- TXT properties: `version=1`

Discovery is a convenience, not a requirement — a client can connect directly
using the address obtained from pairing.

## 2. Pairing

Pairing transfers the connection details and an access token from PC to phone,
out of band. The PC encodes this JSON object as a QR code:

```json
{
  "app": "stenograph",
  "version": 1,
  "token": "<32-character hex string>",
  "port": 9476,
  "ip": "<PC LAN IP address>"
}
```

The phone scans the QR code and stores `token`, `ip`, and `port`.

The token is generated on the PC (`secrets.token_hex(16)`) and stored in
`~/.stenograph/config.json`. It can be regenerated from the settings page; a
paired phone must re-pair after a regeneration.

## 3. Connection

The phone opens a WebSocket connection to:

```
ws://<ip>:<port>
```

The connection upgrade request MUST include an HTTP `Authorization` header:

```
Authorization: Bearer <token>
```

If the token is missing or wrong, the PC responds `403 Forbidden` and refuses
the connection.

## 4. Messages

Once connected, the **phone sends** messages to the **PC** as WebSocket text
frames. Each frame is a JSON object:

```json
{ "type": "<type>", "text": "<string>" }
```

`text` is meaningful only for `partial` and `final`; it may be omitted or empty
for other types.

| `type`      | Meaning                                                              |
|-------------|----------------------------------------------------------------------|
| `partial`   | Interim recognition result. The PC reconciles it against what it has |
|             | already typed (diff, backspace, retype).                             |
| `final`     | Finalized utterance. The PC replaces the current partial with it.    |
| `undo`      | Remove the last finalized utterance.                                 |
| `stop`      | End of dictation session; clears in-progress state.                  |
| `space`     | Type a single space.                                                 |
| `backspace` | Delete one character.                                                |

Unknown `type` values are ignored by the PC.

The PC does **not** send application messages back to the phone; the channel is
one-way (phone to PC) at protocol version 1. The phone observes connection
state from the WebSocket itself.

## 5. Notes for alternative clients

- The PC types using OS-level synthetic keystrokes; it has no model of the
  target application. The client is responsible for sensible `partial` /
  `final` sequencing.
- Send `partial` messages as recognition progresses, each carrying the full
  current text — the PC computes the diff.
- Send `final` when an utterance is committed, then continue with fresh
  `partial` messages for the next utterance.
