package com.dpgil.pathlinker.path_linker.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;

/** Listener class for _loadNodeToSourceButton and _loadNodeToTargetButton in PathLinkerPanel class */
public class PathLinkerNodeSelectionListener implements RowsSetListener {

	/**
	 * Enables the buttons if user selects a node in the network view, otherwise disable
	 */
	@Override
	public void handleEvent(RowsSetEvent e) {

		if (e.containsColumn(CyNetwork.SELECTED) && e.getSource() == 
				PathLinkerPanel._applicationManager.getCurrentNetworkView().getModel().getDefaultNodeTable()) {
			for (RowSetRecord rowSet : e.getColumnRecords(CyNetwork.SELECTED)) {
				if (rowSet.getRow().get(CyNetwork.SELECTED, Boolean.class)) {
					PathLinkerPanel._loadNodeToSourceButton.setEnabled(true);
					PathLinkerPanel._loadNodeToTargetButton.setEnabled(true);
					return;
				}
			}
		}

		PathLinkerPanel._loadNodeToSourceButton.setEnabled(false);
		PathLinkerPanel._loadNodeToTargetButton.setEnabled(false);
	}
}
