package com.dpgil.pathlinker.path_linker.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

import com.dpgil.pathlinker.path_linker.internal.util.Algorithms;
import com.dpgil.pathlinker.path_linker.internal.util.EdgeWeightType;
import com.dpgil.pathlinker.path_linker.internal.util.Algorithms.PathWay;

/** Back end model for the PathLinker plugin */
public class PathLinkerModel {
	/** The original network selected by the user */
	private CyNetwork originalNetwork;
	/** The network to perform the algorithm on */
	private CyNetwork network;
	/** A mapping of the node object to its name*/
	private Map<CyNode, String> cyNodeToId;
	/** Whether or not to created a bi-directed copy of the network and run PathLinker on that */
	private boolean treatNetworkAsUndirected;
	/** Whether or not to allow sources and targets in paths */
	private boolean allowSourcesTargetsInPaths;
	/** column name that links to the edge weight values */
	private String edgeWeightColumnName;
	/** list of source names */
	private Set<String> sourceNames;
	/** list of target names */
	private Set<String> targetNames;
	/** The sources to be used in the algorithm */
	private List<CyNode> sourcesList;
	/** The targets to be used in the algorithm */
	private List<CyNode> targetsList;
	/** The input k value to be used in the algorithm */
	private int inputK;
	/** The output k number paths calculated by the algorithm */
	private int outputK;
	/** The value by which to penalize each edge weight */
	private Double edgePenalty;
	/** Perform algo unweighted, weighted (probs), or weighted (p-values) */
	private EdgeWeightType edgeWeightType;
	/** Weight of edges to be used by the algorithm */
	private Map<CyEdge, Double> edgeWeights;
	/** Edges that we hide from the algorithm */
	private Set<CyEdge> hiddenEdges;
	/** The super source to call ksp with and removed after the algorithm */
	private CyNode superSource;
	/** The super target to call ksp with and removed after the algorithm */
	private CyNode superTarget;
	/** The edges attached to super(source/target) to be removed after ksp */
	private Set<CyEdge> superEdges;
	/** Number of shared nodes between sources and targets */
	private int commonSourcesTargets;
	/** Whether or not to include more than k paths if the path length/score is equal to the kth path's */
	private boolean includePathScoreTies;
	/** sources in the ksp subgraph */
	private Set<CyNode> subgraphSources;
	/** targets in the ksp subgraph */
	private Set<CyNode> subgraphTargets;
	/** The path result produced by the ksp algorithm */
	private ArrayList<PathWay> result;

	/**
	 * Constructor of the model
	 * @param originalNetwork            the original network given by the view
	 * @param treatNetworkAsUndirected   the option to run PathLinker on a bi-directed copy of the network
	 * @param allowSourcesTargetsInPaths boolean deciding if sources and targets should be allow in the result path
	 * @param includePathScoreTies       the option to include all paths of equal length
	 * @param sourceNames                set of sources in string   
	 * @param targetNames                set of targets in string
	 * @param sourcesList                list of sources in CyNode
	 * @param targetsList                list of targets in CyNode
	 * @param edgeWeightColumnName       column name that contains the edge weight information
	 * @param inputK                     input k value
	 * @param edgeWeightType             edge weight type
	 * @param edgePenalty                edge penalty
	 * @param cyNodeToId                 map mapping all CyNode to its string name
	 */
	public PathLinkerModel(CyNetwork originalNetwork, boolean treatNetworkAsUndirected, boolean allowSourcesTargetsInPaths, boolean includePathScoreTies, 
	        Set<String> sourceNames, Set<String> targetNames, List<CyNode> sourcesList, List<CyNode> targetsList, String edgeWeightColumnName, 
	        int inputK, EdgeWeightType edgeWeightType, Double edgePenalty, Map<CyNode, String> cyNodeToId) {

	    this.originalNetwork 			= originalNetwork;
	    this.treatNetworkAsUndirected   = treatNetworkAsUndirected;
	    this.allowSourcesTargetsInPaths = allowSourcesTargetsInPaths;
	    this.includePathScoreTies		= includePathScoreTies;
	    this.sourceNames                = sourceNames;
	    this.targetNames                = targetNames;
	    this.sourcesList                = sourcesList;
	    this.targetsList                = targetsList;
	    this.edgeWeightColumnName		= edgeWeightColumnName;
	    this.inputK 					= inputK;
	    this.edgeWeightType 			= edgeWeightType;
	    this.edgePenalty 				= edgePenalty;
	    this.cyNodeToId                 = cyNodeToId;

	    this.commonSourcesTargets = 0;
	}

