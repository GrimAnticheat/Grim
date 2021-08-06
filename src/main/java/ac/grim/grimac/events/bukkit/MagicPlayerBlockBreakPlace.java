/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2021 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.PlayerOpenBlockData;
import ac.grim.grimac.utils.data.packetentity.latency.BlockPlayerUpdate;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MagicPlayerBlockBreakPlace implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;

        // Taken from:
        // https://github.com/ViaVersion/ViaVersion/blob/master/bukkit-legacy/src/main/java/com/viaversion/viaversion/bukkit/listeners/protocol1_9to1_8/PaperPatch.java
        // (GrimAC and ViaVersion are both GPL licensed)
        //
        // This code fixes an issue where a 1.9 player places a block inside themselves
        // Required due to the following packets:
        // Client -> Server: I right-clicked a block!
        // Client: Interaction failed, not placing block (fails silently)
        // Server: You right-clicked a block?  Placing block! Block place successful because you can place blocks
        // inside yourself because of a bad paper patch.
        // GrimAC: Player placed block, add it to the world queue.
        //
        // Desync occurs because the block is added before it actually was added to the world
        // As we believe this block was placed client sided before server sided, while it is the other way around
        //
        // Also it's nice to have this patch and fix that bug :)
        Material type = event.getBlockPlaced().getType();
        if (!isPlacable(type)) {
            Location location = event.getPlayer().getLocation();
            Block locationBlock = location.getBlock();

            if (locationBlock.equals(event.getBlock())) {
                event.setCancelled(true);
                return;
            } else {
                if (locationBlock.getRelative(BlockFace.UP).equals(event.getBlock())) {
                    event.setCancelled(true);
                    return;
                } else {
                    Location diff = location.clone().subtract(event.getBlock().getLocation().add(0.5D, 0, 0.5D));
                    // Within radius of block
                    if (Math.abs(diff.getX()) <= 0.8 && Math.abs(diff.getZ()) <= 0.8D) {
                        // Are they on the edge / shifting ish
                        if (diff.getY() <= 0.1D && diff.getY() >= -0.1D) {
                            event.setCancelled(true);
                            return;
                        }
                        BlockFace relative = event.getBlockAgainst().getFace(event.getBlock());
                        // Are they towering up, (handles some latency)
                        if (relative == BlockFace.UP) {
                            if (diff.getY() < 1D && diff.getY() >= 0D) {
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }
        }

        Block block = event.getBlock();
        int materialID = block.getType().getId();
        int blockData = block.getData();

        int combinedID = materialID + (blockData << 12);

        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, event.getBlockAgainst().getLocation()), block.getX(), block.getY(), block.getZ(), combinedID);
        player.compensatedWorld.changeBlockQueue.add(data);

    }

    private boolean isPlacable(Material material) {
        if (!material.isSolid()) return true;
        // signs and banners
        switch (material.getId()) {
            case 63:
            case 68:
            case 176:
            case 177:
                return true;
            default:
                return false;
        }
    }

    public static int getPlayerTransactionForPosition(GrimPlayer player, Location location) {
        int transaction = player.lastTransactionAtStartOfTick;

        for (BlockPlayerUpdate update : player.compensatedWorld.packetBlockPositions) {
            if (update.position.getX() == location.getBlockX()
                    && update.position.getY() == location.getBlockY()
                    && update.position.getZ() == location.getBlockZ()) {
                transaction = update.transaction;
            }
        }

        return transaction;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();

        // Even when breaking waterlogged stuff, the client assumes it will turn into air (?)
        // So in 1.12 everything probably turns into air when broken
        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, block.getLocation()), block.getX(), block.getY(), block.getZ(), 0);
        player.compensatedWorld.changeBlockQueue.add(data);
    }

    // This works perfectly and supports the client changing blocks from interacting with blocks
    // This event is broken again.
    //@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockInteractEvent(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        // Client side interactable -> Door, trapdoor, gate
        if (block != null && Materials.checkFlag(block.getType(), Materials.CLIENT_SIDE_INTERACTABLE)) {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            PlayerOpenBlockData data = new PlayerOpenBlockData(getPlayerTransactionForPosition(player, event.getClickedBlock().getLocation()), block.getX(), block.getY(), block.getZ());
            player.compensatedWorld.openBlockData.add(data);
        }
    }
}
