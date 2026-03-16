import java.io.File;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Landing screen with a file-chooser "Load Data" button.
 * On successful load, invokes the callback with the populated Ghana instance.
 */
public class LoadScreen {

    private static final String BG_COLOR = "#1a1a2e";
    private static final String ACCENT = "#e94560";
    private static final String BTN_BG = "#0f3460";

    private final VBox root;

    LoadScreen(App.LoadCallback onLoaded, Stage ownerStage) {
        root = new VBox(24);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        Label icon = new Label("\u2B22");
        icon.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-size: 64;");

        Label title = new Label("Ghana Road Transport Network");
        title.setFont(Font.font("System", FontWeight.BOLD, 32));
        title.setStyle("-fx-text-fill: #ffffff;");

        Label subtitle = new Label("Load a .csv or .txt dataset to begin");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 16));
        subtitle.setStyle("-fx-text-fill: #a0a0b8;");

        Button loadBtn = new Button("Load Data");
        loadBtn.setPrefWidth(220);
        loadBtn.setPrefHeight(50);
        loadBtn.setFont(Font.font("System", FontWeight.BOLD, 16));
        loadBtn.setStyle(buttonStyle(false));
        loadBtn.setOnMouseEntered(e -> loadBtn.setStyle(buttonStyle(true)));
        loadBtn.setOnMouseExited(e -> loadBtn.setStyle(buttonStyle(false)));

        Label hint = new Label("Supports .csv and .txt graph files");
        hint.setStyle("-fx-text-fill: #606080; -fx-font-size: 12;");

        loadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Graph Data File");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Graph files", "*.csv", "*.txt"),
                    new FileChooser.ExtensionFilter("CSV files", "*.csv"),
                    new FileChooser.ExtensionFilter("Text files", "*.txt"));

            File file = fc.showOpenDialog(ownerStage);
            if (file == null) return;

            Ghana ghana = new Ghana();
            try {
                ghana.loadTowns(file.getPath());
                onLoaded.onLoaded(ghana, file.getName(), file.getPath());
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Load Error");
                alert.setHeaderText("Failed to load data file");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });

        root.getChildren().addAll(icon, title, subtitle, loadBtn, hint);
    }

    Parent getRoot() {
        return root;
    }

    private String buttonStyle(boolean hover) {
        String bg = hover ? "#163d73" : BTN_BG;
        return "-fx-background-color: " + bg + ";"
                + "-fx-text-fill: #ffffff;"
                + "-fx-background-radius: 25;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,52,96,0.5), 12, 0, 0, 4);";
    }
}
