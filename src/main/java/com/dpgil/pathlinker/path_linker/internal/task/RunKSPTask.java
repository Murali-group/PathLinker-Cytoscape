package com.dpgil.pathlinker.path_linker.internal.task;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.ci.model.CIError;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;
import com.dpgil.pathlinker.path_linker.internal.rest.PathLinkerModelParams;
import com.dpgil.pathlinker.path_linker.internal.util.EdgeWeightSetting;

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
    /** list of CIError obtained from the validation */
    private List<CIError> errorList;

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
        if (type.equals(CIError.class))
            return (R) errorList;

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
                modelParams.includeTiedPaths, 
                modelParams.sources, 
                modelParams.targets, 
                modelParams.edgeWeightColumnName,
                modelParams.k,
                modelParams.edgeWeightSetting, 
                modelParams.edgePenalty);

        // set up source and target
        pathLinkerModel.prepareIdSourceTarget();

        // check if source and target are set up correctly
        validateSourceTargetEdge();

        if (errorList.size() > 0) {
            taskMonitor.setStatusMessage("Running KSP algorithm failed, error found.");
            return;
        }

        // runs the KSP algorithm
        pathLinkerModel.runKSP();
        taskMonitor.setStatusMessage("Running KSP algorithm success.");

        // check if any path found
        if (pathLinkerModel.getOutputK() == 0) {
            CIError error = new CIError();
            error.message = "No paths found";
            errorList.add(0, error);
        }
    }

    /**
     * Check user inputs on source, target, and edge weights
     */
    public void validateSourceTargetEdge() {

        // initialize error list
        errorList = new ArrayList<CIError>();

        // obtain sources and targets from the model
        ArrayList<String> sourcesNotInNet = pathLinkerModel.getSourcesNotInNet();
        ArrayList<String> targetsNotInNet = pathLinkerModel.getTargetsNotInNet();
        ArrayList<CyNode> sources = pathLinkerModel.getSourcesList();
        ArrayList<CyNode> targets = pathLinkerModel.getTargetsList();

        // edge case where only one source and one target are inputted,
        // so no paths will be found. warn the user
        if (sources.size() == 1 && sources.equals(targets)) {
            CIError error = new CIError();
            error.message = "The only source node is the same as the only target node.\n"
                    + "PathLinker will not compute any paths. Please add more nodes to the sources or targets.\n\n";
            errorList.add(0, error);
        }

        // insert all missing targets/targets to the error message
        if (targetsNotInNet.size() > 0) {
            int totalTargets = targets.size() + targetsNotInNet.size();
            CIError error = new CIError();
            error.message = targets.size() + " out of " + totalTargets + " targets are found in the network." +
                    "\n  - Targets not found: " + targetsNotInNet.toString() +
                    "\n  - Please ensure the entered node names match the 'name' column of the Node Table.\n";
            errorList.add(0, error);
        }

        // insert all missing sources/targets to the error message
        if (sourcesNotInNet.size() > 0) {
            int totalSources = sources.size() + sourcesNotInNet.size();
            CIError error = new CIError();
            error.message = sources.size() + " out of " + totalSources + " sources are found in the network." +
                    "\n  - Sources not found: " + sourcesNotInNet.toString() +
                    "\n  - Please ensure the entered node names match the 'name' column of the Node Table.\n";
            errorList.add(0, error);
        }

        // checks if all the edges in the graph have weights. Skip the check if edge weight setting is unweighted
        // if a weighted option was selected, but not all edges have weights
        // then we say something to the user.
        if (pathLinkerModel.getEdgeWeightSetting() != EdgeWeightSetting.UNWEIGHTED) {
            String edgeWeightColumnName = pathLinkerModel.getEdgeWeightColumnName();
            for (CyEdge edge : network.getEdgeList()) {
                try {
                    Double.parseDouble(network.getRow(edge).getRaw(edgeWeightColumnName).toString());
                } catch (NullPointerException  e) {
                    CIError error = new CIError();
                    error.message = "Weighted option is selected, but at least one edge does not have a weight in the selected edge weight column '" + 
                            edgeWeightColumnName + "'. Please either select the Unweighted option, or ensure all edges have a weight to run PathLinker.\n";

                    errorList.add(error);
                    break;
                }
            }
        }
    }
}
