package com.dpgil.pathlinker.path_linker.internal;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.ActionEnableSupport;
import org.cytoscape.view.model.CyNetworkViewManager;

/** Menu option to open the PathLinker instruction website */
@SuppressWarnings("serial")
public class PathLinkerHelpMenuAction extends AbstractCyAction {

	/**
	 * Constructor for the help menu option
	 * @param applicationManager
	 * @param networkViewManager
	 */
	public PathLinkerHelpMenuAction(
			final CyApplicationManager applicationManager,
			final CyNetworkViewManager networkViewManager) {

		super("Help", applicationManager, ActionEnableSupport.ENABLE_FOR_ALWAYS, networkViewManager);

		setPreferredMenu("Apps.PathLinker");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// opens the instruction website upon clicking
		try {
			Desktop.getDesktop().browse(new URI("http://apps.cytoscape.org/apps/pathlinker"));
		} catch (IOException | URISyntaxException e1) {
			e1.printStackTrace();
		}
	}
}
