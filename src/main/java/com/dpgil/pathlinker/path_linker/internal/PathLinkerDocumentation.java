package com.dpgil.pathlinker.path_linker.internal;

public class PathLinkerDocumentation {
    public static final String RUN_SWAGGER_NOTES = "This function sends the "
            + "user input network SUID, source and target nodes, and a set of parameters "
            + "(see the Parameters section below) to the PathLinker Cytoscape App "
            + "which computes the k-shortest simple paths "
            + "connecting the sources to the targets in the network. " + '\n' + '\n'
            + "This function returns the ranked list of computed paths as well as the SUIDs of the subnetwork "
            + "and subnetwork view and name of the \"path rank\" column created by the app." + '\n' + '\n' 
            + "To learn more about the PathLinker app, please see the [documentation]"
            + "(http://pathlinker-cytoscape-app.readthedocs.io/en/latest/PathLinker_Cytoscape.html).";

    public static final String RUN_CURRENT_NETWORK_SWAGGER_NOTES = "This function sends the "
            + "currently selected network, user input source and target nodes, and a set of parameters "
            + "(see the Parameters section below) to the PathLinker Cytoscape App "
            + "which computes the k-shortest simple paths "
            + "connecting the sources to the targets in the network. " + '\n' + '\n'
            + "This function returns the ranked list of computed paths as well as the SUIDs of the subnetwork "
            + "and subnetwork view and name of the \"path rank\" column created by the app." + '\n' + '\n' 
            + "To learn more about the PathLinker app, please see the [documentation]"
            + "(http://pathlinker-cytoscape-app.readthedocs.io/en/latest/PathLinker_Cytoscape.html).";
}
