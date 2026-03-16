import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

/**
 * Main visualization screen. Renders the loaded graph as an interactive
 * node-edge diagram with a sidebar for stats and a bottom toolbar.
 */
public class MainScreen {

    private static final String BG = "#1a1a2e";
    private static final String SURFACE = "#16213e";
    private static final String CARD = "#1c2a4a";
    private static final String ACCENT = "#e94560";
    private static final String BTN_BG = "#0f3460";
    private static final String TEXT = "#ffffff";
    private static final String TEXT_DIM = "#a0a0b8";
    private static final double NODE_RADIUS = 6;

    private final BorderPane root;
    private final Ghana ghana;
    private final Pane graphPane;
    private final Group graphGroup;
    private final Map<String, Circle> nodeCircles = new HashMap<>();
    private final Map<String, double[]> nodePositions = new HashMap<>();
    private final List<Line> edgeLines = new ArrayList<>();
    private final List<Polygon> arrowHeads = new ArrayList<>();
    private Circle selectedNode = null;
    private double zoomFactor = 1.0;
    private boolean laid = false;

    MainScreen(Ghana ghana, String fileName, Runnable onReload) {
        this.ghana = ghana;

        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        root.setTop(buildTopBar(fileName, onReload));

        graphGroup = new Group();
        graphPane = new Pane(graphGroup);
        graphPane.setStyle("-fx-background-color: " + SURFACE + ";");

        graphPane.setOnScroll(ev -> {
            double delta = ev.getDeltaY() > 0 ? 1.1 : 0.9;
            zoomFactor = Math.max(0.1, Math.min(5.0, zoomFactor * delta));
            graphGroup.getTransforms().setAll(new Scale(zoomFactor, zoomFactor));
            ev.consume();
        });

        ScrollPane scroll = new ScrollPane(graphPane);
        scroll.setStyle("-fx-background: " + SURFACE + "; -fx-border-color: transparent;");
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        root.setCenter(scroll);
        root.setRight(buildSidebar());
        root.setBottom(buildToolbar());

        scroll.viewportBoundsProperty().addListener((obs, old, bounds) -> {
            if (bounds.getWidth() > 100 && bounds.getHeight() > 100 && !laid) {
                laid = true;
                layoutGraph(bounds.getWidth(), bounds.getHeight());
            }
        });
    }

    Parent getRoot() {
        return root;
    }

    // ------------------------------------------------------------------
    //  Top bar
    // ------------------------------------------------------------------

    private HBox buildTopBar(String fileName, Runnable onReload) {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.setStyle("-fx-background-color: " + SURFACE + ";");

        ToggleGroup modeGroup = new ToggleGroup();

        ToggleButton selectBtn = new ToggleButton("Select");
        selectBtn.setToggleGroup(modeGroup);
        selectBtn.setSelected(true);
        styleToggle(selectBtn);

        ToggleButton freeformBtn = new ToggleButton("Freeform");
        freeformBtn.setToggleGroup(modeGroup);
        styleToggle(freeformBtn);

        Label fileLabel = new Label(fileName);
        fileLabel.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 13;");

        Label statsLabel = new Label(ghana.getTownCount() + " towns  |  "
                + ghana.getEdgeCount() + " edges");
        statsLabel.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 13;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button reloadBtn = new Button("New File");
        styleActionBtn(reloadBtn, false);
        reloadBtn.setOnAction(e -> onReload.run());

        bar.getChildren().addAll(selectBtn, freeformBtn,
                new Separator(), fileLabel, statsLabel, spacer, reloadBtn);
        return bar;
    }

    // ------------------------------------------------------------------
    //  Right sidebar
    // ------------------------------------------------------------------

