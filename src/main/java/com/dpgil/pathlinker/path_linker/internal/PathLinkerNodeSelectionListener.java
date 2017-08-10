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

		if (e.getSource() != PathLinkerControlPanel._applicationManager.getCurrentNetworkView().getModel().getDefaultNodeTable())
			return;

		if (e.containsColumn(CyNetwork.SELECTED)) {
			for (RowSetRecord rowSet : e.getColumnRecords(CyNetwork.SELECTED)) {
				if (rowSet.getRow().get(CyNetwork.SELECTED, Boolean.class)) {
					PathLinkerControlPanel._loadNodeToSourceButton.setEnabled(true);
					PathLinkerControlPanel._loadNodeToTargetButton.setEnabled(true);
					return;
				}
			}
		}

		PathLinkerControlPanel._loadNodeToSourceButton.setEnabled(false);
		PathLinkerControlPanel._loadNodeToTargetButton.setEnabled(false);
	}
}
