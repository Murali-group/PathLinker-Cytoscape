package com.dpgil.pathlinker.path_linker.internal;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Queue;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import javax.swing.JOptionPane;
import java.util.List;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyNetwork;

/**
 * // -------------------------------------------------------------------------
 * /** Algorithms class for the PathLinker plugin. Contains all the algorithms
 * (Floyd-Warshall, Dijkstra's, Yen's KSP) used in PathLinker. TODO there is
 * inconsistency between methods and what writes to tables and what doesn't
 *
 * @author Daniel Gil
 * @version Apr 23, 2015
 */
public class Algorithms
{
    static int pairNum = 0;


    /**
     * Runs the floyd warshall algorithm and writes the results to a table
     *
     * @param network
     *            the network to run the algorithm on
     * @param table
     *            the table to write to
     */
    public static void runFloydWarshall(CyNetwork network, CyTable table)
    {
        // create a column for the sources
        table.createColumn("Source", String.class, false);
        // create a column for the targets
        table.createColumn("Target", String.class, false);
        // create a column for the distances
        table.createColumn("Distance", Integer.class, false);

        int[][] adjacencyMatrix =
            new int[network.getNodeCount()][network.getNodeCount()];

        // set all edges to weight "infinity"
        for (int i = 0; i < adjacencyMatrix.length; i++)
        {
            for (int j = 0; j < adjacencyMatrix.length; j++)
            {
                adjacencyMatrix[i][j] = 1000000;
            }
        }

        // fills in the adjacency matrix
        List<CyNode> nodes = network.getNodeList();
        for (int i = 0; i < nodes.size(); i++)
        {
            CyNode node1 = nodes.get(i);

            for (int j = 0; j < nodes.size(); j++)
            {
                CyNode node2 = nodes.get(j);

                // there is a connection between them
                // TODO adjust for directed graphs
                if (network.getConnectingEdgeList(node1, node2, CyEdge.Type.ANY)
                    .size() > 0)
                {
                    adjacencyMatrix[i][j] = 1;
                }
            }
        }

        // floyd warshall
        for (int i = 0; i < nodes.size(); i++)
        {
            for (int j = 0; j < nodes.size(); j++)
            {
                for (int k = 0; k < nodes.size(); k++)
                {
                    adjacencyMatrix[j][k] = Math.min(
                        adjacencyMatrix[j][k],
                        adjacencyMatrix[j][i] + adjacencyMatrix[i][k]);
                }
            }
        }

        // print the results
        pairNum = 0;
        for (int i = 0; i < nodes.size(); i++)
        {
            CyNode node1 = nodes.get(i);
            String node1Name =
                network.getRow(node1).get(CyNetwork.NAME, String.class);
            for (int j = 0; j < nodes.size(); j++)
            {
                CyNode node2 = nodes.get(j);
                String node2Name =
                    network.getRow(node2).get(CyNetwork.NAME, String.class);
                if (i != j)
                {
                    CyRow row = table.getRow(pairNum++);

                    row.set("Source", node1Name);
                    row.set("Target", node2Name);
                    row.set("Distance", adjacencyMatrix[i][j]);
                }
            }
        }
    }


    /**
     * Runs dijkstra for each source
     *
     * @param network
     *            the network to run the algorithm on
     * @param table
     *            the table to write to
     */
    public static void runDijkstra(CyNetwork network, CyTable table)
    {
        JOptionPane.showMessageDialog(null, "PathLinker is running.");
        // create a column for the sources
        table.createColumn("Source", String.class, false);
        // create a column for the targets
        table.createColumn("Target", String.class, false);
        // create a column for the distances
        table.createColumn("Distance", Integer.class, false);

        List<CyNode> nodes = network.getNodeList();
        pairNum = 0;

        // runs dijkstra for each source
        for (CyNode node : nodes)
        {
            dijkstra(table, network, node);
        }
    }


