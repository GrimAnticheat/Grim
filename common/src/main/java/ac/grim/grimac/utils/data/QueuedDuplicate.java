package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

public record QueuedDuplicate(WrapperPlayClientPlayerFlying packet, TeleportAcceptData teleportData) {}
