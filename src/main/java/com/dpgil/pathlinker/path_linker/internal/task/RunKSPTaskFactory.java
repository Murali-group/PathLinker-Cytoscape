package com.dpgil.pathlinker.path_linker.internal.task;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;
import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;

public class RunKSPTaskFactory extends AbstractNetworkTaskFactory {

    private PathLinkerControlPanel controlPanel;

    public RunKSPTaskFactory(PathLinkerControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    @Override
    public boolean isReady(CyNetwork network) {
        if (network == null) {
            return false;
        }

        return false;
    }
    
    @Override
    public TaskIterator createTaskIterator(CyNetwork network) {
        return create(network);
    }

    private final TaskIterator create(CyNetwork network) {
        return null;
    }

}
