import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Dimension;

/**
 * Time Complexity Analysis for Ghana Road Transport Network Algorithms
 *
 * Measures actual performance up to 900 nodes, then predicts scaling to 5000
 * based on theoretical complexity:
 * 1. getFastestTime() - O((V+E) log V) ≈ O(V log V) for sparse graphs
 * 2. getTop3PathsByTotalCost() - O(k*V*E) ≈ O(V²) for sparse graphs
 * 3. recommendRoute() - O((V+E) log V) ≈ O(V log V) for sparse graphs
 *
 * @author Stanley and Linos 2026
 */
public class ComplexityAnalysis {

    private static final Random RANDOM = new Random(42);
    private static final int MAX_ACTUAL_NODES = 900;
    private static final int MAX_PREDICTED_NODES = 5000;
    private static final int INCREMENT = 10;

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("GHANA ROAD TRANSPORT NETWORK - COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(80));
        System.out.println("\nPhase 1: Running actual tests from 10 to " + MAX_ACTUAL_NODES + " nodes");
        System.out.println(
                "Phase 2: Predicting performance from " + MAX_ACTUAL_NODES + " to " + MAX_PREDICTED_NODES + " nodes");
        System.out.println("Please wait...\n");

        List<ComplexityResult> actualResults = new ArrayList<>();

        for (int nodeCount = 10; nodeCount <= MAX_ACTUAL_NODES; nodeCount += INCREMENT) {
            if (nodeCount % 100 == 0) {
                System.out.println("Testing " + nodeCount + " nodes...");
            }
            ComplexityResult result = analyzeComplexity(nodeCount);
            actualResults.add(result);
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Actual testing complete! Computing predictions...");
        System.out.println("=".repeat(80));

        List<ComplexityResult> predictedResults = predictPerformance(actualResults);

        // Display the plots
        SwingUtilities.invokeLater(() -> displayPlots(actualResults, predictedResults));
    }

    private static ComplexityResult analyzeComplexity(int nodeCount) {
        Ghana ghana = createSyntheticGraph(nodeCount);

        List<String> townNames = new ArrayList<>(ghana.getTowns().keySet());
        String startTown = townNames.get(RANDOM.nextInt(townNames.size()));
        String endTown = townNames.get(RANDOM.nextInt(townNames.size()));

        while (startTown.equals(endTown)) {
            endTown = townNames.get(RANDOM.nextInt(townNames.size()));
        }

        ComplexityResult result = new ComplexityResult();
        result.nodeCount = nodeCount;
        result.edgeCount = ghana.getEdgeCount();

        for (int i = 0; i < 3; i++) {
            ghana.getFastestTime(startTown, endTown);
        }

        long start = System.nanoTime();
        int runs = Math.max(1, 10 / (nodeCount / 1000 + 1));
        for (int i = 0; i < runs; i++) {
            ghana.getFastestTime(startTown, endTown);
        }
        long end = System.nanoTime();
        result.fastestTimeMs = (end - start) / 1_000_000.0 / runs;

        // getTop3PathsByTotalCost()
        start = System.nanoTime();
        runs = Math.max(1, 5 / (nodeCount / 1000 + 1));
        for (int i = 0; i < runs; i++) {
            ghana.getTop3PathsByTotalCost(startTown, endTown);
        }
        end = System.nanoTime();
        result.top3PathsMs = (end - start) / 1_000_000.0 / runs;

        // recommendRoute()
        start = System.nanoTime();
        runs = Math.max(1, 10 / (nodeCount / 1000 + 1));
        for (int i = 0; i < runs; i++) {
            ghana.recommendRoute(startTown, endTown);
        }
        end = System.nanoTime();
        result.recommendRouteMs = (end - start) / 1_000_000.0 / runs;

        return result;
    }

    private static Ghana createSyntheticGraph(int nodeCount) {
        Ghana ghana = new Ghana();

        List<String> townNames = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            String townName = "Town_" + i;
            townNames.add(townName);
            ghana.addTown(townName);
        }

        int avgDegree = Math.min(6, nodeCount / 20 + 2);

        for (int i = 0; i < nodeCount; i++) {
            String source = townNames.get(i);
            int edgesToCreate = RANDOM.nextInt(avgDegree) + 2;

            for (int j = 0; j < edgesToCreate; j++) {
                int targetIndex;
                if (RANDOM.nextDouble() < 0.7) {
                    int range = Math.max(1, nodeCount / 10);
                    targetIndex = (i + RANDOM.nextInt(range) + 1) % nodeCount;
                } else {
                    targetIndex = RANDOM.nextInt(nodeCount);
                }

                if (targetIndex != i) {
                    String destination = townNames.get(targetIndex);
                    int distance = RANDOM.nextInt(200) + 50;
                    int time = (int) (distance * 0.6 + RANDOM.nextInt(30));

                    try {
                        ghana.updateEdge(source, destination, distance, time);
                    } catch (IllegalArgumentException e) {
                        // Edge already exists, skip
                    }
                }
            }
        }

        return ghana;
    }