    /**
     * Runs Yen's KSP algorithm and returns a list of the k shortest paths
     *
     * @param table
     *            the table to write to. TODO make this not require table
     * @param network
     *            the network to run ksp on
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @param K
     *            the number of shortest paths to compute
     * @return a list of paths (each path is a list of cynodes)
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<ArrayList<CyNode>> ksp(
        CyTable table,
        CyNetwork network,
        CyNode source,
        CyNode target,
        int K)
    {
        // list of paths to store all k-shortest paths
        ArrayList<ArrayList<CyNode>> A = new ArrayList<ArrayList<CyNode>>(K);
// JOptionPane.showMessageDialog(null, "line 137");

        // A[0] = dijkstra(source, sink)
        ArrayList<CyNode> ssd = ssd2(table, network, source, target, 0, false);

        // for debugging
        String sp = "Shortest path: ";
        for (CyNode s : ssd)
        {
            sp += network.getRow(s).get(CyNetwork.NAME, String.class) + " ";
        }
// JOptionPane.showMessageDialog(null, sp);

        A.add(ssd);

        // initialize the heap to store the potential shortest paths
        ArrayList<ArrayList<CyNode>> B = new ArrayList<ArrayList<CyNode>>();

        for (int k = 1; k <= K; k++)
        {
            // The spur node ranges from the first node to the next to last
            // node in the previous k-shortest path
            for (int i = 0; i < A.get(k - 1).size() - 1; i++)
            {
                ArrayList<CyNode> removedNodes = new ArrayList<CyNode>();
                ArrayList<RemovedEdge> remEdges = new ArrayList<RemovedEdge>();

                // Spur node is retrieved from the previous k-shortest path,
                // k − 1.
                CyNode spurNode = A.get(k - 1).get(i);

                // for debugging
// String spurNodeName =
// "Spur node (k="
// + (k - 1)
// + ", i="
// + i
// + "): "
// + network.getRow(spurNode).get(
// CyNetwork.NAME,
// String.class);
// JOptionPane.showMessageDialog(null, spurNodeName);

                // The sequence of nodes from the source to the spur node of
// the
// previous k-shortest path.
                ArrayList<CyNode> rootPath = new ArrayList<CyNode>();
                for (int j = 0; j < i; j++)
                {
                    rootPath.add(A.get(k - 1).get(j));
                }

                // for debugging
// String p = "Root path: ";
// for (CyNode rp : rootPath)
// {
// p +=
// network.getRow(rp).get(CyNetwork.NAME, String.class)
// + " ";
// }
// JOptionPane.showMessageDialog(null, p);

                for (ArrayList<CyNode> path : A)
                {
                    // root path is equal to p.nodes(0, i)
                    if (path.size() >= i && rootPath.equals(path.subList(0, i)))
                    {
                        // Remove the links that are part of the previous
                        // shortest paths which share the same root path.
                        if (i + 1 < path.size())
                        {
                            if (network
                                .containsEdge(path.get(i), path.get(i + 1)))
                            {
                                List<CyEdge> connect =
                                    network.getConnectingEdgeList(
                                        path.get(i),
                                        path.get(i + 1),
                                        CyEdge.Type.DIRECTED);

                                // if the sources don't match - remove the edge
                                if (connect.size() > 1)
                                {
                                    if (!connect.get(0).getSource()
                                        .equals(path.get(i)))
                                    {
                                        connect.remove(0);
                                    }
                                    else
                                    {
                                        connect.remove(1);
                                    }
                                }

                                if (connect.size() > 1)
                                {
                                    JOptionPane
                                        .showMessageDialog(null, "Uh oh what");
                                }

                                RemovedEdge remE = new RemovedEdge(
                                    connect.get(0).getSource(),
                                    connect.get(0).getTarget());
                                remEdges.add(remE);

                                network.removeEdges(connect);
                            }
                        }
                    }
                }

                // FOR DEBUGGING CAN REMOVE after tests
                StringBuilder resb = new StringBuilder();
                resb.append("k=").append(k).append(" i=").append(i).append("\n")
                    .append("Removed edges:\n");
                for (RemovedEdge re : remEdges)
                {

                    String n1n = network.getRow(re.node1)
                        .get(CyNetwork.NAME, String.class);
                    String n2n = network.getRow(re.node2)
                        .get(CyNetwork.NAME, String.class);

                    resb.append(n1n).append("->").append(n2n).append("\n");
                }
// JOptionPane.showMessageDialog(null, resb.toString());
                resb.setLength(0);

// JOptionPane.showMessageDialog(null, "line 237");

                // removes all nodes in rootPath except spurNode from graph
                ArrayList<CyNode> rootPathWithoutSpurNode =
                    new ArrayList<CyNode>();
                // copies the root path
                // TODO source does not fit in dest
// Collections.copy(rootPathWithoutSpurNode, rootPath);
                for (int j = 0; j < rootPath.size(); j++)
                {
                    rootPathWithoutSpurNode.add(rootPath.get(j));
                }
                // removes spur node
                rootPathWithoutSpurNode.remove(spurNode);
                // removes all nodes in rootPath except spurNode from graph
                // TODO may need to actually remove
// network.removeNodes(rootPathWithoutSpurNode);
                removedNodes.addAll(rootPathWithoutSpurNode);

                // Calculate the spur path from the spur node to the sink
                // TODO use A* instead
                ArrayList<CyNode> spurPath = ssd2WithRemovals(
                    table,
                    network,
                    spurNode,
                    target,
                    removedNodes,
                    0,
                    false);

                if (spurPath.size() > 0)
                {
                    // Entire path is made up of the root path and spur path
                    ArrayList<CyNode> totalPath = new ArrayList<CyNode>();
                    totalPath.addAll(rootPath);
                    totalPath.addAll(spurPath);
                    // Add the potential k-shortest path to the heap
                    B.add(totalPath);
                }

                // Add back the edges that were removed
                resb.append("Restored edges:\n");
                for (RemovedEdge removedEdge : remEdges)
                {

                    network.addEdge(removedEdge.node1, removedEdge.node2, true);

                    String n1n = network.getRow(removedEdge.node1)
                        .get(CyNetwork.NAME, String.class);
                    String n2n = network.getRow(removedEdge.node2)
                        .get(CyNetwork.NAME, String.class);
                    resb.append("Restoring ").append(n1n).append("->")
                        .append(n2n).append("\n");

                }
// JOptionPane.showMessageDialog(null, resb.toString());

            }

            if (B.isEmpty())
            {
                // This handles the case of there being no spur paths, or no
                // spur paths left. This could happen if the spur paths have
                // already been exhausted (added to A), or there are no spur
                // paths at all - such as when both the source and sink vertices
                // lie along a "dead end".
                break;
            }

            // Sort the potential k-shortest paths by cost.
            Collections.sort(B, new Comparator() {
                @Override
                public int compare(Object t1, Object t2)
                {
                    if (t1 == null || t2 == null)
                    {
                        return 0;
                    }

                    if (t1 == t2)
                    {
                        return 0;
                    }

                    ArrayList<CyNode> a1 = (ArrayList<CyNode>)t1;
                    ArrayList<CyNode> a2 = (ArrayList<CyNode>)t2;

                    return Integer.compare(a1.size(), a2.size());
                }
            });
// JOptionPane.showMessageDialog(null, "line 280");

            // Add the lowest cost path becomes the k-shortest path.
            // TODO may not be efficient
            A.add(B.remove(0));

            String sourceName =
                network.getRow(source).get(CyNetwork.NAME, String.class);
            String targetName =
                network.getRow(target).get(CyNetwork.NAME, String.class);

// String message =
// k + "-th shortest path from " + sourceName + " to "
// + targetName + ": ";
// for (CyNode node : A.get(k))
// {
// String nodeName =
// network.getRow(node).get(CyNetwork.NAME, String.class);
// message += nodeName + " ";
// }
// JOptionPane.showMessageDialog(null, message);
        }

        // sorts a's paths by cost
        // Sort the potential k-shortest paths by cost.
        Collections.sort(A, new Comparator() {
            @Override
            public int compare(Object t1, Object t2)
            {
                if (t1 == null || t2 == null)
                {
                    return 0;
                }

                if (t1 == t2)
                {
                    return 0;
                }

                ArrayList<CyNode> a1 = (ArrayList<CyNode>)t1;
                ArrayList<CyNode> a2 = (ArrayList<CyNode>)t2;

                if (a1.size() == a2.size())
                {
                    return 0;
                }

// return a1.size() < a2.size() ? 1 : -1;
// original one below
                return a1.size() < a2.size() ? -1 : 1;
            }
        });

        return A;
    }


    static class RemovedEdge
    {
        private CyNode node1;
        private CyNode node2;


        public RemovedEdge(CyNode n1, CyNode n2)
        {
            node1 = n1;
            node2 = n2;
        }
    }


    /**
     * Performs single source dijkstra's ignoring all nodes that were removed in
     * Yen's KSP
     *
     * @param table
     *            the table to write to
     * @param network
     *            the cy network
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @param removedNodes
     *            the nodes that were removed from the network
     * @param pair
     *            the pair number (k)
     * @param print
     *            whether or not to print to the table
     * @return ArrayList<CyNode> the path from source to target
     */
    public static ArrayList<CyNode> ssd2WithRemovals(
        CyTable table,
        CyNetwork network,
        CyNode source,
        CyNode target,
        ArrayList<CyNode> removedNodes,
        int pair,
        boolean print)
    {
        // distance from the source to the specified CyNode
        HashMap<CyNode, Integer> dist = new HashMap<CyNode, Integer>();
        HashMap<CyNode, CyNode> pred = new HashMap<CyNode, CyNode>();
        List<CyNode> nodes = network.getNodeList();

        final int INFINITY = 1000000;
        // give the the distance to every node "infinite" weight
        for (CyNode node : nodes)
        {
            dist.put(node, INFINITY);
        }
        // distance from the source to itself is 0
        dist.put(source, 0);

        ArrayList<CyNode> pq = new ArrayList<CyNode>();
        pq.add(source);

        while (!pq.isEmpty())
        {
            CyNode current = pq.remove(0);

            // short circuit single source single target
            if (current.equals(target))
            {
                // don't need path to self or unreachable node
                if (dist.get(target) == 0 || dist.get(target) == INFINITY)
                {
                    return new ArrayList<CyNode>();
                }

                // print all the pairs and their distances
                String sourceName =
                    network.getRow(source).get(CyNetwork.NAME, String.class);
                String targetName =
                    network.getRow(target).get(CyNetwork.NAME, String.class);

                // constructs the path traveled
                CyNode iter = target;
                ArrayList<String> nodeNamePath = new ArrayList<String>();
                ArrayList<CyNode> nodePath = new ArrayList<CyNode>();
                do
                {
                    String srcName =
                        network.getRow(iter).get(CyNetwork.NAME, String.class);
                    nodeNamePath.add(srcName);
                    // TODO might be a huge problem with assigning references
                    nodePath.add(iter);
                }
                while (!(iter = pred.get(iter)).equals(source));
                nodeNamePath.add(sourceName);
                nodePath.add(source);

                Collections.reverse(nodeNamePath);
                Collections.reverse(nodePath);

                if (print)
                {
                    StringBuilder pathBuilder = new StringBuilder();
                    for (String nodeName : nodeNamePath)
                    {
                        pathBuilder.append(nodeName).append(" ");
                    }

                    CyRow row = table.getRow(pair);

                    row.set("Source", sourceName);
                    row.set("Target", targetName);
                    row.set("Distance", dist.get(target));
                    row.set("Path", pathBuilder.toString());
                }

                return nodePath;
            }

            // iterate over node's neighbors
            List<CyNode> neighbors =
                network.getNeighborList(current, CyEdge.Type.OUTGOING);
// neighbors.removeAll(removedNodes);

            for (CyNode neighbor : neighbors)
            {
                // simulates removed nodes TODO
                if (removedNodes.contains(neighbor))
                    continue;
                // TODO the 1 could be replaced with the weight of edge from
                // current->neighbor
                int distanceThroughU = dist.get(current) + 1;

                if (distanceThroughU < dist.get(neighbor))
                {
                    pq.remove(neighbor);
                    dist.put(neighbor, distanceThroughU);

                    // priority queue add
                    for (int i = 0; i < pq.size(); i++)
                    {
                        if (dist.get(neighbor) < dist.get(pq.get(i)))
                        {
                            pq.add(i, neighbor);
                            break;
                        }
                    }
                    if (!pq.contains(neighbor))
                        pq.add(neighbor);
                    // queue.add(neighbor);
                    pred.put(neighbor, current);
                }
            }
        }

        // don't need path to self or unreachable node
        if (dist.get(target) == 0 || dist.get(target) == INFINITY)
        {
            return new ArrayList<CyNode>();
        }

        // print all the pairs and their distances
        String sourceName =
            network.getRow(source).get(CyNetwork.NAME, String.class);
        String targetName =
            network.getRow(target).get(CyNetwork.NAME, String.class);

        // constructs the path traveled
        CyNode iter = target;
        ArrayList<String> nodeNamePath = new ArrayList<String>();
        ArrayList<CyNode> nodePath = new ArrayList<CyNode>();
        do
        {
            String srcName =
                network.getRow(iter).get(CyNetwork.NAME, String.class);
            nodeNamePath.add(srcName);
            // TODO might be a huge problem with assigning references
            nodePath.add(iter);
        }
        while (!(iter = pred.get(iter)).equals(source));
        nodeNamePath.add(sourceName);
        nodePath.add(source);

        Collections.reverse(nodeNamePath);
        Collections.reverse(nodePath);

        if (print)
        {
            StringBuilder pathBuilder = new StringBuilder();
            for (String nodeName : nodeNamePath)
            {
                pathBuilder.append(nodeName).append(" ");
            }

            CyRow row = table.getRow(pair);

            row.set("Source", sourceName);
            row.set("Target", targetName);
            row.set("Distance", dist.get(target));
            row.set("Path", pathBuilder.toString());
        }

        return nodePath;
    }


