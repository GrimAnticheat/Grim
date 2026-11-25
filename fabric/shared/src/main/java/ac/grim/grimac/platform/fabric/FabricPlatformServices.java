package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.BlockTranslator;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformInventory;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import ac.grim.grimac.platform.fabric.player.FabricOfflineProfile;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.function.Function;

@UtilityClass
public final class FabricPlatformServices {
    private static Function<AbstractFabricPlatformPlayer<?>, AbstractFabricPlatformInventory> inventoryFactory;
    private static Function<Object, GrimEntity> entityFactory;
    private static Function<Object, BlockTranslator> blockTranslatorFactory;
    private static Function<String, Object> textLiteralFactory;
    private static Function<String, FabricOfflineProfile> profileLookup;
    private static IFabricConversionUtil conversionUtil;

    public static void configure(
            Function<AbstractFabricPlatformPlayer<?>, AbstractFabricPlatformInventory> inventoryFactory,
            Function<Object, GrimEntity> entityFactory,
            Function<Object, BlockTranslator> blockTranslatorFactory,
            Function<String, Object> textLiteralFactory,
            Function<String, FabricOfflineProfile> profileLookup,
            IFabricConversionUtil conversionUtil
    ) {
        FabricPlatformServices.inventoryFactory = Objects.requireNonNull(inventoryFactory, "inventoryFactory");
        FabricPlatformServices.entityFactory = Objects.requireNonNull(entityFactory, "entityFactory");
        FabricPlatformServices.blockTranslatorFactory = Objects.requireNonNull(blockTranslatorFactory, "blockTranslatorFactory");
        FabricPlatformServices.textLiteralFactory = Objects.requireNonNull(textLiteralFactory, "textLiteralFactory");
        FabricPlatformServices.profileLookup = Objects.requireNonNull(profileLookup, "profileLookup");
        FabricPlatformServices.conversionUtil = Objects.requireNonNull(conversionUtil, "conversionUtil");
    }

    public static AbstractFabricPlatformInventory createInventory(AbstractFabricPlatformPlayer<?> player) {
        return Objects.requireNonNull(inventoryFactory, "inventoryFactory").apply(player);
    }

    public static GrimEntity createEntity(Object entity) {
        return Objects.requireNonNull(entityFactory, "entityFactory").apply(entity);
    }

    public static BlockTranslator createBlockTranslator(Object player) {
        return Objects.requireNonNull(blockTranslatorFactory, "blockTranslatorFactory").apply(player);
    }

    public static Object textLiteral(String message) {
        return Objects.requireNonNull(textLiteralFactory, "textLiteralFactory").apply(message);
    }

    public static FabricOfflineProfile profileByName(String name) {
        return Objects.requireNonNull(profileLookup, "profileLookup").apply(name);
    }

    public static IFabricConversionUtil conversionUtil() {
        return Objects.requireNonNull(conversionUtil, "conversionUtil");
    }
}
