package ac.grim.grimac.test;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.fabric.mc261.Fabric261ConversionUtil;
import ac.grim.grimac.utils.message.DisconnectMessagePacket;
import ac.grim.grimac.utils.message.KyoriMessageSerializer;
import ac.grim.grimac.utils.message.MessagePackets;
import ac.grim.grimac.utils.message.SystemMessagePacket;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.*;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;

import java.util.*;

public final class MessageEncodingTestMod implements ModInitializer {
    private static final Map<String, Component> MESSAGES = fixtures();
    private final Set<UUID> sent = new HashSet<>();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::verify);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                if (!sent.add(player.getUUID())) continue;
                var platformPlayer = GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromUUID(player.getUUID());
                var broken = new BrokenPacket();
                try {
                    MessagePackets.send(PacketEvents.getAPI().getPlayerManager().getUser(player), broken);
                    throw new AssertionError("Invalid packet was sent");
                } catch (IllegalArgumentException expected) {
                    if (broken.captured == null || broken.captured.refCnt() != 0 || broken.buffer != null) {
                        throw new AssertionError("Invalid packet leaked its buffer");
                    }
                }
                System.out.println("GRIM_MESSAGE_TEST PASS buffer-ownership");
                int count = 0;
                for (var entry : MESSAGES.entrySet()) {
                    // Prefix survives every encoding so the socket client can count received fixtures.
                    Component message = Component.text("GRIM_MESSAGE_TEST:" + entry.getKey() + ":").append(entry.getValue());
                    platformPlayer.sendMessage(message);
                    platformPlayer.getSender().sendMessage(message);
                    count += 2;
                }
                platformPlayer.sendMessage("GRIM_MESSAGE_TEST:legacy:§aGreen §lBold");
                System.out.println("GRIM_MESSAGE_TEST SENT=" + (count + 1));
            }
        });
    }

    private static final class BrokenPacket extends com.github.retrooper.packetevents.wrapper.PacketWrapper<BrokenPacket> {
        private io.netty.buffer.ByteBuf captured;

        private BrokenPacket() {
            super(PacketType.Play.Server.SYSTEM_CHAT_MESSAGE);
        }

        @Override
        public void write() {
            captured = (io.netty.buffer.ByteBuf) buffer;
            throw new IllegalArgumentException("Intentional invalid test payload");
        }
    }

    private void verify(MinecraftServer server) {
        int failures = 0;
        var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var conversion = new Fabric261ConversionUtil();
        if (conversion.dataVersion() != net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version()) {
            throw new AssertionError("Wrong Minecraft data version");
        }
        var version = PacketEvents.getAPI().getServerManager().getVersion();
        var clientVersion = Boolean.getBoolean("grim.messageTestVia")
                ? com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_21_11 : version.toClientVersion();
        var ids = new com.google.gson.JsonObject();
        ids.addProperty("protocol", clientVersion.getProtocolVersion());
        ids.addProperty("config_end", PacketType.Configuration.Server.CONFIGURATION_END.getId(clientVersion));
        ids.addProperty("config_end_ack", PacketType.Configuration.Client.CONFIGURATION_END_ACK.getId(clientVersion));
        ids.addProperty("known_packs", PacketType.Configuration.Server.SELECT_KNOWN_PACKS.getId(clientVersion));
        ids.addProperty("known_packs_reply", PacketType.Configuration.Client.SELECT_KNOWN_PACKS.getId(clientVersion));
        ids.addProperty("system_chat", PacketType.Play.Server.SYSTEM_CHAT_MESSAGE.getId(clientVersion));
        ids.addProperty("keepalive", PacketType.Play.Server.KEEP_ALIVE.getId(clientVersion));
        ids.addProperty("keepalive_reply", PacketType.Play.Client.KEEP_ALIVE.getId(clientVersion));
        ids.addProperty("loaded", PacketType.Play.Client.PLAYER_LOADED.getId(clientVersion));
        System.out.println("GRIM_MESSAGE_TEST IDS=" + ids);
        for (var entry : MESSAGES.entrySet()) {
            var buffer = Unpooled.buffer();
            try {
                SystemMessagePacket packet = new SystemMessagePacket(version, conversion.dataVersion(), entry.getValue());
                packet.buffer = buffer;
                packet.write();
                var decoded = ClientboundSystemChatPacket.STREAM_CODEC.decode(new RegistryFriendlyByteBuf(buffer, server.registryAccess()));
                if (decoded.overlay() || buffer.isReadable()) throw new AssertionError("Invalid packet trailer");
                com.google.gson.JsonElement source;
                if (entry.getKey().equals("hover-item-typed")) {
                    source = JsonParser.parseString("{\"text\":\"typed\",\"hover_event\":{\"action\":\"show_item\",\"id\":\"minecraft:stone\",\"count\":1,\"components\":{\"minecraft:custom_data\":{\"byte\":1,\"float\":1.5,\"long\":9223372036854775807,\"array\":[1,2]}}}}");
                } else if (entry.getKey().equals("click-custom")) {
                    source = JsonParser.parseString("{\"text\":\"custom\",\"click_event\":{\"action\":\"custom\",\"id\":\"grim:test\",\"payload\":{\"n\":1,\"s\":\"value\"}}}");
                } else source = KyoriMessageSerializer.forDataVersion(conversion.dataVersion()).serializeToTree(entry.getValue());
                if (entry.getKey().equals("translation-values")) {
                    // Minecraft documents that boolean translation arguments need string form in NBT.
                    source.getAsJsonObject().getAsJsonArray("with").set(1, new com.google.gson.JsonPrimitive("true"));
                }
                var expected = ComponentSerialization.CODEC.parse(ops, source).getOrThrow();
                var expectedJson = ComponentSerialization.CODEC.encodeStart(ops, expected).getOrThrow();
                var actualJson = ComponentSerialization.CODEC.encodeStart(ops, decoded.content()).getOrThrow();
                if (!expectedJson.equals(actualJson)) throw new AssertionError("Expected " + expectedJson + " but got " + actualJson);
                var nativeConverted = (net.minecraft.network.chat.Component) conversion.toNativeText(entry.getValue());
                if (!expectedJson.equals(ComponentSerialization.CODEC.encodeStart(ops, nativeConverted).getOrThrow())) {
                    throw new AssertionError("Native conversion differs for " + entry.getKey());
                }
                System.out.println("GRIM_MESSAGE_TEST PASS " + entry.getKey());
            } catch (Throwable failure) {
                failures++;
                System.out.println("GRIM_MESSAGE_TEST FAIL " + entry.getKey());
                failure.printStackTrace();
            } finally {
                buffer.release();
            }
        }
        GrimAPI.INSTANCE.getPlatformServer().getConsoleSender().sendMessage(Component.text("GRIM_MESSAGE_TEST console"));
        var disconnectBuffer = Unpooled.buffer();
        try {
            Component reason = Component.text("disconnect").color(NamedTextColor.RED).hoverEvent(Component.text("reason"));
            var packet = new DisconnectMessagePacket(conversion.dataVersion(), reason);
            packet.buffer = disconnectBuffer;
            packet.write();
            var decoded = net.minecraft.network.protocol.common.ClientboundDisconnectPacket.STREAM_CODEC.decode(disconnectBuffer);
            if (disconnectBuffer.isReadable() || !decoded.reason().getString().equals("disconnect")) throw new AssertionError("Invalid disconnect");
            System.out.println("GRIM_MESSAGE_TEST PASS disconnect");
        } catch (Throwable failure) {
            failures++;
            failure.printStackTrace();
        } finally {
            disconnectBuffer.release();
        }
        System.out.println("GRIM_MESSAGE_TEST VERIFIED=" + MESSAGES.size() + " FAILURES=" + failures);
    }

    private static Map<String, Component> fixtures() {
        Map<String, Component> messages = new LinkedHashMap<>();
        messages.put("empty", Component.empty());
        messages.put("unicode", Component.text("Hello 🌍 漢字\nSecond line"));
        messages.put("compressed", Component.text("Repeated payload ".repeat(100)));
        messages.put("style", Component.text("styled").color(TextColor.color(0x12abef)).decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false).font(Key.key("minecraft:uniform"))
                .shadowColor(ShadowColor.shadowColor(0x7f123456)).insertion("insert")
                .append(Component.text(" child").color(NamedTextColor.RED)));
        messages.put("translation", Component.translatable("chat.type.text", Component.text("name"), Component.text("message")));
        messages.put("translation-values", Component.translatable().key("test.translation").fallback("%s %s")
                .arguments(TranslationArgument.numeric(42), TranslationArgument.bool(true)).build());
        messages.put("keybind", Component.keybind("key.jump"));
        messages.put("score", Component.score("TestPlayer", "objective"));
        messages.put("selector", Component.selector("@a").separator(Component.text(", ")));
        messages.put("block-nbt", Component.blockNBT().nbtPath("CustomName").pos(BlockNBTComponent.Pos.fromString("~0 ~0 ~0")).interpret(true).build());
        messages.put("entity-nbt", Component.entityNBT().nbtPath("CustomName").selector("@s").plain(true).build());
        messages.put("storage-nbt", Component.storageNBT().nbtPath("message").storage(Key.key("grim:test")).separator(Component.text(" | ")).build());
        messages.put("hover-text", Component.text("hover").hoverEvent(Component.text("nested").color(NamedTextColor.GREEN)));
        messages.put("hover-entity", Component.text("entity").hoverEvent(HoverEvent.showEntity(Key.key("minecraft:pig"), new UUID(1, 2), Component.text("Pig"))));
        messages.put("hover-item", Component.text("item").hoverEvent(HoverEvent.showItem(Key.key("minecraft:stone"), 2)));
        messages.put("hover-item-json", Component.text("item-json").hoverEvent(HoverEvent.showItem(Key.key("minecraft:stone"), 1,
                Map.of(Key.key("minecraft:custom_data"), GsonDataComponentValue.gsonDataComponentValue(JsonParser.parseString("{\"decimal\":1.5,\"large\":9223372036854775807}"))))));
        DataComponentValue.TagSerializable typed = () -> BinaryTagHolder.binaryTagHolder("{byte:1b,float:1.5f,long:9223372036854775807L,array:[I;1,2]}");
        messages.put("hover-item-typed", Component.text("typed").hoverEvent(HoverEvent.showItem(Key.key("minecraft:stone"), 1,
                Map.of(Key.key("minecraft:custom_data"), typed))));
        messages.put("click-command", Component.text("run").clickEvent(ClickEvent.runCommand("/grim alerts")));
        messages.put("click-suggest", Component.text("suggest").clickEvent(ClickEvent.suggestCommand("/grim profile ")));
        messages.put("click-url", Component.text("url").clickEvent(ClickEvent.openUrl("https://grim.ac")));
        messages.put("click-copy", Component.text("copy").clickEvent(ClickEvent.copyToClipboard("copied")));
        messages.put("click-page", Component.text("page").clickEvent(ClickEvent.changePage(2)));
        messages.put("click-custom", Component.text("custom").clickEvent(ClickEvent.custom(Key.key("grim:test"), BinaryTagHolder.binaryTagHolder("{n:1b,s:\"value\"}"))));
        messages.put("sprite", Component.object(ObjectContents.sprite(Key.key("minecraft:gui"), Key.key("minecraft:icon/checkmark"))).fallback(Component.text("check")));
        messages.put("head", Component.object(ObjectContents.playerHead().name("TestPlayer").id(new UUID(1, 2)).hat(false).build()));
        return messages;
    }
}
