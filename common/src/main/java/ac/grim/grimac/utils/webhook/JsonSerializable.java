package ac.grim.grimac.utils.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.IntFunction;

public interface JsonSerializable {
    @NotNull JsonElement toJson();

    static @NotNull JsonArray serializeArray(@Nullable JsonSerializable @NotNull [] serializableArray) {
        JsonArray array = new JsonArray();

        for (JsonSerializable serializable : serializableArray) {
            array.add(serializable == null ? JsonNull.INSTANCE : serializable.toJson());
        }

        return array;
    }

    static <T extends JsonSerializable> T @NotNull [] deserializeArray(JsonArray jsonArray, IntFunction<T[]> newArray, Function<JsonElement, T> constructor) {
        T[] array = newArray.apply(jsonArray.size());

        for (int i = 0; i < jsonArray.size(); i++) {
            array[i] = constructor.apply(jsonArray.get(i));
        }

        return array;
    }
}
