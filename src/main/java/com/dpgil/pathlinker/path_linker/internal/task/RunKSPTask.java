package com.dpgil.pathlinker.path_linker.internal.task;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;
import com.dpgil.pathlinker.path_linker.internal.rest.PathLinkerModelParams;

/**
 * Class which creates a task that runs the KSP algorithm
 */
public class RunKSPTask extends AbstractNetworkTask implements ObservableTask {

    /** network to perform ksp algorithm */
    private CyNetwork network;
    /** parameters to create model for running the ksp algorithm */
    private PathLinkerModelParams modelParams;
    /** the model to run ksp algorithm */
    private PathLinkerModel pathLinkerModel;
    /** task monitor for the RunKSPTask */
    private TaskMonitor taskMonitor;

    /**
     * Default constructor
     * @param network network to perform ksp algorithm
     * @param modelParams parameters to create model for running the ksp algorithm
     */
    public RunKSPTask(CyNetwork network, 
            PathLinkerModelParams modelParams) {
        super(network);
        this.network = network;
        this.modelParams = modelParams;
    }

    /**
     * Get Result method
     * returns PathLinkerModel used by the RunKSPTask
     *      the model consist of error lists and results from running the ksp algorithm
     * @return object based on user input type
     *          otherwise null
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(PathLinkerModel.class))
            return (R) pathLinkerModel;

        return null;
    }

    /**
     * Runs the KSP algorithm
     */
    @Override
    public void run(TaskMonitor taskMonitor) {
        this.taskMonitor = taskMonitor;
        taskMonitor.setTitle("Running KSP algorithm");
        taskMonitor.setStatusMessage("Running KSP algorithm. Please wait...");
        runKSP();
    }

    /**
     * Wrapper method that runs the KSP algorithm
     * Initialize the PathLinkerModel to run KSP algorithm
     */
    private void runKSP() {
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

        // runs the KSP algorithm
        if (pathLinkerModel.runKSP())
            taskMonitor.setStatusMessage("Running KSP algorithm success.");
        else
            taskMonitor.setStatusMessage("Running KSP algorithm failed, error found.");
    }
}
