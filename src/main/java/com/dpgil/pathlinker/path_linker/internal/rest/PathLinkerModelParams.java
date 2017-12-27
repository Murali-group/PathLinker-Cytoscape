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

    @ApiModelProperty(value = "Generate KSP subgraph/subgraph view, pathlinker index, and result panel in Cytoscape."
            + " Default set to false")
    public boolean generateKSPSubgraph = false;

    @ApiModelProperty(value = "Allow Sources and Targets in Computed Path. Default set to false", example = "false")
    public boolean allowSourcesTargetsInPaths= false;

    @ApiModelProperty(value = "Include All Paths With Same Score/Lengths. Default set to false", example = "false")
    public boolean includePathScoreTies = false; 

    @ApiModelProperty(value = "Source Node Name Seperate By Space", example = "S1 S2 S3")
    public String sourcesTextField; 

    @ApiModelProperty(value = "Target Node Name Seperate By Space", example = "T1 T2 T3")
    public String targetsTextField;

    @ApiModelProperty(value = "Edge Weight Column Name", example = "edge_weight")
    public String edgeWeightColumnName;

    @ApiModelProperty(value = "Number of Paths to be Generated. Default set to 50", example = "50")
    public int inputK = 50;

    @ApiModelProperty(value = "Edge Weight Setting Name. Default set to UNWEIGHTED", example = "UNWEIGHTED")
    public EdgeWeightSetting edgeWeightSetting = EdgeWeightSetting.UNWEIGHTED;
    public String edgeWeightSettingName;

    @ApiModelProperty(value = "Edge Penality. Default set to 0", example = "0")
    public double edgePenalty = 0;
}