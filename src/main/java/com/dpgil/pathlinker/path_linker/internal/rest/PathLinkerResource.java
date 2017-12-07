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

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("currentView/runKSP/")
    @ApiOperation(value = "Excute PathLinker on Current Network with Options, ", 
    notes = "PathLinker takes user inputs to generate and return a k-number sorted path list",
    response = PathLinkerAppResponse.class)
    @ApiResponses(value = { 
            @ApiResponse(code = 404, message = "Network does not exist", response = CIResponse.class),
    })
    public Response runKSP(
            @ApiParam(value = "PathLinker Parameters", required = true) PathLinkerModelParams modelParams
            );
}
