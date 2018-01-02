package com.dpgil.pathlinker.path_linker.internal.task;

import java.util.ArrayList;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import com.dpgil.pathlinker.path_linker.internal.util.Algorithms.PathWay;
import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;
import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerResultPanel;

/**
 * Class which creates a task that constructs result panel for the given network and paths
 */
public class CreateResultPanelTask extends AbstractNetworkTask implements ObservableTask {

    /** the current PathLinker control panel associated with */
    private PathLinkerControlPanel controlPanel;
    /** the network that the result panel is corresponded to */
    private CyNetwork network;
    /** the title of the result panel */
    private String title;
    /** the network manager the result panel instance used to access networks */
    private CyNetworkManager networkManager;
    /** the result paths that the result panel is constructed from */
    private ArrayList<PathWay> results;
    /** service registrar to register result panel */
    private CyServiceRegistrar serviceRegistrar;
    /** swing application to set the status of the result panel */
    private CySwingApplication cySwingApp;
    /** the result panel instance created */
    private PathLinkerResultPanel resultPanel;

    /**
     * Default constructor
     * @param controlPanel the PathLinkerControlPanel
     * @param network the network
     * @param title the title
     * @param networkManager the network manager
     * @param results the result paths
     * @param serviceRegistrar the CyServiceRegistrar
     * @param cySwingApp the CySwingApplication
     */
    public CreateResultPanelTask(
            PathLinkerControlPanel controlPanel,
            CyNetwork network,
            String title,
            CyNetworkManager networkManager,
            ArrayList<PathWay> results,
            CyServiceRegistrar serviceRegistrar,
            CySwingApplication cySwingApp) {

        super(network);
        this.controlPanel = controlPanel;
        this.network = network;
        this.title = title;
        this.networkManager = networkManager;
        this.results = results;
        this.serviceRegistrar = serviceRegistrar;
        this.cySwingApp = cySwingApp;
    }

    /**
     * Get Result method
     * returns PathLinkerResultPanel instance created by the task
     * @return object based on user input type
     *          otherwise null
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(PathLinkerResultPanel.class))
            return (R) resultPanel;

        return null;
    }

    /**
     * Creates the Result Panel
     */
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Creating Result Panel");
        taskMonitor.setStatusMessage("Creating Result Panel. Please wait...");
        createResultPanel();
        taskMonitor.setStatusMessage("Result Panel created successful.");
    }

    /**
     * Creates PathLinkerResultPanel instance
     * Writes the ksp results to result panel given the results from the ksp algorithm
     */
    private void createResultPanel() {
        // create and register a new panel in result panel with specific title
        // the result panel name will be sync with network and path index using nameIndex
        resultPanel = new PathLinkerResultPanel(controlPanel, title, networkManager, network, results);
        serviceRegistrar.registerService(resultPanel, CytoPanelComponent.class, new Properties());

        // open and show the result panel if in hide state
        CytoPanel cytoPanel = cySwingApp.getCytoPanel(resultPanel.getCytoPanelName());

        if (cytoPanel.getState() == CytoPanelState.HIDE)
            cytoPanel.setState(CytoPanelState.DOCK);

        // set visible and selected
        resultPanel.setVisible(true);
        cytoPanel.setSelectedIndex(cytoPanel.indexOfComponent(resultPanel.getComponent()));
    }
}
