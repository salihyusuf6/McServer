package org.nova.cosmos;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.*;

/** Serves only the bundled pack, never a directory or an arbitrary filesystem path. */
public final class PackHost implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;
    public final String sha1;
    public PackHost(byte[] bytes, String bind, int port) throws Exception {
        sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        server = HttpServer.create(new InetSocketAddress(bind, port), 16);
        executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), r -> {
            Thread t = new Thread(r, "NovaCosmos-Pack"); t.setDaemon(true); return t;
        }, new ThreadPoolExecutor.AbortPolicy());
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            try {
                if (!exchange.getRequestURI().getPath().equals("/novacosmos.zip")) {
                    exchange.sendResponseHeaders(404, -1); return;
                }
                boolean head = exchange.getRequestMethod().equals("HEAD");
                if (!head && !exchange.getRequestMethod().equals("GET")) {
                    exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                    exchange.sendResponseHeaders(405, -1); return;
                }
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.getResponseHeaders().set("Content-Length", Integer.toString(bytes.length));
                exchange.getResponseHeaders().set("ETag", "\"" + sha1 + "\"");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, head ? -1 : bytes.length);
                if (!head) exchange.getResponseBody().write(bytes);
            } catch (IOException ignored) {
                // Client disconnected during download; no game-thread work occurs here.
            } finally { exchange.close(); }
        });
        server.start();
    }
    public int port() { return server.getAddress().getPort(); }
    @Override public void close() { server.stop(0); executor.shutdownNow(); }
}
