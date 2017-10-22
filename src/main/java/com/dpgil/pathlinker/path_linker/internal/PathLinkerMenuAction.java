package com.dpgil.pathlinker.path_linker.internal;

import java.awt.event.ActionEvent;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;

import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;
import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel.PanelState;

/**
 * // -------------------------------------------------------------------------
 * /** Menu option to open the PathLinker plugin
 *
 * @author Daniel Gil
 * @version Nov 4, 2015
 */
public class PathLinkerMenuAction extends AbstractCyAction {
    private PathLinkerControlPanel _panel;

    /**
     * Constructor for the menu option
     * @param panel the pathlinker control panel
     * @param applicationManager the application manager to add this option into the menu
     */
    public PathLinkerMenuAction(
            PathLinkerControlPanel panel,
            CyApplicationManager applicationManager) {
        super("PathLinker", applicationManager, null, null);
        setPreferredMenu("Apps");

        _panel = panel;
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        // opens the panel
        _panel.setPanelState(PanelState.OPEN);
    }
}
