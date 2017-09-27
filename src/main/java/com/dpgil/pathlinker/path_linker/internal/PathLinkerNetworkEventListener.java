package com.dpgil.pathlinker.path_linker.internal;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;

/**
 * Listener class for _edgeWeightColumnBox
 *                    _networkCmb
 * Can be use by more GUI objects in the future
 */
public class PathLinkerNetworkEventListener implements SetCurrentNetworkListener, NetworkAddedListener, NetworkAboutToBeDestroyedListener {

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        // update edge weight column choices when  changing network
        PathLinkerControlPanel.updateEdgeWeightColumn();

        // update the networkCmb when changing selected network
        CyNetwork network = e.getNetwork();
        if (network == null)
            PathLinkerControlPanel._networkCmb.setSelectedIndex(0);
        else
            PathLinkerControlPanel._networkCmb.
            setSelectedIndex(PathLinkerControlPanel._suidToIndexMap.get(network.getSUID()));
    }

    @Override
    public void handleEvent(NetworkAddedEvent e) {

        // update the networkCmb when adding new network
        CyNetwork network = e.getNetwork();

        if (network != null) {
            String networkName = network.getRow(network).get(CyNetwork.NAME, String.class);
            PathLinkerControlPanel._indexToSUIDMap.put(
                    PathLinkerControlPanel._networkCmb.getItemCount(), network.getSUID());
            PathLinkerControlPanel._suidToIndexMap.put(
                    network.getSUID(), PathLinkerControlPanel._networkCmb.getItemCount());
            PathLinkerControlPanel._networkCmb.addItem(networkName);
        }
    }

    @Override
    public void handleEvent(NetworkAboutToBeDestroyedEvent e) {

        // update the networkCmb when removing a network
        CyNetwork network = e.getNetwork();

        if (network != null) {
            int index = PathLinkerControlPanel._suidToIndexMap.get(network.getSUID());
            PathLinkerControlPanel._networkCmb.removeItemAt(index);

            PathLinkerControlPanel._suidToIndexMap.remove(network.getSUID());
            PathLinkerControlPanel._indexToSUIDMap.remove(index);
        }
    }
}