	/**
	 * Getter method of the subgraph sources
	 * @return subgraphSources
	 */
	public Set<CyNode> getSubgraphSources() {
	    return this.subgraphSources;
	}

	/**
	 * Getter method of the subgraph targets
	 * @return subgraphTargets
	 */
	public Set<CyNode> getSubgraphTargets() {
	    return this.subgraphTargets;
	}

	/**
	 * Getter method of output k value
	 * @return output k value
	 */
	public int getOutputK() {
	    return this.outputK;
	}

	/**
	 * Getter method of the result
	 * @return result
	 */
	public ArrayList<PathWay> getResult() {
	    return this.result;
	}

	/**
	 * Runs all the necessary algorithms to calculate kth shortest path
	 * If path exists, selects corresponding nodes and edges in the network
	 *     to prepare for the subnetwork creation
	 */
	public void runKSP() {
	    // sets the number of common sources and targets
		// this is for a temporary hack
		setCommonSourcesTargets();

		// creates a copy of the original network which is modified to run PathLinker
		// 1. undirected edges are converted to bidirectional edges
		// 2. the weight of multiple source-target edges are averaged because
		// PathLinker does not support multi-graphs
		initializeNetwork();

		// "removes" the edges that are incoming to source nodes and outgoing
		// from target nodes
		initializeHiddenEdges();

		// set the edge weights of the new network to be used in the algorithm.
		// doesn't actually set the values as edge attributes 
		// because that dominates runtime.
		setEdgeWeights();

		// adds a superSource and superTarget and attaches them to the sources
		// and targets, respectively
		addSuperNodes();

		// runs the KSP algorithm
		result = Algorithms.ksp(network, cyNodeToId, superSource, superTarget, 
		        inputK + commonSourcesTargets, includePathScoreTies);

		// discard first _commonSourcesTargets paths
		// this is for a temporary hack: when there are n nodes that are both
		// sources and targets,
		// the algorithm will generate paths of length 0 from superSource ->
		// node -> superTarget
		// we don't want these, so we generate k + n paths and discard those n
		// paths
		result.subList(0, commonSourcesTargets).clear();

		// sort the result paths in alphabetical order if weight is same
		Algorithms.sortResult(result);

		// "un log-transforms" the path scores in the weighted options
		// as to undo the log transformations and leave the path scores
		// in terms of the edge weights
		undoLogTransformPathLength(result);

		// selects all the paths that involved in the resulting for generating ksp subgraph
		selectKSPSubgraph(result);

		// set the number of paths the subgraph contains
		outputK = result.size();
	}

	/**
	 * Setter method for commonSourcesTargets
	 * sets the number of common sources and targets
	 * this is for a temporary hack: when there are n nodes that are both sources and targets,
	 * the algorithm will generate paths of length 0 from superSource -> node -> superTarget
	 * we don't want these, so we generate k + n paths and discard those n paths
	 */
	private void setCommonSourcesTargets() {
		Set<CyNode> targetSet = new HashSet<CyNode>(targetsList);
		for (CyNode source : sourcesList) {
			if (targetSet.contains(source))
				commonSourcesTargets++;
		}
	}

