package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

/** Panel for the PathLinker plugin */
public class PathLinkerPanel extends JPanel implements CytoPanelComponent {
	/** UI components of the panel */
	private JLabel _sourcesLabel;
	private JLabel _targetsLabel;
	private JLabel _kLabel;
	private JLabel _edgePenaltyLabel;
	private JTextField _sourcesTextField;
	private JTextField _targetsTextField;
	private JTextField _kTextField;
	private JTextField _edgePenaltyTextField;
	protected static JButton _loadNodeToSourceButton;
	protected static JButton _loadNodeToTargetButton;
	private JButton _clearSourceTargetPanelButton;
	private JButton _submitButton;
	private JLabel _edgeWeightColumnLabel;
	private JComboBox<String> _edgeWeightColumnBox;
	private ButtonGroup _weightedOptionGroup;
	private JRadioButton _unweighted;
	private JRadioButton _weightedAdditive;
	private JRadioButton _weightedProbabilities;
	private JCheckBox _subgraphOption;
	private JCheckBox _allowSourcesTargetsInPathsOption;
	private JCheckBox _targetsSameAsSourcesOption;
	private JCheckBox _includePathScoreTiesOption;
	private JLabel _runningMessage;

	/** Cytoscape class for network and view management */
	private CySwingApplication _cySwingApp;
	protected static CyApplicationManager _applicationManager;
	private CyNetworkManager _networkManager;
	private CyNetworkViewFactory _networkViewFactory;
	private CyNetworkViewManager _networkViewManager;
	private CyAppAdapter _adapter;

	/** The model that runs ksp algorithm from the user input */
	private PathLinkerModel _model;
	/** The network to perform the algorithm on */
	private CyNetwork _network;
	/** Parent container of the panel to re add to when we call open */
	private Container _parent;
	/** State of the panel. Initially null b/c it isn't open or closed yet */
	private PanelState _state = null;
	/** Index of the tab in the parent panel */
	private int _tabIndex;
	private boolean _allEdgesContainWeights = true;
	/** The original network selected by the user */
	private CyNetwork _originalNetwork;
	/** The k value to be used in the algorithm */
	private int _kValue;
	/** Perform algo unweighted, weighted (probs), or weighted (p-values) */
	private EdgeWeightSetting _edgeWeightSetting;
	/** The value by which to penalize each edge weight */
	private double _edgePenalty;
	/** The StringBuilder that construct error messages if any to the user */
	private StringBuilder errorMessage;

	/** The state of the panel */
	public enum PanelState {
		/** The panel is hidden */
		CLOSED,
		/** The panel is visible */
		OPEN
	};

	/**
	 * Sets the state of the panel (open or closed).
	 *
	 * @param newState
	 *            the new state
	 */
	public void setPanelState(PanelState newState) {
		if (newState == _state) {
			// occurs when panel is already "open" (it's in the cytopanel)
			// so we don't need to re add it to the panel, just set it as
			// selected
			if (newState == PanelState.OPEN) {
				CytoPanel cytoPanel = _cySwingApp.getCytoPanel(getCytoPanelName());
				if (cytoPanel.getState() == CytoPanelState.HIDE) {
					cytoPanel.setState(CytoPanelState.DOCK);
				}
				setVisible(true);
				// The panel is selected upon clicking PathLinker -> Open
				cytoPanel.setSelectedIndex(cytoPanel.indexOfComponent(getComponent()));
			}

			return;
		}

		if (newState == PanelState.CLOSED) {
			_state = PanelState.CLOSED;
			_parent.remove(this);
		}
		// only occurs if panel is previously closed
		else if (newState == PanelState.OPEN) {
			_state = PanelState.OPEN;
			((JTabbedPane) _parent).addTab(this.getTitle(), this);
			CytoPanel cytoPanel = _cySwingApp.getCytoPanel(getCytoPanelName());
			if (cytoPanel.getState() == CytoPanelState.HIDE) {
				cytoPanel.setState(CytoPanelState.DOCK);
			}
			setVisible(true);
			// The panel is selected upon clicking PathLinker -> Open
			cytoPanel.setSelectedIndex(cytoPanel.indexOfComponent(getComponent()));
		}

		this.revalidate();
		this.repaint();
	}

	/**
	 * Constructor for the panel Initializes the visual elements in the panel
	 */
	public PathLinkerPanel() {
		initializePanelItems();
	}

