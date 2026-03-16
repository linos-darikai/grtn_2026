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
 * @see Town
 */
public class Ghana {

    /**
     * Master map of all towns in the network.
     * <p>
     * Key: <strong>lowercase</strong> town name (e.g. {@code "cape coast"}).<br>
     * Value: the corresponding {@link Town} instance, which stores the
     * original display name.
     * </p>
     */
    private HashMap<String, Town> towns;

    /**
     * Running count of directed edges (roads) loaded into the network.
     */
    private int edgeCount;

    /**
     * Constructs an empty {@code Ghana} network with no towns loaded.
     */
    public Ghana() {
        this.towns = new HashMap<>();
        this.edgeCount = 0;
    }

    /**
     * Normalizes a town name into a consistent lowercase key for map storage
     * and lookup.
     *
     * <p>
     * This ensures that {@code "Accra"}, {@code "accra"}, and
     * {@code " ACCRA "} all map to the same key {@code "accra"}.
     * </p>
     *
     * @param name the raw town name
     * @return the trimmed, lowercased key
     */
    private String normalizeKey(String name) {
        return name.trim().toLowerCase();
    }

    // -----------------------------------------------------------------------
    // Loading
    // -----------------------------------------------------------------------

    /**
     * Loads town and edge data from the specified file into the network.
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
    public void loadTowns(String filePath) throws IOException {
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
     * Town names are stored and looked up using {@link #normalizeKey(String)}
     * to guarantee case-insensitive matching. The first-encountered casing of
     * a town name becomes its display name in the {@link Town} object.
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
                    "] Skipping line — blank town name detected");
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

        towns.putIfAbsent(sourceKey, new Town(sourceRaw));
        towns.putIfAbsent(destKey, new Town(destRaw));

        Town sourceTown = towns.get(sourceKey);

        if (sourceTown.hasNeighbor(destKey)) {
            System.err.println("WARNING [" + filePath + ":" + lineNumber +
                    "] Duplicate directed edge " + sourceRaw + " -> " + destRaw +
                    " — overwriting previous values");
        } else {
            edgeCount++;
        }

        sourceTown.addNeighbor(destKey, distance, time);
    }

    /**
     * Completely removes a town from the network, including all outgoing edges
     * from it and all incoming edges to it.
     *
     * @param townName the name of the town to remove (case-insensitive)
     * @return true if the town was removed, false if it didn't exist
     */
    public boolean removeTown(String townName) {
        String key = normalizeKey(townName);
        if (!towns.containsKey(key)) {
            return false;
        }

        // 1. Remove the town itself and its outgoing edges
        Town removed = towns.remove(key);
        if (removed != null && removed.getNeighbors() != null) {
            edgeCount -= removed.getNeighbors().size();
        }

        // 2. Remove all incoming edges pointing to this town
        for (Town town : towns.values()) {
            if (town.getNeighbors() != null && town.getNeighbors().containsKey(key)) {
                town.getNeighbors().remove(key);
                edgeCount--;
            }
        }

        return true;
    }

    /**
     * Removes a directed edge between two towns.
     *
     * @param fromTown the origin town
     * @param toTown   the destination town
     * @return true if the edge was removed, false if it didn't exist
     */
    public boolean removeEdge(String fromTown, String toTown) {
        String fromKey = normalizeKey(fromTown);
        String toKey = normalizeKey(toTown);

        Town from = towns.get(fromKey);
        if (from != null && from.getNeighbors() != null && from.getNeighbors().containsKey(toKey)) {
            from.getNeighbors().remove(toKey);
            edgeCount--;
            return true;
        }

        return false;
    }

    /**
     * Helper to rewrite the graph file, excluding lines matching the condition.
     */
    private void deleteLinesFromFile(String filePath, java.util.function.Predicate<String[]> shouldDelete)
            throws IOException {
        java.io.File file = new java.io.File(filePath);
        if (!file.exists())
            return;

        boolean isCsv = filePath.endsWith(".csv");
        String separator = isCsv ? "," : ",\\s*";

        List<String> linesToKeep = new ArrayList<>();

        try (FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr)) {

            String line = br.readLine();
            if (line == null)
                return;

            // keep header for csv
            if (isCsv) {
                linesToKeep.add(line);
                line = br.readLine();
            }

            while (line != null) {
                if (line.isBlank()) {
                    linesToKeep.add(line);
                    line = br.readLine();
                    continue;
                }

                String[] parts = line.split(separator);
                if (parts.length >= 2 && !shouldDelete.test(parts)) {
                    linesToKeep.add(line);
                }
                line = br.readLine();
            }
        }

