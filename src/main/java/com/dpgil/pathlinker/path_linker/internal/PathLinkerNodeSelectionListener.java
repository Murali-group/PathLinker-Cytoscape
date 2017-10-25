package com.dpgil.pathlinker.path_linker.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;

/** Listener class for _loadNodeToSourceButton and _loadNodeToTargetButton in PathLinkerPanel class */
public class PathLinkerNodeSelectionListener implements RowsSetListener {

    // field to enable/disable updating netowrk combo box
    private static boolean active = true;

    /**
     * Setter method for active field
     * @param active true if enable updating network combo box
     *          false if disable
     */
    public static void setActive(boolean active) {
        PathLinkerNodeSelectionListener.active = active;
    }

	/**
	 * Enables the buttons if user selects a node in the network view, otherwise disable
	 */
	@Override
	public void handleEvent(RowsSetEvent e) {

	    // update the networkCmb if active is set to true
	    // and if user changes rename certain network
	    if (active && e.containsColumn(CyNetwork.NAME)) {
	        PathLinkerControlPanel.initializeNetworkCmb();
	        return;
	    }

	    // if event is triggered by unselect/select an edge or blank screen then do nothing
		// since user may select node and then edge simultaneously
		// if event is triggered by selecting blank screen, then unselect nodes will triggered another RowsSetEvent to disable buttons
		if (e.getSource() != PathLinkerControlPanel._applicationManager.getCurrentNetworkView().getModel().getDefaultNodeTable())
			return;

		// if event is triggered unselect/select node then check if any node is selected, enable buttons if true
		if (e.containsColumn(CyNetwork.SELECTED)) {
			for (RowSetRecord rowSet : e.getColumnRecords(CyNetwork.SELECTED)) {
				if (rowSet.getRow().get(CyNetwork.SELECTED, Boolean.class)) {
					PathLinkerControlPanel._loadNodeToSourceButton.setEnabled(true);
                    // if the targetsSame AsSources option is selected, then don't allow the user to add more targets
                    if (!PathLinkerControlPanel._targetsSameAsSourcesOption.isSelected())
                        PathLinkerControlPanel._loadNodeToTargetButton.setEnabled(true);
					return;
				}
			}
		}

		// disable buttons if no node is selected
		PathLinkerControlPanel._loadNodeToSourceButton.setEnabled(false);
		PathLinkerControlPanel._loadNodeToTargetButton.setEnabled(false);
	}
}
