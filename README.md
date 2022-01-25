# GrimAC

**Currently too unstable to use in production.  Work is being done on a partial rewrite to simplify the code, run block place/break/interact logic at the packet level, and to rewrite handling core netcode issues such as tick skipping and 0.03.  No ETA on completion for this partial rewrite, it will be pushed when the partial rewrite is complete. The rewrite also only supports 1.18 currently.**


GrimAC is an open source anticheat designed for 1.17 and supporting 1.7-1.18. It will be sold for $15 on SpigotMC and
other various websites, without obfuscation, DRM, subscriptions, or other nonsense that plague other anticheats.

**Discord:** https://discord.gg/FNRrcGAybJ

## Grim supremacy

Here are the main cores that make grim stand out against other anticheats

### Prediction Engine

* We have a 1:1 replication of the player's possible movements
* This covers everything from basic walking, swimming, knockback, cobwebs, to bubble columns
* It even covers riding entities from boats to pigs to striders
* Built upon covering edge cases to confirm accuracy
* 1.13+ clients on 1.13+ servers, 1.12- clients on 1.13+ servers, 1.13+ clients on 1.12- servers, and 1.12- clients on
  1.12- servers are all supported regardless of the large technical changes between these versions.
* Order of collisions depends on client version and is correct
* Accounts for minor bounding box differences between versions, for example:
    * Single glass panes will be a + shape for 1.7-1.8 players and * for 1.9+ players
    * 1.13+ clients on 1.8 servers see the + glass pane hitbox due to ViaVersion
    * Many other blocks have this extreme attention to detail.
    * Waterlogged blocks do not exist for 1.12 or below players
    * Blocks that do not exist in the client's version use ViaVersion's replacement block
    * Block data that cannot be translated to previous versions is replaced correctly
    * All vanilla collision boxes have been implemented

### Fully asynchronous and multithreaded design

* All movement checks are run off the main thread and netty thread
* The anticheat can scale to many hundreds of players, if not more
* Thread safety is carefully thought out
* The next core allows for this design

### Full world replication

* The anticheat keeps a replica of the world for each player
* The replica is created by listening to chunk data packets and block changes
* On all versions, chunks are compressed to 16-64 kb per chunk using palettes
* Using this cache, the anticheat can safety access the world state
* Per player cache allows for multithreaded design
* Sending players fake blocks with packets is safe and does not lead to falses
* The world is recreated for each player to allow lag compensation

### Latency compensation

* World changes are queue'd until they reach the player
* This means breaking blocks under a player does not false the anticheat
* Everything from flying status to movement speed will be latency compensated

### Secure by design, not obscurity

* All systems are designed to be highly secure and mathematically impossible to bypass
* For example, the prediction engine knows all possible movements and cannot be bypassed
