package com.dpgil.pathlinker.path_linker.internal;

import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import javax.swing.JOptionPane;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyNetwork;

/**
 * // -------------------------------------------------------------------------
 * /** Algorithms class for the PathLinker plugin. Contains all the algorithms
 * (Dijkstra's, Yen's KSP) used in PathLinker.
 *
 * @author Daniel Gil
 * @version Apr 23, 2015
 */
public class Algorithms
{
    private static final double            INFINITY = Integer.MAX_VALUE;
    private static HashSet<CyEdge>         initialHiddenEdges;
    private static HashSet<CyEdge>         hiddenEdges;
    private static HashMap<CyEdge, Double> _edgeWeights;
    // Boolean indicating whether or not to include the undirected outgoing/incoming edges
    // For now, always use both undirected and directed edges
    private static boolean                 includeUndirected = true; 


    /**
  	 * This heuristic function gives a lower bound on the path
	 * length to go the rest of the way to the finish from the start

	 * If this function returns infinity, then the target
	 * is necessarily unreachable, so don't expand the search along
	 * this edge
     *            
	 * @param minDists
     *            the map of nodes to their minimum distance from the target
	 * @param node
	 * 			  the node to check the distance for
	 * @return minimum distance to target or infinity if the target is unreachable
     *            
     */
    private static double heuristicF(
        HashMap<CyNode, Double> minDists,
        CyNode node)
    {
        if (minDists.containsKey(node))
            return minDists.get(node);
        else
            return INFINITY;
    }


    /**
     * Represents a pathway. Stores the list of nodes in order in the path and
     * the weight of the path
     */
    public static class Path
    {
        /** the list of nodes in order in the path */
        public ArrayList<CyNode> nodeList;
        /** the total weight of the path */
        public double            weight;


        /**
         * Constructor for the path class
         *
         * @param nodeList
         *            the list of nodes in order in the path
         * @param weight
         *            the total weight of the path
         */
        public Path(ArrayList<CyNode> nodeList, double weight)
        {
            this.nodeList = nodeList;
            this.weight = weight;
        }


        /**
         * Returns the size of the path
         *
         * @return size of the path
         */
        public int size()
        {
            return nodeList.size();
        }


        /**
         * Returns the ith node in the path
         *
         * @param i
         *            the index of the node to get
         * @return the ith node in the path
         */
        public CyNode get(int i)
        {
            return nodeList.get(i);
        }


        @Override
        public boolean equals(Object o)
        {
            if (o == null)
                return false;

            Path p = (Path)o;

            return this.nodeList.equals(p.nodeList);
        }


        @Override
        public int hashCode()
        {
            return nodeList.hashCode();
        }
    }


