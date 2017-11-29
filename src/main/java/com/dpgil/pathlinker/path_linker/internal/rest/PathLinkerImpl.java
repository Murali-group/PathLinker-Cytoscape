package com.dpgil.pathlinker.path_linker.internal.rest;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;
import com.dpgil.pathlinker.path_linker.internal.util.Algorithms.PathWay;
import com.dpgil.pathlinker.path_linker.internal.util.EdgeWeightSetting;
/**
 * Implementation of the PathLinkerResource interface
 */
public class PathLinkerImpl implements PathLinkerResource {

    /** CytoScape application manager */
    private CyApplicationManager cyApplicationManager;

    /**
     * Default constructor
     * @param cyApplicationManager application manager
     */
    public PathLinkerImpl(CyApplicationManager cyApplicationManager) {
        this.cyApplicationManager = cyApplicationManager;
    }

    /**
     * Implementation of the postModel method
     * Creates a PathLinkerModel and generate a ksp subgraph of the given network
     */
    @Override
    public ArrayList<String> postModel(PathLinkerModelParams modelParams) {

        // Initialize EdgeWeightSetting enum accordingly
        EdgeWeightSetting setting;
        if (modelParams.edgeWeightSetting.equals("unweighted"))
            setting = EdgeWeightSetting.UNWEIGHTED;
        else if (modelParams.edgeWeightSetting.equals("additive"))
            setting = EdgeWeightSetting.ADDITIVE;
        else
            setting = EdgeWeightSetting.PROBABILITIES;

        // creates the model to run ksp
        PathLinkerModel model = new PathLinkerModel(
                modelParams.network, 
                modelParams.allowSourcesTargetsInPaths, 
                modelParams.includePathScoreTies, 
                modelParams.sourcesTextField, 
                modelParams.targetsTextField, 
                modelParams.edgeWeightColumnName,
                modelParams.inputK,
                setting, 
                modelParams.edgePenalty);

        // run ksp and stores result
        ArrayList<PathWay> paths = model.runKSP();

        // string formatter for path weight
        DecimalFormat df = new DecimalFormat("#.######");
        df.setRoundingMode(RoundingMode.HALF_UP);

        // StringBuilder to construct PathWay object to string
        StringBuilder sb;

        // the result that stores all paths in string format
        ArrayList<String> result = new ArrayList<String>();

        // loop through the paths, construct string, and add to result
        for (int i = 0; i < paths.size(); i++) {

            sb = new StringBuilder(); // Initialize the string builder
            sb.append(i + 1);
            sb.append("\t");
            sb.append(df.format(paths.get(i).weight));
            sb.append("\t");
            sb.append(pathAsString(paths.get(i)));

            result.add(sb.toString());
        }

        return result;
    }

    /**
     * Converts a path to a string concatenating the node names A path in the
     * network involving A -> B -> C would return A|B|C
     * @param p the path to convert to a string
     * @return the concatenation of the node names
     */
    private static String pathAsString(PathWay p)
    {
        // builds the path string without supersource/supertarget [1,len-1]
        StringBuilder currPath = new StringBuilder();
        for (int i = 1; i < p.size() - 1; i++)
            currPath.append(p.nodeIdMap.get(p.get(i)) + "|");

        currPath.setLength(currPath.length() - 1);

        return currPath.toString();
    }

    /**
     * Runs KSP method on current network
     * Return the list of paths as string
     */
    @Override
    public String runKSP() {
        // access current network
        CyNetwork cyNetwork = cyApplicationManager.getCurrentNetwork();

        // check if current network exists
        if (cyNetwork == null) return "No network selected!";

        // construct PathLinkerModel to run KSP algorithm
        PathLinkerModel model = new PathLinkerModel(
                cyNetwork,
                false,
                false,
                "P35968 P00533 Q02763",
                "Q15797 Q14872 Q16236 P14859 P36956",
                "", 50, EdgeWeightSetting.UNWEIGHTED, 0);

        // sets up the source and targets, and check to see if network is construct correctly
        boolean success = model.prepareIdSourceTarget();

        // terminate and return error message if network is not c onstruct correctly
        if (!success)
            return "source and target not found!";

        // run ksp and stores result
        ArrayList<PathWay> paths = model.runKSP();

        // string formatter for path weight
        DecimalFormat df = new DecimalFormat("#.######");
        df.setRoundingMode(RoundingMode.HALF_UP);

        // StringBuilder to construct PathWay object to string
        StringBuilder sb;

        // the result that stores all paths in string format
        // ArrayList<String> result = new ArrayList<String>();

        // string to store all paths
        String output = "";

        // loop through the paths, construct string, and add to result
        for (int i = 0; i < paths.size(); i++) {
            sb = new StringBuilder(); // Initialize the string builder
            sb.append(i + 1);
            sb.append("\t");
            sb.append(df.format(paths.get(i).weight));
            sb.append("\t");
            sb.append(pathAsString(paths.get(i)));

            // result.add(sb.toString());
            output += (sb.toString() + "\n");
        }

        return output;
    }
}