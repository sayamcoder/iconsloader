package iconloader.iconLoader.resourcepack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import iconloader.iconLoader.IconLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class HttpServerManager {
    private final IconLoader plugin;
    private HttpServer server;
    private String downloadUrl = "";

    public HttpServerManager(IconLoader plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("http-server.enabled", false)) return;

        int port = plugin.getConfig().getInt("http-server.port", 8080);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/pack.zip", this::servePack);
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            server = null;
            plugin.getLogger().warning("Failed to start HTTP Server (Port might be in use): " + e.getMessage());
            return;
        }

        String ip = plugin.getConfig().getString("http-server.public-ip", "127.0.0.1").trim();
        downloadUrl = "http://" + (ip.isEmpty() ? "127.0.0.1" : ip) + ":" + port + "/pack.zip";
        plugin.getLogger().info("Internal HTTP Server started on " + downloadUrl);
        adoptDownloadUrl();
    }

    public void stop() {
        if (server == null) return;
        server.stop(0);
        server = null;
        downloadUrl = "";
        plugin.getLogger().info("Internal HTTP Server stopped.");
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Only fills in an empty resource-pack.url. A URL the admin wrote by hand is never overwritten,
     * because a CDN or reverse-proxy address is usually the correct one.
     */
    private void adoptDownloadUrl() {
        String configured = plugin.getConfig().getString("resource-pack.url", "").trim();
        if (configured.isEmpty()) {
            plugin.getConfig().set("resource-pack.url", downloadUrl);
            plugin.saveConfig();
            plugin.getLogger().info("resource-pack.url was empty, so it was set to " + downloadUrl + ".");
        } else if (!configured.equals(downloadUrl)) {
            plugin.getLogger().info("Serving " + downloadUrl + " but keeping resource-pack.url as " + configured
                    + ". Point it at this address if players should download from here.");
        }
    }

    private void servePack(HttpExchange exchange) throws IOException {
        File packFile = plugin.getConfigManager().getPackFile();
        if (!packFile.isFile()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.sendResponseHeaders(200, packFile.length());
        try (InputStream in = new FileInputStream(packFile); OutputStream out = exchange.getResponseBody()) {
            in.transferTo(out);
        }
    }
}
