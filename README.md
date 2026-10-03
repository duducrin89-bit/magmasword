# Magma Sword

A Fabric mod for Minecraft 26.1.2 that adds the **Magma Sword**: an unbreakable blade with a launch-and-smash dash, a mega fireball, fire resistance and fast lava swimming.

## The sword

- 10 attack damage, 1.6 attack speed (netherite tier)
- Unbreakable and fire resistant, so it survives lava and fire as a dropped item
- Sets enemies on fire for 5 seconds on hit
- Found in the Combat creative tab, after the netherite sword

### Crafting

Shapeless recipe:

- 1 Netherite Ingot
- 1 Magma Block
- 1 Lava Bucket
- 1 Splash Potion of Fire Resistance (regular or extended)
- 2 Sticks

## Abilities

The sword must be in your main hand.

| Ability | Input | Cooldown |
| --- | --- | --- |
| Launch | Use (right click) | 45 s |
| Fireball | Swap-with-offhand key (`F` by default) | 90 s |

### Launch

Flings you in the direction you are looking. You have to start on the ground or in lava. The landing deals no fall damage to you and smashes the ground like a mace: every entity within 5 blocks takes 4 to 20 damage, growing with how far you fell, and is knocked away. Your teammates, your tamed pets and armor stands are spared. Landing in water or lava ends the launch without a smash.

### Fireball

Shoots a large, fast fireball that flies up to 100 blocks. On impact it deals 12 damage and knockback to everything within 10 blocks except you, and breaks no blocks. Firing it gives you Slowness II, Resistance IV and Regeneration III for 10 seconds.

Because the swap key fires the fireball, the sword cannot be moved to the offhand with that key while you hold it.

### Passives while held

- Fire Resistance
- Fast swimming in lava
- Flame particles around you
- Both cooldown timers shown above the hotbar

A chat message tells you when an ability is ready again. Cooldowns are saved with the player, so they persist through death and relogging.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/magmasword resetcooldown <player>` | Operator (gamemaster level) | Makes both abilities ready immediately |

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.5 or newer
- Fabric API
- Java 25

The mod is needed on both the client and the server.

## Building

```
./gradlew build
```

The mod jar is written to `build/libs/`. To start a development client, run `./gradlew runClient`.

For IDE setup, see the [Fabric Documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up).

## Version
This is V1 and it is being developed right now

## License

Available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
