# Kell Auto Clicker — free auto clicker & macro recorder (Windows, macOS, Linux)

Record your mouse clicks, key presses, scrolls and drags once, then replay them as many times as you want. Global hotkeys F7 (start), F8 (stop), F9 (record). No Java installation needed.

[![Download](https://img.shields.io/github/v/release/ds-kell/kell-auto-clicker?label=Download&logo=github)](../../releases/latest)

| OS | File to download |
|---|---|
| Windows | `.msi` installer, or `auto-clicker-windows-portable.zip` (unzip, run `auto-clicker.exe`) |
| macOS | `.dmg` (`arm64` = Apple Silicon, `x64` = Intel). Allow it under *Privacy & Security → Accessibility*. |
| Linux | `.deb`, or `.tar.gz` (run `kell-auto-clicker/bin/kell-auto-clicker`). Hotkeys need X11, not Wayland. |


## Macro Flow

The app has two ways to create a macro, but only one playback engine.

```text
Manual input UI
-> MacroManualRecorder
-> TableItems
-> MacroPlayer
```

```text
Native record hook
-> MacroRecorder
-> TableItems
-> MacroPlayer
```

Main classes:

- `AutoClicker`: UI orchestration, start/stop lifecycle, native hook wiring, save/load entry points.
- `MacroManualRecorder`: creates macro items from the manual UI forms.
- `MacroRecorder`: creates macro items from native mouse/key/wheel events.
- `MacroPlayer`: replays all macro items.
- `MacroFileStore`: saves/loads JSON and legacy TXT macro files.
- `AutoClickerUtils`: shared parsing, key mapping, clamp, and window helpers.

## Manual Macro

Manual actions are created from the UI:

- Add Click
- Add Key
- Add Scroll
- Add Drag & Drop
- Add Note

For key input, entering multiple characters such as `wasd` creates separate key actions:

```text
W
A
S
D
```

Playback sends real `Robot.keyPress()` / `Robot.keyRelease()` events, not typed text.

## Recorded Macro

Record mode captures native events:

- mouse move
- mouse drag
- mouse down
- mouse up
- mouse wheel
- key down
- key up

Recorded clicks are event-level, so holding a mouse button or keyboard key is represented by separate down/up rows. The hold duration is written back to the down row.

Example:

```text
Left Down  Hold(ms)=820
Left Up
```

Keyboard combinations keep their real down/up order:

```text
Key Down Control
Key Down C
Key Up C
Key Up Control
```

## Mouse Replay Path

Recorded macros have two mouse path modes in the UI.

### Recorded Mouse Path

Uses the recorded mouse move/drag points as the replay path.

Intermediate move points get a tiny jitter so repeated runs are not exactly identical. Mouse down/up events still use the exact recorded target coordinates to avoid clicking the wrong place.

### Random Mouse Path

Ignores recorded intermediate move points and generates a random Bezier-style path to each recorded mouse down/up target.

The target click coordinates stay exact, but the route to reach them changes between runs.

## Timing

Recorded macro playback uses an absolute schedule instead of repeatedly sleeping relative to the previous action. This prevents delay drift from accumulating across long recordings.

The goal is that click timing stays close to the original recording. OS scheduling, JVM pauses, and very slow generated mouse paths can still introduce small timing variance.

For generated Bezier paths, the number of sampled points is based on movement distance:

- short movement: fewer points, faster movement
- medium movement: moderate points
- long movement: more points, smoother path

## Hotkeys

- `F7`: Start playback
- `F8`: Stop playback
- `F9`: Start/stop recording

Native hotkeys and recording require the global native hook. On macOS this usually needs Accessibility permission.

## Build And Test

```bash
mvn -q test
mvn package -Dskip.win.portable   # installer for the current OS in target/dist
```

## ☕ Support / Ủng hộ

Free and open source. If it saved you time, you can buy me a coffee — thank you!

[![PayPal](https://img.shields.io/badge/PayPal-Donate-00457C?logo=paypal&logoColor=white)](https://paypal.me/truyennt)
[![VietQR](https://img.shields.io/badge/VietQR-Chuy%E1%BB%83n%20kho%E1%BA%A3n-E31837)](https://github.com/ds-kell/ds-kell/blob/main/DONATE.md)

<img src="https://raw.githubusercontent.com/ds-kell/ds-kell/main/assets/donate-vietqr.jpg" alt="VietQR VietinBank" width="180">

## License

MIT
