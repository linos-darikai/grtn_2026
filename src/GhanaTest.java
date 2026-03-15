import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

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
        int dist = g.getDistance("A", "B");
        assertEqual(100, dist, "direct neighbor: A->B = 100");
    }

    private static void testDistanceMultiHop() throws IOException {
        Ghana g = buildTestGraph();
        // A->B->C = 100+50 = 150, which is shorter than A->C = 200
        int dist = g.getDistance("A", "C");
        assertEqual(150, dist, "multi-hop: A->B->C (150) beats A->C (200)");
    }

    private static void testDistanceNoPath() throws IOException {
        Ghana g = buildTestGraph();
        // D has no outgoing edges, and no edges lead from D/C back to A
        int dist = g.getDistance("D", "A");
        assertEqual(-1, dist, "no path: D->A returns -1");
    }

    private static void testDistanceNonExistentSource() throws IOException {
        Ghana g = buildTestGraph();
        int dist = g.getDistance("Z", "A");
        assertEqual(-1, dist, "non-existent source: returns -1");
    }

    private static void testDistanceNonExistentDestination() throws IOException {
        Ghana g = buildTestGraph();
        int dist = g.getDistance("A", "Z");
        assertEqual(-1, dist, "non-existent destination: returns -1");
    }

    private static void testDistanceSameTown() throws IOException {
        Ghana g = buildTestGraph();
        int dist = g.getDistance("A", "A");
        assertEqual(0, dist, "same town: A->A = 0");
    }

    private static void testDistanceDirectedOnly() throws IOException {
        Ghana g = buildTestGraph();
        // A->B exists (100), but B->A does not
        int distForward = g.getDistance("A", "B");
        int distReverse = g.getDistance("B", "A");

        assertEqual(100, distForward, "directed: A->B = 100");
        assertEqual(-1, distReverse, "directed: B->A = -1 (no reverse edge)");
    }

    private static void testDistanceCaseInsensitiveLookup() throws IOException {
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "Accra,Tema,316,307\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        int dist = g.getDistance("accra", "TEMA");
        assertEqual(316, dist, "case-insensitive distance: accra->TEMA = 316");
    }

    private static void testDistanceChoosesShortestRoute() throws IOException {
        // A->B->C->D = 10+10+10 = 30
        // A->D = 50 (direct but longer)
        // A->C->D = 25+10 = 35 (also longer)
        File f = createTempCsv(
                "source,destination,distance_km,avg_time_min\n" +
                "A,B,10,10\n" +
                "B,C,10,10\n" +
                "C,D,10,10\n" +
                "A,D,50,50\n" +
                "A,C,25,25\n");

        Ghana g = new Ghana();
        g.loadTowns(f.getPath());

        int dist = g.getDistance("A", "D");
        assertEqual(30, dist, "shortest among 3 routes: A->B->C->D = 30");
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
