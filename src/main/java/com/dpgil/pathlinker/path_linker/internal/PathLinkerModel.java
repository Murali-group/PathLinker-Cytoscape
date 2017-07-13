package com.dpgil.pathlinker.path_linker.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;

public class PathLinkerModel {

	/** The original network selected by the user */
	private CyNetwork originalNetwork;
	/** The network to perform the algorithm on */
	private CyNetwork network;
	/** A mapping of the name of a node to the actual node object */
	private HashMap<String, CyNode> idToCyNode;
	/** Whether or not to allow sources and targets in paths */
	private boolean allowSourcesTargetsInPaths;
	
	private HashSet<String> sourceNames;
	private HashSet<String> targetNames;
	
	/** The sources to be used in the algorithm */
	private ArrayList<CyNode> sources;
	/** The targets to be used in the algorithm */
	private ArrayList<CyNode> targets;
	/** The k value to be used in the algorithm */
	private int k;
	/** The value by which to penalize each edge weight */
	private double edgePenalty;
	/** Perform algo unweighted, weighted (probs), or weighted (p-values) */
	private EdgeWeightSetting edgeWeightSetting;
	/** Weight of edges to be used by the algorithm */
	private HashMap<CyEdge, Double> edgeWeights;
	/** Edges that we hide from the algorithm */
	private HashSet<CyEdge> hiddenEdges;
	/** The super source to call ksp with and removed after the algorithm */
	private CyNode superSource;
	/** The super target to call ksp with and removed after the algorithm */
	private CyNode superTarget;
	/** The edges attached to super(source/target) to be removed after ksp */
	private HashSet<CyEdge> superEdges;
	/** Number of shared nodes between sources and targets */
	private int commonSourcesTargets;
	
	public PathLinkerModel(CyNetwork originalNetwork, boolean allowSourcesTargetsInPaths) {
		this.originalNetwork = originalNetwork;
		this.allowSourcesTargetsInPaths = allowSourcesTargetsInPaths;
		this.idToCyNode = new HashMap<String, CyNode>();	
		this.commonSourcesTargets = 0;
	}
	
	/**
	 * Getter method of originalNetwork
	 * @return originalNetwork
	 */
	public CyNetwork getOriginalNetwork() {
		return this.originalNetwork;
	}
	
	/**
	 * Getter method of idToCyNode
	 * @return idToCyNode
	 */
	public HashMap<String, CyNode> getIdToCyNode() {
		return this.idToCyNode;
	}
	
	/**
	 * Getter method of allowSourcesTargetsInPaths
	 * @return allowSourcesTargetsInPaths
	 */
	public boolean getAllowSourcesTargetsInPaths() {
		return this.allowSourcesTargetsInPaths;
	}
	
	/**
	 * Getter method of sourceNames
	 * @return
	 */
	public HashSet<String> getSourceNames() {
		return this.sourceNames;
	}
	
	/**
	 * Getter method of targetNames
	 * @return targetNames
	 */
	public HashSet<String> getTargetNames() {
		return this.targetNames;
	}
	
	/**
	 * Getter method of sources
	 * @return sources
	 */
	public ArrayList<CyNode> getSourcesList() {
		return this.sources;
	}
	
	/**
	 * Getter method of targets
	 * @return targets
	 */
	public ArrayList<CyNode> getTargetsList() {
		return this.targets;
	}
	
	/**
	 * Getter method of k value
	 * @return k value
	 */
	public int getK() {
		return this.k;
	}
	
	/**
	 * Getter method of edge penalty
	 * @return edgePenalty
	 */
	public double getEdgePenalty() {
		return this.edgePenalty;
	}
	
	/**
	 * Getter method of edgeWeightSetting
	 * @return edgeWeightSetting
	 */
	public EdgeWeightSetting getEdgeWeightSetting() {
		return this.edgeWeightSetting;
	}
	
	/**
	 * Setter method for both sourceNames and sources
	 * @param sourcesTextFieldValue string of source names separate by spaces
	 * @return return null if no mistyped source names, else return the list of mistyped source names
	 */
	public ArrayList<String> setSourceAndSourceNames(String sourcesTextFieldValue) {
		
		// splits the names by spaces
		String[] rawSourceNames = sourcesTextFieldValue.split(" ");
		
		sourceNames = new HashSet<String>(Arrays.asList(rawSourceNames));
		
		// stores the sources that were inputted but are not actually in the network, may have been mistyped
		ArrayList<String> sourcesNotInNet = new ArrayList<String>();
		
		// checks for mistyped source names
		for (String sourceName : sourceNames) {
			if (!idToCyNode.containsKey(sourceName))
				sourcesNotInNet.add(sourceName);
		}
		
		// generates a list of the valid source nodes to be used in the graph
		sourceNames.removeAll(sourcesNotInNet);
		sources = stringsToNodes(sourceNames);
		
		return sourcesNotInNet.size() > 0 ? null : sourcesNotInNet;
	}
	
