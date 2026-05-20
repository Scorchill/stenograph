# Third-Party Notices

Stenograph is released under the MIT License (see `LICENSE`). It builds on the
following open-source packages, each under its own license. When you run the PC
app from source, these are installed from PyPI.

## PC app (Python)

| Package    | License           |
|------------|-------------------|
| websockets | BSD-3-Clause      |
| zeroconf   | LGPL-2.1-or-later |
| pystray    | LGPL-3.0-or-later |
| Pillow     | MIT-CMU (HPND)    |
| Flask      | BSD-3-Clause      |
| qrcode     | BSD-3-Clause      |

## Android app

The Android app depends on standard AndroidX / Jetpack libraries (declared in
`android/app/build.gradle.kts` and `android/gradle/libs.versions.toml`), which
are distributed under the Apache License 2.0.

When the Phase 2 installer bundles the PC dependencies into a single
executable, the full license texts will be included with that distribution.
