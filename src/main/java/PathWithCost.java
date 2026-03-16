import java.util.List;

/**
 * Represents a path through the network along with its associated costs.
 *
 * @author Stanley and Linos 2026
 */
public class PathWithCost {
    private final List<String> path;
    private final int distance;
    private final int time;
    private final double fuelCost;
    private final double timeCost;
    private final double totalCost;

    /**
     * Constructs a PathWithCost with the given path and cost details.
     *
     * @param path      ordered list of town names from origin to destination
     * @param distance  total distance in kilometers
     * @param time      total time in minutes
     * @param fuelCost  fuel cost in GHS
     * @param timeCost  time cost in GHS
     * @param totalCost total cost (fuel + time) in GHS
     */
    public PathWithCost(List<String> path, int distance, int time,
                        double fuelCost, double timeCost, double totalCost) {
        this.path = path;
        this.distance = distance;
        this.time = time;
        this.fuelCost = fuelCost;
        this.timeCost = timeCost;
        this.totalCost = totalCost;
    }

    public List<String> getPath() {
        return path;
    }

    public int getDistance() {
        return distance;
    }

    public int getTime() {
        return time;
    }

    public double getFuelCost() {
        return fuelCost;
    }

    public double getTimeCost() {
        return timeCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    /**
     * Returns a formatted string representation of the path.
     *
     * @return path as "Town1 → Town2 → Town3"
     */
    public String getPathString() {
        return String.join(" → ", path);
    }

    @Override
    public String toString() {
        return String.format("PathWithCost{path=%s, distance=%dkm, time=%dmin, totalCost=%.2f GHS}",
                getPathString(), distance, time, totalCost);
    }
}
