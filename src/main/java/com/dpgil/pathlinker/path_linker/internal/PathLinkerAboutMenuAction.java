package com.dpgil.pathlinker.path_linker.internal;

import java.awt.event.ActionEvent;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.ActionEnableSupport;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.view.model.CyNetworkViewManager;

/** Menu option to open the about dialogue in PathLinker*/
@SuppressWarnings("serial")
public class PathLinkerAboutMenuAction extends AbstractCyAction{

	/** the swing application from the CyActivator */
	final CySwingApplication _swingApplication;
	/** The about dialog box object */
	private PathLinkerAboutMenuDialog _aboutMenuDialog;
	/** the version of the current PathLinker app */
	private String _version;
	/** the build date of the current PathLinker app */
	private String _buildDate;

	/**
	 * Constructor for the about menu option
	 * @param applicationManager
	 * @param swingApplication
	 * @param networkViewManager
	 * @param version the version date of the PathLinker app
	 * @param buildDate the build date of the PathLinker app
	 */
	public PathLinkerAboutMenuAction(
			final CyApplicationManager applicationManager,
			final CySwingApplication swingApplication,
			final CyNetworkViewManager networkViewManager,
			String version,
			String buildDate) {

		super("About", applicationManager, ActionEnableSupport.ENABLE_FOR_ALWAYS, networkViewManager);

		this._swingApplication = swingApplication;
		this._version = version;
		this._buildDate = buildDate;

		setPreferredMenu("Apps.PathLinker");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		//display about box
		synchronized (this) {
			if (_aboutMenuDialog == null)
				_aboutMenuDialog = new PathLinkerAboutMenuDialog(_swingApplication, _version, _buildDate);

			if (!_aboutMenuDialog.isVisible()) {
				_aboutMenuDialog.setLocationRelativeTo(null);
				_aboutMenuDialog.setVisible(true);
			}
		}
		_aboutMenuDialog.toFront();			
	}
}