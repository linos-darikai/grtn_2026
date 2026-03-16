import java.util.HashMap;

/**
 * Represents a town (vertex) in the Ghana Road Transport Network directed
 * graph.
 *
 * <p>
 * Each {@code Town} holds a name and a map of outgoing edges to neighboring
 * towns.
 * Every edge carries two weights: the road distance in kilometres and the
 * average
 * travel time in minutes. This mirrors the structure of the dataset in
 * {@code ghana_directed_graph_2026.csv} where each row defines a directed edge
 * {@code (source, destination, distance_km, avg_time_min)}.
 * </p>
 *
 * <p>
 * The neighbors map uses the neighbor town's name as the key and a two-element
 * {@code int[]} as the value:
 * </p>
 * <ul>
 * <li>Index 0 — distance in kilometres</li>
 * <li>Index 1 — average travel time in minutes</li>
 * </ul>
 *
 * <p>
 * <strong>Example usage:</strong>
 * </p>
 * 
 * <pre>{@code
 * Town accra = new Town("Accra");
 * accra.addNeighbor("Tema", 316, 307);
 * accra.addNeighbor("Osu", 82, 126);
 *
 * if (accra.hasNeighbor("Tema")) {
 *     int[] edge = accra.getNeighborEdge("Tema");
 *     System.out.println("Distance: " + edge[0] + " km, Time: " + edge[1] + " min");
 * }
 * }</pre>
 *
 * @author Stanley and Linos 2026
 * @version 1.0
 */
public class Town {

    private String name;

    private HashMap<String, int[]> neighbors;

    /**
     * Constructs a new {@code Town} with the given name and an empty neighbor map.
     *
     * @param name the name of the town; must not be {@code null}
     */
    public Town(String name) {
        this.name = name;
        this.neighbors = new HashMap<>();
    }

    /**
     * Returns the name of this town.
     *
     * @return the town name
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the name of this town.
     *
     * @param name the new town name;
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     *
     * @return the neighbors map;
     */
    public HashMap<String, int[]> getNeighbors() {
        return neighbors;
    }

    /**
     * @param neighbors the new neighbors map to set;
     */
    public void setNeighbors(HashMap<String, int[]> neighbors) {
        this.neighbors = neighbors;
    }

    /**
     * Adds a directed edge from this town to the specified neighbor.
     *
     * <p>
     * If an edge to the same neighbor already exists, it is overwritten
     * with the new distance and time values.
     * </p>
     *
     * @param neighborName the name of the destination town
     * @param distanceKm   the road distance to the neighbor in kilometres
     * @param avgTimeMin   the average travel time to the neighbor in minutes
     */
    public void addNeighbor(String neighborName, int distanceKm, int avgTimeMin) {
        this.neighbors.put(neighborName, new int[] { distanceKm, avgTimeMin });
    }

    /**
     * Removes the directed edge from this town to the specified neighbor.
     *
     * <p>
     * If no such edge exists, this method does nothing.
     * </p>
     *
     * @param neighborName the name of the neighbor town to disconnect
     */
    public void removeNeighbor(String neighborName) {
        this.neighbors.remove(neighborName);
    }

    /**
     * Retrieves the edge data for a specific neighbor.
     *
     * @param neighborName the name of the neighbor town to look up
     * @return a two-element {@code int[]} where index 0 is the distance in
     *         kilometres and index 1 is the average time in minutes, or
     *         {@code null} if no edge to that neighbor exists
     */
    public int[] getNeighborEdge(String neighborName) {
        return this.neighbors.get(neighborName);
    }

    /**
     * Checks whether a directed edge exists from this town to the given neighbor.
     *
     * @param neighborName the name of the town to check
     * @return {@code true} if an edge to the neighbor exists, {@code false}
     *         otherwise
     */
    public boolean hasNeighbor(String neighborName) {
        return this.neighbors.containsKey(neighborName);
    }

    /**
     * Returns a concise string representation of this town, including the
     * town name and the number of outgoing edges.
     *
     * @return a string in the format {@code Town{name='X', neighbors=N}}
     */
    @Override
    public String toString() {
        return "Town{name='" + name + "', neighbors=" + neighbors.size() + "}";
    }
}
