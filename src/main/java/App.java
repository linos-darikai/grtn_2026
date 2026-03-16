import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the Ghana Road Transport Network application.
 * Manages the primary stage and switches between the landing (load) screen
 * and the main graph-visualization screen.
 */
public class App extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Ghana Road Transport Network");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);

        showLoadScreen();
        primaryStage.show();
    }

    void showLoadScreen() {
        LoadScreen loadScreen = new LoadScreen(this::onDataLoaded, primaryStage);
        Scene scene = new Scene(loadScreen.getRoot(), 1100, 700);
        primaryStage.setScene(scene);
    }

    private void onDataLoaded(Ghana ghana, String fileName) {
        MainScreen mainScreen = new MainScreen(ghana, fileName, this::showLoadScreen);
        Scene scene = new Scene(mainScreen.getRoot(), 1200, 800);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
