# Threnody

> **An adaptive horror entity for Minecraft Forge 1.20.1 that stalks you in silence, freezes the moment you look at it, and is gone before anyone believes you.**

Threnody is a server-authoritative horror mod for Minecraft Java 1.20.1. It is built around patience rather than jump scares: a towering, emaciated figure that keeps its distance, holds perfectly still while you are watching, and closes the gap the second you turn away.

[Project website](https://suslimc.github.io/threnody/) · [Download the latest release](https://github.com/SUSLImc/threnody/releases/latest)

## How it hunts

Threnody has three moods, and almost all of the fear lives in the first one.

### Lurking

- It holds a configurable distance and simply watches you.
- **It stops dead while you are looking at it.** No drifting, no turning, no being pushed around.
- It only advances while nobody has eyes on it, so it is always closer than it was.
- Stare at it too long and it refuses to be studied — it leaves.

### Hunting

- Once its patience runs out, or you get too close, or you hit it, it shrieks and commits.
- It moves fast, hunched forward, and it no longer cares about being seen.
- Light drains out of the area around its target.
- In later tiers it tears through the blocks in its way instead of going around them.

### Vanishing

- It vanishes at dawn.
- It vanishes if you watch it for too long.
- **It vanishes the moment it kills you.** It does not stand over your corpse waiting for you to respawn.

## Design

The entity is a tall, thin silhouette rather than a detailed monster. Its textures are almost pure shadow, so at range the only thing you can resolve is a shape that is the wrong height and two burning eyes. Detail is deliberately withheld — the horror is in proportion and stillness, the same approach used by the best-regarded Minecraft horror mods.

- Custom Bedrock geometry roughly **2.85 blocks tall** with elongated arms that hang past the knees.
- A featureless head with no mouth, only eyes.
- Per-tier textures with emissive glow masks so the eyes brighten as it escalates.

## Sound

All audio is original and synthesised for this mod — no recognisable vanilla mob noise.

| Sound | When you hear it |
| --- | --- |
| `drone` | Detuned sub tones that beat against each other while it is nearby |
| `heartbeat` | Your own pulse, quickening as the distance closes |
| `whisper` | Breath-like noise that never resolves into words |
| `knock` | A rare cue that something moved just out of sight |
| `seen` | The rising sting for the moment it notices you |
| `shriek` | A dissonant, distorted cluster when it commits to the hunt |
| `chase` | Pulsing rumble while it is running you down |
| `step` | A heavy body meeting the ground |
| `vanish` | The space it leaves behind |

Every clip is **mono**, so Minecraft applies full directional attenuation and you can actually tell where it is.

## Escalation tiers

Threnody grows more dangerous the longer a hunt continues. Each tier raises health, speed and damage, brightens the eyes, and increases its size.

1. Distant and patient
2. Faster, and it slows what it touches
3. Compact enough to fit through gaps you thought were safe
4. Strong enough to open a path through blocks
5. Quicker still, and it takes the light with it
6. Everything at once, with nothing held back

## Memory

Every encounter is written into persistent player data. Recent and violent encounters raise that player's priority the next time Threnody chooses prey, and entries expire on a configurable timer.

## Requirements

- Minecraft Java Edition 1.20.1
- Forge 47.4.23 or a compatible 47.x build
- [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) 4.8.4 or newer for Forge 1.20.1
- Java 17 for development

The Gradle wrapper is included; a system Gradle installation is not required.

## Build

From this directory on Windows:

```powershell
.\gradlew.bat clean build
```

The distributable jar is generated at:

```text
build\libs\threnody-0.2.0.jar
```

For a development client or dedicated server:

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
```

## Install and use

1. Install Forge 47.x for Minecraft 1.20.1.
2. Install **GeckoLib 4.8.4+** for Forge 1.20.1 into the same `mods` directory.
3. Copy `threnody-0.2.0.jar` into the Minecraft or server `mods` directory.
4. Install both mods on the client and the server.
5. Start the game once to generate `config/threnody-common.toml`.

Threnody spawns rarely at night. Operators can also test it immediately with:

```mcfunction
/summon threnody:threnody
```

## Configuration

`threnody-common.toml` contains:

**`[behaviour]`**

- `freezeWhenWatched` — stand completely still while a player is looking at it.
- `stalkDistance` — blocks it tries to keep between itself and its prey.
- `secondsBeforeHunt` — how long it stalks before it charges.
- `secondsWatchedBeforeVanish` — how long it tolerates being stared at.
- `vanishAfterKill` — leave immediately after killing a player.
- `darknessWhenClose` — drain the light around players it is closing in on.

**`[audio]`**

- `ambientSounds` — drones, whispers and distant knocks.
- `heartbeat` — the proximity heartbeat.

**`[spawn]`**

- `spawnEnabled`, `spawnLightThreshold`.

**`[memory]`**

- `memoryCapacity`, `memoryDecayMinutes`.

**`[performance]`**

- `disableBlockBreaking`.

Block breaking additionally respects the `mobGriefing` game rule and only affects blocks in:

```text
data/threnody/tags/blocks/breakable_by_threnody.json
```

Datapacks can replace or extend that tag. Natural spawn biomes and weight can be customized by overriding:

```text
data/threnody/forge/biome_modifier/add_threnody_spawns.json
```

## Performance and multiplayer notes

- All behaviour is server-authoritative; clients only receive the tier, state and watched flag.
- Observation checks run once per tick against nearby players only, using a squared-distance cutoff.
- Repathing is throttled to once every ten ticks while stalking.
- Block-breaking checks run once per second and only in the two applicable tiers.
- Memory is capacity-limited and expired lazily when used.
- Natural groups are limited to one entity by the biome spawn entry.

## Credits

Threnody is designed, built, and maintained by **Susli76**.

- TikTok: [@susli.ae](https://www.tiktok.com/@susli.ae)
- GitHub: [SUSLImc/threnody](https://github.com/SUSLImc/threnody)

Animation runtime provided by [GeckoLib](https://github.com/bernie-g/geckolib). Released under the MIT License.
