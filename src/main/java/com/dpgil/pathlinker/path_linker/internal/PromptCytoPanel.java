package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import org.cytoscape.app.CyAppAdapter;
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
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

/** Panel for the PathLinker plugin */
public class PromptCytoPanel
    extends JPanel
    implements CytoPanelComponent
{
    private CyNetwork               _network;
    private CyTable                 _table;
    private HashMap<String, CyNode> _idToCyNode;

    private JLabel _sourcesLabel;
    private JLabel _targetsLabel;
    private JLabel _kLabel;

    private JTextField _sourcesTextField;
    private JTextField _targetsTextField;
    private JTextField _kTextField;

    private JButton _submitButton;

    private ButtonGroup  _group;
//    private JRadioButton _weightedOption;
//    private JRadioButton _unweightedOption;
    private JRadioButton _unweighted;
    private JRadioButton _weightedProbabilities;
    private JRadioButton _weightedPValues;

    private CyApplicationManager _applicationManager;
    private CyTableFactory       _tableFactory;
    private CyTableManager       _tableManager;

    private CyNetworkFactory     _networkFactory;
    private CyNetworkManager     _networkManager;
    private CyNetworkViewFactory _networkViewFactory;
    private CyNetworkViewManager _networkViewManager;

    private CyAppAdapter _adapter;

    private OpenPathLinkerMenuAction _oplaction;
    private ClosePathLinkerMenuAction _cplaction;

    private HashSet<CyEdge> _hiddenEdges;

    private EdgeWeights _edgeWeights;

    private Container _parent;

    private enum EdgeWeights
    {
        UNWEIGHTED,
        PROBABILITIES,
        P_VALUES
    };

    public enum PanelState
    {
        CLOSED,
        OPEN
    };

    private PanelState _state = null;

    /**
     * Sets the state of the panel (open or closed).
     * @param state the new state
     */
    public void setPanelState(PanelState state)
    {
        if (state == _state)
            return;

        if (state == PanelState.CLOSED)
        {
            _state = PanelState.CLOSED;
            _parent.remove(this);
            _cplaction.setEnabled(false);
            _oplaction.setEnabled(true);
        }
        else if (state == PanelState.OPEN)
        {
            _state = PanelState.OPEN;
            ((JTabbedPane)_parent).addTab(this.getTitle(), this);
            _oplaction.setEnabled(false);
            _cplaction.setEnabled(true);
        }

        this.revalidate();
        this.repaint();
    }

    /**
     * Constructor for the panel
     *
     * @param network
     *            the supplied network
     * @param table
     *            the table created
     */
    public PromptCytoPanel(CyNetwork network, CyTable table)
    {
        _network = network;
        _table = table;
        initializePanelItems();
    }


    /**
     * Constructor for the panel
     *
     * @param applicationManager
     *            the application manager
     * @param tableFactory
     *            the table factory
     * @param tableManager
     *            the table manager
     */
    public PromptCytoPanel(
        CyApplicationManager applicationManager,
        CyTableFactory tableFactory,
        CyTableManager tableManager)
    {
        _applicationManager = applicationManager;
        _tableFactory = tableFactory;
        _tableManager = tableManager;
        initializePanelItems();
        this.setVisible(false);
    }


    /**
     * Initializer for the panel
     *
     * @param networkFactory
     *            network factory
     * @param networkManager
     *            network manager
     * @param networkViewFactory
     *            network view factory
     * @param networkViewManager
     *            network view manager
     * @param adapter
     *            the cy application adapter
     */
    public void initialize(
        CyNetworkFactory networkFactory,
        CyNetworkManager networkManager,
        CyNetworkViewFactory networkViewFactory,
        CyNetworkViewManager networkViewManager,
        CyAppAdapter adapter,
        OpenPathLinkerMenuAction oplaction,
        ClosePathLinkerMenuAction cplaction)
    {
        _networkFactory = networkFactory;
        _networkManager = networkManager;
        _networkViewFactory = networkViewFactory;
        _networkViewManager = networkViewManager;
        _adapter = adapter;
        _oplaction = oplaction;
        _cplaction = cplaction;
        _parent = this.getParent();
    }


    /** Listener for the submit button in the panel */
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
        CyNetwork kspSubgraph = _networkFactory.createNetwork();

        HashSet<String> edgesAdded = new HashSet<String>();
        HashSet<String> nodesAdded = new HashSet<String>();

        HashMap<String, CyNode> subIdToCyNode = new HashMap<String, CyNode>();

        int id = 0;

        for (Path currPath : paths)
        {
            // excluding supersource and supertarget
            for (int i = 1; i < currPath.size() - 2; i++)
            {
                CyNode node1 = currPath.get(i);
                CyNode node2 = currPath.get(i + 1);

                String node1Name =
                    _network.getRow(node1).get(CyNetwork.NAME, String.class);
                String node2Name =
                    _network.getRow(node2).get(CyNetwork.NAME, String.class);

                String edgeKey = node1Name + "|" + node2Name;

                if (!nodesAdded.contains(node1Name))
                {
                    CyNode added = kspSubgraph.addNode();

//                    View<CyNode> nodeView = kspSubgraphView.getNodeView(added);
//                    VisualProperty<?> temp = new NodeShapeVisualProperty(
//                        NodeShapeVisualProperty.DIAMOND,
//                        String.valueOf(id++),
//                        node1Name,
//                        CyNode.class);
//                    nodeView.setVisualProperty(temp, (Object)NodeShapeVisualProperty.DIAMOND);

                    kspSubgraph.getRow(added).set(CyNetwork.NAME, node1Name);
                    _network.getRow(node1).set(CyNetwork.SELECTED, true);
                    nodesAdded.add(node1Name);
                    subIdToCyNode.put(node1Name, added);
                }

                if (!nodesAdded.contains(node2Name))
                {
                    CyNode added = kspSubgraph.addNode();
                    kspSubgraph.getRow(added).set(CyNetwork.NAME, node2Name);
                    _network.getRow(node2).set(CyNetwork.SELECTED, true);
                    nodesAdded.add(node2Name);
                    subIdToCyNode.put(node2Name, added);
                }

                if (!edgesAdded.contains(edgeKey))
                {
                    CyNode a = subIdToCyNode.get(node1Name);
                    CyNode b = subIdToCyNode.get(node2Name);
                    kspSubgraph.addEdge(a, b, true);

                    CyEdge select = Algorithms.getEdge(_network, node1, node2);
                    _network.getRow(select).set(CyNetwork.SELECTED, true);
                    edgesAdded.add(edgeKey);
                }
            }
        }

        // TODO update view
        CyNetworkView kspSubgraphView =
            _networkViewFactory.createNetworkView(kspSubgraph);
        _networkManager.addNetwork(kspSubgraph);
        _networkViewManager.addNetworkView(kspSubgraphView);

        // set node layout
        CyLayoutAlgorithm algo =
            _adapter.getCyLayoutAlgorithmManager().getDefaultLayout();
        TaskIterator iter = algo.createTaskIterator(
            kspSubgraphView,
            algo.createLayoutContext(),
            CyLayoutAlgorithm.ALL_NODE_VIEWS,
            null);
        _adapter.getTaskManager().execute(iter);
        SynchronousTaskManager<?> synTaskMan = _adapter.getCyServiceRegistrar()
            .getService(SynchronousTaskManager.class);
        synTaskMan.execute(iter);
        _adapter.getVisualMappingManager().getVisualStyle(kspSubgraphView)
            .apply(kspSubgraphView);
        kspSubgraphView.updateView();

    }


    private boolean prepareForKSP()
    {

        _network = _applicationManager.getCurrentNetwork();

        _idToCyNode = new HashMap<String, CyNode>();
        boolean success = populateIdToCyNode();
        if (!success)
        {
            JOptionPane.showMessageDialog(
                null,
                "No current network. PathLinker cannot run without a network. Exiting...");
            return false;
        }

        return true;
    }


    private void runKSP()
    {
        // grabs the values in the source and target text fields
        String sourcesTextFieldValue = _sourcesTextField.getText();
        String targetsTextFieldValue = _targetsTextField.getText();

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
            if (!_idToCyNode.containsKey(sourceName))
            {
                sourcesNotInNet.add(sourceName);
            }
        }
        for (String targetName : targetNames)
        {
            if (!_idToCyNode.containsKey(targetName))
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
        String kInput = _kTextField.getText();
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

        if (_unweighted.isSelected())
        {
            _edgeWeights = EdgeWeights.UNWEIGHTED;
        }
        else if (_weightedProbabilities.isSelected())
        {
            _edgeWeights = EdgeWeights.PROBABILITIES;
        }
        else if (_weightedPValues.isSelected())
        {
            _edgeWeights = EdgeWeights.P_VALUES;
        }
        else
        {
            // TODO error check
            JOptionPane.showMessageDialog(null, "No option selected for edge weights. Exiting...");
            return;
        }

        _hiddenEdges = new HashSet<CyEdge>();

        // hides all incoming edges to source nodes
        for (CyNode source : sources)
        {
            _hiddenEdges.addAll(
                _network.getAdjacentEdgeList(source, CyEdge.Type.INCOMING));
        }
        // hides all outgoing edges from target nodes
        for (CyNode target : targets)
        {
            _hiddenEdges.addAll(
                _network.getAdjacentEdgeList(target, CyEdge.Type.OUTGOING));
        }

        storeOldWeights();

        prepareEdgesForKSP();

        // sets up the super source/super target
        CyNode superSource = _network.addNode();
        CyNode superTarget = _network.addNode();
        ArrayList<CyEdge> superEdges = new ArrayList<CyEdge>();

        // attaches super source to all sources
        for (CyNode source : sources)
        {
            CyEdge superEdge = _network.addEdge(superSource, source, true);

            // sets an edge weight of 1 for the edges. in the weighted case,
            // we do path.weight - 2 to account for the supersource->source and
            // the target->supertarget. in the unweighted case we also do
            // path.weight - 2 because we don't include those two edges.
            // assigning an edge weight of 1 allows us to calculate the final
            // weight the same way for both cases.
            Algorithms.setWeight(_network, superEdge, 0.);
            superEdges.add(superEdge);
        }
        // attaches all targets to super target
        for (CyNode target : targets)
        {
            CyEdge superEdge = _network.addEdge(target, superTarget, true);

            // sets an edge weight of 1 for the edges. in the weighted case,
            // we do path.weight - 2 to account for the supersource->source and
            // the target->supertarget. in the unweighted case we also do
            // path.weight - 2 because we don't include those two edges.
            // assigning an edge weight of 1 allows us to calculate the final
            // weight the same way for both cases.
            Algorithms.setWeight(_network, superEdge, 0.);
            superEdges.add(superEdge);
        }

        long startTime = System.currentTimeMillis();

        // runs the ksp
        Algorithms.initializeHiddenEdges(_hiddenEdges);
        ArrayList<Path> paths =
            Algorithms.ksp(_network, superSource, superTarget, k);

        long endTime = System.currentTimeMillis();

        // removes supernodes and their edges
        _network.removeEdges(superEdges);
        _network.removeNodes(Arrays.asList(superSource, superTarget));

        writeResults(paths);

        restoreOldWeights();

        createKSPSubgraph(paths);

        // notifies user of time taken
        long totalTimeMs = endTime - startTime;
        String timeMessage =
            "PathLinker took " + totalTimeMs + " ms to execute";
        JOptionPane.showMessageDialog(null, timeMessage);
    }

    private void prepareEdgesForKSP()
    {
        if (_edgeWeights == EdgeWeights.UNWEIGHTED)
        {
            // sets all edge values to 1
            for (CyEdge edge : _network.getEdgeList())
            {
                Algorithms.setWeight(_network, edge, 1.);
            }
        }
        else
        {
            logTransformEdgeWeights();
        }
    }

    private void restoreOldWeights()
    {
        for (CyEdge edge : _network.getEdgeList())
        {
            double oldWeight = _network.getRow(edge).get("old_weight", Double.class);
            _network.getRow(edge).set("edge_weight", oldWeight);
        }

        _network.getDefaultEdgeTable().deleteColumn("old_weight");
    }

    private void storeOldWeights()
    {
        _network.getDefaultEdgeTable().createColumn("old_weight", Double.class, false);

        for (CyEdge edge : _network.getEdgeList())
        {
            double weight = Algorithms.getWeight(_network, edge);
            _network.getRow(edge).set("old_weight", weight);
        }
    }

    private void logTransformEdgeWeights()
    {
        double sumWeight = 0.;

        for (CyEdge edge : _network.getEdgeList())
        {
            if (_hiddenEdges.contains(edge))
                continue;

            double edge_weight = Algorithms.getWeight(_network, edge);
            if (_edgeWeights == EdgeWeights.P_VALUES)
                edge_weight = 1 - edge_weight;

            sumWeight += edge_weight;
        }

        for (CyEdge edge : _network.getEdgeList())
        {
            if (_hiddenEdges.contains(edge))
                continue;

            double edge_weight = Algorithms.getWeight(_network, edge);
            if (_edgeWeights == EdgeWeights.P_VALUES)
                edge_weight = 1 - edge_weight;

            double w = -1 * Math.log(Math.max(0.000000001, edge_weight / sumWeight)) / Math.log(10);
            Algorithms.setWeight(_network, edge, w);
        }
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
            if (_idToCyNode.containsKey(name))
            {
                nodes.add(_idToCyNode.get(name));
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

        _table = _tableFactory
            .createTable("PathLinker ", "Path index", Integer.class, true, true);
        // sets up the table
        // table.createColumn("k", Integer.class, false);
        _table.createColumn("Path score", Double.class, false);
        _table.createColumn("Path", String.class, false);
        // adds the table to cytoscape
        _applicationManager.setCurrentTable(_table);
        _tableManager.addTable(_table);

        // updates the table's values
        for (int i = 0; i < paths.size(); i++)
        {
            // empty path; should never happen
            if (paths.get(i).size() == 0)
                continue;

            CyRow row = _table.getRow(i + 1);

            // builds the path string without supersource/supertarget [1,len-1]
            StringBuilder currPath = new StringBuilder();
            for (int j = 1; j < paths.get(i).size() - 1; j++)
            {
                currPath.append(
                    _network.getRow(paths.get(i).get(j))
                        .get(CyNetwork.NAME, String.class) + "|");
            }
            currPath.setLength(currPath.length() - 1);

            // sets all the values
// row.set("k", i + 1);

            row.set("Length", paths.get(i).weight);
            row.set("Path", currPath.toString());
        }
    }


    /**
     * Sets up all the components in the panel
     */
    private void initializePanelItems()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        _sourcesLabel = new JLabel("Sources separated by spaces ex. S1 S2 S3");
        _sourcesTextField = new JTextField(20);
        _sourcesTextField.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                _sourcesTextField.getPreferredSize().height));

        _targetsLabel = new JLabel("Targets separated by spaces ex. T1 T2 T3");
        _targetsTextField = new JTextField(20);
        _targetsTextField.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                _targetsTextField.getPreferredSize().height));

        _kLabel = new JLabel("k (# of paths)");
        _kTextField = new JTextField(4);
        _kTextField.setMaximumSize(_kTextField.getPreferredSize());

        _group = new ButtonGroup();

        _unweighted = new JRadioButton("Unweighted - PathLinker will compute the k lowest cost paths, where the cost is the number of edges in the path.");
        _weightedProbabilities = new JRadioButton("Weighted, edge weights are probabilities - PathLinker will compute the k highest cost paths, where the cost is the product of the edge weights.");
        _weightedPValues = new JRadioButton("Weighted, edge weights are p-values - PathLinker will compute the k highest cost paths, where the cost is the product of (1 - p-value) for each edge in the path.");
        _group.add(_unweighted);
        _group.add(_weightedProbabilities);
        _group.add(_weightedPValues);

        JPanel sourceTargetPanel = new JPanel();
        sourceTargetPanel
            .setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.PAGE_AXIS));
        TitledBorder sourceTargetBorder =
            BorderFactory.createTitledBorder("Sources/Targets");
        sourceTargetPanel.setBorder(sourceTargetBorder);
        sourceTargetPanel.add(_sourcesLabel);
        sourceTargetPanel.add(_sourcesTextField);
        sourceTargetPanel.add(_targetsLabel);
        sourceTargetPanel.add(_targetsTextField);
        this.add(sourceTargetPanel);

        JPanel kPanel = new JPanel();
        kPanel.setLayout(new BoxLayout(kPanel, BoxLayout.PAGE_AXIS));
        TitledBorder kBorder = BorderFactory.createTitledBorder("Algorithm");
        kPanel.setBorder(kBorder);
        kPanel.add(_kLabel);
        kPanel.add(_kTextField);
        this.add(kPanel);

        JPanel graphPanel = new JPanel();
        graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.PAGE_AXIS));
        TitledBorder graphBorder = BorderFactory.createTitledBorder("Edge Weights");
        graphPanel.setBorder(graphBorder);
        graphPanel.add(_unweighted);
        graphPanel.add(_weightedProbabilities);
        graphPanel.add(_weightedPValues);
        this.add(graphPanel);

        _submitButton = new JButton("Submit");
        _submitButton.addActionListener(new SubmitButtonListener());
        this.add(_submitButton, BorderLayout.SOUTH);

        _unweighted.setSelected(true);
    }


    /**
     * Populates idToCyNode, the map of node names to their objects
     */
    private boolean populateIdToCyNode()
    {
        if (_network == null)
            return false;

        for (CyNode node : _network.getNodeList())
        {
            String nodeName =
                _network.getRow(node).get(CyNetwork.NAME, String.class);
            _idToCyNode.put(nodeName, node);
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
