package com.dpgil.pathlinker.path_linker.internal.rest;

import java.util.ArrayList;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.cytoscape.model.CyNetwork;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Interface for PathLinker Cytoscape CyRest Service
 */
@Api(tags = "PathLinker-Cytoscape CyRest")
@Path("/pathlinker/v1/")
public interface PathLinkerResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getModel();

    @Path("test/")
    @ApiOperation(value = "Create and execute PathLinker on the given network, return array list path in string format")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ArrayList<String> postModel(
            @ApiParam(value = "PathLinker model parameters", required = true) PathLinkerModelParams modelParams
            );
}
