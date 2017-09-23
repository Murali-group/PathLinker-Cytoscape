package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
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
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

/** Panel for the PathLinker plugin */
public class PathLinkerControlPanel extends JPanel implements CytoPanelComponent {
	/** UI components of the panel */
	private JPanel _titlePanel;
	private JPanel _sourceTargetPanel;
	private JPanel _algorithmPanel;
	private JPanel _graphPanel;

	private JLabel _logoLabel;
	private JLabel _titleLabel;
	private JLabel _sourcesLabel;
	private JLabel _targetsLabel;
	private JLabel _kLabel;
	private JLabel _edgePenaltyLabel;
	private JLabel _edgeWeightColumnBoxLabel;
	// private JLabel _runningMessage;

	private HintTextField _sourcesTextField;
	private HintTextField _targetsTextField;
	private JTextField _kTextField;
	private static JTextField _edgePenaltyTextField;

	private JButton _helpBtn;
	private JButton _aboutBtn;
	protected static JButton _loadNodeToSourceButton;
	protected static JButton _loadNodeToTargetButton;
	private JButton _clearSourceTargetPanelButton;
	private JButton _submitButton;
	private JButton _closeButton;

	protected static JComboBox<String> _edgeWeightColumnBox;
	private static ButtonGroup _weightedOptionGroup;
	private static JRadioButton _unweighted;
	private static JRadioButton _weightedAdditive;
	private static JRadioButton _weightedProbabilities;

	private JCheckBox _allowSourcesTargetsInPathsOption;
	private JCheckBox _targetsSameAsSourcesOption;
	private JCheckBox _includePathScoreTiesOption;

	private CyServiceRegistrar _serviceRegistrar;

	/** Cytoscape class for network and view management */
	private CySwingApplication _cySwingApp;
	protected static CyApplicationManager _applicationManager;
	private CyNetworkManager _networkManager;
	private CyAppAdapter _adapter;

