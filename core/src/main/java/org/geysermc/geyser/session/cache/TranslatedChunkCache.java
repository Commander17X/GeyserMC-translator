/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cross-session LRU cache for translated Bedrock chunk payloads.
 */
public final class TranslatedChunkCache {

  public static final int DEFAULT_MAX_ENTRIES = 512;

  public record TranslatedChunkKey(int protocolVersion, int dimensionId, int chunkX, int chunkZ, int sectionMaskHash) {
    public static TranslatedChunkKey from(GeyserSession session, int chunkX, int chunkZ, byte[] chunkData) {
      return new TranslatedChunkKey(
          session.getUpstream().getProtocolVersion(),
          session.getBedrockDimension().bedrockId(),
          chunkX,
          chunkZ,
          Arrays.hashCode(chunkData)
      );
    }
  }

  public record CachedTranslatedChunk(
      int subChunksLength,
      int chunkX,
      int chunkZ,
      int dimension,
      byte[] data,
      DataPalette[] javaChunks,
      boolean updateJavaChunkCache
  ) {
    public LevelChunkPacket toPacket() {
      LevelChunkPacket packet = new LevelChunkPacket();
      packet.setSubChunksLength(subChunksLength);
      packet.setCachingEnabled(false);
      packet.setChunkX(chunkX);
      packet.setChunkZ(chunkZ);
      packet.setDimension(dimension);
      packet.setData(io.netty.buffer.Unpooled.wrappedBuffer(data).retain());
      return packet;
    }
  }

  private final Map<TranslatedChunkKey, CachedTranslatedChunk> cache;
  private final int maxEntries;

  public TranslatedChunkCache(int maxEntries) {
    this.maxEntries = Math.max(1, maxEntries);
    this.cache = new LinkedHashMap<>(16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<TranslatedChunkKey, CachedTranslatedChunk> eldest) {
        return size() > TranslatedChunkCache.this.maxEntries;
      }
    };
  }

  public TranslatedChunkCache() {
    this(DEFAULT_MAX_ENTRIES);
  }

  public synchronized CachedTranslatedChunk get(TranslatedChunkKey key) {
    return cache.get(key);
  }

  public synchronized void put(TranslatedChunkKey key, LevelChunkPacket packet, DataPalette[] javaChunks, boolean updateJavaChunkCache) {
    ByteBuf data = packet.getData();
    if (data == null) {
      return;
    }
    byte[] bytes = new byte[data.readableBytes()];
    data.getBytes(data.readerIndex(), bytes);
    cache.put(key, new CachedTranslatedChunk(
        packet.getSubChunksLength(),
        packet.getChunkX(),
        packet.getChunkZ(),
        packet.getDimension(),
        bytes,
        javaChunks,
        updateJavaChunkCache
    ));
  }

  public synchronized void clear() {
    cache.clear();
  }

  public synchronized int size() {
    return cache.size();
  }

  public int maxEntries() {
    return maxEntries;
  }
}
