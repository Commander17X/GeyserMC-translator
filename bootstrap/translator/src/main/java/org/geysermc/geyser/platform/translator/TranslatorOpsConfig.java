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

package org.geysermc.geyser.platform.translator;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public final class TranslatorOpsConfig {

    @Comment("HTTP health and metrics port. 0 disables the endpoint.")
    @Setting("health-port")
    private int healthPort = 8080;

    @Comment("Unique identifier for this translator node (used for session affinity metadata)")
    @Setting("node-id")
    private String nodeId = "translator-1";

    @Comment("Maximum translated chunks kept in the cross-session LRU cache")
    @Setting("chunk-cache-size")
    private int chunkCacheSize = 512;

    @Comment("Worker threads for async chunk translation. 0 uses available processors.")
    @Setting("chunk-translation-threads")
    private int chunkTranslationThreads = 0;

    @Comment("Whether chunk translation runs on a dedicated worker pool")
    @Setting("async-chunk-translation")
    private boolean asyncChunkTranslation = true;

    @Comment("Bedrock protocol versions this node accepts (e.g. 924, 944, 975). Empty = all supported.")
    @Setting("supported-bedrock-protocols")
    private List<Integer> supportedBedrockProtocols = new ArrayList<>();

    @Comment("Optional skin CDN base URL (e.g. https://cdn.example.com/skins). Skins are fetched as {base}/{hash}.png")
    @Setting("skin-cdn-base-url")
    private String skinCdnBaseUrl = "";

    @Comment("RakNet: send connection cookies for DDoS mitigation")
    @Setting("rak-send-cookie")
    private boolean rakSendCookie = true;

    @Comment("RakNet: max packets per second per connection. -1 uses Geyser default (500).")
    @Setting("rak-packet-limit")
    private int rakPacketLimit = -1;

    @Comment("RakNet: global packet limit per second. -1 uses Geyser default (100000).")
    @Setting("rak-global-packet-limit")
    private int rakGlobalPacketLimit = -1;

    @Comment("RakNet: max connections per IP address. -1 uses Geyser default (10).")
    @Setting("max-connections-per-address")
    private int maxConnectionsPerAddress = -1;

    public int healthPort() {
        return healthPort;
    }

    public String nodeId() {
        return nodeId;
    }

    public int chunkCacheSize() {
        return chunkCacheSize;
    }

    public int chunkTranslationThreads() {
        return chunkTranslationThreads;
    }

    public boolean asyncChunkTranslation() {
        return asyncChunkTranslation;
    }

    public List<Integer> supportedBedrockProtocols() {
        return supportedBedrockProtocols == null ? Collections.emptyList() : supportedBedrockProtocols;
    }

    public String skinCdnBaseUrl() {
        return skinCdnBaseUrl;
    }

    public boolean rakSendCookie() {
        return rakSendCookie;
    }

    public int rakPacketLimit() {
        return rakPacketLimit;
    }

    public int rakGlobalPacketLimit() {
        return rakGlobalPacketLimit;
    }

    public int maxConnectionsPerAddress() {
        return maxConnectionsPerAddress;
    }
}