    private static List<ComplexityResult> predictPerformance(List<ComplexityResult> actualResults) {
        List<ComplexityResult> predictions = new ArrayList<>();

        ComplexityResult reference = actualResults.get(actualResults.size() - 1);
        int refNodes = reference.nodeCount;

        System.out.println("\nFitting curves based on theoretical complexity:");
        System.out.println("  - getFastestTime & recommendRoute: O(V log V)");
        System.out.println("  - getTop3Paths: O(V²)");
        System.out.println();

        int sampleSize = Math.min(20, actualResults.size() / 2);
        ComplexityResult earlier = actualResults.get(actualResults.size() - sampleSize);

        double nodeRatio = (double) refNodes / earlier.nodeCount;
        double logRatio = Math.log(refNodes) / Math.log(earlier.nodeCount);

        double fastestScaleFactor = reference.fastestTimeMs / earlier.fastestTimeMs;
        double top3ScaleFactor = reference.top3PathsMs / earlier.top3PathsMs;
        double recommendScaleFactor = reference.recommendRouteMs / earlier.recommendRouteMs;

        double kFastest = reference.fastestTimeMs / (refNodes * Math.log(refNodes));
        double kTop3 = reference.top3PathsMs / (refNodes * refNodes);
        double kRecommend = reference.recommendRouteMs / (refNodes * Math.log(refNodes));

        for (int nodeCount = MAX_ACTUAL_NODES + INCREMENT; nodeCount <= MAX_PREDICTED_NODES; nodeCount += INCREMENT) {
            ComplexityResult pred = new ComplexityResult();
            pred.nodeCount = nodeCount;
            pred.edgeCount = (int) (nodeCount * 4.5); // Estimated edge count

            pred.fastestTimeMs = kFastest * nodeCount * Math.log(nodeCount);
            pred.recommendRouteMs = kRecommend * nodeCount * Math.log(nodeCount);

            pred.top3PathsMs = kTop3 * nodeCount * nodeCount;

            predictions.add(pred);
        }

        return predictions;
    }

