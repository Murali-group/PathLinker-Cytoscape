package com.dpgil.pathlinker.path_linker.internal;

import org.cytoscape.model.events.ColumnCreatedEvent;
import org.cytoscape.model.events.ColumnCreatedListener;
import org.cytoscape.model.events.ColumnDeletedEvent;
import org.cytoscape.model.events.ColumnDeletedListener;
import org.cytoscape.model.events.ColumnNameChangedEvent;
import org.cytoscape.model.events.ColumnNameChangedListener;

/**
 * Listener class for _edgeWeightColumnBox in PathLinkerPanel class
 * Can be use by more GUI objects in the future
 */
public class PathLinkerColumnUpdateListener implements ColumnCreatedListener, ColumnDeletedListener, ColumnNameChangedListener {

	@Override
	public void handleEvent(ColumnNameChangedEvent e) {
		PathLinkerPanel.updateEdgeWeightColumn();
	}

	@Override
	public void handleEvent(ColumnDeletedEvent e) {
		PathLinkerPanel.updateEdgeWeightColumn();
	}

	@Override
	public void handleEvent(ColumnCreatedEvent e) {
		PathLinkerPanel.updateEdgeWeightColumn();
	}
}
