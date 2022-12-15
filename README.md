# GrimAC

**Test server with bufferless alerts: test.grim.ac**

GrimAC is an open-source Minecraft anticheat designed for 1.19 and supports versions 1.8-1.19. It is free to use while in beta, but will eventually become a paid service or offer subscription-based paid checks. Geyser players are fully exempt from all checks.

**Discord:** https://discord.gg/FNRrcGAybJ

### Compiling through terminal/command prompt
1. git clone https://github.com/MWHunter/Grim (or click the green "Code" button and download the code, then unzip it)
2. cd Grim
3. gradlew shadowJar

The final JAR file will be located in the 'build/libs' directory.

### API Information

Grim's API allows you to integrate Grim into your own plugins. For more information, check out the API's GitHub repository here[here](https://github.com/MWHunter/GrimAPI).

## Grim supremacy

Grim stands out among other anticheats for several reasons:

### Movement Simulation Engine

Grim has a 1:1 replication of a player's possible movements, **covering everything from basic walking and swimming to knockback, cobwebs, and bubble columns**. The engine is built to account for **edge cases and confirm accuracy**, and supports all combinations of client and server **versions from 1.8-1.19**. The engine also accounts for minor bounding box differences between versions, **such as the shape of glass panes, and replaces blocks and block data that cannot be translated to previous versions correctly**. All vanilla collision boxes have been implemented.

### Fully asynchronous and multithreaded design

Grim's movement checks and most listeners **run on the Netty thread**, allowing the anticheat to scale to hundreds of players. The design is carefully thought out to ensure thread safety.

### Full world replication

Grim keeps a replica of the world for each player, created by listening to **chunk data packets, block places, and block changes**. The replica is compressed to 16-64 kb per chunk using palettes, and allows the anticheat to safely access the world state. This replica also allows for multithreaded design and **prevents false positives from packet-based blocks and block glitching**.

### Latency compensation

Grim queues world changes until they reach the player, **preventing false positives from latency**. This also compensates for changes in flying status and movement speed.

### Inventory compensation

Grim tracks a player's inventory **to prevent ghost blocks and other errors at high latency.**

### Secure by design, not obscurity

All systems are designed to be highly secure and mathematically impossible to bypass
For example, the prediction engine knows all possible movements and cannot be bypassed

### Support

If you need help or support with Grim, you can join the Grim Discord community and ask for assistance in the #on-topic channel. To join the Discord server, use this link: https://discord.gg/FNRrcGAybJ

In the #on-topic channel, you can ask questions and get help from other members of the community. Please make sure to provide as much detail as possible, including your Minecraft version, server version, and any relevant error messages or logs.

If you are reporting a bug or issue, please report it on the GitHub repository.

Please note that the support provided in the Discord community is community-based. If you need official support or want to request a specific feature, you can create an issue on the [GrimAC GitHub repository](https://github.com/GrimAnticheat/Grim/issues).
