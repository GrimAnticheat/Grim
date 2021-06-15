package ac.grim.grimac.utils.data.packetentity.latency;

public class EntityMountData {
    public int vehicleID;
    public int[] passengers;
    public int lastTransaction;

    public EntityMountData(int vehicleID, int[] passengers, int lastTransaction) {
        this.vehicleID = vehicleID;
        this.passengers = passengers;
        this.lastTransaction = lastTransaction;
    }
}
