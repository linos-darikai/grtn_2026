import java.util.List;

/**
 * Encapsulates the result of a path computation — either shortest
 * distance or fastest time.
 *
 * <p>The numeric {@code value} represents <strong>distance in km</strong>
 * when returned by {@link Ghana#getDistance(String, String)}, or
 * <strong>time in minutes</strong> when returned by
 * {@link Ghana#getFastestTime(String, String)}. Use the semantically
 * appropriate accessor ({@link #getDistance()} or {@link #getTime()})
 * for clarity.</p>
 */
public class PathResult {
    private final int value;
    private final List<String> path;

    PathResult(int value, List<String> path) {
        this.value = value;
        this.path = path;
    }

    /** @return the total shortest distance in kilometres */
    public int getDistance() {
        return value;
    }

    /** @return the fastest travel time in minutes */
    public int getTime() {
        return value;
    }

    /** @return the ordered list of town names from origin to destination */
    public List<String> getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "PathResult{value=" + value + ", path=" + path + "}";
    }
}
