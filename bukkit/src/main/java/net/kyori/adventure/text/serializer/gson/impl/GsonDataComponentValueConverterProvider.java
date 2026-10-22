package net.kyori.adventure.text.serializer.gson.impl;

import com.google.gson.JsonNull;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.DataComponentValueConverterRegistry;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;

import java.util.Collections;

/**
 * Keeps Adventure's Gson data-component provider available after Bukkit shading.
 * The provider is discovered only through ServiceLoader, so Shadow's minimizer
 * otherwise removes it while retaining its service descriptor.
 */
public final class GsonDataComponentValueConverterProvider
        implements DataComponentValueConverterRegistry.Provider {
    private static final Key ID = Key.key("adventure", "gson");

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public Iterable<DataComponentValueConverterRegistry.Conversion<?, ?>> conversions() {
        return Collections.singletonList(DataComponentValueConverterRegistry.Conversion.convert(
                DataComponentValue.Removed.class,
                GsonDataComponentValue.class,
                (key, value) -> GsonDataComponentValue.gsonDataComponentValue(JsonNull.INSTANCE)
        ));
    }
}
