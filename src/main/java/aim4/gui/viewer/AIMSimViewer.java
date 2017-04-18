package aim4.gui.viewer;

import aim4.config.Debug;
import aim4.gui.screen.aim.AIMCanvas;
import aim4.gui.StatusPanelContainer;
import aim4.gui.Viewer;
import aim4.gui.frame.VehicleInfoFrame;
import aim4.gui.setuppanel.AIMSimSetupPanel;
import aim4.im.aim.IntersectionManager;
import aim4.map.Road;
import aim4.map.lane.Lane;
import aim4.sim.simulator.aim.AutoDriverOnlySimulator;
import aim4.sim.Simulator;
import aim4.sim.UdpListener;
import aim4.sim.simulator.aim.AIMSimulator;
import aim4.sim.setup.aim.BasicSimSetup;
import aim4.vehicle.aim.AIMVehicleSimModel;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * Created by Callum on 09/11/2016.
 */
public class AIMSimViewer extends SimViewer {
    /**
     * UDP listener
     */
    private UdpListener udpListener;

    public AIMSimViewer(StatusPanelContainer statusPanel, Viewer viewer){
        super(statusPanel, viewer, new AIMSimSetupPanel(new BasicSimSetup(1, // columns
                1, // rows
                4, // lane width
                25.0, // speed limit
                3, // lanes per road
                1, // median size
                150, // distance between
                0.28, // traffic level
                1.0 // stop distance before intersection
        )), true);
        this.udpListener = null;
    }

    @Override
    protected Simulator.SimStepResult runSimulationStep() {
        Simulator.SimStepResult simStepResult = super.runSimulationStep();
        if (simStepResult instanceof AutoDriverOnlySimulator.AutoDriverOnlySimStepResult) {
            AutoDriverOnlySimulator.AutoDriverOnlySimStepResult simStepResult2 =
                    (AutoDriverOnlySimulator.AutoDriverOnlySimStepResult) simStepResult;
            for (int vin : simStepResult2.getCompletedVINs()) {
                Debug.removeVehicleColor(vin);
            }
        }
        return simStepResult;
    }

    @Override
    protected void createCanvas(Viewer viewer) {
        canvas = new AIMCanvas(this, viewer);

        // Make self key listener
        setFocusable(true);
        requestFocusInWindow();
    }

    /**
     * Set whether or not the canvas draws the IM shapes.
     *
     * @param useIMDebugShapes  whether or not the canvas should draw the shapes
     */
    public void setIsShowIMDebugShapes(boolean useIMDebugShapes) {
        ((AIMCanvas) this.canvas).setIsShowIMDebugShapes(useIMDebugShapes);
    }

    // /////////////////////////////////////
    // UDP listening
    // /////////////////////////////////////
    /**
     * Start the UDP listening.
     */
    public void startUdpListening() {
        assert sim instanceof AIMSimulator;
        if (sim != null) {
            if (Debug.SHOW_PROXY_VEHICLE_DEBUG_MSG) {
                System.err.print("Starting UDP listener...\n");
            }
            // create the UDP listener thread
            udpListener = new UdpListener((AIMSimulator) sim);
            udpListener.start();
        } else {
            System.err.printf("Must start the simulator before starting "
                    + "UdpListener.\n");
        }
    }

    /**
     * Stop the UDP listening
     */
    public void stopUdpListening() {
        if (Debug.SHOW_PROXY_VEHICLE_DEBUG_MSG) {
            System.err.print("Stopping UDP listener...\n");
        }
        udpListener.stop();
    }

    /**
     * Sets the udpListener to null.
     */
    public void removeUdpListener() {
        udpListener = null;
    }

    /**
     * Returns a boolean indicating whether the udpListener has started.
     * @return A boolean indicating whether the udpListener has started.
     */
    public boolean udpListenerHasStarted() {
        return udpListener.hasStarted();
    }

    protected void runBeforeCreatingSimulator() {
        assert udpListener == null;
        assert sim instanceof AIMSimulator;
    }

    protected void runBeforeResettingSimulator() {
        if (udpListener != null) {
            stopUdpListening();
        }
    }

    // ///////////////////////////////
    // MouseListener interface
    // ///////////////////////////////
    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO: may be move this function to canvas.
        // right click
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (sim != null) {
                assert sim instanceof AIMSimulator;
                Point2D leftClickPoint = canvas.getMapPosition(e.getX(), e.getY());
                // See if we hit any vehicles
                for (AIMVehicleSimModel vehicle : ((AIMSimulator) sim).getActiveVehicles()) {
                    if (vehicle.getShape().contains(leftClickPoint)) {
                        if (Debug.getTargetVIN() != vehicle.getVIN()) {
                            Debug.setTargetVIN(vehicle.getVIN());
                            if (vehicleInfoFrame == null) {
                                vehicleInfoFrame = new VehicleInfoFrame(this);
                            }
                            if (!vehicleInfoFrame.isVisible()) {
                                vehicleInfoFrame.setVisible(true);
                                this.requestFocusInWindow();
                                this.requestFocus();
                            }
                            vehicleInfoFrame.setVehicle(vehicle);
                        } else {
                            Debug.removeTargetVIN();
                            vehicleInfoFrame.setVehicle(null);
                        }
                        canvas.update();
                        return;  // just exit
                    }
                }
                // see if we hit any intersection
                for (IntersectionManager im : ((AIMSimulator)sim).getMap().getIntersectionManagers()) {
                    if (im.getIntersection().getArea().contains(leftClickPoint)) {
                        if (Debug.getTargetIMid() != im.getId()) {
                            Debug.setTargetIMid(im.getId());
                        } else {
                            Debug.removeTargetIMid();
                        }
                        canvas.cleanUp();  // TODO: ugly code, one more reason to move this
                        // function to canvas
                        canvas.update();
                        return;  // just exit
                    }
                }
                // hit nothing, just unselect the vehicle and intersection manager.
                Debug.removeTargetVIN();
                if (vehicleInfoFrame != null) {
                    vehicleInfoFrame.setVehicle(null);
                }
                Debug.removeTargetIMid();
                canvas.cleanUp();
                canvas.update();
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            if (sim != null) {
                Point2D rightClickPoint = canvas.getMapPosition(e.getX(), e.getY());
                System.err.printf("Right click at (%.0f, %.0f)\n",
                        rightClickPoint.getX(), rightClickPoint.getY());
                // print the lane id
                for (Road r : sim.getMap().getRoads()) {
                    for (Lane l : r.getLanes()) {
                        if (l.getShape().contains(rightClickPoint)) {
                            System.err.printf("Right click on lane %d\n", l.getId());
                        }
                    }
                }
            }
        } // else ignore other event
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
    }


}
