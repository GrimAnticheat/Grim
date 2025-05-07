package ac.grim.grimac.utils.data;



public class PlayerPistonData {
    public final PistonTemplate pistonTemplate;
    public final int lastTransactionSent;

    // Calculate if the player has no-push, and when to end the possibility of applying piston
    int ticksOfPistonBeingAlive = 0;

    public PlayerPistonData(PistonTemplate playerPistonData, int lastTransactionSent) {
        this.pistonTemplate = playerPistonData;
        this.lastTransactionSent = lastTransactionSent;
    }

    // We don't know when the piston has applied, or what stage of pushing it is on
    // Therefore, we need to use what we have - the number of movement packets.
    // 10 is a very cautious number
    public boolean tickIfGuaranteedFinished() {
        return ++ticksOfPistonBeingAlive >= 10;
    }
}