    /**
     * Computes the k shortest acyclic paths in the supplied network using Yen's
     * algorithm. Assumes that this is NOT a multigraph (there is at most one
     * edge between any two nodes). A* is used as the pathfinding subroutine,
     * with the distances in the input graph as a heuristic. Because the
     * algorithm computes paths over subsets of the initial heuristic is valid
     * and effective. If the graph contains n < k paths, n paths will be
     * returned.
     *
     * @param network
     *            the supplied network
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @param maxK
     *            the number of shortest paths
     * @return a list of k-shortest paths in sorted order by cost
     */
    public static ArrayList<Path> ksp(
        CyNetwork network,
        CyNode source,
        CyNode target,
        int maxK)
    {
        // the list of shortest paths
        ArrayList<Path> A = new ArrayList<Path>();

        // compute the original distance from the source to use for the
        // heuristic function
        HashMap<CyNode, Double> minDists =
            reverseSingleSourceDijkstra(network, target);

        // compute the initial shortest path to initialize Yen's
        Path shortestPath = dijkstra(network, source, target);

        // there is no path from source to target
        if (shortestPath == null)
            return A;

        A.add(shortestPath);

        // the heap, stores the potential k shortest paths
        ArrayList<Path> B = new ArrayList<Path>();

        // A cache mapping prefixes of accepted paths to the next node after
        // the prefix. Used to avoid scanning all previous paths many times,
        // which otherwise dominates runtime.
        HashMap<ArrayList<CyNode>, ArrayList<CyNode>> prefixCache =
            new HashMap<ArrayList<CyNode>, ArrayList<CyNode>>();
        for (int i = 1; i < shortestPath.size(); i++)
        {
            ArrayList<CyNode> subPath =
                new ArrayList<CyNode>(shortestPath.nodeList.subList(0, i));
            CyNode node = shortestPath.get(i);

            if (prefixCache.containsKey(subPath))
            {
                prefixCache.get(subPath).add(node);
            }
            else
            {
                prefixCache
                    .put(subPath, new ArrayList<CyNode>(Arrays.asList(node)));
            }
        }

        for (int k = 1; k < maxK; k++)
        {
            // previously computed shortest path
            Path latestPath = A.get(A.size() - 1);

            // process each node of the most recently found path, computing the
            // shortest path that deviates at that node and adding it to the
            // candidate heap
            for (int i = 0; i < latestPath.size() - 1; i++)
            {
                CyNode nodeSpur = latestPath.get(i);
                List<CyNode> pathRoot = latestPath.nodeList.subList(0, i + 1);

                // hide edges incoming to x until iteration k is over to avoid
                // finding cycles. note that this effect is cumulative, meaning
                // that while processing the current node in the path, all
                // incoming edges to this node and all previous nodes have
                // been hidden
                List<CyEdge> inEdges =
                    getAdjacentEdgeList(network, nodeSpur, CyEdge.Type.INCOMING);
                for (CyEdge inEdge : inEdges)
                {
                    hiddenEdges.add(inEdge);
                }

                // for each previously-found shortest path P_j with the same
                // first i nodes as the first i nodes of prevPath, hide the
                // edge from x to the i+1 node in P_j to ensure we don't
                // re-find a previously found path. Lookup the prefixes in a
                // cache to disallow them. Requires more memory to store the
                // cache, but saves scanning the list of found paths
                for (CyNode repNode : prefixCache
                    .get(latestPath.nodeList.subList(0, i + 1)))
                {
                    CyEdge repEdge = getEdge(network, nodeSpur, repNode);

                    if (repEdge != null)
                    {
                        hiddenEdges.add(repEdge);
                    }
                }

                // find the shortest path using A*
                Path pathSpur =
                    shortestPathAStar(network, nodeSpur, target, minDists);

                // short circuit if the target node was unreachable, which is
                // expected to happen as we remove edges
                if (pathSpur != null)
                {
                    // concatenates prevPath[:i+1] and the shortest path from
                    // nodeSpur to the target, and add this path to candidates
                    ArrayList<CyNode> pathTotal = new ArrayList<CyNode>(
                        pathRoot.subList(0, pathRoot.size() - 1));
                    pathTotal.addAll(pathSpur.nodeList);

                    double distTotal = computePathDist(network, pathTotal);
                    Path potentialK = new Path(pathTotal, distTotal);

                    if (!B.contains(potentialK))
                    {
                        B.add(potentialK);
                    }
                }
            }

            resetHiddenEdges();

            if (B.size() > 0)
            {
                // sorts the candidate paths by their weight
                Collections.sort(B, new Comparator<Path>() {

                    @Override
                    public int compare(Path path1, Path path2)
                    {
                        return Double.compare(path1.weight, path2.weight);
                    }

                });

                // accepts the next shortest path on the candidates heap, which
                // is necessarily the next shortest path
                Path newShortest = B.remove(0);

                // adds this to the list of prefixes for efficient lookup later
                for (int i = 1; i < newShortest.size(); i++)
                {
                    CyNode currNode = newShortest.get(i);
                    ArrayList<CyNode> subPath = new ArrayList<CyNode>(
                        newShortest.nodeList.subList(0, i));

                    if (!prefixCache.containsKey(subPath))
                        prefixCache.put(subPath, new ArrayList<CyNode>());

                    ArrayList<CyNode> cachedPath = prefixCache.get(subPath);
                    if (!cachedPath.contains(currNode))
                        cachedPath.add(currNode);
                }

                // adds the next shortest path to the accepted list of paths
                A.add(newShortest);
            }
            else
            {
                // terminates early if there are no more paths found from the
                // source to the target
                break;
            }
        }

        return A;
    }


    /**
     * Used to store data about a node while performing A*
     */
    private static class AStarData
    {
        public double heurDist;
        public CyNode node;
        public double actDist;


        public AStarData(double heurDist, CyNode node, double actDist)
        {
            this.heurDist = heurDist;
            this.node = node;
            this.actDist = actDist;
        }
    }


    /**
     * "Resets" hidden edges, by changing hiddenEdges state to what it was
     * initialized to
     */
    private static void resetHiddenEdges()
    {
        hiddenEdges.clear();
        hiddenEdges.addAll(initialHiddenEdges);
    }


