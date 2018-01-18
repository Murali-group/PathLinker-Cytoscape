package com.dpgil.pathlinker.path_linker.internal.rest;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;
import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModelParams;
import com.dpgil.pathlinker.path_linker.internal.task.CreateKSPViewTask;
import com.dpgil.pathlinker.path_linker.internal.task.CreateResultPanelTask;
import com.dpgil.pathlinker.path_linker.internal.task.RunKSPTask;
import com.dpgil.pathlinker.path_linker.internal.util.Algorithms.PathWay;
import com.dpgil.pathlinker.path_linker.internal.util.PathLinkerError;
import com.dpgil.pathlinker.path_linker.internal.util.Path;
import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;

/**
 * Implementation of the PathLinkerResource interface
 */
public class PathLinkerImpl implements PathLinkerResource {

    /** the PathLinker control panel associated with */
    private PathLinkerControlPanel controlPanel;
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

    /**
     * Default constructor
     * @param controlPanel          the PathLinkerControlPanel
     * @param cyApplicationManager  application manager
     * @param cyNetworkManager      network manager
     * @param adapter               app adapter
     * @param serviceRegistrar      service registrar
     * @param cySwingApp            swing app
     * @param ciExceptionFactory    CIException factory
     * @param ciErrorFactory        CIError factory
     */
    public PathLinkerImpl(
            PathLinkerControlPanel controlPanel,
            CyApplicationManager cyApplicationManager, 
            CyNetworkManager cyNetworkManager,
            CyAppAdapter adapter,
            CyServiceRegistrar serviceRegistrar,
            CySwingApplication cySwingApp,
            CIExceptionFactory ciExceptionFactory) {
        this.controlPanel = controlPanel;
        this.cyApplicationManager = cyApplicationManager;
        this.cyNetworkManager = cyNetworkManager;
        this.adapter = adapter;
        this.serviceRegistrar = serviceRegistrar;
        this.cySwingApp = cySwingApp;
        this.ciExceptionFactory = ciExceptionFactory;
    }

    /**
     * Implementation of runPathLinker method from PathLinkerResource
     * Runs PathLinker on the input network and user parameters
     * 
     * @param networkSUID the SUID of the network to run PathLinker
     * @param modelParams user inputs
     */
    @Override
    public Response runPathLinker(long networkSUID, PathLinkerModelParams modelParams) {

        // access the network of the given network SUID
        CyNetwork cyNetwork = cyNetworkManager.getNetwork(networkSUID);

        // process validation for input parameters
        // throw exception if error found
        List<PathLinkerError> errorList = modelParams.validate(cyNetwork, "runPathLinker");
        if (!errorList.isEmpty()) {
            if (errorList.get(0).status == PathLinkerError.CY_NETWORK_NOT_FOUND_CODE)
                throw ciExceptionFactory.getCIException(PathLinkerError.CY_NETWORK_NOT_FOUND_CODE,
                        errorList.toArray(new CIError[errorList.size()]));
            else
                throw ciExceptionFactory.getCIException(PathLinkerError.INVALID_INPUT_CODE,
                        errorList.toArray(new CIError[errorList.size()]));
        }

        // create synchronous task manager to run the task on creating KSP subgraph and etc.
        SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);

        // performs KSP algorithm by creating the runKSPTask
        RunKSPTask runKSPTask = new RunKSPTask(cyNetwork, modelParams);
        synTaskMan.execute(new TaskIterator(runKSPTask));

        // response to be returned
        PathLinkerAppResponse response = new PathLinkerAppResponse();

        // obtain results from the runKSPTask
        pathLinkerModel = runKSPTask.getResults(PathLinkerModel.class);

        // check for error where no path is found
        if (pathLinkerModel.getOutputK() == 0) {
            throw ciExceptionFactory.getCIException(PathLinkerError.PATH_NOT_FOUND_CODE, 
                    new CIError[]{new PathLinkerError(PathLinkerError.PATH_NOT_FOUND_CODE, 
                            PathLinkerError.RESOURCE_ERROR_ROOT + ":runPathLinker:" + PathLinkerError.PATH_NOT_FOUND_ERROR, 
                            "No path found", null)
            });
        }

        List<PathWay> paths = pathLinkerModel.getResult(); // obtain result path

        // only generate subgraph/view if user agrees to
        if (!modelParams.skipSubnetworkGeneration) {
            // construct createKSPViewTask to create KSP subgraph, subgraph view, path rank, and update related properties
            CreateKSPViewTask createKSPViewTask = new CreateKSPViewTask(controlPanel, cyNetwork, pathLinkerModel , adapter, cyApplicationManager);
            synTaskMan.execute(new TaskIterator(createKSPViewTask));

            // store subgraph/view suids and path rank column name to the response
            response.setSubNetworkSUID(createKSPViewTask.getResults(CyNetwork.class).getSUID());
            response.setSubnetworkViewSUID(createKSPViewTask.getResults(CyNetworkView.class).getSUID());
            response.setPathRankColumnName(createKSPViewTask.getResults(String.class));

            // writes the result of the algorithm to a table
            CreateResultPanelTask createResultPanelTask = new CreateResultPanelTask(controlPanel,
                    createKSPViewTask.getResults(CyNetwork.class), 
                    String.valueOf(controlPanel.nameIndex),
                    cyNetworkManager, paths, serviceRegistrar, cySwingApp);
            synTaskMan.execute(new TaskIterator(createResultPanelTask));
        }

        // the result that stores all paths in string format
        ArrayList<Path> result = new ArrayList<Path>();

        // A decimal formatter to round path score up to 6 decimal places
        DecimalFormat df = new DecimalFormat("#.######");
        df.setRoundingMode(RoundingMode.HALF_UP);

        // loop through the paths, construct PathLinkerPath object, and add to result
        for (int i = 0; i < paths.size(); i++) {
            PathWay path = paths.get(i);
            ArrayList<String> currentPath = new ArrayList<String>();

            for (int j = 1; j < path.size() - 1; j++) {
                currentPath.add(path.nodeIdMap.get(path.get(j)));
            }

            result.add(new Path(i + 1, Double.valueOf(df.format(paths.get(i).weight)), currentPath));
        }

        // store results into response
        response.setPaths(result);

        return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(response).build();
    }
}