	/**
	 * Creates a copy of the original network to run ksp 
	 * with the following modifications:
	 * 1. undirected edges are converted to bidirectional edges.  
	 * 2. treats multiple edges as one edge with a weight of the average of the
	 * multiple edges. This is done because pathlinker is not compatible with
	 * multigraphs.
	 */
	private void initializeNetwork() {
		// Originally I had created a text version of each edge and stored and checked 
		// for the text version in the hiddenEdges set, but retrieving both directed 
		// and undirected edges was dominating the run time. Thus here we 
		// create a new network to which we will copy the original nodes and edges 
		// with undirected edges converted to two directed edges
		CyRootNetwork root = ((CySubNetwork) originalNetwork).getRootNetwork();
		// create a subnetwork of the original network without any of the original edges
		network = root.addSubNetwork(root.getNodeList(), null);
		// create a subnetwork rather than a new private network so the nodes will be the same.
		// this way the source and target node lists will still work with the new network
		//_network = _networkFactory.createNetworkWithPrivateTables();

		// maps each source-target SUID string to its corresponding CyEdge in the 
		// newly created network. Used to keep track of which edges in the original 
		// network match the newly created edge 
		HashMap<String, CyEdge> sourcetargetToEdge = new HashMap<String, CyEdge>();
		// maps a source-target pair to all the weights of the edges connecting the same
		// source/target pair in the original network 
		// so we can go through afterwards and remove the extra edges
		HashMap<String, List<Double>> edgeMultiWeights = new HashMap<String, List<Double>>(); 

		// copy all of the edges of the original network to this network
		// convert undirected edges to bidirectional edges
		for (CyEdge e : originalNetwork.getEdgeList()) {

			CyNode source = e.getSource();
			CyNode target = e.getTarget();

			// a hack for unweighted edge to avoid calling getNetworkTableWeight
			// should be edit in the future to avoid constant checks
			Double w = edgeWeightType == EdgeWeightType.UNWEIGHTED ? 1 : getNetworkTableWeight(e);

			// check if this source-target was already added as an edge. If it was, keep track of the 
			// multiple weights. If not, add it as a new edge
			checkAddEdge(sourcetargetToEdge, edgeMultiWeights, source, target, w);
			// also add the reverse direction if the original edge was undirected
            // or if the treatNetworkAsUndirected option is checked
			if (!e.isDirected() || treatNetworkAsUndirected)
				checkAddEdge(sourcetargetToEdge, edgeMultiWeights, target, source, w);
		}

		edgeWeights = new HashMap<CyEdge, Double>();

		// now set the edge weight of each of the edges in the newly created network
		// if there were any multi-edges, then average the weights
		for (String sourcetarget : sourcetargetToEdge.keySet()){

			List<Double> weights = edgeMultiWeights.get(sourcetarget);
			// the final weight of this edge
			Double edgeWeight = weights.get(0);

			// if there are more than 1 weights for this edge, then average them together
			if (weights.size() > 1){
				Double sum = 0.0;
				for (Double w : weights){
					sum += w;
				}
				// divide the sum by the total number of weights to get the average
				edgeWeight = sum / weights.size();
			}

			edgeWeights.put(sourcetargetToEdge.get(sourcetarget), edgeWeight);
		}
	}

	/**
	 * Initializes the edges that we are hiding from the algorithm. Doesn't
	 * actually remove the edges as that dominates runtime.
	 */
	private void initializeHiddenEdges() {
		hiddenEdges = new HashSet<CyEdge>();

		// only if we don't allow sources and targets internal to paths
		if (!allowSourcesTargetsInPaths) {
			// hides all incoming directed edges to source nodes
			for (CyNode source : sourcesList) {
				hiddenEdges.addAll(network.getAdjacentEdgeList(source, CyEdge.Type.INCOMING));
			}
			// hides all outgoing directed edges from target nodes
			for (CyNode target : targetsList) {
				hiddenEdges.addAll(network.getAdjacentEdgeList(target, CyEdge.Type.OUTGOING));
			}
		}

		Algorithms.initializeHiddenEdges(hiddenEdges);
	}

	/**
	 * Sets the edge weights to be used in the algorithm. Doesn't actually set
	 * the weights as attributes because that dominates runtime.
	 */
	private void setEdgeWeights() {

		if (edgeWeightType == EdgeWeightType.UNWEIGHTED){
			for (CyEdge edge : network.getEdgeList()) {
				edgeWeights.put(edge, 1.);
			}
		}

		// applies edge penalty and then log transforms the edge weights for the
		// probability option
		else if (edgeWeightType == EdgeWeightType.PROBABILITIES) {
			applyMultiplicativeEdgePenalty(edgePenalty);
			logTransformEdgeWeights();
		}

		// applies edge penalty for the additive option
		else if (edgeWeightType == EdgeWeightType.ADDITIVE) {
			applyAdditiveEdgePenalty(edgePenalty);
		}

		// sets the weights in the algorithms class
		Algorithms.setEdgeWeights(edgeWeights);
	}

