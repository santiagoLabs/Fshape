package application;
	
import org.opencv.core.Core;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			// Caricamento visual design GUI
			FXMLLoader loader = new FXMLLoader(getClass().getResource("GraficaDetenction.fxml"));
			// Creazione della finestra
			BorderPane rootElement = (BorderPane) loader.load();
			// Caricamento del controllo della GUI
			FXController controller = loader.getController();
			controller.init();
			// Creazione scena con misure 700x401
			Scene scene = new Scene(rootElement,700,401);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			Image applicationIcon = new Image(getClass().getResourceAsStream("icon.png"));
			primaryStage.getIcons().add(applicationIcon);
			// Titolo della finestra
			primaryStage.setTitle("FSHAPE");
			primaryStage.setScene(scene);
			primaryStage.show();
		} 
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// Caricamento della librerie di OpenCV
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		launch(args);
	}
}
