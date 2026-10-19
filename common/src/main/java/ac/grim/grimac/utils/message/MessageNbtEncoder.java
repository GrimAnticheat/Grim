/*
 * Component encoding adapted from PacketEvents' AdventureNBTSerializer.
 * Copyright (C) 2024 retrooper and contributors.
 * Licensed under the GNU General Public License, version 3 or later.
 */
package ac.grim.grimac.utils.message;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.nbt.codec.NBTCodec;
import com.github.retrooper.packetevents.protocol.util.NbtCodecs;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Write-only Adventure encoder. It never loads PacketEvents' patched Adventure serializers. */
final class MessageNbtEncoder {
    private final PacketWrapper<?> wrapper;
    private final ServerVersion version;
    private final boolean downsampleColors;

    MessageNbtEncoder(PacketWrapper<?> wrapper, boolean downsampleColors) {
        this.wrapper = wrapper;
        this.version = wrapper.getServerVersion();
        this.downsampleColors = downsampleColors;
    }

    NBT encode(Component component) {
        if (component instanceof TextComponent text && !component.hasStyling() && component.children().isEmpty()) {
            return new NBTString(text.content());
        }
        return compound(component);
    }

    private NBTCompound compound(Component component) {
        NBTCompound out = new NBTCompound();
        if (component instanceof TextComponent text) {
            string(out, "text", text.content());
        } else if (component instanceof TranslatableComponent translation) {
            string(out, "translate", translation.key());
            string(out, "fallback", translation.fallback());
            if (!translation.arguments().isEmpty()) {
                var args = new java.util.ArrayList<NBT>(translation.arguments().size());
                for (TranslationArgument argument : translation.arguments()) {
                    Object value = argument.value();
                    // NBT has no boolean type. A byte would render as "1"/"0" in a translation.
                    if (value instanceof Boolean bool) args.add(new NBTString(bool.toString()));
                    else if (value instanceof Number number) args.add(NBTCodec.jsonToNBT(typedNumbers(new JsonPrimitive(number))));
                    else args.add(encode((Component) value));
                }
                out.setTag("with", NbtCodecs.GENERIC_LIST.encode(wrapper, args));
            }
        } else if (component instanceof ScoreComponent score) {
            NBTCompound value = new NBTCompound();
            string(value, "name", score.name());
            string(value, "objective", score.objective());
            out.setTag("score", value);
        } else if (component instanceof SelectorComponent selector) {
            string(out, "selector", selector.pattern());
            component(out, "separator", selector.separator());
        } else if (component instanceof KeybindComponent keybind) {
            string(out, "keybind", keybind.keybind());
        } else if (component instanceof NBTComponent<?, ?> nbt) {
            string(out, "nbt", nbt.nbtPath());
            if (nbt.interpret()) out.setTag("interpret", new NBTByte(true));
            if (AdventureMessageExtensions.plain(nbt)) out.setTag("plain", new NBTByte(true));
            component(out, "separator", nbt.separator());
            if (nbt instanceof BlockNBTComponent block) string(out, "block", block.pos().asString());
            else if (nbt instanceof EntityNBTComponent entity) string(out, "entity", entity.selector());
            else if (nbt instanceof StorageNBTComponent storage) string(out, "storage", storage.storage().asString());
            else throw new IllegalArgumentException("Unsupported NBT component: " + nbt.getClass());
        } else if (component instanceof ObjectComponent object) {
            Component fallback = AdventureMessageExtensions.fallback(object);
            if (version.isOlderThan(ServerVersion.V_1_21_9)) {
                if (fallback != null) out = compound(fallback);
                else string(out, "text", "");
            } else if (object.contents() instanceof SpriteObjectContents sprite) {
                string(out, "sprite", sprite.sprite().asString());
                string(out, "atlas", sprite.atlas().asString());
            } else if (object.contents() instanceof PlayerHeadObjectContents head) {
                NBTCompound profile = new NBTCompound();
                string(profile, "name", head.name());
                if (head.id() != null) profile.setTag("id", uuid(head.id()));
                if (head.texture() != null) string(profile, "texture", head.texture().asString());
                if (!head.profileProperties().isEmpty()) {
                    NBTList<NBTCompound> properties = new NBTList<>(NBTType.COMPOUND);
                    for (var property : head.profileProperties()) {
                        NBTCompound entry = new NBTCompound();
                        string(entry, "name", property.name());
                        string(entry, "value", property.value());
                        string(entry, "signature", property.signature());
                        properties.addTag(entry);
                    }
                    profile.setTag("properties", properties);
                }
                out.setTag("player", profile);
                out.setTag("hat", new NBTByte(head.hat()));
            } else throw new IllegalArgumentException("Unsupported object component: " + object.contents().getClass());
            if (version.isNewerThanOrEquals(ServerVersion.V_1_21_9)) component(out, "fallback", fallback);
        } else throw new IllegalArgumentException("Unsupported component: " + component.getClass());

        style(out, component.style());
        if (!component.children().isEmpty()) {
            NBTList<NBTCompound> children = new NBTList<>(NBTType.COMPOUND);
            for (Component child : component.children()) children.addTag(compound(child));
            out.setTag("extra", children);
        }
        return out;
    }

