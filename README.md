Note: There currently is no stable branch, the majority of stuff is currently disabled and broken in the master branch
while doing major refactors.

A free and open source anticheat for 1.16. Everything that can be promised is promised, and is about 40% complete.
Should release late summer or early fall 2021. We are a prediction based anticheat meaning that we use client code
directly in order to see whether a movement is possible. This gives us a huge advantage over traditional anticheats. In
addition, the open source nature should allow it to server as a base to other anticheats, who are free to integrate our
prediction system as long as they follow the GPL license. Yes, I am serious about the license, don't break it. You
should be following GPL anyways as a bukkit plugin.

**What has been done so far?**

- A semi-decent prediction system for 1.16 movement that is accurate for most movement such as swimming, running, water pushing, jumping, shifting, fireworks, lava movement, water movement, knockback, cobwebs, bubble columns, and using an elytra. Most of the work so far has been spent on the prediction engine, as it is the main feature of this anticheat.
- The framework for allowing async and multithreaded checks
- Half-working boat support
- A half finished way to get block bounding boxes independently of server version
- A fast collision system that is based on 1.12 and works for all client versions.

**What is not done?**

- The combat checks to this anticheat
- Handling lower precision on 1.9+ clients
- Porting the chunk caching system back to 1.7-1.12
- Grabbing movement packets before ViaVersion, as ViaVersion messes up the packets
- The logic for utilizing the prediction engine in the anticheat
- Punishment system
- Handling ping and other latency

**What will be left out in release**

- Actual geyser support. Not sure how geyser support will be done, but for release we will just exempt Geyser players as normal.
- Artificial intelligence combat checks. Seems fun and I should be able to get enough data if I include some optional telemetry in this anticheat.


**License (We are GPL, this is a summary not legal advice. If you use my code you must use this or a compatible
license):**

- All people who have access to the program should be able to request and access source code, no exceptions or tiers
  without source code.
- All people who have access to the program are able to redistribute this program freely, and are allowed to remove
  limitations such as DRM.
- All people who have access to the program can redistribute modified versions of the program.
- All software that uses GPL code, such as in dependies such as Bukkit or from this project itself, is also GPL and must
  follow this license.
