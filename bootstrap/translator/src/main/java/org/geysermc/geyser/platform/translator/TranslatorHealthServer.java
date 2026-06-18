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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.translator.TranslatorVersionRouter;
import org.geysermc.geyser.util.TranslatorMetrics;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lightweight HTTP health and Prometheus-style metrics endpoint for translator nodes.
 */
public final class TranslatorHealthServer {

    private final HttpServer server;

    public TranslatorHealthServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext("/metrics", this::handleMetrics);
        server.createContext("/routing", this::handleRouting);
        server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        GeyserImpl geyser = GeyserImpl.getInstance();
        int sessions = geyser.getSessionManager().size();
        boolean draining = geyser.getSessionManager().isDraining();
        String status = draining ? "draining" : "ok";
        String nodeId = geyser.getTranslatorNodeId() != null ? geyser.getTranslatorNodeId() : "";
        List<Integer> protocols = advertisedProtocols(geyser);

        String body = String.format(
            "{\"status\":\"%s\",\"sessions\":%d,\"platform\":\"%s\",\"draining\":%s,\"node_id\":\"%s\",\"supported_protocols\":[%s]}",
            status,
            sessions,
            PlatformType.TRANSLATOR.platformName().toLowerCase(),
            draining,
            nodeId,
            protocols.stream().map(String::valueOf).collect(Collectors.joining(","))
        );
        writeResponse(exchange, 200, "application/json", body);
    }

    private void handleRouting(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        GeyserImpl geyser = GeyserImpl.getInstance();
        List<Integer> protocols = advertisedProtocols(geyser);
        String nodeId = geyser.getTranslatorNodeId() != null ? geyser.getTranslatorNodeId() : "";
        boolean draining = geyser.getSessionManager().isDraining();

        String versions = protocols.stream()
            .map(protocol -> {
                var codec = GameProtocol.getBedrockCodec(protocol);
                return codec != null ? codec.getMinecraftVersion() : String.valueOf(protocol);
            })
            .map(v -> "\"" + v + "\"")
            .collect(Collectors.joining(","));

        String body = String.format(
            "{\"node_id\":\"%s\",\"draining\":%s,\"sessions\":%d,\"supported_protocols\":[%s],\"supported_versions\":[%s]}",
            nodeId,
            draining,
            geyser.getSessionManager().size(),
            protocols.stream().map(String::valueOf).collect(Collectors.joining(",")),
            versions
        );
        writeResponse(exchange, 200, "application/json", body);
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        TranslatorMetrics metrics = GeyserImpl.getInstance().getTranslatorMetrics();
        String body = metrics != null ? metrics.toPrometheus() : "";
        writeResponse(exchange, 200, "text/plain; version=0.0.4", body);
    }

    private static List<Integer> advertisedProtocols(GeyserImpl geyser) {
        TranslatorVersionRouter router = geyser.getTranslatorVersionRouter();
        if (router != null) {
            return router.advertisedProtocols();
        }
        return GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS;
    }

    private static void writeResponse(HttpExchange exchange, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
