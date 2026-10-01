package org.nova.cosmos;

import java.net.URI;
import java.net.http.*;
import java.util.Arrays;

public final class PackHostTest {
    public static void main(String[] args) throws Exception {
        byte[] payload = "test-pack-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (var host = new PackHost(payload, "127.0.0.1", 0)) {
            String base = "http://127.0.0.1:" + host.port();
            var client = HttpClient.newHttpClient();
            var get = client.send(HttpRequest.newBuilder(URI.create(base + "/novacosmos.zip")).build(), HttpResponse.BodyHandlers.ofByteArray());
            assert get.statusCode() == 200 && Arrays.equals(get.body(), payload);
            assert get.headers().firstValue("Content-Type").orElse("").equals("application/zip");
            assert host.sha1.equals(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(get.body())));
            var head = client.send(HttpRequest.newBuilder(URI.create(base + "/novacosmos.zip")).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofByteArray());
            assert head.statusCode() == 200 && head.body().length == 0;
            var denied = client.send(HttpRequest.newBuilder(URI.create(base + "/config.yml")).build(), HttpResponse.BodyHandlers.ofString());
            assert denied.statusCode() == 404;
            var post = client.send(HttpRequest.newBuilder(URI.create(base + "/novacosmos.zip")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            assert post.statusCode() == 405;
            System.out.println("PASS: HTTP ZIP, hash, HEAD, non-pack path denied, POST denied, host shutdown");
        }
    }
}
