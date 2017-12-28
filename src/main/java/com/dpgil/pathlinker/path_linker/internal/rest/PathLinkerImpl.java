package com.dpgil.pathlinker.path_linker.internal.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;
import com.dpgil.pathlinker.path_linker.internal.task.CreateKSPViewTask;
import com.dpgil.pathlinker.path_linker.internal.task.CreateResultPanelTask;
import com.dpgil.pathlinker.path_linker.internal.task.RunKSPTask;
import com.dpgil.pathlinker.path_linker.internal.util.Algorithms.PathWay;
import com.dpgil.pathlinker.path_linker.internal.util.EdgeWeightSetting;
import com.dpgil.pathlinker.path_linker.internal.util.PathLinkerPath;
import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;

/**
 * Implementation of the PathLinkerResource interface
 */
public class PathLinkerImpl implements PathLinkerResource {

    /** CytoScape application manager */
    private CyApplicationManager cyApplicationManager;
    private CyNetworkManager cyNetworkManager;
    /** adapter to create necessary tasks to create the sub network */
    private CyAppAdapter adapter;
    /** service registrar to register result panel */
    private CyServiceRegistrar serviceRegistrar;
    /** swing application to set the status of the result panel */
    private CySwingApplication cySwingApp;
    /** PathLinker Model object to run analysis */
    private PathLinkerModel pathLinkerModel;

    /** CI Factories for Exceptions */
    private final CIExceptionFactory ciExceptionFactory;
    /** CI Factories for Errors */
    private final CIErrorFactory ciErrorFactory;
    /** Response String for error origin */
    private final static String RESOURCE_ERROR_ROOT = "urn:cytoscape:ci:pathlinker-app:v1";
    /** Error Message for Network Not Found */
    private static final String CY_NETWORK_NOT_FOUND_ERROR = "CY_NETWORK_NOT_FOUND_ERROR";
    /** Error Message for invalid user input */
    private static final String INVALID_INPUT_ERROR = "INVALID_INPUT_ERROR";
    /** Error Message for invalid source/target/edge inputs */
    private static final String SOURCE_TARGET_EDGE_ERROR = "INVALID_SOURCE_TARGET_EDGE_INPUT_ERROR";

    /**
     * Default constructor
     * @param cyApplicationManager application manager
     * @param cyNetworkManager network manager
     * @param adapter app adapter
     * @param serviceRegistrar service registrar
     * @param cySwingApp swing app
     * @param ciExceptionFactory CIException factory
     * @param ciErrorFactory CIError factory
     */
    public PathLinkerImpl(
            CyApplicationManager cyApplicationManager, 
            CyNetworkManager cyNetworkManager,
            CyAppAdapter adapter,
            CyServiceRegistrar serviceRegistrar,
            CySwingApplication cySwingApp,
            CIExceptionFactory ciExceptionFactory,
            CIErrorFactory ciErrorFactory) {
        this.cyApplicationManager = cyApplicationManager;
        this.cyNetworkManager = cyNetworkManager;
        this.adapter = adapter;
        this.serviceRegistrar = serviceRegistrar;
        this.cySwingApp = cySwingApp;
        this.ciExceptionFactory = ciExceptionFactory;
        this.ciErrorFactory = ciErrorFactory;
    }

    /**
     * Helper method which accesses current CyNetwork
     * @param resourcePath error resource path
     * @param errorType error type
     * @return current CyNetwork if exists
     *          otherwise throws CI Error
     */
    private CyNetwork getCyNetwork(long networkSUID) {
        CyNetwork cyNetwork = cyNetworkManager.getNetwork(networkSUID);

        if (cyNetwork == null) {
            String errorMsg = "The given network does not exist";
            throw ciExceptionFactory.getCIException(404, 
                    new CIError[]{this.buildCIError(404, 
                            "runPathLinker", CY_NETWORK_NOT_FOUND_ERROR, errorMsg, null)});
        }

        return cyNetwork;
    }

    /**
     * Helper method which constructs CI Error
     * @param status error status
     * @param resourcePath error resource path
     * @param code error code
     * @param message error message
     * @param e the exception that causes the error
     * @return CIError object
     */
    private CIError buildCIError(int status, String resourcePath, 
            String code, String message, Exception e) {
        return ciErrorFactory.getCIError(status, 
                RESOURCE_ERROR_ROOT + ":" + resourcePath + ":" + code, message);
    }

    /**
     * Implementation of runPathLinker method from PathLinkerResource
     * Runs PathLinker on the input network and user parameters
     * 
     * @param networkSUID the SUID of the network to run PathLinker
     * @param modelParams user inputs
     */
    @SuppressWarnings("unchecked")
    @Override
    public Response runPathLinker(long networkSUID, PathLinkerModelParams modelParams) {

        // access the network of the given network SUID
        CyNetwork cyNetwork = getCyNetwork(networkSUID);

        // process validation for input parameters
        // throw exception if error found
        List<CIError> errorList = validateUserInput(modelParams);
        if (errorList.size() > 0) {
            throw ciExceptionFactory.getCIException(422, errorList.toArray(new CIError[errorList.size()]));
        }

        // create synchronous task manager to run the task on creating KSP subgraph and etc.
        SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);

        // performs KSP algorithm by creating the runKSPTask
        RunKSPTask runKSPTask = new RunKSPTask(cyNetwork, modelParams);
        synTaskMan.execute(new TaskIterator(runKSPTask));