    /**
     * Creates and displays the plots with actual and predicted data
     */
    private static void displayPlots(List<ComplexityResult> actualResults,
            List<ComplexityResult> predictedResults) {
        // create datasets
        XYSeriesCollection actualDataset = new XYSeriesCollection();
        XYSeriesCollection predictedDataset = new XYSeriesCollection();

        XYSeries actualFastest = new XYSeries("getFastestTime() - Measured");
        XYSeries actualTop3 = new XYSeries("getTop3Paths() - Measured");
        XYSeries actualRecommend = new XYSeries("recommendRoute() - Measured");

        for (ComplexityResult r : actualResults) {
            actualFastest.add(r.nodeCount, r.fastestTimeMs);
            actualTop3.add(r.nodeCount, r.top3PathsMs);
            actualRecommend.add(r.nodeCount, r.recommendRouteMs);
        }

        actualDataset.addSeries(actualFastest);
        actualDataset.addSeries(actualTop3);
        actualDataset.addSeries(actualRecommend);

        // Predicted measurements
        XYSeries predictedFastest = new XYSeries("getFastestTime() - Predicted");
        XYSeries predictedTop3 = new XYSeries("getTop3Paths() - Predicted");
        XYSeries predictedRecommend = new XYSeries("recommendRoute() - Predicted");

        // Add last actual point to predictions for continuity
        ComplexityResult lastActual = actualResults.get(actualResults.size() - 1);
        predictedFastest.add(lastActual.nodeCount, lastActual.fastestTimeMs);
        predictedTop3.add(lastActual.nodeCount, lastActual.top3PathsMs);
        predictedRecommend.add(lastActual.nodeCount, lastActual.recommendRouteMs);

        for (ComplexityResult r : predictedResults) {
            predictedFastest.add(r.nodeCount, r.fastestTimeMs);
            predictedTop3.add(r.nodeCount, r.top3PathsMs);
            predictedRecommend.add(r.nodeCount, r.recommendRouteMs);
        }

        predictedDataset.addSeries(predictedFastest);
        predictedDataset.addSeries(predictedTop3);
        predictedDataset.addSeries(predictedRecommend);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Algorithm Complexity: Measured (10-900) vs Predicted (900-5000)",
                "Number of Nodes",
                "Execution Time (ms)",
                actualDataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

        plot.setDataset(1, predictedDataset);

        XYLineAndShapeRenderer actualRenderer = new XYLineAndShapeRenderer();
        XYLineAndShapeRenderer predictedRenderer = new XYLineAndShapeRenderer();

        actualRenderer.setSeriesPaint(0, new Color(0, 120, 215));
        actualRenderer.setSeriesStroke(0, new BasicStroke(3.0f));

        actualRenderer.setSeriesPaint(1, new Color(255, 140, 0));
        actualRenderer.setSeriesStroke(1, new BasicStroke(3.0f));

        actualRenderer.setSeriesPaint(2, new Color(0, 180, 80));
        actualRenderer.setSeriesStroke(2, new BasicStroke(3.0f));

        float[] dashPattern = { 10.0f, 5.0f };
        predictedRenderer.setSeriesPaint(0, new Color(0, 120, 215));
        predictedRenderer.setSeriesStroke(0, new BasicStroke(2.5f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));

        predictedRenderer.setSeriesPaint(1, new Color(255, 140, 0));
        predictedRenderer.setSeriesStroke(1, new BasicStroke(2.5f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));

        predictedRenderer.setSeriesPaint(2, new Color(0, 180, 80));
        predictedRenderer.setSeriesStroke(2, new BasicStroke(2.5f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));

        plot.setRenderer(0, actualRenderer);
        plot.setRenderer(1, predictedRenderer);

        JFrame frame = new JFrame("Complexity Analysis: Measured vs Predicted");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1400, 800));
        chartPanel.setMouseWheelEnabled(true);

        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Print summary
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PREDICTIONS FOR 5000 NODES:");
        System.out.println("=".repeat(80));
        ComplexityResult pred5000 = predictedResults.get(predictedResults.size() - 1);
        System.out.printf("  getFastestTime():         %.2f ms%n", pred5000.fastestTimeMs);
        System.out.printf("  getTop3PathsByTotalCost(): %.2f ms%n", pred5000.top3PathsMs);
        System.out.printf("  recommendRoute():         %.2f ms%n", pred5000.recommendRouteMs);
        System.out.println("=".repeat(80));
        System.out.println("\nNote: Solid lines = measured data, Dashed lines = predicted data");
        System.out.println("Predictions based on theoretical complexity: O(V log V) and O(V²)");
    }

    static class ComplexityResult {
        int nodeCount;
        int edgeCount;
        double fastestTimeMs;
        double top3PathsMs;
        double recommendRouteMs;
    }
}
