# GeyserMC Translator Node

> **Not a plugin.** `Geyser-Translator.jar` must **not** go in Paper `plugins/` or Velocity `plugins/`.
> It has no `plugin.yml` / `velocity-plugin.json`. Run it as a **standalone process**:
>
> ```bash
> java -jar Geyser-Translator.jar --nogui
> ```
>
> See [INSTALL.txt](INSTALL.txt) and [config.yml.example](config.yml.example). Use `run.bat` / `run.sh` after copying the JAR and config into one folder.

This module provides a dedicated GeyserMC translator node, designed to offload Bedrock translation from your Velocity/Paper server. It acts as an intermediary, translating Bedrock client connections to the Java protocol before forwarding them to your Java proxy (e.g., Velocity).

```
Bedrock  →  Geyser-Translator (standalone)  →  Velocity (+ Floodgate)  →  Paper
```

**On Velocity:** install **Floodgate** only (not this JAR). **On Paper:** no Geyser-Translator — Bedrock players arrive via Floodgate through Velocity.

## Setup with Floodgate and HAProxy

### 1. Floodgate Setup (on Velocity/Paper)

Floodgate is essential for allowing Bedrock players to join your Java server without a Java Edition account. Ensure Floodgate is installed and configured on your Velocity (or Paper, if not using Velocity) instance.

- **Download Floodgate**: Obtain the appropriate Floodgate plugin for your Velocity or Paper version.
- **Install**: Place the Floodgate plugin JAR in the `plugins` folder of your Velocity/Paper instance.
- **Configuration**: Floodgate will generate a `config.yml` on first run. Ensure that the `enable-proxy-protocol` setting is set to `true` if you are using HAProxy or another proxy that supports it.

### 2. Translator Node Configuration

In the `config.yml` of your Geyser Translator node, you will need to configure the following:

```yaml
# Geyser Translator specific configuration
bedrock:
  address: 0.0.0.0 # Listen on all interfaces
  port: 19132 # Default Bedrock port, change if necessary

remote:
  address: "your_velocity_ip" # IP address of your Velocity proxy
  port: 25577 # The port Velocity is listening on for PROXY protocol connections (e.g., in a server entry in Velocity's config)

advanced:
  java:
    useHaproxyProtocol: true # Enable HAProxy PROTOCOL for forwarding real IP and Floodgate data
```

### 3. HAProxy Configuration (Example)

If you are using HAProxy (or another Layer 4 Load Balancer) in front of your Translator nodes, you'll need to configure it to forward the PROXY protocol to the Translator nodes.

Here's an example `haproxy.cfg` snippet:

```
frontend bedrock_in
  bind *:19132
  mode tcp
  default_backend geyser_translators

backend geyser_translators
  mode tcp
  balance roundrobin
  option tcplog
  option tcp-check
  server translator1 192.168.1.101:19132 check send-proxy-v2
  server translator2 192.168.1.102:19132 check send-proxy-v2
  # Add more translator nodes as needed

listen velocity_proxy
  bind your_velocity_ip:25577 # This is the port Velocity will listen on for connections from the Translators
  mode tcp
  option tcplog
  option tcp-check
  server velocity_server your_velocity_ip:25577 check send-proxy-v2
```

