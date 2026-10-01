# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository overview

This is a Minecraft network ("BannanCraft" / Nova) consisting of several independent Maven plugin
projects, a Velocity proxy, a running Paper server instance, and a small Flask backend. There is no
top-level build tool tying these together — each directory is built and deployed independently.

```
a/                    Velocity proxy runtime (config: a/velocity.toml, secret: a/forwarding.secret)
server/               A live Paper server instance ("MainLobby") — world data, plugins/, server.properties
x/                    Flask microservice used for custom login/name validation (main.py, db.py, config.py)
NovaCrates/           Paper plugin — crate/key system (org.nova groupId)
NovaDungeon/          Paper plugin — dungeon system, depends on WorldGuard
NovaEconomy/          Paper plugin — Vault economy provider + auction house
NovaSidebar/          Paper plugin — scoreboard sidebar, PlaceholderAPI/LuckPerms/SuperiorSkyblock2 softdepends
RankManager/          Paper plugin — Shopier payment webhook -> LuckPerms rank grants
SkyMinions/           Paper plugin — SkyBlock-style minion/farming system
bannanDefaultLobby/   Paper plugin — lobby plugin: custom login screen, calls the x/ Flask service
```

Each Nova* / bannanDefaultLobby directory is a standalone Maven module (own `pom.xml`, `.idea/`, `target/`).
They are **not** linked by a parent POM or shared dependency — common logic (e.g. Vault economy hookup,
GUI patterns, PersistentDataContainer item IDs) is duplicated per-plugin rather than factored into a
shared library. Keep that in mind: a fix in one plugin does not propagate to the others.

## Commands

Build a single plugin (run from inside its directory, or with `-f`):
```
cd NovaEconomy && mvn clean package
```
This produces a shaded jar in `target/` (e.g. `NovaEconomy-1.0-SNAPSHOT.jar`) via the `maven-shade-plugin`,
suitable for dropping into `server/plugins/`. There are no test suites in any module (`mvn test` will pass
trivially — no tests exist).

Run the Flask backend (`x/`):
```
cd x && source .env/bin/activate  # or .env/Scripts/activate on Windows
python main.py
```
It listens on `0.0.0.0:5000` and exposes `/NameCheck` and `/Login`, both POST JSON endpoints guarded by a
shared-secret string compared against `config.py` (`PlayerValidationCheck`, `PlayerLoginCheck`). `db.py` is
currently an in-memory placeholder (`# Make a Proper DB BRO`), not a real database.

There is no automated way to run the Paper server or Velocity proxy from Claude Code — they are long-running
Java processes normally started with the platform's `java -jar server.jar` / `java -jar velocity.jar`.

## Architecture notes

- **All plugins target Paper/Spigot 1.20.4 (`spigot-api`/`paper-api` 1.20.4-R0.1-SNAPSHOT) on Java 17.**
  Check `api-version` in each `plugin.yml` before using newer Bukkit API.
- **Network topology**: Velocity proxy (`a/`, port 25565, `online-mode = false`, modern forwarding via
  `forwarding.secret`) routes to a single backend server `lobby` at `127.0.0.1:25555`, which is the Paper
  instance in `server/`. Player auth is *not* Mojang online-mode — `bannanDefaultLobby`'s `LoginManager`/
  `Network.java` calls the Flask service in `x/` over HTTP to validate names/passwords instead.
- **Plugin structure convention** (seen across NovaEconomy, SkyMinions, NovaCrates, NovaDungeon): the
  `JavaPlugin` subclass (e.g. `NovaEconomy.java`) holds static shared state (economy provider, in-memory
  collections, NamespacedKeys) and wires up commands/listeners in `onEnable()`. Sub-packages follow
  `commands/`, `listeners/` or `listener/`, `gui/`, `manager/` or the domain noun (`crate/`, `minyon/`),
  `utils/`/`util/`, `models/`. Follow the existing package layout of a plugin rather than introducing a new
  convention within it.
- **Custom item identity**: items with persistent identity (auction listings, crate keys, minion items) are
  tagged via `PersistentDataContainer` + a `NamespacedKey` on the plugin (e.g. `NovaEconomy.itemKey`), not by
  display name/lore/material. Follow this pattern for any new stateful item.
- **Persistence**: plugins without a real database persist state as JSON via Gson into the plugin's data
  folder (e.g. `NovaEconomy` saves `auctions.json` on `onDisable()`/load on `onEnable()`). There is no shared
  database layer across plugins — each does its own thing (NovaEconomy: JSON; RankManager: presumably
  LuckPerms API directly; SkyMinions: check `MinyonManager`).
- **Vault** is the shared economy abstraction (`net.milkbowl.vault.economy.Economy`) used by NovaEconomy,
  NovaCrates, NovaSidebar, SkyMinions — plugins that need money must declare `depend: [Vault]` in
  `plugin.yml` and fail `onEnable()` gracefully (disable self) if the Vault economy provider isn't found.
- **Turkish is used for in-game-facing strings** (plugin descriptions, chat messages, log messages) in
  several plugins (NovaCrates, NovaDungeon, NovaEconomy, SkyMinions) — match the existing language when
  editing user-facing text in those plugins rather than switching to English.
- `server/` contains live world/save data (region files, playerdata, level.dat) — treat it as runtime state,
  not source to be refactored, and be careful not to overwrite/delete it.

## Project-specific engineering guidelines

The `mc` skill (`.claude/skills/mc/SKILL.md`) is loaded automatically for this project and contains detailed,
opinionated rules for Minecraft server plugin development here: economy/transaction safety, GUI exploit
prevention, quest/reward systems, custom items/enchantments, minions, LuckPerms integration, configuration
conventions, logging/error-handling expectations, and a step-by-step feature/debugging workflow. Follow it —
it is more specific than general best practices and reflects this project's production-readiness bar
(duplicate-claim prevention, race conditions, server-side validation of all client input, etc.).