    /**
     * An implementation of the A* algorithm. Computes exact shortest paths in
     * the network, utilizing the fact that the heuristic function is monotonic
     *
     * @param network
     *            the supplied network
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @param minDists
     *            the map of nodes to their minimum distance from the target
     * @return a path from source to target and its weight
     */
    public static Path shortestPathAStar(
        CyNetwork network,
        CyNode source,
        CyNode target,
        final HashMap<CyNode, Double> minDists)
    {
        Path currPath = new Path(new ArrayList<CyNode>(), 0.);

        // if source==target:
        // return ({source:0}, {source:[source]})
        if (source.equals(target))
        {
            return currPath;
        }

        // dictionary of final distances
        HashMap<CyNode, Double> distances = new HashMap<CyNode, Double>();
        // dictionary of paths
        HashMap<CyNode, CyNode> preds = new HashMap<CyNode, CyNode>();
        // map of seen nodes and their distancess
        HashMap<CyNode, Double> seen = new HashMap<CyNode, Double>();
        seen.put(source, 0.);

        // heap of nodes on the border to process, keyed by heuristic distance
        PriorityQueue<AStarData> fringe =
            new PriorityQueue<AStarData>(10, new Comparator<AStarData>() {
                @Override
                public int compare(AStarData data1, AStarData data2)
                {
                    return Double.compare(data1.heurDist, data2.heurDist);
                }

            });

        fringe.add(new AStarData(heuristicF(minDists, source), source, 0));

        // real-valued edge weights can cause the search to fail due to
        // accumulated error summing along the path. test with a relative
        // epsilon to catch only the 'real' errors
        final double REL_EPS = 1E-10;

        // iteratively search the graph outward until we've processed all nodes
        while (fringe.size() > 0)
        {
            AStarData currData = fringe.poll();
            CyNode currNode = currData.node;

            // if we've already processed this node, don't re-process it. this
            // happens beecause when we see a better path to an already seen
            // node, it's cheaper to leave it in the heap and deal with it here
            // than to remove it
            if (distances.containsKey(currNode))
                continue;

            // process this node, this is necessarily the best possible path
            // to it
            distances.put(currNode, currData.actDist);

            // check for a solution
            if (currNode.equals(target))
                break;

            // examine all neighbors to this node and consider adding them to
            // the fringe
            List<CyNode> neighbors =
                getNeighborList(network, currNode, CyEdge.Type.OUTGOING);
            for (CyNode nextNode : neighbors)
            {
            	CyEdge nextEdge = getEdge(network, currNode, nextNode);
                // doesn't consider edges that are hidden. uses this structure
                // of hiding edges because manipulating the graph completely
                // dominates runtime in cytoscape
                if (hiddenEdges.contains(nextEdge))
                {
                    continue;
                }

                // the actual distance to the node from the source
                double nextActDist =
                    currData.actDist + getWeight(network, nextEdge);

                // the heuristic function gives a lower bound on the path
                // length to go the rest of the way to the finish from the start
                double nextHeurDist =
                    nextActDist + heuristicF(minDists, nextNode);

                // if the heuristic function returns infinity, then the target
                // is necessarily unreachable, so don't expand the search along
                // this edge
                if (isInf(heuristicF(minDists, nextNode)))
                    continue;

                // if we've already processed the neighbor, then this can't
                // possibly be a better path, assuming the problem is
                // well-formed
                if (distances.containsKey(nextNode))
                {
                    // verify that the graph and heuristic don't break the
                    // search property
                    if ((nextActDist * (1 + REL_EPS)) < distances.get(nextNode))
                    {
                        JOptionPane.showMessageDialog(
                            null,
                            "Contradictory search path. Bad heuristic? Negative weights?");
                        return null;
                    }
                }
                // if this node hasn't already been processed, we need to
                // consider adding it to the heap. if it's not already in the
                // heap, we should only add it if this path to it is an
                // improvement over the previous path. for performance, we leave
                // the old entry in the heap in that case and skip it when it
                // pops out
                else if (!seen.containsKey(nextNode)
                    || nextActDist < seen.get(nextNode))
                {
                    seen.put(nextNode, nextActDist);
                    fringe.add(
                        new AStarData(nextHeurDist, nextNode, nextActDist));
                    preds.put(nextNode, currNode);
                }
            }
        }

        // builds the path and returns it
        ArrayList<CyNode> nodeList = constructNodeList(preds, source, target);
        if (nodeList == null)
            return null;

        return new Path(nodeList, distances.get(target));
    }