**Explanation:**
- The `bedrock_in` frontend listens for incoming Bedrock connections on port 19132.
- It forwards these connections to the `geyser_translators` backend.
- The `geyser_translators` backend distributes connections across your Translator nodes. The `send-proxy-v2` option is crucial for HAProxy to send the PROXY protocol header to the Translator nodes.
- The `velocity_proxy` listen block (or similar configuration in your Velocity's `config.yml`) ensures Velocity is ready to receive PROXY protocol connections from the Translator nodes.

### 4. Velocity Configuration

Your Velocity `velocity.toml` (or `config.yml` if older version) should be configured to accept PROXY protocol connections from the Translator nodes and forward Floodgate data.

```toml
# velocity.toml (example)

# Enable PROXY protocol for connections from the Translator nodes
# This tells Velocity to expect a PROXY protocol header for connections on this port
servers:
  translator_entry:
    address: your_velocity_ip:25577 # Must match the bind address/port in HAProxy or Translator config
    proxy_protocol: true

# Configure your actual backend Minecraft server(s)
  lobby:
    address: "your_paper_server_ip:25565"
    # ... other lobby server settings

# Replace "lobby" with the actual name of your lobby server
force-players-to-server = "lobby"

# Ensure ip-forwarding is enabled if you want real client IPs on Paper
ip-forwarding = true

# If using Floodgate, ensure it's configured to work with a proxy
# (usually done automatically by Floodgate plugin itself)
```

## Operations (Phase 5)

### Configuration

Add a `translator` section to `config.yml`:

```yaml
translator:
  health-port: 8080          # HTTP /health and /metrics; 0 disables
  node-id: translator-1      # Unique node ID for session affinity metadata
  chunk-cache-size: 512      # Cross-session LRU cache for translated chunks
  chunk-translation-threads: 0  # 0 = available processors
  async-chunk-translation: true
  supported-bedrock-protocols: []   # e.g. [924, 944, 975] — empty = all
  skin-cdn-base-url: ""             # e.g. https://cdn.example.com/skins
  rak-send-cookie: true
  rak-packet-limit: -1              # -1 = Geyser default (500)
  rak-global-packet-limit: -1
  max-connections-per-address: -1
```

### Health and metrics

When `health-port` is set (default `8080`):

- `GET /health` — JSON with status, sessions, node_id, supported_protocols
- `GET /routing` — JSON for load-balancer routing (node_id, supported_protocols, supported_versions)
- `GET /metrics` — Prometheus-style counters:
  - `geyser_translator_active_sessions`
  - `geyser_translator_chunks_translated_total`
  - `geyser_translator_chunk_cache_hits_total`
  - `geyser_translator_chunk_cache_misses_total`

Use these endpoints behind HAProxy HTTP health checks or Prometheus scraping.

### Graceful drain

Run `geyser drain` in the translator console to toggle drain mode. While draining:

- New Bedrock connections are rejected
- Existing sessions continue until they disconnect
- `/health` reports `"status":"draining"`

### Session affinity

Each logged-in Java player UUID is mapped to this node's `node-id` and Bedrock protocol version in `SessionManager`. External load balancers can use `/routing` and session affinity for sticky routing on reconnect.

## Production hardening (Phase 6)

### Multi-version routing

Pin each translator node to specific Bedrock protocol versions via `supported-bedrock-protocols`. Clients on unsupported versions are rejected with a message listing accepted protocols. HAProxy can use HTTP checks against `/routing` to route by version:

```
backend geyser_translators_v26
  option httpchk GET /routing
  http-check expect rstring "supported_protocols":\[.*1001
  server t1 192.168.1.101:8080 check
  server t2 192.168.1.102:8080 check
```

### Skin CDN

When `skin-cdn-base-url` is set, skins are fetched from `{base}/{hash}.png` (hash = UUID.nameUUIDFromBytes(textureUrl)) before hitting Mojang.

### RakNet / DDoS tuning

Translator-specific RakNet overrides: `rak-send-cookie`, `rak-packet-limit`, `rak-global-packet-limit`, `max-connections-per-address`. Values of `-1` keep Geyser defaults.

### JMH benchmarks

Run microbenchmarks for movement normalization and version routing:

```powershell
./gradlew :benchmark:jmh
```

Results are written to `benchmark/build/results/jmh/`.

### Async chunk pipeline

Chunk encoding runs on a `chunk-translate-N` worker pool. Translated Bedrock payloads are cached cross-session by `(protocolVersion, dimension, chunkX, chunkZ, chunkData hash)` and sent on the session tick event loop.

This setup ensures that Bedrock clients connect to your Translator nodes, which then translate the packets and forward them via TCP with PROXY protocol to Velocity. Velocity then forwards these connections (including Floodgate authentication data and real client IPs) to your Paper backend server. Adjust IP addresses and ports according to your network topology.