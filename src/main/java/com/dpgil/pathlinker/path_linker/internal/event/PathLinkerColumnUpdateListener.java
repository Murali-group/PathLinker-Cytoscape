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

    private PathLinkerControlPanel controlPanel;

    public PathLinkerColumnUpdateListener(PathLinkerControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    @Override
    public void handleEvent(ColumnNameChangedEvent e) {
        controlPanel.updateEdgeWeightColumn();

        long suid = controlPanel._pathIndexToSuidMap.remove(e.getOldColumnName());
        controlPanel._suidToPathIndexMap.put(suid, e.getNewColumnName());
        controlPanel._pathIndexToSuidMap.put(e.getNewColumnName(), suid);
    }

    @Override
    public void handleEvent(ColumnDeletedEvent e) {
        controlPanel.updateEdgeWeightColumn();

        controlPanel._suidToPathIndexMap.remove(
                controlPanel._pathIndexToSuidMap.remove(
                        e.getColumnName()));
    }

    @Override
    public void handleEvent(ColumnCreatedEvent e) {
        controlPanel.updateEdgeWeightColumn();
    }
}