    /**
     * Computes the shortest distance from a source to every node in the graph
     *
     * @param network
     *            the supplied network
     * @param source
     *            the source
     * @return a map of nodes and their distances from the source
     */
    public static HashMap<CyNode, Double> reverseSingleSourceDijkstra(
        CyNetwork network,
        CyNode source)
    {
        final HashMap<CyNode, Double> distances = new HashMap<CyNode, Double>();
        HashMap<CyNode, CyNode> previous = new HashMap<CyNode, CyNode>();
        PriorityQueue<CyNode> pq =
            new PriorityQueue<CyNode>(10, new Comparator<CyNode>() {
                @Override
                public int compare(CyNode o1, CyNode o2)
                {
                    return Double.compare(distances.get(o1), distances.get(o2));
                }
            });

        // intializes distances
        for (CyNode v : network.getNodeList())
        {
            distances.put(v, INFINITY);
            previous.put(v, null);
        }
        distances.put(source, 0.);
        pq.add(source);

        while (!pq.isEmpty())
        {
            CyNode current = pq.poll();

            // goes through incoming neighbors because we are finding the paths
            // that lead to the target. however, we don't want to reverse the
            // network and call a normal SSD because manipulating the network
            // dominates runtime in Cytoscape
            for (CyNode neighbor : 
            	getNeighborList(network, current, CyEdge.Type.INCOMING))
            {

            	CyEdge neighborEdge = getEdge(network, neighbor, current);

                double newCost =
                    distances.get(current) + getWeight(network, neighborEdge);

                if (newCost < distances.get(neighbor))
                {
                    distances.put(neighbor, newCost);
                    previous.put(neighbor, current);

                    // re-add to priority queue
                    pq.remove(neighbor);
                    pq.add(neighbor);
                }
            }
        }

        return distances;
    }


    /**
     * Computes the shortest distance from a source to every node in the graph
     *
     * @param network
     *            the supplied network
     * @param source
     *            the source
     * @return a map of nodes and their distances from the source
     */
    public static HashMap<CyNode, Double> singleSourceDijkstra(
        CyNetwork network,
        CyNode source)
    {
        HashMap<CyNode, Double> distances = new HashMap<CyNode, Double>();
        HashMap<CyNode, CyNode> previous = new HashMap<CyNode, CyNode>();
        ArrayList<CyNode> pq = new ArrayList<CyNode>();

        // intializes distances
        for (CyNode v : network.getNodeList())
        {
            distances.put(v, INFINITY);
            previous.put(v, null);
        }
        distances.put(source, 0.);
        pq.add(source);

        while (!pq.isEmpty())
        {
            CyNode current = pq.remove(0);

            // goes through the neighbors
            for (CyNode neighbor : 
            	getNeighborList(network, current, CyEdge.Type.OUTGOING))
            {
            	CyEdge neighborEdge = getEdge(network, current, neighbor);

                double newCost =
                    distances.get(current) + getWeight(network, neighborEdge);

                if (newCost < distances.get(neighbor))
                {
                    distances.put(neighbor, newCost);
                    previous.put(neighbor, current);

                    // add to priority queue
                    pq.remove(neighbor);
                    for (int i = 0; i < pq.size(); i++)
                    {
                        if (distances.get(neighbor) < distances.get(pq.get(i)))
                        {
                            pq.add(i, neighbor);
                            break;
                        }
                    }
                    if (!pq.contains(neighbor))
                        pq.add(neighbor);
                }
            }
        }

        return distances;
    }


    /**
     * Computes the shortest path from a source to a sink in the supplied
     * network
     *
     * @param network
     *            the network
     * @param source
     *            the source node of the graph
     * @param target
     *            the target node of the graph
     * @return the path from source to target
     */
    public static Path dijkstra(CyNetwork network, CyNode source, CyNode target)
    {
        final HashMap<CyNode, Double> distances = new HashMap<CyNode, Double>();
        HashMap<CyNode, CyNode> previous = new HashMap<CyNode, CyNode>();
        PriorityQueue<CyNode> pq =
            new PriorityQueue<CyNode>(10, new Comparator<CyNode>() {
                @Override
                public int compare(CyNode o1, CyNode o2)
                {
                    return Double.compare(distances.get(o1), distances.get(o2));
                }
            });

        // intializes distances
        for (CyNode v : network.getNodeList())
        {
            distances.put(v, INFINITY);
            previous.put(v, null);
        }
        distances.put(source, 0.);
        pq.add(source);

        while (!pq.isEmpty())
        {
            CyNode current = pq.poll();

            // short circuit
            if (current.equals(target))
            {
                // return path reconstructed
                break;
            }
            // goes through the neighbors
            for (CyNode neighbor : 
            	getNeighborList(network, current, CyEdge.Type.OUTGOING))
            {

            	CyEdge neighborEdge = getEdge(network, current, neighbor);
                double newCost =
                    distances.get(current) + getWeight(network, neighborEdge);

                if (newCost < distances.get(neighbor))
                {
                    distances.put(neighbor, newCost);
                    previous.put(neighbor, current);

                    // add to priority queue
                    pq.remove(neighbor);
                    pq.add(neighbor);
                }
            }
        }

        if (isInf(distances.get(target)))
        {
            // unreachable node
            return null;
        }

        // return constructed path
        ArrayList<CyNode> nodeList =
            constructNodeList(previous, source, target);
        if (nodeList == null)
            return null;

        return new Path(nodeList, distances.get(target));
    }


