package com.dpgil.pathlinker.path_linker.internal.rest;

import org.cytoscape.model.CyNetwork;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
/**
 * The Class that holds all the required parameters for PathLinkerModel
 *      in order to run ksp algorithm in CyRest
 */
@ApiModel(value="PathLinkerModel Parameters", description="Parameters for PathLinkerModel")
public class PathLinkerModelParams {
    @ApiModelProperty(value = "The network to run ksp algorithm")
    public CyNetwork network;
    @ApiModelProperty(value = "boolean deciding if sources and targets should be allow in the result path")
    public boolean allowSourcesTargetsInPaths;
    @ApiModelProperty(value = "the option to include all paths of equal length")
    public boolean includePathScoreTies; 
    @ApiModelProperty(value = "source node names in string")
    public String sourcesTextField; 
    @ApiModelProperty(value = "target node names in string")
    public String targetsTextField;
    @ApiModelProperty(value = "the column name that contains the edge weight information")
    public String edgeWeightColumnName;
    @ApiModelProperty(value = "input k value")
    public int inputK; 
    @ApiModelProperty(value = "edge weight setting in string")
    public String edgeWeightSetting; 
    @ApiModelProperty(value = "edge penalty")
    public double edgePenalty;
}
