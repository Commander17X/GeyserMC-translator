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

package org.geysermc.geyser.translator.protocol.java.level;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.TranslatedChunkCache;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;

import java.io.IOException;
import java.util.Map;

@Translator(packet = ClientboundLevelChunkWithLightPacket.class)
public class JavaLevelChunkWithLightTranslator extends PacketTranslator<ClientboundLevelChunkWithLightPacket> {

  @Override
  public void translate(GeyserSession session, ClientboundLevelChunkWithLightPacket packet) {
    if (session.isSpawned()) {
      ChunkUtils.updateChunkPosition(session, session.getPlayerEntity().position().toInt());
    }

    session.getChunkBatchTracker().chunkReceived();

    if (useAsyncTranslation(session)) {
      translateAsync(session, packet);
      return;
    }

    translateSync(session, packet);
  }

  private static boolean useAsyncTranslation(GeyserSession session) {
    GeyserImpl geyser = session.getGeyser();
    return geyser.platformType() == PlatformType.TRANSLATOR
        && geyser.isTranslatorAsyncChunksEnabled()
        && !session.getErosionHandler().isActive();
  }

  private void translateSync(GeyserSession session, ClientboundLevelChunkWithLightPacket packet) {
    try {
      ChunkTranslationEngine.ChunkTranslationResult result = ChunkTranslationEngine.translate(
          session,
          packet.getX(),
          packet.getZ(),
          packet.getChunkData(),
          packet.getBlockEntities()
      );
      finishTranslation(session, packet, result, null);
    } catch (IOException e) {
      session.getGeyser().getLogger().error("IO error while encoding chunk", e);
    }
  }

  private void translateAsync(GeyserSession session, ClientboundLevelChunkWithLightPacket packet) {
    GeyserImpl geyser = session.getGeyser();
    byte[] chunkData = packet.getChunkData().clone();
    BlockEntityInfo[] blockEntities = packet.getBlockEntities();
    int chunkX = packet.getX();
    int chunkZ = packet.getZ();

    TranslatedChunkCache.TranslatedChunkKey cacheKey =
        TranslatedChunkCache.TranslatedChunkKey.from(session, chunkX, chunkZ, chunkData);

    TranslatedChunkCache cache = geyser.getTranslatedChunkCache();
    if (cache != null) {
      TranslatedChunkCache.CachedTranslatedChunk cached = cache.get(cacheKey);
      if (cached != null) {
        geyser.getTranslatorMetrics().recordCacheHit();
        session.executeInEventLoop(() -> {
          if (session.isClosed()) {
            return;
          }
          if (cached.updateJavaChunkCache()) {
            session.getChunkCache().addToCache(chunkX, chunkZ, cached.javaChunks());
          }
          LevelChunkPacket levelChunkPacket = cached.toPacket();
          session.sendUpstreamPacket(levelChunkPacket);
          updateItemFrames(session, packet);
        });
        return;
      }
      geyser.getTranslatorMetrics().recordCacheMiss();
    }

    ChunkTranslationExecutor.getInstance().execute(() -> {
      try {
        ChunkTranslationEngine.ChunkTranslationResult result = ChunkTranslationEngine.translate(
            session, chunkX, chunkZ, chunkData, blockEntities
        );
        geyser.getTranslatorMetrics().recordChunkTranslated();

        session.executeInEventLoop(() -> {
          if (session.isClosed()) {
            result.release();
            return;
          }
          finishTranslation(session, packet, result, cacheKey);
        });
      } catch (IOException e) {
        session.executeInEventLoop(() -> {
          if (!session.isClosed()) {
            session.getGeyser().getLogger().error("IO error while encoding chunk", e);
          }
        });
      } catch (Throwable t) {
        session.executeInEventLoop(() -> {
          if (!session.isClosed()) {
            session.getGeyser().getLogger().error("Async chunk translation failed", t);
          }
        });
      }
    });
  }

  private static void finishTranslation(
      GeyserSession session,
      ClientboundLevelChunkWithLightPacket packet,
      ChunkTranslationEngine.ChunkTranslationResult result,
      TranslatedChunkCache.TranslatedChunkKey cacheKey
  ) {
    if (result.updateJavaChunkCache()) {
      session.getChunkCache().addToCache(packet.getX(), packet.getZ(), result.javaChunks());
    }

    LevelChunkPacket levelChunkPacket = result.levelChunkPacket();
    if (cacheKey != null) {
      TranslatedChunkCache cache = session.getGeyser().getTranslatedChunkCache();
      if (cache != null) {
        cache.put(cacheKey, levelChunkPacket, result.javaChunks(), result.updateJavaChunkCache());
      }
    }

    session.sendUpstreamPacket(levelChunkPacket);
    updateItemFrames(session, packet);
  }

  private static void updateItemFrames(GeyserSession session, ClientboundLevelChunkWithLightPacket packet) {
    for (Map.Entry<Vector3i, ItemFrameEntity> entry : session.getItemFrameCache().entrySet()) {
      Vector3i position = entry.getKey();
      if ((position.getX() >> 4) == packet.getX() && (position.getZ() >> 4) == packet.getZ()) {
        entry.getValue().updateBlock(true);
      }
    }
  }
}
