import java.util.List;

/**
 * Encapsulates a side-by-side comparison of the shortest-distance route
 * and the fastest-time route between two towns, including their
 * individual cost breakdowns and an overall recommendation.
 *
 * <p>Use {@link #getSummary()} to obtain a formatted comparison table
 * suitable for display.</p>
 */
public class RouteComparison {
    private final List<String> shortestPath;
    private final int shortestDistance;
    private final int shortestTime;
    private final double shortestFuelCost;
    private final double shortestTimeCost;
    private final double shortestTotalCost;

    private final List<String> fastestPath;
    private final int fastestDistance;
    private final int fastestTime;
    private final double fastestFuelCost;
    private final double fastestTimeCost;
    private final double fastestTotalCost;

    private final String recommendation;

    RouteComparison(
            List<String> shortestPath, int shortestDistance, int shortestTime,
            double shortestFuelCost, double shortestTimeCost, double shortestTotalCost,
            List<String> fastestPath, int fastestDistance, int fastestTime,
            double fastestFuelCost, double fastestTimeCost, double fastestTotalCost,
            String recommendation) {
        this.shortestPath = shortestPath;
        this.shortestDistance = shortestDistance;
        this.shortestTime = shortestTime;
        this.shortestFuelCost = shortestFuelCost;
        this.shortestTimeCost = shortestTimeCost;
        this.shortestTotalCost = shortestTotalCost;
        this.fastestPath = fastestPath;
        this.fastestDistance = fastestDistance;
        this.fastestTime = fastestTime;
        this.fastestFuelCost = fastestFuelCost;
        this.fastestTimeCost = fastestTimeCost;
        this.fastestTotalCost = fastestTotalCost;
        this.recommendation = recommendation;
    }

    public List<String> getShortestPath()    { return shortestPath; }
    public int getShortestDistance()          { return shortestDistance; }
    public int getShortestTime()             { return shortestTime; }
    public double getShortestFuelCost()      { return shortestFuelCost; }
    public double getShortestTimeCost()      { return shortestTimeCost; }
    public double getShortestTotalCost()     { return shortestTotalCost; }

    public List<String> getFastestPath()     { return fastestPath; }
    public int getFastestDistance()           { return fastestDistance; }
    public int getFastestTime()              { return fastestTime; }
    public double getFastestFuelCost()       { return fastestFuelCost; }
    public double getFastestTimeCost()       { return fastestTimeCost; }
    public double getFastestTotalCost()      { return fastestTotalCost; }

    /** @return {@code "shortest"}, {@code "fastest"}, or {@code "either"} */
    public String getRecommendation()        { return recommendation; }

    /**
     * Returns a formatted comparison table and recommendation string.
     *
     * @return multi-line summary suitable for printing
     */
    public String getSummary() {
        String fmt = "| %-18s | %-24s | %-24s |%n";
        String sep = "+" + "-".repeat(20) + "+" + "-".repeat(26) + "+" + "-".repeat(26) + "+\n";

        StringBuilder sb = new StringBuilder();
        sb.append(sep);
        sb.append(String.format(fmt, "Metric", "Shortest Route", "Fastest Route"));
        sb.append(sep);
        sb.append(String.format(fmt, "Path", shortestPath, fastestPath));
        sb.append(String.format(fmt, "Distance (km)", shortestDistance, fastestDistance));
        sb.append(String.format(fmt, "Time (min)", shortestTime, fastestTime));
        sb.append(String.format(fmt, "Fuel Cost (GHS)",
                String.format("%.2f", shortestFuelCost),
                String.format("%.2f", fastestFuelCost)));
        sb.append(String.format(fmt, "Time Cost (GHS)",
                String.format("%.2f", shortestTimeCost),
                String.format("%.2f", fastestTimeCost)));
        sb.append(String.format(fmt, "Total Cost (GHS)",
                String.format("%.2f", shortestTotalCost),
                String.format("%.2f", fastestTotalCost)));
        sb.append(sep);

        if ("either".equals(recommendation)) {
            sb.append("Recommendation: Either route — both have equal total cost (")
              .append(String.format("%.2f", shortestTotalCost)).append(" GHS).\n");
        } else {
            String winner = "shortest".equals(recommendation) ? "Shortest Route" : "Fastest Route";
            double winCost = "shortest".equals(recommendation) ? shortestTotalCost : fastestTotalCost;
            double loseCost = "shortest".equals(recommendation) ? fastestTotalCost : shortestTotalCost;
            sb.append("Recommendation: ").append(winner)
              .append(" — lower total cost (")
              .append(String.format("%.2f", winCost)).append(" GHS vs ")
              .append(String.format("%.2f", loseCost)).append(" GHS).\n");
        }

        return sb.toString();
    }
}
