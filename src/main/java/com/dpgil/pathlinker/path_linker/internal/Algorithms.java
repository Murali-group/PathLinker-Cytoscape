package com.dpgil.pathlinker.path_linker.internal;

import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import javax.swing.JOptionPane;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
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
    private static final int INFINITY = Integer.MAX_VALUE;


    /**
     * Reverses all the edges in a CyNetwork by removing all and then readding
     * all of them in the opposite direction
     *
     * @param network
     *            the supplied network
     */
    public static void reverseNetwork(CyNetwork network)
    {
        List<CyEdge> edges = network.getEdgeList();
        network.removeEdges(edges);
        for (CyEdge edge : edges)
            network.addEdge(edge.getTarget(), edge.getSource(), true);
    }


    public static int heuristicF(HashMap<CyNode, Integer> minDists, CyNode node)
    {
        if (minDists.containsKey(node))
            return minDists.get(node);
        else
            return INFINITY;
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
     * @throws Exception
     */
    public static ArrayList<ArrayList<CyNode>> ksp(
        CyNetwork network,
        CyNode source,
        CyNode target,
        int maxK)
    {
        // the list of shortest paths
        ArrayList<ArrayList<CyNode>> A = new ArrayList<ArrayList<CyNode>>();

        // network.reverse();
        reverseNetwork(network);
        // minDists = single_source_dijkstra(network, target);
        HashMap<CyNode, Integer> minDists =
            singleSourceDijkstra(network, target);
        // network.reverse();
        reverseNetwork(network);

        // A[0] = shortest path
        ArrayList<CyNode> shortestPath = dijkstra(network, source, target);

        // there is no path from source to target
        if (shortestPath == null)
            return A;

        A.add(shortestPath);

        // B = []; the heap
        ArrayList<ArrayList<CyNode>> B = new ArrayList<ArrayList<CyNode>>();

        // prefixCache = defaultdict(list)
        // A cache mapping prefixes of accepted paths to the next node after
        // the prefix. Used to avoid scanning all previous paths many time,
        // which otherwise dominates runtime.
        HashMap<ArrayList<CyNode>, ArrayList<CyNode>> prefixCache =
            new HashMap<ArrayList<CyNode>, ArrayList<CyNode>>();
        for (int i = 1; i < shortestPath.size(); i++)
        {
            ArrayList<CyNode> subPath =
                new ArrayList<CyNode>(shortestPath.subList(0, i));
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
            ArrayList<RemovedEdge> edgesRemoved = new ArrayList<RemovedEdge>();

            // for i in range(0, len(A[-1]['path]) - 1)
            ArrayList<CyNode> latestPath = A.get(A.size() - 1);
            for (int i = 0; i < latestPath.size() - 1; i++)
            {
                // node_spur = A[-1]['path'][i]
                CyNode nodeSpur = latestPath.get(i);
                // path_root = A[-1]['path'][:i+1]
                List<CyNode> pathRoot = latestPath.subList(0, i + 1);

                // to avoid cycles
                List<CyEdge> inEdges =
                    network.getAdjacentEdgeList(nodeSpur, CyEdge.Type.INCOMING);
                for (CyEdge inEdge : inEdges)
                {
                    edgesRemoved.add(
                        new RemovedEdge(
                            inEdge.getSource(),
                            inEdge.getTarget()));
                }
                network.removeEdges(inEdges);

//                # for each previously-found shortest path P_j with the same first i nodes as
//                # the first i nodes of prevPath, hide the edge from x to the i+1 node in P_j
//                # to ensure we don't re-find a previously found path.
//                # Lookup the prefixes in a cache to disallow them. Requires more memory
//                # to store the cache, but saves scanning the list of found paths
                for (CyNode repNode : prefixCache.get(latestPath.subList(0, i+1)))
                {
                    CyEdge repEdge = getEdge(network, nodeSpur, repNode);
                    if (repEdge != null)
                    {
                        edgesRemoved.add(new RemovedEdge(nodeSpur, repNode));
                        network.removeEdges(Arrays.asList(repEdge));
                    }
                }

//                // scans all previous paths
//                // for path_k in A:
//                for (ArrayList<CyNode> currPath : A)
//                {
//                    // if len(curr_path) > i and path_root == curr_path[:i+1]:
//                    if (currPath.size() > i
//                        && pathRoot.equals(currPath.subList(0, i + 1)))
//                    {
//                        // cost = graph.remove_edge(curr_path[i],
//                        // curr_path[i+1])
//                        CyEdge toRemove = getEdge(
//                            network,
//                            currPath.get(i),
//                            currPath.get(i + 1));
//
//                        // if cost == -1: continue
//                        if (toRemove == null)
//                            continue;
//
//                        // edges_removed.append([curr_path[i], curr_path[i+1],
//                        // cost])
//                        network.removeEdges(Arrays.asList(toRemove));
//                        edgesRemoved.add(
//                            new RemovedEdge(
//                                currPath.get(i),
//                                currPath.get(i + 1)));
//                    }
//                }

                // path_spur = a_star(graph, node_spur, node_end)
                ArrayList<CyNode> pathSpur = shortestPathAStar(
                    network,
                    nodeSpur,
                    target,
                    minDists,
                    false);

                // if path_spur['path']:
                if (pathSpur != null)
                {
                    // path_total = path_root[:-1] + path_spur['path']
                    ArrayList<CyNode> pathTotal = new ArrayList<CyNode>(
                        pathRoot.subList(0, pathRoot.size() - 1));
                    pathTotal.addAll(pathSpur);

                    // dist_total = distances[node_spur] + path_spur['cost']

                    // potential_k = {'cost': dist_total, 'path':
                    // path_total}

                    // if not (potential_k in B):
                    if (!B.contains(pathTotal))
                    {
                        // B.append(potential_k)
                        B.add(pathTotal);
                    }
                }
            }

            // restores removed edges
            for (RemovedEdge removed : edgesRemoved)
            {
                network.addEdge(removed.source, removed.target, true);
            }

            // if len(B):
            if (B.size() > 0)
            {
                // B = sorted(B, key=itemgetter('cost'))
                Collections.sort(B, new Comparator<ArrayList<CyNode>>() {

                    @Override
                    public int compare(
                        ArrayList<CyNode> path1,
                        ArrayList<CyNode> path2)
                    {
                        return Integer.compare(path1.size(), path2.size());
                    }

                });

                ArrayList<CyNode> newShortest = B.remove(0);

                for (int i = 1; i < newShortest.size(); i++)
                {
                    CyNode currNode = newShortest.get(i);
                    ArrayList<CyNode> subPath = new ArrayList<CyNode>(newShortest.subList(0, i));

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
        public int    heurDist;
        public CyNode node;
        public int    actDist;


        public AStarData(int heurDist, CyNode node, int actDist)
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
     * @return a list of the nodes in order of the path from source to target
     */
    public static ArrayList<CyNode> shortestPathAStar(
        CyNetwork network,
        CyNode source,
        CyNode target,
        final HashMap<CyNode, Integer> minDists,
        boolean print)
    {
        if (print)
            JOptionPane.showMessageDialog(null, "line 257");

        ArrayList<CyNode> path = new ArrayList<CyNode>();

        // if source==target:
        // return ({source:0}, {source:[source]})
        if (source.equals(target))
        {
            if (print)
                JOptionPane.showMessageDialog(null, "source equals target");
            return path;
        }

        // dist = {} | dictionary of final distances
        HashMap<CyNode, Integer> distances = new HashMap<CyNode, Integer>();
        // preds = {source:None} | dictionary of paths
        HashMap<CyNode, CyNode> preds = new HashMap<CyNode, CyNode>();
        // seen = {source:0} | map of seen nodes and their dists
        HashMap<CyNode, Integer> seen = new HashMap<CyNode, Integer>();
        seen.put(source, 0);

        if (print)
            JOptionPane.showMessageDialog(null, "line 276");

        // fringe = [] | heap of nodes on the border to process, keyed by
        // heuristic distance
        PriorityQueue<AStarData> fringe =
            new PriorityQueue<AStarData>(10, new Comparator<AStarData>() {
                @Override
                public int compare(AStarData data1, AStarData data2)
                {
                    return Integer.compare(data1.heurDist, data2.heurDist);
                }

            });

        if (print)
            JOptionPane.showMessageDialog(null, "line 290");

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
            List<CyNode> neighbors =
                network.getNeighborList(currNode, CyEdge.Type.OUTGOING);

            if (print)
                JOptionPane.showMessageDialog(null, "line 320");

            // for nextNode, edgedata in currEdges
            for (CyNode nextNode : neighbors)
            {
                // nextActDist = actualDist + edgedata.get(weight, 1)
                int edgeWeight = 1;
                int nextActDist = currData.actDist + edgeWeight;

                // nextHeurDist = nextActDist + heuristicF(nextNode)
                int nextHeurDist = nextActDist + heuristicF(minDists, nextNode);

                // if isinf(nextHeurDist): continue
                if (heuristicF(minDists, nextNode) == INFINITY)
                    continue;

                if (print)
                    JOptionPane.showMessageDialog(null, "line 336");

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

                if (print)
                    JOptionPane.showMessageDialog(null, "line 366");
            }

            if (print)
                JOptionPane.showMessageDialog(null, "line 369");
        }

        if (print)
            JOptionPane.showMessageDialog(null, "line 372");
        return constructPath(preds, source, target);
    }


    public static ArrayList<CyNode> shortestPathAStarFirst(
        CyNetwork network,
        CyNode source,
        CyNode target,
        final HashMap<CyNode, Integer> minDists)
            throws Exception
    {
        ArrayList<CyNode> path = new ArrayList<CyNode>();

        // if source==target:
        // return ({source:0}, {source:[source]})
        if (source.equals(target))
            return path;

        // dist = {} | dictionary of final distances
        HashMap<CyNode, Integer> distances = new HashMap<CyNode, Integer>();
        // preds = {source:None} | dictionary of paths
        HashMap<CyNode, CyNode> preds = new HashMap<CyNode, CyNode>();
        // seen = {source:0} | map of seen nodes and their dists
        HashMap<CyNode, Integer> seen = new HashMap<CyNode, Integer>();
        seen.put(source, 0);

        HashMap<CyNode, Integer> sortingDists = new HashMap<CyNode, Integer>();
        sortingDists.put(source, 0);

        // fringe = [] | heap of nodes on the border to process, keyed by
        // heuristic distance
        PriorityQueue<CyNode> fringe =
            new PriorityQueue<CyNode>(0, new Comparator<CyNode>() {
                @Override
                public int compare(CyNode node1, CyNode node2)
                {
                    return Integer.compare(
                        heuristicF(minDists, node1),
                        heuristicF(minDists, node2));
// return Integer.compare(sortingDists.get(node1), sortingDists.get(node2));
                }

            });

        // heapq.heappush(fringe, (heuristicF(source), (source, 0)))
        fringe.add(source);

        // REL_EPS = 1e-10
        final double REL_EPS = 1E-10;

        // while fringe:
        while (fringe.size() > 0)
        {
            // (heurDist, (currNode, actualDist)) = heapq.heappop(fringe)
            CyNode currNode = fringe.poll();
// int actualDist = seen.get(currNode);
            int actualDist = sortingDists.get(currNode);

            // if currNode in dist: continue
            if (distances.containsKey(currNode))
                continue;

            // dist[currNode] = actualDist
            distances.put(currNode, actualDist);

            // if currNode == target: break
            if (currNode.equals(target))
                break;

            // currEdges = iter(net[currNode].items())
            List<CyNode> neighbors =
                network.getNeighborList(currNode, CyEdge.Type.OUTGOING);
            // for nextNode, edgedata in currEdges
            for (CyNode nextNode : neighbors)
            {
                // nextActDist = actualDist + edgedata.get(weight, 1)
                int edgeWeight = 1;
                int nextActDist = actualDist + edgeWeight;

                // nextHeurDist = nextActDist + heuristicF(nextNode)
                int nextHeurDist = nextActDist + heuristicF(minDists, nextNode);

                // if isinf(nextHeurDist): continue
                if (heuristicF(minDists, nextNode) == INFINITY)
                    continue;

                // if nextNode in dist
                if (distances.containsKey(nextNode))
                {
                    // if (nextActDist * (1+REL_EPS)) < dist[nextNode]:
                    if ((nextActDist * (1 + REL_EPS)) < distances.get(nextNode))
                    {
                        // raise ValueError('Contradictory search path:', 'bad
                        // heuristic? negative weights?')
                        throw new Exception(
                            "Contradictory search path: bad heuristic? negative weights?");
                    }
                }
                // elif nextNode not in seen or nextActDist < seen[nextNode]:
                else if (!seen.containsKey(nextNode)
                    || nextActDist < seen.get(nextNode))
                {
                    // TODO might need to sort the stuff by dist + heuristicF
                    // TODO instead of just heuristicF

                    // seen[nextNode] = nextActDist
                    seen.put(nextNode, nextActDist);
                    // TODO sortingDists.put(?, ?);
                    sortingDists.put(nextNode, nextHeurDist);
                    // heapq.heappush(fringe, (nextHeurDist, (nextNode,
                    // nextActDist)))
                    fringe.add(nextNode);
                    // preds[nextNode] = currNode
                    preds.put(nextNode, currNode);
                }
            }
        }

        return path;
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
    public static HashMap<CyNode, Integer> singleSourceDijkstra(
        CyNetwork network,
        CyNode source)
    {
        final int INFINITY = Integer.MAX_VALUE;

        HashMap<CyNode, Integer> distances = new HashMap<CyNode, Integer>();
        HashMap<CyNode, CyNode> previous = new HashMap<CyNode, CyNode>();
        ArrayList<CyNode> pq = new ArrayList<CyNode>();

        // intializes distances
        for (CyNode v : network.getNodeList())
        {
            distances.put(v, INFINITY);
            previous.put(v, null);
        }
        distances.put(source, 0);
        pq.add(source);

        while (!pq.isEmpty())
        {
            CyNode current = pq.remove(0);

            // goes through the neighbors
            for (CyNode neighbor : network
                .getNeighborList(current, CyEdge.Type.OUTGOING))
            {
                int edgeWeight = 1;
                int newCost = distances.get(current) + edgeWeight;

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
    public static ArrayList<CyNode> dijkstra(
        CyNetwork network,
        CyNode source,
        CyNode target)
    {
        final int INFINITY = Integer.MAX_VALUE;

        HashMap<CyNode, Integer> distances = new HashMap<CyNode, Integer>();
        HashMap<CyNode, CyNode> previous = new HashMap<CyNode, CyNode>();
        ArrayList<CyNode> pq = new ArrayList<CyNode>();

        // intializes distances
        for (CyNode v : network.getNodeList())
        {
            distances.put(v, INFINITY);
            previous.put(v, null);
        }
        distances.put(source, 0);
        pq.add(source);

        while (!pq.isEmpty())
        {
            CyNode current = pq.remove(0);

            // short circuit
            if (current.equals(target))
            {
                // return path reconstructed
                break;
            }

            // goes through the neighbors
            for (CyNode neighbor : network
                .getNeighborList(current, CyEdge.Type.OUTGOING))
            {
                int edgeWeight = 1;
                int newCost = distances.get(current) + edgeWeight;

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

        if (distances.get(target) == INFINITY)
        {
            // unreachable node
            return null;
        }

        // return constructed path
        return constructPath(previous, source, target);
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
    public static ArrayList<CyNode> constructPath(
        HashMap<CyNode, CyNode> previous,
        CyNode source,
        CyNode target)
    {
        ArrayList<CyNode> path = new ArrayList<CyNode>();

        // constructs the path
        CyNode iter = target;
        do
        {
            path.add(iter);

            if (!previous.containsKey(iter))
                return null;
        }
        while (!(iter = previous.get(iter)).equals(source));

        path.add(source);

        Collections.reverse(path);

        return path;
    }


    /**
     * Represents a removed edge so it can be restored to a CyNetwork
     *
     * @author Daniel
     * @version Sep 10, 2015
     */
    static class RemovedEdge
    {
        private CyNode source;
        private CyNode target;


        /**
         * Constructor for removed edge
         *
         * @param source
         *            the source of the edge
         * @param target
         *            the target of the edge
         */
        public RemovedEdge(CyNode source, CyNode target)
        {
            this.source = source;
            this.target = target;
        }
    }
}
