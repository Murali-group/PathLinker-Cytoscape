package com.dpgil.pathlinker.path_linker.internal;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;

/**
 * Listener class for _edgeWeightColumnBox in PathLinkerPanel class
 * Can be use by more GUI objects in the future
 */
public class PathLinkerNetworkEventListener implements SetCurrentNetworkListener {

	@Override
	public void handleEvent(SetCurrentNetworkEvent e) {
		PathLinkerControlPanel.updateEdgeWeightColumn();
		PathLinkerControlPanel.updateNetworkColumn();
	}

}
