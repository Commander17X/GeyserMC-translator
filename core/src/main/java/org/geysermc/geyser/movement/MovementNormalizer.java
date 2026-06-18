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
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;

/**
 * Main entry for Bedrock player movement normalization.
 */
public final class MovementNormalizer {

    private final GeyserSession session;
    private final TickCoalescer tickCoalescer = new TickCoalescer();

    public MovementNormalizer(GeyserSession session) {
        this.session = session;
    }

    /**
     * Called from {@link org.geysermc.geyser.translator.protocol.bedrock.entity.player.input.BedrockMovePlayer}.
     */
    public void process(PlayerAuthInputPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        if (!session.isSpawned()) {
            return;
        }

        entity.setBedrockInteractRotation(packet.getInteractRotation());

        if (TeleportGate.blocksMovement(session, packet.getPosition())) {
            return;
        }

        boolean actualPositionChanged = entity.bedrockPosition().distanceSquared(packet.getPosition()) > 4e-8;
        if (actualPositionChanged) {
            session.getBookEditCache().checkForSend();
        }

        if (entity.getBedPosition() != null) {
            return;
        }

        float yaw = packet.getRotation().getY();
        float pitch = packet.getRotation().getX();
        float headYaw = packet.getRotation().getY();
        if (!YawUnwrapper.isValidRotation(yaw, pitch, headYaw)) {
            return;
        }

        AntiCheatProfile profile = session.getGeyser().config().advanced().movementProfile();

        GroundStateResolver.applyJumpVelocity(entity, packet.getInputData().contains(PlayerAuthInputData.START_JUMPING));
        GroundStateResolver.applyClimbableVelocity(entity, entity.isOnClimbableBlock(), packet.getInputData().contains(PlayerAuthInputData.JUMPING));

        entity.setCollidingVertically(packet.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION));
        boolean isOnGround = GroundStateResolver.resolve(session, entity, entity.isCollidingVertically());

        handleVoidNoClip(packet, entity);

        JavaMovementState state = BedrockAuthInputAdapter.adapt(session, entity, packet, isOnGround);

        if (state.positionUpdateRequired() && state.javaPosition() == null) {
            // Collision adjustment cancelled the packet (e.g. honey block)
            finalizeEntityState(entity, packet, isOnGround, state.horizontalCollision());
            return;
        }

        if (state.positionUpdateRequired()) {
            if (!JavaMovePacketEmitter.isValidMove(session, entity.bedrockPosition(), packet.getPosition(), profile)) {
                session.getGeyser().getLogger().debug(ChatColor.RED + "Recalculating position...");
                session.getCollisionManager().recalculatePosition();
                finalizeEntityState(entity, packet, isOnGround, state.horizontalCollision());
                return;
            }

            int tick = session.sessionTick();
            if (tickCoalescer.shouldEmitPosition(tick, profile)) {
                JavaMovePacketEmitter.EmitResult result = JavaMovePacketEmitter.emit(session, entity, state);
                if (result == JavaMovePacketEmitter.EmitResult.POSITION) {
                    tickCoalescer.markPositionEmitted(tick);
                }
            } else {
                tickCoalescer.deferPosition(tick, state);
            }
        } else {
            JavaMovePacketEmitter.emit(session, entity, state);
        }

        finalizeEntityState(entity, packet, isOnGround, state.horizontalCollision());
        moveParrots(entity);
    }

    /**
     * Flushes a deferred position update at the end of a server tick.
     */
    public void flushPending() {
        int tick = session.sessionTick();
        JavaMovementState pending = tickCoalescer.takePending(tick);
        if (pending == null) {
            return;
        }

        SessionPlayerEntity entity = session.getPlayerEntity();
        if (pending.javaPosition() != null && JavaMovePacketEmitter.isValidMove(session, entity.bedrockPosition(), pending.bedrockPosition(), session.getGeyser().config().advanced().movementProfile())) {
            JavaMovePacketEmitter.EmitResult result = JavaMovePacketEmitter.emit(session, entity, pending);
            if (result == JavaMovePacketEmitter.EmitResult.POSITION) {
                tickCoalescer.markPositionEmitted(tick);
            }
        }
    }

    private void handleVoidNoClip(PlayerAuthInputPacket packet, SessionPlayerEntity entity) {
        if (packet.getPosition().getY() - EntityDefinitions.PLAYER.offset() >= session.getBedrockDimension().minY() - 5) {
            return;
        }

        boolean possibleOnGround = false;
        BoundingBox boundingBox = session.getCollisionManager().getPlayerBoundingBox().clone();
        boundingBox.extend(0, packet.getDelta().getY() - 2, 0);

        for (Entity other : session.getEntityCache().getEntities().values()) {
            if (!other.getFlag(EntityFlag.COLLIDABLE) || other == entity) {
                continue;
            }

            BoundingBox entityBoundingBox = new BoundingBox(
                other.position().up(other.getBoundingBoxHeight() / 2).toDouble(),
                other.getBoundingBoxWidth(),
                other.getBoundingBoxHeight(),
                other.getBoundingBoxWidth()
            );

            if (entityBoundingBox.checkIntersection(boundingBox)) {
                possibleOnGround = true;
                break;
            }
        }

        session.setNoClip(!possibleOnGround);
    }

    private void finalizeEntityState(SessionPlayerEntity entity, PlayerAuthInputPacket packet, boolean isOnGround, boolean horizontalCollision) {
        session.getInputCache().setLastHorizontalCollision(horizontalCollision);
        entity.setOnGround(isOnGround);
        entity.setLastTickEndVelocity(packet.getDelta());
        entity.setMotion(packet.getDelta());
    }

    private static void moveParrots(SessionPlayerEntity entity) {
        if (entity.getLeftParrot() != null) {
            entity.getLeftParrot().moveAbsoluteRaw(entity.position(), entity.getYaw(), entity.getPitch(), entity.getHeadYaw(), true, false);
        }
        if (entity.getRightParrot() != null) {
            entity.getRightParrot().moveAbsoluteRaw(entity.position(), entity.getYaw(), entity.getPitch(), entity.getHeadYaw(), true, false);
        }
    }
}
