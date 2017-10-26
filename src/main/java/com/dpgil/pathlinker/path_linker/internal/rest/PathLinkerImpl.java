package com.dpgil.pathlinker.path_linker.internal.rest;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.cytoscape.model.CyNetwork;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;
import com.dpgil.pathlinker.path_linker.internal.util.Algorithms.PathWay;
import com.dpgil.pathlinker.path_linker.internal.util.EdgeWeightSetting;
/**
 * Implementation of the PathLinkerResource interface
 *      for CyRest service
 */
public class PathLinkerImpl implements PathLinkerResource {

    /**
     * Implementation of the postModel method
     * Creates a PathLinkerModel and generate a ksp subgraph of the given network
     */
    @Override
    public ArrayList<String> postModel(
            CyNetwork network, 
            boolean allowSourcesTargetsInPaths,
            boolean includePathScoreTies, 
            String sourcesTextField, 
            String targetsTextField,
            String edgeWeightColumnName,
            int inputK,
            String edgeWeightSetting, 
            double edgePenalty) {

        // Initialize EdgeWeightSetting enum accordingly
        EdgeWeightSetting setting;
        if (edgeWeightSetting.equals("unweighted"))
            setting = EdgeWeightSetting.UNWEIGHTED;
        else if (edgeWeightSetting.equals("additive"))
            setting = EdgeWeightSetting.ADDITIVE;
        else
            setting = EdgeWeightSetting.PROBABILITIES;

        // creates the model to run ksp
        PathLinkerModel model = new PathLinkerModel(
                network, 
                allowSourcesTargetsInPaths, 
                includePathScoreTies, 
                sourcesTextField, 
                targetsTextField, 
                edgeWeightColumnName,
                inputK,
                setting, 
                edgePenalty);

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
}