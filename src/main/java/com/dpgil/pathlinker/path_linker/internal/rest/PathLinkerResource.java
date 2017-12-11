package com.dpgil.pathlinker.path_linker.internal.rest;

import java.util.ArrayList;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.cytoscape.ci.model.CIResponse;

import com.dpgil.pathlinker.path_linker.internal.util.PathLinkerPath;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Interface for PathLinker Cytoscape CyRest Service
 */
@Api(tags = "Apps: PathLinker")
@Path("/pathlinker/v1/")
public interface PathLinkerResource {

    @ApiModel(value="PathLinker App Response", 
            description="PathLinker Analysis Results in CI Format", 
            parent=CIResponse.class)
    public static class PathLinkerAppResponse extends CIResponse<ArrayList<PathLinkerPath>> {
    }

    /**
     * Post Function that takes user input and generate and return a k-number sorted path list
     * Does not modify or generate network/network view in Cytoscape itself
     * @param modelParams paramters needed to generate a network
     * @return k-number sorted path list in JSON Array format
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("currentView/generatePathList/")
    @ApiOperation(value = "Excute PathLinker on Current Network with Options, ", 
    notes = "PathLinker takes user inputs to generate and return a k-number sorted path list",
    response = PathLinkerAppResponse.class)
    @ApiResponses(value = { 
            @ApiResponse(code = 404, message = "Network does not exist", response = CIResponse.class),
    })
    public Response generatePathList(
            @ApiParam(value = "PathLinker Parameters", required = true) PathLinkerModelParams modelParams
            );

    /**
     * Post Function that takes user input and generate and return a k-number sorted path list
     * Does not modify or generate network/network view in Cytoscape itself
     * @param modelParams paramters needed to generate a network
     * @return k-number sorted path list in JSON Array format
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("{networkSUID}/views/{networkViewSUID}/generateKSPGraph")
    @ApiOperation(value = "Excute PathLinker on Selected Network and Network View with Options, ", 
    notes = "PathLinker takes user inputs to create a sub-network/network view and return a k-number sorted path list",
    response = PathLinkerAppResponse.class)
    @ApiResponses(value = { 
            @ApiResponse(code = 404, message = "Network does not exist", response = CIResponse.class),
    })
    public Response generateKSPGraph(
            @ApiParam(value="Network SUID (see GET /v1/networks)") 
            @PathParam("networkSUID") long networkSUID, 

            @ApiParam(value="Network View SUID (see GET /v1/networks/{networkId}/views)") 
            @PathParam("networkViewSUID") long networkViewSUID,

            @ApiParam(value = "PathLinker Parameters", required = true) PathLinkerModelParams modelParams
            );
}
