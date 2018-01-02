package com.dpgil.pathlinker.path_linker.internal.rest;

import com.dpgil.pathlinker.path_linker.internal.util.EdgeWeightSetting;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
/**
 * The Class that holds all the required parameters for PathLinkerModel
 *      in order to run ksp algorithm in CyRest
 */
@ApiModel(value="PathLinker Parameters", description="Parameters for PathLinkerModel")
public class PathLinkerModelParams {

    @ApiModelProperty(value = "Source Node Names Seperate By Spaces. Must match the name column in the Node Table.", example = "S1 S2 S3", required = true)
    public String sources; 

    @ApiModelProperty(value = "Target Node Names Seperate By Spaces. Must match the name column in the Node Table.", example = "T1 T2 T3", required = true)
    public String targets;

    @ApiModelProperty(value = "Allow Sources and Targets in Computed Path. Default set to false", example = "false")
    public boolean allowSourcesTargetsInPaths = false;

    @ApiModelProperty(value = "Include All Paths With Same Score/Lengths. Default set to false", example = "false")
    public boolean includeTiedPaths = false; 

    @ApiModelProperty(value = "Number of Paths to be Generated. Default set to 50", example = "50")
    public int k = 50;

    @ApiModelProperty(value = "Edge Weight Setting Name. Default set to UNWEIGHTED", example = "UNWEIGHTED")
    public EdgeWeightSetting edgeWeightSetting = EdgeWeightSetting.UNWEIGHTED;

    @ApiModelProperty(value = "Edge Penality. Default set to 1", example = "1")
    public double edgePenalty = 1;

    @ApiModelProperty(value = "Edge Weight Column Name", example = "edge_weight")
    public String edgeWeightColumnName;

    @ApiModelProperty(value = "Generate KSP subgraph/subgraph view, pathlinker index, and result panel in Cytoscape."
            + " Default set to false", example = "true")
    public boolean generateKSPSubgraph = true;
}