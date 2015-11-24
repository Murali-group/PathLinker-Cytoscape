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
import java.util.Map;
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
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

/** Panel for the PathLinker plugin */
public class PathLinkerPanel
    extends JPanel
    implements CytoPanelComponent
{
    /** UI components of the panel */
    private JLabel       _sourcesLabel;
    private JLabel       _targetsLabel;
    private JLabel       _kLabel;
    private JTextField   _sourcesTextField;
    private JTextField   _targetsTextField;
    private JTextField   _kTextField;
    private JButton      _submitButton;
    private ButtonGroup  _weightedOptionGroup;
    private JRadioButton _unweighted;
    private JRadioButton _weightedProbabilities;
    private JRadioButton _weightedPValues;
    private ButtonGroup  _subgraphOptionGroup;
    private JRadioButton _subgraph;
    private JRadioButton _noSubgraph;

    /** Cytoscape classes for table management */
    private CyTableFactory _tableFactory;
    private CyTableManager _tableManager;

    /** Cytoscape class for network and view management */
    private CyApplicationManager _applicationManager;
    private CyNetworkFactory     _networkFactory;
    private CyNetworkManager     _networkManager;
    private CyNetworkViewFactory _networkViewFactory;
    private CyNetworkViewManager _networkViewManager;
    private CyAppAdapter         _adapter;

    /** Menu options for PathLinker */
    private OpenPathLinkerMenuAction  _openAction;
    private ClosePathLinkerMenuAction _closeAction;

    /** Edges that we hide from the algorithm */
    private HashSet<CyEdge>         _hiddenEdges;
    /** Perform algo unweighted, weighted (probs), or weighted (p-values) */
    private EdgeWeightSetting       _edgeWeightSetting;
    /** Parent container of the panel to re add to when we call open */
    private Container               _parent;
    /** The network to perform the algorithm on */
    private CyNetwork               _network;
    /** The table to write the results of the algorithm to */
    private CyTable                 _table;
    /** A mapping of the name of a node to the acutal node object */
    private HashMap<String, CyNode> _idToCyNode;
    /** State of the panel. Initially null b/c it isn't open or closed yet */
    private PanelState              _state = null;

    /** The sources to be used in the algorithm */
    private ArrayList<CyNode> _sources;
    /** The targets to be used in the algorithm */
    private ArrayList<CyNode> _targets;
    /** The k value to be used in the algorithm */
    private int               _k;
    /** The super source to call ksp with and removed after the algorithm */
    private CyNode            _superSource;
    /** The super target to call ksp with and removed after the algorithm */
    private CyNode            _superTarget;
    /** The edges attached to super(source/target) to be removed after ksp */
    private HashSet<CyEdge>   _superEdges;
    /** Whether or not to generate a subgraph */
    private boolean           _generateSubgraph;


    private enum EdgeWeightSetting
    {
        UNWEIGHTED,
        PROBABILITIES,
        P_VALUES
    };


    /** The state of the panel */
    public enum PanelState
    {
        /** The panel is hidden */
        CLOSED,
        /** The panel is visible */
        OPEN
    };


    /**
     * Sets the state of the panel (open or closed).
     *
     * @param state
     *            the new state
     */
    public void setPanelState(PanelState state)
    {
        if (state == _state)
            return;

        if (state == PanelState.CLOSED)
        {
            _state = PanelState.CLOSED;
            _parent.remove(this);
            _closeAction.setEnabled(false);
            _openAction.setEnabled(true);
        }
        else if (state == PanelState.OPEN)
        {
            _state = PanelState.OPEN;
            ((JTabbedPane)_parent).addTab(this.getTitle(), this);
            _openAction.setEnabled(false);
            _closeAction.setEnabled(true);
        }

        this.revalidate();
        this.repaint();
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
    public PathLinkerPanel(
        CyApplicationManager applicationManager,
        CyTableFactory tableFactory,
        CyTableManager tableManager)
    {
        _applicationManager = applicationManager;
        _tableFactory = tableFactory;
        _tableManager = tableManager;
        initializePanelItems();
    }


    /**
     * Initializer for the panel to reduce the number of parameters in the
     * constructor
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
     * @param openAction
     *            the menu action to open PathLinker
     * @param closeAction
     *            the menu action to close PathLinkers
     */
    public void initialize(
        CyNetworkFactory networkFactory,
        CyNetworkManager networkManager,
        CyNetworkViewFactory networkViewFactory,
        CyNetworkViewManager networkViewManager,
        CyAppAdapter adapter,
        OpenPathLinkerMenuAction openAction,
        ClosePathLinkerMenuAction closeAction)
    {
        _networkFactory = networkFactory;
        _networkManager = networkManager;
        _networkViewFactory = networkViewFactory;
        _networkViewManager = networkViewManager;
        _adapter = adapter;
        _openAction = openAction;
        _closeAction = closeAction;
        _parent = this.getParent();
    }


    /** Listener for the submit button in the panel */
    class SubmitButtonListener
        implements ActionListener
    {
        /**
         * Responds to a click of the submit button in the pathlinker jpanel
         */
        @Override
        public void actionPerformed(ActionEvent e)
        {
            runKSP();
        }
    }


    private void runKSP()
    {
        boolean success;

        // populates a mapping from the name of a node to the actual node object
        // used for converting user input to node objects. populates the map
        // named _idToCyNode. is unsuccessful if there is no network
        success = populateIdToCyNode();
        if (!success)
            return;

        // reads the raw values from the panel and converts them into useful
        // objects to be used in the algorithms
        success = readValuesFromPanel();
        if (!success)
            return;

        // "removes" the edges that are incoming to source nodes and outgoing
        // from target nodes
        initializeHiddenEdges();

        // sets the edge weights to be used in the algorithm. doesn't actually
        // set the values as edge attributes because that dominates runtime.
        setEdgeWeights();

        // adds a superSource and superTarget and attaches them to the sources
        // and targets, respectively
        addSuperNodes();

        // runs the KSP algorithm
        ArrayList<Path> result =
            Algorithms.ksp(_network, _superSource, _superTarget, _k);

        // removes the superSource, superTarget, and edges associated
        // with them
        removeSuperNodes();

        // "un log-transforms" the path scores in the weighted options
        // as to undo the log transformations and leave the path scores
        // in terms of the edge weights
        normalizePathScores(result);

        // generates a subgraph of the nodes and edges involved in the resulting
        // paths and displays it to the user
        if (_generateSubgraph)
            createKSPSubgraph(result);

        // writes the result of the algorithm to a table
        writeResult(result);
    }


    /**
     * Reads in the raw values from the panel and converts them to useful
     * objects that can be used for the algorithm. Performs error checking on
     * the values and warns the user if it is a minor error or quits if there
     * are any major errors.
     *
     * @return true if the parsing was successful, false otherwise
     */
    private boolean readValuesFromPanel()
    {
        // error message to report errors to the user if they occur
        StringBuilder errorMessage = new StringBuilder();

        // grabs the values in the source and target text fields
        String sourcesTextFieldValue = _sourcesTextField.getText();
        String targetsTextFieldValue = _targetsTextField.getText();

        // splits the names by spaces
        String[] rawSourceNames = sourcesTextFieldValue.split(" ");
        String[] rawTargetNames = targetsTextFieldValue.split(" ");
        ArrayList<String> sourceNames =
            new ArrayList<String>(Arrays.asList(rawSourceNames));
        ArrayList<String> targetNames =
            new ArrayList<String>(Arrays.asList(rawTargetNames));

        // stores the sources/targets that were inputted but are not actually in
        // the network, may have been mistyped
        ArrayList<String> sourcesNotInNet = new ArrayList<String>();
        ArrayList<String> targetsNotInNet = new ArrayList<String>();

        // checks for mistyped source/target names
        for (String sourceName : sourceNames)
        {
            if (!_idToCyNode.containsKey(sourceName))
                sourcesNotInNet.add(sourceName);
        }
        for (String targetName : targetNames)
        {
            if (!_idToCyNode.containsKey(targetName))
                targetsNotInNet.add(targetName);
        }

        // appends all missing sources/targets to the error message
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

        // parses the value inputted for k
        // if it is an invalid value, uses 200 by default and also appends the
        // error to the error message
        String kInput = _kTextField.getText();
        try
        {
            _k = Integer.parseInt(kInput);
        }
        catch (NumberFormatException exception)
        {
            errorMessage.append(
                "Invalid number " + kInput
                    + " entered for k. Using default k=200.\n");
            _k = 200;
        }

        if (_unweighted.isSelected())
        {
            _edgeWeightSetting = EdgeWeightSetting.UNWEIGHTED;
        }
        else if (_weightedProbabilities.isSelected())
        {
            _edgeWeightSetting = EdgeWeightSetting.PROBABILITIES;
        }
        else if (_weightedPValues.isSelected())
        {
            _edgeWeightSetting = EdgeWeightSetting.P_VALUES;
        }
        else
        {
            errorMessage.append(
                "No option selected for edge weights. Using unweighted as default.\n");
            _edgeWeightSetting = EdgeWeightSetting.UNWEIGHTED;
        }

        if (_subgraph.isSelected())
        {
            _generateSubgraph = true;
        }
        else if (_noSubgraph.isSelected())
        {
            _generateSubgraph = false;
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
                return false;
            }
        }
        else
        {
            String message = "No errors found. Press 'OK' to run PathLinker.";
            JOptionPane.showMessageDialog(null, message);
        }

        // TODO check if there are actually edge weights

        // generates a list of the valid source/target nodes to be used in
        // the graph
        sourceNames.removeAll(sourcesNotInNet);
        targetNames.removeAll(targetsNotInNet);
        _sources = stringsToNodes(sourceNames);
        _targets = stringsToNodes(targetNames);

        // makes sure that we actually have valid sources and targets
        if (_sources.size() == 0)
        {
            JOptionPane.showMessageDialog(
                null,
                "There are no valid sources to be used. Quitting...");
            return false;
        }
        if (_targets.size() == 0)
        {
            JOptionPane.showMessageDialog(
                null,
                "There are no valid targets to be used. Quitting...");
            return false;
        }

        // successful parsing
        return true;
    }


    /**
     * Initializes the edges that we are hiding from the algorithm. Doesn't
     * actually remove the edges as that dominates runtime.
     */
    private void initializeHiddenEdges()
    {
        _hiddenEdges = new HashSet<CyEdge>();

        // hides all incoming edges to source nodes
        for (CyNode source : _sources)
        {
            _hiddenEdges.addAll(
                _network.getAdjacentEdgeList(source, CyEdge.Type.INCOMING));
        }
        // hides all outgoing edges from target nodes
        for (CyNode target : _targets)
        {
            _hiddenEdges.addAll(
                _network.getAdjacentEdgeList(target, CyEdge.Type.OUTGOING));
        }

        Algorithms.initializeHiddenEdges(_hiddenEdges);
    }


    /**
     * Sets the edge weights to be used in the algorithm. Doesn't actually set
     * the weights as attributes because that dominates runtime.
     */
    private void setEdgeWeights()
    {
        HashMap<CyEdge, Double> edgeWeights = new HashMap<CyEdge, Double>();

        // TODO error checking if weighted but not all edges have weights

        for (CyEdge edge : _network.getEdgeList())
        {
            // gets the attribute edge weight value
            Double value =
                _network.getRow(edge).get("edge_weight", Double.class);
            double edge_weight = value != null ? value.doubleValue() : -44444;

            if (_edgeWeightSetting == EdgeWeightSetting.UNWEIGHTED)
            {
                edgeWeights.put(edge, 1.);
            }
            else if (_edgeWeightSetting == EdgeWeightSetting.PROBABILITIES)
            {
                edgeWeights.put(edge, edge_weight);
            }
            else if (_edgeWeightSetting == EdgeWeightSetting.P_VALUES)
            {
                edgeWeights.put(edge, 1. - edge_weight);
            }
        }

        // log transforms the edge weights for both weighted options
        if (_edgeWeightSetting != EdgeWeightSetting.UNWEIGHTED)
        {
            logTransformEdgeWeights(edgeWeights);
        }

        // sets the weights in the algorithms class
        Algorithms.setEdgeWeights(edgeWeights);
    }


    /**
     * Performs a log transformation on the supplied edges in place given a
     * mapping from edges to their initial weights
     *
     * @param weights
     *            the mapping from edges to their initial weights
     */
    private void logTransformEdgeWeights(HashMap<CyEdge, Double> weights)
    {
        double sumWeight = 0.;

        for (CyEdge edge : weights.keySet())
        {
            if (_hiddenEdges.contains(edge))
                continue;

            sumWeight += weights.get(edge);
        }

        for (CyEdge edge : weights.keySet())
        {
            if (_hiddenEdges.contains(edge))
                continue;

            double edge_weight = weights.get(edge);

            double w =
                -1 * Math.log(Math.max(0.000000001, edge_weight / sumWeight))
                    / Math.log(10);
            weights.put(edge, w);
        }
    }


    /**
     * Adds a superSource and superTarget and attaches them to the sources and
     * targets, respectively. Sets _superSource, _superTarget, and populates the
     * list _superEdges, so they can be removed later.
     */
    private void addSuperNodes()
    {
        // sets up the super source/super target
        _superSource = _network.addNode();
        _superTarget = _network.addNode();
        _superEdges = new HashSet<CyEdge>();

        // attaches super source to all sources
        for (CyNode source : _sources)
        {
            CyEdge superEdge = _network.addEdge(_superSource, source, true);

            // sets an edge weight of 0, so the edges connecting the super nodes
            // and the sources/targets don't affect the final path weights
            Algorithms.setWeight(superEdge, 0.);
            _superEdges.add(superEdge);
        }
        // attaches all targets to super target
        for (CyNode target : _targets)
        {
            CyEdge superEdge = _network.addEdge(target, _superTarget, true);

            // sets an edge weight of 0, so the edges connecting the super nodes
            // and the sources/targets don't affect the final path weights
            Algorithms.setWeight(superEdge, 0.);
            _superEdges.add(superEdge);
        }
    }


    /**
     * Removes the super source, super target, and all edges associated with
     * these nodes to restore the network back to its original condition
     */
    private void removeSuperNodes()
    {
        _network.removeNodes(Arrays.asList(_superSource, _superTarget));
        _network.removeEdges(_superEdges);
    }


    /**
     * "un log-transforms" the path scores in the weighted options to undo the
     * log transformations and leave the path scores in terms of the original
     * edge weights
     *
     * @param paths
     *            the list of paths from the ksp algorithm
     */
    private void normalizePathScores(ArrayList<Path> paths)
    {
        // weighted probabilities option sets the weight to 2 ^ (-weight)
        if (_edgeWeightSetting == EdgeWeightSetting.PROBABILITIES)
        {
            for (Path p : paths)
            {
                p.weight = Math.pow(2, -1 * p.weight);
            }
        }
        // weighted p-values option sets the weight to 1 - 2 ^ (-weight)
        else if (_edgeWeightSetting == EdgeWeightSetting.P_VALUES)
        {
            for (Path p : paths)
            {
                p.weight = 1 - Math.pow(2, -1 * p.weight);
            }
        }

        // don't have to do anything for unweighted option
    }


    /**
     * Writes the ksp results to a table given the results from the ksp
     * algorithm
     *
     * @param paths
     *            a list of paths generated from the ksp algorithm
     */
    private void writeResult(ArrayList<Path> paths)
    {
        if (paths.size() == 0)
        {
            JOptionPane.showMessageDialog(null, "No paths found.");
            return;
        }

        ResultFrame resultFrame = new ResultFrame(_network, paths);
        resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        resultFrame.setVisible(true);
        resultFrame.setSize(500, 700);

// _table = _tableFactory.createTable(
// "PathLinker ",
// "Path index",
// Integer.class,
// true,
// true);
// // sets up the table
// _table.createColumn("Path score", Double.class, false);
// _table.createColumn("Path", String.class, false);
// // adds the table to cytoscape
// _applicationManager.setCurrentTable(_table);
// _tableManager.addTable(_table);
//
// // updates the table's values
// for (int i = 0; i < paths.size(); i++)
// {
// // empty path; should never happen
// if (paths.get(i).size() == 0)
// continue;
//
// CyRow row = _table.getRow(i + 1);
//
// // builds the path string without supersource/supertarget [1,len-1]
// StringBuilder currPath = new StringBuilder();
// for (int j = 1; j < paths.get(i).size() - 1; j++)
// {
// currPath.append(
// _network.getRow(paths.get(i).get(j))
// .get(CyNetwork.NAME, String.class) + "|");
// }
// currPath.setLength(currPath.length() - 1);
//
// // sets all the values
// row.set("Path score", paths.get(i).weight);
// row.set("Path", currPath.toString());
// }
    }


    /**
     * Generates a subgraph of the user supplied graph that contains only the
     * nodes and edges that are in the k shortest paths
     *
     * @param paths
     *            the list of paths generated by ksp algorithm
     */
    private void createKSPSubgraph(ArrayList<Path> paths)
    {
        CyNetwork kspSubgraph = _networkFactory.createNetwork();
        CyTable kspSubTable = kspSubgraph.getDefaultEdgeTable();
        HashSet<String> seenColumns = new HashSet<String>();

        // sets the network name
        String subgraphName = "PathLinker-subnetwork-" + _k + "-paths";
        kspSubgraph.getRow(kspSubgraph).set(CyNetwork.NAME, subgraphName);

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
                    _network.getRow(node1).get(CyNetwork.NAME, String.class);
                String node2Name =
                    _network.getRow(node2).get(CyNetwork.NAME, String.class);

                String edgeKey = node1Name + "|" + node2Name;

                // adds a node if we haven't seen it yet
                if (!nodesAdded.contains(node1Name))
                {
                    CyNode added = kspSubgraph.addNode();
                    kspSubgraph.getRow(added).set(CyNetwork.NAME, node1Name);
// _network.getRow(node1).set(CyNetwork.SELECTED, true);
                    nodesAdded.add(node1Name);
                    subIdToCyNode.put(node1Name, added);
                }

                // adds the node if we haven't seen it yet
                if (!nodesAdded.contains(node2Name))
                {
                    CyNode added = kspSubgraph.addNode();
                    kspSubgraph.getRow(added).set(CyNetwork.NAME, node2Name);
// _network.getRow(node2).set(CyNetwork.SELECTED, true);
                    nodesAdded.add(node2Name);
                    subIdToCyNode.put(node2Name, added);
                }

                // adds the edge if we haven't seen it yet
                if (!edgesAdded.contains(edgeKey))
                {
                    // adds the edge to the subgraph
                    CyNode a = subIdToCyNode.get(node1Name);
                    CyNode b = subIdToCyNode.get(node2Name);
                    CyEdge added = kspSubgraph.addEdge(a, b, true);
                    CyRow addedRow = kspSubgraph.getRow(added);

                    // selects the edge in the underlying network
                    CyEdge select = Algorithms.getEdge(_network, node1, node2);
                    CyRow currRow = _network.getRow(select);
// currRow.set(CyNetwork.SELECTED, true);

                    // gets the current edge attributes
                    Map<String, Object> values = currRow.getAllValues();

                    // updates the edge attributes for the new network
                    for (String key : values.keySet())
                    {
                        // haven't seen this column yet
                        // might need to create it in the new subgraph table
                        if (!seenColumns.contains(key))
                        {
                            if (kspSubTable.getColumn(key) == null)
                            {
                                kspSubTable.createColumn(
                                    key,
                                    values.get(key).getClass(),
                                    false);
                            }

                            seenColumns.add(key);
                        }

                        // sets the new edge attribute
                        addedRow.set(key, values.get(key));
                    }

                    // makes sure the subnetwork edges aren't selected
                    addedRow.set(CyNetwork.SELECTED, false);

                    edgesAdded.add(edgeKey);
                }
            }
        }

        // creates the new network and its view
        CyNetworkView kspSubgraphView =
            _networkViewFactory.createNetworkView(kspSubgraph);
        _networkManager.addNetwork(kspSubgraph);
        _networkViewManager.addNetworkView(kspSubgraphView);

        // set node layout by applying the default layout algorithm
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


    /**
     * Converts an array of node names to a list of the actual corresponding
     * nodes
     *
     * @param names
     *            the names of the nodes that we want
     * @return a list of the actual node objects with the given names
     */
    private ArrayList<CyNode> stringsToNodes(ArrayList<String> names)
    {
        ArrayList<CyNode> nodes = new ArrayList<CyNode>();

        for (String name : names)
        {
            if (_idToCyNode.containsKey(name))
            {
                nodes.add(_idToCyNode.get(name));
            }
        }

        return nodes;
    }


    /**
     * Populates idToCyNode, the map of node names to their objects
     */
    private boolean populateIdToCyNode()
    {
        _network = _applicationManager.getCurrentNetwork();
        _idToCyNode = new HashMap<String, CyNode>();

        if (_network == null)
        {
            JOptionPane.showMessageDialog(
                null,
                "No current network. PathLinker cannot run without a network. Exiting...");
            return false;
        }

        for (CyNode node : _network.getNodeList())
        {
            String nodeName =
                _network.getRow(node).get(CyNetwork.NAME, String.class);
            _idToCyNode.put(nodeName, node);
        }

        return true;
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
        _kTextField = new JTextField(7);
        _kTextField.setMaximumSize(_kTextField.getPreferredSize());

        _weightedOptionGroup = new ButtonGroup();
        _unweighted = new JRadioButton(
            "<html><b>Unweighted</b> - PathLinker will compute the k lowest cost paths, where the cost is the number of edges in the path.</html>");
        _weightedProbabilities = new JRadioButton(
            "<html><b>Weighted, edge weights are probabilities</b> - PathLinker will compute the k highest cost paths, where the cost is the product of the edge weights.</html>");
        _weightedPValues = new JRadioButton(
            "<html><b>Weighted, edge weights are p-values</b> - PathLinker will compute the k highest cost paths, where the cost is the product of (1 - p-value) for each edge in the path.</html>");
        _weightedOptionGroup.add(_unweighted);
        _weightedOptionGroup.add(_weightedProbabilities);
        _weightedOptionGroup.add(_weightedPValues);

        _subgraphOptionGroup = new ButtonGroup();
        _subgraph = new JRadioButton(
            "<html>Generate a subnetwork of the nodes/edges involved in the k paths</html>");
        _noSubgraph = new JRadioButton(
            "<html>Do not generate a subnetwork of the nodes/edges involved in the k paths</html>");
        _subgraphOptionGroup.add(_subgraph);
        _subgraphOptionGroup.add(_noSubgraph);

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
        TitledBorder graphBorder =
            BorderFactory.createTitledBorder("Edge Weights");
        graphPanel.setBorder(graphBorder);
        graphPanel.add(_unweighted);
        graphPanel.add(_weightedProbabilities);
        graphPanel.add(_weightedPValues);
        this.add(graphPanel);

        JPanel subgraphPanel = new JPanel();
        subgraphPanel
            .setLayout(new BoxLayout(subgraphPanel, BoxLayout.PAGE_AXIS));
        TitledBorder subgraphBorder =
            BorderFactory.createTitledBorder("Output");
        subgraphPanel.setBorder(subgraphBorder);
        subgraphPanel.add(_subgraph);
        subgraphPanel.add(_noSubgraph);
        this.add(subgraphPanel);

        _submitButton = new JButton("Submit");
        _submitButton.addActionListener(new SubmitButtonListener());
        this.add(_submitButton, BorderLayout.SOUTH);

        _unweighted.setSelected(true);
        _subgraph.setSelected(true);

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
