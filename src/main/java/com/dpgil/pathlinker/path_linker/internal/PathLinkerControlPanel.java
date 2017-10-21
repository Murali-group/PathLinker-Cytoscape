package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;

import java.awt.BorderLayout;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
    private JPanel _innerPanel;
	private JPanel _titlePanel;
	private JPanel _sourceTargetPanel;
	private JPanel _algorithmPanel;
	private JPanel _graphPanel;

	private JLabel _networkCmbLabel;
	private JLabel _logoLabel;
	private JLabel _titleLabel;
	private JLabel _sourcesLabel;
	private JLabel _targetsLabel;
	private JLabel _kLabel;
	private JLabel _edgePenaltyLabel;
	private JLabel _edgeWeightColumnBoxLabel;
	private JLabel _runningMessage;

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

	protected static JComboBox<String> _networkCmb;
	protected static JComboBox<String> _edgeWeightColumnBox;
	private static ButtonGroup _weightedOptionGroup;
	private static JRadioButton _unweighted;
	private static JRadioButton _weightedAdditive;
	private static JRadioButton _weightedProbabilities;

	private JCheckBox _allowSourcesTargetsInPathsOption;
	protected static  JCheckBox _targetsSameAsSourcesOption;
	private JCheckBox _includePathScoreTiesOption;

	private CyServiceRegistrar _serviceRegistrar;

	/** Cytoscape class for network and view management */
	private CySwingApplication _cySwingApp;
	protected static CyApplicationManager _applicationManager;
	private static CyNetworkManager _networkManager;
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

	/** The map stores the index-SUID pair of each network inside the networkCmb */
    protected static Map<Integer, Long> _indexToSUIDMap;
    /** The map stores the SUID-index pair of each network inside the networkCmb */
    protected static Map<Long, Integer> _suidToIndexMap;
	/** The map stores the SUID to path index column name pair of each network */
    protected static Map<Long, String> _suidToPathIndexMap;
    /** The map stores path index column name to SUID pair of each network */
    protected static Map<String, Long> _pathIndexToSuidMap;
    /** Global sync index number to sync network, Path Index, and result names upon creation */
    protected static int nameIndex;

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
            // Update: for now, close the app without warning.
		    //String[] options = {"Yes", "Cancel"};
		    //int choice = JOptionPane.showOptionDialog(null, "Are you sure you want to exit PathLinker?", 
		    //        "Warning", 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            //if (choice != 0) return; // quit if they say cancel
			
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

		// initialize the maps for path index columns
		_suidToPathIndexMap = new HashMap<Long, String>();
		_pathIndexToSuidMap = new HashMap<String, Long>();

		// initialize the name index field
		nameIndex = 0;

		initializeControlPanel(); // construct the GUI
	}

	/** Listener for _allowSourcesTargetsInPathsOption */
	class CheckBoxListener implements ItemListener {
		/** Enable/disable the button based on the check boxes */
		@Override
		public void itemStateChanged(ItemEvent e) {
			enableClearButton();
		}
	}

	/** Listener for _targetsSameAsSourcesOption */
	class ConnectSourcesCheckBoxListener implements ItemListener {
		/** Enable/disable the button based on the check boxes */
		@Override
		public void itemStateChanged(ItemEvent e) {
            if (_targetsSameAsSourcesOption.isSelected()) {
                // ensure text field is not in shadow text mode
                if (_targetsTextField.hintEnabled())
                    _targetsTextField.gainFocus();

                _targetsTextField.setText(_sourcesTextField.getText());
                _targetsTextField.setEditable(false);
                _allowSourcesTargetsInPathsOption.setSelected(true);
                _loadNodeToTargetButton.setEnabled(false);
            }
            else{
                _targetsTextField.setEditable(true);
                if (_targetsTextField.getText().equals(""))
                    _targetsTextField.loseFocus();
                if (_loadNodeToSourceButton.isEnabled())
                    _loadNodeToTargetButton.setEnabled(true);
            }

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
	 * Listener for the target text field in the panel
	 * Enable/disable the _clearSourceTargetPanelButton and _submitButton based on the text fields
	 */
	class TargetTextFieldListener implements DocumentListener {

	    @Override
		public void changedUpdate(DocumentEvent e) {
			enableClearButton();
            enableSubmitButton();
		}
		@Override
		public void insertUpdate(DocumentEvent e) {
			enableClearButton();
            enableSubmitButton();
		}
		@Override
		public void removeUpdate(DocumentEvent e) {
			enableClearButton();
            enableSubmitButton();
		}
	}

	/**
	 * Listener for the source text field in the panel
	 * Enable/disable the _clearSourceTargetPanelButton and _submitButton based on the text fields
     * Also update the targets text field if anything changes here
	 */
	class SourceTextFieldListener implements DocumentListener {
		@Override
		public void changedUpdate(DocumentEvent e) {
		    updateTargets();
		    enableClearButton();
            enableSubmitButton();
		}
		@Override
		public void insertUpdate(DocumentEvent e) {
            updateTargets();
		    enableClearButton();
            enableSubmitButton();
		}
		@Override
		public void removeUpdate(DocumentEvent e) {
		    updateTargets(); 
		    enableClearButton();
            enableSubmitButton();
		}

        private void updateTargets() {
            if (_targetsSameAsSourcesOption.isSelected()) {
                _targetsTextField.setText(_sourcesTextField.getText());
            }
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

			if (_sourcesTextField.hintEnabled())
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

	         if (_targetsTextField.hintEnabled())
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
			// uncheck both checkboxes
		    _allowSourcesTargetsInPathsOption.setSelected(false);
			_targetsSameAsSourcesOption.setSelected(false);

            // lose focus to trigger shadow hint behavior
            _sourcesTextField.loseFocus();
            _targetsTextField.loseFocus();
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
	 * construct/update the combo box items for the network combo box
	 * Use when the PathLinker starts
	 *     when network name is changed
	 */
	protected static void initializeNetworkCmb() {
	    
	  //make sure combo box and related maps is empty when initializing
	    _networkCmb.removeAllItems();
	    _indexToSUIDMap.clear();
	    _suidToIndexMap.clear();
	    
	    // No network exists in CytoScape
	    if (_networkManager == null || _networkManager.getNetworkSet().size() == 0)
	        return;

	    _networkCmb.addItem(""); // add placeholder empty string
	    int indexCounter = 1; // the index counter to add before each item
	    
	    for (CyNetwork network : _networkManager.getNetworkSet()) {
	        _indexToSUIDMap.put(_networkCmb.getItemCount(), network.getSUID());
	        _suidToIndexMap.put(network.getSUID(), _networkCmb.getItemCount());
	        _networkCmb.addItem(
	                indexCounter + ". " + network.getRow(network).get(CyNetwork.NAME, String.class));
	        indexCounter++;
	    }

	    // ends if no network is selected, otherwise sets the default value for networkCmb
	    if (_applicationManager.getCurrentNetwork() == null)
	        return;

	    _networkCmb.setSelectedItem(
	            _suidToIndexMap.get(_applicationManager.getCurrentNetwork().getSUID()));
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
		if ((_sourcesTextField.hintEnabled() || _sourcesTextField.getText().trim().equals(""))
		        && (_targetsTextField.hintEnabled() || _targetsTextField.getText().trim().equals(""))
				&& !_allowSourcesTargetsInPathsOption.isSelected()
				&& !_targetsSameAsSourcesOption.isSelected())
			_clearSourceTargetPanelButton.setEnabled(false);
		else _clearSourceTargetPanelButton.setEnabled(true);
	}

	/** enables/disable the _clearSourceTargetPanelButton
	 * based on the source/target text fields and the check boxes
	 */
	private void enableSubmitButton() {
        // TODO if the text field is empty, sometimes the button is still enabled
		if (_sourcesTextField.hintEnabled() || _targetsTextField.hintEnabled() 
		        || _applicationManager.getCurrentNetwork() == null)
			_submitButton.setEnabled(false);
		else _submitButton.setEnabled(true);
	}

	/**
	 * RunningMessage and related methods are now temporarily removed from the PathLinker 
	 * as new layout doesn't support dynamically adding components
	 * 
	 * Will be removed after alternative is found
	 */
	private void prepareAndRunKSP() {
		showRunningMessage();

		// callRunKSP();

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
	}

	private void hideRunningMessage() {
		_runningMessage.setVisible(false);
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
            JOptionPane.showMessageDialog(null, 
                    "Network not found. Please load or select a cytoscape network", 
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

	    // If no paths were found, then exit with this error
        if (_model.getOutputK() == 0) {
            JOptionPane.showMessageDialog(null, 
                    "No paths found", 
                    "Error Message", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // disable the action to update the network combo box while creating the new network
        PathLinkerNodeSelectionListener.setActive(false);

        // increment the index use for creating the network, path index column, and result panel
        nameIndex++;

		// generates a subgraph of the nodes and edges involved in the resulting paths and displays it to the user
		createKSPSubgraphAndView();

		// enables the action to update the network combo box after creating the new network
		PathLinkerNodeSelectionListener.setActive(true);

		// manually updates the network combo box after creating the new network
		initializeNetworkCmb();

		// update the table path index attribute
		updatePathIndexAttribute(result);

		updateNetworkName();

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
        boolean quit = false;

		// edge case where only one source and one target are inputted,
		// so no paths will be found. warn the user
		if (sources.size() == 1 && sources.equals(targets)) {
            errorMessage.insert(0, "The only source node is the same as the only target node.\n"
                    + "PathLinker will not compute any paths. Please add more nodes to the sources or targets.\n\n");
            quit = true;
		}

		// makes sure that we actually have at least one valid source and target
        if (targets.size() == 0 && targetsNotInNet.size() == 0) {
            errorMessage.insert(0, "The targets text field is empty.\n  - Targets are required to run PathLinker.\n");
            quit = true;
        }
        else if (targets.size() == 0) {
            errorMessage.insert(0, "\n  - Targets are required to run PathLinker.\n");
            quit = true;
        }
		// insert all missing targets/targets to the error message
        if (targetsNotInNet.size() > 0) {
            int totalTargets = targets.size() + targetsNotInNet.size();
            errorMessage.insert(0, targets.size() + " out of " + totalTargets + " targets are found in the network." +
                    "\n  - Targets not found: " + targetsNotInNet.toString() +
                    "\n  - Please ensure the entered node names match the 'name' column of the Node Table.\n");
		}

        if (sources.size() == 0 && sourcesNotInNet.size() == 0) {
            errorMessage.insert(0, "The sources text field is empty.\n  - Sources are required to run PathLinker.\n");
            quit = true;
        }
        else if (sources.size() == 0) {
            errorMessage.insert(0, "  - Sources are required to run PathLinker.\n");
            quit = true;
        }
		// insert all missing sources/targets to the error message
        if (sourcesNotInNet.size() > 0) {
            int totalSources = sources.size() + sourcesNotInNet.size();
            errorMessage.insert(0, sources.size() + " out of " + totalSources + " sources are found in the network." +
                    "\n  - Sources not found: " + sourcesNotInNet.toString() +
                    "\n  - Please ensure the entered node names match the 'name' column of the Node Table.\n");
		}

		// checks if all the edges in the graph have weights. Skip the check if edge weight setting is unweighted
		// if a weighted option was selected, but not all edges have weights
		// then we say something to the user.
		if (_edgeWeightSetting != EdgeWeightSetting.UNWEIGHTED){

            _originalNetwork = _model.getOriginalNetwork();
            for (CyEdge edge : _originalNetwork.getEdgeList()) {
                try {
                    Double.parseDouble(_originalNetwork.getRow(edge).getRaw(_edgeWeightColumnName).toString());
                } catch (NullPointerException  e) {
                    errorMessage.append("Weighted option is selected, but at least one edge does not have a weight in the selected edge weight column '" + 
                            _edgeWeightColumnName + "'. Please either select the Unweighted option, or ensure all edges have a weight to run PathLinker.\n");
                    quit = true;
                    break;
                }
            }
        }

        // if PathLinker cannot continue, then show the error message
        if (quit) {
            JOptionPane.showMessageDialog(null, errorMessage.toString(), 
                    "Error Message", JOptionPane.ERROR_MESSAGE);
            return false;
        }

		// there is some error but PathLinker can continue, tell the user
		if (errorMessage.length() > 0) {
		    errorMessage.append("\nWould you like to cancel and correct the inputs?" + 
                    "\nOr continue and run PathLinker with " + sources.size() + " sources, " + + targets.size() + " targets, ");
            if (_edgeWeightSetting != EdgeWeightSetting.UNWEIGHTED)
                errorMessage.append("k = " + _kValue + ", and edge penalty = " + _edgePenalty + "?");
            else
                errorMessage.append("and k = " + _kValue + "?");
		    
		    String[] options = {"Continue", "Cancel"};
		    
		    int choice = JOptionPane.showOptionDialog(null, errorMessage.toString(), 
                    "Warning", 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		    
		    if (choice != 0) // quit if they say cancel
		        return false;
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

			// throw exception if _kValue is a integer but less than 1
			if (_kValue < 1)
			    throw new NumberFormatException();

		} catch (NumberFormatException exception) {
			errorMessage.append("Invalid text entered for k: '" + kInput + "'.\n  - Must be a positive integer. " +
                    "\n  - Setting to default: 50.\n");
			_kValue = 50;
            _kTextField.setText("50");
		}

		// gets the option for edge weight setting
		if (_unweighted.isSelected()) {
			_edgeWeightSetting = EdgeWeightSetting.UNWEIGHTED;
            // skip the rest of the code for getting the penalty and edge weight column
            return;
		} else if (_weightedAdditive.isSelected()) {
			_edgeWeightSetting = EdgeWeightSetting.ADDITIVE;
		} else if (_weightedProbabilities.isSelected()) {
			_edgeWeightSetting = EdgeWeightSetting.PROBABILITIES;
		}
		// parses the value inputted for edge penalty
		// if it is an invalid value, uses 1.0 by default for multiplicative
		// option or 0.0 by default for additive option and also appends the
		// error to the error message
		String edgePenaltyInput = _edgePenaltyTextField.getText().trim();
        // try to parse the user's input
        try {
            _edgePenalty = Double.parseDouble(edgePenaltyInput);
			// throw exception if the edge penalty is a double but less than 0
			if (_edgePenalty < 0)
			    throw new NumberFormatException();
            // or if the option is probabilities and the edge penalty is less than 1
            // this is because dividing by a number less than 1 could cause the edge weights to be > 1, 
            // which would cause them to be negative after taking the -log.
            if (_edgePenalty < 1 && _edgeWeightSetting == EdgeWeightSetting.PROBABILITIES)
			    throw new NumberFormatException();

        } catch (NumberFormatException exception) {
            errorMessage.append("Invalid text entered for edge penalty: '" + edgePenaltyInput + "'.\n");
            // invalid number was entered, invoked an exception
            if (_edgeWeightSetting == EdgeWeightSetting.PROBABILITIES) {
                errorMessage.append("  - Must be a number >= 1.0 for the probability/multiplicative setting." + 
                        "\n  - Setting to default: 1.0.\n");
                _edgePenalty = 1.0;
				_edgePenaltyTextField.setText("1");
            }

            if (_edgeWeightSetting == EdgeWeightSetting.ADDITIVE) {
                errorMessage.append("  - Must be a number >= 0 for the additive setting." +
                        "\n  - Setting to default: 0.0\n");
                _edgePenalty = 0;
				_edgePenaltyTextField.setText("0");
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
		// Use nameIndex to create a new attribute "path index n"
	    // in the network edge table, where n is an unique number
		while (_originalNetwork.getDefaultEdgeTable().getColumn("path index " + nameIndex) != null)
			nameIndex++;

		String columnName = "path index " + nameIndex;
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

		// add the newly created column into the maps
        _pathIndexToSuidMap.put(columnName, _kspSubgraph.getSUID());
        _suidToPathIndexMap.put(_kspSubgraph.getSUID(), columnName);
	}

	/**
	 * Writes the ksp results to result panel given the results from the ksp algorithm
	 * @param paths a list of paths generated from the ksp algorithm
	 */
	private void writeResult(ArrayList<Path> paths) {

		// create and register a new panel in result panel with specific title
	    // the result panel name will be sync with network and path index using nameIndex
		PathLinkerResultPanel resultsPanel = new PathLinkerResultPanel(String.valueOf(nameIndex),
				_networkManager, _kspSubgraph, paths);
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
	 */
	private void createKSPSubgraphAndView() {
		// creates task iterator and execute it to generate a sub-network from the original network
		// the bypass values and other styles from the original network will be pass down to the sub-network
		TaskIterator subNetworkTask = _adapter.get_NewNetworkSelectedNodesAndEdgesTaskFactory()
				.createTaskIterator(_model.getOriginalNetwork());

		// obtain the current network size before running the TaskIterator
		int currentNetworkSize = _networkManager.getNetworkSet().size();

		_adapter.getTaskManager().execute(subNetworkTask);

        // sleep until the subgraph is been created
		// by checking if the size of the network is incremented
		try {
			while (currentNetworkSize == _networkManager.getNetworkSet().size())
				Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// The current network view is set to the new sub-network view already
		// while current network is still the originalNetwork
        _kspSubgraph = _applicationManager.getCurrentNetworkView().getModel();
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

		// apply layout according to the k value
		applyLayout();
	}

	/**
	 * Assign appropriate network name to the new sub-network created using nameIndex field
	 */
	public void updateNetworkName() {
	    // Create the new name to the sub-network
        String subgraphName = "PathLinker-subnetwork-" + _model.getOutputK() + "-paths-" + nameIndex;

        int count = 1;
        boolean condition = false;
        List<CyNetwork> networkList = new ArrayList<CyNetwork>();
        networkList.addAll(_networkManager.getNetworkSet());

        // check if network network already exist
        for (CyNetwork network : networkList) {
            if (network.getRow(network).get(CyNetwork.NAME, String.class).trim().equals(subgraphName)) {
                condition = true;
                break;
            }
        }

        // if network name already exist, create alternative name
        // check if alternative name also exists
        outerLoop:
        while (condition) {
            for (CyNetwork network : networkList) {
                if (network.getRow(network).get(CyNetwork.NAME, String.class).trim().
                        equals(subgraphName + " (" + count + ")")) {
                    count++;
                    continue outerLoop;
                }
            }

            subgraphName += (" (" + count + ")");
            condition = false;
        }

        // apply the name to the network
        _kspSubgraph.getRow(_kspSubgraph).set(CyNetwork.NAME, subgraphName);
	}

	/**
	 * Applies hierarchical layout to the sub-network If k <= 2000, otherwise the users default layout will be applied
	 */
	private void applyLayout() {
        // Applying the hierarchical layout is quick for a small number of nodes and edges. 
        // Applying the hierarchical layout took ~2 sec for k=1000, ~10 sec for k=2000, and ~5 min for k=5000. 
        // The user can apply the layout after generating the network, so to keep running time down, set the max to k=2000
        boolean hierarchical = _model.getOutputK() <= 2000;

        // set node layout by applying the default or hierarchical layout algorithm
        CyLayoutAlgorithm algo = hierarchical ? _adapter.getCyLayoutAlgorithmManager().getLayout("hierarchical")
                : _adapter.getCyLayoutAlgorithmManager().getDefaultLayout();
		TaskIterator iter = algo.createTaskIterator(_kspSubgraphView, algo.createLayoutContext(),
				CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		_adapter.getTaskManager().execute(iter);
		SynchronousTaskManager<?> synTaskMan = _adapter.getCyServiceRegistrar()
				.getService(SynchronousTaskManager.class);
		synTaskMan.execute(iter);

		if (!hierarchical) // ends if default layout
		    return;

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
		// Update: only reflect nodes if k < 200. For k >= 200, the hierarchical layout is right-side up
		if (_model.getOutputK() < 200){
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
		
		_titleLabel = new JLabel("PathLinker");
		_titleLabel.setFont(_titleLabel.getFont().deriveFont(32f)); 
		_titleLabel.setFont(_titleLabel.getFont().deriveFont(Font.BOLD));

		_helpBtn = new JButton("Help");
		_helpBtn.setToolTipText("Visit https://github.com/Murali-group/PathLinker-Cytoscape to learn more about how to use PathLinker");
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
				.addPreferredGap(ComponentPlacement.RELATED, 
				        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
		
		_networkCmbLabel = new JLabel("Select network: ");
        _networkCmbLabel.setToolTipText("The network to run PathLinker on");

        _networkCmb = new JComboBox<String>(new String[]{""});
        _networkCmb.setToolTipText("Select the network to run PathLinker on");
        _networkCmb.setMaximumSize(new Dimension(_networkCmb.getMaximumSize().width, 
                _networkCmb.getPreferredSize().height));
        _networkCmb.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXXX");
        
        _indexToSUIDMap = new HashMap<Integer, Long>(); // creates a empty index-SUID pair map
        _suidToIndexMap = new HashMap<Long, Integer>(); // creates a empty SUID-index pair map
        initializeNetworkCmb();

        // add action listener to change network when selecting
        _networkCmb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (_indexToSUIDMap.containsKey(_networkCmb.getSelectedIndex())) {
                    _applicationManager.setCurrentNetwork(_networkManager.getNetwork(
                            _indexToSUIDMap.get(_networkCmb.getSelectedIndex())));
                }
                enableSubmitButton();
            }
        });

		_sourcesLabel = new JLabel("<html>Sources separated by spaces (e.g., S1 S2 S3)" 
                + "<br>Must match the 'name' column in the Node Table</html>");

		_sourcesTextField = new HintTextField("Select sources in the network or enter text manually");
		_sourcesTextField.setMaximumSize(new Dimension(_sourcesTextField.getMaximumSize().width, 
		        _sourcesTextField.getPreferredSize().height));
		_sourcesTextField.getDocument().addDocumentListener(new SourceTextFieldListener());

		_loadNodeToSourceButton = new JButton("Add selected source(s)");
		_loadNodeToSourceButton.setToolTipText("Add selected node(s) from the network view into the sources field");
		_loadNodeToSourceButton.setEnabled(false);
		_loadNodeToSourceButton.addActionListener(new LoadNodeToSourceButtonListener());

		_targetsLabel = new JLabel("Targets separated by spaces (e.g., T1 T2 T3)");

		_targetsTextField = new HintTextField("Select targets in the network or enter text manually");
		_targetsTextField.setMaximumSize(new Dimension(_targetsTextField.getMaximumSize().width, 
		        _targetsTextField.getPreferredSize().height));
		_targetsTextField.getDocument().addDocumentListener(new TargetTextFieldListener());

		_loadNodeToTargetButton = new JButton("Add selected target(s)");
		_loadNodeToTargetButton.setToolTipText("Add selected node(s) from the network view into the targets field");
		_loadNodeToTargetButton.setEnabled(false);
		_loadNodeToTargetButton.addActionListener(new LoadNodeToTargetButtonListener());

		_allowSourcesTargetsInPathsOption = new JCheckBox("<html>Allow sources and targets in paths</html>", false);
		_allowSourcesTargetsInPathsOption.setToolTipText("Allow source/target nodes to appear as intermediate nodes in "
		        + "computed paths");
		_allowSourcesTargetsInPathsOption.addItemListener(new CheckBoxListener());

		_targetsSameAsSourcesOption = new JCheckBox("<html>Connect sources to each other</html>", false);
		_targetsSameAsSourcesOption.setToolTipText("PathLinker will compute a subnetwork connecting sources to each other." + 
                " Copies the nodes in the sources field to the targets field");
		_targetsSameAsSourcesOption.addItemListener(new ConnectSourcesCheckBoxListener());

		_clearSourceTargetPanelButton = new JButton("Clear");
		_clearSourceTargetPanelButton.setEnabled(false);
		_clearSourceTargetPanelButton.setToolTipText("Clear all Sources and Targets inputs");
		_clearSourceTargetPanelButton.addActionListener(new ClearSourceTargetPanelButtonListener());

		// add all components into the horizontal and vertical group of the GroupLayout
		sourceTargetPanelLayout.setHorizontalGroup(sourceTargetPanelLayout.createParallelGroup()
                .addGroup(sourceTargetPanelLayout.createSequentialGroup()
                        .addComponent(_networkCmbLabel)
                        .addComponent(_networkCmb))
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
                .addGroup(sourceTargetPanelLayout.createParallelGroup(Alignment.LEADING, true)
                        .addComponent(_networkCmbLabel)
                        .addComponent(_networkCmb))
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
		_kTextField.setText("50");
		_kTextField.setMaximumSize(_kTextField.getPreferredSize());
        _kTextField.setToolTipText("Number of shortest paths to compute");

		_includePathScoreTiesOption = new JCheckBox("Include tied paths");
		_includePathScoreTiesOption.setToolTipText("Include more than k paths if the path length/score "
				+ "is equal to the kth path's length/score");

		_edgePenaltyLabel = new JLabel("Edge penalty: ");

		_edgePenaltyTextField = new JTextField(5);
		_edgePenaltyTextField.setMaximumSize(_edgePenaltyTextField.getPreferredSize());
        _edgePenaltyTextField.setToolTipText("Penalize additional edges according to the edge weight type. " +
                "The higher the penalty, the more short paths of high cost will appear before long paths of low cost");

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
		_unweighted.setToolTipText("PathLinker will compute the k lowest cost paths, where the cost is the number of edges in the path");
		_unweighted.addActionListener(new RadioButtonListener());

		_weightedAdditive = new JRadioButton("Weights are additive");
		_weightedAdditive.setActionCommand("weightedAdditive");
		_weightedAdditive.setToolTipText("PathLinker will compute the k lowest cost paths, where the cost is the sum of the edge weights");
		_weightedAdditive.addActionListener(new RadioButtonListener());

		_weightedProbabilities = new JRadioButton("Weights are probabilities");
		_weightedProbabilities.setActionCommand("weightedProbabilities");
		_weightedProbabilities.setToolTipText("PathLinker will compute the k highest weight, lowest cost paths, " +
                "where the path weight is the product of the edge weights and the cost is the sum of the -log edge weights");
		_weightedProbabilities.addActionListener(new RadioButtonListener());

		_weightedOptionGroup = new ButtonGroup();
		_weightedOptionGroup.add(_unweighted);
		_weightedOptionGroup.add(_weightedAdditive);
		_weightedOptionGroup.add(_weightedProbabilities);

		_edgeWeightColumnBoxLabel = new JLabel("Edge weight column: ");
		_edgeWeightColumnBoxLabel.setToolTipText("The column in the edge table containing edge weight property. Must be integer or float");

		_edgeWeightColumnBox = new JComboBox<String>(new String[]{""});
		_edgeWeightColumnBox.setToolTipText("Select the name of the edge table column to use as the edge weights");
		_edgeWeightColumnBox.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXXX");

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

	    // sets control panel to use border layout for maximizing scroll bar
	    // creates inner panel for holding all components inside the scroll panel
	    this.setLayout(new BorderLayout());
	    _innerPanel = new JPanel(null);

		// set inner panel layout to group layout
		final GroupLayout mainLayout = new GroupLayout(_innerPanel);
		_innerPanel.setLayout(mainLayout);

		mainLayout.setAutoCreateContainerGaps(false);
		mainLayout.setAutoCreateGaps(true);

		// sets up all the sub panels and its components
		setUpTitlePanel();
		setUpSourceTargetPanel();
		setUpAlgorithmPanel();
		setUpGraphPanel();

		// creates the submit button
		_submitButton = new JButton("Submit");
        _submitButton.setEnabled(false);
		_submitButton.addActionListener(new SubmitButtonListener());

		_closeButton = new JButton("Close");
		_closeButton.addActionListener(new ActionListener() {
		    // close the control panel upon clicking
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setPanelState(PanelState.CLOSED);
            }
		});

		// initialize the running message
		// set to invisible and only visible when pathlinker is generating subnetwork
        _runningMessage = new JLabel("<html><b>Generating subnetwork...</b></html>");
        _runningMessage.setForeground(Color.BLUE);
        hideRunningMessage();

		// add all components into the horizontal and vertical group of the GroupLayout
		mainLayout.setHorizontalGroup(mainLayout.createParallelGroup(Alignment.LEADING, true)
				.addComponent(_titlePanel)
				.addComponent(_sourceTargetPanel)
				.addComponent(_algorithmPanel)
				.addComponent(_graphPanel)
				.addGroup(mainLayout.createSequentialGroup()
				        .addComponent(_submitButton)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, 145)
		                .addComponent(_runningMessage)
		                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, 145)
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
                        .addComponent(_runningMessage)
                        .addComponent(_closeButton))
				);

		// creates scroll panel that creates scroll bar for inner panel
		JScrollPane scrollPane = new JScrollPane(_innerPanel,
		        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setMinimumSize(_innerPanel.getPreferredSize());

		// add scroll panel to control panel
        this.add(scrollPane);
        this.setPreferredSize(
                new Dimension(this.getPreferredSize().width + 30,
                        this.getPreferredSize().height + 20));
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

	    public boolean hintEnabled() {
	        return this.showingHint;
	    }

	    /**
	     * Method use to remove hint and gray color of the text field
	     * Use by select node listeners
	     */
	    public void gainFocus() {
	        showingHint = false;
            super.setText("");
            super.setForeground(Color.BLACK);
	    }
	    
	    /**
	     * Method use to show hint and gray color of the text field
	     * Use by clear button listeners
	     */
	    public void loseFocus() {
	        showingHint = true;
            super.setText(hint);
            super.setForeground(Color.GRAY);
            enableClearButton();
	    }

	    @Override
	    public void focusGained(FocusEvent e) {
	        if (this.getText().trim().isEmpty()) {
                showingHint = false;
	            super.setText("");
	            super.setForeground(Color.BLACK);
	        }
	    }
	    
	    @Override
	    public void focusLost(FocusEvent e) {
	        if (this.getText().trim().isEmpty()) {
	            showingHint = true;
	            super.setText(hint);
	            super.setForeground(Color.GRAY);    
	        }
	    }
	    
	    @Override
	    public String getText() {
	        return showingHint ? "" : super.getText();
	    }    
	}
}
