import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import java.io.IOException;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

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
    private final String filePath;
    private final Pane graphPane;
    private final Group graphGroup;
    private final Map<String, Circle> nodeCircles = new HashMap<>();
    private final Map<String, double[]> nodePositions = new HashMap<>();
    private final List<Line> edgeLines = new ArrayList<>();
    private final List<Polygon> arrowHeads = new ArrayList<>();
    private Circle selectedNode = null;
    private String selectedKey = null;
    private boolean editMode = false;
    private boolean addMode = false;
    private VBox editPanel = null;
    private double zoomFactor = 1.0;
    private boolean laid = false;
    private double lastViewW;
    private double lastViewH;

    MainScreen(Ghana ghana, String fileName, String filePath, Runnable onReload) {
        this.ghana = ghana;
        this.filePath = filePath;

        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        root.setTop(buildTopBar(fileName, onReload));

        graphGroup = new Group();
        graphPane = new Pane(graphGroup);
        graphPane.setStyle("-fx-background-color: " + SURFACE + ";");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(graphPane.widthProperty());
        clip.heightProperty().bind(graphPane.heightProperty());
        graphPane.setClip(clip);

        graphPane.setOnScroll(ev -> {
            if (ev.isControlDown() || ev.isMetaDown()) {
                double delta = ev.getDeltaY() > 0 ? 1.1 : 0.9;
                double oldZoom = zoomFactor;
                zoomFactor = Math.max(0.1, Math.min(5.0, zoomFactor * delta));

                double mouseX = ev.getX();
                double mouseY = ev.getY();
                double scale = zoomFactor / oldZoom;
                panX = mouseX - scale * (mouseX - panX);
                panY = mouseY - scale * (mouseY - panY);

                applyTransform();
            } else {
                panX += ev.getDeltaX();
                panY += ev.getDeltaY();
                applyTransform();
            }
            ev.consume();
        });

        graphPane.setOnMousePressed(ev -> {
            if (ev.getTarget() == graphPane) {
                dragStartX = ev.getSceneX();
                dragStartY = ev.getSceneY();
                dragPanX = panX;
                dragPanY = panY;
            }
        });
        graphPane.setOnMouseClicked(ev -> {
            if (ev.getTarget() == graphPane && !ev.isDragDetect()) {
                resetAllNodes();
                selectedNode = null;
                selectedKey = null;
            }
        });
        graphPane.setOnMouseDragged(ev -> {
            if (ev.getTarget() == graphPane) {
                panX = dragPanX + (ev.getSceneX() - dragStartX);
                panY = dragPanY + (ev.getSceneY() - dragStartY);
                applyTransform();
            }
        });

        Button zoomInBtn = new Button("+");
        Button zoomOutBtn = new Button("-");
        String zoomBtnStyle = "-fx-background-color: " + BTN_BG + ";"
                + "-fx-text-fill: " + TEXT + ";"
                + "-fx-background-radius: 6;"
                + "-fx-padding: 4 10;"
                + "-fx-cursor: hand;"
                + "-fx-font-size: 16;"
                + "-fx-font-weight: bold;";
        zoomInBtn.setStyle(zoomBtnStyle);
        zoomOutBtn.setStyle(zoomBtnStyle);
        zoomInBtn.setOnAction(e -> applyZoomDelta(1.2));
        zoomOutBtn.setOnAction(e -> applyZoomDelta(0.8));

        VBox zoomBox = new VBox(4, zoomInBtn, zoomOutBtn);
        zoomBox.setPadding(new Insets(0, 10, 10, 0));
        zoomBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane graphStack = new StackPane(graphPane, zoomBox);
        StackPane.setAlignment(zoomBox, Pos.BOTTOM_RIGHT);

        root.setCenter(graphStack);
        root.setRight(buildSidebar());
        root.setBottom(buildToolbar());

        graphPane.widthProperty().addListener((obs, old, w) -> {
            lastViewW = w.doubleValue();
            tryInitialLayout();
        });
        graphPane.heightProperty().addListener((obs, old, h) -> {
            lastViewH = h.doubleValue();
            tryInitialLayout();
        });
    }

    private double dragStartX, dragStartY, dragPanX, dragPanY;
    private double panX = 0, panY = 0;

    private void applyTransform() {
        graphGroup.getTransforms().setAll(
                new Translate(panX, panY),
                new Scale(zoomFactor, zoomFactor, 0, 0));
    }

    private void applyZoomDelta(double delta) {
        double oldZoom = zoomFactor;
        zoomFactor = Math.max(0.1, Math.min(5.0, zoomFactor * delta));
        double cx = lastViewW / 2.0;
        double cy = lastViewH / 2.0;
        double scale = zoomFactor / oldZoom;
        panX = cx - scale * (cx - panX);
        panY = cy - scale * (cy - panY);
        applyTransform();
    }

    private void tryInitialLayout() {
        if (lastViewW > 100 && lastViewH > 100 && !laid) {
            laid = true;
            layoutGraph(lastViewW, lastViewH);
        }
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

        TextField searchField = new TextField();
        searchField.setPromptText("Search town...");
        searchField.setPrefWidth(180);
        searchField.setStyle("-fx-background-color: " + CARD + ";"
                + "-fx-text-fill: " + TEXT + ";"
                + "-fx-prompt-text-fill: #606080;"
                + "-fx-border-color: #2a3a5a;"
                + "-fx-border-radius: 15;"
                + "-fx-background-radius: 15;"
                + "-fx-padding: 5 12;");
        searchField.setFont(Font.font("System", 12));

        searchField.setOnAction(e -> {
            String query = searchField.getText().trim().toLowerCase();
            if (query.isEmpty()) return;

            String bestKey = null;
            for (Map.Entry<String, Town> entry : ghana.getTowns().entrySet()) {
                if (entry.getKey().equals(query)) {
                    bestKey = entry.getKey();
                    break;
                }
                if (entry.getKey().startsWith(query) && bestKey == null) {
                    bestKey = entry.getKey();
                }
                if (entry.getValue().getName().toLowerCase().contains(query)
                        && bestKey == null) {
                    bestKey = entry.getKey();
                }
            }

            if (bestKey != null) {
                Circle c = nodeCircles.get(bestKey);
                if (c != null) {
                    selectNode(c, bestKey);
                    searchField.setText(ghana.getTowns().get(bestKey).getName());
                }
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button reloadBtn = new Button("New File");
        styleActionBtn(reloadBtn, false);
        reloadBtn.setOnAction(e -> onReload.run());

        bar.getChildren().addAll(selectBtn, freeformBtn,
                new Separator(), fileLabel, statsLabel,
                spacer, searchField, reloadBtn);
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

    private Button editBtnRef;
    private Button addBtnRef;

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

        editBtnRef = editBtn;
        editBtn.setOnAction(e -> toggleEditMode());

        addBtnRef = addBtn;
        addBtn.setOnAction(e -> toggleAddMode());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(deleteBtn, editBtn, addBtn, pathBtn,
                spacer);
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

        double area = 4000.0 * n;
        double side = Math.sqrt(area);
        double k = Math.sqrt(area / n);
        double simCx = side / 2.0;
        double simCy = side / 2.0;
        double maxDrift = side * 0.45;

        Random rng = new Random(42);
        double[] posX = new double[n];
        double[] posY = new double[n];
        for (int i = 0; i < n; i++) {
            posX[i] = simCx + (rng.nextDouble() - 0.5) * side * 0.6;
            posY[i] = simCy + (rng.nextDouble() - 0.5) * side * 0.6;
        }

        Map<String, Integer> keyIndex = new HashMap<>();
        for (int i = 0; i < n; i++) keyIndex.put(keys.get(i), i);

        int iterations = 250;
        double temp = side * 0.10;
        double cooling = temp / (iterations + 1);

        for (int iter = 0; iter < iterations; iter++) {
            double[] dispX = new double[n];
            double[] dispY = new double[n];

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double dx = posX[i] - posX[j];
                    double dy = posY[i] - posY[j];
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 0.1) dist = 0.1;
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
                    if (dist < 0.1) dist = 0.1;
                    double attForce = (dist * dist) / k;
                    double fx = (dx / dist) * attForce;
                    double fy = (dy / dist) * attForce;
                    dispX[si] -= fx; dispY[si] -= fy;
                    dispX[di] += fx; dispY[di] += fy;
                }
            }

            double gravityStrength = 0.3;
            for (int i = 0; i < n; i++) {
                double gx = (simCx - posX[i]) * gravityStrength;
                double gy = (simCy - posY[i]) * gravityStrength;
                dispX[i] += gx;
                dispY[i] += gy;

                double dLen = Math.sqrt(dispX[i] * dispX[i] + dispY[i] * dispY[i]);
                if (dLen < 0.01) dLen = 0.01;
                double cap = Math.min(dLen, temp);
                posX[i] += (dispX[i] / dLen) * cap;
                posY[i] += (dispY[i] / dLen) * cap;

                double dFromCenter = Math.sqrt(
                        (posX[i] - simCx) * (posX[i] - simCx)
                        + (posY[i] - simCy) * (posY[i] - simCy));
                if (dFromCenter > maxDrift) {
                    posX[i] = simCx + (posX[i] - simCx) * maxDrift / dFromCenter;
                    posY[i] = simCy + (posY[i] - simCy) * maxDrift / dFromCenter;
                }
            }
            temp -= cooling;
        }

        double margin = 80;
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

        double canvasW = Math.max(1800, n * 20);
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
        panX = 0;
        panY = 0;
        applyTransform();
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

        double sz = 10;
        double baseX = tipX - ux * sz;
        double baseY = tipY - uy * sz;

        double perpX = -uy;
        double perpY = ux;

        Polygon arrow = new Polygon(
                tipX, tipY,
                baseX + perpX * sz * 0.4, baseY + perpY * sz * 0.4,
                baseX - perpX * sz * 0.4, baseY - perpY * sz * 0.4);
        arrow.setFill(Color.web("#5c6a8a", 0.7));
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

    private final Set<String> highlightedNeighborKeys = new HashSet<>();

    private void resetAllNodes() {
        if (selectedNode != null) {
            selectedNode.setStroke(Color.web("#ffffff"));
            selectedNode.setStrokeWidth(1.5);
            selectedNode.setRadius(NODE_RADIUS);
        }
        if (selectedKey != null) {
            Text selLbl = nodeLabels.get(selectedKey);
            if (selLbl != null)
                selLbl.setFont(Font.font("System", FontWeight.NORMAL, 10));
        }
        for (String nk : highlightedNeighborKeys) {
            Circle nc = nodeCircles.get(nk);
            if (nc != null) {
                nc.setStroke(Color.web("#ffffff"));
                nc.setStrokeWidth(1.5);
                nc.setRadius(NODE_RADIUS);
            }
            Text nLbl = nodeLabels.get(nk);
            if (nLbl != null)
                nLbl.setFont(Font.font("System", FontWeight.NORMAL, 10));
        }
        highlightedNeighborKeys.clear();

        edgeLines.forEach(l -> {
            l.setStroke(Color.web("#5c6a8a", 0.45));
            l.setStrokeWidth(0.8);
        });
        arrowHeads.forEach(a -> a.setFill(Color.web("#5c6a8a", 0.7)));
    }

    private void selectNode(Circle circle, String key) {
        resetAllNodes();

        selectedNode = circle;
        selectedKey = key;

        circle.setStroke(Color.web(ACCENT));
        circle.setStrokeWidth(3);
        circle.setRadius(NODE_RADIUS * 2);
        circle.toFront();

        Text lbl = nodeLabels.get(key);
        if (lbl != null) {
            lbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
            lbl.toFront();
        }

        HashMap<String, Town> towns = ghana.getTowns();
        Town srcTown = towns.get(key);
        if (srcTown == null) return;

        Set<String> neighborKeys = new HashSet<>();
        if (srcTown.getNeighbors() != null) {
            neighborKeys.addAll(srcTown.getNeighbors().keySet());
        }

        for (String nk : neighborKeys) {
            Circle nc = nodeCircles.get(nk);
            if (nc != null) {
                nc.setStroke(Color.web(ACCENT, 0.7));
                nc.setStrokeWidth(2.5);
                nc.setRadius(NODE_RADIUS * 1.6);
                nc.toFront();
            }
            Text nLbl = nodeLabels.get(nk);
            if (nLbl != null) {
                nLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
                nLbl.toFront();
            }
            highlightedNeighborKeys.add(nk);
        }

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
                }
                idx++;
            }
        }
        circle.toFront();
        if (lbl != null) lbl.toFront();

        if (editMode) {
            showEditPanel(key);
        }
    }

    // ------------------------------------------------------------------
    //  Edit mode
    // ------------------------------------------------------------------

    private void toggleEditMode() {
        if (addMode) toggleAddMode();
        editMode = !editMode;
        if (editMode) {
            editBtnRef.setStyle("-fx-background-color: " + ACCENT + ";"
                    + "-fx-text-fill: " + TEXT + ";"
                    + "-fx-background-radius: 8;"
                    + "-fx-padding: 6 14;"
                    + "-fx-cursor: hand;");

            VBox prompt = new VBox(12);
            prompt.setPrefWidth(280);
            prompt.setPadding(new Insets(20));
            prompt.setStyle("-fx-background-color: " + SURFACE + ";");

            Label heading = new Label("Edit Mode");
            heading.setFont(Font.font("System", FontWeight.BOLD, 16));
            heading.setStyle("-fx-text-fill: " + ACCENT + ";");

            Label hint = new Label("Click a node in the graph\nto edit its properties.");
            hint.setWrapText(true);
            hint.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 13;");

            prompt.getChildren().addAll(heading, hint);
            editPanel = prompt;
            root.setLeft(editPanel);
        } else {
            styleActionBtn(editBtnRef, true);
            editPanel = null;
            root.setLeft(null);
        }
    }

    private void showEditPanel(String key) {
        Town town = ghana.getTowns().get(key);
        if (town == null) return;

        VBox panel = new VBox(12);
        panel.setPrefWidth(300);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: " + SURFACE + ";");

        Label heading = new Label("Edit Town");
        heading.setFont(Font.font("System", FontWeight.BOLD, 16));
        heading.setStyle("-fx-text-fill: " + ACCENT + ";");

        Label nameLabel = new Label("Town Name");
        nameLabel.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 11;");
        TextField nameField = new TextField(town.getName());
        styleTextField(nameField);

        panel.getChildren().addAll(heading, nameLabel, nameField);

        Label nbHeading = new Label("Outgoing Edges");
        nbHeading.setFont(Font.font("System", FontWeight.BOLD, 13));
        nbHeading.setStyle("-fx-text-fill: " + TEXT + ";");
        nbHeading.setPadding(new Insets(8, 0, 0, 0));
        panel.getChildren().add(nbHeading);

        GridPane header = new GridPane();
        header.setHgap(6);
        Label hNeighbor = new Label("Neighbor");
        hNeighbor.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 10;");
        hNeighbor.setPrefWidth(120);
        Label hDist = new Label("Dist (km)");
        hDist.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 10;");
        hDist.setPrefWidth(65);
        Label hTime = new Label("Time (min)");
        hTime.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 10;");
        hTime.setPrefWidth(65);
        header.add(hNeighbor, 0, 0);
        header.add(hDist, 1, 0);
        header.add(hTime, 2, 0);
        panel.getChildren().add(header);

        HashMap<String, int[]> neighbors = town.getNeighbors();
        List<String> nbKeys = new ArrayList<>(neighbors.keySet());
        nbKeys.sort(String::compareTo);

        VBox edgeRows = new VBox(6);
        List<TextField[]> edgeFields = new ArrayList<>();

        for (String nbKey : nbKeys) {
            int[] edge = neighbors.get(nbKey);
            Town nbTown = ghana.getTowns().get(nbKey);
            String displayName = nbTown != null ? nbTown.getName() : nbKey;

            GridPane row = new GridPane();
            row.setHgap(6);

            Label nbLabel = new Label(displayName);
            nbLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12;");
            nbLabel.setPrefWidth(120);
            nbLabel.setWrapText(true);

            TextField distField = new TextField(String.valueOf(edge[0]));
            styleTextField(distField);
            distField.setPrefWidth(65);

            TextField timeField = new TextField(String.valueOf(edge[1]));
            styleTextField(timeField);
            timeField.setPrefWidth(65);

            row.add(nbLabel, 0, 0);
            row.add(distField, 1, 0);
            row.add(timeField, 2, 0);
            edgeRows.getChildren().add(row);

            edgeFields.add(new TextField[]{distField, timeField});
        }

        ScrollPane edgeScroll = new ScrollPane(edgeRows);
        edgeScroll.setFitToWidth(true);
        edgeScroll.setStyle("-fx-background: " + SURFACE + ";"
                + " -fx-border-color: transparent;");
        edgeScroll.setPrefHeight(350);
        VBox.setVgrow(edgeScroll, Priority.ALWAYS);
        panel.getChildren().add(edgeScroll);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        styleActionBtn(saveBtn, true);
        saveBtn.setStyle(saveBtn.getStyle()
                + "-fx-background-color: #1b7a3d;");
        styleActionBtn(cancelBtn, true);

        saveBtn.setOnAction(e -> handleSave(key, nameField, nbKeys, edgeFields));
        cancelBtn.setOnAction(e -> {
            editPanel = null;
            root.setLeft(null);
            toggleEditMode();
        });

        buttons.getChildren().addAll(saveBtn, cancelBtn);
        panel.getChildren().add(buttons);

        editPanel = panel;
        root.setLeft(editPanel);
    }

    private void handleSave(String originalKey, TextField nameField,
                            List<String> nbKeys, List<TextField[]> edgeFields) {
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            showError("Town name cannot be empty.");
            return;
        }

        for (int i = 0; i < nbKeys.size(); i++) {
            TextField[] fields = edgeFields.get(i);
            String distStr = fields[0].getText().trim();
            String timeStr = fields[1].getText().trim();
            int dist, time;
            try {
                dist = Integer.parseInt(distStr);
                time = Integer.parseInt(timeStr);
            } catch (NumberFormatException ex) {
                Town nb = ghana.getTowns().get(nbKeys.get(i));
                String label = nb != null ? nb.getName() : nbKeys.get(i);
                showError("Invalid number for edge to " + label + ".");
                return;
            }
            if (dist < 0 || time < 0) {
                Town nb = ghana.getTowns().get(nbKeys.get(i));
                String label = nb != null ? nb.getName() : nbKeys.get(i);
                showError("Distance/time for " + label + " cannot be negative.");
                return;
            }
        }

        Town town = ghana.getTowns().get(originalKey);
        if (town == null) return;

        try {
            String oldName = town.getName();
            if (!oldName.equals(newName)) {
                ghana.renameTown(oldName, newName);
            }

            for (int i = 0; i < nbKeys.size(); i++) {
                TextField[] fields = edgeFields.get(i);
                int dist = Integer.parseInt(fields[0].getText().trim());
                int time = Integer.parseInt(fields[1].getText().trim());
                ghana.updateEdge(newName, nbKeys.get(i), dist, time);
            }

            ghana.saveToFile(filePath);

            editPanel = null;
            root.setLeft(null);
            if (editMode) toggleEditMode();

            layoutGraph(lastViewW, lastViewH);

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (IOException ex) {
            showError("Failed to save file: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Edit Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void styleTextField(TextField field) {
        field.setStyle("-fx-background-color: " + CARD + ";"
                + "-fx-text-fill: " + TEXT + ";"
                + "-fx-border-color: #2a3a5a;"
                + "-fx-border-radius: 4;"
                + "-fx-background-radius: 4;"
                + "-fx-padding: 4 8;");
        field.setFont(Font.font("System", 12));
    }

    private ListCell<String> makeComboCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + CARD + ";");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: " + CARD + ";"
                            + "-fx-text-fill: " + TEXT + ";"
                            + "-fx-font-size: 12;");
                }
            }
        };
    }

    // ------------------------------------------------------------------
    //  Add mode
    // ------------------------------------------------------------------

    private void toggleAddMode() {
        if (editMode) toggleEditMode();
        addMode = !addMode;
        if (addMode) {
            addBtnRef.setStyle("-fx-background-color: " + ACCENT + ";"
                    + "-fx-text-fill: " + TEXT + ";"
                    + "-fx-background-radius: 8;"
                    + "-fx-padding: 6 14;"
                    + "-fx-cursor: hand;");
            showAddPanel();
        } else {
            styleActionBtn(addBtnRef, true);
            editPanel = null;
            root.setLeft(null);
        }
    }

    private void showAddPanel() {
        VBox panel = new VBox(12);
        panel.setPrefWidth(320);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: " + SURFACE + ";");

        Label heading = new Label("Add New Town");
        heading.setFont(Font.font("System", FontWeight.BOLD, 16));
        heading.setStyle("-fx-text-fill: " + ACCENT + ";");

        Label nameLabel = new Label("Town Name");
        nameLabel.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 11;");
        TextField nameField = new TextField();
        nameField.setPromptText("Enter town name");
        styleTextField(nameField);

        Label connHeading = new Label("Outgoing Connections");
        connHeading.setFont(Font.font("System", FontWeight.BOLD, 13));
        connHeading.setStyle("-fx-text-fill: " + TEXT + ";");
        connHeading.setPadding(new Insets(8, 0, 0, 0));

        GridPane header = new GridPane();
        header.setHgap(4);
        Label hNb = new Label("Neighbor");
        hNb.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 10;");
        hNb.setPrefWidth(120);
        Label hDist = new Label("Dist (km)");
        hDist.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 10;");
        hDist.setPrefWidth(55);
        Label hTime = new Label("Time (min)");
        hTime.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 10;");
        hTime.setPrefWidth(55);
        header.add(hNb, 0, 0);
        header.add(hDist, 1, 0);
        header.add(hTime, 2, 0);

        VBox connRows = new VBox(6);
        List<Object[]> rowData = new ArrayList<>();

        List<String> townNames = new ArrayList<>();
        for (Town t : ghana.getTowns().values()) {
            townNames.add(t.getName());
        }
        townNames.sort(String::compareTo);

        Runnable addRow = () -> {
            GridPane row = new GridPane();
            row.setHgap(4);

            ComboBox<String> combo = new ComboBox<>(
                    FXCollections.observableArrayList(townNames));
            combo.setPromptText("Select town");
            combo.setPrefWidth(120);
            combo.setStyle("-fx-background-color: " + CARD + ";"
                    + "-fx-font-size: 11;");
            combo.setButtonCell(makeComboCell());
            combo.setCellFactory(lv -> makeComboCell());

            TextField distField = new TextField();
            distField.setPromptText("km");
            styleTextField(distField);
            distField.setPrefWidth(55);

            TextField timeField = new TextField();
            timeField.setPromptText("min");
            styleTextField(timeField);
            timeField.setPrefWidth(55);

            Button removeBtn = new Button("x");
            removeBtn.setStyle("-fx-background-color: #5c2030;"
                    + "-fx-text-fill: " + TEXT + ";"
                    + "-fx-background-radius: 4;"
                    + "-fx-padding: 3 7;"
                    + "-fx-cursor: hand;"
                    + "-fx-font-size: 11;");

            Object[] entry = new Object[]{combo, distField, timeField, row};
            rowData.add(entry);

            removeBtn.setOnAction(ev -> {
                connRows.getChildren().remove(row);
                rowData.remove(entry);
            });

            row.add(combo, 0, 0);
            row.add(distField, 1, 0);
            row.add(timeField, 2, 0);
            row.add(removeBtn, 3, 0);
            connRows.getChildren().add(row);
        };

        Button addConnBtn = new Button("+ Add Connection");
        addConnBtn.setStyle("-fx-background-color: " + CARD + ";"
                + "-fx-text-fill: " + ACCENT + ";"
                + "-fx-background-radius: 6;"
                + "-fx-padding: 6 14;"
                + "-fx-cursor: hand;"
                + "-fx-font-size: 12;");
        addConnBtn.setOnAction(ev -> addRow.run());

        addRow.run();

        ScrollPane connScroll = new ScrollPane(connRows);
        connScroll.setFitToWidth(true);
        connScroll.setStyle("-fx-background: " + SURFACE + ";"
                + " -fx-border-color: transparent;");
        connScroll.setPrefHeight(300);
        VBox.setVgrow(connScroll, Priority.ALWAYS);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        styleActionBtn(saveBtn, true);
        saveBtn.setStyle(saveBtn.getStyle()
                + "-fx-background-color: #1b7a3d;");
        styleActionBtn(cancelBtn, true);

        saveBtn.setOnAction(e -> handleAddSave(nameField, rowData));
        cancelBtn.setOnAction(e -> toggleAddMode());

        buttons.getChildren().addAll(saveBtn, cancelBtn);

        panel.getChildren().addAll(heading, nameLabel, nameField,
                connHeading, header, connScroll, addConnBtn, buttons);

        editPanel = panel;
        root.setLeft(editPanel);
    }

    @SuppressWarnings("unchecked")
    private void handleAddSave(TextField nameField, List<Object[]> rowData) {
        String townName = nameField.getText().trim();
        if (townName.isEmpty()) {
            showError("Town name cannot be empty.");
            return;
        }

        String key = townName.trim().toLowerCase();
        if (ghana.getTowns().containsKey(key)) {
            showError("A town named \"" + townName + "\" already exists.");
            return;
        }

        List<String> neighbors = new ArrayList<>();
        List<int[]> edgeValues = new ArrayList<>();

        for (int i = 0; i < rowData.size(); i++) {
            Object[] entry = rowData.get(i);
            ComboBox<String> combo = (ComboBox<String>) entry[0];
            TextField distField = (TextField) entry[1];
            TextField timeField = (TextField) entry[2];

            String selected = combo.getValue();
            if (selected == null || selected.isEmpty()) {
                showError("Connection row " + (i + 1)
                        + ": please select a neighbor town.");
                return;
            }

            String distStr = distField.getText().trim();
            String timeStr = timeField.getText().trim();
            if (distStr.isEmpty() || timeStr.isEmpty()) {
                showError("Connection row " + (i + 1)
                        + ": distance and time are required.");
                return;
            }

            int dist, time;
            try {
                dist = Integer.parseInt(distStr);
                time = Integer.parseInt(timeStr);
            } catch (NumberFormatException ex) {
                showError("Connection row " + (i + 1)
                        + ": distance and time must be numbers.");
                return;
            }
            if (dist < 0 || time < 0) {
                showError("Connection row " + (i + 1)
                        + ": distance and time cannot be negative.");
                return;
            }

            if (neighbors.contains(selected.trim().toLowerCase())) {
                showError("Duplicate connection to \"" + selected + "\".");
                return;
            }

            neighbors.add(selected.trim().toLowerCase());
            edgeValues.add(new int[]{dist, time});
        }

        try {
            ghana.addTown(townName);

            for (int i = 0; i < neighbors.size(); i++) {
                String nbDisplay = ((ComboBox<String>) rowData.get(i)[0]).getValue();
                int[] vals = edgeValues.get(i);
                ghana.updateEdge(townName, nbDisplay, vals[0], vals[1]);
            }

            ghana.saveToFile(filePath);

            editPanel = null;
            root.setLeft(null);
            if (addMode) toggleAddMode();

            layoutGraph(lastViewW, lastViewH);

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (IOException ex) {
            showError("Failed to save file: " + ex.getMessage());
        }
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
