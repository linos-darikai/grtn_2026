import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Unit tests for the {@link Town} and {@link Ghana} classes.
 *
 * <p>Covers three categories:</p>
 * <ol>
 *   <li><strong>Town creation</strong> — constructor, getters, setters,
 *       neighbor operations, edge overwrites, removals.</li>
 *   <li><strong>Data loading</strong> — CSV and TXT parsing, blank lines,
 *       missing columns, blank names, non-numeric values, negative values,
 *       case-insensitive deduplication, duplicate edge counting.</li>
 *   <li><strong>Shortest distance (Dijkstra)</strong> — direct neighbor,
 *       multi-hop, unreachable towns, non-existent towns, directed-only
 *       traversal, same-origin-and-destination.</li>
 * </ol>
 *
 * <p>All test data files are created as temp files and deleted on JVM exit,
 * leaving no artefacts on disk.</p>
 *
 * @author Stanley and Linos 2026
 * @version 1.0
 */
public class GhanaTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        System.out.println("=== Town Tests ===");
        testTownConstructor();
        testTownGettersSetters();
        testTownAddNeighbor();
        testTownOverwriteNeighbor();
        testTownRemoveNeighbor();
        testTownRemoveNonExistentNeighbor();
        testTownGetNeighborEdgeNonExistent();
        testTownHasNeighbor();
        testTownSetNeighborsReplacesMap();
        testTownToString();

        System.out.println("\n=== Ghana Loading Tests ===");
        testLoadFromCsv();
        testLoadFromTxt();
        testLoadUnsupportedFileType();
        testLoadBlankLines();
        testLoadMissingColumns();
        testLoadBlankTownName();
        testLoadNonNumericValues();
        testLoadNegativeValues();
        testLoadCaseInsensitive();
        testLoadDuplicateEdgeCount();
        testLoadBothFormatsAccumulate();

        System.out.println("\n=== Dijkstra / getDistance Tests ===");
        testDistanceDirectNeighbor();
        testDistanceMultiHop();
        testDistanceNoPath();
        testDistanceNonExistentSource();
        testDistanceNonExistentDestination();
        testDistanceSameTown();
        testDistanceDirectedOnly();
        testDistanceCaseInsensitiveLookup();
        testDistanceChoosesShortestRoute();

        System.out.println("\n=== getFastestTime Tests ===");
        testFastestTimeDirectNeighbor();
        testFastestTimeMultiHop();
        testFastestTimeDiffersFromDistance();
        testFastestTimeNoPath();
        testFastestTimeNonExistentSource();
        testFastestTimeNonExistentDestination();
        testFastestTimeSameTown();
        testFastestTimeDirectedOnly();
        testFastestTimeCaseInsensitive();
        testFastestTimeChoosesFastestRoute();

        System.out.println("\n=== getFuelCost Tests ===");
        testFuelCostDirectNeighbor();
        testFuelCostMultiHopRoute();
        testFuelCostIntegerDivisionTruncates();
        testFuelCostDistanceLessThanOneLitre();
        testFuelCostEmptyRoute();
        testFuelCostSingleTownRoute();
        testFuelCostNullRoute();
        testFuelCostNoPathInRoute();
        testFuelCostNonExistentTown();
        testFuelCostCaseInsensitive();
        testFuelCostNoDirectEdgeUsesShortestPath();
        testFuelCostUsesShortestNotDirect();
        testFuelCostZeroDistanceEdge();

        System.out.println("\n=== getTotalCost Tests ===");
        testTotalCostSingleHop();
        testTotalCostMultiHop();
        testTotalCostBreakdown();
        testTotalCostEmptyRoute();
        testTotalCostSingleTown();
        testTotalCostNullRoute();
        testTotalCostNoEdge();
        testTotalCostNonExistentTown();
        testTotalCostCaseInsensitive();
        testTotalCostNoDirectEdgeUsesShortestPath();
        testTotalCostUsesShortestNotDirect();
        testTotalCostZeroDistanceAndTime();

        System.out.println("\n=== recommendRoute Tests ===");
        testRecommendShortestWins();
        testRecommendFastestWins();
        testRecommendSameRoute();
        testRecommendSameTown();
        testRecommendNoPath();
        testRecommendNonExistentTown();
        testRecommendCaseInsensitive();
        testRecommendSummaryContent();

        System.out.println("\n=== Top 3 Shortest Paths Tests ===");
        testTop3ShortestPathsStandard();
        testTop3ShortestPathsFewerThan3();
        testTop3ShortestPathsNoPath();

        System.out.println("\n=== Results ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));

        if (failed > 0) {
            System.exit(1);
        }
    }

    // -----------------------------------------------------------------------
    //  Assertion helpers
    // -----------------------------------------------------------------------

    private static void assertEqual(Object expected, Object actual, String testName) {
        if (expected == null && actual == null) {
            pass(testName);
        } else if (expected != null && expected.equals(actual)) {
            pass(testName);
        } else {
            fail(testName, "expected <" + expected + "> but got <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            pass(testName);
        } else {
            fail(testName, "expected true but got false");
        }
    }

    private static void assertFalse(boolean condition, String testName) {
        if (!condition) {
            pass(testName);
        } else {
            fail(testName, "expected false but got true");
        }
    }

    private static void assertNull(Object obj, String testName) {
        if (obj == null) {
            pass(testName);
        } else {
            fail(testName, "expected null but got <" + obj + ">");
        }
    }

    private static void assertNotNull(Object obj, String testName) {
        if (obj != null) {
            pass(testName);
        } else {
            fail(testName, "expected non-null but got null");
        }
    }

    private static void pass(String testName) {
        System.out.println("  PASS: " + testName);
        passed++;
    }

    private static void fail(String testName, String reason) {
        System.out.println("  FAIL: " + testName + " — " + reason);
        failed++;
    }

    // -----------------------------------------------------------------------
    //  Temp file helpers
    // -----------------------------------------------------------------------

    private static File createTempCsv(String content) throws IOException {
        File f = File.createTempFile("test_", ".csv");
        f.deleteOnExit();
        try (FileWriter w = new FileWriter(f)) {
            w.write(content);
        }
        return f;
    }

    private static File createTempTxt(String content) throws IOException {
        File f = File.createTempFile("test_", ".txt");
        f.deleteOnExit();
        try (FileWriter w = new FileWriter(f)) {
            w.write(content);
        }
        return f;
    }

    // -----------------------------------------------------------------------
    //  Town Tests
    // -----------------------------------------------------------------------

    private static void testTownConstructor() {
        Town t = new Town("Accra");
        assertEqual("Accra", t.getName(), "constructor sets name");
        assertNotNull(t.getNeighbors(), "constructor initializes neighbors map");
        assertEqual(0, t.getNeighbors().size(), "constructor creates empty neighbors");
    }

    private static void testTownGettersSetters() {
        Town t = new Town("Accra");
        t.setName("Kumasi");
        assertEqual("Kumasi", t.getName(), "setName updates name");
    }

    private static void testTownAddNeighbor() {
        Town t = new Town("Accra");
        t.addNeighbor("tema", 316, 307);

        assertTrue(t.hasNeighbor("tema"), "addNeighbor makes hasNeighbor true");

        int[] edge = t.getNeighborEdge("tema");
        assertNotNull(edge, "addNeighbor stores edge data");
        assertEqual(316, edge[0], "addNeighbor stores correct distance");
        assertEqual(307, edge[1], "addNeighbor stores correct time");
        assertEqual(1, t.getNeighbors().size(), "addNeighbor increases neighbor count");
    }

    private static void testTownOverwriteNeighbor() {
        Town t = new Town("Accra");
        t.addNeighbor("tema", 100, 50);
        t.addNeighbor("tema", 200, 80);

        int[] edge = t.getNeighborEdge("tema");
        assertEqual(200, edge[0], "overwrite updates distance");
        assertEqual(80, edge[1], "overwrite updates time");
        assertEqual(1, t.getNeighbors().size(), "overwrite does not add second entry");
    }

    private static void testTownRemoveNeighbor() {
        Town t = new Town("Accra");
        t.addNeighbor("tema", 316, 307);
        t.removeNeighbor("tema");

        assertFalse(t.hasNeighbor("tema"), "removeNeighbor removes the edge");
        assertEqual(0, t.getNeighbors().size(), "removeNeighbor decreases count");
    }

    private static void testTownRemoveNonExistentNeighbor() {
        Town t = new Town("Accra");
        t.removeNeighbor("nowhere");
        assertEqual(0, t.getNeighbors().size(), "removing non-existent neighbor is a no-op");
    }

    private static void testTownGetNeighborEdgeNonExistent() {
        Town t = new Town("Accra");
        assertNull(t.getNeighborEdge("nowhere"), "getNeighborEdge returns null for missing");
    }

    private static void testTownHasNeighbor() {
        Town t = new Town("Accra");
        t.addNeighbor("tema", 316, 307);
        assertTrue(t.hasNeighbor("tema"), "hasNeighbor true for existing");
        assertFalse(t.hasNeighbor("kumasi"), "hasNeighbor false for missing");
    }

    private static void testTownSetNeighborsReplacesMap() {
        Town t = new Town("Accra");
        t.addNeighbor("tema", 316, 307);

        HashMap<String, int[]> newMap = new HashMap<>();
        newMap.put("kumasi", new int[]{252, 240});
        t.setNeighbors(newMap);

        assertFalse(t.hasNeighbor("tema"), "setNeighbors replaced old map");
        assertTrue(t.hasNeighbor("kumasi"), "setNeighbors contains new entry");
        assertEqual(1, t.getNeighbors().size(), "setNeighbors has correct size");
    }

    private static void testTownToString() {
        Town t = new Town("Accra");
        t.addNeighbor("tema", 316, 307);
        assertEqual("Town{name='Accra', neighbors=1}", t.toString(), "toString format");
    }

    // -----------------------------------------------------------------------
    //  Ghana Loading Tests
    // -----------------------------------------------------------------------

    private static void testLoadFromCsv() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,316,307\n" +
                "Accra,Osu,82,126\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        assertEqual(3, g.getTownCount(), "CSV: 3 towns loaded (Accra, Tema, Osu)");
        assertEqual(2, g.getEdgeCount(), "CSV: 2 edges loaded");
        assertNotNull(g.getTown("Accra"), "CSV: source town exists");
        assertNotNull(g.getTown("Tema"), "CSV: destination town exists");
    }

    private static void testLoadFromTxt() throws IOException {
        File f = createTempTxt(
                "Accra, Tema, 316, 307\n" +
                "Kumasi, Obuasi, 53, 57\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        assertEqual(4, g.getTownCount(), "TXT: 4 towns loaded");
        assertEqual(2, g.getEdgeCount(), "TXT: 2 edges loaded");
    }

    private static void testLoadUnsupportedFileType() throws IOException {
        boolean thrown = false;
        Ghana g = new Ghana();
        try {
            g.loadTowns("data.json");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown, "unsupported extension throws IllegalArgumentException");
    }

    private static void testLoadBlankLines() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "\n" +
                "Accra,Tema,316,307\n" +
                "   \n" +
                "Kumasi,Obuasi,53,57\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        assertEqual(4, g.getTownCount(), "blank lines: correct town count");
        assertEqual(2, g.getEdgeCount(), "blank lines: correct edge count");
    }

    private static void testLoadMissingColumns() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema\n" +
                "Kumasi,Obuasi,53,57\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        assertEqual(2, g.getTownCount(), "missing columns: only valid line creates towns");
        assertEqual(1, g.getEdgeCount(), "missing columns: only valid edge counted");
    }

    private static void testLoadBlankTownName() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                ",Tema,100,50\n" +
                "Accra,,200,80\n" +
                "Kumasi,Obuasi,53,57\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        assertEqual(2, g.getTownCount(), "blank name: only valid towns created");
        assertEqual(1, g.getEdgeCount(), "blank name: only valid edge counted");
    }

    private static void testLoadNonNumericValues() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,abc,307\n" +
                "Accra,Osu,82,xyz\n" +
                "Kumasi,Obuasi,53,57\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        assertEqual(2, g.getTownCount(), "non-numeric: only valid towns created");
        assertEqual(1, g.getEdgeCount(), "non-numeric: only valid edge counted");
    }

    private static void testLoadNegativeValues() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,-10,307\n" +
                "Kumasi,Obuasi,53,-5\n" +
                "Ho,Hohoe,354,342\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        assertEqual(2, g.getTownCount(), "negative values: only valid towns created");
        assertEqual(1, g.getEdgeCount(), "negative values: only valid edge counted");
    }

    private static void testLoadCaseInsensitive() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,316,307\n" +
                "ACCRA,Osu,82,126\n" +
                "accra,Kumasi,252,240\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        assertEqual(4, g.getTownCount(),
                "case-insensitive: Accra/ACCRA/accra = 1 town + Tema + Osu + Kumasi = 4");

        Town accra = g.getTown("aCcRa");
        assertNotNull(accra, "case-insensitive: lookup with mixed case works");
        assertEqual("Accra", accra.getName(),
                "case-insensitive: display name is first-encountered casing");
        assertEqual(3, accra.getNeighbors().size(),
                "case-insensitive: all 3 edges assigned to same town");
    }

    private static void testLoadDuplicateEdgeCount() throws IOException {
        File f1 = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,316,307\n");

        File f2 = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,320,310\n");

        Ghana g = new Ghana();
        g.loadTowns(f1.getPath());
        g.loadTowns(f2.getPath());

        assertEqual(1, g.getEdgeCount(),
                "duplicate edge: count stays at 1 after loading same edge twice");

        int[] edge = g.getNeighbors("Accra").get("tema");
        assertEqual(320, edge[0], "duplicate edge: values overwritten to latest");
    }

    private static void testLoadBothFormatsAccumulate() throws IOException {
        File csv = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,316,307\n");

        File txt = createTempTxt(
                "Kumasi, Obuasi, 53, 57\n");

        Ghana g = new Ghana();
        g.loadTowns(csv.getPath());
        g.loadTowns(txt.getPath());

        assertEqual(4, g.getTownCount(), "accumulate: 4 towns from both files");
        assertEqual(2, g.getEdgeCount(), "accumulate: 2 distinct edges from both files");
    }

    // -----------------------------------------------------------------------
    //  Dijkstra / getDistance Tests
    // -----------------------------------------------------------------------

    /**
     * Builds a small test graph:
     * <pre>
     *   A --100--> B --50--> C
     *   A --200--> C           (direct but longer)
     *   B --30-->  D
     * </pre>
     * D has no outgoing edges (dead end from D).
     * C has no outgoing edges.
     * No edge back from B/C/D to A (directed).
     */
    private static Ghana buildTestGraph() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "A,B,100,60\n" +
                "A,C,200,120\n" +
                "B,C,50,30\n" +
                "B,D,30,20\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());
        return g;
    }

    private static void testDistanceDirectNeighbor() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getDistance("A", "B");
        assertNotNull(result, "direct neighbor: A->B path exists");
        assertEqual(100, result.getDistance(), "direct neighbor: A->B = 100");
        assertEqual("[A, B]", result.getPath().toString(), "direct neighbor path: [A, B]");
    }

    private static void testDistanceMultiHop() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getDistance("A", "C");
        assertNotNull(result, "multi-hop: A->C path exists");
        assertEqual(150, result.getDistance(), "multi-hop: A->B->C (150) beats A->C (200)");
        assertEqual("[A, B, C]", result.getPath().toString(), "multi-hop path: [A, B, C]");
    }

    private static void testDistanceNoPath() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getDistance("D", "A");
        assertNull(result, "no path: D->A returns null");
    }

    private static void testDistanceNonExistentSource() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getDistance("Z", "A");
        assertNull(result, "non-existent source: returns null");
    }

    private static void testDistanceNonExistentDestination() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getDistance("A", "Z");
        assertNull(result, "non-existent destination: returns null");
    }

    private static void testDistanceSameTown() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getDistance("A", "A");
        assertNotNull(result, "same town: A->A path exists");
        assertEqual(0, result.getDistance(), "same town: A->A = 0");
        assertEqual("[A]", result.getPath().toString(), "same town path: [A]");
    }

    private static void testDistanceDirectedOnly() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult forward = g.getDistance("A", "B");
        Ghana.PathResult reverse = g.getDistance("B", "A");

        assertNotNull(forward, "directed: A->B path exists");
        assertEqual(100, forward.getDistance(), "directed: A->B = 100");
        assertNull(reverse, "directed: B->A = null (no reverse edge)");
    }

    private static void testDistanceCaseInsensitiveLookup() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,316,307\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        Ghana.PathResult result = g.getDistance("accra", "TEMA");
        assertNotNull(result, "case-insensitive: path exists");
        assertEqual(316, result.getDistance(), "case-insensitive distance: accra->TEMA = 316");
        assertEqual("[Accra, Tema]", result.getPath().toString(), "case-insensitive path: [Accra, Tema]");
    }

    private static void testDistanceChoosesShortestRoute() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "A,B,10,10\n" +
                "B,C,10,10\n" +
                "C,D,10,10\n" +
                "A,D,50,50\n" +
                "A,C,25,25\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        Ghana.PathResult result = g.getDistance("A", "D");
        assertNotNull(result, "shortest route: path exists");
        assertEqual(30, result.getDistance(), "shortest among 3 routes: A->B->C->D = 30");
        assertEqual("[A, B, C, D]", result.getPath().toString(), "shortest route path: [A, B, C, D]");
    }

    // -----------------------------------------------------------------------
    //  getFastestTime Tests
    // -----------------------------------------------------------------------

    private static void testFastestTimeDirectNeighbor() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getFastestTime("A", "B");
        assertNotNull(result, "fastest direct neighbor: path exists");
        assertEqual(60, result.getTime(), "fastest direct neighbor: A->B = 60 min");
        assertEqual("[A, B]", result.getPath().toString(), "fastest direct neighbor path: [A, B]");
    }

    private static void testFastestTimeMultiHop() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getFastestTime("A", "C");
        assertNotNull(result, "fastest multi-hop: path exists");
        assertEqual(90, result.getTime(), "fastest multi-hop: A->B->C (90) beats A->C (120)");
        assertEqual("[A, B, C]", result.getPath().toString(), "fastest multi-hop path: [A, B, C]");
    }

    /**
     * The critical edge case: the fastest-time route is different from the
     * shortest-distance route.
     *
     * <pre>
     *   A --slow road-->  C : 50 km, 200 min  (short but slow)
     *   A --fast road-->  B : 100 km, 10 min   (long but fast)
     *   B --fast road-->  C : 100 km, 10 min   (long but fast)
     * </pre>
     *
     * Shortest distance: A->C = 50 km  (path [A, C])
     * Fastest time:      A->B->C = 20 min (path [A, B, C])
     */
    private static void testFastestTimeDiffersFromDistance() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "A,C,50,200\n" +
                "A,B,100,10\n" +
                "B,C,100,10\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        Ghana.PathResult distResult = g.getDistance("A", "C");
        Ghana.PathResult timeResult = g.getFastestTime("A", "C");

        assertNotNull(distResult, "distance-optimal: A->C path exists");
        assertEqual(50, distResult.getDistance(), "distance-optimal: A->C = 50 km (direct)");
        assertEqual("[A, C]", distResult.getPath().toString(), "distance-optimal path: [A, C]");

        assertNotNull(timeResult, "time-optimal: A->C path exists");
        assertEqual(20, timeResult.getTime(), "time-optimal: A->B->C = 20 min (different route)");
        assertEqual("[A, B, C]", timeResult.getPath().toString(), "time-optimal path: [A, B, C]");
    }

    private static void testFastestTimeNoPath() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getFastestTime("D", "A");
        assertNull(result, "fastest no path: D->A returns null");
    }

    private static void testFastestTimeNonExistentSource() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getFastestTime("Z", "A");
        assertNull(result, "fastest non-existent source: returns null");
    }

    private static void testFastestTimeNonExistentDestination() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getFastestTime("A", "Z");
        assertNull(result, "fastest non-existent destination: returns null");
    }

    private static void testFastestTimeSameTown() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult result = g.getFastestTime("A", "A");
        assertNotNull(result, "fastest same town: path exists");
        assertEqual(0, result.getTime(), "fastest same town: A->A = 0 min");
        assertEqual("[A]", result.getPath().toString(), "fastest same town path: [A]");
    }

    private static void testFastestTimeDirectedOnly() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.PathResult forward = g.getFastestTime("A", "B");
        Ghana.PathResult reverse = g.getFastestTime("B", "A");

        assertNotNull(forward, "fastest directed: A->B path exists");
        assertEqual(60, forward.getTime(), "fastest directed: A->B = 60 min");
        assertNull(reverse, "fastest directed: B->A = null (no reverse edge)");
    }

    private static void testFastestTimeCaseInsensitive() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,316,307\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        Ghana.PathResult result = g.getFastestTime("accra", "TEMA");
        assertNotNull(result, "fastest case-insensitive: path exists");
        assertEqual(307, result.getTime(), "fastest case-insensitive: accra->TEMA = 307 min");
        assertEqual("[Accra, Tema]", result.getPath().toString(), "fastest case-insensitive path: [Accra, Tema]");
    }

    /**
     * Three competing routes with different times:
     * <pre>
     *   A->B->C->D = 5+5+5   = 15 min  (fastest)
     *   A->C->D    = 12+5     = 17 min
     *   A->D       = 100      = 100 min (direct but slowest)
     * </pre>
     */
    private static void testFastestTimeChoosesFastestRoute() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "A,B,10,5\n" +
                "B,C,10,5\n" +
                "C,D,10,5\n" +
                "A,C,20,12\n" +
                "A,D,5,100\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        Ghana.PathResult result = g.getFastestTime("A", "D");
        assertNotNull(result, "fastest chooses best: path exists");
        assertEqual(15, result.getTime(), "fastest among 3 routes: A->B->C->D = 15 min");
        assertEqual("[A, B, C, D]", result.getPath().toString(), "fastest chooses best path: [A, B, C, D]");
    }

    // -----------------------------------------------------------------------
    //  getFuelCost Tests
    // -----------------------------------------------------------------------

    /**
     * Helper to compare doubles within a small tolerance.
     */
    private static void assertDoubleEqual(double expected, double actual, String testName) {
        if (Math.abs(expected - actual) < 0.001) {
            pass(testName);
        } else {
            fail(testName, "expected <" + expected + "> but got <" + actual + ">");
        }
    }

    /**
     * Route [A, B]: edge A->B = 100 km.
     * Litres = 100 / 8 = 12.  Cost = 12 * 11.955 = 143.46
     */
    private static void testFuelCostDirectNeighbor() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(Arrays.asList("A", "B"));
        assertDoubleEqual(143.46, cost, "fuel cost route [A,B]: (100/8)*11.955 = 143.46");
    }

    /**
     * Multi-hop route [A, B, C]:
     * edge A->B = 100, edge B->C = 50, total = 150 km.
     * Litres = 150 / 8 = 18.  Cost = 18 * 11.955 = 215.19
     */
    private static void testFuelCostMultiHopRoute() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(Arrays.asList("A", "B", "C"));
        assertDoubleEqual(215.19, cost, "fuel cost route [A,B,C]: (150/8)*11.955 = 215.19");
    }

    /**
     * Integer division must truncate, not round.
     * Route [A, B, D]: edge A->B=100, edge B->D=30 → 130 km.
     * Litres = 130 / 8 = 16 (not 16.25).  Cost = 16 * 11.955 = 191.28
     */
    private static void testFuelCostIntegerDivisionTruncates() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(Arrays.asList("A", "B", "D"));
        assertDoubleEqual(191.28, cost, "fuel cost truncates: (130/8)*11.955 = 191.28");
    }

    /**
     * Distance < 8 km → 0 litres → cost 0.0.
     * Route [X, Y] = 5 km.
     */
    private static void testFuelCostDistanceLessThanOneLitre() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "X,Y,5,10\n");
        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        double cost = g.getFuelCost(Arrays.asList("X", "Y"));
        assertDoubleEqual(0.0, cost, "fuel cost < 8 km: (5/8)*11.955 = 0.0");
    }

    private static void testFuelCostEmptyRoute() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(new ArrayList<>());
        assertDoubleEqual(0.0, cost, "fuel cost empty route: 0.0");
    }

    private static void testFuelCostSingleTownRoute() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(Arrays.asList("A"));
        assertDoubleEqual(0.0, cost, "fuel cost single-town route: 0.0");
    }

    private static void testFuelCostNullRoute() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost((List<String>) null);
        assertDoubleEqual(0.0, cost, "fuel cost null route: 0.0");
    }

    /**
     * Route with a broken segment: [A, B, D, A].
     * A->B = 100, B->D = 30, D->A has no path → returns -1.0.
     */
    private static void testFuelCostNoPathInRoute() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(Arrays.asList("A", "B", "D", "A"));
        assertDoubleEqual(-1.0, cost, "fuel cost broken route [A,B,D,A]: D->A missing = -1.0");
    }

    private static void testFuelCostNonExistentTown() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(Arrays.asList("A", "Z"));
        assertDoubleEqual(-1.0, cost, "fuel cost non-existent town in route: -1.0");
    }

    /**
     * Case insensitive: route ["a", "b"] resolves to A->B = 100 km.
     */
    private static void testFuelCostCaseInsensitive() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(Arrays.asList("a", "b"));
        assertDoubleEqual(143.46, cost, "fuel cost case-insensitive: (100/8)*11.955 = 143.46");
    }

    /**
     * Route [A, D]: no direct edge, but Dijkstra finds A->B->D = 130 km, 80 min.
     * Litres = 130 / 8 = 16.  Cost = 16 * 11.955 = 191.28
     */
    private static void testFuelCostNoDirectEdgeUsesShortestPath() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(Arrays.asList("A", "D"));
        assertDoubleEqual(191.28, cost, "fuel cost no direct edge [A,D]: A->B->D = 130 km, (130/8)*11.955 = 191.28");
    }

    /**
     * Route [A, C]: direct edge = 200 km, but shortest path A->B->C = 150 km.
     * Litres = 150 / 8 = 18.  Cost = 18 * 11.955 = 215.19
     */
    private static void testFuelCostUsesShortestNotDirect() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getFuelCost(Arrays.asList("A", "C"));
        assertDoubleEqual(215.19, cost, "fuel cost [A,C]: shortest 150 km (not direct 200), (150/8)*11.955 = 215.19");
    }

    /**
     * Zero-distance edge: route [X, Y] = 0 km. Cost = 0.0.
     */
    private static void testFuelCostZeroDistanceEdge() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "X,Y,0,10\n");
        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        double cost = g.getFuelCost(Arrays.asList("X", "Y"));
        assertDoubleEqual(0.0, cost, "fuel cost zero distance: 0.0");
    }

    // -----------------------------------------------------------------------
    //  getTotalCost Tests
    // -----------------------------------------------------------------------

    /**
     * Single hop [A, B]: edge = 100 km, 60 min.
     * distance_cost = (100/8) * 11.955 = 12 * 11.955 = 143.46
     * time_cost     = 60 * 0.5 = 30.0
     * total         = 173.46
     */
    private static void testTotalCostSingleHop() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(Arrays.asList("A", "B"));
        assertDoubleEqual(173.46, cost, "total cost [A,B]: 143.46 + 30.0 = 173.46");
    }

    /**
     * Multi-hop [A, B, C]: edges = (100km,60min) + (50km,30min) = 150km, 90min.
     * distance_cost = (150/8) * 11.955 = 18 * 11.955 = 215.19
     * time_cost     = 90 * 0.5 = 45.0
     * total         = 260.19
     */
    private static void testTotalCostMultiHop() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(Arrays.asList("A", "B", "C"));
        assertDoubleEqual(260.19, cost, "total cost [A,B,C]: 215.19 + 45.0 = 260.19");
    }

    /**
     * Verifies each component independently: different distance and time
     * values to ensure both are accumulated correctly.
     * <pre>
     *   X->Y: 24 km, 100 min
     *   Y->Z: 16 km, 50  min
     *   total: 40 km, 150 min
     * </pre>
     * distance_cost = (40/8) * 11.955 = 5 * 11.955 = 59.775
     * time_cost     = 150 * 0.5 = 75.0
     * total         = 134.775
     */
    private static void testTotalCostBreakdown() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "X,Y,24,100\n" +
                "Y,Z,16,50\n");
        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        double cost = g.getTotalCost(Arrays.asList("X", "Y", "Z"));
        assertDoubleEqual(134.775, cost, "total cost breakdown: 59.775 + 75.0 = 134.775");
    }

    private static void testTotalCostEmptyRoute() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(new ArrayList<>());
        assertDoubleEqual(0.0, cost, "total cost empty route: 0.0");
    }

    private static void testTotalCostSingleTown() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(Arrays.asList("A"));
        assertDoubleEqual(0.0, cost, "total cost single-town route: 0.0");
    }

    private static void testTotalCostNullRoute() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(null);
        assertDoubleEqual(0.0, cost, "total cost null route: 0.0");
    }

    /**
     * Route with a missing directed edge: D->A does not exist.
     */
    private static void testTotalCostNoEdge() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(Arrays.asList("A", "B", "D", "A"));
        assertDoubleEqual(-1.0, cost, "total cost missing edge D->A: -1.0");
    }

    private static void testTotalCostNonExistentTown() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(Arrays.asList("A", "Z"));
        assertDoubleEqual(-1.0, cost, "total cost non-existent town: -1.0");
    }

    /**
     * Case insensitive: ["a", "b"] resolves to A->B edge (100km, 60min).
     * total = 143.46 + 30.0 = 173.46
     */
    private static void testTotalCostCaseInsensitive() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(Arrays.asList("a", "b"));
        assertDoubleEqual(173.46, cost, "total cost case-insensitive: 173.46");
    }

    /**
     * Route [A, D]: no direct edge, but Dijkstra finds A->B->D = 130 km, 80 min.
     * distance_cost = (130/8) * 11.955 = 16 * 11.955 = 191.28
     * time_cost     = 80 * 0.5 = 40.0
     * total         = 231.28
     */
    private static void testTotalCostNoDirectEdgeUsesShortestPath() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(Arrays.asList("A", "D"));
        assertDoubleEqual(231.28, cost, "total cost no direct edge [A,D]: 191.28 + 40.0 = 231.28");
    }

    /**
     * Route [A, C]: direct edge = 200 km / 120 min, but shortest-distance
     * path is A->B->C = 150 km / 90 min.
     * distance_cost = (150/8) * 11.955 = 18 * 11.955 = 215.19
     * time_cost     = 90 * 0.5 = 45.0
     * total         = 260.19
     */
    private static void testTotalCostUsesShortestNotDirect() throws IOException {
        Ghana g = buildTestGraph();
        double cost = g.getTotalCost(Arrays.asList("A", "C"));
        assertDoubleEqual(260.19, cost, "total cost [A,C]: shortest 150km/90min, 215.19 + 45.0 = 260.19");
    }

    /**
     * Zero distance and zero time: cost = 0.0 + 0.0 = 0.0
     */
    private static void testTotalCostZeroDistanceAndTime() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "X,Y,0,0\n");
        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        double cost = g.getTotalCost(Arrays.asList("X", "Y"));
        assertDoubleEqual(0.0, cost, "total cost zero distance and time: 0.0");
    }

    // -----------------------------------------------------------------------
    //  recommendRoute Tests
    // -----------------------------------------------------------------------

    /**
     * Shortest-distance route wins.
     * <pre>
     *   A->C : 50 km, 200 min  (short but slow)
     *   A->B : 100 km, 10 min  (long but fast)
     *   B->C : 100 km, 10 min  (long but fast)
     * </pre>
     * Shortest path: [A, C] — 50 km, 200 min
     *   fuel = (50/8)*11.955 = 6*11.955 = 71.73
     *   time = 200*0.5 = 100.0
     *   total = 171.73
     * Fastest path:  [A, B, C] — 200 km, 20 min
     *   fuel = (200/8)*11.955 = 25*11.955 = 298.875
     *   time = 20*0.5 = 10.0
     *   total = 308.875
     * Recommendation: shortest (171.73 < 308.875)
     */
    private static void testRecommendShortestWins() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "A,C,50,200\n" +
                "A,B,100,10\n" +
                "B,C,100,10\n");
        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        Ghana.RouteComparison rc = g.recommendRoute("A", "C");
        assertNotNull(rc, "recommend shortest wins: result exists");
        assertEqual("shortest", rc.getRecommendation(), "recommend shortest wins: recommendation");
        assertEqual("[A, C]", rc.getShortestPath().toString(), "recommend shortest wins: shortest path");
        assertEqual("[A, B, C]", rc.getFastestPath().toString(), "recommend shortest wins: fastest path");
        assertEqual(50, rc.getShortestDistance(), "recommend shortest wins: shortest distance");
        assertEqual(200, rc.getShortestTime(), "recommend shortest wins: shortest time");
        assertEqual(200, rc.getFastestDistance(), "recommend shortest wins: fastest distance");
        assertEqual(20, rc.getFastestTime(), "recommend shortest wins: fastest time");
        assertDoubleEqual(171.73, rc.getShortestTotalCost(), "recommend shortest wins: shortest total cost");
        assertDoubleEqual(308.875, rc.getFastestTotalCost(), "recommend shortest wins: fastest total cost");
    }

    /**
     * Fastest-time route wins.
     * <pre>
     *   A->C : 100 km, 10 min  (short-ish, very fast)
     *   A->B : 10 km, 200 min  (very short, very slow)
     *   B->C : 10 km, 200 min  (very short, very slow)
     * </pre>
     * Shortest path: [A, B, C] — 20 km, 400 min
     *   fuel = (20/8)*11.955 = 2*11.955 = 23.91
     *   time = 400*0.5 = 200.0
     *   total = 223.91
     * Fastest path:  [A, C] — 100 km, 10 min
     *   fuel = (100/8)*11.955 = 12*11.955 = 143.46
     *   time = 10*0.5 = 5.0
     *   total = 148.46
     * Recommendation: fastest (148.46 < 223.91)
     */
    private static void testRecommendFastestWins() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "A,C,100,10\n" +
                "A,B,10,200\n" +
                "B,C,10,200\n");
        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        Ghana.RouteComparison rc = g.recommendRoute("A", "C");
        assertNotNull(rc, "recommend fastest wins: result exists");
        assertEqual("fastest", rc.getRecommendation(), "recommend fastest wins: recommendation");
        assertEqual("[A, B, C]", rc.getShortestPath().toString(), "recommend fastest wins: shortest path");
        assertEqual("[A, C]", rc.getFastestPath().toString(), "recommend fastest wins: fastest path");
        assertDoubleEqual(223.91, rc.getShortestTotalCost(), "recommend fastest wins: shortest total cost");
        assertDoubleEqual(148.46, rc.getFastestTotalCost(), "recommend fastest wins: fastest total cost");
    }

    /**
     * Both optimizations produce the same route — recommendation is "either".
     * In buildTestGraph, A->B: shortest (100km, 60min) and fastest (60min)
     * both go directly A->B.
     */
    private static void testRecommendSameRoute() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.RouteComparison rc = g.recommendRoute("A", "B");
        assertNotNull(rc, "recommend same route: result exists");
        assertEqual("either", rc.getRecommendation(), "recommend same route: recommendation is either");
        assertEqual(rc.getShortestPath().toString(), rc.getFastestPath().toString(),
                "recommend same route: both paths identical");
        assertDoubleEqual(rc.getShortestTotalCost(), rc.getFastestTotalCost(),
                "recommend same route: both costs equal");
    }

    /**
     * Same town — zero cost both ways, "either".
     */
    private static void testRecommendSameTown() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.RouteComparison rc = g.recommendRoute("A", "A");
        assertNotNull(rc, "recommend same town: result exists");
        assertEqual("either", rc.getRecommendation(), "recommend same town: recommendation");
        assertDoubleEqual(0.0, rc.getShortestTotalCost(), "recommend same town: shortest cost = 0");
        assertDoubleEqual(0.0, rc.getFastestTotalCost(), "recommend same town: fastest cost = 0");
    }

    /**
     * No path between towns — returns null.
     */
    private static void testRecommendNoPath() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.RouteComparison rc = g.recommendRoute("D", "A");
        assertNull(rc, "recommend no path: returns null");
    }

    /**
     * Non-existent town — returns null.
     */
    private static void testRecommendNonExistentTown() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.RouteComparison rc = g.recommendRoute("A", "Z");
        assertNull(rc, "recommend non-existent town: returns null");
    }

    /**
     * Case insensitive town names.
     */
    private static void testRecommendCaseInsensitive() throws IOException {
        Ghana g = buildTestGraph();
        Ghana.RouteComparison rc = g.recommendRoute("a", "B");
        assertNotNull(rc, "recommend case-insensitive: result exists");
        assertEqual("[A, B]", rc.getShortestPath().toString(), "recommend case-insensitive: path uses display name");
    }

    /**
     * Verify getSummary() contains the comparison table and recommendation.
     */
    private static void testRecommendSummaryContent() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "A,C,50,200\n" +
                "A,B,100,10\n" +
                "B,C,100,10\n");
        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        Ghana.RouteComparison rc = g.recommendRoute("A", "C");
        String summary = rc.getSummary();
        assertTrue(summary.contains("Shortest Route"), "summary contains 'Shortest Route'");
        assertTrue(summary.contains("Fastest Route"), "summary contains 'Fastest Route'");
        assertTrue(summary.contains("Total Cost"), "summary contains 'Total Cost'");
        assertTrue(summary.contains("Recommendation"), "summary contains 'Recommendation'");
        assertTrue(summary.contains("171.73"), "summary contains shortest total cost");
        assertTrue(summary.contains("308.88"), "summary contains fastest total cost");
    }

    // -----------------------------------------------------------------------
    //  Top 3 Shortest Paths Tests
    // -----------------------------------------------------------------------

    private static void testTop3ShortestPathsStandard() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "A,B,10,10\n" +
                "B,C,10,10\n" +
                "A,C,25,25\n" +
                "A,D,15,15\n" +
                "D,C,15,15\n" +
                "A,E,20,20\n" +
                "E,C,20,20\n");
        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        java.util.List<java.util.List<String>> paths = g.getTop3ShortestPaths("A", "C");
        assertEqual(3, paths.size(), "Standard top 3 paths: Should return exactly 3 paths");
        assertEqual("[A, B, C]", paths.get(0).toString(), "First shortest path");
        assertEqual("[A, C]", paths.get(1).toString(), "Second shortest path");
        assertEqual("[A, D, C]", paths.get(2).toString(), "Third shortest path");
    }

    private static void testTop3ShortestPathsFewerThan3() throws IOException {
        Ghana g = buildTestGraph();
        java.util.List<java.util.List<String>> paths = g.getTop3ShortestPaths("A", "C");
        assertEqual(2, paths.size(), "Fewer than 3: Should return exactly 2 paths");
        assertEqual("[A, B, C]", paths.get(0).toString(), "First shortest path");
        assertEqual("[A, C]", paths.get(1).toString(), "Second shortest path");
    }

    private static void testTop3ShortestPathsNoPath() throws IOException {
        Ghana g = buildTestGraph();
        java.util.List<java.util.List<String>> paths = g.getTop3ShortestPaths("D", "A");
        assertEqual(0, paths.size(), "No path: Should return 0 paths");
    }
}
