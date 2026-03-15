import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * 
 * @author Stanley and Linos 2026
 * @version 1.1
 * @see City
 */
public class Ghana {

    /**
     * Master map of all cities in the network.
     * <p>
     * Key: <strong>lowercase</strong> city name (e.g. {@code "cape coast"}).<br>
     * Value: the corresponding {@link City} instance, which stores the
     * original display name.
     * </p>
     */
    private HashMap<String, City> cities;

    /**
     * Running count of directed edges (roads) loaded into the network.
     */
    private int edgeCount;

    /**
     * Constructs an empty {@code Ghana} network with no cities loaded.
     */
    public Ghana() {
        this.cities = new HashMap<>();
        this.edgeCount = 0;
    }

    /**
     * Normalizes a city name into a consistent lowercase key for map storage
     * and lookup.
     *
     * <p>
     * This ensures that {@code "Accra"}, {@code "accra"}, and
     * {@code " ACCRA "} all map to the same key {@code "accra"}.
     * </p>
     *
     * @param name the raw city name
     * @return the trimmed, lowercased key
     */
    private String normalizeKey(String name) {
        return name.trim().toLowerCase();
    }

    // -----------------------------------------------------------------------
    // Loading
    // -----------------------------------------------------------------------

    /**
     * Loads city and edge data from the specified file into the network.
     *
     * <p>
     * The method inspects the file extension to choose the correct parser:
     * </p>
     * <ul>
     * <li>{@code .csv} — expects a header row followed by comma-separated
     * data rows ({@code source,destination,distance_km,avg_time_min})</li>
     * <li>{@code .txt} — expects comma-and-space-separated data rows with
     * no header ({@code source, destination, distance_km, avg_time_min})</li>
     * </ul>
     *
     * <p>
     * All I/O resources are managed with try-with-resources to prevent
     * leaks. Malformed or incomplete lines are skipped with a warning printed
     * to {@code System.err}.
     * </p>
     *
     * @param filePath the path to the {@code .csv} or {@code .txt} data file
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the file extension is unsupported
     */
    public void loadCities(String filePath) throws IOException {
        if (filePath.endsWith(".csv")) {
            loadFromCsv(filePath);
        } else if (filePath.endsWith(".txt")) {
            loadFromTxt(filePath);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + filePath +
                            ". Expected .csv or .txt");
        }
    }

    /**
     * Parses a CSV file with a header row.
     *
     * <p>
     * Expected format per data row:
     * {@code source,destination,distance_km,avg_time_min}
     * </p>
     *
     * @param filePath path to the CSV file
     * @throws IOException if the file cannot be read
     */
    private void loadFromCsv(String filePath) throws IOException {
        try (FileReader fileReader = new FileReader(filePath);
                BufferedReader reader = new BufferedReader(fileReader)) {
            reader.readLine(); // skip header
            String line;
            int lineNumber = 1; // header was line 1
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank())
                    continue;
                String[] parts = line.split(",");
                processEdge(parts, filePath, lineNumber);
            }
        }
    }

    /**
     * Parses a TXT file with no header row.
     *
     * <p>
     * Expected format per row:
     * {@code source, destination, distance_km, avg_time_min}
     * (comma optionally followed by whitespace).
     * </p>
     *
     * @param filePath path to the TXT file
     * @throws IOException if the file cannot be read
     */
    private void loadFromTxt(String filePath) throws IOException {
        try (FileReader fileReader = new FileReader(filePath);
                BufferedReader reader = new BufferedReader(fileReader)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank())
                    continue;
                String[] parts = line.split(",\\s*");
                processEdge(parts, filePath, lineNumber);
            }
        }
    }

    /**
     * Validates and processes a single parsed directed edge, registering it in
     * the graph.
     *
     * <p>
     * Each valid line produces exactly one directed edge
     * from source to destination. No reverse edge is created,the graph
     * treats {@code A → B} and {@code B → A} as independent edges that must
     * each appear as their own row in the dataset.
     * </p>
     *
     * <p>
     * Validation checks performed before insertion:
     * </p>
     * <ol>
     * <li>The line must have at least 4 columns.</li>
     * <li>Both source and destination names must be non-blank after trimming.</li>
     * <li>Distance and time columns must be valid non-negative integers.</li>
     * </ol>
     *
     * <p>
     * If any check fails, a descriptive warning is printed to
     * {@code System.err} and the line is skipped — no exception is thrown so
     * that loading continues for remaining lines.
     * </p>
     *
     * <p>
     * If a directed edge from source to destination already exists (e.g.
     * from a previous file load), the edge data is overwritten with the new
     * values and a warning is printed. The {@link #edgeCount} is only
     * incremented for genuinely new edges to keep the count accurate.
     * </p>
     *
     * <p>
     * City names are stored and looked up using {@link #normalizeKey(String)}
     * to guarantee case-insensitive matching. The first-encountered casing of
     * a city name becomes its display name in the {@link City} object.
     * </p>
     *
     * @param parts      the split columns from one data line
     * @param filePath   the file being loaded (for warning context)
     * @param lineNumber the 1-based line number (for warning context)
     */
    private void processEdge(String[] parts, String filePath, int lineNumber) {
        if (parts.length < 4) {
            System.err.println("WARNING [" + filePath + ":" + lineNumber +
                    "] Skipping line — expected 4 columns, found " + parts.length);
            return;
        }

        String sourceRaw = parts[0].trim();
        String destRaw = parts[1].trim();

        if (sourceRaw.isEmpty() || destRaw.isEmpty()) {
            System.err.println("WARNING [" + filePath + ":" + lineNumber +
                    "] Skipping line — blank city name detected");
            return;
        }

        int distance;
        int time;
        try {
            distance = Integer.parseInt(parts[2].trim());
            time = Integer.parseInt(parts[3].trim());
        } catch (NumberFormatException e) {
            System.err.println("WARNING [" + filePath + ":" + lineNumber +
                    "] Skipping line — non-numeric distance or time value");
            return;
        }

        if (distance < 0 || time < 0) {
            System.err.println("WARNING [" + filePath + ":" + lineNumber +
                    "] Skipping line — negative distance or time value");
            return;
        }

        String sourceKey = normalizeKey(sourceRaw);
        String destKey = normalizeKey(destRaw);

        cities.putIfAbsent(sourceKey, new City(sourceRaw));
        cities.putIfAbsent(destKey, new City(destRaw));

        City sourceCity = cities.get(sourceKey);

        if (sourceCity.hasNeighbor(destKey)) {
            System.err.println("WARNING [" + filePath + ":" + lineNumber +
                    "] Duplicate directed edge " + sourceRaw + " -> " + destRaw +
                    " — overwriting previous values");
        } else {
            edgeCount++;
        }

        sourceCity.addNeighbor(destKey, distance, time);
    }

    /**
     * Returns the full map of all cities in the network.
     *
     * @return a {@link HashMap} mapping normalized city names to {@link City}
     *         objects; never {@code null}
     */
    public HashMap<String, City> getCities() {
        return cities;
    }

    /**
     * Retrieves a single {@link City} by name (case-insensitive).
     *
     * @param name the name of the city; casing and leading/trailing whitespace
     *             are ignored
     * @return the {@link City} instance, or {@code null} if no city with that
     *         name exists in the network
     */
    public City getCity(String name) {
        return cities.get(normalizeKey(name));
    }

    /**
     * Returns the number of cities (vertices) in the network.
     *
     * @return the total city count
     */
    public int getCityCount() {
        return cities.size();
    }

    /**
     * Returns the total number of directed edges (roads) in the network.
     *
     * @return the total edge count
     */
    public int getEdgeCount() {
        return edgeCount;
    }

    /**
     * Returns the adjacency map for a given city — that is, all outgoing edges
     * from that city.
     *
     * @param cityName the name of the city whose neighbors are requested
     * @return the neighbor map, or {@code null} if the city does not exist
     */
    public HashMap<String, int[]> getNeighbors(String cityName) {
        City city = cities.get(normalizeKey(cityName));
        if (city == null) {
            return null;
        }
        return city.getNeighbors();
    }

    /**
     * Prints the total number of cities (vertices) in the network to
     */
    public void displayTotalCities() {
        System.out.println("Total cities: " + cities.size());
    }

    /**
     * Prints the total number of directed roads (edges) in the network to
     */
    public void displayTotalEdges() {
        System.out.println("Total directed roads (edges): " + edgeCount);
    }

    /**
     * Computes the shortest distance between two cities in the network.
     *
     * <p>
     * Uses a shortest-path algorithm (Dijkstra) over the distance weights.
     * </p>
     *
     * @param fromCity the name of the origin city (case-insensitive)
     * @param toCity   the name of the destination city (case-insensitive)
     * @return the shortest distance in kilometres, or {@code -1} if no path
     *         exists
     */
    public int getDistance(String fromCity, String toCity) {
        String startKey = normalizeKey(fromCity);
        String endKey = normalizeKey(toCity);

        if (!cities.containsKey(startKey) || !cities.containsKey(endKey)) {
            return -1;
        }

        class Node implements Comparable<Node> {
            String name;
            int dist;

            Node(String name, int dist) {
                this.name = name;
                this.dist = dist;
            }

            @Override
            public int compareTo(Node other) {
                return Integer.compare(this.dist, other.dist);
            }
        }

        HashMap<String, Integer> distances = new HashMap<>();
        HashMap<String, String> previous = new HashMap<>();

        for (String cityKey : cities.keySet()) {
            distances.put(cityKey, Integer.MAX_VALUE);
        }
        distances.put(startKey, 0);

        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(startKey, 0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            String currentKey = current.name;
            int currentDist = current.dist;

            if (currentKey.equals(endKey)) {
                break;
            }

            if (currentDist > distances.get(currentKey)) {
                continue;
            }

            City currentCity = cities.get(currentKey);
            if (currentCity != null && currentCity.getNeighbors() != null) {
                for (Map.Entry<String, int[]> entry : currentCity.getNeighbors().entrySet()) {
                    String neighborKey = entry.getKey();
                    int edgeDistance = entry.getValue()[0];
                    int newDist = currentDist + edgeDistance;

                    if (distances.containsKey(neighborKey) && newDist < distances.get(neighborKey)) {
                        distances.put(neighborKey, newDist);
                        previous.put(neighborKey, currentKey);
                        pq.add(new Node(neighborKey, newDist));
                    }
                }
            }
        }

        int finalDist = distances.get(endKey);
        if (finalDist == Integer.MAX_VALUE) {
            return -1;
        }

        ArrayList<String> path = new ArrayList<>();
        String curr = endKey;
        while (curr != null) {
            path.add(cities.get(curr).getName());
            curr = previous.get(curr);
        }
        java.util.Collections.reverse(path);

        System.out.println("Path: " + path);

        return finalDist;
    }

    /**
     * Finds the top 3 shortest paths between two cities, ranked by total
     * distance.
     *
     * <p>
     * <strong>Not yet implemented.</strong> Will use a k-shortest-paths
     * algorithm (e.g. Yen's algorithm) to return up to three distinct
     * routes.
     * </p>
     *
     * @param fromCity the name of the origin city (case-insensitive)
     * @param toCity   the name of the destination city (case-insensitive)
     * @return a {@link List} of up to 3 paths, each path being an ordered
     *         {@link List} of city names from origin to destination; fewer
     *         than 3 entries are returned if fewer distinct paths exist
     */
    public List<List<String>> getTop3ShortestPaths(String fromCity, String toCity) {
        // TODO: implement k-shortest-paths (k=3)
        return new ArrayList<>();
    }

    /**
     * Returns a summary string showing the total number of cities and edges
     * in the network.
     *
     * @return a string in the format {@code Ghana{cities=N, edges=M}}
     */
    @Override
    public String toString() {
        return "Ghana{cities=" + cities.size() + ", edges=" + edgeCount + "}";
    }
}
