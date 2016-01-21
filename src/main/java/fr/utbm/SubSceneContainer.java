package fr.utbm;

import javafx.scene.SubScene;
import javafx.scene.layout.Pane;

/**
 * A container for a {@link SubScene} that resizes the sub-scene to fit its own size.
 */
public class SubSceneContainer extends Pane {
    
    private SubScene subScene;

    /**
     * Sets the sub-scene to be displayed inside this container.
     * 
     * @param subScene the {@link SubScene} of this container
     */
    public void setSubScene(final SubScene subScene) {

        this.subScene = subScene;
        getChildren().add(subScene);
    }

    @Override
    protected void layoutChildren() {
        
        final double width = getWidth();
        final double height = getHeight();

        super.layoutChildren();

        subScene.setWidth(width);
        subScene.setHeight(height);
    }
}
