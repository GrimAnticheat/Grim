package ac.grim.grimac.platform.fabric.utils;

import ac.grim.grimac.platform.api.player.BlockTranslator;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class PolymerHook {
    private static final boolean HAS_POLYMER = FabricLoader.getInstance().isModLoaded("polymer-core");

    private static final MethodHandle CREATE_CONTEXT;
    private static final MethodHandle GET_STATE;

    static {
        MethodHandle createContext = null;
        MethodHandle getState = null;

        if (HAS_POLYMER) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                Class<?> contextClass = Class.forName("xyz.nucleoid.packettweaker.PacketContext");
                Class<?> utilsClass = Class.forName("eu.pb4.polymer.core.api.block.PolymerBlockUtils");

                Method createMethod = null;
                for (Method m : contextClass.getDeclaredMethods()) {
                    if (m.getName().equals("create") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == ServerPlayer.class) {
                        createMethod = m;
                        break;
                    }
                }

                if (createMethod != null) {
                    MethodHandle rawCreate = lookup.unreflect(createMethod);

                    createContext = rawCreate.asType(MethodType.methodType(Object.class, ServerPlayer.class));
                }
                MethodHandle rawGet = lookup.findStatic(utilsClass, "getPolymerBlockState",
                        MethodType.methodType(BlockState.class, BlockState.class, contextClass));
                getState = rawGet.asType(MethodType.methodType(BlockState.class, BlockState.class, Object.class));

            } catch (Throwable t) {
                // If Polymer changes their API drastically, log it so server owners know why custom blocks aren't translating
                System.err.println("[GrimAC] Failed to hook Polymer translation API. Custom blocks may not render correctly or crash client when re-synchronizing.");
                t.printStackTrace();
            }
        }

        CREATE_CONTEXT = createContext;
        GET_STATE = getState;
    }

    public static BlockTranslator createTranslator(ServerPlayer player) {
        if (!HAS_POLYMER || CREATE_CONTEXT == null || GET_STATE == null) {
            return BlockTranslator.IDENTITY;
        }

        try {
            Object context = (Object) CREATE_CONTEXT.invokeExact(player);
            return new PolymerBlockTranslator(context);
        } catch (Throwable t) {
            return BlockTranslator.IDENTITY;
        }
    }

    public record PolymerBlockTranslator(Object context) implements BlockTranslator {

        @Override
        public int translate(int serverBlockId) {
            try {
                BlockState state = Block.stateById(serverBlockId);

                BlockState mappedState = (BlockState) GET_STATE.invokeExact(state, context);

                return Block.getId(mappedState);
            } catch (Throwable t) {
                return serverBlockId;
            }
        }
    }
}
