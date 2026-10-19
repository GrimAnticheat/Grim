package ac.grim.grimac.utils.message;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.json.legacyimpl.NBTLegacyHoverEventSerializer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Serializers for Minecraft's data format, selected by the running game's world data version. */
public final class KyoriMessageSerializer {
    private static final ConcurrentMap<Integer, GsonComponentSerializer> SERIALIZERS = new ConcurrentHashMap<>();

    private KyoriMessageSerializer() {
    }

    public static GsonComponentSerializer forDataVersion(int dataVersion) {
        return forDataVersion(dataVersion, false);
    }

    public static GsonComponentSerializer forDataVersion(int dataVersion, boolean downsampleColors) {
        int cacheKey = downsampleColors ? ~dataVersion : dataVersion;
        return SERIALIZERS.computeIfAbsent(cacheKey, ignored -> GsonComponentSerializer.builder()
                .options(JSONOptions.byDataVersion().at(dataVersion))
                .editOptions(options -> {
                    if (downsampleColors) options.value(JSONOptions.EMIT_RGB, false);
                })
                .legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.get())
                .build());
    }
}
