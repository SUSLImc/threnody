# Threnody

> **An adaptive horror entity for Minecraft Forge 1.20.1 that remembers encounters, evolves through six stages, and turns the night into a hunt.**

Threnody is a configurable, server-authoritative horror mod for Minecraft Java 1.20.1. It is built to create sustained tension instead of scripted jump scares: the entity tracks encounter history per player, adapts its threat profile as it evolves, and uses the environment to close the distance.

[Project website](https://suslimc.github.io/threnody/) · [Download the latest release](https://github.com/SUSLImc/threnody/releases/latest)

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
- Threnody vanishes with smoke during daytime in dimensions with skylight, unless it is executing a judgment.
- AI and gameplay changes run on the server. Clients receive only normal entity state synchronization, including the current stage.

## Custom animations

Threnody ships with its own skeletal model and a hand-authored keyframe animation set driven by GeckoLib.

- Custom Bedrock geometry: a gaunt, elongated figure with a hinged jaw, oversized arms, and digitigrade legs.
- Eight animations: `idle`, `walk`, `run`, `crawl`, `attack`, `scream`, `transform`, and `judgment`.
- Two blended controllers: a looping movement controller and a triggered action controller, so attacks and screams play over locomotion.
- Crawling stages automatically switch to the low, splayed crawl cycle; hunting stages switch from the walk cycle to the aggressive run cycle.
- Per-stage textures with emissive glow masks, so the eyes, teeth, and veins burn brighter as the entity evolves.

Animation assets live in:

```text
assets/threnody/geo/entity/threnody.geo.json
assets/threnody/animations/entity/threnody.animation.json
```

Because the model, animations, and textures are plain data files, they can be edited in Blockbench without touching the Java code.

## The judgment system

Threnody treats survival as a covenant. Players who escape it by cheating are judged.

Detected transgressions:

- Switching into **creative** or **spectator** mode.
- Running a watched cheat command such as `/give`, `/effect`, `/kill`, `/tp`, or `/gamerule`.
- Sitting in creative mode when the mod is installed, caught by a periodic sweep.

The response escalates with each transgression inside the forgiveness window:

| Offense | Response |
| --- | --- |
| 1st | Pulled back into survival, darkness, and a warning that something noticed. |
| 2nd | Adds blindness and nausea, and any nearby Threnody is enraged and sent after the offender. |
| 3rd and beyond | Adds weakness, and Threnody is summoned next to the offender even in daylight. |

A judged Threnody enters an enraged state: it ignores daylight, plays the judgment animation, and hunts the offender directly. Transgressions are stored in persistent player NBT and are forgiven after the configured time.

The system is fully configurable and can be disabled entirely. `/summon` is deliberately **not** watched by default so the entity can still be spawned for testing.

## Requirements

- Minecraft Java Edition 1.20.1
- Forge 47.4.23 or a compatible 47.x build
- [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) 4.8.4 or newer for Forge 1.20.1
- Java 17 for development

The Gradle wrapper is included; a system Gradle installation is not required. GeckoLib is resolved automatically for development builds.

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
2. Install **GeckoLib 4.8.4+** for Forge 1.20.1 into the same `mods` directory.
3. Copy `threnody-0.1.0.jar` into the Minecraft or server `mods` directory.
4. Install both mods on the client and the server.
5. Start the game once to generate `config/threnody-common.toml`.

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
- `judgment.judgmentEnabled` - master switch for the anti-cheat judgment system.
- `judgment.judgmentRevertGameMode` - pulls judged players back into survival mode.
- `judgment.judgmentWatchCommands` - also treats watched cheat commands as transgressions.
- `judgment.judgmentWatchedCommands` - the list of command names that count as cheating.
- `judgment.judgmentExemptOperators` - exempts server operators so admins can build freely.
- `judgment.judgmentSummonOnRepeatOffense` - allows Threnody to appear next to repeat offenders.
- `judgment.judgmentForgivenessMinutes` - time before a recorded transgression is forgiven.

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
- Transgression sweeps run once every 100 ticks per player and stop as soon as the offender is back in survival.
- Natural groups are limited to one entity by the biome spawn entry.
- Vanilla path navigation is used; climbing and compact collision behavior augment it without running a separate expensive 3D pathfinder.
- Animation state is resolved on the client; the server only syncs the stage, enraged flag, and triggered animation names.