	/**
	 * Adds a superSource and superTarget and attaches them to the sources and
	 * targets, respectively. Sets _superSource, _superTarget, and populates the
	 * list _superEdges, so they can be removed later.
	 */
	private void addSuperNodes() {
		// sets up the super source/super target
		superSource = network.addNode();
		superTarget = network.addNode();
		superEdges = new HashSet<CyEdge>();

		// attaches super source to all sources
		for (CyNode source : sourcesList) {
			CyEdge superEdge = network.addEdge(superSource, source, true);

			// sets an edge weight of 0, so the edges connecting the super nodes
			// and the sources/targets don't affect the final path weights
			Algorithms.setWeight(superEdge, 0.);
			superEdges.add(superEdge);
		}
		// attaches all targets to super target
		for (CyNode target : targetsList) {
			CyEdge superEdge = network.addEdge(target, superTarget, true);

			// sets an edge weight of 0, so the edges connecting the super nodes
			// and the sources/targets don't affect the final path weights
			Algorithms.setWeight(superEdge, 0.);
			superEdges.add(superEdge);
		}
	}

	/**
	 * Gets the edge weight value from the network table. Expensive operation,
	 * so we try to minimize how often we use this
	 */
	private double getNetworkTableWeight(CyEdge e) {
		// gets the attribute edge weight value
		Double value = Double.parseDouble((originalNetwork.getRow(e).getRaw(edgeWeightColumnName).toString()));
		double edge_weight = value != null ? value.doubleValue() : -44444;

		return edge_weight;
	}

	/**
	 * Checks to see if the given source->target edge in the original network was already added 
	 * to the new network. If it was, then add the weight of the edge to the list of weights 
	 * for this edge. If it wasn't, then add the edge as a new edge and keep track of it in 
	 * the HashMap for possible future duplicate edges
	 * @param sourcetargetToEdge
	 * 			maps the source-target SUIDs to the edge in the new network
	 * @param edgeMultiWeights
	 * 			maps each edge to the list of weights 
	 * @param source 
	 * 			source node
	 * @param target 
	 * 			target node
	 * @param w 
	 * 			edge weight
	 */
	private void checkAddEdge(HashMap<String, CyEdge> sourcetargetToEdge, 
			HashMap<String, List<Double>> edgeMultiWeights,
			CyNode source, CyNode target, Double w){

		String sourceSUID = source.getSUID().toString();
		String targetSUID = target.getSUID().toString();
		String sourcetargetSUID = sourceSUID + "-" + targetSUID;
		boolean duplicate = sourcetargetToEdge.containsKey(sourcetargetSUID);

		// make sure we aren't adding any duplicate edges to this new network
		if (!duplicate){
			// add the first direction of the edge
			CyEdge newEdge = network.addEdge(source, target, true);
			sourcetargetToEdge.put(sourcetargetSUID, newEdge);
			List<Double> weights = new ArrayList<Double>();
			weights.add(w);
			edgeMultiWeights.put(sourcetargetSUID, weights);
		}
		else{
			// if the network already contains this edge, then add the extra edge weight to this edge's list of edge weights
			edgeMultiWeights.get(sourcetargetSUID).add(w);
		}
	}

