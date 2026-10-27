package iconloader.iconLoader.resourcepack;

import com.sun.net.httpserver.HttpServer;
import iconloader.iconLoader.IconLoader;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class HttpServerManager {
    private final IconLoader plugin;
    private HttpServer server;
    private String localUrl = "";

    public HttpServerManager(IconLoader plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("http-server.enabled", false)) return;

        int port = plugin.getConfig().getInt("http-server.port", 8080);
        File packFile = new File(plugin.getDataFolder(), "output/IconsLoader-ResourcePack.zip");

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/pack.zip", exchange -> {
                if (!packFile.exists()) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                byte[] bytes = Files.readAllBytes(packFile.toPath());
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
            server.setExecutor(null);
            server.start();

            String ip = plugin.getConfig().getString("http-server.public-ip", "127.0.0.1");
            localUrl = "http://" + ip + ":" + port + "/pack.zip";
            plugin.getLogger().info("Internal HTTP Server started on " + localUrl);

            // Auto-apply the URL to config
            plugin.getConfig().set("resource-pack.url", localUrl);
            plugin.saveConfig();

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to start HTTP Server (Port might be in use): " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Internal HTTP Server stopped.");
        }
    }
}