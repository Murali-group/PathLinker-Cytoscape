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
    private static HashMap<String, Long> timeSpent = new HashMap<String, Long>();

    /**
     * Reverses all the edges in a CyNetwork by removing all and then readding
     * all of them in the opposite direction
     *
     * @param network
     *            the supplied network
     */
    public static void reverseNetwork(CyNetwork network)
    {
        long startTime = System.currentTimeMillis();

        List<CyEdge> edges = network.getEdgeList();
        network.removeEdges(edges);
        for (CyEdge edge : edges)
            network.addEdge(edge.getTarget(), edge.getSource(), true);

        long endTime = System.currentTimeMillis();
        long difference = endTime - startTime;
        timeSpent.put("reverseNetwork", (timeSpent.containsKey("reverseNetwork") ? timeSpent.get("reverseNetwork") : 0) + difference);
    }


    public static int heuristicF(HashMap<CyNode, Integer> minDists, CyNode node)
    {
        long startTime = System.currentTimeMillis();

        if (minDists.containsKey(node))
            return minDists.get(node);
        else
            return INFINITY;
    }

static HashSet<CyEdge> pathHiddenEdges;
static int aStarContinues = 0;

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
timeSpent.clear();
aStarContinues = 0;

long startSection1 = System.currentTimeMillis();

        // the list of shortest paths
        ArrayList<ArrayList<CyNode>> A = new ArrayList<ArrayList<CyNode>>();

long startDijkstras = System.currentTimeMillis();

        // network.reverse();
        //reverseNetwork(network);
        // minDists = single_source_dijkstra(network, target);
        HashMap<CyNode, Integer> minDists =
            reverseSingleSourceDijkstra(network, target);
        // network.reverse();
        //reverseNetwork(network);

        // A[0] = shortest path
        ArrayList<CyNode> shortestPath = dijkstra(network, source, target);

long endDijkstras = System.currentTimeMillis();
long differenceDijkstras = endDijkstras - startDijkstras;
timeSpent.put("bothDijsktras", (timeSpent.containsKey("bothDijsktras") ? timeSpent.get("bothDijsktras") : 0) + differenceDijkstras);


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

long endSection1 = System.currentTimeMillis();
long differenceSection1 = endSection1 - startSection1;
timeSpent.put("kspSection1", (timeSpent.containsKey("kspSection1") ? timeSpent.get("kspSection1") : 0) + differenceSection1);

        // for k in range(1, max_k)
        for (int k = 1; k < maxK; k++)
        {
            // edges_removed = []
            pathHiddenEdges = new HashSet<CyEdge>();

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
                    pathHiddenEdges.add(inEdge);
                }

long startSection3 = System.currentTimeMillis();

long startRepNode = System.currentTimeMillis();

//                # for each previously-found shortest path P_j with the same first i nodes as
//                # the first i nodes of prevPath, hide the edge from x to the i+1 node in P_j
//                # to ensure we don't re-find a previously found path.
//                # Lookup the prefixes in a cache to disallow them. Requires more memory
//                # to store the cache, but saves scanning the list of found paths,
//                # which otherwise dominates runtime
                for (CyNode repNode : prefixCache.get(latestPath.subList(0, i+1)))
                {
                    CyEdge repEdge = getEdge(network, nodeSpur, repNode);

                    if (repEdge != null)
                    {
                        pathHiddenEdges.add(repEdge);
                    }
                }

long endRepNode = System.currentTimeMillis();
long differenceRepNode = endRepNode - startRepNode;
timeSpent.put("repNode", (timeSpent.containsKey("repNode") ? timeSpent.get("repNode") : 0) + differenceRepNode);

long startAStar = System.currentTimeMillis();

                // path_spur = a_star(graph, node_spur, node_end)
                ArrayList<CyNode> pathSpur = shortestPathAStar(
                    network,
                    nodeSpur,
                    target,
                    minDists);
//                ArrayList<CyNode> pathSpur = dijkstraHiddenEdges(network, nodeSpur, target);

long endAStar = System.currentTimeMillis();
long differenceAStar = endAStar - startAStar;
timeSpent.put("AStar", (timeSpent.containsKey("AStar") ? timeSpent.get("AStar") : 0) + differenceAStar);

long startPathSpur = System.currentTimeMillis();

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

long endPathSpur = System.currentTimeMillis();
long differencePathSpur = endPathSpur - startPathSpur;
timeSpent.put("pathSpur", (timeSpent.containsKey("pathSpur") ? timeSpent.get("pathSpur") : 0) + differencePathSpur);


long endSection3 = System.currentTimeMillis();
long differenceSection3 = endSection3 - startSection3;
timeSpent.put("kspSection3", (timeSpent.containsKey("kspSection3") ? timeSpent.get("kspSection3") : 0) + differenceSection3);

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

