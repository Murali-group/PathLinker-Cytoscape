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
    private static final double    INFINITY = Integer.MAX_VALUE;
    private static HashSet<CyEdge> pathHiddenEdges;


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
     *
     * @author Daniel
     * @version Oct 9, 2015
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
     * Runs Yen's k-shortest paths algorithm on the supplied network given a
     * source, target, and k
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

        HashMap<CyNode, Double> minDists =
            reverseSingleSourceDijkstra(network, target);

        // A[0] = shortest path
        Path shortestPath = dijkstra(network, source, target);

        // there is no path from source to target
        if (shortestPath == null)
            return A;

        A.add(shortestPath);

        // B = []; the heap
        ArrayList<Path> B = new ArrayList<Path>();

        // prefixCache = defaultdict(list)
        // A cache mapping prefixes of accepted paths to the next node after
        // the prefix. Used to avoid scanning all previous paths many time,
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

        // for k in range(1, max_k)
        for (int k = 1; k < maxK; k++)
        {
            // edges_removed = []
            pathHiddenEdges = new HashSet<CyEdge>();

            // for i in range(0, len(A[-1]['path]) - 1)
            Path latestPath = A.get(A.size() - 1);
            for (int i = 0; i < latestPath.size() - 1; i++)
            {
                // node_spur = A[-1]['path'][i]
                CyNode nodeSpur = latestPath.get(i);
                // path_root = A[-1]['path'][:i+1]
                List<CyNode> pathRoot = latestPath.nodeList.subList(0, i + 1);

                // to avoid cycles
                List<CyEdge> inEdges =
                    network.getAdjacentEdgeList(nodeSpur, CyEdge.Type.INCOMING);
                for (CyEdge inEdge : inEdges)
                {
                    pathHiddenEdges.add(inEdge);
                }

                // for each previously-found shortest path P_j with the same
                // first i nodes as
                // the first i nodes of prevPath, hide the edge from x to the
                // i+1 node in P_j
                // to ensure we don't re-find a previously found path.
                // Lookup the prefixes in a cache to disallow them. Requires
                // more memory
                // to store the cache, but saves scanning the list of found
                // paths,
                // which otherwise dominates runtime
                for (CyNode repNode : prefixCache
                    .get(latestPath.nodeList.subList(0, i + 1)))
                {
                    CyEdge repEdge = getEdge(network, nodeSpur, repNode);

                    if (repEdge != null)
                    {
                        pathHiddenEdges.add(repEdge);
                    }
                }

                // path_spur = a_star(graph, node_spur, node_end)
                Path pathSpur =
                    shortestPathAStar(network, nodeSpur, target, minDists);

                // if path_spur['path']:
                if (pathSpur != null)
                {
                    // path_total = path_root[:-1] + path_spur['path']
                    ArrayList<CyNode> pathTotal = new ArrayList<CyNode>(
                        pathRoot.subList(0, pathRoot.size() - 1));
                    pathTotal.addAll(pathSpur.nodeList);

                    // dist_total = distances[node_spur] + path_spur['cost']
                    double distTotal = computePathDist(network, pathTotal);

                    // potential_k = {'cost': dist_total, 'path':
                    // path_total}
                    Path potentialK = new Path(pathTotal, distTotal);

                    // if not (potential_k in B):
                    if (!B.contains(potentialK))
                    {
                        // B.append(potential_k)
                        B.add(potentialK);
                    }
                }
            }

            // if len(B):
            if (B.size() > 0)
            {
                // B = sorted(B, key=itemgetter('cost'))
                Collections.sort(B, new Comparator<Path>() {

                    @Override
                    public int compare(
                        Path path1,
                        Path path2)
                    {
                        return Double.compare(path1.weight, path2.weight);
                    }

                });

                Path newShortest = B.remove(0);

                for (int i = 1; i < newShortest.size(); i++)
                {
                    CyNode currNode = newShortest.get(i);
                    ArrayList<CyNode> subPath =
                        new ArrayList<CyNode>(newShortest.nodeList.subList(0, i));

                    if (!prefixCache.containsKey(subPath))
                        prefixCache.put(subPath, new ArrayList<CyNode>());

                    ArrayList<CyNode> cachedPath = prefixCache.get(subPath);
                    if (!cachedPath.contains(currNode))
                        cachedPath.add(currNode);
                }

                // A.append(B[0])
                // B.pop(0)
                A.add(newShortest);
            }
            else
            {
                break;
            }
        }

        return A;
    }


    private static class AStarData
    {
        public double    heurDist;
        public CyNode    node;
        public double    actDist;


        public AStarData(double heurDist, CyNode node, double actDist)
        {
            this.heurDist = heurDist;
            this.node = node;
            this.actDist = actDist;
        }
    }


    /**
     * Finds the shortest path between source and target using the A* algorithm
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

        // dist = {} | dictionary of final distances
        HashMap<CyNode, Double> distances = new HashMap<CyNode, Double>();
        // preds = {source:None} | dictionary of paths
        HashMap<CyNode, CyNode> preds = new HashMap<CyNode, CyNode>();
        // seen = {source:0} | map of seen nodes and their dists
        HashMap<CyNode, Double> seen = new HashMap<CyNode, Double>();
        seen.put(source, 0.);

        // fringe = [] | heap of nodes on the border to process, keyed by
        // heuristic distance
        PriorityQueue<AStarData> fringe =
            new PriorityQueue<AStarData>(10, new Comparator<AStarData>() {
                @Override
                public int compare(AStarData data1, AStarData data2)
                {
                    return Double.compare(data1.heurDist, data2.heurDist);
                }

            });

        // heapq.heappush(fringe, (heuristicF(source), (source, 0)))
        fringe.add(new AStarData(heuristicF(minDists, source), source, 0));

        // REL_EPS = 1e-10
        final double REL_EPS = 1E-10;

        // while fringe:
        while (fringe.size() > 0)
        {
            // (heurDist, (currNode, actualDist)) = heapq.heappop(fringe)
            AStarData currData = fringe.poll();
            CyNode currNode = currData.node;

            // if currNode in dist: continue
            if (distances.containsKey(currNode))
                continue;

            // dist[currNode] = actualDist
            distances.put(currNode, currData.actDist);

            // if currNode == target: break
            if (currNode.equals(target))
                break;

            // currEdges = iter(net[currNode].items())
            // List<CyNode> neighbors =
            // network.getNeighborList(currNode, CyEdge.Type.OUTGOING);
            List<CyEdge> neighbors =
                network.getAdjacentEdgeList(currNode, CyEdge.Type.OUTGOING);

            // for nextNode, edgedata in currEdges
            for (CyEdge nextEdge : neighbors)
            {
                if (pathHiddenEdges.contains(nextEdge))
                {
                    continue;
                }

                CyNode nextNode = nextEdge.getTarget();

                // nextActDist = actualDist + edgedata.get(weight, 1)
//                int edgeWeight = 1;
                double nextActDist = currData.actDist + getWeight(network, nextEdge);

                // nextHeurDist = nextActDist + heuristicF(nextNode)
                double nextHeurDist = nextActDist + heuristicF(minDists, nextNode);

                // if isinf(nextHeurDist): continue
                if (isInf(heuristicF(minDists, nextNode)))
                    continue;

                // if nextNode in dist
                if (distances.containsKey(nextNode))
                {
                    // if (nextActDist * (1+REL_EPS)) < dist[nextNode]:
                    if ((nextActDist * (1 + REL_EPS)) < distances.get(nextNode))
                    {
                        // raise ValueError('Contradictory search path:', 'bad
                        // heuristic? negative weights?')
// throw new Exception(
// "Contradictory search path: bad heuristic? negative weights?");
                        JOptionPane.showMessageDialog(
                            null,
                            "Contradictory search path. Bad heuristic? Negative weights?");
                        return null;
                    }
                }
                // elif nextNode not in seen or nextActDist < seen[nextNode]:
                else if (!seen.containsKey(nextNode)
                    || nextActDist < seen.get(nextNode))
                {
                    // seen[nextNode] = nextActDist
                    seen.put(nextNode, nextActDist);
                    // heapq.heappush(fringe, (nextHeurDist, (nextNode,
                    // nextActDist)))
                    fringe.add(
                        new AStarData(nextHeurDist, nextNode, nextActDist));
                    // preds[nextNode] = currNode
                    preds.put(nextNode, currNode);
                }
            }
        }

        ArrayList<CyNode> nodeList = constructNodeList(preds, source, target);
        if (nodeList == null) return null;

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
        final HashMap<CyNode, Double> distances =
            new HashMap<CyNode, Double>();
        HashMap<CyNode, CyNode> previous = new HashMap<CyNode, CyNode>();
        PriorityQueue<CyNode> pq =
            new PriorityQueue<CyNode>(10, new Comparator<CyNode>() {
                @Override
                public int compare(CyNode o1, CyNode o2)
                {
                    return Double
                        .compare(distances.get(o1), distances.get(o2));
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
            // that lead to the target
            // however, we don't want to reverse the network and call a normal
            // SSD because
            // reversing the network dominates runtime in Cytoscape
            for (CyEdge neighborEdge : network
                .getAdjacentEdgeList(current, CyEdge.Type.INCOMING))
            {
                CyNode neighbor = neighborEdge.getSource();

//                int edgeWeight = 1;
                double newCost = distances.get(current) + getWeight(network, neighborEdge);

                if (newCost < distances.get(neighbor))
                {
                    distances.put(neighbor, newCost);
                    previous.put(neighbor, current);

                    // Q[u] = cost_vu
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
            for (CyEdge neighborEdge : network
                .getAdjacentEdgeList(current, CyEdge.Type.OUTGOING))
            {
                CyNode neighbor = neighborEdge.getTarget();

// int edgeWeight = 1;
                double newCost = distances.get(current) + getWeight(network, neighborEdge);

                if (newCost < distances.get(neighbor))
                {
                    distances.put(neighbor, newCost);
                    previous.put(neighbor, current);

                    // Q[u] = cost_vu
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
     * @return list of nodes that make up the path from source to target
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
            for (CyEdge neighborEdge : network
                .getAdjacentEdgeList(current, CyEdge.Type.OUTGOING))
            {
                CyNode neighbor = neighborEdge.getTarget();

// int edgeWeight = 1;
                double newCost = distances.get(current) + getWeight(network, neighborEdge);

                if (newCost < distances.get(neighbor))
                {
                    distances.put(neighbor, newCost);
                    previous.put(neighbor, current);

                    // Q[u] = cost_vu
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
     * Returns the weight of an edge in the network
     *
     * @param network
     *            the supplied network
     * @param edge
     *            the edge to find the weight of
     * @return double the weight of the edge
     */
    public static double getWeight(CyNetwork network, CyEdge edge)
    {
        Double entry = network.getRow(edge).get("edge_weight", Double.class);
        return entry != null ? entry.doubleValue() : 1;
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
        List<CyEdge> connections =
            network.getConnectingEdgeList(source, target, CyEdge.Type.DIRECTED);

        for (CyEdge edge : connections)
        {
            if (edge.getSource().equals(source)
                && edge.getTarget().equals(target))
            {

                return edge;
            }
        }

        return null;
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
     */
    private static boolean isInf(double value)
    {
        return Math.abs(value - INFINITY) < 0.1;
    }


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

            Double entry =
                network.getRow(edge).get("edge_weight", Double.class);
            double edgeWeight = entry != null ? entry.doubleValue() : 1;
            sum += edgeWeight;
        }

        return sum;
    }
}
