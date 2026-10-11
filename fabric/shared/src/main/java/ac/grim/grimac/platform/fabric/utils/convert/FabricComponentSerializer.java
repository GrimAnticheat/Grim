package ac.grim.grimac.platform.fabric.utils.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Utility to serialize Adventure {@link Component}s to Gson {@link JsonElement}s
 * safely across both Adventure 4.x and Adventure 5.x runtime environments on Fabric.
 */
public final class FabricComponentSerializer {

    private static final MethodHandle SERIALIZE_TO_TREE_MH;
    private static final Object SERIALIZER_INSTANCE;
    private static final MethodHandle SERIALIZE_STRING_MH;

    static {
        MethodHandle serializeToTree = null;
        MethodHandle serializeString = null;
        Object instance = null;

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        // 1. Prefer native Kyori Adventure Gson serializer (Adventure 4.x & 5.x)
        try {
            Class<?> adventureGsonClass = Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
            Object inst = adventureGsonClass.getMethod("gson").invoke(null);
            try {
                serializeToTree = lookup.findVirtual(adventureGsonClass, "serializeToTree",
                        MethodType.methodType(JsonElement.class, Component.class));
            } catch (NoSuchMethodException ignored) {
                serializeString = lookup.findVirtual(adventureGsonClass, "serialize",
                        MethodType.methodType(String.class, Component.class));
            }
            instance = inst;
        } catch (Throwable ignored) {
            // 2. Fall back to PacketEvents shaded Adventure serializer (Adventure 4.x)
            try {
                Class<?> peGsonClass = Class.forName("io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer");
                Object inst = peGsonClass.getMethod("gson").invoke(null);
                try {
                    serializeToTree = lookup.findVirtual(peGsonClass, "serializeToTree",
                            MethodType.methodType(JsonElement.class, Component.class));
                } catch (NoSuchMethodException ignored2) {
                    serializeString = lookup.findVirtual(peGsonClass, "serialize",
                            MethodType.methodType(String.class, Component.class));
                }
                instance = inst;
            } catch (Throwable ignored2) {
            }
        }

        SERIALIZE_TO_TREE_MH = serializeToTree;
        SERIALIZE_STRING_MH = serializeString;
        SERIALIZER_INSTANCE = instance;
    }

    private FabricComponentSerializer() {}

    /**
     * Serializes an Adventure component into a {@link JsonElement}.
     *
     * @param component the component to serialize
     * @return the serialized json element
     */
    public static JsonElement serializeToTree(Component component) {
        if (SERIALIZER_INSTANCE != null) {
            if (SERIALIZE_TO_TREE_MH != null) {
                try {
                    return (JsonElement) SERIALIZE_TO_TREE_MH.invoke(SERIALIZER_INSTANCE, component);
                } catch (Throwable t) {
                    if (t instanceof RuntimeException) throw (RuntimeException) t;
                    throw new RuntimeException("Failed to serialize component to tree", t);
                }
            } else if (SERIALIZE_STRING_MH != null) {
                try {
                    String json = (String) SERIALIZE_STRING_MH.invoke(SERIALIZER_INSTANCE, component);
                    return JsonParser.parseString(json);
                } catch (Throwable t) {
                    if (t instanceof RuntimeException) throw (RuntimeException) t;
                    throw new RuntimeException("Failed to serialize component to string", t);
                }
            }
        }
        return io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer.gson().serializeToTree(component);
    }
}
