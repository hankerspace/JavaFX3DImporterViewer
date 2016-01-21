package fr.utbm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * @author $Author: tmartinet$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public class JavaFX3DsImporter extends Application {

    @Override
    public void start(Stage stage) throws IOException {

//        final URL location = getClass().getResource("FbxViewer.fxml");
//        final FXMLLoader loader = new FXMLLoader();
//        final VBox root = (VBox) loader.load(location.openStream());

        final Parent root = FXMLLoader.load(getClass().getResource("FbxViewer.fxml"));
        final Scene scene = new Scene(root, 800, 600, true);

        stage.setScene(scene);
        stage.setTitle("FBX Viewer");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
