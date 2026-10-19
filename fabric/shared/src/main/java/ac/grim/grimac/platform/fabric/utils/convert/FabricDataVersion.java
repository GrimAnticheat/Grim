package ac.grim.grimac.platform.fabric.utils.convert;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Minecraft's own version resource avoids accessor ABI changes between supported releases. */
final class FabricDataVersion {
    static final int CURRENT = read();

    private FabricDataVersion() {
    }

    private static int read() {
        try (var stream = FabricDataVersion.class.getResourceAsStream("/version.json")) {
            if (stream == null) throw new IllegalStateException("Minecraft's version.json is missing");
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new Gson().fromJson(reader, JsonObject.class).get("world_version").getAsInt();
            }
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot read Minecraft's data version", failure);
        }
    }
}