        try (java.io.FileWriter fw = new java.io.FileWriter(file);
                java.io.BufferedWriter bw = new java.io.BufferedWriter(fw)) {
            for (String l : linesToKeep) {
                bw.write(l);
                bw.newLine();
            }
        }
    }

    /**
     * Deletes all edges associated with a town from the given file.
     */
    public void deleteTownFromFile(String filePath, String townName) throws IOException {
        String key = normalizeKey(townName);
        deleteLinesFromFile(filePath, parts -> {
            String srcKey = normalizeKey(parts[0]);
            String dstKey = normalizeKey(parts[1]);
            return srcKey.equals(key) || dstKey.equals(key);
        });
    }

    /**
     * Deletes a specific directed edge from the given file.
     */
    public void deleteEdgeFromFile(String filePath, String fromTown, String toTown) throws IOException {
        String fKey = normalizeKey(fromTown);
        String tKey = normalizeKey(toTown);
        deleteLinesFromFile(filePath, parts -> {
            String srcKey = normalizeKey(parts[0]);
            String dstKey = normalizeKey(parts[1]);
            return srcKey.equals(fKey) && dstKey.equals(tKey);
        });
    }

    /**
     * Returns the full map of all towns in the network.
     *
     * @return a {@link HashMap} mapping normalized town names to {@link Town}
     *         objects; never {@code null}
     */
    public HashMap<String, Town> getTowns() {
        return towns;
    }

    /**
     * Retrieves a single {@link Town} by name (case-insensitive).
     *
     * @param name the name of the town; casing and leading/trailing whitespace
     *             are ignored
     * @return the {@link Town} instance, or {@code null} if no town with that
     *         name exists in the network
     */
    public Town getTown(String name) {
        return towns.get(normalizeKey(name));
    }

    /**
     * Returns the number of towns (vertices) in the network.
     *
     * @return the total town count
     */
    public int getTownCount() {
        return towns.size();
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
     * Returns the adjacency map for a given town — that is, all outgoing edges
     * from that town.
     *
     * @param townName the name of the town whose neighbors are requested
     * @return the neighbor map, or {@code null} if the town does not exist
     */
    public HashMap<String, int[]> getNeighbors(String townName) {
        Town town = towns.get(normalizeKey(townName));
        if (town == null) {
            return null;
        }
        return town.getNeighbors();
    }

    /**
     * Prints the total number of towns (vertices) in the network to
     */
    public void displayTotalTowns() {
        System.out.println("Total towns: " + towns.size());
    }

    /**
     * Prints the total number of directed roads (edges) in the network to
     */
    public void displayTotalEdges() {
        System.out.println("Total directed roads (edges): " + edgeCount);
    }

    /**
     * Computes the shortest distance between two towns in the network and
     * returns both the distance and the ordered path.
     *
     * <p>
     * Uses Dijkstra's algorithm over the distance weights (index 0 of
     * each edge's {@code int[]}). The returned {@link PathResult} contains
     * the total shortest distance and the list of town names (display
     * casing) traversed from origin to destination.
     * </p>
     *
     * @param fromTown the name of the origin town (case-insensitive)
     * @param toTown   the name of the destination town (case-insensitive)
     * @return a {@link PathResult} with the shortest distance and path, or
     *         {@code null} if no path exists or either town is not in the
     *         network
     */
    public PathResult getDistance(String fromTown, String toTown) {
        String startKey = normalizeKey(fromTown);
        String endKey = normalizeKey(toTown);

        if (!towns.containsKey(startKey) || !towns.containsKey(endKey)) {
            return null;
        }

        if (startKey.equals(endKey)) {
            List<String> path = new ArrayList<>();
            path.add(towns.get(startKey).getName());
            return new PathResult(0, path);
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

        for (String townKey : towns.keySet()) {
            distances.put(townKey, Integer.MAX_VALUE);
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

            Town currentTown = towns.get(currentKey);
            if (currentTown != null && currentTown.getNeighbors() != null) {
                for (Map.Entry<String, int[]> entry : currentTown.getNeighbors().entrySet()) {
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
            return null;
        }

        ArrayList<String> path = new ArrayList<>();
        String curr = endKey;
        while (curr != null) {
            path.add(towns.get(curr).getName());
            curr = previous.get(curr);
        }
        java.util.Collections.reverse(path);

        return new PathResult(finalDist, path);
    }

    /**
     * Computes the fastest travel time between two towns in the network and
     * returns both the time and the ordered path.
     *
     * <p>
     * Uses Dijkstra's algorithm over the <strong>average travel time</strong>
     * weights (index 1 of each edge's {@code int[]}). This may produce a
     * different route than {@link #getDistance(String, String)}, which
     * optimizes for distance instead.
     * </p>
     *
     * @param fromTown the name of the origin town (case-insensitive)
     * @param toTown   the name of the destination town (case-insensitive)
     * @return a {@link PathResult} with the fastest time (via
     *         {@link PathResult#getTime()}) and path, or {@code null} if
     *         no path exists or either town is not in the network
     */
    public PathResult getFastestTime(String fromTown, String toTown) {
        String startKey = normalizeKey(fromTown);
        String endKey = normalizeKey(toTown);

        if (!towns.containsKey(startKey) || !towns.containsKey(endKey)) {
            return null;
        }

        if (startKey.equals(endKey)) {
            List<String> path = new ArrayList<>();
            path.add(towns.get(startKey).getName());
            return new PathResult(0, path);
        }

        class Node implements Comparable<Node> {
            String name;
            int time;

            Node(String name, int time) {
                this.name = name;
                this.time = time;
            }

            @Override
            public int compareTo(Node other) {
                return Integer.compare(this.time, other.time);
            }
        }

        HashMap<String, Integer> times = new HashMap<>();
        HashMap<String, String> previous = new HashMap<>();

        for (String townKey : towns.keySet()) {
            times.put(townKey, Integer.MAX_VALUE);
        }
        times.put(startKey, 0);

        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(startKey, 0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            String currentKey = current.name;
            int currentTime = current.time;

            if (currentKey.equals(endKey)) {
                break;
            }

            if (currentTime > times.get(currentKey)) {
                continue;
            }

            Town currentTown = towns.get(currentKey);
            if (currentTown != null && currentTown.getNeighbors() != null) {
                for (Map.Entry<String, int[]> entry : currentTown.getNeighbors().entrySet()) {
                    String neighborKey = entry.getKey();
                    int edgeTime = entry.getValue()[1];
                    int newTime = currentTime + edgeTime;

                    if (times.containsKey(neighborKey) && newTime < times.get(neighborKey)) {
                        times.put(neighborKey, newTime);
                        previous.put(neighborKey, currentKey);
                        pq.add(new Node(neighborKey, newTime));
                    }
                }
            }
        }

        int finalTime = times.get(endKey);
        if (finalTime == Integer.MAX_VALUE) {
            return null;
        }

        ArrayList<String> path = new ArrayList<>();
        String curr = endKey;
        while (curr != null) {
            path.add(towns.get(curr).getName());
            curr = previous.get(curr);
        }
        java.util.Collections.reverse(path);

        return new PathResult(finalTime, path);
    }

    /** Fuel consumption rate: 8 kilometres per litre. */
    private static final int KM_PER_LITRE = 8;

    /** Fuel price in GHS per litre. */
    private static final double FUEL_PRICE_PER_LITRE = 11.955;

    /** Time cost in GHS per minute of travel. */
    private static final double TIME_COST_PER_MINUTE = 0.5;

    /**
     * Computes the shortest-distance path between two towns by delegating
     * to {@link #getDistance(String, String)}, then walks the returned path
     * to accumulate the travel time from each edge's time weight.
     *
     * <p>
     * Distance comes directly from the {@link PathResult}. Time is the
     * sum of the time weights (index 1 of each edge's {@code int[]}) along
     * the distance-optimal path, which may differ from the time-optimal
     * path.
     * </p>
     *
     * @param fromKey lowercase key of the origin town
     * @param toKey   lowercase key of the destination town
     * @return a two-element {@code int[]} where index 0 is total distance
     *         (km) and index 1 is total time (min), or {@code null} if no
     *         path exists or either key is not in the network
     */
    private int[] computeSegmentTotals(String fromKey, String toKey) {
        PathResult result = getDistance(fromKey, toKey);
        if (result == null) {
            return null;
        }

        List<String> path = result.getPath();
        int totalTime = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            String currentKey = normalizeKey(path.get(i));
            String nextKey = normalizeKey(path.get(i + 1));

            Town current = towns.get(currentKey);
            int[] edge = current.getNeighborEdge(nextKey);
            totalTime += edge[1];
        }

        return new int[] { result.getDistance(), totalTime };
    }

    /**
     * Walks a <strong>complete</strong> path (where every consecutive pair
     * shares a direct edge) and sums the distance and time from each edge.
     *
     * <p>
     * This is intended for paths produced by algorithms like Dijkstra
     * ({@link #getDistance}, {@link #getFastestTime}) where every hop is
     * guaranteed to be a direct edge. Unlike
     * {@link #computeRouteTotals(List)}, it does <em>not</em> re-optimise
     * each segment — it reads the actual edge weights as given.
     * </p>
     *
     * @param path an ordered list of town names (display casing) where each
     *             consecutive pair is connected by a direct directed edge
     * @return a two-element {@code int[]} where index 0 is total distance
     *         (km) and index 1 is total time (min), or {@code null} if a
     *         town or direct edge is missing
     */
    private int[] walkPathTotals(List<String> path) {
        int totalDistance = 0;
        int totalTime = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            String fromKey = normalizeKey(path.get(i));
            String toKey = normalizeKey(path.get(i + 1));

            Town from = towns.get(fromKey);
            if (from == null) {
                return null;
            }

            int[] edge = from.getNeighborEdge(toKey);
            if (edge == null) {
                return null;
            }

            totalDistance += edge[0];
            totalTime += edge[1];
        }

        return new int[] { totalDistance, totalTime };
    }

    /**
     * Walks a supplied route of waypoints and accumulates the total distance
     * and total travel time across all consecutive segments.
     *
     * <p>
     * For each pair of consecutive waypoints the method runs
     * {@link #computeSegmentTotals(String, String)} (Dijkstra by distance)
     * to obtain the shortest-distance path's distance and time. This means
     * a route like {@code ["A", "D"]} works even if {@code A} and {@code D}
     * do not share a direct edge — the algorithm will find the optimal
     * multi-hop path for that segment.
     * </p>
     *
     * <p>
     * This is the shared core used by both {@link #getFuelCost} and
     * {@link #getTotalCost}.
     * </p>
     *
     * @param route an ordered list of waypoint town names from origin to
     *              destination
     * @return a two-element {@code int[]} where index 0 is the total
     *         distance in km and index 1 is the total time in minutes, or
     *         {@code null} if the route is invalid (no path between a pair
     *         of consecutive waypoints, or a town does not exist)
     */
    private int[] computeRouteTotals(List<String> route) {
        int totalDistance = 0;
        int totalTime = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            String fromKey = normalizeKey(route.get(i));
            String toKey = normalizeKey(route.get(i + 1));

            int[] segment = computeSegmentTotals(fromKey, toKey);
            if (segment == null) {
                return null;
            }

            totalDistance += segment[0];
            totalTime += segment[1];
        }

        return new int[] { totalDistance, totalTime };
    }

    /**
     * Computes the fuel (distance) cost for a supplied route through the
     * network.
     *
     * <p>
     * The route is a list of <strong>waypoints</strong>. For each pair of
     * consecutive waypoints the shortest-distance path is computed via
     * Dijkstra (see {@link #computeSegmentTotals(String, String)}), so the
     * waypoints need not be direct neighbours.
     * </p>
     *
     * <pre>
     *   distance_cost = (total_distance_km / 8) &times; 11.955
     * </pre>
     *
     * <p>
     * where {@code /} is <strong>integer division</strong> (floor).
     * Constants used:
     * </p>
     * <ul>
     * <li>Vehicle fuel consumption: {@value #KM_PER_LITRE} km per litre</li>
     * <li>Fuel price: {@value #FUEL_PRICE_PER_LITRE} GHS per litre</li>
     * </ul>
     *
     * <p>
     * <strong>Edge cases:</strong>
     * </p>
     * <ul>
     * <li>A {@code null}, empty, or single-town route returns {@code 0.0}.</li>
     * <li>If no path exists between any consecutive pair of waypoints, or
     * a town is not in the network, returns {@code -1.0}.</li>
     * <li>Town names are matched case-insensitively.</li>
     * </ul>
     *
     * @param route an ordered list of waypoint town names from origin to
     *              destination
     * @return the fuel cost in GHS, or {@code -1.0} if the route is invalid
     */
    public double getFuelCost(List<String> route) {
        if (route == null || route.size() < 2) {
            return 0.0;
        }

        int[] totals = computeRouteTotals(route);
        if (totals == null) {
            return -1.0;
        }

        int litresNeeded = totals[0] / KM_PER_LITRE;
        return litresNeeded * FUEL_PRICE_PER_LITRE;
    }

    /**
     * Computes the total travel cost for a supplied route, combining fuel
     * cost and time cost.
     *
     * <p>
     * The route is a list of <strong>waypoints</strong>. For each pair of
     * consecutive waypoints the shortest-distance path is computed via
     * Dijkstra (see {@link #computeSegmentTotals(String, String)}), so the
     * waypoints need not be direct neighbours. Shares the same underlying
     * {@link #computeRouteTotals(List)} helper as {@link #getFuelCost}.
     * </p>
     *
     * <pre>
     *   distance_cost = (total_distance_km / 8) &times; 11.955
     *   time_cost     = total_time_min &times; 0.5
     *   total_cost    = distance_cost + time_cost
     * </pre>
     *
     * <p>
     * Constants used:
     * </p>
     * <ul>
     * <li>Vehicle fuel consumption: {@value #KM_PER_LITRE} km per litre</li>
     * <li>Fuel price: {@value #FUEL_PRICE_PER_LITRE} GHS per litre</li>
     * <li>Time cost: {@value #TIME_COST_PER_MINUTE} GHS per minute</li>
     * </ul>
     *
     * <p>
     * <strong>Edge cases:</strong>
     * </p>
     * <ul>
     * <li>A {@code null}, empty, or single-town route returns {@code 0.0}.</li>
     * <li>If no path exists between any consecutive pair of waypoints, or
     * a town is not in the network, returns {@code -1.0}.</li>
     * <li>Town names are matched case-insensitively.</li>
     * </ul>
     *
     * @param route an ordered list of waypoint town names from origin to
     *              destination
     * @return the total cost in GHS, or {@code -1.0} if the route is invalid
     */
    public double getTotalCost(List<String> route) {
        if (route == null || route.size() < 2) {
            return 0.0;
        }

        int[] totals = computeRouteTotals(route);
        if (totals == null) {
            return -1.0;
        }

        double distanceCost = (totals[0] / KM_PER_LITRE) * FUEL_PRICE_PER_LITRE;
        double timeCost = totals[1] * TIME_COST_PER_MINUTE;
        return distanceCost + timeCost;
    }

    /**
     * Recommends the best route between two towns by comparing the
     * shortest-distance route and the fastest-time route on total cost.
     *
     * <p>
     * Internally calls {@link #getDistance(String, String)} and
     * {@link #getFastestTime(String, String)} to obtain the two candidate
     * paths, then uses {@link #computeRouteTotals(List)} to derive
     * distance, time, fuel cost, time cost, and total cost for each.
     * </p>
     *
     * <p>
     * The route with the <strong>lower total cost</strong> is
     * recommended. If both costs are equal the recommendation is
     * {@code "either"}.
     * </p>
     *
     * @param fromTown the origin town (case-insensitive)
     * @param toTown   the destination town (case-insensitive)
     * @return a {@link RouteComparison} containing the full breakdown and
     *         recommendation, or {@code null} if no path exists between
     *         the towns or either town is not in the network
     */
    public RouteComparison recommendRoute(String fromTown, String toTown) {
        PathResult shortestResult = getDistance(fromTown, toTown);
        PathResult fastestResult = getFastestTime(fromTown, toTown);

        if (shortestResult == null || fastestResult == null) {
            return null;
        }

        int[] sTotals = walkPathTotals(shortestResult.getPath());
        int[] fTotals = walkPathTotals(fastestResult.getPath());

        if (sTotals == null || fTotals == null) {
            return null;
        }

        double sFuelCost = (sTotals[0] / KM_PER_LITRE) * FUEL_PRICE_PER_LITRE;
        double sTimeCost = sTotals[1] * TIME_COST_PER_MINUTE;
        double sTotalCost = sFuelCost + sTimeCost;

        double fFuelCost = (fTotals[0] / KM_PER_LITRE) * FUEL_PRICE_PER_LITRE;
        double fTimeCost = fTotals[1] * TIME_COST_PER_MINUTE;
        double fTotalCost = fFuelCost + fTimeCost;

        String rec;
        if (Math.abs(sTotalCost - fTotalCost) < 0.001) {
            rec = "either";
        } else if (sTotalCost < fTotalCost) {
            rec = "shortest";
        } else {
            rec = "fastest";
        }

        return new RouteComparison(
                shortestResult.getPath(), sTotals[0], sTotals[1],
                sFuelCost, sTimeCost, sTotalCost,
                fastestResult.getPath(), fTotals[0], fTotals[1],
                fFuelCost, fTimeCost, fTotalCost,
                rec);
    }

    /**
     * Finds the top 3 paths with lowest total cost between two towns.
     *
     * <p>
     * Uses a priority queue exploration to find multiple distinct paths,
     * then ranks them by total cost (fuel + time).
     * </p>
     *
     * @param fromTown the name of the origin town (case-insensitive)
     * @param toTown   the name of the destination town (case-insensitive)
     * @return a {@link List} of {@link PathWithCost} objects containing
     *         path and cost details, sorted by total cost (lowest first)
     */
    public List<PathWithCost> getTop3PathsByTotalCost(String fromTown, String toTown) {
        String startKey = normalizeKey(fromTown);
        String endKey = normalizeKey(toTown);

        List<PathWithCost> result = new ArrayList<>();

        if (!towns.containsKey(startKey) || !towns.containsKey(endKey)) {
            return result;
        }

        class PathNode implements Comparable<PathNode> {
            String currentTownKey;
            int dist;
            List<String> pathKeys;

            PathNode(String currentTownKey, int dist, List<String> pathKeys) {
                this.currentTownKey = currentTownKey;
                this.dist = dist;
                this.pathKeys = pathKeys;
            }

            @Override
            public int compareTo(PathNode other) {
                return Integer.compare(this.dist, other.dist);
            }
        }

        PriorityQueue<PathNode> pq = new PriorityQueue<>();
        List<String> initKeys = new ArrayList<>();
        initKeys.add(startKey);

        pq.add(new PathNode(startKey, 0, initKeys));

        List<List<String>> foundPaths = new ArrayList<>();
        int maxPaths = 50; // Explore more paths to find best by cost

        while (!pq.isEmpty() && foundPaths.size() < maxPaths) {
            PathNode current = pq.poll();
            String u = current.currentTownKey;

            if (u.equals(endKey)) {
                List<String> names = new ArrayList<>();
                for (String key : current.pathKeys) {
                    names.add(towns.get(key).getName());
                }
                foundPaths.add(names);
                continue;
            }

            Town currentTown = towns.get(u);
            if (currentTown != null && currentTown.getNeighbors() != null) {
                for (Map.Entry<String, int[]> entry : currentTown.getNeighbors().entrySet()) {
                    String v = entry.getKey();
                    int weight = entry.getValue()[0];

                    if (current.pathKeys.contains(v)) {
                        continue;
                    }

                    List<String> newPathKeys = new ArrayList<>(current.pathKeys);
                    newPathKeys.add(v);

                    pq.add(new PathNode(v, current.dist + weight, newPathKeys));
                }
            }
        }

        // Now calculate total cost for each path and sort
        class PathCostPair implements Comparable<PathCostPair> {
            List<String> path;
            double totalCost;
            int distance;
            int time;
            double fuelCost;
            double timeCost;

            PathCostPair(List<String> path, int distance, int time, double fuelCost, double timeCost, double totalCost) {
                this.path = path;
                this.distance = distance;
                this.time = time;
                this.fuelCost = fuelCost;
                this.timeCost = timeCost;
                this.totalCost = totalCost;
            }

            @Override
            public int compareTo(PathCostPair other) {
                return Double.compare(this.totalCost, other.totalCost);
            }
        }

        List<PathCostPair> pathsWithCost = new ArrayList<>();

        for (List<String> path : foundPaths) {
            int[] totals = walkPathTotals(path);
            if (totals != null) {
                double fuelCost = (totals[0] / KM_PER_LITRE) * FUEL_PRICE_PER_LITRE;
                double timeCost = totals[1] * TIME_COST_PER_MINUTE;
                double totalCost = fuelCost + timeCost;
                pathsWithCost.add(new PathCostPair(path, totals[0], totals[1], fuelCost, timeCost, totalCost));
            }
        }

        pathsWithCost.sort(PathCostPair::compareTo);

        // Return top 3
        int count = Math.min(3, pathsWithCost.size());
        for (int i = 0; i < count; i++) {
            PathCostPair p = pathsWithCost.get(i);
            result.add(new PathWithCost(p.path, p.distance, p.time, p.fuelCost, p.timeCost, p.totalCost));
        }

        return result;
    }

    /**
     * Finds the top 3 shortest paths between two towns, ranked by total
     * distance.
     *
     * <p>
     * <strong>Not yet implemented.</strong> Will use a k-shortest-paths
     * algorithm (e.g. Yen's algorithm) to return up to three distinct
     * routes.
     * </p>
     *
     * @param fromTown the name of the origin town (case-insensitive)
     * @param toTown   the name of the destination town (case-insensitive)
     * @return a {@link List} of up to 3 paths, each path being an ordered
     *         {@link List} of town names from origin to destination; fewer
     *         than 3 entries are returned if fewer distinct paths exist
     */
    public List<List<String>> getTop3ShortestPaths(String fromTown, String toTown) {
        String startKey = normalizeKey(fromTown);
        String endKey = normalizeKey(toTown);

        List<List<String>> top3Paths = new ArrayList<>();

        if (!towns.containsKey(startKey) || !towns.containsKey(endKey)) {
            return top3Paths;
        }

        class PathNode implements Comparable<PathNode> {
            String currentTownKey;
            int dist;
            List<String> pathKeys;

            PathNode(String currentTownKey, int dist, List<String> pathKeys) {
                this.currentTownKey = currentTownKey;
                this.dist = dist;
                this.pathKeys = pathKeys;
            }

            @Override
            public int compareTo(PathNode other) {
                return Integer.compare(this.dist, other.dist);
            }
        }

        PriorityQueue<PathNode> pq = new PriorityQueue<>();
        List<String> initKeys = new ArrayList<>();
        initKeys.add(startKey);

        pq.add(new PathNode(startKey, 0, initKeys));

        while (!pq.isEmpty()) {
            PathNode current = pq.poll();
            String u = current.currentTownKey;

            if (u.equals(endKey)) {
                List<String> names = new ArrayList<>();
                for (String key : current.pathKeys) {
                    names.add(towns.get(key).getName());
                }
                top3Paths.add(names);

                if (top3Paths.size() == 3) {
                    break;
                }
                continue;
            }

            Town currentTown = towns.get(u);
            if (currentTown != null && currentTown.getNeighbors() != null) {
                for (Map.Entry<String, int[]> entry : currentTown.getNeighbors().entrySet()) {
                    String v = entry.getKey();
                    int weight = entry.getValue()[0];

                    if (current.pathKeys.contains(v)) {
                        continue;
                    }

                    List<String> newPathKeys = new ArrayList<>(current.pathKeys);
                    newPathKeys.add(v);

                    pq.add(new PathNode(v, current.dist + weight, newPathKeys));
                }
            }
        }

        return top3Paths;
    }

    /**
     * Returns a summary string showing the total number of towns and edges
     * in the network.
     *
     * @return a string in the format {@code Ghana{towns=N, edges=M}}
     */
    @Override
    public String toString() {
        return "Ghana{towns=" + towns.size() + ", edges=" + edgeCount + "}";
    }
}
