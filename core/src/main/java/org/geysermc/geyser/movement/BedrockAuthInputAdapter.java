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

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.physics.CollisionResult;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Set;

/**
 * Converts {@link PlayerAuthInputPacket} into a {@link JavaMovementState}.
 */
public final class BedrockAuthInputAdapter {

    private BedrockAuthInputAdapter() {
    }

    public static JavaMovementState adapt(GeyserSession session, SessionPlayerEntity entity, PlayerAuthInputPacket packet, boolean onGround) {
        Set<PlayerAuthInputData> inputData = packet.getInputData();
        float yaw = packet.getRotation().getY();
        float pitch = packet.getRotation().getX();
        float headYaw = packet.getRotation().getY();
        float javaYaw = YawUnwrapper.unwrap(yaw, entity.getJavaYaw());

        boolean hasVehicle = entity.getVehicle() != null;
        boolean actualPositionChanged = entity.bedrockPosition().distanceSquared(packet.getPosition()) > 4e-8;
        boolean positionUpdateRequired = !hasVehicle && (session.getInputCache().shouldSendPositionReminder() || actualPositionChanged);
        boolean rotationChanged = hasVehicle || (entity.getJavaYaw() != javaYaw || entity.getPitch() != pitch);
        boolean horizontalCollision = inputData.contains(PlayerAuthInputData.HORIZONTAL_COLLISION);
        boolean handleTeleport = inputData.contains(PlayerAuthInputData.HANDLE_TELEPORT);

        Vector3d javaPosition = null;
        if (positionUpdateRequired) {
            CollisionResult result = session.getCollisionManager().adjustBedrockPosition(packet.getPosition(), onGround, handleTeleport);
            if (result != null) {
                javaPosition = result.correctedMovement();
            }
        }

        return new JavaMovementState(
            javaPosition,
            packet.getPosition(),
            packet.getDelta(),
            yaw,
            javaYaw,
            pitch,
            headYaw,
            onGround,
            horizontalCollision,
            actualPositionChanged,
            rotationChanged,
            positionUpdateRequired,
            handleTeleport
        );
    }
}
