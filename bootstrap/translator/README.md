# GeyserMC Translator Node

This module provides a dedicated GeyserMC translator node, designed to offload Bedrock translation from your Velocity/Paper server. It acts as an intermediary, translating Bedrock client connections to the Java protocol before forwarding them to your Java proxy (e.g., Velocity).

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

This setup ensures that Bedrock clients connect to your Translator nodes, which then translate the packets and forward them via TCP with PROXY protocol to Velocity. Velocity then forwards these connections (including Floodgate authentication data and real client IPs) to your Paper backend server. Adjust IP addresses and ports according to your network topology.