        // obtain validation to see if any error occurs
        errorList = (List<CIError>) runKSPTask.getResults(CIError.class);
        if (errorList.size() > 0) {

            // reconstruct error for JSON format
            for (int i = 0; i < errorList.size(); i++) {
                CIError error = this.buildCIError(422, 
                        "runPathLinker", SOURCE_TARGET_EDGE_ERROR, errorList.get(i).message.replace("\n", ""), null);
                errorList.set(i, error);
            }

            throw ciExceptionFactory.getCIException(422, errorList.toArray(new CIError[errorList.size()]));
        }

        // obtain results from the runKSPTask
        pathLinkerModel = runKSPTask.getResults(PathLinkerModel.class);
        ArrayList<PathWay> paths = pathLinkerModel.getResult();

        // only generate subgraph/view if user agrees to
        if (modelParams.generateKSPSubgraph) {
            // construct createKSPViewTask to create KSP subgraph, subgraph view, path index, and update related properties
            CreateKSPViewTask createKSPViewTask = new CreateKSPViewTask(cyNetwork, pathLinkerModel , adapter, cyApplicationManager);
            synTaskMan.execute(new TaskIterator(createKSPViewTask));
            CyNetwork kspSubgraph = createKSPViewTask.getResults(CyNetwork.class);

            // writes the result of the algorithm to a table
            CreateResultPanelTask createResultPanelTask = new CreateResultPanelTask(kspSubgraph, 
                    String.valueOf(PathLinkerControlPanel.nameIndex),
                    cyNetworkManager, paths, serviceRegistrar, cySwingApp);
            synTaskMan.execute(new TaskIterator(createResultPanelTask));
        }

        // the result that stores all paths in string format
        ArrayList<PathLinkerPath> result = new ArrayList<PathLinkerPath>();

        // loop through the paths, construct PathLinkerPath object, and add to result
        for (int i = 0; i < paths.size(); i++) {
            PathWay path = paths.get(i);
            ArrayList<String> currentPath = new ArrayList<String>();

            for (int j = 1; j < path.size() - 1; j++)
                currentPath.add(path.nodeIdMap.get(path.get(j)));

            result.add(new PathLinkerPath(i + 1, paths.get(i).weight, currentPath));
        }

        // construct the list of paths into JSON Response format
        CIResponse<ArrayList<PathLinkerPath>>response = new CIResponse<ArrayList<PathLinkerPath>>();
        ((CIResponse<ArrayList<PathLinkerPath>>)response).data = result;

        return Response.status(Response.Status.OK)
                .type(MediaType.APPLICATION_JSON)
                .entity(result).build();
    }

    /**
     * Check for invalid user input
     * Store all errors in a list for creating exception
     * @param modelParams the user inputs
     * @return a list of CIError
     */
    public List<CIError> validateUserInput(PathLinkerModelParams modelParams) {

        // construct errorList
        List<CIError> errorList = new ArrayList<CIError>();

        // validate sources text field
        if (modelParams.sourcesTextField == null || modelParams.sourcesTextField.trim().isEmpty()) {
            String errorMsg = "Invalid sourcesTextField. Source field cannot be empty";
            CIError error = this.buildCIError(422, 
                    "runPathLinker", INVALID_INPUT_ERROR, errorMsg, null);
            errorList.add(error);
        }

        // validate targets text field
        if (modelParams.targetsTextField == null || modelParams.targetsTextField.trim().isEmpty()) {
            String errorMsg = "Invalid targetTextField. Target field cannot be empty";
            CIError error = this.buildCIError(422, 
                    "runPathLinker", INVALID_INPUT_ERROR, errorMsg, null);
            errorList.add(error);
        }

        // validate input k value
        if (modelParams.inputK < 1) {
            String errorMsg = "Invalid inputK. K value cannot be less than 1";
            CIError error = this.buildCIError(422, 
                    "runPathLinker", INVALID_INPUT_ERROR, errorMsg, null);
            errorList.add(error);
        }

        // skip validation for other parameters if edge weight setting is unweighted
        if (modelParams.edgeWeightSetting == EdgeWeightSetting.UNWEIGHTED)
            return errorList;

        // validation for edge penalty
        if (modelParams.edgePenalty < 1 && modelParams.edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
            String errorMsg  = "Invalid edgePenalty. Edge penalty must be greater than or equal to 1 for edge weight setting PROBABILITIES";
            CIError error = this.buildCIError(422, 
                    "runPathLinker", INVALID_INPUT_ERROR, errorMsg, null);
            errorList.add(error);    
        }

        else if (modelParams.edgePenalty < 0) {
            String errorMsg  = "Invalid edgePenalty. Edge penalty must be greater than or equal to 0";
            CIError error = this.buildCIError(422, 
                    "runPathLinker", INVALID_INPUT_ERROR, errorMsg, null);
            errorList.add(error);
        }

        // validation for edge weight column name
        Collection<CyColumn> columns = cyApplicationManager.getCurrentNetwork().getDefaultEdgeTable().getColumns();  
        for (CyColumn column : columns) {
            if (column.getName().equals(modelParams.edgeWeightColumnName) && (column.getType() == Double.class 
                    || column.getType() == Integer.class || column.getType() == Float.class 
                    || column.getType() == Long.class))
                return errorList; // column name exists
        }

        // column name does not exist
        String errorMsg  = "Invalid edgeWeightColumnName. Column name must point to a valid edge column with numerical type";
        CIError error = this.buildCIError(422, 
                "runPathLinker", INVALID_INPUT_ERROR, errorMsg, null);
        errorList.add(error);   

        return errorList;
    }
}