package org.abyssmc.reaperac.protocol;

public class PlayerAbilitiesPacket {
    // TODO: I most likely have to account for player latency
    // TODO: Most likely need a method to simulate a "network" based on player latency
    /*public static void createListener(Plugin plugin, ProtocolManager protocolManager) {
        protocolManager.addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ABILITIES) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        PacketContainer packet = event.getPacket();

                        packet.getBooleans();

                        Grim player = GrimManager.playerGrimHashMap.get(event.getPlayer());

                        Bukkit.broadcastMessage(packet.toString());
                        player.isFlying = packet.getBooleans().read(0);
                        player.allowFlying = packet.getBooleans().read(0);
                        player.instantBreak = packet.getBooleans().read(0);
                    }
                }
        );
    }*/
}
