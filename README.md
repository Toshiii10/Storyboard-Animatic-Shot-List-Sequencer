# BoardFlow

**Web development is now the primary track.** Run `build_web_windows.bat`, then `run_web_windows.bat`, and visit http://localhost:8080. The Spring MVC web app follows the shared application-service/domain/repository architecture planned for the future JavaFX client. See [web/README.md](web/README.md) for setup, features, architecture, and tests.

The documentation below describes the earlier Swing prototype.

A Java Swing storyboard and silent animatic sequencer for multimedia students and indie filmmakers.

## Run on Windows

Install a JDK (Java 17 or newer), then double-click `build_windows.bat` followed by `run_windows.bat`. Both scripts work from any folder. No database or downloaded dependencies are required. The bundled FlatLaf library provides the dark theme; the app can also run with the JDK look and feel.

The app starts with three example shots. Choose **New** for an empty storyboard. Select a shot type and click **Add shot**. Edit the title, camera angle, dialogue/action notes, seconds (1-3600), and exit transition. Use **Choose image** for a PNG, JPEG, or GIF, and **Up/Down** to reorder. **Remove** asks before deleting a card. The header always shows shot count and total runtime.

**Camera checklist** gives advice for the current shot type. **Use recommended timing** sets establishing shots to 6 seconds, action shots to 8 seconds, and dialogue to approximately 150 words per minute plus a pause (minimum 3 seconds). These are editable planning estimates.

**Save / Save as** writes a `.boardflow` XML project with embedded images, so the project remains portable if source images move. Images are resized to at most 1600 pixels on their longest edge. **Open** restores shot order and all fields. New, Open, and Close prompt to save unsaved work. Ctrl+S saves.

**Play animatic** shows a timed, silent preview with pause and restart. Cut, dissolve, and fade-to-black transitions are supported; effects occupy the final 0.6 seconds of a shot and do not add to runtime. Missing images appear as labeled placeholders. This version does not export video or play recorded audio.

## OOP structure

- `StoryboardPanel extends JPanel`: shared card UI and abstract `getCameraSetupChecklist()` / `computeRecommendedDuration()` methods.
- `EstablishingShot`, `DialogueCloseUp`, `ActionTrackingShot`: concrete subclasses override both methods.
- `BoardFlowFrame`: manages a sequence of `StoryboardPanel` instances inside a `JScrollPane`; calls the overridden methods through the superclass.
- `ShotData`: shot fields, independent of the UI.
- `ProjectIO`: validated, versioned save/load with atomic file replacement where supported.
- `AnimaticDialog`: timed playback on a Swing timer using a monotonic clock.

## Checks

Run `test_windows.bat` to compile and execute headless checks for polymorphic recommendations, project round-trip (including images and Unicode notes), invalid project rejection, and playback boundaries. Tests use only the JDK.
