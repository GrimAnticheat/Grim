package ac.grim.grimac.utils.chunkdata.sixteen;

// Credit to https://github.com/Steveice10/MCProtocolLib/blob/master/src/main/java/com/github/steveice10/mc/protocol/data/game/chunk/palette/Palette.java
public interface Palette {
    int size();

    int stateToId(int var1);

    int idToState(int var1);
}