	/**
	 * Setter method for both targetNames and targets
	 * @param targetsTextField string of target names separate by spaces
	 * @return return null if no mistyped target names, else return the list of mistyped target names
	 */
	public ArrayList<String> setTargetAndTargetNames(String targetsTextField) {
		
		// splits the names by spaces
		String[] rawTargetNames = targetsTextField.split(" ");
		
		targetNames = new HashSet<String>(Arrays.asList(rawTargetNames));
		
		// stores the targets that were inputted but are not actually in the network, may have been mistyped
		ArrayList<String> targetsNotInNet = new ArrayList<String>();
		
		// checks for mistyped target  names
		for (String targetName : targetNames) {
			if (!idToCyNode.containsKey(targetName))
				targetsNotInNet.add(targetName);
		}
		
		// generates a list of the valid target nodes to be used in the graph
		targetNames.removeAll(targetsNotInNet);
		targets = stringsToNodes(targetNames);
		
		return targetsNotInNet.size() > 0 ? null : targetsNotInNet;
	}
	
	/**
	 * Setter method for k value
	 * @param k value
	 */
	public void setK(int k) {
		this.k = k;
	}
	
	/**
	 * Setter method for edgePenalty
	 * @param edgePenalty
	 */
	public void setEdgePenalty(double edgePenalty) {
		this.edgePenalty = edgePenalty;
	}
	
	/**
	 * Setter method for edgeWeightSetting
	 * @param setting passed from the PathLinkerPanel
	 */
	public void setEdgeWeightSetting (EdgeWeightSetting setting) {
		this.edgeWeightSetting = setting;
	}
	
	/**
	 * Setter method for commonSOurcesTargets
	 * sets the number of common sources and targets
	 * this is for a temporary hack: when there are n nodes that are both sources and targets,
	 * the algorithm will generate paths of length 0 from superSource -> node -> superTarget
	 * we don't want these, so we generate k + n paths and discard those n paths
	 */
	public void setCommonSourcesTargets() {
		Set<CyNode> targetSet = new HashSet<CyNode>(targets);
		for (CyNode source : sources) {
			if (targetSet.contains(source))
				commonSourcesTargets++;
		}
	}
	
	public ArrayList<Path> runKSP() {
		
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
		ArrayList<Path> result = Algorithms.ksp(network, superSource, superTarget, k + commonSourcesTargets);

		// discard first _commonSourcesTargets paths
		// this is for a temporary hack: when there are n nodes that are both
		// sources and targets,
		// the algorithm will generate paths of length 0 from superSource ->
		// node -> superTarget
		// we don't want these, so we generate k + n paths and discard those n
		// paths
		result.subList(0, commonSourcesTargets).clear();

		// "un log-transforms" the path scores in the weighted options
		// as to undo the log transformations and leave the path scores
		// in terms of the edge weights
		undoLogTransformPathLength(result);
		
		return result;
	}
	
	/**
	 * Populates idToCyNode, the map of node names to their objects
	 * @return false if originalNetwork does not exist, otherwise populate idToCyNode and return true
	 */
	public boolean populateIdToCyNode() {
		if (this.originalNetwork == null)
			return false;
		
		for (CyNode node : originalNetwork.getNodeList()) {
			String nodeName = originalNetwork.getRow(node).get(CyNetwork.NAME, String.class);
			idToCyNode.put(nodeName, node);
		}
		
		return true;
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
			Double w = getNetworkTableWeight(e);

			// check if this source-target was already added as an edge. If it was, keep track of the 
			// multiple weights. If not, add it as a new edge
			checkAddEdge(sourcetargetToEdge, edgeMultiWeights, source, target, w);
			// also add the reverse direction if the original edge was undirected
			if (!e.isDirected())
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
			for (CyNode source : sources) {
				hiddenEdges.addAll(network.getAdjacentEdgeList(source, CyEdge.Type.INCOMING));
			}
			// hides all outgoing directed edges from target nodes
			for (CyNode target : targets) {
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
		//HashMap<CyEdge, Double> edgeWeights = new HashMap<CyEdge, Double>();

		if (edgeWeightSetting == EdgeWeightSetting.UNWEIGHTED){
			for (CyEdge edge : network.getEdgeList()) {
				edgeWeights.put(edge, 1.);
			}
		}

		// applies edge penalty and then log transforms the edge weights for the
		// probability option
		else if (edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
			applyMultiplicativeEdgePenalty(edgePenalty);
			logTransformEdgeWeights();
		}

		// applies edge penalty for the additive option
		else if (edgeWeightSetting == EdgeWeightSetting.ADDITIVE) {
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
		for (CyNode source : sources) {
			CyEdge superEdge = network.addEdge(superSource, source, true);

			// sets an edge weight of 0, so the edges connecting the super nodes
			// and the sources/targets don't affect the final path weights
			Algorithms.setWeight(superEdge, 0.);
			superEdges.add(superEdge);
		}
		// attaches all targets to super target
		for (CyNode target : targets) {
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
		Double value = originalNetwork.getRow(e).get("edge_weight", Double.class);
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
	private void undoLogTransformPathLength(ArrayList<Path> paths) {
		// weighted probabilities option sets the weight to 2 ^ (-weight)
		if (edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
			for (Path p : paths) {
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
	
	/**
	 * Converts an array of node names to a list of the actual corresponding nodes
	 * @param names the names of the nodes that we want
	 * @return a list of the actual node objects with the given names
	 */
	private ArrayList<CyNode> stringsToNodes(HashSet<String> names) {
		ArrayList<CyNode> nodes = new ArrayList<CyNode>();

		for (String name : names) {
			if (idToCyNode.containsKey(name)) {
				nodes.add(idToCyNode.get(name));
			}
		}

		return nodes;
	}
}