    public static ArrayList<CyNode> ssd2(
        CyTable table,
        CyNetwork network,
        CyNode source,
        CyNode target,
        int pair,
        boolean print)
    {
        // distance from the source to the specified CyNode
        HashMap<CyNode, Integer> dist = new HashMap<CyNode, Integer>();
        HashMap<CyNode, CyNode> pred = new HashMap<CyNode, CyNode>();
        List<CyNode> nodes = network.getNodeList();

        final int INFINITY = 1000000;
        // give the the distance to every node "infinite" weight
        for (CyNode node : nodes)
        {
            dist.put(node, INFINITY);
        }
        // distance from the source to itself is 0
        dist.put(source, 0);

        ArrayList<CyNode> pq = new ArrayList<CyNode>();
        pq.add(source);

        while (!pq.isEmpty())
        {
            CyNode current = pq.remove(0);

            // short circuit single source single target
            if (current.equals(target))
            {
                // don't need path to self or unreachable node
                if (dist.get(target) == 0 || dist.get(target) == INFINITY)
                {
                    return new ArrayList<CyNode>();
                }

                // print all the pairs and their distances
                String sourceName =
                    network.getRow(source).get(CyNetwork.NAME, String.class);
                String targetName =
                    network.getRow(target).get(CyNetwork.NAME, String.class);

                // constructs the path traveled
                CyNode iter = target;
                ArrayList<String> nodeNamePath = new ArrayList<String>();
                ArrayList<CyNode> nodePath = new ArrayList<CyNode>();
                do
                {
                    String srcName =
                        network.getRow(iter).get(CyNetwork.NAME, String.class);
                    nodeNamePath.add(srcName);
                    // TODO might be a huge problem with assigning references
                    nodePath.add(iter);
                }
                while (!(iter = pred.get(iter)).equals(source));
                nodeNamePath.add(sourceName);
                nodePath.add(source);

                Collections.reverse(nodeNamePath);
                Collections.reverse(nodePath);

                if (print)
                {
                    StringBuilder pathBuilder = new StringBuilder();
                    for (String nodeName : nodeNamePath)
                    {
                        pathBuilder.append(nodeName).append(" ");
                    }

                    CyRow row = table.getRow(pair);

                    row.set("Source", sourceName);
                    row.set("Target", targetName);
                    row.set("Distance", dist.get(target));
                    row.set("Path", pathBuilder.toString());
                }

                return nodePath;
            }

            // iterate over node's neighbors
            List<CyNode> neighbors =
                network.getNeighborList(current, CyEdge.Type.OUTGOING);

            for (CyNode neighbor : neighbors)
            {
                // TODO the 1 could be replaced with the weight of edge from
                // current->neighbor
                int distanceThroughU = dist.get(current) + 1;

                if (distanceThroughU < dist.get(neighbor))
                {
                    pq.remove(neighbor);
                    dist.put(neighbor, distanceThroughU);

                    // priority queue add
                    for (int i = 0; i < pq.size(); i++)
                    {
                        if (dist.get(neighbor) < dist.get(pq.get(i)))
                        {
                            pq.add(i, neighbor);
                            break;
                        }
                    }
                    if (!pq.contains(neighbor))
                        pq.add(neighbor);
                    // queue.add(neighbor);
                    pred.put(neighbor, current);
                }
            }
        }

        // don't need path to self or unreachable node
        if (dist.get(target) == 0 || dist.get(target) == INFINITY)
        {
            return new ArrayList<CyNode>();
        }

        // print all the pairs and their distances
        String sourceName =
            network.getRow(source).get(CyNetwork.NAME, String.class);
        String targetName =
            network.getRow(target).get(CyNetwork.NAME, String.class);

        // constructs the path traveled
        CyNode iter = target;
        ArrayList<String> nodeNamePath = new ArrayList<String>();
        ArrayList<CyNode> nodePath = new ArrayList<CyNode>();
        do
        {
            String srcName =
                network.getRow(iter).get(CyNetwork.NAME, String.class);
            nodeNamePath.add(srcName);
            // TODO might be a huge problem with assigning references
            nodePath.add(iter);
        }
        while (!(iter = pred.get(iter)).equals(source));
        nodeNamePath.add(sourceName);
        nodePath.add(source);

        Collections.reverse(nodeNamePath);
        Collections.reverse(nodePath);

        if (print)
        {
            StringBuilder pathBuilder = new StringBuilder();
            for (String nodeName : nodeNamePath)
            {
                pathBuilder.append(nodeName).append(" ");
            }

            CyRow row = table.getRow(pair);

            row.set("Source", sourceName);
            row.set("Target", targetName);
            row.set("Distance", dist.get(target));
            row.set("Path", pathBuilder.toString());
        }

        return nodePath;
    }


