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
 * THE above copyright notice and this permission notice shall be included in
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

package org.geysermc.geyser.translator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.geysermc.geyser.network.GameProtocol;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Declares which Bedrock protocol versions a translator node accepts for load-balancer routing.
 */
public final class TranslatorVersionRouter {

    private final IntList acceptedProtocols;
    private final boolean acceptAll;

    public TranslatorVersionRouter(List<Integer> configuredProtocols) {
        if (configuredProtocols == null || configuredProtocols.isEmpty()) {
            this.acceptAll = true;
            this.acceptedProtocols = IntLists.emptyList();
        } else {
            this.acceptAll = false;
            IntArrayList list = new IntArrayList(configuredProtocols.size());
            for (Integer protocol : configuredProtocols) {
                if (protocol != null && protocol > 0) {
                    list.add(protocol.intValue());
                }
            }
            this.acceptedProtocols = IntLists.unmodifiable(list);
        }
    }

    public boolean accepts(int protocolVersion) {
        if (GameProtocol.getBedrockCodec(protocolVersion) == null) {
            return false;
        }
        if (acceptAll) {
            return true;
        }
        return acceptedProtocols.contains(protocolVersion);
    }

    public List<Integer> advertisedProtocols() {
        if (acceptAll) {
            return GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS;
        }
        return Collections.unmodifiableList(
            acceptedProtocols.intStream().boxed().collect(Collectors.toList())
        );
    }

    public boolean isRestricted() {
        return !acceptAll;
    }
}
