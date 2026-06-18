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

/**
 * Resolves onGround from vertical collision flags and Y velocity heuristics.
 */
public final class GroundStateResolver {

    private GroundStateResolver() {
    }

    public static boolean resolve(GeyserSession session, SessionPlayerEntity entity, boolean collidingVertically) {
        boolean hasVehicle = entity.getVehicle() != null;
        if (hasVehicle || session.isNoClip()) {
            // VERTICAL_COLLISION is not accurate while in a vehicle (as of 1.21.62)
            return false;
        }
        return collidingVertically && entity.getLastTickEndVelocity().getY() < 0;
    }

    public static void applyJumpVelocity(SessionPlayerEntity entity, boolean startJumping) {
        if (entity.isOnGround() && startJumping) {
            entity.setLastTickEndVelocity(Vector3f.from(
                entity.getLastTickEndVelocity().getX(),
                Math.max(entity.getLastTickEndVelocity().getY(), entity.getJumpVelocity()),
                entity.getLastTickEndVelocity().getZ()
            ));
        }
    }

    public static void applyClimbableVelocity(SessionPlayerEntity entity, boolean onClimbableBlock, boolean jumping) {
        if (onClimbableBlock && jumping) {
            entity.setLastTickEndVelocity(Vector3f.from(
                entity.getLastTickEndVelocity().getX(),
                0.2F,
                entity.getLastTickEndVelocity().getZ()
            ));
        }
    }
}
