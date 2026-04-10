/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2022 retrooper and contributors
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

package com.github.retrooper.packetevents.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PacketEventsImplHelper {

    private PacketEventsImplHelper() {
    }

    public static @Nullable ProtocolPacketEvent handlePacket(
            Object channel, User user, Object player, Object buffer,
            boolean autoProtocolTranslation, PacketSide side
    ) throws Exception {
        if (side == PacketSide.SERVER) {
            return handleClientBoundPacket(channel, user, player, buffer, autoProtocolTranslation);
        } else {
            return handleServerBoundPacket(channel, user, player, buffer, autoProtocolTranslation);
        }
    }

    public static @Nullable PacketSendEvent handleClientBoundPacket(
            Object channel, User user, Object player, Object buffer,
            boolean autoProtocolTranslation
    ) throws Exception {
        if (!ByteBufHelper.isReadable(buffer)) {
            return null;
        }

        int preProcessIndex = ByteBufHelper.readerIndex(buffer);
        PacketSendEvent packetSendEvent;
        try {
            packetSendEvent = EventCreationUtil.createSendEvent(channel, user, player, buffer, autoProtocolTranslation);
        } catch (Exception exception) {
            if (shouldBypassClientboundException(user)) {
                ByteBufHelper.readerIndex(buffer, preProcessIndex);
                return null;
            }
            throw exception;
        }
        int processIndex = ByteBufHelper.readerIndex(buffer);
        PacketEvents.getAPI().getEventManager().callEvent(packetSendEvent, () -> {
            ByteBufHelper.readerIndex(buffer, processIndex);
        }, !autoProtocolTranslation);
        if (!packetSendEvent.isCancelled()) {
            if (packetSendEvent.getLastUsedWrapper() != null) {
                ByteBufHelper.clear(buffer);
                packetSendEvent.getLastUsedWrapper().writeVarInt(packetSendEvent.getPacketId());
                packetSendEvent.getLastUsedWrapper().write();
            } else {
                ByteBufHelper.readerIndex(buffer, preProcessIndex);
            }
        } else {
            ByteBufHelper.clear(buffer);
        }

        if (packetSendEvent.hasPostTasks()) {
            for (Runnable task : packetSendEvent.getPostTasks()) {
                task.run();
            }
        }

        return packetSendEvent;
    }

    private static boolean shouldBypassClientboundException(@Nullable User user) {
        return user == null || user.getEncoderState() == ConnectionState.LOGIN;
    }

    public static @Nullable PacketReceiveEvent handleServerBoundPacket(
            Object channel, User user, Object player, Object buffer,
            boolean autoProtocolTranslation
    ) throws Exception {
        if (!ByteBufHelper.isReadable(buffer)) {
            return null;
        }

        int preProcessIndex = ByteBufHelper.readerIndex(buffer);
        PacketReceiveEvent packetReceiveEvent = EventCreationUtil.createReceiveEvent(channel, user, player, buffer, autoProtocolTranslation);
        int processIndex = ByteBufHelper.readerIndex(buffer);
        PacketEvents.getAPI().getEventManager().callEvent(packetReceiveEvent, () -> {
            ByteBufHelper.readerIndex(buffer, processIndex);
        }, !autoProtocolTranslation);
        if (!packetReceiveEvent.isCancelled()) {
            if (packetReceiveEvent.getLastUsedWrapper() != null) {
                ByteBufHelper.clear(buffer);
                packetReceiveEvent.getLastUsedWrapper().writeVarInt(packetReceiveEvent.getPacketId());
                packetReceiveEvent.getLastUsedWrapper().write();
            } else {
                ByteBufHelper.readerIndex(buffer, preProcessIndex);
            }
        } else {
            ByteBufHelper.clear(buffer);
        }
        if (packetReceiveEvent.hasPostTasks()) {
            for (Runnable task : packetReceiveEvent.getPostTasks()) {
                task.run();
            }
        }
        return packetReceiveEvent;
    }

    public static void handleDisconnection(Object channel, @Nullable UUID uuid) {
        synchronized (channel) {
            ProtocolManager protocolManager = PacketEvents.getAPI().getProtocolManager();
            User user = protocolManager.getUser(channel);

            if (user != null) {
                UserDisconnectEvent disconnectEvent = new UserDisconnectEvent(user);
                PacketEvents.getAPI().getEventManager().callEvent(disconnectEvent);
                protocolManager.removeUser(user.getChannel());
            }

            if (uuid == null) {
                protocolManager.removeChannel(channel);
            } else {
                protocolManager.removeChannelById(uuid);
            }
        }
    }
}
