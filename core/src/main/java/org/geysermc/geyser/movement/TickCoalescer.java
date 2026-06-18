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

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Coalesces Bedrock sub-tick position inputs to at most one Java move packet per server tick.
 */
public final class TickCoalescer {

    private int lastPositionEmitTick = -1;
    private int pendingTick = -1;
    private @Nullable JavaMovementState pendingState;

    /**
     * @return true if a position move packet may be emitted now
     */
    public boolean shouldEmitPosition(int currentTick, AntiCheatProfile profile) {
        if (!profile.coalescePerTick()) {
            return true;
        }
        return lastPositionEmitTick < currentTick;
    }

    /**
     * Stores the latest movement state for deferredStop coalesced emit at tick boundary.
     */
    public void deferPosition(int currentTick, JavaMovementState state) {
        pendingTick = currentTick;
        pendingState = state;
    }

    public @Nullable JavaMovementState takePending(int currentTick) {
        if (pendingState == null || pendingTick != currentTick) {
            return null;
        }
        JavaMovementState state = pendingState;
        pendingState = null;
        pendingTick = -1;
        return state;
    }

    public void markPositionEmitted(int currentTick) {
        lastPositionEmitTick = currentTick;
    }

    public boolean hasPending() {
        return pendingState != null;
    }
}
