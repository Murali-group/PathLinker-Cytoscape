package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
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
	private JButton _submitButton;
	private ButtonGroup _weightedOptionGroup;
	private JRadioButton _unweighted;
	private JRadioButton _weightedAdditive;
	private JRadioButton _weightedProbabilities;
	private JCheckBox _subgraphOption;
	private JCheckBox _allowSourcesTargetsInPathsOption;
	private JCheckBox _targetsSameAsSourcesOption;
	private JLabel _runningMessage;

	/** Cytoscape class for network and view management */
	private CySwingApplication _cySwingApp;
	private CyApplicationManager _applicationManager;
	private CyNetworkManager _networkManager;
	private CyNetworkViewFactory _networkViewFactory;
	private CyNetworkViewManager _networkViewManager;
	private CyAppAdapter _adapter;

	/** Edges that we hide from the algorithm */
	private HashSet<CyEdge> _hiddenEdges;
	/** Perform algo unweighted, weighted (probs), or weighted (p-values) */
	private EdgeWeightSetting _edgeWeightSetting;
	/** Parent container of the panel to re add to when we call open */
	private Container _parent;
	/** The network to perform the algorithm on */
	private CyNetwork _network;
	/** A mapping of the name of a node to the actual node object */
	private HashMap<String, CyNode> _idToCyNode;
	/** State of the panel. Initially null b/c it isn't open or closed yet */
	private PanelState _state = null;
	/** Index of the tab in the parent panel */
	private int _tabIndex;

	/** The sources to be used in the algorithm */
	private ArrayList<CyNode> _sources;
	/** The targets to be used in the algorithm */
	private ArrayList<CyNode> _targets;
	/** The k value to be used in the algorithm */
	private int _k;
	/** The value by which to penalize each edge weight */
	private double _edgePenalty;
	/** The super source to call ksp with and removed after the algorithm */
	private CyNode _superSource;
	/** The super target to call ksp with and removed after the algorithm */
	private CyNode _superTarget;
	/** The edges attached to super(source/target) to be removed after ksp */
	private HashSet<CyEdge> _superEdges;
	/** Whether or not to generate a subgraph */
	private boolean _generateSubgraph;
	/** Whether or not to allow sources and targets in paths */
	private boolean _allowSourcesTargetsInPaths;
	/** Number of shared nodes between sources and targets */
	private int _commonSourcesTargets;

	private boolean _allEdgesContainWeights = true;

	private HashSet<String> _sourceNames;
	private HashSet<String> _targetNames;

	private enum EdgeWeightSetting {
		UNWEIGHTED, ADDITIVE, PROBABILITIES
	};

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
				if (runKSP()) {
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
	 * Main driving method for the KSP algorithm Makes all the calls for
	 * preprocessing and displaying the results
	 */
	private boolean runKSP() {
		boolean success;

		// populates a mapping from the name of a node to the actual node object
		// used for converting user input to node objects. populates the map
		// named _idToCyNode. is unsuccessful if there is no network
		success = populateIdToCyNode();
		if (!success)
			return false;

		// reads the raw values from the panel and converts them into useful
		// objects to be used in the algorithms
		success = readValuesFromPanel();
		if (!success)
			return false;

		// treats multiple edges as one edge with a weight of the average of
		// the multiple edges. this is done because pathlinker is not compatible
		// with multigraphs
		averageMultiEdges();

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
		ArrayList<Path> result = Algorithms.ksp(_network, _superSource, _superTarget, _k + _commonSourcesTargets);

		// discard first _commonSourcesTargets paths
		// this is for a temporary hack: when there are n nodes that are both
		// sources and targets,
		// the algorithm will generate paths of length 0 from superSource ->
		// node -> superTarget
		// we don't want these, so we generate k + n paths and discard those n
		// paths
		result.subList(0, _commonSourcesTargets).clear();

		// removes the superSource, superTarget, and edges associated
		// with them
		removeSuperNodes();

		// "un log-transforms" the path scores in the weighted options
		// as to undo the log transformations and leave the path scores
		// in terms of the edge weights
		undoLogTransformPathLength(result);

		// generates a subgraph of the nodes and edges involved in the resulting
		// paths and displays it to the user
		if (_generateSubgraph)
			createKSPSubgraph(result);

		// writes the result of the algorithm to a table
		writeResult(result);

		return true;
	}

	/**
	 * Reads in the raw values from the panel and converts them to useful
	 * objects that can be used for the algorithm. Performs error checking on
	 * the values and warns the user if it is a minor error or quits if there
	 * are any major errors.
	 *
	 * @return true if the parsing was successful, false otherwise
	 */
	private boolean readValuesFromPanel() {
		// error message to report errors to the user if they occur
		StringBuilder errorMessage = new StringBuilder();

		// set boolean for allowing sources/targets in paths
		_allowSourcesTargetsInPaths = _allowSourcesTargetsInPathsOption.isSelected();

		// grabs the values in the source and target text fields
		String sourcesTextFieldValue = _sourcesTextField.getText();
		String targetsTextFieldValue = _targetsTextField.getText();

		// splits the names by spaces
		String[] rawSourceNames = sourcesTextFieldValue.split(" ");
		String[] rawTargetNames = targetsTextFieldValue.split(" ");

		_sourceNames = new HashSet<String>(Arrays.asList(rawSourceNames));
		_targetNames = new HashSet<String>(Arrays.asList(rawTargetNames));

		// stores the sources/targets that were inputted but are not actually in
		// the network, may have been mistyped
		ArrayList<String> sourcesNotInNet = new ArrayList<String>();
		ArrayList<String> targetsNotInNet = new ArrayList<String>();

		// checks for mistyped source/target names
		for (String sourceName : _sourceNames) {
			if (!_idToCyNode.containsKey(sourceName))
				sourcesNotInNet.add(sourceName);
		}
		for (String targetName : _targetNames) {
			if (!_idToCyNode.containsKey(targetName))
				targetsNotInNet.add(targetName);
		}

		// generates a list of the valid source/target nodes to be used in
		// the graph
		_sourceNames.removeAll(sourcesNotInNet);
		_targetNames.removeAll(targetsNotInNet);
		_sources = stringsToNodes(_sourceNames);
		_targets = stringsToNodes(_targetNames);

		// makes sure that we actually have at least one valid source and target
		if (_sources.size() == 0) {
			JOptionPane.showMessageDialog(null, "There are no valid sources to be used. Quitting...");
			return false;
		}
		if (_targets.size() == 0) {
			JOptionPane.showMessageDialog(null, "There are no valid targets to be used. Quitting...");
			return false;
		}

		// appends all missing sources/targets to the error message
		if (sourcesNotInNet.size() + targetsNotInNet.size() > 0) {
			if (sourcesNotInNet.size() > 0) {
				errorMessage.append("The sources " + sourcesNotInNet.toString() + " are not in the network.\n");
			}
			if (targetsNotInNet.size() > 0) {
				errorMessage.append("The targets " + targetsNotInNet.toString() + " are not in the network.\n");
			}
		}

		// edge case where only one source and one target are inputted,
		// so no paths will be found. warn the user
		if (_sources.size() == 1 && _sources.equals(_targets)) {
			JOptionPane.showMessageDialog(null,
					"The only source node is the same as the only target node. PathLinker will not compute any paths. Please add more nodes to the sources or targets.");
		}

		// sets the number of common sources and targets
		// this is for a temporary hack: when there are n nodes that are both
		// sources and targets,
		// the algorithm will generate paths of length 0 from superSource ->
		// node -> superTarget
		// we don't want these, so we generate k + n paths and discard those n
		// paths
		_commonSourcesTargets = 0;
		Set<CyNode> targetSet = new HashSet<CyNode>(_targets);
		for (CyNode source : _sources) {
			if (targetSet.contains(source))
				_commonSourcesTargets++;
		}

		// parses the value inputted for k
		// if it is an invalid value, uses 200 by default and also appends the
		// error to the error message
		String kInput = _kTextField.getText().trim();
		try {
			_k = Integer.parseInt(kInput);
		} catch (NumberFormatException exception) {
			errorMessage.append("Invalid number " + kInput + " entered for k. Using default k=200.\n");
			_k = 200;
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
				_edgePenalty = 0.;
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
					_edgePenalty = 0;
				}
			}

			// valid number was entered, but not valid for the algorithm
			// i.e., negative number
			if (_edgePenalty <= 0 && _edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
				errorMessage.append(
						"Invalid number entered for edge penalty with multiplicative option. Edge penalty for multiplicative option must be greater than 0. Using default penalty=1.0\n");
				_edgePenalty = 1.0;
			}

			if (_edgePenalty < 0 && _edgeWeightSetting == EdgeWeightSetting.ADDITIVE) {
				errorMessage.append(
						"Invalid number entered for edge penalty with additive option. Edge penalty for additive option must be greater than or equal to 0. Using default penalty=0\n");
				_edgePenalty = 0;
			}
		}

		_generateSubgraph = _subgraphOption.isSelected();

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
		for (CyEdge edge : _network.getEdgeList()) {
			Double value = _network.getRow(edge).get("edge_weight", Double.class);

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
	 * Treats multiple edges as one edge with a weight of the average of the
	 * multiple edges. This is done because pathlinker is not compatible with
	 * multigraphs
	 */
	private void averageMultiEdges() {
		// maps one edge to all the other edges connecting the same
		// source/target pair so we can go through afterwards and remove
		// the extra edges
		HashMap<CyEdge, List<CyEdge>> edgeToMulti = new HashMap<CyEdge, List<CyEdge>>();

		// keeps track of edges we've already visited so we don't
		// store multiple (edge:extra) entries
		HashSet<CyEdge> seenEdges = new HashSet<CyEdge>();

		// populates the edgeToMulti hash map
		for (CyEdge e : _network.getEdgeList()) {
			// don't want to have multiple (edge:extra) entries
			if (seenEdges.contains(e))
				continue;

			CyNode eSource = e.getSource();
			CyNode eTarget = e.getTarget();

			// stores all edges except e, all the "extra" edges
			List<CyEdge> extraEdges = new ArrayList<CyEdge>();
			// have to go through and filter the edges that connect
			// src->target, because the getConnectingEdgeList method gives us
			// any edges between source and target
			for (CyEdge potentialExtraEdge : _network.getConnectingEdgeList(e.getSource(), e.getTarget(),
					CyEdge.Type.DIRECTED)) {
				// e doesn't count as an extra edge; it is the single edge
				// that we want left
				if (potentialExtraEdge.equals(e))
					continue;

				// if the e is undirected, we don't want to lose a directed version of it 
				// because only one direction of an undirected edge is used in our implementation of ksp
				if (e.isDirected())
				{
					// verifies the edges direction
					if (potentialExtraEdge.getSource().equals(eSource) && potentialExtraEdge.getTarget().equals(eTarget)) {
						extraEdges.add(potentialExtraEdge);
					}
				}
			}
			// Also consider the undirected edges
			for (CyEdge potentialExtraEdge : _network.getConnectingEdgeList(e.getSource(), e.getTarget(),
					CyEdge.Type.UNDIRECTED)) {
				// e doesn't count as an extra edge; it is the single edge
				// that we want left
				if (potentialExtraEdge.equals(e))
					continue;
				// if e is a directed edge (for example A->B), 
				// then don't remove the undirected edge 
				// because we would lose the B->A connection 
				if (e.isDirected())
					continue;

				extraEdges.add(potentialExtraEdge);
			}

			// marks all edges as dealt with so we don't duplicate entries
			// in the map. i.e. if we have edges A,B,C, we only want
			// A: {B,C}. we don't want the other entries B: {C,A} and C: {A,B}
			seenEdges.add(e);
			seenEdges.addAll(extraEdges);

			// no extra edges, we don't do anything
			if (extraEdges.size() == 0)
				continue;

			// adds the (edge:extra) entry to the map
			edgeToMulti.put(e, extraEdges);
		}

		// root network for removing nodes and edges
		CyRootNetwork root = ((CySubNetwork) _network).getRootNetwork();

		// for every (edge:extra) pair, we set edge's weight to the average
		// of all the edges, and then remove the extra edges from the network
		for (CyEdge e : edgeToMulti.keySet()) {
			// if not all the edges contain weights, no point in averaging the
			// edge weights, because we'll hit null entries in the edge table.
			// so we just remove the extra edges
			if (!_allEdgesContainWeights) {
				root.removeEdges(edgeToMulti.get(e));
			}
			// all the edges contain weights, so we should average the edge
			// weights and transform this graph into one without multi edges
			// regardless if it's unweighted or not. if the unweighted option
			// is selected, it'll just treat that single edge as weight 1 anyway
			else {
				double sumWeight = getNetworkTableWeight(e);
				int edgeCount = 1;

				List<CyEdge> extraEdges = edgeToMulti.get(e);
				for (CyEdge extraEdge : extraEdges) {
					sumWeight += getNetworkTableWeight(extraEdge);
					edgeCount++;
				}

				// averages the weights
				double averageWeight = sumWeight / edgeCount;

				// stores the new average weight as the table weight for e
				setNetworkTableWeight(e, averageWeight);

				// removes the extra edges from the graph that aren't e
				root.removeEdges(extraEdges);
			}
		}
	}

	/**
	 * Initializes the edges that we are hiding from the algorithm. Doesn't
	 * actually remove the edges as that dominates runtime.
	 */
	private void initializeHiddenEdges() {
		_hiddenEdges = new HashSet<CyEdge>();

		// only if we don't allow sources and targets internal to paths
		if (!_allowSourcesTargetsInPaths) {
			// hides all incoming directed edges to source nodes
			for (CyNode source : _sources) {
				_hiddenEdges.addAll(_network.getAdjacentEdgeList(source, CyEdge.Type.INCOMING));
			}
			// hides all outgoing directed edges from target nodes
			for (CyNode target : _targets) {
				_hiddenEdges.addAll(_network.getAdjacentEdgeList(target, CyEdge.Type.OUTGOING));
			}
		}

		Algorithms.initializeHiddenEdges(_hiddenEdges);
	}

	/**
	 * Sets the edge weights to be used in the algorithm. Doesn't actually set
	 * the weights as attributes because that dominates runtime.
	 */
	private void setEdgeWeights() {
		HashMap<CyEdge, Double> edgeWeights = new HashMap<CyEdge, Double>();

		for (CyEdge edge : _network.getEdgeList()) {
			// gets the attribute edge weight value
			Double value = _network.getRow(edge).get("edge_weight", Double.class);
			double edge_weight = value != null ? value.doubleValue() : -44444;

			if (_edgeWeightSetting == EdgeWeightSetting.UNWEIGHTED) {
				edgeWeights.put(edge, 1.);
			} else if (_edgeWeightSetting == EdgeWeightSetting.ADDITIVE) {
				edgeWeights.put(edge, edge_weight);
			} else if (_edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
				edgeWeights.put(edge, edge_weight);
			}
		}

		// applies edge penalty and then log transforms the edge weights for the
		// probability option
		if (_edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
			applyMultiplicativeEdgePenalty(edgeWeights, _edgePenalty);
			logTransformEdgeWeights(edgeWeights);
		}

		// applies edge penalty for the additive option
		if (_edgeWeightSetting == EdgeWeightSetting.ADDITIVE) {
			applyAdditiveEdgePenalty(edgeWeights, _edgePenalty);
		}

		// sets the weights in the algorithms class
		Algorithms.setEdgeWeights(edgeWeights);
	}

	/**
	 * Applies the user specified edge penalty for the multiplicative option.
	 * This weight penalizes the score of every path by a factor equal to (the
	 * number of edges in the path)^(this factor). This was previously done in
	 * the logTransformEdgeWeights method with a parameter weight=(sum of all
	 * edge weights). In the "standard" PathLinker case, this was necessary to
	 * account for the probability that is lost when edges (incoming source or
	 * outgoing target) are removed, along with probability lost to zero degree
	 * nodes in the edge flux calculation.
	 *
	 * @param weights
	 *            the map from edges to their weights
	 * @param edgePenalty
	 *            the penalty to apply to each edge
	 */
	private void applyMultiplicativeEdgePenalty(HashMap<CyEdge, Double> weights, double edgePenalty) {
		if (edgePenalty == 1.0)
			return;

		for (CyEdge edge : weights.keySet()) {
			if (_hiddenEdges.contains(edge))
				continue;

			double edgeWeight = weights.get(edge);
			double w = edgeWeight / edgePenalty;
			weights.put(edge, w);
		}
	}

	/**
	 * Applies the user specified edge penalty for the additive option. This
	 * weight penalizes the score of every path by a factor equal to (the number
	 * of edges in the path)*(this factor).
	 *
	 * @param weights
	 *            the map from edges to their weights
	 * @param edgePenalty
	 *            the penalty to apply to each edge
	 */
	private void applyAdditiveEdgePenalty(HashMap<CyEdge, Double> weights, double edgePenalty) {
		if (edgePenalty == 0)
			return;

		for (CyEdge edge : weights.keySet()) {
			if (_hiddenEdges.contains(edge))
				continue;

			double edgeWeight = weights.get(edge);
			double w = edgeWeight + edgePenalty;
			weights.put(edge, w);
		}
	}

	/**
	 * Performs a log transformation on the supplied edges in place given a
	 * mapping from edges to their initial weights
	 *
	 * @param weights
	 *            the mapping from edges to their initial weights
	 */
	private void logTransformEdgeWeights(HashMap<CyEdge, Double> weights) {
		for (CyEdge edge : weights.keySet()) {
			if (_hiddenEdges.contains(edge))
				continue;

			double edgeWeight = weights.get(edge);

			// double w = -1 * Math.log(edge_weight);
			double w = -1 * Math.log(Math.max(0.000000001, edgeWeight)) / Math.log(10);
			weights.put(edge, w);
		}
	}

	/**
	 * Adds a superSource and superTarget and attaches them to the sources and
	 * targets, respectively. Sets _superSource, _superTarget, and populates the
	 * list _superEdges, so they can be removed later.
	 */
	private void addSuperNodes() {
		// sets up the super source/super target
		_superSource = _network.addNode();
		_superTarget = _network.addNode();
		_superEdges = new HashSet<CyEdge>();

		// attaches super source to all sources
		for (CyNode source : _sources) {
			CyEdge superEdge = _network.addEdge(_superSource, source, true);

			// sets an edge weight of 0, so the edges connecting the super nodes
			// and the sources/targets don't affect the final path weights
			Algorithms.setWeight(superEdge, 0.);
			_superEdges.add(superEdge);
		}
		// attaches all targets to super target
		for (CyNode target : _targets) {
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
	private void removeSuperNodes() {
		// removes node entries from the default node table
		CyRootNetwork root = ((CySubNetwork) _network).getRootNetwork();
		root.removeNodes(Arrays.asList(_superSource, _superTarget));
		root.removeEdges(_superEdges);
	}

	/**
	 * "un log-transforms" the path scores in the weighted options to undo the
	 * log transformations and leave the path scores in terms of the original
	 * edge weights
	 *
	 * @param paths
	 *            the list of paths from the ksp algorithm
	 */
	private void undoLogTransformPathLength(ArrayList<Path> paths) {
		// weighted probabilities option sets the weight to 2 ^ (-weight)
		if (_edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
			for (Path p : paths) {
				p.weight = Math.pow(10, -1 * p.weight);
			}
		}

		// don't have to do anything for unweighted or additive option
	}

	/**
	 * Writes the ksp results to a table given the results from the ksp
	 * algorithm
	 *
	 * @param paths
	 *            a list of paths generated from the ksp algorithm
	 */
	private void writeResult(ArrayList<Path> paths) {
		if (paths.size() == 0) {
			JOptionPane.showMessageDialog(null, "No paths found.");
			return;
		}

		ResultFrame resultFrame = new ResultFrame(_network, paths);
		resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		resultFrame.setVisible(true);
		resultFrame.setSize(500, 700);
	}

	/**
	 * Generates a subgraph of the user supplied graph that contains only the
	 * nodes and edges that are in the k shortest paths
	 *
	 * @param paths
	 *            the list of paths generated by ksp algorithm
	 */
	private void createKSPSubgraph(ArrayList<Path> paths) {
		// creates a new network in the same network collection
		// as the original network
		CyRootNetwork root = ((CySubNetwork) _network).getRootNetwork();
		
		HashSet<CyNode> nodesToAdd = new HashSet<CyNode>();
		HashSet<CyEdge> edgesToAdd = new HashSet<CyEdge>();

		// keeps track of sources/targets in the ksp subgraph
		// to change their visual properties later
		HashSet<CyNode> sources = new HashSet<CyNode>();
		HashSet<CyNode> targets = new HashSet<CyNode>();
		
		for (Path currPath : paths) {
			// excluding supersource and supertarget
			for (int i = 1; i < currPath.size() - 2; i++) {
				CyNode node1 = currPath.get(i);
				CyNode node2 = currPath.get(i + 1);
				nodesToAdd.add(node1);
				nodesToAdd.add(node2);

				// check if the nodes are part of the sources or targets specified
				String node1name = _network.getRow(node1).get(CyNetwork.NAME, String.class);
				String node2name = _network.getRow(node2).get(CyNetwork.NAME, String.class);
				if (_sourceNames.contains(node1name))
					sources.add(node1);
				if (_targetNames.contains(node2name))
					targets.add(node2);

				// add all of the directed edges from node1 to node2
				List<CyEdge> edges = _network.getConnectingEdgeList(node1, node2, CyEdge.Type.DIRECTED);
				for (CyEdge edge : edges){
					// verifies the edges direction
					if (edge.getSource().equals(node1) && edge.getTarget().equals(node2))
						edgesToAdd.add(edge);
				}
				// also add all of the undirected edges from node1 to node2
				edgesToAdd.addAll(_network.getConnectingEdgeList(node1, node2, CyEdge.Type.UNDIRECTED));


			}
		}
		CyNetwork kspSubgraph = root.addSubNetwork(nodesToAdd, edgesToAdd);

		// sets the network name
		String subgraphName = "PathLinker-subnetwork-" + _k + "-paths";
		kspSubgraph.getRow(kspSubgraph).set(CyNetwork.NAME, subgraphName);
		
		// creates the new network and its view
		CyNetworkView kspSubgraphView = _networkViewFactory.createNetworkView(kspSubgraph);
		_networkManager.addNetwork(kspSubgraph);
		_networkViewManager.addNetworkView(kspSubgraphView);

		Color targetColor = new Color(255, 223, 0);

		// use a visual bypass to color the sources and targets
		for (CyNode source : sources) {
			View<CyNode> currView = kspSubgraphView.getNodeView(source);
			currView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.DIAMOND);
			currView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.CYAN);
		}
		for (CyNode target : targets) {
			View<CyNode> currView = kspSubgraphView.getNodeView(target);
			currView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.RECTANGLE);
			currView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, targetColor);

		}

		applyLayout(kspSubgraph, kspSubgraphView);
	}

	/**
	 * Applies a layout algorithm to the nodes If k <= 200, we apply a
	 * hierarchical layout Otherwise, we apply the default layout
	 * 
	 * @param kspSubgraphView
	 */
	private void applyLayout(CyNetwork kspSubgraph, CyNetworkView kspSubgraphView) {
		boolean hierarchical = _k <= 200;

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
	 * Converts an array of node names to a list of the actual corresponding
	 * nodes
	 *
	 * @param names
	 *            the names of the nodes that we want
	 * @return a list of the actual node objects with the given names
	 */
	private ArrayList<CyNode> stringsToNodes(HashSet<String> names) {
		ArrayList<CyNode> nodes = new ArrayList<CyNode>();

		for (String name : names) {
			if (_idToCyNode.containsKey(name)) {
				nodes.add(_idToCyNode.get(name));
			}
		}

		return nodes;
	}

	/**
	 * Populates idToCyNode, the map of node names to their objects
	 */
	private boolean populateIdToCyNode() {
		_network = _applicationManager.getCurrentNetwork();
		_idToCyNode = new HashMap<String, CyNode>();

		if (_network == null) {
			JOptionPane.showMessageDialog(null,
					"No current network. PathLinker cannot run without a network. Exiting...");
			return false;
		}

		for (CyNode node : _network.getNodeList()) {
			String nodeName = _network.getRow(node).get(CyNetwork.NAME, String.class);
			_idToCyNode.put(nodeName, node);
		}

		return true;
	}

	/**
	 * Sets up all the components in the panel
	 */
	private void initializePanelItems() {
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		_sourcesLabel = new JLabel("Sources separated by spaces, e.g., S1 S2 S3");
		_sourcesTextField = new JTextField(20);
		_sourcesTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, _sourcesTextField.getPreferredSize().height));

		_targetsLabel = new JLabel("Targets separated by spaces, e.g., T1 T2 T3");
		_targetsTextField = new JTextField(20);
		_targetsTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, _targetsTextField.getPreferredSize().height));

		_allowSourcesTargetsInPathsOption = new JCheckBox("<html>Allow sources and targets in paths</html>", false);
		_targetsSameAsSourcesOption = new JCheckBox("<html>Targets are identical to sources</html>", false);

		_kLabel = new JLabel("k (# of paths)");
		_kTextField = new JTextField(7);
		_kTextField.setMaximumSize(_kTextField.getPreferredSize());

		_edgePenaltyLabel = new JLabel("Edge penalty");
		_edgePenaltyTextField = new JTextField(7);
		_edgePenaltyTextField.setMaximumSize(_edgePenaltyTextField.getPreferredSize());

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

		_subgraphOption = new JCheckBox("<html>Generate a subnetwork of the nodes/edges involved in the k paths</html>",
				true);

		_runningMessage = new JLabel("PathLinker is running...");

		JPanel sourceTargetPanel = new JPanel();
		sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.PAGE_AXIS));
		TitledBorder sourceTargetBorder = BorderFactory.createTitledBorder("Sources/Targets");
		sourceTargetPanel.setBorder(sourceTargetBorder);
		sourceTargetPanel.add(_sourcesLabel);
		sourceTargetPanel.add(_sourcesTextField);
		sourceTargetPanel.add(_targetsLabel);
		sourceTargetPanel.add(_targetsTextField);
		sourceTargetPanel.add(_allowSourcesTargetsInPathsOption);
		sourceTargetPanel.add(_targetsSameAsSourcesOption);
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

		JPanel graphPanel = new JPanel();
		graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.PAGE_AXIS));
		TitledBorder graphBorder = BorderFactory.createTitledBorder("Edge Weights");
		graphPanel.setBorder(graphBorder);
		graphPanel.add(_unweighted);
		graphPanel.add(_weightedAdditive);
		graphPanel.add(_weightedProbabilities);
		this.add(graphPanel);

		JPanel subgraphPanel = new JPanel();
		subgraphPanel.setLayout(new BoxLayout(subgraphPanel, BoxLayout.PAGE_AXIS));
		TitledBorder subgraphBorder = BorderFactory.createTitledBorder("Output");
		subgraphPanel.setBorder(subgraphBorder);
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
	 * Gets the edge weight value from the network table. Expensive operation,
	 * so we try to minimize how often we use this
	 */
	private double getNetworkTableWeight(CyEdge e) {
		// gets the attribute edge weight value
		Double value = _network.getRow(e).get("edge_weight", Double.class);
		double edge_weight = value != null ? value.doubleValue() : -44444;

		return edge_weight;
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
