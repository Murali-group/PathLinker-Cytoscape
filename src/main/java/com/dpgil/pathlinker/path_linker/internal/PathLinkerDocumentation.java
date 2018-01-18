package com.dpgil.pathlinker.path_linker.internal;

public class PathLinkerDocumentation {
    public static final String RUN_SWAGGER_NOTES = "PathLinker \"run\" function takes "
            + "user input network SUID, source and target nodes, and a set of parameters "
            + "(input specification are shown in the Parameters Model section below), "
            + "and send the information to a web-based REST service to compute the k-shortest simple paths. " + '\n' + '\n'
            + "Function results are returned and represented by \"PathLinker App Response\" (specification shown below). "
            + "Based on user input, the function could also generates a subnetwork and subnetwork view to view the computed paths, "
            + "and adds a “path-rank” column to the Edge Table "
            + "which contains the rank of the first path in which a given edge was used." + '\n' + '\n' 
            + "To learn more about the PathLinker app itself, please see the documentation:" + '\n' + '\n'
            + "http://pathlinker-cytoscape-app.readthedocs.io/en/latest/PathLinker_Cytoscape.html";
    
    public static final String OLD = "PathLinker takes as input a network SUID, source nodes, target nodes, and a set of parameters, "
            + "and computes the k-shortest simple paths (ksp) in the network from any source to any target. "
            + "The app also generates a subnetwork (kspSubNetwork) to view the computed paths and adds a “path-rank” column to the Edge Table "
            + "which contains the rank of the first path in which a given edge was used." + '\n' + '\n' 
            + "The app returns the computed paths as well as the network and networkView SUIDs of "
            + "the generated subnetwork containing those paths." + '\n' + '\n'
            + "For more details, please see the documentation:" + '\n' + '\n'
            + "http://pathlinker-cytoscape-app.readthedocs.io/en/latest/PathLinker_Cytoscape.html";
}
