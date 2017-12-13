package com.dpgil.pathlinker.path_linker.internal.task;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;


public class RunKSPTask extends AbstractNetworkTask implements ObservableTask {

    
    private CyNetwork network;
    private PathLinkerControlPanel controlPanel;
    
    protected TaskMonitor taskMonitor;
    
    public RunKSPTask(CyNetwork network, PathLinkerControlPanel controlPanel) {
        super(network);
        
        this.network = network;
        this.controlPanel = controlPanel;
    }

    @Override
    public <R> R getResults(Class<? extends R> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        this.taskMonitor = taskMonitor;
        taskMonitor.setTitle("Running PathLinker");
        taskMonitor.setStatusMessage("Running PathLinker. Please wait...");
        
    }
    
    protected void runKSP() {
        boolean success;

        // initialize the model from the user inputs
    }
    

}
