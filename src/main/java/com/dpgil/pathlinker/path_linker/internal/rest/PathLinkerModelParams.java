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

    @ApiModelProperty(value = "Source node names seperate by spaces. Must match the name column in the Node Table.", example = "S1 S2 S3", required = true)
    public String sources; 

    @ApiModelProperty(value = "target node names seperate by spaces. Must match the name column in the Node Table.", example = "T1 T2 T3", required = true)
    public String targets;

    @ApiModelProperty(value = "Allow sources and targets in computed path. Default set to false.", 
            example = "false", required = true, allowableValues = "true,false", dataType = "boolean")
    public boolean allowSourcesTargetsInPaths = false;

    @ApiModelProperty(value = "Include all paths with same score/lengths. Default set to false.", 
            example = "false", required = true, allowableValues = "true,false", dataType = "boolean")
    public boolean includeTiedPaths = false;

    @ApiModelProperty(value = "Number of paths to be generated. Default set to 50.", example = "50", required = true)
    public int k = 50;

    @ApiModelProperty(value = "Edge weight setting name. Default set to UNWEIGHTED", 
            example = "UNWEIGHTED", allowableValues = "UNWEIGHTED,ADDITIVE,PROBABILITIES")
    public EdgeWeightSetting edgeWeightSetting = EdgeWeightSetting.UNWEIGHTED;

    @ApiModelProperty(value = "Edge Penality. Default set to 1", example = "1")
    public double edgePenalty = 1;

    @ApiModelProperty(value = "Edge weight column name. Must match the column name in the Edge Table.", example = "weight")
    public String edgeWeightColumnName;

    @ApiModelProperty(value = "Generate KSP subgraph/subgraph view, pathlinker index, and result panel in Cytoscape."
            + " Default set to true", example = "true", required = true, allowableValues = "true,false", dataType = "boolean")
    public Boolean generateKSPSubgraph = true;
}