	/** The model that runs ksp algorithm from the user input */
	private PathLinkerModel _model;
    /** The about dialog box object */
    private PathLinkerAboutMenuDialog _aboutMenuDialog;
    /** the version of the current PathLinker app */
    private String _version;
    /** the build date of the current PathLinker app */
    private String _buildDate;
	/** Parent container of the panel to re add to when we call open */
	private Container _parent;
	/** State of the panel. Initially null b/c it isn't open or closed yet */
	private PanelState _state = null;
	/** The original network selected by the user */
	private CyNetwork _originalNetwork;
	/** The sub-network created by the algorithm */
	private CyNetwork _kspSubgraph = null;
	/** The sub-network view created by the algorithm */
	private CyNetworkView _kspSubgraphView = null;
	/** column name that links to the edge weight values */
	private String _edgeWeightColumnName;
	/** The k value to be used in the algorithm */
	private int _kValue;
	/** Perform algo unweighted, weighted (probs), or weighted (p-values) */
	private EdgeWeightSetting _edgeWeightSetting;
	/** The value by which to penalize each edge weight */
	private double _edgePenalty;
	/** The string representation of the edge weight button selection user selected */
	private static String _savedEdgeWeightSelection;
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
	 * @param newState the new state
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
		    String[] options = {"Yes", "Cancel"};
		    int choice = JOptionPane.showOptionDialog(null, "Do you want to exit the PathLinker?", 
		            "Warning", 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (choice != 0) return; // quit if they say cancel
			
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
	 * Default constructor for the panel
	 */
	public PathLinkerControlPanel() {
	}

	/**
	 * Initializer for the panel to reduce the number of parameters in the
	 * constructor
	 *
	 * @param applicationManager
	 *            application manager
	 * @param networkManager
	 *            network manager
	 * @param networkViewManager
	 *            network view manager
	 * @param adapter
	 *            the cy application adapter
	 */
	public void initialize(CySwingApplication cySwingApp, CyServiceRegistrar serviceRegistrar,
			CyApplicationManager applicationManager, CyNetworkManager networkManager, CyAppAdapter adapter,
			String version, String buildDate) {
		_cySwingApp = cySwingApp;
		_serviceRegistrar  = serviceRegistrar;
		_applicationManager = applicationManager;
		_networkManager = networkManager;
		_adapter = adapter;
		_version = version;
		_buildDate = buildDate;
		_parent = this.getParent();

		initializeControlPanel(); // construct the GUI
	}

	/** Listener for _allowSourcesTargetsInPathsOption and _targetsSameAsSourcesOption */
	class CheckBoxListener implements ItemListener {
		/** Enable/disable the button based on the check boxes */
		@Override
		public void itemStateChanged(ItemEvent e) {
			enableClearButton();
		}
	}

	/** Listener for the edge weight radio buttons */
	class RadioButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			updateEdgeWeightColumn();
			updateEdgePenaltyTextField();
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

			String sourceText = _sourcesTextField.getText();

			if (_sourcesTextField.showingHint)
			    _sourcesTextField.gainFocus();

			if (sourceText.length() > 0 && sourceText.charAt(_sourcesTextField.getText().length() - 1) != ' ')
				_sourcesTextField.setText(sourceText + " " + sources.toString());
			else _sourcesTextField.setText(sourceText + sources.toString());

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

			String targetText = _targetsTextField.getText();

	         if (_targetsTextField.showingHint)
	             _targetsTextField.gainFocus();

			if (targetText.length() > 0 &&
					targetText.charAt(targetText.length() - 1) != ' ')
				_targetsTextField.setText(targetText + " " + targets.toString());
			else _targetsTextField.setText(targetText + targets.toString());
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
			
			// gain focus to trigger shadow hint behavior
			_sourcesTextField.loseFocus();
			_targetsTextField.loseFocus();
			
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

	/**
	 * construct/update the combo box items for selecting edge weight
	 * Called by PathLinkerNetworkEventListener and PathLinkerColumnUpdateListener class if event triggered
	 */
	protected static void updateEdgeWeightColumn() {

		_edgeWeightColumnBox.removeAllItems(); //remove all items for update

		//keep box empty if no network found or user selected unweighted as edge weight setting
		if (_applicationManager == null || _applicationManager.getCurrentNetwork() == null || _unweighted.isSelected()) {
			_edgeWeightColumnBox.setEnabled(false); //disables the drop-down box
			return;
		}

		_edgeWeightColumnBox.setEnabled(true);

		Collection<CyColumn> columns = _applicationManager.getCurrentNetwork().getDefaultEdgeTable().getColumns();	
		for (CyColumn column : columns) {
			if (!column.getName().equals(CyNetwork.SUID) && (column.getType() == Double.class 
					|| column.getType() == Integer.class || column.getType() == Float.class 
					|| column.getType() == Long.class))
				_edgeWeightColumnBox.addItem(column.getName());		
		}
	}

	/**
	 * update the edge penalty text field depending on user's selection edge weight radio button
	 * Called by RadioButtonListener class if user selects a radio button
	 */
	private static void updateEdgePenaltyTextField() {

		// if user clicks on the same button that is previously selected then do nothing
		if (_savedEdgeWeightSelection.equals(_weightedOptionGroup.getSelection().getActionCommand()))
			return;

		// saves user's new selection to the string
		_savedEdgeWeightSelection = _weightedOptionGroup.getSelection().getActionCommand();

		// clear and disable the edge penalty text field if user selects unweighted option, otherwise enable the text field
		// if select weighted additive -> default is set to 0
		// if select weighted probabilities -> default is set to 1
		if (_unweighted.isSelected()) {
			_edgePenaltyTextField.setText("");
			_edgePenaltyTextField.setEditable(false);

			_savedEdgeWeightSelection = "unweighted";
		}
		else {
			_edgePenaltyTextField.setEditable(true);

			if (_weightedAdditive.isSelected())
				_edgePenaltyTextField.setText("0");
			else
				_edgePenaltyTextField.setText("1");
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

	/**
	 * RunningMessage and related methods are now temporarily removed from the PathLinker 
	 * as new layout doesn't support dynamically adding components
	 * 
	 * Will be removed after alternative is found
	 */
	private void prepareAndRunKSP() {
		// showRunningMessage();

		// checks for identical sources/targets option selection to
		// update the panel values
		if (_targetsSameAsSourcesOption.isSelected()) {
		    
		    // ensure text field is not in shadow text mode
	          if (_targetsTextField.showingHint)
	              _targetsTextField.gainFocus();
		    
			_targetsTextField.setText(_sourcesTextField.getText());
			_allowSourcesTargetsInPathsOption.setSelected(true);
		}

		callRunKSP();

		/*		// this looks extremely stupid, but is very important.
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
		});*/

	}

	/*	private void showRunningMessage() {
		_runningMessage.setVisible(true);
		_runningMessage.setForeground(Color.BLUE);

		repaint();
		revalidate();
	}

	private void hideRunningMessage() {
		_runningMessage.setVisible(false);
		repaint();
		revalidate();
	}*/

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
            JOptionPane.showMessageDialog(null, 
                    "Network not found. Please load a valid network", 
                    "Error Message", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		// reads the raw values from the panel and converts them into useful
		readValuesFromPanel();

		// initialize the model from the user inputs
		_model= new PathLinkerModel(_originalNetwork, _allowSourcesTargetsInPathsOption.isSelected(), 
				_includePathScoreTiesOption.isSelected(), _sourcesTextField.getText().trim(), 
				_targetsTextField.getText().trim(), _edgeWeightColumnName, 
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

		// generates a subgraph of the nodes and edges involved in the resulting paths and displays it to the user
		createKSPSubgraphAndView();

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
	          JOptionPane.showMessageDialog(null, 
	                  "There are no valid sources to be used. Quitting...", 
	                  "Error Message", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (targets.size() == 0) {
            JOptionPane.showMessageDialog(null, 
                    "There are no valid targets to be used. Quitting...", 
                    "Error Message", JOptionPane.ERROR_MESSAGE);
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
                    "The only source node is the same as the only target node.\n"
                    + "PathLinker will not compute any paths. Please add more nodes to the sources or targets.", 
                    "Warning", JOptionPane.WARNING_MESSAGE);
		}

		// there is some error, tell the user
		if (errorMessage.length() > 0) {
		    errorMessage.append("Continue anyway?");
		    
		    String[] options = {"Yes", "Cancel"};
		    
		    int choice = JOptionPane.showOptionDialog(null, errorMessage.toString(), 
                    "Warning", 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		    
		    if (choice != 0) // quit if they say cancel
		        return false;
		}
		

		// checks if all the edges in the graph have weights. Skip the check if edge weight setting is unweighted
		// if a weighted option was selected, but not all edges have weights
		// then we say something to the user.
		if (_edgeWeightSetting == EdgeWeightSetting.UNWEIGHTED) return true;

		_originalNetwork = _model.getOriginalNetwork();
		for (CyEdge edge : _originalNetwork.getEdgeList()) {
			try {
				Double.parseDouble(_originalNetwork.getRow(edge).getRaw(_edgeWeightColumnName).toString());
			} catch (NullPointerException  e) {
		           JOptionPane.showMessageDialog(null, 
		                   "Weighted option was selected, but there exists at least one edge without a weight. Quitting...", 
		                   "Error Message", JOptionPane.ERROR_MESSAGE);
				return false;
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

		// set _edgeWeightColumnName to empty string if no item is selected in _edgeWeightColumnBox
		_edgeWeightColumnName = _edgeWeightColumnBox.getSelectedIndex() == -1 ? "" : _edgeWeightColumnBox.getSelectedItem().toString();
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
	 * Writes the ksp results to result panel given the results from the ksp algorithm
	 * @param paths a list of paths generated from the ksp algorithm
	 */
	private void writeResult(ArrayList<Path> paths) {
		// If no paths were found, then exit with this error
		// TODO This should be done before the empty kspSubgraph is created 
		if (paths.size() == 0) {
            JOptionPane.showMessageDialog(null, 
                    "No paths found", 
                    "Error Message", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// create and register a new panel in result panel with specific title
		// if user did not generate sub-network then we pass down the original network to the result panel
		PathLinkerResultPanel resultsPanel = new PathLinkerResultPanel(
				String.valueOf(_cySwingApp.getCytoPanel(CytoPanelName.EAST).getCytoPanelComponentCount() + 1),
				_kspSubgraph == null ? _applicationManager.getCurrentNetwork() : _kspSubgraph,
						paths);
		_serviceRegistrar.registerService(resultsPanel, CytoPanelComponent.class, new Properties());

		// open and show the result panel if in hide state
		CytoPanel cytoPanel = _cySwingApp.getCytoPanel(resultsPanel.getCytoPanelName());

		if (cytoPanel.getState() == CytoPanelState.HIDE)
			cytoPanel.setState(CytoPanelState.DOCK);

		// set visible and selected
		resultsPanel.setVisible(true);
		cytoPanel.setSelectedIndex(cytoPanel.indexOfComponent(resultsPanel.getComponent()));
	}

	/**
	 * Creates a new sub-network and sub-network view for the subgraph generated by the KSP
	 * CONTAINS HACK, more info in the line comments, welcome future modification
	 */
	private void createKSPSubgraphAndView() {
		// creates task iterator and execute it to generate a sub-network from the original network
		// the bypass values and other styles from the original network will be pass down to the sub-network
		TaskIterator subNetworkTask = _adapter.get_NewNetworkSelectedNodesAndEdgesTaskFactory()
				.createTaskIterator(_model.getOriginalNetwork());

		// obtain the current network size before running the TaskIterator 
		int currentNetworkSize = _networkManager.getNetworkSet().size();

		_adapter.getTaskManager().execute(subNetworkTask);

		// This IS A HACK
		// Currently we are aren't able to access the new sub-network
		// Therefore we are accessing the new sub-network through the network set
		// We need to pause execution with sleep to wait for the sub network to be added into the network set
		// The new network is add to the set by detecting if the the network set size is changed
		// More about the issue:
		//    https://github.com/Murali-group/PathLinker-Cytoscape/issues/33
		//    https://groups.google.com/forum/#!topic/cytoscape-app-dev/cSUOwhk30fA
		// Source of the hack:
		// https://github.com/smd-faizan/CySpanningTree
		//    -> PrimsTreadThread.java line 219
		try {
			while (currentNetworkSize == _networkManager.getNetworkSet().size())
				Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// THIS IS A HACK CONT. 
		// As we can't access the new sub-network generated from the task
		// loop through all the networks and find one with the highest SUID, 
		// which is the SUID of the sub-network we created
		Set<CyNetwork> allNetworks = _networkManager.getNetworkSet();
		long maxSUID = Integer.MIN_VALUE;
		for(CyNetwork network : allNetworks) {
			if (network.getSUID() > maxSUID) maxSUID = network.getSUID();
		}

		// Apply the new name to the sub-network
		_kspSubgraph = _networkManager.getNetwork(maxSUID);
		String subgraphName = "PathLinker-subnetwork-" + _model.getK() + "-paths";
		_kspSubgraph.getRow(_kspSubgraph).set(CyNetwork.NAME, subgraphName);

		// The current network view is set to the new sub-network view already
		// while current network is still the originalNetwork
		_kspSubgraphView = _applicationManager.getCurrentNetworkView();

		// use a visual bypass to color the sources and targets for the sub-network view
		Color targetColor = new Color(255, 223, 0);

		for (CyNode source : _model.getSubgraphSources()) {
			View<CyNode> currView = _kspSubgraphView.getNodeView(source);
			currView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.DIAMOND);
			currView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.CYAN);
		}
		for (CyNode target : _model.getSubgraphTargets()) {
			View<CyNode> currView = _kspSubgraphView.getNodeView(target);
			currView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.RECTANGLE);
			currView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, targetColor);
		}

		_kspSubgraphView.updateView();

		// applies hierarchical layout if the k <= 200
		if (_model.getK() <= 200)
			applyHierarchicalLayout();
	}

	/**
	 * Applies hierarchical layout to the sub-network If k <= 200
	 */
	private void applyHierarchicalLayout() {

		// set node layout by applying the hierarchical layout algorithm
		CyLayoutAlgorithm algo = _adapter.getCyLayoutAlgorithmManager().getLayout("hierarchical");
		TaskIterator iter = algo.createTaskIterator(_kspSubgraphView, algo.createLayoutContext(),
				CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		_adapter.getTaskManager().execute(iter);
		SynchronousTaskManager<?> synTaskMan = _adapter.getCyServiceRegistrar()
				.getService(SynchronousTaskManager.class);
		synTaskMan.execute(iter);

		// if we applied the hierarchical layout, by default it is rendered upside down
		// so we reflect all the nodes about the x axis
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
		for (CyNode node : _kspSubgraph.getNodeList()) {
			View<CyNode> nodeView = _kspSubgraphView.getNodeView(node);
			double yCoord = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);

			if (yCoord > maxY)
				maxY = yCoord;

			if (yCoord < minY)
				minY = yCoord;
		}

		double midY = (maxY + minY) / 2;

		// reflects each node about the midpoint x axis
		for (CyNode node : _kspSubgraph.getNodeList()) {
			View<CyNode> nodeView = _kspSubgraphView.getNodeView(node);
			double yCoord = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);

			double newY = -1 * yCoord + 2 * midY;
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, newY);
		}

		_kspSubgraphView.updateView();
	}

	/**
     * Sets up title panel
     * contains the logo and the title of the pathlinker app
     * contains help and about button
	 */
	private void setUpTitlePanel() {
		if (_titlePanel != null) // stops if panel already created
			return;

		// initialize the JPanel and group layout
		_titlePanel = new JPanel();
		final GroupLayout titlePanelLayout = new GroupLayout(_titlePanel);
		_titlePanel.setLayout(titlePanelLayout);
		titlePanelLayout.setAutoCreateContainerGaps(true);
		titlePanelLayout.setAutoCreateGaps(true);

		// set up all components
		ImageIcon logo = new ImageIcon(getClass().getResource(("/logo.png")));
		_logoLabel = new JLabel(new ImageIcon(logo.getImage().getScaledInstance(60, 80, java.awt.Image.SCALE_SMOOTH)));
		
		_titleLabel = new JLabel("PATHLINKER");
		_titleLabel.setFont(_titleLabel.getFont().deriveFont(32f)); 
		_titleLabel.setFont(_titleLabel.getFont().deriveFont(Font.BOLD));

		_helpBtn = new JButton("Help");
		_helpBtn.setToolTipText("Click to learn more on how to use PathLinker");
		_helpBtn.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		        // opens the instruction site upon clicking
		        try {
		            Desktop.getDesktop().browse(new URI("http://apps.cytoscape.org/apps/pathlinker"));
		        }
		        catch (IOException | URISyntaxException e1) {
		            e1.printStackTrace();
		        }
		    }
		});

		_aboutBtn = new JButton("About");
		_aboutBtn.setToolTipText("Click to learn more about PathLinker");
		_aboutBtn.addActionListener(new ActionListener() {
		    // sets up the about dialog option
            @Override
            public void actionPerformed(ActionEvent e) {
                //display about box
                synchronized (this) {
                    if (_aboutMenuDialog == null)
                        _aboutMenuDialog = new PathLinkerAboutMenuDialog(_cySwingApp, _version, _buildDate);

                    if (!_aboutMenuDialog.isVisible()) {
                        _aboutMenuDialog.setLocationRelativeTo(null);
                        _aboutMenuDialog.setVisible(true);
                    }
                }
                _aboutMenuDialog.toFront();     
            }
		}); 

		// add all components into the horizontal and vertical group of the GroupLayout
		titlePanelLayout.setHorizontalGroup(titlePanelLayout.createSequentialGroup()
				.addGroup(titlePanelLayout.createParallelGroup()
						.addComponent(_logoLabel))
				.addContainerGap(75, 80)
				.addGroup(titlePanelLayout.createParallelGroup(Alignment.TRAILING, true)
						.addGroup(titlePanelLayout.createParallelGroup()
								.addComponent(_titleLabel))
						.addGroup(titlePanelLayout.createParallelGroup()
								.addGroup(titlePanelLayout.createSequentialGroup()
										.addComponent(_helpBtn)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(_aboutBtn))))
				);
		titlePanelLayout.setVerticalGroup(titlePanelLayout.createParallelGroup()
				.addGroup(titlePanelLayout.createSequentialGroup()
						.addComponent(_logoLabel))
				.addGroup(titlePanelLayout.createSequentialGroup()
						.addGroup(titlePanelLayout.createSequentialGroup()
								.addComponent(_titleLabel))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(titlePanelLayout.createSequentialGroup()
								.addGroup(titlePanelLayout.createParallelGroup()
										.addComponent(_helpBtn)
										.addComponent(_aboutBtn))))
				);
	}

	/**
	 * Sets up source target panel
	 * contains input field for source and target
	 * contains allow Sources Targets In Paths Option check box
	 * contains targets Same As Sources Option check box
	 */
	private void setUpSourceTargetPanel() {
		if (_sourceTargetPanel != null) // stops if panel already created
			return;

		// initialize the JPanel, panel border, and group layout
		_sourceTargetPanel = new JPanel();
		TitledBorder sourceTargetBorder = BorderFactory.createTitledBorder("Sources/Targets");
		_sourceTargetPanel.setBorder(sourceTargetBorder);

		final GroupLayout sourceTargetPanelLayout = new GroupLayout(_sourceTargetPanel);
		_sourceTargetPanel.setLayout(sourceTargetPanelLayout);
		sourceTargetPanelLayout.setAutoCreateContainerGaps(true);
		sourceTargetPanelLayout.setAutoCreateGaps(true);

		_sourcesLabel = new JLabel("Sources separated by spaces, e.g., S1 S2 S3");

		_sourcesTextField = new HintTextField("Type or use button to add selected node name(s) in the network");
		_sourcesTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, _sourcesTextField.getPreferredSize().height));
		_sourcesTextField.getDocument().addDocumentListener(new TextFieldListener());

		_loadNodeToSourceButton = new JButton("Add selected source(s)");
		_loadNodeToSourceButton.setToolTipText("Add selected node(s) from the network view into the sources field");
		_loadNodeToSourceButton.setEnabled(false);
		_loadNodeToSourceButton.addActionListener(new LoadNodeToSourceButtonListener());

		_targetsLabel = new JLabel("Targets separated by spaces, e.g., T1 T2 T3");

		_targetsTextField = new HintTextField("Type or use button to add selected node name(s) in the network");
		_targetsTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, _targetsTextField.getPreferredSize().height));
		_targetsTextField.getDocument().addDocumentListener(new TextFieldListener());

		_loadNodeToTargetButton = new JButton("Add selected target(s)");
		_loadNodeToTargetButton.setToolTipText("Add selected node(s) from the network view into the targets field");
		_loadNodeToTargetButton.setEnabled(false);
		_loadNodeToTargetButton.addActionListener(new LoadNodeToTargetButtonListener());

		_allowSourcesTargetsInPathsOption = new JCheckBox("<html>Allow sources and targets in paths</html>", false);
		_allowSourcesTargetsInPathsOption.setToolTipText("Allow source/target nodes appear as intermediate nodes in "
		        + "path computed.");
		_allowSourcesTargetsInPathsOption.addItemListener(new CheckBoxListener());

		_targetsSameAsSourcesOption = new JCheckBox("<html>Targets are identical to sources</html>", false);
		_targetsSameAsSourcesOption.setToolTipText("Copy the sources to the targets field.");
		_targetsSameAsSourcesOption.addItemListener(new CheckBoxListener());

		_clearSourceTargetPanelButton = new JButton("Clear");
		_clearSourceTargetPanelButton.setEnabled(false);
		_clearSourceTargetPanelButton.setToolTipText("Clear all inputs from Sources/Targets panel");
		_clearSourceTargetPanelButton.addActionListener(new ClearSourceTargetPanelButtonListener());

		// add all components into the horizontal and vertical group of the GroupLayout
		sourceTargetPanelLayout.setHorizontalGroup(sourceTargetPanelLayout.createParallelGroup()
				.addGroup(sourceTargetPanelLayout.createParallelGroup(Alignment.LEADING, true)
						.addComponent(_sourcesLabel)
						.addComponent(_sourcesTextField)
						.addComponent(_loadNodeToSourceButton))
				.addGroup(sourceTargetPanelLayout.createParallelGroup(Alignment.LEADING, true)
						.addComponent(_targetsLabel)
						.addComponent(_targetsTextField)
						.addComponent(_loadNodeToTargetButton))
				.addGroup(sourceTargetPanelLayout.createParallelGroup(Alignment.LEADING, true)
						.addComponent(_allowSourcesTargetsInPathsOption)
						.addGroup(sourceTargetPanelLayout.createSequentialGroup()
								.addComponent(_targetsSameAsSourcesOption)
								.addComponent(_clearSourceTargetPanelButton))
						)
				);
		sourceTargetPanelLayout.setVerticalGroup(sourceTargetPanelLayout.createSequentialGroup()
				.addGroup(sourceTargetPanelLayout.createSequentialGroup()
						.addComponent(_sourcesLabel)
						.addComponent(_sourcesTextField)
						.addComponent(_loadNodeToSourceButton))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(sourceTargetPanelLayout.createSequentialGroup()
						.addComponent(_targetsLabel)
						.addComponent(_targetsTextField)
						.addComponent(_loadNodeToTargetButton))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(sourceTargetPanelLayout.createSequentialGroup()
						.addComponent(_allowSourcesTargetsInPathsOption)
						.addGroup(sourceTargetPanelLayout.createParallelGroup(Alignment.LEADING, true)
								.addComponent(_targetsSameAsSourcesOption)
								.addComponent(_clearSourceTargetPanelButton))
						)
				);
	}

	/**
	 * Sets up the algorithm panel
	 * contains k input field and edge penalty input field
	 */
	private void setUpAlgorithmPanel() {
		if (_algorithmPanel != null) // stops if panel already created
			return;

		// initialize the JPanel, panel border, and group layout
		_algorithmPanel = new JPanel();
		TitledBorder algorithmBorder = BorderFactory.createTitledBorder("Algorithm");
		_algorithmPanel.setBorder(algorithmBorder);

		final GroupLayout algorithmPanelLayout = new GroupLayout(_algorithmPanel);
		_algorithmPanel.setLayout(algorithmPanelLayout);
		algorithmPanelLayout.setAutoCreateContainerGaps(true);
		algorithmPanelLayout.setAutoCreateGaps(true);

		// sets up all the components
		_kLabel = new JLabel("k (# of paths): ");

		_kTextField = new JTextField(5);
		_kTextField.setText("200");
		_kTextField.setMaximumSize(_kTextField.getPreferredSize());

		_includePathScoreTiesOption = new JCheckBox("Include tied paths");
		_includePathScoreTiesOption.setToolTipText("Include more than k paths if the path length/score "
				+ "is equal to the kth path's length/score");

		_edgePenaltyLabel = new JLabel("Edge penalty: ");

		_edgePenaltyTextField = new JTextField(5);
		_edgePenaltyTextField.setMaximumSize(_edgePenaltyTextField.getPreferredSize());

		// add all components into the horizontal and vertical group of the GroupLayout
		algorithmPanelLayout.setHorizontalGroup(algorithmPanelLayout.createParallelGroup(Alignment.TRAILING, true)
				.addGroup(algorithmPanelLayout.createSequentialGroup()
						.addComponent(_kLabel)
						.addComponent(_kTextField))
				.addGroup(algorithmPanelLayout.createSequentialGroup()
						.addComponent(_edgePenaltyLabel)
						.addComponent(_edgePenaltyTextField))
				.addGroup(algorithmPanelLayout.createSequentialGroup()
						.addComponent(_includePathScoreTiesOption))
				);
		algorithmPanelLayout.setVerticalGroup(algorithmPanelLayout.createSequentialGroup()
				.addGroup(algorithmPanelLayout.createParallelGroup(Alignment.CENTER, true)
						.addComponent(_kLabel)
						.addComponent(_kTextField))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(algorithmPanelLayout.createParallelGroup(Alignment.CENTER, true)
						.addComponent(_edgePenaltyLabel)
						.addComponent(_edgePenaltyTextField))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(algorithmPanelLayout.createParallelGroup(Alignment.CENTER, true)
						.addComponent(_includePathScoreTiesOption))
				);
	}

	/**
	 * Sets up the edge weight panel
	 * contains unweighted, weighted additive, and weighted probabilities buttons
	 * contains edge weight column name combo box
	 */
	private void setUpGraphPanel() {
		if (_graphPanel != null) // stops if panel already created
			return;

		// initialize the JPanel, panel border, and group layout
		_graphPanel = new JPanel();
		TitledBorder graphBorder = BorderFactory.createTitledBorder("Edge Weights");
		_graphPanel.setBorder(graphBorder);
		_savedEdgeWeightSelection = ""; // initialize the string

		final GroupLayout graphPanelLayout = new GroupLayout(_graphPanel);
		_graphPanel.setLayout(graphPanelLayout);
		graphPanelLayout.setAutoCreateContainerGaps(true);
		graphPanelLayout.setAutoCreateGaps(true);

		// sets up all the components
		_unweighted = new JRadioButton("Unweighted");
		_unweighted.setActionCommand("unweighted");
		_unweighted.setToolTipText("PathLinker will compute the k lowest cost paths, where the cost is the number of edges in the path.");
		_unweighted.addActionListener(new RadioButtonListener());

		_weightedAdditive = new JRadioButton("Weights are additive");
		_weightedAdditive.setActionCommand("weightedAdditive");
		_weightedAdditive.setToolTipText("PathLinker will compute the k lowest cost paths, where the cost is the sum of the edge weights.");
		_weightedAdditive.addActionListener(new RadioButtonListener());

		_weightedProbabilities = new JRadioButton("Weights are probabilities");
		_weightedProbabilities.setActionCommand("weightedProbabilities");
		_weightedProbabilities.setToolTipText("PathLinker will compute the k highest cost paths, where the cost is the product of the edge weights.");
		_weightedProbabilities.addActionListener(new RadioButtonListener());

		_weightedOptionGroup = new ButtonGroup();
		_weightedOptionGroup.add(_unweighted);
		_weightedOptionGroup.add(_weightedAdditive);
		_weightedOptionGroup.add(_weightedProbabilities);

		_edgeWeightColumnBoxLabel = new JLabel("Edge weight column: ");
		_edgeWeightColumnBoxLabel.setToolTipText("The column in the edge table containing edge weight property");

		_edgeWeightColumnBox = new JComboBox<String>(new String[]{""});
		_edgeWeightColumnBox.setToolTipText("Select the name of the column in the edge table containing edge weight property");

		// sets up the correct behavior and default value for edge weight column and edge penalty text field
		_unweighted.setSelected(true);
		updateEdgeWeightColumn();
		updateEdgePenaltyTextField();

		// add all components into the horizontal and vertical group of the GroupLayout
		graphPanelLayout.setHorizontalGroup(graphPanelLayout.createParallelGroup()
				.addGroup(graphPanelLayout.createParallelGroup(Alignment.LEADING, true)
						.addComponent(_unweighted)
						.addComponent(_weightedAdditive)
						.addComponent(_weightedProbabilities))
				.addGroup(graphPanelLayout.createSequentialGroup()
						.addComponent(_edgeWeightColumnBoxLabel)
						.addComponent(_edgeWeightColumnBox))
				);
		graphPanelLayout.setVerticalGroup(graphPanelLayout.createSequentialGroup()
				.addGroup(graphPanelLayout.createSequentialGroup()
						.addComponent(_unweighted)
						.addComponent(_weightedAdditive)
						.addComponent(_weightedProbabilities))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(graphPanelLayout.createParallelGroup(Alignment.LEADING, false)
						.addComponent(_edgeWeightColumnBoxLabel)
						.addComponent(_edgeWeightColumnBox))
				);
	}

	/**
	 * Sets up the control panel layout
	 * Sets up all the sub panel and its components and add to control panel
	 */
	private void initializeControlPanel() {
		// sets up the size of the control panel
		setMinimumSize(new Dimension(400, 400));
		setPreferredSize(getMinimumSize());

		// set control panel layout to group layout
		final GroupLayout mainLayout = new GroupLayout(this);
		setLayout(mainLayout);

		mainLayout.setAutoCreateContainerGaps(false);
		mainLayout.setAutoCreateGaps(true);

		// sets up all the sub panels and its components
		setUpTitlePanel();
		setUpSourceTargetPanel();
		setUpAlgorithmPanel();
		setUpGraphPanel();

		// creates the submit button
		_submitButton = new JButton("Submit");
		_submitButton.addActionListener(new SubmitButtonListener());
		
		_closeButton = new JButton("Close");
		_closeButton.addActionListener(new ActionListener() {
		    // close the control panel upon clicking
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setPanelState(PanelState.CLOSED);
            }
		});

		// add all components into the horizontal and vertical group of the GroupLayout
		mainLayout.setHorizontalGroup(mainLayout.createParallelGroup(Alignment.LEADING, true)
				.addComponent(_titlePanel)
				.addComponent(_sourceTargetPanel)
				.addComponent(_algorithmPanel)
				.addComponent(_graphPanel)
				.addGroup(mainLayout.createSequentialGroup()
				        .addComponent(_submitButton)
				        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, 300)
				        .addComponent(_closeButton))
				);
		mainLayout.setVerticalGroup(mainLayout.createSequentialGroup()
				.addComponent(_titlePanel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(_sourceTargetPanel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(_algorithmPanel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(_graphPanel)
                .addGroup(mainLayout.createParallelGroup(Alignment.LEADING, true)
                        .addComponent(_submitButton)
                        .addComponent(_closeButton))
				);
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
	
	/**
	 * A JTextField class that comes with "ghost test"
	 * Giving the text field ability to shows hint message that disappears when focus upon
	 * Source: https://stackoverflow.com/questions/1738966/java-jtextfield-with-input-hint
	 */
	class HintTextField extends JTextField implements FocusListener {
	    
	    private final String hint;
	    private boolean showingHint;
	    
	    public HintTextField(final String hint) {
	        super(hint);
	        this.hint = hint;
	        this.showingHint = true;
	        super.setForeground(Color.GRAY);
	        super.addFocusListener(this);    
	    }

	    /**
	     * Method use to remove hint and gray color of the text field
	     * Use by select node listeners
	     */
	    public void gainFocus() {
            super.setText("");
            super.setForeground(Color.BLACK);
            showingHint = false;
	    }
	    
	    /**
	     * Method use to show hint and gray color of the text field
	     * Use by clear button listeners
	     */
	    public void loseFocus() {
            super.setText(hint);
            super.setForeground(Color.GRAY);
            showingHint = true;      
	    }

	    @Override
	    public void focusGained(FocusEvent e) {
	        if(this.getText().isEmpty()) {
	            super.setText("");
	            super.setForeground(Color.BLACK);
	            showingHint = false;
	        }
	    }
	    
	    @Override
	    public void focusLost(FocusEvent e) {
	        if(this.getText().isEmpty()) {
	            super.setText(hint);
	            super.setForeground(Color.GRAY);
	            showingHint = true;    
	        }
	    }
	    
	    @Override
	    public String getText() {
	        return showingHint ? "" : super.getText();    
	    }    
	}
}
