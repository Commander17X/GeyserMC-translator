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

import org.geysermc.geyser.util.MathUtils;

/**
 * Converts Bedrock's wrapped yaw (-180..180) into Java's unwrapped yaw.
 */
public final class YawUnwrapper {

    private YawUnwrapper() {
    }

    /**
     * @param bedrockYaw wrapped yaw from the Bedrock client
     * @param previousJavaYaw last yaw sent to the Java server
     * @return unwrapped yaw suitable for Java Edition (never ±180 jumps)
     */
    public static float unwrap(float bedrockYaw, float previousJavaYaw) {
        return previousJavaYaw + MathUtils.wrapDegrees(bedrockYaw - previousJavaYaw);
    }

    public static boolean isValidRotation(float yaw, float pitch, float headYaw) {
        return !isInvalidNumber(yaw) && !isInvalidNumber(pitch) && !isInvalidNumber(headYaw);
    }

    private static boolean isInvalidNumber(float val) {
        return Float.isNaN(val) || Float.isInfinite(val);
    }
}