	/**
	 * Initializer for the panel to reduce the number of parameters in the
	 * constructor
	 *
	 * @param applicationManager
	 *            application manager
	 * @param networkManager
	 *            network manager
	 * @param networkViewFactory
	 *            network view factory
	 * @param networkViewManager
	 *            network view manager
	 * @param adapter
	 *            the cy application adapter
	 */
	public void initialize(CySwingApplication cySwingApp, CyApplicationManager applicationManager,
			CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory,
			CyNetworkViewManager networkViewManager, CyAppAdapter adapter) {
		_cySwingApp = cySwingApp;
		_applicationManager = applicationManager;
		_networkManager = networkManager;
		_networkViewFactory = networkViewFactory;
		_networkViewManager = networkViewManager;
		_adapter = adapter;
		_parent = this.getParent();
	}
	
	/** Listener for _allowSourcesTargetsInPathsOption and _targetsSameAsSourcesOption */
	class CheckBoxListener implements ItemListener {
		/** Enable/disable the button based on the check boxes */
		@Override
		public void itemStateChanged(ItemEvent e) {
			enableClearButton();
		}
	}
	
	/**
	 * Listener for the source and target text fields in the panel
	 * enable/disable the _clearSourceTargetPanelButton based on the text fields
	 */
	class TextFieldListener implements DocumentListener {
		@Override
		  public void changedUpdate(DocumentEvent e) {
			enableClearButton();
		  }
		@Override
		public void insertUpdate(DocumentEvent e) {
			enableClearButton();
		}
		@Override
		public void removeUpdate(DocumentEvent e) {
			enableClearButton();
		}
	}

	/**
	 * Listener for the select node to source button in the panel
	 * Obtain selected node names from the network, construct a string of them
	 * separate by space, and pass it onto sources text field
	 */
	class LoadNodeToSourceButtonListener implements ActionListener {
		/** Responds to a click of the button */
		@Override
		public void actionPerformed(ActionEvent e) {
			StringBuilder sources = new StringBuilder();
			CyNetwork network = _applicationManager.getCurrentNetwork();
			List<CyNode> nodes = CyTableUtil.getNodesInState(network,"selected",true);
			for (CyNode node : nodes)
				sources.append(network.getRow(node).get(CyNetwork.NAME, String.class) + "\n");

			_sourcesTextField.setText(sources.toString());
		}
	}

	/**
	 * Listener for the select node to target button in the panel
	 * Obtain selected node names from the network, construct a string of them
	 * separate by space, and pass it onto target text field
	 */
	class LoadNodeToTargetButtonListener implements ActionListener {
		/** Responds to a click of the button */
		@Override
		public void actionPerformed(ActionEvent e) {
			StringBuilder targets = new StringBuilder();
			CyNetwork network = _applicationManager.getCurrentNetwork();
			List<CyNode> nodes = CyTableUtil.getNodesInState(network,"selected",true);
			for (CyNode node : nodes)
				targets.append(network.getRow(node).get(CyNetwork.NAME, String.class) + "\n");

			_targetsTextField.setText(targets.toString());
		}
	}
	
	/**
	 * Listener for the clear button in the source target panel
	 * clear all the user inputs inside the source target panel
	 */
	class ClearSourceTargetPanelButtonListener implements ActionListener {
		/** Responds to a click of the button */
		@Override
		public void actionPerformed(ActionEvent e) {
			_sourcesTextField.setText("");
			_targetsTextField.setText("");
			_allowSourcesTargetsInPathsOption.setSelected(false);
			_targetsSameAsSourcesOption.setSelected(false);
		}
	}

	/** Listener for the submit button in the panel */
	class SubmitButtonListener implements ActionListener {
		/**
		 * Responds to a click of the submit button in the pathlinker jpanel
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			prepareAndRunKSP();
		}
	}
	
	/** enables/disable the _clearSourceTargetPanelButton 
	 * based on the source/target text fields and the check boxes
	 */
	private void enableClearButton() {
		if (_sourcesTextField.getText().equals("") && _targetsTextField.getText().equals("") 
				&& !_allowSourcesTargetsInPathsOption.isSelected() && !_targetsSameAsSourcesOption.isSelected())
				_clearSourceTargetPanelButton.setEnabled(false);
		else _clearSourceTargetPanelButton.setEnabled(true);
	}
	