    /**
     * Initializes our local copy of the edge weights
     *
     * @param weights
     *            a map of the edge to its value
     */
    public static void setEdgeWeights(HashMap<CyEdge, Double> weights)
    {
        _edgeWeights = weights;
    }


    /**
     * Returns the weight of an edge in the network
     *
     * @param network
     *            the supplied network
     * @param edge
     *            the edge to find the weight of
     * @return double the weight of the edge. returns a very noticeable and
     *         obscure number if the weight is not in the network
     */
    public static double getWeight(CyNetwork network, CyEdge edge)
    {
        if (!_edgeWeights.containsKey(edge))
            return -44444;

        return _edgeWeights.get(edge);
    }


    /**
     * Sets the weight of an edge in the network. Doesn't actually set the edge
     * weight as an attribute, as that takes too much time. Instead, stores the
     * edge weight in our copy of the weights local to the algorithm.
     *
     * @param edge
     *            the edge to set the weight of
     * @param value
     *            the new weight of the edge
     */
    public static void setWeight(CyEdge edge, double value)
    {
        _edgeWeights.put(edge, value);
    }


    /**
     * Returns an edge that connects source to target if it exists, null
     * otherwise
     *
     * @param network
     *            the network
     * @param source
     *            the source of the edge
     * @param target
     *            the target of the edge
     * @return the edge connecting source and target, null otherwise
     */
    public static CyEdge getEdge(
        CyNetwork network,
        CyNode source,
        CyNode target)
    {
        List<CyEdge> dirConnections =
            network.getConnectingEdgeList(source, target, CyEdge.Type.DIRECTED);

        // getConnectingEdgeList() returns both the incoming and outgoing edges connected to the two nodes
        // Using CyEdge.Type.OUTGOING gave strange results in testing, so I stuck with this loop
        // Use this for loop to get the edge directed from source->target
        // Currently PathLinker does not support multi-graphs, so just return the first edge found
        for (CyEdge edge : dirConnections)
        {
            if (edge.getSource().equals(source)
                && edge.getTarget().equals(target))
            {

                return edge;
            }
        }

        // If undirected edges are allowed, then also check for an undirected edge between them
        // Currently directed edges are favored over undirected edges
        if (includeUndirected) 
        {
			List<CyEdge> undirConnections =
				network.getConnectingEdgeList(source, target, CyEdge.Type.UNDIRECTED);
			for (CyEdge edge : undirConnections)
			{
				return edge;
			}
        	
        }

        return null;
    }
    
    
    /**
     * Returns the list of edges connected to this node based on the specified CyEdge.Type
     * This is a wrapper around the built-in CyNetwork function getAdjacentEdgeList. In the built-in function:
     * If CyEdge.Type.OUTGOING is passed, the directed outgoing adjacent edges will be returned
     * If CyEdge.Type.INCOMING is passed, the directed incoming adjacent edges will be returned
     * If CyEdge.Type.DIRECTED is passed, the directed (outgoing and incoming) adjacent edges will be returned
     * If CyEdge.Type.UNDIRECTED is passed, the undirected adjacent edges will be returned
     * 
     * In networks containing both directed and undirected networks, we want to get both the directed and undirected adjacent edges
     * hence why this wrapper was made
     * 
     * Undirected edges will be included based on the value of includeUndirected which is currently always true
     * 
     * @param network
     * 				the network
     * @param node
     * 				the node to get the connecting edges
     * @param edgeType
     * 				Should be either CyEdge.Type.OUTGOING or CyEdge.Type.INCOMING
     * @return List<CyEdge> the list of edges (either outgoing or incoming) connected to the given node
     */
    public static List<CyEdge> getAdjacentEdgeList(CyNetwork network, CyNode node, CyEdge.Type edgeType)
    {
		
    	// retrieves all of the directed edges incoming to or outgoing from the given node
    	List<CyEdge> adjacentEdges = network.getAdjacentEdgeList(node, edgeType);

    	// If specified, add all of the undirected edges connected to the given node as well
    	if (includeUndirected){
    		adjacentEdges.addAll(network.getAdjacentEdgeList(node, CyEdge.Type.UNDIRECTED));
    	}

    	return adjacentEdges;
    }