    public static void dijkstra(CyTable table, CyNetwork network, CyNode source)
    {
        // distance from the source to the specified CyNode
        HashMap<CyNode, Integer> dist = new HashMap<CyNode, Integer>();
        List<CyNode> nodes = network.getNodeList();

        final int INFINITY = 1000000;
        // give the the distance to every node "infinite" weight
        for (CyNode node : nodes)
        {
            dist.put(node, INFINITY);
        }
        // distance from the source to itself is 0
        dist.put(source, 0);

        Queue<CyNode> queue = new ArrayDeque<CyNode>();
        queue.add(source);

        while (!queue.isEmpty())
        {
            CyNode current = queue.poll();

            // TODO add polling for shortest dist cynode
            // TODO change for directed edges
            // iterate over node's neighbors
            List<CyNode> neighbors =
                network.getNeighborList(current, CyEdge.Type.OUTGOING);
            for (CyNode neighbor : neighbors)
            {
                int distanceThroughU = dist.get(current) + 1;

                if (distanceThroughU < dist.get(neighbor))
                {
                    queue.remove(neighbor);
                    dist.put(neighbor, distanceThroughU);
                    queue.add(neighbor);
                }
            }
        }

        // print all the pairs and their distances
        String sourceName =
            network.getRow(source).get(CyNetwork.NAME, String.class);
        for (CyNode node : nodes)
        {
            String nodeName =
                network.getRow(node).get(CyNetwork.NAME, String.class);

            if (!node.equals(source))
            {
                CyRow row = table.getRow(pairNum++);

                row.set("Source", sourceName);
                row.set("Target", nodeName);
                row.set("Distance", dist.get(node));
            }
        }
    }


