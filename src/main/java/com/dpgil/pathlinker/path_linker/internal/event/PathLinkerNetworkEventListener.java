package com.dpgil.pathlinker.path_linker.internal.event;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.NetworkDestroyedEvent;
import org.cytoscape.model.events.NetworkDestroyedListener;

import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;

/**
 * Listener class for _edgeWeightColumnBox
 *                    _networkCmb
 * Can be use by more GUI objects in the future
 */
public class PathLinkerNetworkEventListener implements SetCurrentNetworkListener, NetworkAddedListener, NetworkDestroyedListener {

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        // update edge weight column choices when  changing network
        PathLinkerControlPanel.updateEdgeWeightColumn();

        // update the networkCmb when changing selected network
        CyNetwork network = e.getNetwork();
        if (network == null)
            PathLinkerControlPanel._networkCmb.setSelectedIndex(0);
        else if (PathLinkerControlPanel._suidToIndexMap.containsKey(network.getSUID())) {
            PathLinkerControlPanel._networkCmb.setSelectedIndex(
                    PathLinkerControlPanel._suidToIndexMap.get(network.getSUID()));
        }
    }

    @Override
    public void handleEvent(NetworkAddedEvent e) {

        // update the networkCmb when adding new network
        CyNetwork network = e.getNetwork();

        if (network != null) {
            PathLinkerControlPanel.initializeNetworkCmb();
        }
    }

    @Override
    public void handleEvent(NetworkDestroyedEvent e) {

        // update the networkCmb when deleting a network
        PathLinkerControlPanel.initializeNetworkCmb();
    }
}
