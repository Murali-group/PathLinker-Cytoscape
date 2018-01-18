package com.dpgil.pathlinker.path_linker.internal.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.cytoscape.ci.model.CIResponse;

import com.dpgil.pathlinker.path_linker.internal.PathLinkerDocumentation;
import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModelParams;
import com.dpgil.pathlinker.path_linker.internal.util.PathLinkerError;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Interface for PathLinker CytoScape CyRest Service
 */
@Api(tags = "Apps: PathLinker")
@Path("/pathlinker/v1/")
public interface PathLinkerResource {

    /**
     * Post Function that takes user input, generate new network/network view, 
     *      and return network/view SUIDs and k-number sorted path list
     * Does not modify or generate network/network view in CytoScape itself
     * @param modelParams parameters needed to generate a network
     * @return k-number sorted path list in JSON Array format
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("{networkSUID}/run")
    @ApiOperation(value = "Run PathLinker on a Specific Network with Options", 
            notes = PathLinkerDocumentation.RUN_SWAGGER_NOTES,
            response = PathLinkerAppResponse.class)
    @ApiResponses(value = { 
            @ApiResponse(code = PathLinkerError.CY_NETWORK_NOT_FOUND_CODE, message = "Input Network Does Not Exist", response = CIResponse.class),
            @ApiResponse(code = PathLinkerError.INVALID_INPUT_CODE, 
                    message = "Invalid User Input" + '\n' + '\n'
                            + "Possible problems:" + '\n'
                            + " - Sources or targets are not found in the network." + '\n'
                            + " - Invalid input for parameters." + '\n'
                            + " - Not all edges have weights (for a weighted option)", 
                    response = CIResponse.class),
            @ApiResponse(code = PathLinkerError.PATH_NOT_FOUND_CODE, 
                    message = "No Path Found" + '\n' + '\n'
                            + "PathLinker cannot find a sequence of edges (path) connecting the source(s) to the target(s) in the given network.", 
                    response = CIResponse.class),
    })
    public Response runPathLinker(
            @ApiParam(value="Network SUID") 
            @PathParam("networkSUID") long networkSUID,

            @ApiParam(value = "PathLinker Parameters", required = true) PathLinkerModelParams modelParams
            );
}
