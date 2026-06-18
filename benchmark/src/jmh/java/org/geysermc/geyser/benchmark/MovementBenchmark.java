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

package org.geysermc.geyser.benchmark;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.movement.AntiCheatProfile;
import org.geysermc.geyser.movement.JavaMovementState;
import org.geysermc.geyser.movement.TickCoalescer;
import org.geysermc.geyser.movement.YawUnwrapper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class MovementBenchmark {

    private float previousYaw;
    private JavaMovementState movementState;
    private TickCoalescer coalescer;

    @Setup
    public void setup() {
        previousYaw = 90f;
        movementState = new JavaMovementState(
            Vector3d.ZERO,
            Vector3f.ZERO,
            Vector3f.ZERO,
            90f,
            90f,
            0f,
            90f,
            true,
            false,
            true,
            false,
            false,
            false
        );
        coalescer = new TickCoalescer();
    }

    @Benchmark
    public float unwrapYaw() {
        previousYaw = YawUnwrapper.unwrap(45f, previousYaw);
        return previousYaw;
    }

    @Benchmark
    public boolean coalescePosition() {
        int tick = 100;
        if (coalescer.shouldEmitPosition(tick, AntiCheatProfile.BALANCED)) {
            coalescer.markPositionEmitted(tick);
            return true;
        }
        coalescer.deferPosition(tick, movementState);
        return false;
    }
}
