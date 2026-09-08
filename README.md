# Broken Script 3.0 — Fan-Made

> `[SYSTEM] script integrity 47.2% and falling.`

A long-form (20+ hour) **psychological & meta-horror** mod for **Minecraft 1.21.5** on **NeoForge**.
Mod ID: `brokenscript` · Package: `net.brokenscript.mod` · Java 21.

---

## Building & Running

```bash
./gradlew build          # produces build/libs/brokenscript-3.0.0.jar
./gradlew runClient      # dev client
./gradlew runServer      # dev dedicated server
```

Requires JDK 21 (Gradle's toolchain resolver will fetch one if missing).
Toolchain: NeoGradle 7.1.x · NeoForge **21.5.98** · Parchment mappings for parameter names.

---

## Architecture Overview

```
net.brokenscript.mod
├── BrokenScriptMod        # @Mod entry — wires DeferredRegisters + mod-bus listeners
├── Config                 # consent switches: file writes, fake crashes, input glitches...
├── infection/             # InfectionSavedData (SavedDataType, world-saved 0..100) + stages
├── horror/                # HorrorDirector (server "game master") + LogsUncannyManager
├── entity/                # Herobrine, Null, Entity 303/505, Lick, The Reaper
├── block/                 # TerminalBlock(+Entity), PurificationAltarBlock
├── network/               # PayloadRegistrar + 6 CustomPacketPayload records
├── worldgen/              # TerminalVaultStructure + hand-built piece
├── event/CommonEvents     # server game-bus listeners
├── client/                # Dist.CLIENT only: HUD layers, CLI screen, phantom tab list,
│                          # input glitcher, chat corruption
└── util/                  # TerminalProtocol, CorruptedNames, ModDamageTypes
```

### 1. Infection & stages
`InfectionSavedData` persists via 1.21.5's `SavedDataType` codec system into
`saves/<world>/data/brokenscript_infection.dat`. `HorrorDirector` ticks once per
second on the server thread: infection rises over time, faster below y=0 and
near cursed netherrack scars, and *drains* near Purification Altars. Stages:

| Stage | Level | What happens |
|---|---|---|
| 1 Dormant | 0–25 | Fake ambient sounds, footsteps behind you |
| 2 Watching | 26–50 | Distant spectators (Null / 505), phantom chat joins, first LOGS_UNCANNY files |
| 3 Hunting | 51–75 | Herobrine / 303 / Lick aggression, chat corruption, UI distortion |
| 4 Collapse | 76–100 | World degradation, inverted controls, fake crashes, The Reaper |

### 2. Entities
- **Herobrine** — teleports behind you when unobserved, swaps lone hotbar items
  for corrupted ones, plants redstone torches out of view, fake explosions.
  Stared at ≥5s → vanishes.
- **Null** — 15-block blindness aura, dowses torches/lanterns/campfires,
  relocates via raycast checks so you never quite see it.
- **Entity 303 / 505** — one class, two types. 505 spectates at long range and
  vanishes on approach; 303 sends `GlitchEffectPayload`s: UI scramble, mouse
  sensitivity randomization, rare fake crash overlay.
- **Lick** — melee digger; carves Cursed Crosses (orthogonal trenches lined
  with netherrack/obsidian).
- **The Reaper** — gravity-less drift, no-clips through soft blocks, damage via
  the `brokenscript:reaper` damage type (tagged `bypasses_armor`/`shield`/`effects`).
  Immune to players; only Holy Light from Purification Altars repels it.

### 3. Meta-horror
- **Fake chat + tab list** — `FakeSystemMessagePayload` injects
  "`Null joined the game`" lines; `PhantomRoster` fabricates
  `ClientboundPlayerInfoUpdatePacket`s client-side (AT-opened `entries` field)
  so phantoms appear in TAB with **999ms ping**.
- **LOGS_UNCANNY** — real `.txt` files in `<gamedir>/LOGS_UNCANNY/` written on
  the IO pool: cryptic lines, your (slightly wrong) coordinates, ASCII art.
  Capped at 64 files; disable with `allowUncannyLogs=false`.
- **Input glitcher** — `MovementInputUpdateEvent` negates the move vector
  (W/S inversion) during stage 4 events; mouse sensitivity jitters and is
  always restored afterwards.
- **Item rename interceptor** — tooltips occasionally become `ERR_404.json`,
  `DO_NOT_OPEN.txt`, ...

### 4. Endgame — "Fixing the Script"
Find the **Terminal Vault** (rare buried structure, obsidian server room).
Feed the **Terminal Block** 8 **Source Code Fragments** (dropped by sealed
stage-3/4 entities, or in vault chests). The CLI screen opens; enter the 5-part
repair sequence (`help` lists it) while the terminal spawns wave assaults.
Complete `commit fixed_the_script` and the infection is purged **permanently**.

---

## Notes
- All world mutation happens on the server main thread (director tick, payload
  handlers default to `HandlerThread.MAIN`); file I/O uses `Util.ioPool()`.
- Every invasive feature has a config kill-switch (`config/brokenscript-common.toml`).
- Entity renderers are `NoopRenderer` placeholders wired for real models later —
  invisible stalkers that snuff your torches are, frankly, worse.

*This is a fan-made homage. It is not affiliated with the original Broken Script.*
