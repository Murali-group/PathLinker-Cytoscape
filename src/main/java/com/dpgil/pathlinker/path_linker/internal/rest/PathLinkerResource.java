package com.dpgil.pathlinker.path_linker.internal.rest;

import java.util.ArrayList;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.cytoscape.model.CyNetwork;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Interface for PathLinker Cytoscape CyRest Service
 */
@Api(tags = "PathLinker-Cytoscape CyRest")
@Path("/pathlinker/v1/")
public interface PathLinkerResource {
    
    @ApiOperation(value = "Create and execute PathLinker on the given network, return array list path in string format")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ArrayList<String> postModel(
            @ApiParam(value = "The network to run ksp algorithm", required = true) CyNetwork network,
            @ApiParam(value = "boolean deciding if sources and targets should be allow in the result path", required = true) boolean allowSourcesTargetsInPaths,
            @ApiParam(value = "the option to include all paths of equal length", required = true) boolean includePathScoreTies,
            @ApiParam(value = "source node names in string", required = true) String sourcesTextField,
            @ApiParam(value = "target node names in string", required = true) String targetsTextField,
            @ApiParam(value = "the column name that contains the edge weight information", required = true) String edgeWeightColumnName,
            @ApiParam(value = "input k value", required = true) int inputK,
            @ApiParam(value = "edge weight setting in string", required = true) String edgeWeightSetting,
            @ApiParam(value = "edge penalty", required = true) double edgePenalty
    );
}
