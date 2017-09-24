package com.dpgil.pathlinker.path_linker.internal;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.NetworkDestroyedEvent;
import org.cytoscape.model.events.NetworkDestroyedListener;

/**
 * Listener class for _edgeWeightColumnBox in PathLinkerPanel class
 * Can be use by more GUI objects in the future
 */
public class PathLinkerNetworkEventListener implements SetCurrentNetworkListener, NetworkAddedListener, NetworkDestroyedListener {

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        PathLinkerControlPanel.updateEdgeWeightColumn();
        PathLinkerControlPanel.updateNetworkCmb();
    }

    @Override
    public void handleEvent(NetworkAddedEvent e) {
        PathLinkerControlPanel.updateNetworkCmb();
    }

    @Override
    public void handleEvent(NetworkDestroyedEvent e) {
        PathLinkerControlPanel.updateNetworkCmb();
    }

}
