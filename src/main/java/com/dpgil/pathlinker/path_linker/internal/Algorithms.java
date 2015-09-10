package com.dpgil.pathlinker.path_linker.internal;

import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
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
    public static ArrayList<ArrayList<CyNode>> ksp(
        CyNetwork network,
        CyNode source,
        CyNode target,
        int maxK)
    {
        // the list of shortest paths
        ArrayList<ArrayList<CyNode>> A = new ArrayList<ArrayList<CyNode>>();

        // A[0] = shortest path
        ArrayList<CyNode> shortestPath = dijkstra(network, source, target);

        // there is no path from source to target
        if (shortestPath == null)
            return A;

        A.add(shortestPath);

        // B = []; the heap
        ArrayList<ArrayList<CyNode>> B = new ArrayList<ArrayList<CyNode>>();

        // for k in range(1, max_k)
        for (int k = 1; k < maxK; k++)
        {
            // for i in range(0, len(A[-1]['path]) - 1
            ArrayList<CyNode> latestPath = A.get(A.size() - 1);
            for (int i = 0; i < latestPath.size() - 1; i++)
            {
                // node_spur = A[-1]['path'][i]
                CyNode nodeSpur = latestPath.get(i);
                // path_root = A[-1]['path'][:i+1]
                List<CyNode> pathRoot = latestPath.subList(0, i + 1);

                // edges_removed = []
                ArrayList<RemovedEdge> edgesRemoved =
                    new ArrayList<RemovedEdge>();
                // for path_k in A:
                for (ArrayList<CyNode> currPath : A)
                {
                    // if len(curr_path) > i and path_root == curr_path[:i+1]:
                    // TODO make sure .equals on CyNodes works
                    if (currPath.size() > i
                        && pathRoot.equals(currPath.subList(0, i + 1)))
                    {
                        // cost = graph.remove_edge(curr_path[i],
                        // curr_path[i+1])
                        CyEdge toRemove = getEdge(
                            network,
                            currPath.get(i),
                            currPath.get(i + 1));

                        // if cost == -1: continue
                        if (toRemove == null)
                            continue;

                        // edges_removed.append([curr_path[i], curr_path[i+1],
                        // cost])
                        network.removeEdges(Arrays.asList(toRemove));
                        edgesRemoved.add(
                            new RemovedEdge(
                                currPath.get(i),
                                currPath.get(i + 1)));
                    }
                }

                // path_spur = dijkstra(graph, node_spur, node_end)
                ArrayList<CyNode> pathSpur =
                    dijkstra(network, nodeSpur, target);

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

                // for edge in edges_removed:
                // graph.add_edge(edge[0], edge[1], edge[2])
                for (RemovedEdge removed : edgesRemoved)
                {
                    network.addEdge(removed.source, removed.target, true);
                }

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

                // A.append(B[0])
                // B.pop(0)
                A.add(B.remove(0));
            }
            else
            {
                break;
            }
        }

        return A;
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
    private static CyEdge getEdge(
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
