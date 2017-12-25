package com.dpgil.pathlinker.path_linker.internal.task;

import java.util.ArrayList;
import java.util.Collection;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;
import com.dpgil.pathlinker.path_linker.internal.rest.PathLinkerModelParams;
import com.dpgil.pathlinker.path_linker.internal.util.Algorithms.PathWay;

public class RunKSPTask extends AbstractNetworkTask implements ObservableTask {


    private CyNetwork network;
    private PathLinkerModelParams modelParams;
    private PathLinkerModel pathLinkerModel;
    private ArrayList<PathWay> result;

    protected TaskMonitor taskMonitor;

    public RunKSPTask(CyNetwork network, 
            PathLinkerModelParams modelParams) {
        super(network);
        this.network = network;
        this.modelParams = modelParams;
    }

    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(PathLinkerModel.class))
            return (R) pathLinkerModel;
        else if (type.equals(Collection.class))
            return (R) result;

        return null;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        this.taskMonitor = taskMonitor;
        taskMonitor.setTitle("Running PathLinker");
        taskMonitor.setStatusMessage("Running PathLinker. Please wait...");
        runKSP();
    }

    protected void runKSP() {
        // initialize the PathLinkerModel to run ksp
        pathLinkerModel = new PathLinkerModel(
                network, 
                modelParams.allowSourcesTargetsInPaths, 
                modelParams.includePathScoreTies, 
                modelParams.sourcesTextField, 
                modelParams.targetsTextField, 
                modelParams.edgeWeightColumnName,
                modelParams.inputK,
                modelParams.edgeWeightSetting, 
                modelParams.edgePenalty);

        taskMonitor.setStatusMessage("Running KSP algorithm...");
        // runs the KSP algorithm
        result = pathLinkerModel.runKSP();
    }
}
