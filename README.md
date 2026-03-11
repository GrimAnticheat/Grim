<div align="center">
 <h1>GrimAC</h1>

 <div>
  <a href="https://github.com/GrimAnticheat/Grim/actions/workflows/gradle-publish.yml">
   <img alt="Workflow" src="https://img.shields.io/github/actions/workflow/status/GrimAnticheat/Grim/gradle-publish.yml?style=flat&logo=github"/>
  </a>&nbsp;&nbsp;
  <a href="https://modrinth.com/plugin/grimac">
   <img alt="Modrinth" src="https://img.shields.io/modrinth/v/LJNGWSvH?style=flat&label=version&logo=modrinth">
  </a>&nbsp;&nbsp;
  <a href="https://modrinth.com/plugin/grimac#download">
   <img alt="Downloads" src="https://img.shields.io/modrinth/dt/LJNGWSvH?style=flat&logo=modrinth&label=downloads&link=https%3A%2F%2Fmodrinth.com%2Fplugin%2Fgrimac%23download">
  </a>&nbsp;&nbsp;
  <a href="https://discord.grim.ac">
   <img alt="Discord" src="https://img.shields.io/discord/811396969670901800?style=flat&label=discord&logo=discord">
  </a>
 </div>
 <br>
</div>

GrimAC is an open source Minecraft anticheat designed to support the latest versions of Minecraft.
It currently supports minecraft versions 1.8–1.21. Geyser players are fully exempt from the anticheat to prevent false positives.
This project is considered feature-complete for the 2.0 (open-source) branch. If you would like a bug fix or enhancement and cannot sponsor the work, pull requests are welcome.
A premium version is planned, which will offer additional subscription-based paid checks, such as heuristics.

## Downloads

- Latest updates:
  - **[Modrinth](https://modrinth.com/plugin/grimac)** *(recommended)*
  - GitHub
  artifacts: [Bukkit](https://nightly.link/GrimAnticheat/Grim/workflows/gradle-publish/2.0/grimac-bukkit.zip), [Fabric](https://nightly.link/GrimAnticheat/Grim/workflows/gradle-publish/2.0/grimac-fabric.zip) *(bleeding edge)*
- Major releases only:
  - ~~[Hangar](https://hangar.papermc.io/GrimAnticheat/GrimAnticheat)~~
  - ~~[SpigotMC](https://www.spigotmc.org/resources/grim-anticheat.99923/)~~

## Requirements & Installation

- Java 17 or higher. *For more details, see [Updating-to-Java-17](https://github.com/GrimAnticheat/Grim/wiki/Updating-to-Java-17).*
- A Spigot, Paper, Folia, or Fabric server environment. *For more details, see [Supported-environments](https://github.com/GrimAnticheat/Grim/wiki/Supported-environments).*

If you use a proxy such as Velocity or Bungeecord:
- If you use Geyser, Floodgate must be installed on the backend server (where Grim is) so Grim can access the Floodgate API.
- If you use ViaVersion, it must be installed on the backend server (where Grim is) ONLY.
  Grim does not support having ViaVersion installed on the proxy, even if it is also installed on the backend.

## Resources

- For documentation and examples visit the [Wiki](https://github.com/GrimAnticheat/Grim/wiki).
- For answers to commonly asked questions visit the [FAQ](https://github.com/GrimAnticheat/Grim/wiki/FAQ).
- For community support and project discussion join our [Discord](https://discord.grim.ac).

## Pull Requests

See [Contributing](CONTRIBUTING.md) for more information about contributing and what our guidelines
are.

## Developer Plugin API

Grim's plugin API allows you to integrate Grim into your own plugins. Visit
the [plugin API repository](https://github.com/GrimAnticheat/GrimAPI) for the source code and more
information.

## Compiling From Source

1. `git clone https://github.com/GrimAnticheat/Grim.git`
2. `cd Grim`
3. `./gradlew build`
4. The final jars will compile into the `<platform>/build/libs` folders

## Grim Supremacy

What makes Grim stand out against other anticheats?

### Movement Simulation Engine

* We have a 1:1 replication of the player's possible movements
    * This covers everything from basic walking, swimming, knockback, cobwebs, to bubble columns
    * It even covers riding entities from boats to pigs to striders
* Built upon covering edge cases to confirm accuracy
* 1.13+ clients on 1.13+ servers, 1.12- clients on 1.13+ servers, 1.13+ clients on 1.12- servers,
  and 1.12- clients on 1.12- servers are all supported regardless of the large technical changes
  between these versions.
* The order of collisions depends on the client version and is correct
* Accounts for minor bounding box differences between versions, for example:
    * Single glass panes will be a + shape for 1.7-1.8 players and * for 1.9+ players
    * 1.13+ clients on 1.8 servers see the + glass pane hitbox due to ViaVersion
    * Many other blocks have this extreme attention to detail.
    * Waterlogged blocks do not exist for 1.12 or below players
    * Blocks that do not exist in the client's version use ViaVersion's replacement block
    * Block data that cannot be translated to previous versions is replaced correctly
    * All vanilla collision boxes have been implemented

### Fully asynchronous and multithreaded design

* All movement checks and the overwhelming majority of listeners run on the netty thread
* The anticheat can scale to many hundreds of players, if not more
* Thread safety is carefully thought out
* The next core allows for this design

### Full world replication

* The anticheat keeps a replica of the world for each player
* The replica is created by listening to chunk data packets, block places, and block changes
* On all versions, chunks are compressed to 16-64 kb per chunk using palettes
* Using this cache, the anticheat can safely access the world state
* Per player, the cache allows for multithreaded design
* Sending players fake blocks with packets is safe and does not lead to falses
* The world is recreated for each player to allow lag compensation
* Client sided blocks cause no issues with packet based blocks. Block glitching does not false the
  anticheat.

### Latency compensation

* World changes are queued until they reach the player
* This means breaking blocks under a player does not false the anticheat
* Everything from flying status to movement speed will be latency compensated

### Inventory compensation

* The player's inventory is tracked to prevent ghost blocks at high latency, and other errors

### Secure by design, not obscurity

* All systems are designed to be highly secure and mathematically impossible to bypass
* For example, the prediction engine knows all possible movements and cannot be bypassed
