package com.dpgil.pathlinker.path_linker.internal.rest;

import java.util.List;

import com.dpgil.pathlinker.path_linker.internal.util.PathLinkerPath;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Class handles PathLinker API function response
 * @author li huang
 * @version 1.3.2017
 */
@ApiModel(value="PathLinker App Response", 
description="PathLinker Analysis Results in CI Format")
public class PathLinkerAppResponse {

    /** SUID of the ksp subnetwork */
    private Long kspSubNetworkSUID;
    /** SUID of the ksp subnetwork view */
    private Long kspSubNetworkViewSUID;
    /** List of PathLinkerPath objects */
    private List<PathLinkerPath> paths;

    /**
     * Getter method of the kspSubNetworkSUID
     * @return kspSubNetworkSUID
     */
    @ApiModelProperty(value = "SUID of the ksp subnetwork created. Only given if user set generateKSPSubgraph to true")
    public Long getKspSubNetworkSUID() {
        return kspSubNetworkSUID;
    }

    /**
     * Setter method of the kspSubNetworkSUID
     * @param kspSubNetworkSUID
     */
    public void setKspSubNetworkSUID(Long kspSubNetworkSUID) {
        this.kspSubNetworkSUID = kspSubNetworkSUID;
    }

    /**
     * Getter method of the kspSubNetworkViewSUID
     * @return kspSubNetworkViewSUID
     */
    @ApiModelProperty(value = "SUID of the ksp subnetwork view created. Only given if user set generateKSPSubgraph to true")
    public Long getKspSubNetworkViewSUID() {
        return kspSubNetworkViewSUID;
    }

    /**
     * Setter method of the kspSubNetworkViewSUID
     * @param kspSubNetworkViewSUID
     */
    public void setKspSubNetworkViewSUID(Long kspSubNetworkViewSUID) {
        this.kspSubNetworkViewSUID = kspSubNetworkViewSUID;
    }

    /**
     * Getter method of the paths
     * @return paths
     */
    @ApiModelProperty(value = "List of paths generated by the ksp algorithm", required = true)
    public List<PathLinkerPath> getPaths() {
        return paths;
    }

    /**
     * Setter method of the paths
     * @param paths
     */
    public void setPaths(List<PathLinkerPath> paths) {
        this.paths = paths;
    }
}
