package com.dpgil.pathlinker.path_linker.internal.rest;

import java.util.ArrayList;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Interface for PathLinker Cytoscape CyRest Service
 */
@Api(tags = "Apps: PathLinker")
@Path("/pathlinker/v1/")
public interface PathLinkerResource {

    /**
     * The GET method accesses the current network selected in CytoScape and run KSP algorithm
     *      on the selected network. The method does not create new network or modify
     *      existing network. Instead it returns the sorted paths list as a string
     * @return sorted paths list in string format
     */
    @Path("currentView/runKSP/")
    @ApiOperation(value = "Run PathLinker on the current network, "
            + "return array list path in string format")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String runKSP();

    @Path("test/")
    @ApiOperation(value = "Create and execute PathLinker on the given network, "
            + "return array list path in string format")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ArrayList<String> postModel(
            @ApiParam(value = "PathLinker model parameters", required = true) PathLinkerModelParams modelParams
            );
}