	private String[] updateEdgeWeightColumn() {
		
		if (_originalNetwork == null && _applicationManager == null)
		{
			String[] item = {"No item"};
			return item;
		}
		
		Collection<CyColumn> columns = _originalNetwork == null ? 
				_applicationManager.getCurrentNetwork().getDefaultEdgeTable().getColumns() :
					_originalNetwork.getDefaultEdgeTable().getColumns();
				
		ArrayList<String> items = new ArrayList<String>();
		for (CyColumn column : columns)
			items.add(column.getName());
		
		return items.toArray(new String[items.size()]);
	}

	private void prepareAndRunKSP() {
		showRunningMessage();

		// checks for identical sources/targets option selection to
		// update the panel values
		if (_targetsSameAsSourcesOption.isSelected()) {
			_targetsTextField.setText(_sourcesTextField.getText());
			_allowSourcesTargetsInPathsOption.setSelected(true);
		}

		// this looks extremely stupid, but is very important.
		// due to the multi-threaded nature of the swing gui, if
		// this were simply runKSP() and then hideRunningMessage(), java
		// would assign a thread to the hideRunningMessage and we would
		// never see the "PathLinker is running..." message. By adding
		// the if else we force the program to wait on the result of
		// runKSP and thus peforming these events in the order we want
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (callRunKSP()) {
					hideRunningMessage();
				} else {
					hideRunningMessage();
				}
			}
		});

	}

	private void showRunningMessage() {
		_runningMessage.setVisible(true);
		_runningMessage.setForeground(Color.BLUE);
		add(_runningMessage, BorderLayout.SOUTH);
		repaint();
		revalidate();
	}

	private void hideRunningMessage() {
		remove(_runningMessage);
		repaint();
		revalidate();
	}

	/**
	 * access user inputs to create the model for running ksp algorithm
	 * check user inputs for errors prior running ksp algorithm
	 * generate result panel and/or sub network graph for the user
	 */
	private boolean callRunKSP() {
		boolean success;

		// Check to see if network exists before starting reading the values from the panel
		_originalNetwork = _applicationManager.getCurrentNetwork();
		if (_originalNetwork == null) {
			JOptionPane.showMessageDialog(null, "Network not found. Please load a valid network");
			return false;
		}

		// reads the raw values from the panel and converts them into useful
		readValuesFromPanel();

		// initialize the model from the user inputs
		_model= new PathLinkerModel(_originalNetwork, _allowSourcesTargetsInPathsOption.isSelected(), 
				_includePathScoreTiesOption.isSelected(), _subgraphOption.isSelected(),
				_sourcesTextField.getText(), _targetsTextField.getText(), 
				_kValue, _edgeWeightSetting, _edgePenalty);

		// sets up the source and targets, and check to see if network is construct correctly
		success = _model.prepareIdSourceTarget();

		if (!success)
			return false;

		// check to see if source, targets, and edges are set up correctly
		success = checkSourceTargetEdge();
		if (!success)
			return false;

		// runs the setup and KSP algorithm
		ArrayList<Path> result = _model.runKSP();

		// generates a subgraph of the nodes and edges involved in the resulting
		// paths and displays it to the user
		if (_model.getGenerateSubgraph())
			createKSPSubgraphView();

		// update the table path index attribute
		updatePathIndexAttribute(result);
		
		// writes the result of the algorithm to a table
		writeResult(result);

		return true;
	}

	/**
	 * Check user inputs on source, target, and edge weights
	 * @return true if check passes, otherwise false
	 */
	private boolean checkSourceTargetEdge() {

		// obtain sources and targets from the model
		ArrayList<String> sourcesNotInNet = _model.getSourcesNotInNet();
		ArrayList<String> targetsNotInNet = _model.getTargetsNotInNet();
		ArrayList<CyNode> sources = _model.getSourcesList();
		ArrayList<CyNode> targets = _model.getTargetsList();

		// makes sure that we actually have at least one valid source and target
		if (sources.size() == 0) {
			JOptionPane.showMessageDialog(null, "There are no valid sources to be used. Quitting...");
			return false;
		}
		if (targets.size() == 0) {
			JOptionPane.showMessageDialog(null, "There are no valid targets to be used. Quitting...");
			return false;
		}

		// insert all missing sources/targets to the error message in the beginning
		if (targetsNotInNet.size() > 0) {
			errorMessage.insert(0, "The targets " + targetsNotInNet.toString() + " are not in the network.\n");
		}
		if (sourcesNotInNet.size() > 0) {
			errorMessage.insert(0, "The sources " + sourcesNotInNet.toString() + " are not in the network.\n");
		}

		// edge case where only one source and one target are inputted,
		// so no paths will be found. warn the user
		if (sources.size() == 1 && sources.equals(targets)) {
			JOptionPane.showMessageDialog(null,
					"The only source node is the same as the only target node. PathLinker will not compute any paths. Please add more nodes to the sources or targets.");
		}

		// there is some error, tell the user
		if (errorMessage.length() > 0) {
			errorMessage.append("Continue anyway?");
			int choice = JOptionPane.showConfirmDialog(null, errorMessage.toString());
			if (choice != 0) {
				// quit if they say no or cancel
				return false;
			}
		}

		// checks if all the edges in the graph have weights.
		// if a weighted option was selected, but not all edges have weights
		// then we say something to the user.
		// we do this check for the unweighted option as well so in the case
		// that we have to deal with multi edges, we know whether or not to
		// average the weights or just delete the extra edges (see
		// averageMultiEdges method)
		_originalNetwork = _model.getOriginalNetwork();

		for (CyEdge edge : _originalNetwork.getEdgeList()) {
			Double value = _originalNetwork.getRow(edge).get("edge_weight", Double.class);

			if (value == null) {
				// not all the edges have weights (i.e., at least one of the
				// entries in the table is null)
				_allEdgesContainWeights = false;

				// only want to warn the user about not having all weighted
				// edges if a weighted option is selected
				if (_edgeWeightSetting != EdgeWeightSetting.UNWEIGHTED) {
					JOptionPane.showMessageDialog(null,
							"Weighted option was selected, but there exists at least one edge without a weight. Quitting...");
					return false;
				}
			}
		}

		// successful parsing
		return true;
	}

	/**
	 * Reads in the raw values from the panel and converts them to useful
	 * objects that can be used for the algorithm. Performs error checking on
	 * the values and warns the user
	 */
	private void readValuesFromPanel() {
		// error message to report errors to the user if they occur
		errorMessage = new StringBuilder();

		// parses the value inputted for k
		// if it is an invalid value, uses 200 by default and also appends the
		// error to the error message
		String kInput = _kTextField.getText().trim();
		try {
			_kValue = Integer.parseInt(kInput);
		} catch (NumberFormatException exception) {
			errorMessage.append("Invalid number " + kInput + " entered for k. Using default k=200.\n");
			_kValue = 200;
		}

		// gets the option for edge weight setting
		if (_unweighted.isSelected()) {
			_edgeWeightSetting = EdgeWeightSetting.UNWEIGHTED;
		} else if (_weightedAdditive.isSelected()) {
			_edgeWeightSetting = EdgeWeightSetting.ADDITIVE;
		} else if (_weightedProbabilities.isSelected()) {
			_edgeWeightSetting = EdgeWeightSetting.PROBABILITIES;
		} else {
			errorMessage.append("No option selected for edge weights. Using unweighted as default.\n");
			_edgeWeightSetting = EdgeWeightSetting.UNWEIGHTED;
		}

		// parses the value inputted for edge penalty
		// if it is an invalid value, uses 1.0 by default for multiplicative
		// option or 0.0 by default for additive option and also appends the
		// error to the error message
		String edgePenaltyInput = _edgePenaltyTextField.getText().trim();
		if (edgePenaltyInput.isEmpty()) {
			// nothing was inputted, use the default values for the setting
			if (_edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
				_edgePenalty = 1.0;
			} else if (_edgeWeightSetting == EdgeWeightSetting.ADDITIVE) {
				_edgePenalty = 0.0;
			}
		} else {
			// try to parse the user's input
			try {
				_edgePenalty = Double.parseDouble(edgePenaltyInput);
			} catch (NumberFormatException exception) {
				// invalid number was entered, invoked an exception
				if (_edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
					errorMessage.append("Invalid number " + edgePenaltyInput
							+ " entered for edge penalty. Using default multiplicative edge penalty=1.0\n");
					_edgePenalty = 1.0;
				}

				if (_edgeWeightSetting == EdgeWeightSetting.ADDITIVE) {
					errorMessage.append("Invalid number " + edgePenaltyInput
							+ " entered for edge penalty. Using default additive edge penalty=0\n");
					_edgePenalty = 1.0;
				}
			}

			// valid number was entered, but not valid for the algorithm
			// i.e., negative number
			if (_edgePenalty < 1 && _edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
				errorMessage.append(
						"Invalid number entered for edge penalty with multiplicative option. Edge penalty for multiplicative option must be greater than or equal to 1. Using default penalty=1.0\n");
				_edgePenalty = 1.0;
			}

			if (_edgePenalty < 0 && _edgeWeightSetting == EdgeWeightSetting.ADDITIVE) {
				errorMessage.append(
						"Invalid number entered for edge penalty with additive option. Edge penalty for additive option must be greater than or equal to 0. Using default penalty=0\n");
				_edgePenalty = 0.0;
			}
		}
	}
	
	/**
	 * Creates a path index attribute to the network edge tables
	 * that rank each edge in the newly generated paths according to its weight
	 * @param paths 
	 * 			the sorted paths of the network generated from the algorithm
	 */
	private void updatePathIndexAttribute(ArrayList<Path> paths) {
		// create a new attribute "path index n" in the network edge table, where n is an unique number
		int columnNum = 1;
		while (_originalNetwork.getDefaultEdgeTable().getColumn("path index " + columnNum) != null)
			columnNum++;

		String columnName = "path index " + (columnNum);
		_originalNetwork.getDefaultEdgeTable().createColumn(columnName, Integer.class, false);

		for (int i = 0; i < paths.size(); i++) {
			Path currPath = paths.get(i);

			// excluding supersource and supertarget
			for (int j = 1; j < currPath.size() - 2; j++) {
				CyNode node1 = currPath.get(j);
				CyNode node2 = currPath.get(j + 1);

				// add all of the directed edges from node1 to node2
				List<CyEdge> edges = _originalNetwork.getConnectingEdgeList(node1, node2, CyEdge.Type.DIRECTED);
				for (CyEdge edge : edges)
				{
					if (_originalNetwork.getRow(edge).get(columnName, Integer.class) == null &&
							edge.getSource().equals(node1) && edge.getTarget().equals(node2)) // verifies the edges direction
						_originalNetwork.getRow(edge).set(columnName, i + 1);
				}
				// also add all of the undirected edges from node1 to node2
				edges = _originalNetwork.getConnectingEdgeList(node1, node2, CyEdge.Type.UNDIRECTED);
				for (CyEdge edge : edges) 
					if (_originalNetwork.getRow(edge).get(columnName, Integer.class) == null)
						_originalNetwork.getRow(edge).set(columnName,  i + 1);
			}
		}
	}

	/**
	 * Writes the ksp results to a table given the results from the ksp
	 * algorithm
	 *
	 * @param paths
	 *            a list of paths generated from the ksp algorithm
	 */
	private void writeResult(ArrayList<Path> paths) {
		// delete the copy of the network created for running pathlinker
		_network = null;

		// If no paths were found, then exit with this error
		// TODO This should be done before the empty kspSubgraph is created 
		if (paths.size() == 0) {
			JOptionPane.showMessageDialog(null, "No paths found.");
			return;
		}

		// passed edge weight setting for sorting purposes
		ResultFrame resultFrame = new ResultFrame(_model.getOriginalNetwork(), _model.getEdgeWeightSetting(), paths);
		resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		resultFrame.setVisible(true);
		resultFrame.setSize(500, 700);
	}

	/**
	 * Creates a new network and view for the subgraph
	 */
	private void createKSPSubgraphView() {

		CyNetwork kspSubgraph = _model.getKspSubgraph();

		// creates the new network and its view
		CyNetworkView kspSubgraphView = _networkViewFactory.createNetworkView(kspSubgraph);
		_networkManager.addNetwork(kspSubgraph);
		_networkViewManager.addNetworkView(kspSubgraphView);

		Color targetColor = new Color(255, 223, 0);

		// use a visual bypass to color the sources and targets
		for (CyNode source : _model.getSubgraphSources()) {
			View<CyNode> currView = kspSubgraphView.getNodeView(source);
			currView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.DIAMOND);
			currView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.CYAN);
		}
		for (CyNode target : _model.getSubgraphTargets()) {
			View<CyNode> currView = kspSubgraphView.getNodeView(target);
			currView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.RECTANGLE);
			currView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, targetColor);

		}

		applyLayout(kspSubgraph, kspSubgraphView);
	}

	/**
	 * Applies a layout algorithm to the nodes If k <= 200, we apply a
	 * hierarchical layout Otherwise, we apply the default layout
	 * @param kspSubgraph
	 * @param kspSubgraphView
	 */
	private void applyLayout(CyNetwork kspSubgraph, CyNetworkView kspSubgraphView) {
		boolean hierarchical = _model.getK() <= 200;

		// set node layout by applying the default layout algorithm
		CyLayoutAlgorithm algo = hierarchical ? _adapter.getCyLayoutAlgorithmManager().getLayout("hierarchical")
				: _adapter.getCyLayoutAlgorithmManager().getDefaultLayout();
		TaskIterator iter = algo.createTaskIterator(kspSubgraphView, algo.createLayoutContext(),
				CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		_adapter.getTaskManager().execute(iter);
		SynchronousTaskManager<?> synTaskMan = _adapter.getCyServiceRegistrar()
				.getService(SynchronousTaskManager.class);
		synTaskMan.execute(iter);
		_adapter.getVisualMappingManager().getVisualStyle(kspSubgraphView).apply(kspSubgraphView);
		kspSubgraphView.updateView();

		// if we applied the hierarchical layout, by default it is rendered
		// upside down
		// so we reflect all the nodes about the x axis
		if (hierarchical) {
			// sleep so the hierarchical layout can get applied
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// reflect nodes about the x-axis because the default hierarchical
			// layout renders the nodes upside down
			// reflect nodes
			double maxY = Integer.MIN_VALUE;
			double minY = Integer.MAX_VALUE;

			// finds the midpoint x coordinate
			for (CyNode node : kspSubgraph.getNodeList()) {
				View<CyNode> nodeView = kspSubgraphView.getNodeView(node);
				double yCoord = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);

				if (yCoord > maxY)
					maxY = yCoord;

				if (yCoord < minY)
					minY = yCoord;
			}

			double midY = (maxY + minY) / 2;

			// reflects each node about the midpoint x axis
			for (CyNode node : kspSubgraph.getNodeList()) {
				View<CyNode> nodeView = kspSubgraphView.getNodeView(node);
				double yCoord = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);

				double newY = -1 * yCoord + 2 * midY;
				nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, newY);
			}

			kspSubgraphView.updateView();
		}
	}

	/**
	 * Sets up all the components in the panel
	 */
	private void initializePanelItems() {
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		_sourcesLabel = new JLabel("Sources separated by spaces, e.g., S1 S2 S3");
		_sourcesTextField = new JTextField(20);
		_sourcesTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, _sourcesTextField.getPreferredSize().height));
		_sourcesTextField.getDocument().addDocumentListener(new TextFieldListener());
		_loadNodeToSourceButton = new JButton("Add selected node(s)");
		_loadNodeToSourceButton.setEnabled(false);
		_loadNodeToSourceButton.addActionListener(new LoadNodeToSourceButtonListener());
		
		_targetsLabel = new JLabel("Targets separated by spaces, e.g., T1 T2 T3");
		_targetsTextField = new JTextField(20);
		_targetsTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, _targetsTextField.getPreferredSize().height));
		_targetsTextField.getDocument().addDocumentListener(new TextFieldListener());
		_loadNodeToTargetButton = new JButton("Add selected node(s)");
		_loadNodeToTargetButton.setEnabled(false);
		_loadNodeToTargetButton.addActionListener(new LoadNodeToTargetButtonListener());
		
		_allowSourcesTargetsInPathsOption = new JCheckBox("<html>Allow sources and targets in paths</html>", false);
		_allowSourcesTargetsInPathsOption.addItemListener(new CheckBoxListener());
		_targetsSameAsSourcesOption = new JCheckBox("<html>Targets are identical to sources</html>", false);
		_targetsSameAsSourcesOption.addItemListener(new CheckBoxListener());
		
		_clearSourceTargetPanelButton = new JButton("Clear");
		_clearSourceTargetPanelButton.setEnabled(false);
		_clearSourceTargetPanelButton.addActionListener(new ClearSourceTargetPanelButtonListener());
		
		_kLabel = new JLabel("k (# of paths)");
		_kTextField = new JTextField(7);
		_kTextField.setMaximumSize(_kTextField.getPreferredSize());
		
		_edgePenaltyLabel = new JLabel("Edge penalty");
		_edgePenaltyTextField = new JTextField(7);
		_edgePenaltyTextField.setMaximumSize(_edgePenaltyTextField.getPreferredSize());
		
		
		_edgeWeightColumnLabel = new JLabel("Edge weight column: ");
		_edgeWeightColumnBox = new JComboBox<String>(updateEdgeWeightColumn());
		
		_weightedOptionGroup = new ButtonGroup();
		_unweighted = new JRadioButton(
				"<html><b>Unweighted</b> - PathLinker will compute the k lowest cost paths, where the cost is the number of edges in the path.</html>");
		_weightedAdditive = new JRadioButton(
				"<html><b>Weighted, edge weights are additive</b> - PathLinker will compute the k lowest cost paths, where the cost is the sum of the edge weights.</html>");
		_weightedProbabilities = new JRadioButton(
				"<html><b>Weighted, edge weights are probabilities</b> - PathLinker will compute the k highest cost paths, where the cost is the product of the edge weights.</html>");
		_weightedOptionGroup.add(_unweighted);
		_weightedOptionGroup.add(_weightedAdditive);
		_weightedOptionGroup.add(_weightedProbabilities);
		
		_includePathScoreTiesOption = new JCheckBox("<html>Include more than k paths if the path length/score is equal to the kth path's length/score<html>");
		_subgraphOption = new JCheckBox("<html>Generate a subnetwork of the nodes/edges involved in the k paths</html>",
				true);

		_runningMessage = new JLabel("PathLinker is running...");

		JPanel sourceTargetPanel = new JPanel();
		sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.PAGE_AXIS));
		TitledBorder sourceTargetBorder = BorderFactory.createTitledBorder("Sources/Targets");
		sourceTargetPanel.setBorder(sourceTargetBorder);
		sourceTargetPanel.add(_sourcesLabel);
		sourceTargetPanel.add(_sourcesTextField);
		sourceTargetPanel.add(_loadNodeToSourceButton);
		sourceTargetPanel.add(_targetsLabel);
		sourceTargetPanel.add(_targetsTextField);
		sourceTargetPanel.add(_loadNodeToTargetButton);
		sourceTargetPanel.add(_allowSourcesTargetsInPathsOption);
		sourceTargetPanel.add(_targetsSameAsSourcesOption);
		sourceTargetPanel.add(_clearSourceTargetPanelButton);
		this.add(sourceTargetPanel);

		JPanel kPanel = new JPanel();
		kPanel.setLayout(new BoxLayout(kPanel, BoxLayout.PAGE_AXIS));
		TitledBorder kBorder = BorderFactory.createTitledBorder("Algorithm");
		kPanel.setBorder(kBorder);
		kPanel.add(_kLabel);
		kPanel.add(_kTextField);
		kPanel.add(_edgePenaltyLabel);
		kPanel.add(_edgePenaltyTextField);
		this.add(kPanel);

		JPanel comboBoxPanel = new JPanel();
		comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.X_AXIS));
		comboBoxPanel.add(_edgeWeightColumnLabel);
		comboBoxPanel.add(_edgeWeightColumnBox);
		comboBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		JPanel graphPanel = new JPanel();
		graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.PAGE_AXIS));
		TitledBorder graphBorder = BorderFactory.createTitledBorder("Edge Weights");
		graphPanel.setBorder(graphBorder);
		graphPanel.add(comboBoxPanel);
		graphPanel.add(_unweighted);
		graphPanel.add(_weightedAdditive);
		graphPanel.add(_weightedProbabilities);
		this.add(graphPanel);

		JPanel subgraphPanel = new JPanel();
		subgraphPanel.setLayout(new BoxLayout(subgraphPanel, BoxLayout.PAGE_AXIS));
		TitledBorder subgraphBorder = BorderFactory.createTitledBorder("Output");
		subgraphPanel.setBorder(subgraphBorder);
		subgraphPanel.add(_includePathScoreTiesOption);
		subgraphPanel.add(_subgraphOption);
		this.add(subgraphPanel);

		_submitButton = new JButton("Submit");
		_submitButton.addActionListener(new SubmitButtonListener());
		this.add(_submitButton, BorderLayout.SOUTH);

		_runningMessage.setForeground(Color.BLUE);
		_runningMessage.setVisible(false);

		_unweighted.setSelected(true);
	}

	/**
	 * Sets the edge weight value in the network table
	 */
	private void setNetworkTableWeight(CyEdge e, double weight) {
		_network.getRow(e).set("edge_weight", weight);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getTitle() {
		return "PathLinker";
	}
}
