package ac.grim.grimac.platform.minestom.player;

import ac.grim.grimac.platform.api.player.BlockTranslator;

/**
 * Minestom uses the vanilla global block-state id space, the same one PacketEvents' server
 * block ids use, so no per-client remapping is needed (identity). ViaVersion clients are
 * translated at the proxy, so the backend only ever deals in native ids.
 */
public final class MinestomBlockTranslator implements BlockTranslator {

    public static final MinestomBlockTranslator INSTANCE = new MinestomBlockTranslator();

    @Override
    public int translate(int serverBlockId) {
        return serverBlockId;
    }
}
