/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.movement;

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;

/**
 * Selects and emits the appropriate Java Edition move packet for a normalized state.
 */
public final class JavaMovePacketEmitter {

    private JavaMovePacketEmitter() {
    }

    public enum EmitResult {
        ROTATION,
        POSITION,
        STATUS_ONLY,
        NONE
    }

    public static EmitResult emit(GeyserSession session, SessionPlayerEntity entity, JavaMovementState state) {
        if (!state.positionUpdateRequired() && state.rotationChanged()) {
            emitRotation(session, entity, state);
            return EmitResult.ROTATION;
        }

        if (state.positionUpdateRequired() && state.javaPosition() != null) {
            if (!session.getWorldBorder().isPassingIntoBorderBoundaries(state.javaPosition().toFloat(), true)) {
                emitPosition(session, entity, state);
                return EmitResult.POSITION;
            }
            session.getCollisionManager().recalculatePosition();
            return EmitResult.NONE;
        }

        if (state.horizontalCollision() != session.getInputCache().lastHorizontalCollision() || state.onGround() != entity.isOnGround()) {
            session.sendDownstreamGamePacket(new ServerboundMovePlayerStatusOnlyPacket(state.onGround(), state.horizontalCollision()));
            return EmitResult.STATUS_ONLY;
        }

        return EmitResult.NONE;
    }

    private static void emitRotation(GeyserSession session, SessionPlayerEntity entity, JavaMovementState state) {
        session.sendDownstreamGamePacket(new ServerboundMovePlayerRotPacket(
            state.onGround(),
            state.horizontalCollision(),
            state.javaYaw(),
            state.pitch()
        ));

        applyRotation(entity, state);

        if (entity.getVehicle() != null) {
            entity.setPositionFromBedrockPos(state.bedrockPosition());
            session.getSkullCache().updateVisibleSkulls();
        }
    }

    private static void emitPosition(GeyserSession session, SessionPlayerEntity entity, JavaMovementState state) {
        Packet movePacket;
        if (state.rotationChanged()) {
            movePacket = new ServerboundMovePlayerPosRotPacket(
                state.onGround(),
                state.horizontalCollision(),
                state.javaPosition().getX(),
                state.javaPosition().getY(),
                state.javaPosition().getZ(),
                state.javaYaw(),
                state.pitch()
            );
            applyRotation(entity, state);
        } else {
            movePacket = new ServerboundMovePlayerPosPacket(
                state.onGround(),
                state.horizontalCollision(),
                state.javaPosition().getX(),
                state.javaPosition().getY(),
                state.javaPosition().getZ()
            );
        }

        entity.setPositionFromBedrockPos(state.bedrockPosition());
        session.sendDownstreamGamePacket(movePacket);
        session.getInputCache().markPositionPacketSent();
        session.getSkullCache().updateVisibleSkulls();
    }

    private static void applyRotation(SessionPlayerEntity entity, JavaMovementState state) {
        entity.setYaw(state.bedrockYaw());
        entity.setJavaYaw(state.javaYaw());
        entity.setPitch(state.pitch());
        entity.setHeadYaw(state.headYaw());
    }

    public static boolean isValidMove(GeyserSession session, Vector3f currentPosition, Vector3f newPosition, AntiCheatProfile profile) {
        if (isInvalidNumber(newPosition.getX()) || isInvalidNumber(newPosition.getY()) || isInvalidNumber(newPosition.getZ())) {
            return false;
        }
        if (currentPosition.distanceSquared(newPosition) > profile.maxMoveDistanceSquared()) {
            session.getGeyser().getLogger().debug(session.bedrockUsername() + " moved too quickly. current: " + currentPosition + ", new: " + newPosition);
            return false;
        }
        return true;
    }

    private static boolean isInvalidNumber(float val) {
        return Float.isNaN(val) || Float.isInfinite(val);
    }
}
