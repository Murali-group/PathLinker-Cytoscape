package com.dpgil.pathlinker.path_linker.internal.view;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;
import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModelParams;
import com.dpgil.pathlinker.path_linker.internal.task.CreateKSPViewTask;
import com.dpgil.pathlinker.path_linker.internal.task.CreateResultPanelTask;
import com.dpgil.pathlinker.path_linker.internal.task.RunKSPTask;
import com.dpgil.pathlinker.path_linker.internal.util.EdgeWeightType;
import com.dpgil.pathlinker.path_linker.internal.util.PathLinkerError;
import com.dpgil.pathlinker.path_linker.internal.util.Algorithms.PathWay;

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

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.PlainDocument;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

/** Panel for the PathLinker plugin */
public class PathLinkerControlPanel extends JPanel implements CytoPanelComponent {
	/** UI components of the panel */
    private JPanel _innerPanel;
	private JPanel _titlePanel;
	private JPanel _networkPanel;
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
	private JTextField _edgePenaltyTextField;

	private JButton _helpBtn;
	private JButton _aboutBtn;
	public JButton _loadNodeToSourceButton;
	public JButton _loadNodeToTargetButton;
	private JButton _clearSourceTargetPanelButton;
	private JButton _submitButton;

	public JComboBox<String> _networkCmb;
	protected JComboBox<String> _edgeWeightColumnBox;
	private ButtonGroup _weightedOptionGroup;
	private JRadioButton _unweighted;
	private JRadioButton _weightedAdditive;
	private JRadioButton _weightedProbabilities;

	private JCheckBox _treatNetworkAsUndirectedOption;
	private JCheckBox _allowSourcesTargetsInPathsOption;
	public JCheckBox _targetsSameAsSourcesOption;
	private JCheckBox _includePathScoreTiesOption;

	private CyServiceRegistrar _serviceRegistrar;

	/** Cytoscape class for network and view management */
	private CySwingApplication _cySwingApp;
	private CyApplicationManager _applicationManager;
	private CyNetworkManager _networkManager;
	private CyAppAdapter _adapter;

	/** The model that runs ksp algorithm from the user input */
	private PathLinkerModel _model;
	/** model parameters to be pass for running ksp algorithm */
	private PathLinkerModelParams _modelParams;
    /** The about dialog box object */
    private PathLinkerAboutMenuDialog _aboutMenuDialog;
    /** the version of the current PathLinker app */
    private String _version;
    /** the build date of the current PathLinker app */
    private String _buildDate;
	/** The original network selected by the user */
	private CyNetwork _originalNetwork;
	/** The sub-network created by the algorithm */
	private CyNetwork _kspSubgraph = null;
	/** The string representation of the edge weight button selection user selected */
	private String _savedEdgeWeightSelection;
    /** The StringBuilder that construct error messages if any to the user */
    private StringBuilder errorMessage;

	/** The map stores the index-SUID pair of each network inside the networkCmb */
    public Map<Integer, Long> _indexToSUIDMap;
    /** The map stores the SUID-index pair of each network inside the networkCmb */
    public Map<Long, Integer> _suidToIndexMap;
	/** The map stores the SUID to path rank column name pair of each network */
    public Map<Long, String> _suidToPathRankMap;
    /** The map stores path raml column name to SUID pair of each network */
    public Map<String, Long> _pathRankToSuidMap;
    /** Global sync index number to sync network, Path Index, and result names upon creation */
    public int nameIndex;


