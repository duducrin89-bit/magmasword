# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Rules

- Every commit that gets pushed needs a specific message that says what actually changed (for example "Reduce fireball cooldown to 60 seconds"), never a generic one like "update", "changes" or "fix".
- Never commit or push the `.claude/` directory or `howtopush.md`. Both are excluded locally through `.git/info/exclude`; do not stage them with `git add -f` and check `git status` before committing to confirm neither is included.

## What this is

A Fabric mod (`magmasword`) for Minecraft 26.1.2 that adds one item, the Magma Sword, with two active abilities and some held passives. `README.md` describes the player-facing behaviour (stats, recipe, cooldowns, command) and must be kept in sync when those change.

## Commands

The development machine is Windows with PowerShell; use `.\gradlew.bat` there (`./gradlew` on Linux/macOS and in CI).

- `.\gradlew.bat build` — compile and package; the jar lands in `build/libs/`. This is the only automated check: there are no tests and no linter, and CI (`.github/workflows/build.yml`) runs exactly this.
- `.\gradlew.bat runClient` — start a development client with the mod loaded.
- `.\gradlew.bat runServer` — start a development dedicated server.

Behaviour can only be verified in game. Useful there: `/give @s magmasword:magma_sword` and `/magmasword resetcooldown @s`.

Requires Java 25. Versions (Minecraft, Fabric Loader, Loom, Fabric API, mod version) live in `gradle.properties`; `fabric.mod.json` repeats the Minecraft, loader and Java constraints and has to be updated alongside it.

## Toolchain notes

- Minecraft 26.1 ships unobfuscated, so the code uses Mojang's own class names (`ServerPlayer`, `Identifier`, `LivingEntity`) and there is no remapping step. That is why `build.gradle` uses the `net.fabricmc.fabric-loom` plugin with plain `implementation` dependencies rather than `modImplementation`. Tutorials and snippets written for Yarn names or older versions will not match.
- Vanilla APIs in this version differ from older ones in ways that are easy to get wrong from memory (for example `hurtServer`, `sendOverlayMessage`, `Item.Properties.sword(...)`, items needing `setId` before construction). Check the actual Minecraft sources that Loom puts on the classpath instead of assuming a signature.
- Loom's `splitEnvironmentSourceSets()` is on: `src/main` is common code and cannot reference client classes; `src/client` can reference `main`. Anything that touches `net.minecraft.client` belongs in `src/client`.

## Architecture

`MagmaSword.onInitialize` calls `initialize()` on `ModItems`, `ModEntities`, `MagmaAbilities` and `ModCommands`. Registration happens in static initialisers of the first two, so the `initialize()` call is what forces the class to load.

All gameplay logic is server-authoritative and concentrated in `MagmaAbilities`. The other classes are thin entry points into it:

- **Launch** is triggered from `MagmaSwordItem.use` (right click, main hand only). The client side of `use` cannot see the cooldown and only checks `canLaunchFrom`; the server decides.
- **Fireball** has no keybind or custom packet. `ServerGamePacketListenerImplMixin` intercepts the vanilla swap-with-offhand action packet while the sword is held, calls `tryFireball`, and always cancels the swap. This is why the sword cannot be moved to the offhand with that key, and why the mod needs no client networking code.
- **Fast lava swimming** is `LivingEntityMixin`, which replaces `travelInLava` for a player holding the sword. It runs on both client and server because player movement is simulated client-side and validated server-side; changing it on one side only causes rubber-banding.
- **Per-tick work** (cooldown-ready messages, launch tracking, held passives and the action-bar timers) hangs off a single `END_SERVER_TICK` handler.

State is kept in two places with different lifetimes:

- Cooldowns are a persistent Fabric data attachment on the player (`COOLDOWNS`), stored as absolute overworld game times at which each ability is ready, with `0` meaning "ready, nothing to announce". They survive death and relogging. Cooldowns that expire while the player is offline are cleared on join without a message.
- In-flight launches are an in-memory map keyed by player UUID (`LAUNCHES`), deliberately not persisted. An entry suppresses fall damage and ends in a smash on landing, or silently on liquid, death, logout or failing to take off.

`MagmaFireball` extends vanilla `Fireball` and does its own area damage and knockback instead of a vanilla explosion, so it never breaks blocks. Its renderer is registered in `MagmaSwordClient` (the only client-side code) and reuses the vanilla thrown-item renderer at a larger scale.

Mixins are listed in `magmasword.mixins.json` with `defaultRequire: 1`, so an injection that fails to find its target crashes at startup rather than being skipped. Both current mixins are in the common `mixins` list; a client-only mixin would need a `client` list and to live in `src/client`.

## Resources

Adding or renaming an item touches several files that must agree on the id: the item definition in `assets/magmasword/items/`, the model in `models/item/`, the texture, the `lang/en_us.json` entry, and any recipe under `data/magmasword/recipe/`. The sword is also added to the vanilla `minecraft:swords` item tag in `data/minecraft/tags/item/swords.json`, which is what makes sword enchantments apply to it.

The recipe uses Fabric's custom ingredient types (`fabric:any`, `fabric:components`) to accept a splash potion of either fire resistance variant; a plain vanilla ingredient cannot match on potion contents.

## Conventions

- Java and JSON under `src/` are indented with tabs, except the asset and data JSON files, which use two spaces. Match the file being edited.
- Tunable numbers (cooldowns, damage, radii, speeds) are named `private static final` constants at the top of the class that uses them, with a comment where the value is derived from a vanilla one.
