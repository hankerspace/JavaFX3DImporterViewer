package fr.utbm;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ResourceBundle;

/**
 * Controller for Viewer.fxml.
 * 
 * <p>
 * The buttons and controls of the viewer are configured here.
 * </p>
 */
public class ViewerController implements Initializable {
    
    private static final String NEAR_CLIP_TOOLTIP_TEXT = "Camera near-clip value";
    private static final String FAR_CLIP_TOOLTIP_TEXT = "Camera far-clip value";
    
    private static final String SUPPORTED_FILES = "Supported files";
    private static final String SELECT_FILE_TO_LOAD = "Select file to load";
    
    private static final double MIN_NEAR_CLIP = 0.01;
    private static final double MAX_NEAR_CLIP = 10;
    private static final double MIN_FAR_CLIP = 100;
    private static final double MAX_FAR_CLIP = 1e7;
    
    @FXML
    private Pane outerPane;
    @FXML
    private SubSceneContainer subSceneContainer;
    @FXML
    private HBox controlsOverlay;
    @FXML
    private Button openButton;
    @FXML
    private ToggleButton rotateButton;
    @FXML
    private Label nearClipLabel;
    @FXML
    private Label farClipLabel;
    @FXML
    private Slider nearClipSlider;
    @FXML
    private Slider farClipSlider;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label status;
    @FXML
    private Label fpsLabel;

    private File loadedPath;
    private ViewerModel model;

    private final long[] frameTimes = new long[100];
    private int frameTimeIndex = 0 ;
    private boolean arrayFilled = false ;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        // FPS counter
        AnimationTimer frameRateMeter = new AnimationTimer() {

            @Override
            public void handle(long now) {
                long oldFrameTime = frameTimes[frameTimeIndex] ;
                frameTimes[frameTimeIndex] = now ;
                frameTimeIndex = (frameTimeIndex + 1) % frameTimes.length ;
                if (frameTimeIndex == 0) {
                    arrayFilled = true ;
                }
                if (arrayFilled) {
                    long elapsedNanos = now - oldFrameTime ;
                    long elapsedNanosPerFrame = elapsedNanos / frameTimes.length ;
                    double frameRate = 1_000_000_000.0 / elapsedNanosPerFrame ;
                    fpsLabel.setText(String.format("%.3f", frameRate));
                }
            }
        };
        frameRateMeter.start();

        model = new ViewerModel();
        subSceneContainer.setSubScene(model.getSubScene());

        subSceneContainer.prefWidthProperty().bind(outerPane.widthProperty());
        subSceneContainer.prefHeightProperty().bind(outerPane.heightProperty());
        
        controlsOverlay.prefWidthProperty().bind(subSceneContainer.widthProperty().subtract(20));
        
        rotateButton.disableProperty().bind(model.contentProperty().isNull());
        
        initializeClipSliders();
        initializeProgressIndicator();
     
        addDragDropHandlers();

