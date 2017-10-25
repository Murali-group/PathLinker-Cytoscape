package com.dpgil.pathlinker.path_linker.internal;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import org.cytoscape.application.swing.CySwingApplication;

/** About dialog box for PathLinker */
@SuppressWarnings("serial")
public class PathLinkerAboutMenuDialog extends JDialog {

	/** the version of the current PathLinker app */
	private String _version;
	/** the build date of the current PathLinker app */
	private String _buildDate;

	/** Main panel for dialog box */
	private JEditorPane _mainPanel;
	/** panel for the cancel button */
	private JPanel _buttonPanel;

	/**
	 * Constructor for the dialog box
	 * @param swingApplication swing application from the activator
	 * @param version the version of the current PathLinker app
	 * @param buildDate the build date of the current PathLinker app
	 */
	public PathLinkerAboutMenuDialog(
			final CySwingApplication swingApplication,
			final String version,
			final String buildDate) {

		super(swingApplication.getJFrame(), "About PathLinker", false);
		this._version = version;
		this._buildDate = buildDate;

		initializePanel();
	}

	/**
	 * Sets up the main panel that displays text and links about the PathLinker
	 */
	private void setMainPanel() {
		if (_mainPanel == null) {
			_mainPanel = new JEditorPane();
			_mainPanel.setMargin(new Insets(10, 10, 10, 10));
			_mainPanel.setEditable(false);
			_mainPanel.setEditorKit(new HTMLEditorKit());
			_mainPanel.addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent event) {
					if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
						try {
							Desktop.getDesktop().browse(event.getURL().toURI());
						} catch (Exception e) {}
					}
				}
			});

			URL logoURL;
			try {
				logoURL = new URL("http://apps.cytoscape.org/media/pathlinker/logo.png.png");
			} catch (MalformedURLException e) {
				logoURL = null;
			}
			String logoCode = logoURL != null ? "<center><img src='" + logoURL + "'></center>" : "";

			String text = "<html><body>" +
					logoCode +
					"<P align=center><b>PathLinker v" + _version + " (" + _buildDate + ")</b><BR>" +
					"A Cytoscape App<BR><BR>" +
					"Version " + _version + " by the <a href='http://bioinformatics.cs.vt.edu/~murali/research.html'>Computational Systems Biology Research Group</a>, Virginia Tech<BR><BR>" +
					"If you use this app in your research, please cite:<BR>" +
					"<a href='https://www.ncbi.nlm.nih.gov/pubmed/28413614'>The PathLinker app: Connect the dots in protein interaction networks</a>. <br>Daniel Gil, Jeffrey Law, Li Huang and T. M. Murali. F1000Research 2017, 6:58 <BR>" +
					"<a href='https://www.nature.com/articles/npjsba20162'>Pathways on Demand: Automated Reconstruction of Human Signaling Networks</a>. <br>Anna Ritz, Christopher L. Poirel, Allison N. Tegge, Nicholas Sharp, Allison Powell, Kelsey Simmons, <br> Shiv D. Kale, and T. M. Murali. Systems Biology and Applications 2, 2016, 16002.<BR>" +
					"<BR><BR><BR>" + // triple line break to make extra space for the logo image
					"</P></body></html>";

			_mainPanel.setText(text);
			_mainPanel.setSize(_mainPanel.getWidth(), _mainPanel.getPreferredSize().height + 200);
		}

		getContentPane().add(_mainPanel, BorderLayout.CENTER);
	}

	/**
	 * Sets up the panel for close button which closes the About dialog window on clicking
	 */
	private void setButtonPanel() {
		if (_buttonPanel == null) {
			_buttonPanel = new JPanel();

			JButton closeButton = new JButton("Close");

			// close the about dialog
			closeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});

			closeButton.setAlignmentX(CENTER_ALIGNMENT);
			_buttonPanel.add(closeButton);
		}

		getContentPane().add(_buttonPanel, BorderLayout.SOUTH);
	}

	/**
	 * Sets up the About dialog window
	 */
	private void initializePanel() {
		setResizable(false);
		setMainPanel();
		setButtonPanel();
		pack();
	}
}
