package application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXSlider;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class FXController extends Thread implements Initializable,ChangeListener{
	
	// Inizializzazione variabili
	@FXML
	private JFXButton btn_start;
	@FXML
	private JFXCheckBox haarClassifier;
	@FXML
	private JFXCheckBox chkDati;
	@FXML
	private ImageView originalFrame;
	@FXML
	private JFXSlider slider;
	

	private ScheduledExecutorService timer;
	private VideoCapture capture;
	private boolean cameraActive;
	
	private CascadeClassifier faceCascade;
	private int absoluteFaceSize;
	private int n_volti;
	private int p_volti;
	private int a;
	
	// Inizializzazione Socket
	Socket socket=null;
	BufferedReader in=null;
	PrintWriter out=null;
	
	protected void init(){
		this.capture = new VideoCapture();
		this.faceCascade = new CascadeClassifier();
		this.absoluteFaceSize = 0;
		this.n_volti = 0;
		this.p_volti = 0;
	}
	
	// Funzione relativa al pulsante di start
	@FXML
	protected void startCamera(ActionEvent event){
		originalFrame.setFitWidth(600);
		originalFrame.setPreserveRatio(true);
		if(!this.cameraActive){
			this.haarClassifier.setDisable(true);
			this.capture.open(0);
			if(this.capture.isOpened()){
				this.cameraActive= true;
				Runnable frameGrabber = new Runnable(){
					@Override
					public void run(){
						Image imageToShow = grabFrame();
						originalFrame.setImage(imageToShow);
					}
				};
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				this.btn_start.setText("Stop Camera");
			}
			else{
				System.err.println("Impossibile aprire la webcam..");
			}
			
		}
		else{
			this.cameraActive = false;
			this.btn_start.setText("Start Camera");
			this.haarClassifier.setDisable(false);
		
			try{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e){
				System.err.println("Impossibile chiudere la webcam, prova ora...... " + e);
			}
			this.capture.release();
			this.originalFrame.setImage(null);
		}
	}
	
	private Image grabFrame(){
	
		Image imageToShow = null;
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened()){
			try{
				// Memorizzare il frame corrente
				this.capture.read(frame);
				// Se il frame non è vuoto si processa
				if (!frame.empty()){
					// face detection
					this.detectAndDisplay(frame);
					// Coneverte oggetto Mat (OpenCV) a Image (JavaFX)
					imageToShow = mat2Image(frame);
				}
			}
			catch (Exception e){
				
				System.err.println("ERROR: " + e);
			}
		}
		return imageToShow;
	}
	
	// Rilevamento volti
	private void  detectAndDisplay(Mat frame) throws IOException{
		MatOfRect faces = new MatOfRect();
		Mat grayFrame = new Mat();
		
		// Processazione dei frame in scala di grigi
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		// Aumento della qualità
		Imgproc.equalizeHist(grayFrame, grayFrame);
		// Calcolo della minima misura del volto
		if(this.absoluteFaceSize == 0){
			int height = grayFrame.rows();
			if(Math.round(height * 0.2f) > 0){
				this.absoluteFaceSize = Math.round(height * 0.2f);
			}
		}
		// Rilevazione volto
		this.faceCascade.detectMultiScale(grayFrame, faces,1.1,2,0 | Objdetect.CASCADE_SCALE_IMAGE,
		          new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());
	  	Rect[] facesArray = faces.toArray();
	  	// Applicazione della censura
	  	for(Rect rect : faces.toArray()){
	  		//Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0,0,255),3);
	  		Rect rectCrop = new Rect(rect.x, rect.y , rect.width, rect.height);
	  		Mat imageROI = new Mat(frame,rectCrop);
	  		Imgproc.GaussianBlur(imageROI, imageROI, new Size(55, 55), a);
	  	}
	  	
		n_volti = facesArray.length;
		// Invio dati al server e stampa della risposta
		/*if(p_volti != n_volti){
			out.println(n_volti);
			System.out.println(in.readLine());
		}
		p_volti = n_volti;*/
	}

	@FXML
	protected void haarSelected(Event event){
		this.checkboxSelection("Resources/haarcascades/haarcascade_frontalface_alt.xml");
	}
	
	
	/*
	@FXML
	protected void invioDati(ActionEvent event){
		try{
			// creazione socket
		
			socket = new Socket("127.0.0.1", 4005);
			System.out.println("Client avviato");
			System.out.println("Client Socket: "+ socket);
		
			// creazione stream di input da socket
		
			InputStreamReader isr = new InputStreamReader( socket.getInputStream());
			in = new BufferedReader(isr);
		
			// creazione stream di output su socket
			
			OutputStreamWriter osw = new OutputStreamWriter( socket.getOutputStream());
			BufferedWriter bw = new BufferedWriter(osw);
			out = new PrintWriter(bw, true);
			
		}
		catch(UnknownHostException e){
			System.err.println("Don’t know about host " + e);
			System.exit(1);
		} 
		catch (IOException e){
			System.err.println("Impossibile stabilire connessione con il server......" ); // PROBLEMA DA RISOLVERE
			System.exit(1);
		}
	}*/
	
	private void checkboxSelection(String classifierPath){
		this.faceCascade.load(classifierPath);
		this.btn_start.setDisable(false);
	}
	
	private Image mat2Image(Mat frame){
		MatOfByte buffer = new MatOfByte();
		Imgcodecs.imencode(".png", frame, buffer);
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		slider.valueProperty().addListener(this);
	}
	@Override
	public void changed(ObservableValue arg0, Object arg1, Object arg2) {
		
		 a = (int) slider.getValue();
	}

	
}