    public static void prepareNetForKSP(
        CyNetwork network,
        ArrayList<CyNode> sources,
        ArrayList<CyNode> targets,
        ArrayList<Integer> nodeWeights)
    {
        // adds sources and targets to the list
        // TODO get a better way to distinguish between them
        for (CyNode node : network.getNodeList())
        {
            String name =
                network.getRow(node).get(CyNetwork.NAME, String.class);
            if (name.charAt(0) == 'S')
            {
                sources.add(node);
            }
            if (name.charAt(0) == 'T')
            {
                targets.add(node);
            }
        }

        // removes all edges entering a source
        for (CyNode source : sources)
        {
            network.removeEdges(
                network.getAdjacentEdgeList(source, CyEdge.Type.INCOMING));
        }
        // removes all edges leaving a target
        for (CyNode target : targets)
        {
            network.removeEdges(
                network.getAdjacentEdgeList(target, CyEdge.Type.OUTGOING));
        }

        int[][] currWeight =
            new int[network.getNodeCount()][network.getNodeCount()];
        // assign EdgeFlux scores to the edges
        // currWeight[(u,v)] = nodeWeights[u] *
// net[u][v]['weight']/net.out_degree(u, 'weight')
        for (CyEdge edge : network.getEdgeList())
        {
            CyNode s = edge.getSource();
        }
    }


    private void KShortestPathsYen(
        CyNetwork network,
        CyNode source,
        CyNode target,
        int k)
    {
        Path[] A = new Path[k];
    }


    private class Path
    {
        private ArrayList<CyNode> path;


        public Path()
        {
            path = new ArrayList<CyNode>();
        }


        // add a node to the path
        public void append(CyNode node)
        {
            path.add(node);
        }


        // access the i-th node in a path
        public CyNode node(int i)
        {
            return path.get(i);
        }


        public ArrayList<CyNode> getPath()
        {
            return path;
        }
    }
}
