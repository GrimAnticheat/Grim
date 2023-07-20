# GrimAC

This project is considered feature complete for the 2.0 (open-source) branch of this project. If you would like a bugfix or enhancement and cannot sponsor the work, pull requests are welcome. Feel free to contact DefineOutside for consulting estimates if desired. You can join the [discord](https://discord.gg/SEywtQMt29) for jar releases & changelogs.

**Test server with bufferless alerts: test.grim.ac**

GrimAC is an open source Minecraft anticheat designed for 1.20 and supports 1.8-1.20. It is free while in beta. It will eventually become paid and/or will include offer additionally subscription based paid checks. Geyser players are fully exempt.

### Compiling through terminal/command prompt
1. git clone https://github.com/MWHunter/Grim (or click green code button, download code, then unzip)
2. cd Grim
3. gradlew build
4. The final jar is located in build/libs

### API Information
Grim's API allows you to integrate Grim into your own plugins. For more information, check out the API's github repository [here](https://github.com/MWHunter/GrimAPI).

## Grim supremacy

Here are the main cores that make Grim stand out against other anticheats

### Movement Simulation Engine

* We have a 1:1 replication of the player's possible movements
* This covers everything from basic walking, swimming, knockback, cobwebs, to bubble columns
* It even covers riding entities from boats to pigs to striders
* Built upon covering edge cases to confirm accuracy
* 1.13+ clients on 1.13+ servers, 1.12- clients on 1.13+ servers, 1.13+ clients on 1.12- servers, and 1.12- clients on 1.12- servers are all supported regardless of the large technical changes between these versions.
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
* Using this cache, the anticheat can safety access the world state
* Per player, the cache allows for multithreaded design
* Sending players fake blocks with packets is safe and does not lead to falses
* The world is recreated for each player to allow lag compensation
* Client sided blocks cause no issues with packet based blocks. Block glitching does not false the anticheat.

### Latency compensation

* World changes are queued until they reach the player
* This means breaking blocks under a player does not false the anticheat
* Everything from flying status to movement speed will be latency compensated

### Inventory compensation

* The player's inventory is tracked to prevent ghost blocks at high latency, and other errors

### Secure by design, not obscurity

* All systems are designed to be highly secure and mathematically impossible to bypass
* For example, the prediction engine knows all possible movements and cannot be bypassed