	/**
	 * Selects all the nodes and edges that is in the k shortest paths to generate the ksp subgraph
	 * The PathLinkerControlPanel->createKSPSubgraphAndView method uses the selected paths to generate the ksp subgraph
	 * @param paths the list of paths generated by ksp algorithm
	 */
	private void selectKSPSubgraph(ArrayList<PathWay> paths) {

		// keeps track of sources/targets in the ksp subgraph
		// to change their visual properties later
		subgraphSources = new HashSet<CyNode>();
		subgraphTargets = new HashSet<CyNode>();

		// un-select all the selected nodes to make sure only nodes in the path are selected
		for (CyNode node : originalNetwork.getNodeList())
		    originalNetwork.getRow(node).set(CyNetwork.SELECTED, false);

		// un-select all the selected edges to make sure only edges in the path are selected
		for (CyEdge edge : originalNetwork.getEdgeList())
		    originalNetwork.getRow(edge).set(CyNetwork.SELECTED, false);

		for (PathWay currPath : paths) {
			// excluding supersource and supertarget
			for (int i = 1; i < currPath.size() - 2; i++) {
				CyNode node1 = currPath.get(i);
				CyNode node2 = currPath.get(i + 1);
				originalNetwork.getRow(node1).set(CyNetwork.SELECTED, true);
				originalNetwork.getRow(node2).set(CyNetwork.SELECTED, true);

				// check if the nodes are part of the sources or targets specified
				String node1name = originalNetwork.getRow(node1).get(CyNetwork.NAME, String.class);
				String node2name = originalNetwork.getRow(node2).get(CyNetwork.NAME, String.class);
				if (sourceNames.contains(node1name))
					subgraphSources.add(node1);
				if (targetNames.contains(node2name))
					subgraphTargets.add(node2);

				// add all of the directed edges from node1 to node2
				List<CyEdge> edges = originalNetwork.getConnectingEdgeList(node1, node2, CyEdge.Type.DIRECTED);
				for (CyEdge edge : edges){
					// verifies the edges direction
					if (edge.getSource().equals(node1) && edge.getTarget().equals(node2))
						originalNetwork.getRow(edge).set(CyNetwork.SELECTED, true);
                    // also check the reverse direction if the network is treated as undirected
					if (edge.getSource().equals(node2) && edge.getTarget().equals(node1))
						originalNetwork.getRow(edge).set(CyNetwork.SELECTED, true);
				}
				// also add all of the undirected edges from node1 to node2
				edges = originalNetwork.getConnectingEdgeList(node1, node2, CyEdge.Type.UNDIRECTED);
				for (CyEdge edge : edges) originalNetwork.getRow(edge).set(CyNetwork.SELECTED, true);
			}
		}
	}

	/**
	 * Applies the user specified edge penalty for the multiplicative option.
	 * This weight penalizes the score of every path by a factor equal to (the
	 * number of edges in the path)^(this factor). This was previously done in
	 * the logTransformEdgeWeights method with a parameter weight=(sum of all
	 * edge weights). In the "standard" PathLinker case, this was necessary to
	 * account for the probability that is lost when edges (incoming source or
	 * outgoing target) are removed, along with probability lost to zero degree
	 * nodes in the edge flux calculation.
	 *
	 * @param edgePenalty
	 *            the penalty to apply to each edge
	 */
	private void applyMultiplicativeEdgePenalty(double edgePenalty) {
		if (edgePenalty == 1.0)
			return;

		for (CyEdge edge : edgeWeights.keySet()) {
			if (hiddenEdges.contains(edge))
				continue;

			double edgeWeight = edgeWeights.get(edge);
			double w = edgeWeight / edgePenalty;
			edgeWeights.put(edge, w);
		}
	}

	/**
	 * Performs a log transformation on the supplied edges in place given a
	 * mapping from edges to their initial weights
	 */
	private void logTransformEdgeWeights() {
		for (CyEdge edge : edgeWeights.keySet()) {
			if (hiddenEdges.contains(edge))
				continue;

			double edgeWeight = edgeWeights.get(edge);

			// double w = -1 * Math.log(edge_weight);
			double w = -1 * Math.log(Math.max(0.000000001, edgeWeight)) / Math.log(10);
			edgeWeights.put(edge, w);
		}
	}

	/**
	 * "un log-transforms" the path scores in the weighted options to undo the
	 * log transformations and leave the path scores in terms of the original
	 * edge weights
	 *
	 * @param paths
	 *            the list of paths from the ksp algorithm
	 */
	private void undoLogTransformPathLength(ArrayList<PathWay> paths) {
		// weighted probabilities option sets the weight to 2 ^ (-weight)
		if (edgeWeightType == EdgeWeightType.PROBABILITIES) {
			for (PathWay p : paths) {
				p.weight = Math.pow(10, -1 * p.weight);
			}
		}

		// don't have to do anything for unweighted or additive option
	}

	/**
	 * Applies the user specified edge penalty for the additive option. This
	 * weight penalizes the score of every path by a factor equal to (the number
	 * of edges in the path)*(this factor).
	 *
	 * @param edgePenalty
	 *            the penalty to apply to each edge
	 */
	private void applyAdditiveEdgePenalty(double edgePenalty) {
		if (edgePenalty == 0)
			return;

		for (CyEdge edge : edgeWeights.keySet()) {
			if (hiddenEdges.contains(edge))
				continue;

			double edgeWeight = edgeWeights.get(edge);
			double w = edgeWeight + edgePenalty;
			edgeWeights.put(edge, w);
		}
	}
}