    private void style(NBTCompound out, Style style) {
        if (style.font() != null) string(out, "font", style.font().asString());
        TextColor color = style.color();
        if (color != null) {
            if (downsampleColors) color = NamedTextColor.nearestTo(color);
            string(out, "color", color instanceof NamedTextColor named ? named.toString() : color.asHexString());
        }
        if (style.shadowColor() != null && version.isNewerThanOrEquals(ServerVersion.V_1_21_4)) {
            out.setTag("shadow_color", new NBTInt(style.shadowColor().value()));
        }
        for (TextDecoration decoration : TextDecoration.values()) {
            TextDecoration.State state = style.decoration(decoration);
            if (state != TextDecoration.State.NOT_SET) out.setTag(decoration.toString(), new NBTByte(state == TextDecoration.State.TRUE));
        }
        string(out, "insertion", style.insertion());
        if (style.clickEvent() != null) click(out, style.clickEvent());
        if (style.hoverEvent() != null) hover(out, style.hoverEvent());
    }

    private void click(NBTCompound out, ClickEvent click) {
        boolean modern = version.isNewerThanOrEquals(ServerVersion.V_1_21_5);
        NBTCompound event = new NBTCompound();
        String action = click.action().toString();
        string(event, "action", action);
        ClickEvent.Payload payload = click.payload();
        if (payload instanceof ClickEvent.Payload.Text text) {
            String field = !modern ? "value" : switch (action) {
                case "open_url" -> "url";
                case "open_file" -> "path";
                case "run_command", "suggest_command" -> "command";
                default -> "value";
            };
            String value = text.value();
            if (modern && action.equals("open_url") && !value.startsWith("http://") && !value.startsWith("https://")) value = "https://" + value;
            string(event, field, value);
        } else if (payload instanceof ClickEvent.Payload.Int number) {
            if (modern) event.setTag("page", new NBTInt(number.integer()));
            else string(event, "value", Integer.toString(number.integer()));
        } else if (payload instanceof ClickEvent.Payload.Custom custom) {
            if (!modern) return; // this event does not exist in the older protocol
            string(event, "id", custom.key().asString());
            if (custom.nbt() != null && !custom.nbt().string().isEmpty()) event.setTag("payload", snbt(custom.nbt()));
        } else if (payload instanceof ClickEvent.Payload.Dialog dialog) {
            if (version.isOlderThan(ServerVersion.V_1_21_6)) return;
            if (!(dialog.dialog() instanceof Dialog value)) throw new IllegalArgumentException("Dialog has no PacketEvents wire representation: " + dialog.dialog().getClass());
            event.setTag("dialog", Dialog.encode(wrapper, value));
        } else throw new IllegalArgumentException("Unsupported click payload: " + payload.getClass());
        out.setTag(modern ? "click_event" : "clickEvent", event);
    }

    private void hover(NBTCompound out, HoverEvent<?> hover) {
        boolean modern = version.isNewerThanOrEquals(ServerVersion.V_1_21_5);
        NBTCompound event = new NBTCompound();
        string(event, "action", hover.action().toString());
        Object value = hover.value();
        if (value instanceof Component text) {
            event.setTag(modern ? "value" : "contents", encode(text));
        } else if (value instanceof HoverEvent.ShowEntity entity) {
            NBTCompound contents = modern ? event : new NBTCompound();
            string(contents, modern ? "id" : "type", entity.type().asString());
            contents.setTag(modern ? "uuid" : "id", uuid(entity.id()));
            component(contents, "name", entity.name());
            if (!modern) event.setTag("contents", contents);
        } else if (value instanceof HoverEvent.ShowItem item) {
            NBTCompound contents = modern ? event : new NBTCompound();
            string(contents, "id", item.item().asString());
            contents.setTag("count", new NBTInt(item.count()));
            if (version.isOlderThan(ServerVersion.V_1_20_5)) {
                if (item.nbt() != null) string(contents, "tag", item.nbt().string());
            } else if (!item.dataComponents().isEmpty()) {
                NBTCompound components = new NBTCompound();
                for (Map.Entry<Key, DataComponentValue> entry : item.dataComponents().entrySet()) {
                    String key = entry.getKey().asString();
                    DataComponentValue data = entry.getValue();
                    if (data == DataComponentValue.removed()) components.setTag("!" + key, new NBTCompound());
                    else if (data instanceof DataComponentValue.TagSerializable tag) components.setTag(key, snbt(tag.asBinaryTag()));
                    else if (data instanceof GsonDataComponentValue json) components.setTag(key, NBTCodec.jsonToNBT(typedNumbers(json.element())));
                    else throw new IllegalArgumentException("Unsupported item data component: " + key + " (" + data.getClass() + ")");
                }
                contents.setTag("components", components);
            }
            if (!modern) event.setTag("contents", contents);
        } else throw new IllegalArgumentException("Unsupported hover payload: " + value.getClass());
        out.setTag(modern ? "hover_event" : "hoverEvent", event);
    }

