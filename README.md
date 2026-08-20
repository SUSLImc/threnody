# Threnody

> **An adaptive horror entity for Minecraft Forge 1.20.1 that remembers encounters, evolves through six stages, and turns the night into a hunt.**

Threnody is a configurable, server-authoritative horror mod for Minecraft Java 1.20.1. It is built to create sustained tension instead of scripted jump scares: the entity tracks encounter history per player, adapts its threat profile as it evolves, and uses the environment to close the distance.

[Project website](index.html) · [Download the latest build](build/libs/threnody-0.1.0.jar)

## Current gameplay

- Threnody spawns rarely in Overworld biomes at night and only below the configured light threshold.
- It hunts players through six progressively stronger stages:
  - **Stalker** - cautious base form.
  - **Crawler** - moves faster, climbs blocked vertical surfaces, and slows players.
  - **Squeeze** - uses a compact collision box that fits through one-block-high openings.
  - **Breaker** - can break blocks in the `threnody:breakable_by_threnody` tag when mob griefing is enabled.
  - **Huntmaster** - becomes faster and inflicts darkness.
  - **Eschaton** - combines climbing, block breaking, the strongest attacks, and a distinct particle effect.
- Each player has bounded encounter memory stored in persistent player NBT. Recent and dangerous encounters increase that player's targeting priority. Entries expire according to the config.
- Threnody vanishes with smoke during daytime in dimensions with skylight.
- AI and gameplay changes run on the server. Clients receive only normal entity state synchronization, including the current stage.

The implementation intentionally uses Minecraft's built-in humanoid model, particles, effects, and sounds so the mod has no runtime dependencies beyond Forge. Custom animated models and audio can be added later without changing the server behavior.

## Requirements

- Minecraft Java Edition 1.20.1
- Forge 47.4.23 or a compatible 47.x build
- Java 17 for development

The Gradle wrapper is included; a system Gradle installation is not required.

## Build

From this directory on Windows:

```powershell
.\gradlew.bat clean build
```

The distributable jar is generated at:

```text
build\libs\threnody-0.1.0.jar
```

For a development client:

```powershell
.\gradlew.bat runClient
```

For a development dedicated server:

```powershell
.\gradlew.bat runServer
```

## Install and use

1. Install Forge 47.x for Minecraft 1.20.1.
2. Copy `threnody-0.1.0.jar` into the Minecraft or server `mods` directory.
3. Install the same mod jar on both the client and server.
4. Start the game once to generate `config/threnody-common.toml`.

Threnody can spawn naturally at night. Operators can also test it immediately with:

```mcfunction
/summon threnody:threnody
```

## Configuration

`threnody-common.toml` contains:

- `spawn.spawnEnabled` - enables or disables spawn-rule acceptance.
- `spawn.spawnLightThreshold` - maximum local light level from 0 through 15.
- `memory.memoryCapacity` - maximum saved encounters per player.
- `memory.memoryDecayMinutes` - lifetime of remembered encounters.
- `performance.disableBlockBreaking` - disables all Threnody block breaking.

Block breaking additionally respects the `mobGriefing` game rule and only affects blocks in:

```text
data/threnody/tags/blocks/breakable_by_threnody.json
```

Datapacks can replace or extend that tag. Natural spawn biomes and weight can be customized by overriding:

```text
data/threnody/forge/biome_modifier/add_threnody_spawns.json
```

## Performance and multiplayer notes

- Target reevaluation occurs every 100 ticks unless the current target becomes invalid.
- Block-breaking checks occur once per second and only in the two applicable stages.
- Memory is capacity-limited and expired lazily when used.
- Natural groups are limited to one entity by the biome spawn entry.
- Vanilla path navigation is used; climbing and compact collision behavior augment it without running a separate expensive 3D pathfinder.
