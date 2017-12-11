package com.dpgil.pathlinker.path_linker.internal.rest;

import org.cytoscape.model.CyNetwork;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
/**
 * The Class that holds all the required parameters for PathLinkerModel
 *      in order to run ksp algorithm in CyRest
 */
@ApiModel(value="PathLinker Parameters", description="Parameters for PathLinkerModel")
public class PathLinkerModelParams {
    @ApiModelProperty(value = "Network Object")
    public CyNetwork network;
    @ApiModelProperty(value = "Allow Sources and Targets in Computed Path", example = "false")
    public boolean allowSourcesTargetsInPaths;
    @ApiModelProperty(value = "Include All Paths With Same Score/Lengths", example = "false")
    public boolean includePathScoreTies; 
    @ApiModelProperty(value = "Source Node Name Seperate By Space", example = "S1 S2 S3")
    public String sourcesTextField; 
    @ApiModelProperty(value = "Target Node Name Seperate By Space", example = "T1 T2 T3")
    public String targetsTextField;
    @ApiModelProperty(value = "Edge Weight Column Name", example = "edge_weight")
    public String edgeWeightColumnName;
    @ApiModelProperty(value = "Number of Paths to be Generated", example = "50")
    public int inputK; 
    @ApiModelProperty(value = "Edge Weight Setting", example = "unweighted/additive/probability")
    public String edgeWeightSetting; 
    @ApiModelProperty(value = "Edge Penality", example = "0")
    public double edgePenalty;
}
