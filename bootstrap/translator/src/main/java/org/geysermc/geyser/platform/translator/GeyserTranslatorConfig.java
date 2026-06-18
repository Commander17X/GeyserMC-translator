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

import org.geysermc.geyser.configuration.GeyserRemoteConfig;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultBoolean;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultNumeric;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultString;
import org.spongepowered.configurate.interfaces.meta.range.NumericRange;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Collections;
import java.util.List;

@ConfigSerializable
public interface GeyserTranslatorConfig extends GeyserRemoteConfig {

    @Comment("Translator node scaling and operations settings")
    TranslatorOpsConfig translator();

    @ConfigSerializable
    interface TranslatorOpsConfig {
        @Comment("HTTP health and metrics port. 0 disables the endpoint.")
        @DefaultNumeric(8080)
        @NumericRange(from = 0, to = 65535)
        int healthPort();

        @Comment("Unique identifier for this translator node (used for session affinity metadata)")
        @DefaultString("translator-1")
        String nodeId();

        @Comment("Maximum translated chunks kept in the cross-session LRU cache")
        @DefaultNumeric(512)
        int chunkCacheSize();

        @Comment("Worker threads for async chunk translation. 0 uses available processors.")
        @DefaultNumeric(0)
        int chunkTranslationThreads();

        @Comment("Whether chunk translation runs on a dedicated worker pool")
        @DefaultBoolean(true)
        boolean asyncChunkTranslation();

        @Comment("Bedrock protocol versions this node accepts (e.g. 924, 944, 975). Empty = all supported.")
        default List<Integer> supportedBedrockProtocols() {
            return Collections.emptyList();
        }

        @Comment("Optional skin CDN base URL (e.g. https://cdn.example.com/skins). Skins are fetched as {base}/{hash}.png")
        @DefaultString("")
        String skinCdnBaseUrl();

        @Comment("RakNet: send connection cookies for DDoS mitigation")
        @DefaultBoolean(true)
        boolean rakSendCookie();

        @Comment("RakNet: max packets per second per connection. -1 uses Geyser default (500).")
        @DefaultNumeric(-1)
        int rakPacketLimit();

        @Comment("RakNet: global packet limit per second. -1 uses Geyser default (100000).")
        @DefaultNumeric(-1)
        int rakGlobalPacketLimit();

        @Comment("RakNet: max connections per IP address. -1 uses Geyser default (10).")
        @DefaultNumeric(-1)
        int maxConnectionsPerAddress();
    }
}
