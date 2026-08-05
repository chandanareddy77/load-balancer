import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.List;

public class LoadBalancerServer {
    public static void main(String[] args) throws Exception {
        ConfigurationManager config = new ConfigurationManager();
        int port = config.getPort();
        List<String> backends = config.getBackendServers();
        String algorithmType = config.getAlgorithm();

        LoadBalancerStrategy strategy;
        if ("random".equalsIgnoreCase(algorithmType)) {
            strategy = new RandomStrategy();
            System.out.println("Loaded Strategy: Random");
        } else {
            strategy = new RoundRobinStrategy();
            System.out.println("Loaded Strategy: Round-Robin");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new ProxyHandler(backends, strategy));
        server.setExecutor(null);
        server.start();
        System.out.println("Java Load Balancer started on port " + port);
    }

    static class ProxyHandler implements HttpHandler {
        private final List<String> backends;
        private final LoadBalancerStrategy strategy;

        public ProxyHandler(List<String> backends, LoadBalancerStrategy strategy) {
            this.backends = backends;
            this.strategy = strategy;
        }

        @Override
        public void handle(HttpExchange exchange) {
            String targetServerUrl = strategy.getServer(backends);
            try {
                // Construct target URL
                String requestUri = exchange.getRequestURI().toString();
                URL url = URI.create(targetServerUrl + requestUri).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set request method
                connection.setRequestMethod(exchange.getRequestMethod());
                connection.setDoOutput(true);
                connection.setDoInput(true);

                // Copy headers
                exchange.getRequestHeaders().forEach((key, values) -> {
                    for (String value : values) {
                        connection.setRequestProperty(key, value);
                    }
                });

                // Forward request body if present
                if (exchange.getRequestMethod().equals("POST") || exchange.getRequestMethod().equals("PUT")) {
                    try (InputStream in = exchange.getRequestBody(); OutputStream out = connection.getOutputStream()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }

                // Get response code and headers
                int responseCode = connection.getResponseCode();
                
                // Copy response headers back
                connection.getHeaderFields().forEach((key, values) -> {
                    if (key != null) {
                        values.forEach(value -> exchange.getResponseHeaders().add(key, value));
                    }
                });

                // Read response from backend
                InputStream backendStream = (responseCode >= 400) ? connection.getErrorStream() : connection.getInputStream();
                byte[] responseBytes = new byte[0];
                if (backendStream != null) {
                    responseBytes = backendStream.readAllBytes();
                }

                // Send response back to client
                exchange.sendResponseHeaders(responseCode, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }

            } catch (Exception e) {
                try {
                    String error = "502 Bad Gateway: " + e.getMessage();
                    exchange.sendResponseHeaders(502, error.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(error.getBytes());
                    os.close();
                } catch (Exception ignored) {}
            }
        }
    }
}