	/**
	 * Initializer for the panel to reduce the number of parameters in the constructor
	 * @param cySwingApp           swing application
	 * @param serviceRegistrar     service registrar
	 * @param applicationManager   application manager
	 * @param networkManager       network manager
	 * @param adapter              app adapter
	 * @param version              PathLinker app version
	 * @param buildDate            PathLinker app build dates
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

		// initialize the maps for path rank columns
		_suidToPathRankMap = new HashMap<Long, String>();
		_pathRankToSuidMap = new HashMap<String, Long>();

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
	public void updateEdgeWeightColumn() {

		_edgeWeightColumnBox.removeAllItems(); //remove all items for update

		//keep box empty if no network found or user selected unweighted as edge weight type
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
	public void initializeNetworkCmb() {
	    
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

	    _networkCmb.setSelectedIndex(_suidToIndexMap.get(_applicationManager.getCurrentNetwork().getSUID()));
	}

	/**
	 * update the edge penalty text field depending on user's selection edge weight radio button
	 * Called by RadioButtonListener class if user selects a radio button
	 */
	private void updateEdgePenaltyTextField() {

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
	    if ((_sourcesTextField.hintEnabled() || _sourcesTextField.getText().trim().isEmpty()) 
		        || (_targetsTextField.hintEnabled() || _targetsTextField.getText().trim().isEmpty())
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
	    // reads the raw values from the panel and converts them into parameters for the model
        // terminates the process if user decides to fix the error manually
        if (!readValuesFromPanel("runPathLinker")) return false;

        // create synchronous task manager to run the task on creating KSP subgraph and etc.
        SynchronousTaskManager<?> synTaskMan = _adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);

		// performs KSP algorithm by creating the runKSPTask
		RunKSPTask runKSPTask = new RunKSPTask(_originalNetwork, _modelParams);
		synTaskMan.execute(new TaskIterator(runKSPTask));
		// obtain results from the runKSPTask
		_model = runKSPTask.getResults(PathLinkerModel.class);

		// check for not path found error
		if (_model.getOutputK() == 0) { 
		    JOptionPane.showMessageDialog(null, 
		            "No paths found", 
		            "Error Message", JOptionPane.ERROR_MESSAGE);
		    return false;
		}

		// obtain result computed from the model
		ArrayList<PathWay> result = _model.getResult();

		// construct createKSPViewTask to create KSP subgraph, subgraph view, path rank, and update related properties
		CreateKSPViewTask createKSPViewTask = new CreateKSPViewTask(this, _originalNetwork, _model, _adapter, _applicationManager);
		synTaskMan.execute(new TaskIterator(createKSPViewTask));
		_kspSubgraph = createKSPViewTask.getResults(CyNetwork.class);

		// writes the result of the algorithm to a table
		CreateResultPanelTask createResultPanelTask = new CreateResultPanelTask(this,
		        _kspSubgraph, String.valueOf(nameIndex),
                _networkManager, result, _serviceRegistrar, _cySwingApp);
		synTaskMan.execute(new TaskIterator(createResultPanelTask));

		return true;
	}

