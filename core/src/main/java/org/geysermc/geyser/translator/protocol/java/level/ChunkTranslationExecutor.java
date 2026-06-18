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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dedicated worker pool for async chunk translation on translator nodes.
 */
public final class ChunkTranslationExecutor {

  private static volatile ChunkTranslationExecutor instance;

  private final ExecutorService executor;

  private ChunkTranslationExecutor(int threads) {
    int poolSize = threads > 0 ? threads : Runtime.getRuntime().availableProcessors();
    AtomicInteger counter = new AtomicInteger();
    ThreadFactory factory = runnable -> {
      Thread thread = new Thread(runnable, "chunk-translate-" + counter.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    };
    this.executor = Executors.newFixedThreadPool(poolSize, factory);
  }

  public static void init(int threads) {
    if (instance == null) {
      synchronized (ChunkTranslationExecutor.class) {
        if (instance == null) {
          instance = new ChunkTranslationExecutor(threads);
        }
      }
    }
  }

  public static ChunkTranslationExecutor getInstance() {
    ChunkTranslationExecutor local = instance;
    if (local == null) {
      init(0);
      local = instance;
    }
    return local;
  }

  public void execute(Runnable task) {
    executor.execute(task);
  }

  public void shutdown() {
    executor.shutdown();
  }
}