StringBuilder profileResult = new StringBuilder();
for (String key : timeSpent.keySet())
{
    profileResult.append(key).append(": ").append(timeSpent.get(key)).append("\n");
}
profileResult.append("AStarContinues: ").append(aStarContinues).append("\n");
JOptionPane.showMessageDialog(null, profileResult.toString());

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
        final HashMap<CyNode, Integer> minDists)
    {
        ArrayList<CyNode> path = new ArrayList<CyNode>();

        // if source==target:
        // return ({source:0}, {source:[source]})
        if (source.equals(target))
        {
            return path;
        }

        // dist = {} | dictionary of final distances
        HashMap<CyNode, Integer> distances = new HashMap<CyNode, Integer>();
        // preds = {source:None} | dictionary of paths
        HashMap<CyNode, CyNode> preds = new HashMap<CyNode, CyNode>();
        // seen = {source:0} | map of seen nodes and their dists
        HashMap<CyNode, Integer> seen = new HashMap<CyNode, Integer>();
        seen.put(source, 0);

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
            //List<CyNode> neighbors =
                //network.getNeighborList(currNode, CyEdge.Type.OUTGOING);
            List<CyEdge> neighbors = network.getAdjacentEdgeList(currNode, CyEdge.Type.OUTGOING);

            // for nextNode, edgedata in currEdges
            for (CyEdge nextEdge : neighbors)
            {
                if (pathHiddenEdges.contains(nextEdge))
                {
                    aStarContinues++;
                    continue;
                }

                CyNode nextNode = nextEdge.getTarget();

                // nextActDist = actualDist + edgedata.get(weight, 1)
                int edgeWeight = 1;
                int nextActDist = currData.actDist + edgeWeight;

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
    public static HashMap<CyNode, Integer> reverseSingleSourceDijkstra(
        CyNetwork network,
        CyNode source)
    {
        final HashMap<CyNode, Integer> distances = new HashMap<CyNode, Integer>();
        HashMap<CyNode, CyNode> previous = new HashMap<CyNode, CyNode>();
        PriorityQueue<CyNode> pq = new PriorityQueue<CyNode>(10, new Comparator<CyNode>()
            {
                @Override
                public int compare(CyNode o1, CyNode o2)
                {
                    return Integer.compare(distances.get(o1), distances.get(o2));
                }
            });

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
            CyNode current = pq.poll();

            // goes through incoming neighbors because we are finding the paths that lead to the target
            // however, we don't want to reverse the network and call a normal SSD because
            // reversing the network dominates runtime in Cytoscape
            for (CyNode neighbor : network
                .getNeighborList(current, CyEdge.Type.INCOMING))
            {
                int edgeWeight = 1;
                int newCost = distances.get(current) + edgeWeight;

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
    public static HashMap<CyNode, Integer> singleSourceDijkstra(
        CyNetwork network,
        CyNode source)
    {
long startTime = System.currentTimeMillis();

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

long endTime = System.currentTimeMillis();
long difference = endTime - startTime;
timeSpent.put("singleSourceDijkstra", (timeSpent.containsKey("singleSourceDijkstra") ? timeSpent.get("singleSourceDijkstra") : 0) + difference);

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
long startTime = System.currentTimeMillis();

        final HashMap<CyNode, Integer> distances = new HashMap<CyNode, Integer>();
        HashMap<CyNode, CyNode> previous = new HashMap<CyNode, CyNode>();
//        ArrayList<CyNode> pq = new ArrayList<CyNode>();
        PriorityQueue<CyNode> pq = new PriorityQueue<CyNode>(10, new Comparator<CyNode>()
            {
                @Override
                public int compare(CyNode o1, CyNode o2)
                {
                    return Integer.compare(distances.get(o1), distances.get(o2));
                }
            });

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
//            CyNode current = pq.remove(0);
            CyNode current = pq.poll();

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
//                    for (int i = 0; i < pq.size(); i++)
//                    {
//                        if (distances.get(neighbor) < distances.get(pq.get(i)))
//                        {
//                            pq.add(i, neighbor);
//                            break;
//                        }
//                    }
//                    if (!pq.contains(neighbor))
//                        pq.add(neighbor);
                    pq.add(neighbor);
                }
            }
        }

        if (distances.get(target) == INFINITY)
        {
            // unreachable node
            return null;
        }

long endTime = System.currentTimeMillis();
long difference = endTime - startTime;
timeSpent.put("dijkstra", (timeSpent.containsKey("dijkstra") ? timeSpent.get("dijkstra") : 0) + difference);


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
