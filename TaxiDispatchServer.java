
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class TaxiDispatchServer {

    static final List<Driver> drivers = new ArrayList<>();
    static final RoadNetwork network = new RoadNetwork();

    static final Path WEB_ROOT =
            Paths.get("web").toAbsolutePath().normalize();

    static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    static void send(HttpExchange exchange, int status, String body)
            throws IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    static void driversApi(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equals("GET")) {
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        StringBuilder json = new StringBuilder("[");

        synchronized (drivers) {
            for (int i = 0; i < drivers.size(); i++) {
                Driver d = drivers.get(i);

                if (i > 0) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"id\":").append(d.id).append(",")
                    .append("\"name\":\"").append(jsonEscape(d.name)).append("\",")
                    .append("\"location\":\"").append(jsonEscape(d.location)).append("\",")
                    .append("\"available\":").append(d.available)
                    .append("}");
            }
        }

        json.append("]");
        send(exchange, 200, json.toString());
    }

    static double readCoordinate(String body, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)"
        );

        Matcher matcher = pattern.matcher(body);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing " + key);
        }

        return Double.parseDouble(matcher.group(1));
    }

    static void assignApi(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equals("POST")) {
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try {
            String body;

            try (InputStream in = exchange.getRequestBody()) {
                body = new String(
                        in.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }

            double x = readCoordinate(body, "pickupX");
            double y = readCoordinate(body, "pickupY");

            String pickupLocation = network.nearestLocation(x, y);

            Driver nearest = null;
            double minimumRoadDistance = Double.MAX_VALUE;
            List<String> bestPath = new ArrayList<>();

            synchronized (drivers) {
                for (Driver d : drivers) {
                    if (!d.available) {
                        continue;
                    }

                    double roadDistance = network.shortestDistance(
                            d.location, pickupLocation
                    );

                    if (roadDistance < minimumRoadDistance) {
                        minimumRoadDistance = roadDistance;
                        nearest = d;

                        bestPath = network.shortestPath(
                                d.location, pickupLocation
                        );
                    }
                }

                if (nearest == null) {
                    send(exchange, 409,
                            "{\"error\":\"No available drivers. Restart the server to reset the demo.\"}");
                    return;
                }

                nearest.available = false;
            }

            StringBuilder pathJson = new StringBuilder("[");

            for (int i = 0; i < bestPath.size(); i++) {
                if (i > 0) {
                    pathJson.append(",");
                }

                pathJson.append("\"")
                        .append(jsonEscape(bestPath.get(i)))
                        .append("\"");
            }

            pathJson.append("]");

            String result =
                    "{"
                    + "\"id\":" + nearest.id + ","
                    + "\"name\":\"" + jsonEscape(nearest.name) + "\","
                    + "\"location\":\"" + jsonEscape(nearest.location) + "\","
                    + "\"pickupLocation\":\"" + jsonEscape(pickupLocation) + "\","
                    + "\"distance\":" + minimumRoadDistance + ","
                    + "\"route\":" + pathJson
                    + "}";

            send(exchange, 200, result);

        } catch (Exception e) {
            send(exchange, 400,
                    "{\"error\":\"Invalid pickup coordinates\"}");
        }
    }

    static void staticFile(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        Path file = WEB_ROOT.resolve(path.substring(1)).normalize();

        if (!file.startsWith(WEB_ROOT) || !Files.isRegularFile(file)) {
            byte[] bytes = "Not found".getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(404, bytes.length);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }

            return;
        }

        String name = file.getFileName().toString();

        String type = name.endsWith(".css") ? "text/css"
                    : name.endsWith(".js") ? "application/javascript"
                    : "text/html";

        byte[] bytes = Files.readAllBytes(file);

        exchange.getResponseHeaders().set(
                "Content-Type", type + "; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void main(String[] args) throws Exception {

        drivers.clear();

        drivers.add(new Driver(1, "Arun", "Gandhipuram", 2, 3));
        drivers.add(new Driver(2, "Bala", "Peelamedu", 8, 7));
        drivers.add(new Driver(3, "Kumar", "RS Puram", 4, 5));

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0
        );

        server.createContext("/api/drivers",
                TaxiDispatchServer::driversApi);

        server.createContext("/api/assign",
                TaxiDispatchServer::assignApi);

        server.createContext("/",
                TaxiDispatchServer::staticFile);

        server.setExecutor(Executors.newCachedThreadPool());

        System.out.println(
                "TaxiGo server running at http://localhost:8080"
        );

        System.out.println(
                "Dijkstra shortest-path dispatch is enabled."
        );

        System.out.println(
                "Keep this window open while using the website."
        );

        server.start();
    }
}
