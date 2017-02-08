package aim4.gui.screen;

import aim4.gui.Viewer;
import aim4.gui.viewer.SimViewer;
import aim4.map.BasicMap;

import java.awt.*;

/**
 * Created by Callum on 08/02/2017.
 */
public class MergeStatScreen extends Canvas {

    /**
     * Create a new canvas.
     *
     * @param simViewer the simViewer object
     * @param viewer
     */
    public MergeStatScreen(SimViewer simViewer, Viewer viewer) {
        super(simViewer, viewer);
    }

    @Override
    protected Image createMapImage(BasicMap map, double scale) {
        return null;
    }

    @Override
    protected void drawTracks(Graphics2D buffer) {

    }
}