        loadSample();


    }

    /**
     * Initializes the sliders that set the camera near and far clip values.
     */
    private void initializeClipSliders() {

        nearClipSlider.setMin(Math.log10(MIN_NEAR_CLIP));
        nearClipSlider.setMax(Math.log10(MAX_NEAR_CLIP));
        nearClipSlider.setValue(Math.log10(model.getCamera().getNearClip()));
        
        farClipSlider.setMin(Math.log10(MIN_FAR_CLIP));
        farClipSlider.setMax(Math.log10(MAX_FAR_CLIP));
        farClipSlider.setValue(Math.log10(model.getCamera().getFarClip()));
        
        nearClipSlider.setTooltip(new Tooltip(NEAR_CLIP_TOOLTIP_TEXT));
        farClipSlider.setTooltip(new Tooltip(FAR_CLIP_TOOLTIP_TEXT));
        
        model.getCamera().nearClipProperty().bind(new Power10DoubleBinding(nearClipSlider.valueProperty()));
        model.getCamera().farClipProperty().bind(new Power10DoubleBinding(farClipSlider.valueProperty()));

        nearClipLabel.textProperty().bind(model.getCamera().nearClipProperty().asString());
        farClipLabel.textProperty().bind(model.getCamera().farClipProperty().asString());
    }
    
    /**
     * Initializes the progress indicator.
     */
    private void initializeProgressIndicator() {
        
        progressIndicator.setVisible(false);        
        
        progressIndicator.layoutXProperty().bind(subSceneContainer.widthProperty().divide(2).subtract(15));
        progressIndicator.layoutYProperty().bind(subSceneContainer.heightProperty().divide(2).subtract(15));
    }
    
    /**
     * Adds handlers for dragging and dropping files into the viewer.
     */
    private void addDragDropHandlers() {
        
        final String[] supportedFormatRegex = Importer3D.getSupportedFormatExtensionFilters();
        
        for (int i = 0; i < supportedFormatRegex.length; i++) {
            supportedFormatRegex[i] = "." + supportedFormatRegex[i].replaceAll("\\.", "\\.");
        }
        
        model.getSubScene().setOnDragOver(new EventHandler<DragEvent>() {
            
            @Override
            public void handle(final DragEvent event) {
                Dragboard db = event.getDragboard();
                if (db.hasFiles()) {
                    boolean hasSupportedFile = false;
                    fileLoop: for (File file : db.getFiles()) {
                        for (String format : supportedFormatRegex) {
                            if (file.getName().toLowerCase().matches(format)) {
                                hasSupportedFile = true;
                                break fileLoop;
                            }
                        }
                    }
                    if (hasSupportedFile) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }
                }
                event.consume();
            }
        });
        
        model.getSubScene().setOnDragDropped(new EventHandler<DragEvent>() {
            
            @Override
            public void handle(final DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    File supportedFile = null;
                    fileLoop: for (File file : db.getFiles()) {
                        for (String format : supportedFormatRegex) {
                            if (file.getName().toLowerCase().matches(format)) {
                                supportedFile = file;
                                break fileLoop;
                            }
                        }
                    }
                    if (supportedFile != null) {
                        if (supportedFile.getAbsolutePath().indexOf('%') != -1) {
                            try {
                                supportedFile = new File(URLDecoder.decode(supportedFile.getAbsolutePath(), "utf-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        load(supportedFile);
                    }
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            }
        });
    }
    
    /**
     * Try to load a sample FBX file.
     */
    private void loadSample() {

        File sample = new File("D:\\pupitre.3ds");
        
        if (sample.exists()) {
            load(sample);
        }
    }
    
    @FXML
    private void open() {
        
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(SUPPORTED_FILES, Importer3D.getSupportedFormatExtensionFilters()));
        if (loadedPath != null && loadedPath.exists()) {
            chooser.setInitialDirectory(loadedPath.getAbsoluteFile().getParentFile());
        }
        chooser.setTitle(SELECT_FILE_TO_LOAD);
        
        File newFile = chooser.showOpenDialog(subSceneContainer.getScene().getWindow());
        
        if (newFile != null) {
            load(newFile);
        }
    }
    
    @FXML
    private void toggleRotation() {
        model.toggleRotation();
    }
    
    /**
     * 
     * Attemps to load a file using {@link Importer3D}.
     * 
     * <p>
     * The loading is done in a background thread so the viewer doesn't appear to hang.
     * </p>
     * 
     * @param file the file to be loaded
     */
    private void load(final File file) {
        
        loadedPath = file;

        updateStatus("");
        disableControls(true);
        model.setContent(null);
        progressIndicator.setVisible(true);
        
        new Thread(new Runnable() {
            
            @Override public void run() {
                try {
                    Group content = Importer3D.load(file.toURI().toURL().toString());
                    handleLoadResult(content, "Loaded file " + loadedPath);
                } catch (OutOfMemoryError e) {
                    handleLoadResult(null, "Not enough memory to load file " + loadedPath);
                    e.printStackTrace();
                } catch (UnsatisfiedLinkError e) {
                    handleLoadResult(null, "Dependency jfbxlib could not be loaded");
                    e.printStackTrace();
                } catch (Throwable e) {
                    handleLoadResult(null, "Failed to load file " + loadedPath);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Updates the status bar text with the given string.
     * 
     * @param text the text to appear in the status bar
     */
    private void updateStatus(final String text) {
        status.setText(text);
    }
    
    /**
     * Handles the result of the load action.
     * 
     * @param content the loaded content
     * @param status the new status text
     */
    private void handleLoadResult(final Group content, final String status) {
        
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                model.setContent(content);
                updateStatus(status);
                disableControls(false);
                progressIndicator.setVisible(false);
            }
        });
    }
    
    private void disableControls(boolean disabled) {
        
        openButton.setDisable(disabled);
        nearClipSlider.setDisable(disabled);
        farClipSlider.setDisable(disabled);
    }
    
    private class Power10DoubleBinding extends DoubleBinding {

        private final DoubleProperty prop;

        public Power10DoubleBinding(final DoubleProperty prop) {
            this.prop = prop;
            bind(prop);
        }

        @Override
        protected double computeValue() {
            return Math.pow(10, prop.getValue());
        }
    }
}
