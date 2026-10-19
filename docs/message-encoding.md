# Message encoding

Grim's Fabric and Bukkit fast senders use one component encoder and small system-chat/disconnect packet writers. PacketEvents supplies packet IDs, NBT containers, channel access and transport. Grim does not call its patched Gson or legacy serializers for these messages.

Outgoing packets use the server protocol and enter the ordinary outbound pipeline. ViaVersion/ViaBackwards translates them for other client versions. Encoding client-format bytes and injecting beyond Via is a different transport and is not implemented here.

- Before 1.20.3, upstream Kyori Gson uses the server's world data version, including legacy hover and RGB settings.
- On 1.20.3+, components are encoded directly to network NBT. No JSON or native-component intermediate is created in the fast path.
- Modern Fabric's normal sender decodes that same component wire format into Minecraft's native component. This preserves typed item data and custom click payloads that the Gson path cannot represent losslessly. Older Fabric still uses upstream Gson.
- Bukkit's normal sender retains the native Adventure/BukkitAudiences path.
- Alerts, verbose/debug messages, command replies, console output, legacy-string messages and Grim disconnect reasons are covered. Platform-native kicks retain their existing platform implementation.

The optional `plain` and object `fallback` getters added in Adventure 5 use cached exact method handles. All ordinary component fields are read directly. No reflective Bukkit codec adapter or `Buildable` shim is included.

The official-mapped Fabric variant bundles a coherent Adventure 5.2.0 dependency set. The intermediary variant retains the existing Adventure 4 dependency versions. The PE dependency includes the separate fix for the class-to-interface change in `BinaryTagType`. Its NBT bridge resolves methods by signature and the registry by field type and contents. Tests exercise relocated and renamed classes, fields and methods on Adventure 4.26.1 and 5.2.0.

## Verification

Run the compiled common code against both runtime API versions:

```sh
./gradlew :common:test --tests '*MessageNbtEncoderTest' -PadventureTestVersion=4.26.1
./gradlew :common:test --tests '*MessageNbtEncoderTest' -PadventureTestVersion=5.2.0
./gradlew :fabric:jar :bukkit:shadowJar :fabric:official:mc261:messageTestJar
```

The optional message-test mod is a separate artifact and is never nested into the release jar. Run it with Grim and Fabric API on an isolated offline server. It compares 25 component fixtures against Minecraft's codec, checks the disconnect packet, and verifies buffer release after an encoding error. When a player joins, it sends each fixture through the player path and command-sender path, plus one legacy-string message.

```sh
python3 tools/message-tests/client.py --port PORT --log SERVER_LOG --expected 51
```

For a Via test, install ViaFabric and ViaBackwards, and launch the test server with `-Dgrim.messageTestVia=true`. The test mod prints packet IDs for a 1.21.11 client and the same socket client negotiates that protocol.

Verified locally with Java 25 and Fabric Loader 0.19.5:

| Server | Test | Result |
| --- | --- | --- |
| Fabric 26.2, Fabric API 0.152.1+26.2 | Native codec and socket delivery | 25 fixtures, disconnect, 51 messages |
| Fabric 26.3, Fabric API 0.163.0+26.3 | Native codec and socket delivery | 25 fixtures, disconnect, 51 messages |
| Fabric 26.3, fast bypass disabled | Native player/command paths and legacy formatting | 51 messages |
| Fabric 26.3 + ViaFabric 0.4.22+186 + ViaBackwards 5.12.1 | 1.21.11 socket client | 51 messages; failed-encoding buffer released |
| Fabric 1.21.11, Adventure 4.26.1 | Intermediary variant, real connection | Eight alert fixtures |
| Paper 26.3 build 205 | `/grim sendalert` over a real connection | Text, RGB, text/item/entity hovers, click, translation and Unicode |

Socket tests use compression threshold 256 and include a compressed message. The Fabric test instances disable the optional database because the baseline Fabric package lacks the SQLite JDBC driver; this is independent of message encoding.
