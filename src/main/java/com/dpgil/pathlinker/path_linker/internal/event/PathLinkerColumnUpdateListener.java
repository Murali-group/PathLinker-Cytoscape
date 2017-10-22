package com.dpgil.pathlinker.path_linker.internal.event;

import org.cytoscape.model.events.ColumnCreatedEvent;
import org.cytoscape.model.events.ColumnCreatedListener;
import org.cytoscape.model.events.ColumnDeletedEvent;
import org.cytoscape.model.events.ColumnDeletedListener;
import org.cytoscape.model.events.ColumnNameChangedEvent;
import org.cytoscape.model.events.ColumnNameChangedListener;

import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;

/**
 * Listener class for _edgeWeightColumnBox in PathLinkerPanel class
 *      Updates the _suidToPathIndexMap and _pathIndexToSuidMap
 * Can be use by more GUI objects in the future
 */
public class PathLinkerColumnUpdateListener implements ColumnCreatedListener, ColumnDeletedListener, ColumnNameChangedListener {

	@Override
	public void handleEvent(ColumnNameChangedEvent e) {
		PathLinkerControlPanel.updateEdgeWeightColumn();

		long suid = PathLinkerControlPanel._pathIndexToSuidMap.remove(e.getOldColumnName());
		PathLinkerControlPanel._suidToPathIndexMap.put(suid, e.getNewColumnName());
		PathLinkerControlPanel._pathIndexToSuidMap.put(e.getNewColumnName(), suid);
	}

	@Override
	public void handleEvent(ColumnDeletedEvent e) {
		PathLinkerControlPanel.updateEdgeWeightColumn();

        PathLinkerControlPanel._suidToPathIndexMap.remove(
                PathLinkerControlPanel._pathIndexToSuidMap.remove(
                        e.getColumnName()));
	}

	@Override
	public void handleEvent(ColumnCreatedEvent e) {
		PathLinkerControlPanel.updateEdgeWeightColumn();
	}
}