    private VBox buildSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(16));
        sidebar.setStyle("-fx-background-color: " + SURFACE + ";");

        sidebar.getChildren().add(sidebarCard("Network Stats",
                "Towns: " + ghana.getTownCount()
                        + "\nEdges: " + ghana.getEdgeCount()));

        sidebar.getChildren().add(sidebarCard("Recommend",
                "Select two towns to\ncompare routes.\n\n(Coming soon)"));

        sidebar.getChildren().add(sidebarCard("Costs",
                "Route cost breakdown\nwill appear here.\n\n(Coming soon)"));

        return sidebar;
    }

    private VBox sidebarCard(String title, String body) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: " + CARD + ";"
                + "-fx-background-radius: 10;");

        Label t = new Label(title);
        t.setFont(Font.font("System", FontWeight.BOLD, 14));
        t.setStyle("-fx-text-fill: " + ACCENT + ";");

        Label b = new Label(body);
        b.setWrapText(true);
        b.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 12;");

        card.getChildren().addAll(t, b);
        return card;
    }

    // ------------------------------------------------------------------
    //  Bottom toolbar
    // ------------------------------------------------------------------

    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.setStyle("-fx-background-color: " + SURFACE + ";");

        Button deleteBtn = new Button("Delete");
        Button editBtn = new Button("Edit");
        Button addBtn = new Button("Add");
        Button pathBtn = new Button("Path");

        for (Button b : new Button[]{deleteBtn, editBtn, addBtn, pathBtn}) {
            styleActionBtn(b, true);
        }
        pathBtn.setStyle(pathBtn.getStyle()
                + "-fx-background-color: " + ACCENT + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label modeLabel = new Label("Node");
        modeLabel.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 12;");
        Label edgeLabel = new Label("Edge");
        edgeLabel.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 12;");

        bar.getChildren().addAll(deleteBtn, editBtn, addBtn, pathBtn,
                spacer, modeLabel, edgeLabel);
        return bar;
    }

    // ------------------------------------------------------------------
    //  Graph layout – force-directed (Fruchterman-Reingold)
    // ------------------------------------------------------------------

    private final Map<String, Text> nodeLabels = new HashMap<>();

    private void layoutGraph(double viewW, double viewH) {
        graphGroup.getChildren().clear();
        nodeCircles.clear();
        nodePositions.clear();
        nodeLabels.clear();
        edgeLines.clear();
        arrowHeads.clear();

        HashMap<String, Town> towns = ghana.getTowns();
        List<String> keys = new ArrayList<>(towns.keySet());
        keys.sort(String::compareTo);
        int n = keys.size();
        if (n == 0) return;

        double area = 2500.0 * n;
        double side = Math.sqrt(area);
        double k = 0.85 * Math.sqrt(area / n);

        Random rng = new Random(42);
        double[] posX = new double[n];
        double[] posY = new double[n];
        for (int i = 0; i < n; i++) {
            posX[i] = rng.nextDouble() * side;
            posY[i] = rng.nextDouble() * side;
        }

        Map<String, Integer> keyIndex = new HashMap<>();
        for (int i = 0; i < n; i++) keyIndex.put(keys.get(i), i);

        int iterations = 120;
        double temp = side * 0.15;
        double cooling = temp / (iterations + 1);

        for (int iter = 0; iter < iterations; iter++) {
            double[] dispX = new double[n];
            double[] dispY = new double[n];

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double dx = posX[i] - posX[j];
                    double dy = posY[i] - posY[j];
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 0.01) dist = 0.01;
                    double repForce = (k * k) / dist;
                    double fx = (dx / dist) * repForce;
                    double fy = (dy / dist) * repForce;
                    dispX[i] += fx; dispY[i] += fy;
                    dispX[j] -= fx; dispY[j] -= fy;
                }
            }

            for (String srcKey : keys) {
                Town srcTown = towns.get(srcKey);
                if (srcTown.getNeighbors() == null) continue;
                int si = keyIndex.get(srcKey);
                for (String dstKey : srcTown.getNeighbors().keySet()) {
                    Integer di = keyIndex.get(dstKey);
                    if (di == null || di <= si) continue;
                    double dx = posX[si] - posX[di];
                    double dy = posY[si] - posY[di];
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 0.01) dist = 0.01;
                    double attForce = (dist * dist) / k;
                    double fx = (dx / dist) * attForce;
                    double fy = (dy / dist) * attForce;
                    dispX[si] -= fx; dispY[si] -= fy;
                    dispX[di] += fx; dispY[di] += fy;
                }
            }

            for (int i = 0; i < n; i++) {
                double dLen = Math.sqrt(dispX[i] * dispX[i] + dispY[i] * dispY[i]);
                if (dLen < 0.01) dLen = 0.01;
                double cap = Math.min(dLen, temp);
                posX[i] += (dispX[i] / dLen) * cap;
                posY[i] += (dispY[i] / dLen) * cap;
                posX[i] = Math.max(0, Math.min(side, posX[i]));
                posY[i] = Math.max(0, Math.min(side, posY[i]));
            }
            temp -= cooling;
        }

        double margin = 60;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (posX[i] < minX) minX = posX[i];
            if (posX[i] > maxX) maxX = posX[i];
            if (posY[i] < minY) minY = posY[i];
            if (posY[i] > maxY) maxY = posY[i];
        }
        double rangeX = maxX - minX;
        double rangeY = maxY - minY;
        if (rangeX < 1) rangeX = 1;
        if (rangeY < 1) rangeY = 1;

        double canvasW = Math.max(1400, n * 16);
        double canvasH = canvasW * 0.75;

        for (int i = 0; i < n; i++) {
            posX[i] = margin + ((posX[i] - minX) / rangeX) * (canvasW - 2 * margin);
            posY[i] = margin + ((posY[i] - minY) / rangeY) * (canvasH - 2 * margin);
        }

        for (int i = 0; i < n; i++) {
            String key = keys.get(i);
            Town town = towns.get(key);
            double x = posX[i];
            double y = posY[i];
            nodePositions.put(key, new double[]{x, y});

            Circle circle = new Circle(x, y, NODE_RADIUS);
            circle.setFill(Color.web(ACCENT));
            circle.setStroke(Color.web("#ffffff"));
            circle.setStrokeWidth(1.5);

            Text label = new Text(town.getName());
            label.setFont(Font.font("System", FontWeight.NORMAL, 10));
            label.setFill(Color.web("#d0d0e8"));
            label.setMouseTransparent(true);
            label.setX(x + NODE_RADIUS + 4);
            label.setY(y + 3.5);
            nodeLabels.put(key, label);

            Tooltip tip = new Tooltip(town.getName()
                    + "\nOutgoing edges: " + town.getNeighbors().size());
            tip.setStyle("-fx-font-size: 12; -fx-background-color: #222244;"
                    + " -fx-text-fill: white;");
            Tooltip.install(circle, tip);

            circle.setOnMousePressed(ev -> {
                circle.setUserData(new double[]{ev.getSceneX(), ev.getSceneY()});
                selectNode(circle, key);
                ev.consume();
            });
            circle.setOnMouseDragged(ev -> {
                double[] origin = (double[]) circle.getUserData();
                double deltaX = (ev.getSceneX() - origin[0]) / zoomFactor;
                double deltaY = (ev.getSceneY() - origin[1]) / zoomFactor;
                double nx = circle.getCenterX() + deltaX;
                double ny = circle.getCenterY() + deltaY;
                circle.setCenterX(nx);
                circle.setCenterY(ny);
                circle.setUserData(new double[]{ev.getSceneX(), ev.getSceneY()});
                nodePositions.get(key)[0] = nx;
                nodePositions.get(key)[1] = ny;
                Text lbl = nodeLabels.get(key);
                if (lbl != null) {
                    lbl.setX(nx + NODE_RADIUS + 4);
                    lbl.setY(ny + 3.5);
                }
                redrawEdges();
                ev.consume();
            });

            nodeCircles.put(key, circle);
        }

        drawEdges(towns, keys);

        edgeLines.forEach(l -> graphGroup.getChildren().add(l));
        arrowHeads.forEach(a -> graphGroup.getChildren().add(a));
        nodeCircles.values().forEach(c -> graphGroup.getChildren().add(c));
        nodeLabels.values().forEach(l -> graphGroup.getChildren().add(l));

        zoomFactor = Math.min(viewW / canvasW, viewH / canvasH) * 0.95;
        graphGroup.getTransforms().setAll(new Scale(zoomFactor, zoomFactor));
    }

    // ------------------------------------------------------------------
    //  Edge drawing
    // ------------------------------------------------------------------

    private void drawEdges(HashMap<String, Town> towns, List<String> keys) {
        edgeLines.clear();
        arrowHeads.clear();

        for (String srcKey : keys) {
            Town srcTown = towns.get(srcKey);
            double[] srcPos = nodePositions.get(srcKey);
            if (srcPos == null || srcTown.getNeighbors() == null) continue;

            for (Map.Entry<String, int[]> neighbor : srcTown.getNeighbors().entrySet()) {
                String dstKey = neighbor.getKey();
                double[] dstPos = nodePositions.get(dstKey);
                if (dstPos == null) continue;

                Line line = new Line(srcPos[0], srcPos[1], dstPos[0], dstPos[1]);
                line.setStroke(Color.web("#5c6a8a", 0.45));
                line.setStrokeWidth(0.8);
                line.setMouseTransparent(true);
                edgeLines.add(line);

                Polygon arrow = createArrowHead(srcPos[0], srcPos[1],
                        dstPos[0], dstPos[1]);
                arrowHeads.add(arrow);
            }
        }
    }

    private Polygon createArrowHead(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return new Polygon();

        double ux = dx / len;
        double uy = dy / len;

        double tipX = x2 - ux * (NODE_RADIUS + 2);
        double tipY = y2 - uy * (NODE_RADIUS + 2);

        double sz = 4;
        double baseX = tipX - ux * sz;
        double baseY = tipY - uy * sz;

        double perpX = -uy;
        double perpY = ux;

        Polygon arrow = new Polygon(
                tipX, tipY,
                baseX + perpX * sz * 0.45, baseY + perpY * sz * 0.45,
                baseX - perpX * sz * 0.45, baseY - perpY * sz * 0.45);
        arrow.setFill(Color.web("#5c6a8a", 0.5));
        arrow.setMouseTransparent(true);
        return arrow;
    }

    private void redrawEdges() {
        HashMap<String, Town> towns = ghana.getTowns();
        List<String> keys = new ArrayList<>(towns.keySet());
        keys.sort(String::compareTo);

        int idx = 0;
        for (String srcKey : keys) {
            Town srcTown = towns.get(srcKey);
            double[] srcPos = nodePositions.get(srcKey);
            if (srcPos == null || srcTown.getNeighbors() == null) continue;

            for (Map.Entry<String, int[]> neighbor : srcTown.getNeighbors().entrySet()) {
                String dstKey = neighbor.getKey();
                double[] dstPos = nodePositions.get(dstKey);
                if (dstPos == null) continue;

                if (idx < edgeLines.size()) {
                    Line line = edgeLines.get(idx);
                    line.setStartX(srcPos[0]);
                    line.setStartY(srcPos[1]);
                    line.setEndX(dstPos[0]);
                    line.setEndY(dstPos[1]);

                    Polygon arrow = arrowHeads.get(idx);
                    Polygon fresh = createArrowHead(srcPos[0], srcPos[1],
                            dstPos[0], dstPos[1]);
                    arrow.getPoints().setAll(fresh.getPoints());
                }
                idx++;
            }
        }
    }

    // ------------------------------------------------------------------
    //  Node selection – highlights outgoing edges
    // ------------------------------------------------------------------

    private void selectNode(Circle circle, String key) {
        if (selectedNode != null) {
            selectedNode.setStroke(Color.web("#ffffff"));
            selectedNode.setStrokeWidth(1.5);
            selectedNode.setRadius(NODE_RADIUS);
        }

        edgeLines.forEach(l -> {
            l.setStroke(Color.web("#5c6a8a", 0.45));
            l.setStrokeWidth(0.8);
        });
        arrowHeads.forEach(a -> a.setFill(Color.web("#5c6a8a", 0.5)));

        selectedNode = circle;
        circle.setStroke(Color.web(ACCENT));
        circle.setStrokeWidth(3);
        circle.setRadius(NODE_RADIUS + 2);
        circle.toFront();

        Text lbl = nodeLabels.get(key);
        if (lbl != null) lbl.toFront();

        HashMap<String, Town> towns = ghana.getTowns();
        Town srcTown = towns.get(key);
        if (srcTown == null) return;

        List<String> keys = new ArrayList<>(towns.keySet());
        keys.sort(String::compareTo);

        int idx = 0;
        for (String srcKey : keys) {
            Town t = towns.get(srcKey);
            double[] srcPos = nodePositions.get(srcKey);
            if (srcPos == null || t.getNeighbors() == null) continue;

            for (Map.Entry<String, int[]> neighbor : t.getNeighbors().entrySet()) {
                String dstKey = neighbor.getKey();
                if (nodePositions.get(dstKey) == null) continue;

                if (srcKey.equals(key) && idx < edgeLines.size()) {
                    edgeLines.get(idx).setStroke(Color.web(ACCENT, 0.8));
                    edgeLines.get(idx).setStrokeWidth(2.0);
                    edgeLines.get(idx).toFront();
                    arrowHeads.get(idx).setFill(Color.web(ACCENT, 0.9));
                    arrowHeads.get(idx).toFront();

                    Circle dstCircle = nodeCircles.get(dstKey);
                    if (dstCircle != null) dstCircle.toFront();
                    Text dstLbl = nodeLabels.get(dstKey);
                    if (dstLbl != null) dstLbl.toFront();
                }
                idx++;
            }
        }
        circle.toFront();
    }

    // ------------------------------------------------------------------
    //  Styling helpers
    // ------------------------------------------------------------------

    private void styleToggle(ToggleButton btn) {
        btn.setFont(Font.font("System", FontWeight.NORMAL, 12));
        btn.setStyle("-fx-background-color: " + CARD + ";"
                + "-fx-text-fill: " + TEXT + ";"
                + "-fx-background-radius: 15;"
                + "-fx-padding: 6 16;"
                + "-fx-cursor: hand;");
        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.setStyle("-fx-background-color: " + ACCENT + ";"
                        + "-fx-text-fill: " + TEXT + ";"
                        + "-fx-background-radius: 15;"
                        + "-fx-padding: 6 16;"
                        + "-fx-cursor: hand;");
            } else {
                btn.setStyle("-fx-background-color: " + CARD + ";"
                        + "-fx-text-fill: " + TEXT + ";"
                        + "-fx-background-radius: 15;"
                        + "-fx-padding: 6 16;"
                        + "-fx-cursor: hand;");
            }
        });
    }

    private void styleActionBtn(Button btn, boolean small) {
        double fontSize = small ? 12 : 13;
        String padding = small ? "6 14" : "8 20";
        btn.setFont(Font.font("System", FontWeight.NORMAL, fontSize));
        btn.setStyle("-fx-background-color: " + BTN_BG + ";"
                + "-fx-text-fill: " + TEXT + ";"
                + "-fx-background-radius: 8;"
                + "-fx-padding: " + padding + ";"
                + "-fx-cursor: hand;");
        btn.setOnMouseEntered(e ->
                btn.setStyle("-fx-background-color: #163d73;"
                        + "-fx-text-fill: " + TEXT + ";"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: " + padding + ";"
                        + "-fx-cursor: hand;"));
        btn.setOnMouseExited(e ->
                btn.setStyle("-fx-background-color: " + BTN_BG + ";"
                        + "-fx-text-fill: " + TEXT + ";"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: " + padding + ";"
                        + "-fx-cursor: hand;"));
    }
}