    /**
     * Returns the list of nodes neighboring the input node based on the specified CyEdge.Type
     * This is a wrapper around the built-in CyNetwork function getNeighborList. In the built-in function:
     * If CyEdge.Type.OUTGOING is passed, the nodes connected by a directed outgoing edge will be returned
     * If CyEdge.Type.INCOMING is passed, the nodes connected by a directed incoming edge will be returned
     * If CyEdge.Type.DIRECTED is passed, the nodes connected by a directed (outgoing and incoming) edge will be returned
     * If CyEdge.Type.UNDIRECTED is passed, the nodes connected by an undirected edge will be returned
     * 
     * In networks containing both directed and undirected networks, we want to get both the directed and undirected neighbors
     * hence why this wrapper was made
     * 
     * Undirected edges will be included based on the value of includeUndirected which is currently always true
     * 
     * @param network
     * 				the network
     * @param node
     * 				the node for which to get the neighboring nodes
     * @param edgeType
     * 				Should be either CyEdge.Type.OUTGOING or CyEdge.Type.INCOMING
     * @return List<CyNode> the list of incoming or outgoing neighboring nodes 
     */
    public static List<CyNode> getNeighborList(CyNetwork network, CyNode node, CyEdge.Type edgeType)
    {
		
    	// retrieves all of the directed edges incoming to or outgoing from the given node
    	List<CyNode> neighbors = network.getNeighborList(node, edgeType);

    	// If specified, add all of the undirected edges connected to the given node as well
    	if (includeUndirected){
    		neighbors.addAll(network.getNeighborList(node, CyEdge.Type.UNDIRECTED));
    	}

    	return neighbors;
    }


    /**
     * Finds a path from a source to a sink using a supplied previous node list
     *
     * @param previous
     *            a map of node predecessors
     * @param source
     *            the source node of the graph
     * @param target
     *            the target node of the graph
     * @return list of nodes in the path
     */
    public static ArrayList<CyNode> constructNodeList(
        HashMap<CyNode, CyNode> previous,
        CyNode source,
        CyNode target)
    {
        ArrayList<CyNode> nodeList = new ArrayList<CyNode>();

        // constructs the path
        CyNode iter = target;
        do
        {
            nodeList.add(iter);

            if (!previous.containsKey(iter))
                return null;
        }
        while (!(iter = previous.get(iter)).equals(source));

        nodeList.add(source);
        Collections.reverse(nodeList);

        return nodeList;
    }


    /**
     * Returns if a value is within an epsilon of INFINITY
     *
     * @param value
     *            the value to check
     * @return true if the value is INFINITY, false otherwise
     */
    private static boolean isInf(double value)
    {
        return Math.abs(value - INFINITY) < 0.1;
    }


    /**
     * Initializes the set of hidden edges. Used to start out the algorithm
     * hiding all source in edges and target out edges.
     *
     * @param edges
     *            the initial hidden edges
     */
    public static void initializeHiddenEdges(HashSet<CyEdge> edges)
    {
        initialHiddenEdges = new HashSet<CyEdge>();
        initialHiddenEdges.addAll(edges);

        hiddenEdges = new HashSet<CyEdge>();
        hiddenEdges.addAll(edges);
    }


    /**
     * Computes the weight of a given path
     *
     * @param network
     *            the supplied network
     * @param nodeList
     *            the list of nodes that make up the path, in order
     */
    private static double computePathDist(
        CyNetwork network,
        ArrayList<CyNode> nodeList)
    {
        double sum = 0.;

        for (int i = 0; i < nodeList.size() - 1; i++)
        {
            CyNode eSource = nodeList.get(i);
            CyNode eTarget = nodeList.get(i + 1);

            CyEdge edge = Algorithms.getEdge(network, eSource, eTarget);

            sum += getWeight(network, edge);
        }

        return sum;
    }
}