    private void component(NBTCompound out, String name, Component value) {
        if (value != null) out.setTag(name, encode(value));
    }

    private static void string(NBTCompound out, String name, String value) {
        if (value != null) out.setTag(name, new NBTString(value));
    }

    private static NBTIntArray uuid(UUID uuid) {
        long most = uuid.getMostSignificantBits(), least = uuid.getLeastSignificantBits();
        return new NBTIntArray(new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least});
    }

    private static NBT snbt(BinaryTagHolder holder) {
        try {
            return binary(TagStringIO.tagStringIO().asTag(holder.string()));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid component SNBT", exception);
        }
    }

    // PE's generic converter treats Gson's LazilyParsedNumber as an int. Preserve decimals and
    // 64-bit integers in user-supplied JSON item data before handing it to the NBT container codec.
    private static JsonElement typedNumbers(JsonElement input) {
        if (input.isJsonObject()) {
            JsonObject out = new JsonObject();
            for (var entry : input.getAsJsonObject().entrySet()) out.add(entry.getKey(), typedNumbers(entry.getValue()));
            return out;
        }
        if (input.isJsonArray()) {
            JsonArray out = new JsonArray();
            for (JsonElement entry : input.getAsJsonArray()) out.add(typedNumbers(entry));
            return out;
        }
        if (input.isJsonPrimitive() && input.getAsJsonPrimitive().isNumber()) {
            var decimal = input.getAsBigDecimal();
            try {
                long value = decimal.longValueExact();
                if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) return new JsonPrimitive((int) value);
                return new JsonPrimitive(value);
            } catch (ArithmeticException ignored) {
                return new JsonPrimitive(decimal.doubleValue());
            }
        }
        return input;
    }

    // Avoid BinaryTagType: it changed from a class to an interface in Adventure 5.
    private static NBT binary(BinaryTag tag) {
        if (tag instanceof CompoundBinaryTag compound) {
            NBTCompound out = new NBTCompound();
            for (var entry : compound) out.setTag(entry.getKey(), binary(entry.getValue()));
            return out;
        }
        if (tag instanceof ListBinaryTag list) {
            if (list.size() == 0) return new NBTList<>(NBTType.END);
            NBT first = binary(list.get(0));
            NBTList<?> out = new NBTList<>(first.getType());
            out.addTagUnsafe(first);
            for (int i = 1; i < list.size(); i++) out.addTagUnsafe(binary(list.get(i)));
            return out;
        }
        if (tag instanceof StringBinaryTag value) return new NBTString(value.value());
        if (tag instanceof ByteBinaryTag value) return new NBTByte(value.value());
        if (tag instanceof ShortBinaryTag value) return new NBTShort(value.value());
        if (tag instanceof IntBinaryTag value) return new NBTInt(value.value());
        if (tag instanceof LongBinaryTag value) return new NBTLong(value.value());
        if (tag instanceof FloatBinaryTag value) return new NBTFloat(value.value());
        if (tag instanceof DoubleBinaryTag value) return new NBTDouble(value.value());
        if (tag instanceof ByteArrayBinaryTag value) return new NBTByteArray(value.value());
        if (tag instanceof IntArrayBinaryTag value) return new NBTIntArray(value.value());
        if (tag instanceof LongArrayBinaryTag value) return new NBTLongArray(value.value());
        if (tag instanceof EndBinaryTag) return NBTEnd.INSTANCE;
        throw new IllegalArgumentException("Unsupported binary tag: " + tag.getClass());
    }
}
