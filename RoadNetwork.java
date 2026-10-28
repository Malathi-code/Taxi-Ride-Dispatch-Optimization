
import java.util.*;

public class RoadNetwork {

    private final Map<String, Map<String, Double>> graph =
            new LinkedHashMap<>();

    private final Map<String, double[]> coordinates =
            new LinkedHashMap<>();

    public RoadNetwork() {
        addLocation("Gandhipuram", 2, 3);
        addLocation("Peelamedu", 8, 7);
        addLocation("RS Puram", 4, 5);
        addLocation("Railway Station", 5, 2);

        addRoad("Gandhipuram", "RS Puram", 3.0);
        addRoad("Gandhipuram", "Peelamedu", 5.0);
        addRoad("Gandhipuram", "Railway Station", 4.0);
        addRoad("RS Puram", "Railway Station", 2.0);
        addRoad("RS Puram", "Peelamedu", 6.0);
        addRoad("Peelamedu", "Railway Station", 3.0);
    }

    private void addLocation(String name, double x, double y) {
        graph.put(name, new LinkedHashMap<>());
        coordinates.put(name, new double[]{x, y});
    }

    public void addRoad(String from, String to, double distance) {
        graph.get(from).put(to, distance);
        graph.get(to).put(from, distance);
    }

    public String nearestLocation(double x, double y) {
        String nearest = null;
        double minimum = Double.MAX_VALUE;

        for (String location : coordinates.keySet()) {
            double[] point = coordinates.get(location);

            double dx = x - point[0];
            double dy = y - point[1];
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < minimum) {
                minimum = distance;
                nearest = location;
            }
        }

        return nearest;
    }

    public List<String> shortestPath(String start, String end) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();

        PriorityQueue<String> queue = new PriorityQueue<>(
                Comparator.comparingDouble(
                        node -> distances.getOrDefault(node, Double.MAX_VALUE)
                )
        );

        for (String node : graph.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }

        distances.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(end)) {
                break;
            }

            for (Map.Entry<String, Double> edge :
                    graph.get(current).entrySet()) {

                String next = edge.getKey();

                double newDistance =
                        distances.get(current) + edge.getValue();

                if (newDistance < distances.get(next)) {
                    distances.put(next, newDistance);
                    previous.put(next, current);

                    queue.remove(next);
                    queue.add(next);
                }
            }
        }

        List<String> path = new ArrayList<>();

        if (distances.get(end) == Double.MAX_VALUE) {
            return path;
        }

        String current = end;

        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }

        Collections.reverse(path);
        return path;
    }

    public double shortestDistance(String start, String end) {
        List<String> path = shortestPath(start, end);

        if (path.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double total = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            total += graph.get(path.get(i)).get(path.get(i + 1));
        }

        return total;
    }

    public void displayNetwork() {
        System.out.println("\n--- ROAD NETWORK ---");

        for (String location : graph.keySet()) {
            System.out.println(location + " -> " + graph.get(location));
        }
    }

    public static void main(String[] args) {
        RoadNetwork network = new RoadNetwork();

        network.displayNetwork();

        String start = "Gandhipuram";
        String end = "Peelamedu";

        System.out.println("\n--- DIJKSTRA RESULT ---");
        System.out.println("Start: " + start);
        System.out.println("Destination: " + end);
        System.out.println("Path: " + network.shortestPath(start, end));
        System.out.println(
                "Shortest distance: "
                        + network.shortestDistance(start, end)
                        + " km"
        );
    }
}
