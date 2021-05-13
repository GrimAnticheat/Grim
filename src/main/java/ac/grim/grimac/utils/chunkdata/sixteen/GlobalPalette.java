package ac.grim.grimac.utils.chunkdata.sixteen;

// Credit to https://github.com/Steveice10/MCProtocolLib/blob/master/src/main/java/com/github/steveice10/mc/protocol/data/game/chunk/palette/GlobalPalette.java
public class GlobalPalette implements Palette {
    public GlobalPalette() {
    }

    public int size() {
        return 2147483647;
    }

    public int stateToId(int state) {
        return state;
    }

    public int idToState(int id) {
        return id;
    }
}
