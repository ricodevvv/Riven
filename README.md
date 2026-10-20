# Riven

Minigame framework for Spigot 1.8.8. It started as the framework inside UltraBedwars (a local, multi-arena port of Hyriode's Hyrame) and grew into its own library, with everything a big minigame plugin needs: games and teams, guided map setup, editable menus, player data, stats, cosmetics, parties and a full Redis network layer.

It's split in four modules so you only pull what you use:

| Module | What's inside |
| --- | --- |
| `riven-core` | Items, menus (also YAML editable ones), packet sidebars, holograms, NPCs, packet interceptor, sign/anvil/chat input, languages, commands, config helpers, fancy chat messages, placeholders, parties, lobby protection |
| `riven-game` | Multi-arena games, teams, protocols (death, spectator, last hitter, waiting room...), generators, world adapters (zip and SlimeWorldManager), HyriBuild style guided setup, votes, kits with levels, loot tables, join signs with rotation, game timelines |
| `riven-data` | MySQL/MariaDB/SQLite pool, JSON document storage, player profiles, wallets, scoped statistics, level curves and prestiges, leaderboards, cosmetics, game history |
| `riven-network` | Redis pub/sub with reconnection, server registry with heartbeats, network wide player lookup, transfers with context, load balancer, drain mode, cross-server parties, global spectating, network broadcasts |

`riven-game`, `riven-data` and `riven-network` already pull `riven-core`.

## Requirements

- Spigot or Paper 1.8.8 (it uses `v1_8_R3` NMS directly).
- Java 8 or newer.
- Optional: PlaceholderAPI, SlimeWorldManager 2.2.1, a MySQL/MariaDB server, a Redis server.

## Adding it to your plugin

Riven lives on my Reposilite.

Gradle:

```groovy
repositories {
    maven { url = 'https://maven.ricodevvv.com/public-repo' }
}

dependencies {
    implementation 'dev.ricodevvv:riven-game:1.0.0-SNAPSHOT'
    implementation 'dev.ricodevvv:riven-data:1.0.0-SNAPSHOT'
    implementation 'dev.ricodevvv:riven-network:1.0.0-SNAPSHOT'
}
```

Maven:

```xml
<repositories>
  <repository>
    <id>ricodevvv</id>
    <url>https://maven.ricodevvv.com/public-repo</url>
  </repository>
</repositories>

<dependency>
  <groupId>dev.ricodevvv</groupId>
  <artifactId>riven-game</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Shade it into your jar and **relocate** `dev.ricodevvv.riven` (plus HikariCP, MariaDB and Jedis if you use data or network). Riven keeps a static instance, so two plugins on the same server sharing the same package would step on each other.

```groovy
shadowJar {
    relocate 'dev.ricodevvv.riven', 'me.you.myplugin.libs.riven'
    relocate 'com.zaxxer.hikari', 'me.you.myplugin.libs.hikari'
    relocate 'org.mariadb.jdbc', 'me.you.myplugin.libs.mariadb'
    relocate 'redis.clients', 'me.you.myplugin.libs.jedis'
    relocate 'org.apache.commons.pool2', 'me.you.myplugin.libs.pool2'
}
```

## Getting started

```java
@Override
public void onEnable() {
    LanguageManager language = new LanguageManager(this, "en", "en", "es");
    language.load();

    Riven riven = Riven.init(this, language);
    GameFramework games = riven.install(new GameFramework());
    DataModule data = riven.install(new DataModule(DatabaseConfig.from(getConfig().getConfigurationSection("database")), true));

    if (getConfig().getBoolean("network.enabled")) {
        riven.install(new NetworkModule(RedisConfig.from(getConfig().getConfigurationSection("redis")), "game-1", "skywars", ServerMode.GAME));
    }

    riven.getCommandManager().register(new PartyCommand(riven.getPartyManager(), "myplugin.party.silentjoin"));
}

@Override
public void onDisable() {
    Riven.get().shutdown();
}
```

Modules are enabled in install order and disabled in reverse. You can write your own by implementing `RivenModule`.

## Core

**Commands.** Extend `RivenCommand`, add children for subcommands and register the root. No plugin.yml needed. `CommandContext` parses numbers, players and enums, and throwing `CommandException` shows the message to the sender.

```java
public class CoinsCommand extends RivenCommand {

    public CoinsCommand() {
        super("coins");
        this.permission = "myplugin.coins";
        this.usage = "<amount> [player]";
        this.minArgs = 1;
    }

    @Override
    protected void execute(CommandContext context) {
        Player target = context.getOnlinePlayerOrSelf(1);
        profiles.get(target).getWallet().add("coins", context.getInt(0));
    }
}
```

**Editable menus.** Drop `menus/<id>.yml` files and open them with `riven.getMenuRegistry().open(player, "cosmetics")`. Items support `slot`/`slots`, any item field from `ItemParser`, placeholders, permissions and actions: `[close]`, `[open] menu`, `[back]`, `[command] cmd`, `[console] cmd`, `[message] text`, `[sound] SOUND:1:1`, `[refresh]`, `[button] id` and anything you register with `registerAction`. Code made buttons (`registerButton`) let the file decide the slot while your code decides the item. `list-slots` plus a `MenuContext` with entries gives you paginated lists (kits, cosmetics, maps).

**Fancy messages.** `FancyMessage` builds hover/click messages in code, and language files can use `{{text|run:/cmd|hover:text}}` blocks. `FancyMessage.sendAuto` sends plain or fancy depending on the text.

**Placeholders.** `riven.getPlaceholders().register("sw", (player, params) -> ...)` makes `%sw_...%` work in Riven menus and, if PlaceholderAPI is installed, everywhere else too.

**Parties.** `PartyManager` handles invites that expire, open/closed parties, kicks, leader transfer, silent joins for staff, party chat (`/pc` or toggled) and offline members. Messages use the `party.*` language keys and fall back to English. `PartyCommand` and `PartyChatCommand` are ready to register. Storage, lookup and message delivery are pluggable, that's how the network module makes parties span servers.

**Lobby.** `LobbyProtection` blocks breaking, placing, drops, hunger, damage, mobs, weather and void falls in lobby worlds, each rule toggleable, and gives auto-fly to whoever has the permission.

Also in core: `ConfigFile` (copies from the jar and fills in missing keys on update), `ItemParser` (items from config in a section or a single line), `SoundData`, `JsonFolder` (one JSON file per object), `Tasks`, `Countdown`, `Cooldowns`, `ChatInputManager`, `UpdateChecker`, `BungeeUtil`.

## Game

The engine is the UltraBedwars one: `Game`, `GamePlayer`, `GameTeam`, `GameManager` (which game is each player in, so every listener filters by game), protocols you turn on and off, starting countdown, reconnection, spectators with first person view and inventory peek, waiting rooms, generators, team chooser and per-game tab teams.

On top of that:

- **World adapters.** `ZipWorldAdapter` keeps every map as a zip and gives each game a temporary copy; `SlimeWorldAdapter` uses SlimeWorldManager and converts old zips on the fly. `WorldSettings` decides game rules and time.
- **Guided setup.** Annotate a config class with `@ConfigData`, `@ConfigOption` and `@ConfigOptionCategory` and `SetupManager` walks the builder through it with a nether star, yes/no dyes, signs for numbers and chat for text. Optional values can be skipped.
- **Votes.** `VoteSession` with `VoteCategory`/`VoteOption` (chests, health, time, projectiles, final event, scenarios...). Per-option permissions, vote power with `<prefix>.power.<n>`, forced results and a ready `VoteGUI`.
- **Kits.** `KitRegistry` loads `kits/<id>.yml` with as many levels as you want and can save a kit straight from an admin's inventory.
- **Loot.** `LootTables` reads groups (swords, helmets, blocks...) with group chance, rolls, weights and amount ranges. `LootGroup.getRealChance` gives the real per-chest probability for editors.
- **Join signs.** `JoinSignManager` with rotation: signs follow free games of their group and move on when their game starts.
- **Timeline.** `GameTimeline` fires refills, final events and whatever else at the right second and tells scoreboards what's next.

## Data

`DataModule` connects to MySQL, MariaDB or SQLite and falls back to JSON files if the database is down. Player data is stored as one JSON document per player, so adding a stat never needs a migration.

`ProfileManager<P>` loads the profile during async pre-login (no join without data), autosaves, saves on quit and never loses a quick rejoin. Extend `PlayerProfile` for your own fields; it already has a `Wallet`, `Statistics`, `CosmeticData`, kit levels, selected kits, xp and settings.

`Statistics` stores values by scope: `StatScope.of(mode, kit, season)` touches total, mode, kit, season, season + mode and the current year in one go. `max` and `min` keep records (most kills, fastest win).

Also: `LevelCurve` and `PrestigeRegistry` for levels and prestige badges, `LeaderboardService` with a cached top N refreshed in the background, `CosmeticRegistry` with one yml per cosmetic type and permission or purchase based ownership, and `GameHistoryStore` for finished games.

## Network

`NetworkModule` needs Redis. Every server sends a heartbeat every two seconds; a server quiet for 30 seconds is dropped and its players cleaned up.

- `NetworkMessenger`: typed messages to one server or all, plus request/reply with timeouts.
- `ServerRegistry`: live view of every server and its games, players per mode, drain flag.
- `NetworkPlayers`: which server every player is on, as a `PlayerLookup`.
- `TransferService`: stores a context in Redis (game, mode, spectate target...) and sends the player through the proxy; the target server gets it in `onArrive`. `transferParty` moves the whole party, wherever each member is.
- `LoadBalancer`: finds a joinable game with room for the whole party, otherwise the least loaded server that can create one.
- `DrainController`: stop taking games, wait until empty, then restart.
- Cross-server parties: installing the module swaps the party storage, lookup and messages for Redis backed ones, with a two hour TTL by default.
- `spectate(staff, name)` sends staff to wherever a player is, and `broadcast(key, permission, ...)` reaches every player of the network.

The proxy has to be BungeeCord compatible (BungeeCord, Waterfall, Velocity with the bungee channel).

## Building

```
./gradlew build
```

Publishing to Reposilite reads `reposiliteUsername` and `reposilitePassword` from `~/.gradle/gradle.properties`:

```
./gradlew publish
```

## License

GPL-3.0, same as Hyrame, which part of the engine comes from.
