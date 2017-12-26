package com.dpgil.pathlinker.path_linker.internal.rest;

import java.util.ArrayList;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.model.CyNetwork;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;
import com.dpgil.pathlinker.path_linker.internal.util.Algorithms.PathWay;
import com.dpgil.pathlinker.path_linker.internal.util.EdgeWeightSetting;
import com.dpgil.pathlinker.path_linker.internal.util.PathLinkerPath;

/**
 * Implementation of the PathLinkerResource interface
 */
public class PathLinkerImpl implements PathLinkerResource {

    /** CytoScape application manager */
    private CyApplicationManager cyApplicationManager;

    /** CI Factories for Exceptions */
    private final CIExceptionFactory ciExceptionFactory;
    /** CI Factories for Errors */
    private final CIErrorFactory ciErrorFactory;
    /** Response String for error origin */
    private final static String resourceErrorRoot = "urn:cytoscape:ci:pathlinker-app:v1";

    /** Error code for Network Not Found */
    private static final String CY_NETWORK_NOT_FOUND_CODE = "1";

    /** PathLinker Model object to run analysis */
    private PathLinkerModel pathLinkerModel;

    /**
     * Default constructor
     * @param cyApplicationManager application manager
     * @param ciExceptionFactory CIException Factory
     * @param ciErrorFactory CIError Factory
     */
    public PathLinkerImpl(CyApplicationManager cyApplicationManager, 
            CIExceptionFactory ciExceptionFactory,
            CIErrorFactory ciErrorFactory) {
        this.cyApplicationManager = cyApplicationManager;
        this.ciExceptionFactory = ciExceptionFactory;
        this.ciErrorFactory = ciErrorFactory;
    }

    /**
     * Implementation of the postModel method
     * Creates a PathLinkerModel and generate a ksp subgraph of the given network
     */
    @Override
    public Response generatePathList(PathLinkerModelParams modelParams) {

        // access current network
        CyNetwork cyNetwork = getCyNetwork("Run PathLinker Algorithm", CY_NETWORK_NOT_FOUND_CODE);

        // Initialize EdgeWeightSetting enum accordingly
        EdgeWeightSetting edgeWeightSetting;
        if (modelParams.edgeWeightSettingName.equals("unweighted"))
            edgeWeightSetting = EdgeWeightSetting.UNWEIGHTED;
        else if (modelParams.edgeWeightSettingName.equals("additive"))
            edgeWeightSetting = EdgeWeightSetting.ADDITIVE;
        else
            edgeWeightSetting = EdgeWeightSetting.PROBABILITIES;

        // initialize the PathLinkerModel to run ksp
        pathLinkerModel = new PathLinkerModel(
                cyNetwork, 
                modelParams.allowSourcesTargetsInPaths, 
                modelParams.includePathScoreTies, 
                modelParams.sourcesTextField, 
                modelParams.targetsTextField, 
                modelParams.edgeWeightColumnName,
                modelParams.inputK,
                edgeWeightSetting, 
                modelParams.edgePenalty);

        // run ksp and store result
        pathLinkerModel.runKSP();
        ArrayList<PathWay> paths = pathLinkerModel.getResult();

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
        response.errors = new ArrayList<CIError>();

        return Response.status(Response.Status.OK)
                .type(MediaType.APPLICATION_JSON)
                .entity(result).build();
    }

    @Override
    public Response generateKSPGraph(long networkSUID, long networkViewSUID, PathLinkerModelParams modelParams) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Helper method which accesses current CyNetwork
     * @param resourcePath error resource path
     * @param errorType error type
     * @return current CyNetwork if exists
     *          otherwise throws CI Error
     */
    private CyNetwork getCyNetwork(String resourcePath, String errorType) {
        CyNetwork cyNetwork = cyApplicationManager.getCurrentNetwork();

        if (cyNetwork == null) {
            String messageString = "Could not find current Network";
            throw ciExceptionFactory.getCIException(404, 
                    new CIError[]{this.buildCIError(404, 
                            resourcePath, errorType, messageString, null)});
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
                resourceErrorRoot + ":" + resourcePath+ ":"+ code, message);
    }
}