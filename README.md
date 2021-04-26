Note: There currently is no stable branch, the majority of stuff is currently disabled and broken in the master branch
while doing major refactors.

A free and open source anticheat for 1.16. Everything that can be promised is promised, except legacy support, and is
about 30% complete. Should release late summer or early fall 2021. We are a prediction based anticheat meaning that we
use client code directly in order to see whether a movement is possible. This gives us a huge advantage over traditional
anticheats. In addition, the open source nature should allow it to server as a base to other anticheats, who are free to
integrate our prediction system as long as they follow the GPL license. Yes, I am serious about the license, don't break
it.

**License (We are GPL, this is a summary not legal advice. If you use my code you must use this or a compatible
license):**

- All people who have access to the program should be able to request and access source code, no exceptions or tiers
  without source code.
- All people who have access to the program are able to redistribute this program freely, and are allowed to remove
  limitations such as DRM.
- All people who have access to the program can redistribute modified versions of the program.
- All software that uses GPL code, such as in dependies such as Bukkit or from this project itself, is also GPL and must
  follow this license.

**What has been done so far?**

- A semi-decent prediction system for 1.16 movement that is accurate for most movement such as swimming, running,
  jumping, shifting, and using an elytra
- An outline for how to keep track of blocks so that movement processing and other checks can be done async

**What is not done?**

- The combat checks to this anticheat
- The logic for utilizing the prediction engine in the anticheat
- Handling ping and other latency
- Some stuff such as handling block bounding boxes async
- A lot of weird edge cases especially with ladders
- Testing, which will be done on Abyss Earth silently and before release. I'm sure I'll catch some cheaters.

**What will be left out in release**

- Legacy support. I'll add your version of choice for (current date - years released ago) * $1,000 otherwise don't
  complain. This is free and open source software. I don't like half decade old versions of the game.
- 1.8-1.15 client support. Please use modern versions of the game.
- Geyser support. Not sure how geyser support will be done.
- Artificial intelligence combat checks. Seems fun and I should be able to get enough data.