	/**
	 * Reads in the raw values from the panel and converts them to useful
	 * objects that can be used for the algorithm. Performs error checking on
	 * the values and warns the user
	 * @return true if user decides to continue with the warning,
	 *         otherwise false
	 */
	private boolean readValuesFromPanel(String resourcePath) {
	    // Access current network
        _originalNetwork = _applicationManager.getCurrentNetwork();

        // initialize the params from user inputs
        _modelParams = new PathLinkerModelParams();
        _modelParams.treatNetworkAsUndirected = _treatNetworkAsUndirectedOption.isSelected();
        _modelParams.allowSourcesTargetsInPaths = _allowSourcesTargetsInPathsOption.isSelected();
        _modelParams.includeTiedPaths = _includePathScoreTiesOption.isSelected();
        _modelParams.sources = _sourcesTextField.getText().trim();
        _modelParams.targets = _targetsTextField.getText().trim();
        _modelParams.k = _kTextField.getText().trim().isEmpty() ? null : Integer.parseInt(_kTextField.getText());
        _modelParams.edgeWeightColumnName = _edgeWeightColumnBox.getSelectedIndex() == -1 ? "" : _edgeWeightColumnBox.getSelectedItem().toString();
        _modelParams.edgePenalty = _edgePenaltyTextField.getText().trim().isEmpty() ? null : Double.parseDouble(_edgePenaltyTextField.getText());

        // gets the option for edge weight type
        if (_unweighted.isSelected()) {
            _modelParams.edgeWeightType = EdgeWeightType.UNWEIGHTED;
        } else if (_weightedAdditive.isSelected()) {
            _modelParams.edgeWeightType = EdgeWeightType.ADDITIVE;
        } else {
            _modelParams.edgeWeightType = EdgeWeightType.PROBABILITIES;
        }

        // validate the modelParams setting
	    List<PathLinkerError> errorList = _modelParams.validate(_originalNetwork, resourcePath);

	    if (errorList.isEmpty()) return true;

	    // look for network not found error
	    if (errorList.get(0).status == PathLinkerError.CY_NETWORK_NOT_FOUND_CODE) {
	        JOptionPane.showMessageDialog(null, 
	                "Network not found. Please load or select a cytoscape network", 
	                "Error Message", JOptionPane.ERROR_MESSAGE);
	        return false;
	    }

	    // error message to report errors to the user if they occur
	    errorMessage = new StringBuilder();

	    for (int i = 0; i < errorList.size(); i++) {
	        errorMessage.append(errorList.get(i).uiMessage);
	    }

	    // check if user is able to continue with the error
	    if (!_modelParams.continueStatus()) {
            JOptionPane.showMessageDialog(null, errorMessage.toString(), 
                    "Error Message", JOptionPane.ERROR_MESSAGE);
            return false;
	    }

	    // construct dialog panel to ask if user wants to continue
	    errorMessage.append("\nWould you like to cancel and correct the inputs?" + 
	            "\nOr continue and run PathLinker with " + 
	            _modelParams.getSourcesList().size() + " sources, " + _modelParams.getTargetsList().size() + " targets, ");
	    if (_modelParams.edgeWeightType != EdgeWeightType.UNWEIGHTED)
	        errorMessage.append("k = " + _modelParams.k + ", and edge penalty = " + _modelParams.edgePenalty + "?");
	    else
	        errorMessage.append("and k = " + _modelParams.k + "?");

	    String[] options = {"Continue", "Cancel"};

	    int choice = JOptionPane.showOptionDialog(null, errorMessage.toString(), 
	            "Warning", 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

	    if (choice != 0) // quit if they say cancel
	        return false;

	    // update k and edge penalty text field if modification to model params were made
	    _kTextField.setText(Integer.toString(_modelParams.k));
	    _edgePenaltyTextField.setText(Double.toString(_modelParams.edgePenalty));

	    // successful parsing
	    return true;
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
		            Desktop.getDesktop().browse(new URI("http://pathlinker-cytoscape-app.readthedocs.io/en/latest/PathLinker_Cytoscape.html"));
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
	 * Sets up network panel
	 * contains drop-down to select network
	 * contains treat Network As Undirected check box
	 * contains targets Same As Sources Option check box
	 */
	private void setUpNetworkPanel() {
		if (_networkPanel != null) // stops if panel already created
			return;

		// initialize the JPanel, panel border, and group layout
		_networkPanel = new JPanel();
		TitledBorder networkBorder = BorderFactory.createTitledBorder("Network");
		_networkPanel.setBorder(networkBorder);

		final GroupLayout networkPanelLayout = new GroupLayout(_networkPanel);
		_networkPanel.setLayout(networkPanelLayout);
		networkPanelLayout.setAutoCreateContainerGaps(true);
		networkPanelLayout.setAutoCreateGaps(true);
		
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

		_treatNetworkAsUndirectedOption = new JCheckBox("<html>Treat network as undirected</html>", false);
		_treatNetworkAsUndirectedOption.setToolTipText("Ignore directionality of edges when computing paths");
		// _treatNetworkAsUndirectedOption.addItemListener(new CheckBoxListener());

		// add all components into the horizontal and vertical group of the GroupLayout
		networkPanelLayout.setHorizontalGroup(networkPanelLayout.createParallelGroup()
                .addGroup(networkPanelLayout.createSequentialGroup()
                        .addComponent(_networkCmbLabel)
                        .addComponent(_networkCmb))
				.addGroup(networkPanelLayout.createParallelGroup(Alignment.LEADING, true)
						.addComponent(_treatNetworkAsUndirectedOption))
				);
		networkPanelLayout.setVerticalGroup(networkPanelLayout.createSequentialGroup()
                .addGroup(networkPanelLayout.createParallelGroup(Alignment.LEADING, true)
                        .addComponent(_networkCmbLabel)
                        .addComponent(_networkCmb))
				.addGroup(networkPanelLayout.createSequentialGroup()
						.addComponent(_treatNetworkAsUndirectedOption))
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
		_targetsSameAsSourcesOption.setToolTipText("Nodes in the sources field are copied to the targets field." + 
                " PathLinker will compute a subnetwork connecting sources to each other");
		_targetsSameAsSourcesOption.addItemListener(new ConnectSourcesCheckBoxListener());

		_clearSourceTargetPanelButton = new JButton("Clear");
		_clearSourceTargetPanelButton.setEnabled(false);
		_clearSourceTargetPanelButton.setToolTipText("Clear all Sources and Targets inputs");
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
		_kTextField.setMaximumSize(_kTextField.getPreferredSize());
        _kTextField.setToolTipText("Number of shortest paths to compute");

        // create text field filter for k text field
        PlainDocument inputKDoc = (PlainDocument) _kTextField.getDocument();
        inputKDoc.setDocumentFilter(new IntegerTextFieldFilter());
        _kTextField.setText("50");

		_includePathScoreTiesOption = new JCheckBox("Include tied paths");
		_includePathScoreTiesOption.setToolTipText("Include more than k paths if the path length/score "
				+ "is equal to the kth path's length/score");

		_edgePenaltyLabel = new JLabel("Edge penalty: ");

		_edgePenaltyTextField = new JTextField(5);
		_edgePenaltyTextField.setMaximumSize(_edgePenaltyTextField.getPreferredSize());
        _edgePenaltyTextField.setToolTipText("Penalize additional edges according to the edge weight type. " +
                "The higher the penalty, the more short paths of high cost will appear before long paths of low cost");

        // create text field filter for edge penalty text field
        PlainDocument edgePenaltyDoc = (PlainDocument) _edgePenaltyTextField.getDocument();
        edgePenaltyDoc.setDocumentFilter(new DoubleTextFieldInputFilter());

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
        setUpNetworkPanel();
		setUpSourceTargetPanel();
		setUpAlgorithmPanel();
		setUpGraphPanel();

		// creates the submit button
		_submitButton = new JButton("Submit");
        _submitButton.setEnabled(false);
		_submitButton.addActionListener(new SubmitButtonListener());

		// initialize the running message
		// set to invisible and only visible when pathlinker is generating subnetwork
        _runningMessage = new JLabel("<html><b>Generating subnetwork...</b></html>");
        _runningMessage.setForeground(Color.BLUE);
        hideRunningMessage();

		// add all components into the horizontal and vertical group of the GroupLayout
		mainLayout.setHorizontalGroup(mainLayout.createParallelGroup(Alignment.LEADING, true)
				.addComponent(_titlePanel)
				.addComponent(_networkPanel)
				.addComponent(_sourceTargetPanel)
				.addComponent(_algorithmPanel)
				.addComponent(_graphPanel)
				.addGroup(mainLayout.createSequentialGroup()
				        .addComponent(_submitButton)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, 145)
		                .addComponent(_runningMessage)
		                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, 145)
                        )
				);
		mainLayout.setVerticalGroup(mainLayout.createSequentialGroup()
				.addComponent(_titlePanel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(_networkPanel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(_sourceTargetPanel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(_algorithmPanel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(_graphPanel)
                .addGroup(mainLayout.createParallelGroup(Alignment.LEADING, true)
                        .addComponent(_submitButton)
                        .addComponent(_runningMessage)
                        )
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
