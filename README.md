# SeaClient

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-informational.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.20.1-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25+-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A high-performance, dark-themed Fabric client mod for Minecraft 26.3 featuring sharp-rectangular UI styling, live Hypixel Bedwars & PvP telemetry, real-time Windows media integration, and an in-game FPS optimization engine.

Originally developed as a personal custom client setup, now fully generalized and open-sourced under the MIT License for anyone to use, play, fork, or study.

---

## Ready-to-Run Release

You do **not** need to compile the code yourself to use SeaClient. A pre-compiled, production-ready build is available directly in this repository:

- **Mod Jar**: [`release/SeaClient-1.0.0.jar`](release/SeaClient-1.0.0.jar)
- **Release Bundle (ZIP)**: [`release/SeaClient-v1.0.0-Fabric-MC26.3.zip`](release/SeaClient-v1.0.0-Fabric-MC26.3.zip)
- **Install Guide**: [`release/INSTALL.md`](release/INSTALL.md)

---

## Features

### 1. Sharp Rectangular Interface
- Custom zero-border-radius design language across the title screen and menus.
- Starfield and planet horizon visual backdrop.
- Dynamic time-of-day greeting customized to the current player's profile (`Good morning`, `Good afternoon`, `Good evening`).
- Interactive 3D Player Skin model with live click-and-drag rotation, auto-resolved directly via Mojang & mc-heads.
- Quick "Continue" session card dynamically indexing recent worlds and multiplayer servers with cached icon previews.

### 2. Hypixel Bedwars & PvP Overlays
- **Bedwars Generator Timers**: Real-time countdowns for Diamond and Emerald tier spawns.
- **Resource HUD**: Automatic tracking of Iron, Gold, Diamonds, and Emeralds in inventory.
- **Wool Counter**: Instant blocks counter displayed on HUD.
- **PvP Telemetry**:
  - Independent Left / Right Click CPS counters with hit-timing tracking.
  - Active hit combo streak counter.
  - Accurate server-side reach distance tracker.
  - Keystrokes overlay (WASD + Space + Mouse Buttons).

### 3. In-Game Optimization Engine (`Right-Shift`)
- Lightweight Mixin hooks replacing redundant rendering passes.
- Instant FPS toggles categorized into **Home**, **Bedwars**, **HUD**, **Render**, **Particles**, **World**, and **System**.
- Low-latency input handling and sound engine optimization.

### 4. Windows System Media Bridge
- Live background audio tracker reading track title, artist, and album art from active Windows media sessions (Spotify, Tidal, browsers).
- Interactive media controls (Play/Pause, Skip, Previous) integrated directly into the Title Screen and in-game overlay.

---

## Installation

### Prerequisites
- **Minecraft**: `26.3`
- **Mod Loader**: [Fabric Loader](https://fabricmc.net/) `>= 0.18.0` (Recommended: `0.20.1`)
- **Runtime**: [Java 25](https://adoptium.net/) or higher
- **Dependency**: [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 26.3

### Step-by-Step
1. Install Fabric Loader for Minecraft 26.3 using the official installer.
2. Put `fabric-api-*.jar` into your Minecraft `mods` folder:
   - **Windows**: `%appdata%\.minecraft\mods`
   - **macOS**: `~/Library/Application Support/minecraft/mods`
   - **Linux**: `~/.minecraft/mods`
3. Download [`SeaClient-1.0.0.jar`](release/SeaClient-1.0.0.jar) and place it into the same `mods` folder.
4. Launch the game using the Fabric 26.3 launcher profile.
5. In game, press `Right-Shift` to configure client features and overlays.

---

## Building from Source

If you want to modify or compile SeaClient yourself:

1. **Clone the repository**:
   ```bash
   git clone https://github.com/seaze-code/SeaClient.git
   cd SeaClient
   ```

2. **Build the mod using the Gradle wrapper**:
   - **Windows**:
     ```powershell
     .\gradlew.bat build
     ```
   - **macOS / Linux**:
     ```bash
     ./gradlew build
     ```

3. The compiled artifact will be generated at:
   ```
   build/libs/SeaClient-1.0.0.jar
   ```

---

## Project Structure

```
SeaClient/
├── .github/              # GitHub configuration & workflows
├── gradle/               # Gradle wrapper distribution
├── release/              # Ready-to-use compiled .jar & release bundles
│   ├── SeaClient-1.0.0.jar
│   ├── SeaClient-v1.0.0-Fabric-MC26.3.zip
│   └── INSTALL.md
├── src/main/java/com/seaclient/menu/
│   ├── SeaClientMod.java          # Client entrypoint & media initialization
│   ├── data/                      # Last-played world/server tracker
│   ├── hud/                       # CPS, PvP stats, Bedwars trackers & renderer
│   ├── media/                     # Windows SystemMediaTransportControls bridge
│   ├── mixin/                     # Fabric Mixins for UI & engine hooks
│   ├── optimize/                  # Performance optimizer & settings state
│   ├── screen/                    # Sharp title screen, icons, & optimizer GUI
│   ├── skin/                      # Dynamic player skin fetching & rendering
│   └── theme/                     # Sharp-corner UI theme colors & widgets
├── src/main/resources/            # Assets, fonts, icons, mixin definitions
├── build.gradle
├── gradle.properties
└── settings.gradle
```

---

## License

This project is licensed under the [MIT License](LICENSE). Feel free to use, fork, and build upon it.
