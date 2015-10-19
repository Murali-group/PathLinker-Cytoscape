package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;

public class PromptCytoPanel
    extends JPanel
    implements CytoPanelComponent
{
    private CyNetwork               network;
    private CyTable                 table;
    private HashMap<String, CyNode> idToCyNode;

    private JLabel sourcesLabel;
    private JLabel targetsLabel;
    private JLabel kLabel;

    private JTextField sourcesTextField;
    private JTextField targetsTextField;
    private JTextField kTextField;

    private JButton submitButton;

    private ButtonGroup  group;
    private JRadioButton weightedOption;
    private JRadioButton unweightedOption;

    private CyApplicationManager applicationManager;
    private CyTableFactory       tableFactory;
    private CyTableManager       tableManager;

    private CyNetworkFactory     networkFactory;
    private CyNetworkManager     networkManager;
    private CyNetworkViewFactory networkViewFactory;
    private CyNetworkViewManager networkViewManager;


    public PromptCytoPanel(CyNetwork network, CyTable table)
    {
        this.network = network;
        this.table = table;
        initializePanelItems();
        this.setVisible(false);
    }


    public PromptCytoPanel(
        CyApplicationManager applicationManager,
        CyTableFactory tableFactory,
        CyTableManager tableManager)
    {
        this.applicationManager = applicationManager;
        this.tableFactory = tableFactory;
        this.tableManager = tableManager;
        initializePanelItems();
        this.setVisible(false);
    }


    public void initialize(
        CyNetworkFactory networkFactory,
        CyNetworkManager networkManager,
        CyNetworkViewFactory networkViewFactory,
        CyNetworkViewManager networkViewManager)
    {
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
    }


    class SubmitButtonListener
        implements ActionListener
    {
        /**
         * Responds to a click of the submit button in the pathlinker jpanel.
         * Takes in the values in the text boxes and makes the corresponding
         * calls to the algorithms class
         */
        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean success = prepareForKSP();
            if (success)
                runKSP();
        }
    }


    /**
     * Generates a subgraph of the user supplied graph that contains only the
     * nodes and edges that are in the k shortest paths
     */
    private void createKSPSubgraph(ArrayList<Path> paths)
    {
        // TODO create another panel that takes in paths
        CyNetwork kspSubgraph = networkFactory.createNetwork();

        HashSet<String> edgesAdded = new HashSet<String>();
        HashSet<String> nodesAdded = new HashSet<String>();

        HashMap<String, CyNode> subIdToCyNode = new HashMap<String, CyNode>();

        for (Path currPath : paths)
        {
            // excluding supersource and supertarget
            for (int i = 1; i < currPath.size() - 2; i++)
            {
                CyNode node1 = currPath.get(i);
                CyNode node2 = currPath.get(i + 1);

                String node1Name =
                    network.getRow(node1).get(CyNetwork.NAME, String.class);
                String node2Name =
                    network.getRow(node2).get(CyNetwork.NAME, String.class);

                String edgeKey = node1Name + "|" + node2Name;

                if (!nodesAdded.contains(node1Name))
                {
                    CyNode added = kspSubgraph.addNode();
                    kspSubgraph.getRow(added).set(CyNetwork.NAME, node1Name);
                    network.getRow(node1).set(CyNetwork.SELECTED, true);
                    nodesAdded.add(node1Name);
                    subIdToCyNode.put(node1Name, added);
                }

                if (!nodesAdded.contains(node2Name))
                {
                    CyNode added = kspSubgraph.addNode();
                    kspSubgraph.getRow(added).set(CyNetwork.NAME, node2Name);
                    network.getRow(node2).set(CyNetwork.SELECTED, true);
                    nodesAdded.add(node2Name);
                    subIdToCyNode.put(node2Name, added);
                }

                if (!edgesAdded.contains(edgeKey))
                {
                    CyNode a = subIdToCyNode.get(node1Name);
                    CyNode b = subIdToCyNode.get(node2Name);
                    kspSubgraph.addEdge(a, b, true);

                    CyEdge select = Algorithms.getEdge(network, node1, node2);
                    network.getRow(select).set(CyNetwork.SELECTED, true);
                    edgesAdded.add(edgeKey);
                }
            }
        }

// networkManager.addNetwork(kspSubgraph);
// CyNetworkView kspSubgraphView =
// networkViewFactory.createNetworkView(kspSubgraph);
// networkViewManager.addNetworkView(kspSubgraphView);
    }


    private boolean prepareForKSP()
    {

        this.network = applicationManager.getCurrentNetwork();

        idToCyNode = new HashMap<String, CyNode>();
        boolean success = populateIdToCyNode();
        if (!success)
        {
            JOptionPane.showMessageDialog(
                null,
                "No current network. PathLinker cannot run without a network. Exiting...");
            return false;
        }

        this.table = tableFactory
            .createTable("PathLinker ", "#", Integer.class, true, true);
        // sets up the table
        table.createColumn("k", Integer.class, false);
        table.createColumn("Length", Double.class, false);
        table.createColumn("Path", String.class, false);
        // adds the table to cytoscape
        applicationManager.setCurrentTable(table);
        tableManager.addTable(table);

        return true;
    }


    private void runKSP()
    {
        // grabs the values in the source and target text fields
        String sourcesTextFieldValue = sourcesTextField.getText();
        String targetsTextFieldValue = targetsTextField.getText();

        // splits them by spaces
        String[] sourceNames = sourcesTextFieldValue.split(" ");
        String[] targetNames = targetsTextFieldValue.split(" ");

        // stores the sources/targets that were inputted but are not actually in
        // the network, may have been mistyped. Warns the user about them
        ArrayList<String> sourcesNotInNet = new ArrayList<String>();
        ArrayList<String> targetsNotInNet = new ArrayList<String>();

        // checks for mistyped source/target names
        for (String sourceName : sourceNames)
        {
            if (!idToCyNode.containsKey(sourceName))
            {
                sourcesNotInNet.add(sourceName);
            }
        }
        for (String targetName : targetNames)
        {
            if (!idToCyNode.containsKey(targetName))
            {
                targetsNotInNet.add(targetName);
            }
        }

        StringBuilder errorMessage = new StringBuilder();
        if (sourcesNotInNet.size() + targetsNotInNet.size() > 0)
        {
            if (sourcesNotInNet.size() > 0)
            {
                errorMessage.append(
                    "The sources " + sourcesNotInNet.toString()
                        + " are not in the network.\n");
            }
            if (targetsNotInNet.size() > 0)
            {
                errorMessage.append(
                    "The targets " + targetsNotInNet.toString()
                        + " are not in the network.\n");
            }
        }

        int k;
        String kInput = kTextField.getText();
        try
        {
            k = Integer.parseInt(kInput);
        }
        catch (NumberFormatException exception)
        {
            errorMessage.append(
                "Invalid number " + kInput
                    + " entered for k. Using default k=5.\n");
            k = 5;
        }

        // there is some error, tell the user
        if (errorMessage.length() > 0)
        {
            errorMessage.append("Continue anyway?");
            int choice =
                JOptionPane.showConfirmDialog(null, errorMessage.toString());
            if (choice != 0)
            {
                // quit if they say no or cancel
                return;
            }
        }

        ArrayList<CyNode> sources = stringsToNodes(sourceNames);
        ArrayList<CyNode> targets = stringsToNodes(targetNames);

        // TODO say something if there are no sources or no targets
        boolean weighted = false;

        // error checking on the weighted option
        if (weightedOption.isSelected())
        {
            weighted = true;

            if (network.getEdgeCount() == 0)
            {
                // TODO warn the user
                JOptionPane.showMessageDialog(
                    null,
                    "There are no edges in the graph. How do you expect the k-shortest paths to be found?!?!");
                return;
            }
            else
            {
                CyEdge representativeEdge = network.getEdgeList().get(0);
                if (network.getRow(representativeEdge)
                    .get("edge_weight", Double.class) == null)
                {
                    // weighted option selected but no weights in the graph
                    JOptionPane.showMessageDialog(
                        null,
                        "Weighted option selected but no values found for edge attribute edge_weight");
                    return;
                }
            }
        }

        HashSet<CyEdge> hiddenEdges = new HashSet<CyEdge>();

        // hides all incoming edges to source nodes
        for (CyNode source : sources)
        {
            hiddenEdges.addAll(
                network.getAdjacentEdgeList(source, CyEdge.Type.INCOMING));
        }
        // hides all outgoing edges from target nodes
        for (CyNode target : targets)
        {
            hiddenEdges.addAll(
                network.getAdjacentEdgeList(target, CyEdge.Type.OUTGOING));
        }

        // sets up the super source/super target
        CyNode superSource = network.addNode();
        CyNode superTarget = network.addNode();
        ArrayList<CyEdge> superEdges = new ArrayList<CyEdge>();

        // attaches super source to all sources
        for (CyNode source : sources)
        {
            CyEdge superEdge = network.addEdge(superSource, source, true);
            network.getRow(superEdge).set("edge_weight", 1.); // TODO EXPLAIN
            superEdges.add(superEdge);
        }
        // attaches all targets to super target
        for (CyNode target : targets)
        {
            CyEdge superEdge = network.addEdge(target, superTarget, true);
            network.getRow(superEdge).set("edge_weight", 1.); // TODO EXPLAIN
            superEdges.add(superEdge);
        }

        long startTime = System.currentTimeMillis();

        // runs the ksp
        Algorithms.setWeighted(weighted);
        Algorithms.initializeHiddenEdges(hiddenEdges);
        ArrayList<Path> paths =
            Algorithms.ksp(network, superSource, superTarget, k);

        long endTime = System.currentTimeMillis();

        // removes supernodes and their edges
        network.removeEdges(superEdges);
        network.removeNodes(Arrays.asList(superSource, superTarget));

        // updates the results
        writeResults(paths);

        // generates subgraphs
// createKSPSubgraph(paths);

        // notifies user of time taken
        long totalTimeMs = endTime - startTime;
        String timeMessage =
            "PathLinker took " + totalTimeMs + " ms to execute";
        JOptionPane.showMessageDialog(null, timeMessage);
    }


    /**
     * Converts an array of node names to a list of the actual corresponding
     * nodes
     */
    private ArrayList<CyNode> stringsToNodes(String[] strings)
    {
        ArrayList<CyNode> nodes = new ArrayList<CyNode>();

        for (String name : strings)
        {
            if (idToCyNode.containsKey(name))
            {
                nodes.add(idToCyNode.get(name));
            }
        }

        return nodes;
    }


    /**
     * Writes the ksp results to a table
     */
    private void writeResults(ArrayList<Path> paths)
    {
        if (paths.size() == 0)
        {
            JOptionPane.showMessageDialog(null, "No paths found.");
            return;
        }

        // updates the table's values
        for (int i = 0; i < paths.size(); i++)
        {
            // empty path; should never happen
            if (paths.get(i).size() == 0)
                continue;

            CyRow row = table.getRow(i + 1);

            // builds the path string without supersource/supertarget [1,len-1]
            StringBuilder currPath = new StringBuilder();
            for (int j = 1; j < paths.get(i).size() - 1; j++)
            {
                currPath.append(
                    network.getRow(paths.get(i).get(j))
                        .get(CyNetwork.NAME, String.class) + "|");
            }
            currPath.setLength(currPath.length() - 1);

            // sets all the values
            row.set("k", i + 1);
            // SS|A|B|ST = length 1 path-size 4

            row.set("Length", paths.get(i).weight - 2);
            row.set("Path", currPath.toString());
        }
    }


    private void initializePanelItems()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        sourcesLabel = new JLabel("Sources separated by spaces ex. S1 S2 S3");
        sourcesTextField = new JTextField(20);
        sourcesTextField.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                sourcesTextField.getPreferredSize().height));

        targetsLabel = new JLabel("Targets separated by spaces ex. T1 T2 T3");
        targetsTextField = new JTextField(20);
        targetsTextField.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                targetsTextField.getPreferredSize().height));

        kLabel = new JLabel("k (shortest paths)");
        kTextField = new JTextField(4);
        kTextField.setMaximumSize(kTextField.getPreferredSize());

        weightedOption = new JRadioButton("Weighted");
        unweightedOption = new JRadioButton("Unweighted");
        group = new ButtonGroup();
        group.add(weightedOption);
        group.add(unweightedOption);

        JPanel sourceTargetPanel = new JPanel();
        sourceTargetPanel
            .setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.PAGE_AXIS));
        TitledBorder sourceTargetBorder =
            BorderFactory.createTitledBorder("Sources/Targets");
        sourceTargetPanel.setBorder(sourceTargetBorder);
        sourceTargetPanel.add(sourcesLabel);
        sourceTargetPanel.add(sourcesTextField);
        sourceTargetPanel.add(targetsLabel);
        sourceTargetPanel.add(targetsTextField);
        this.add(sourceTargetPanel);

        JPanel kPanel = new JPanel();
        kPanel.setLayout(new BoxLayout(kPanel, BoxLayout.PAGE_AXIS));
        TitledBorder kBorder = BorderFactory.createTitledBorder("Algorithm");
        kPanel.setBorder(kBorder);
        kPanel.add(kLabel);
        kPanel.add(kTextField);
        this.add(kPanel);

        JPanel graphPanel = new JPanel();
        graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.PAGE_AXIS));
        TitledBorder graphBorder = BorderFactory.createTitledBorder("Graph");
        graphPanel.setBorder(graphBorder);
        graphPanel.add(weightedOption);
        graphPanel.add(unweightedOption);
        this.add(graphPanel);

        submitButton = new JButton("Submit");
        submitButton.addActionListener(new SubmitButtonListener());
        this.add(submitButton, BorderLayout.SOUTH);
    }


    private boolean populateIdToCyNode()
    {
        if (network == null)
            return false;

        for (CyNode node : network.getNodeList())
        {
            String nodeName =
                network.getRow(node).get(CyNetwork.NAME, String.class);
            idToCyNode.put(nodeName, node);
        }

        return true;
    }


    @Override
    public Component getComponent()
    {
        return this;
    }


    @Override
    public CytoPanelName getCytoPanelName()
    {
        return CytoPanelName.WEST;
    }


    @Override
    public Icon getIcon()
    {
        return null;
    }


    @Override
    public String getTitle()
    {
        return "PathLinker";
    }
}
