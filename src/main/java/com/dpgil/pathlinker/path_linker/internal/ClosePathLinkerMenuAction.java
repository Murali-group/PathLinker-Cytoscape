package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.PathLinkerCytoPanel.PanelState;
import java.awt.event.ActionEvent;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;

/**
 * // -------------------------------------------------------------------------
 * /** Menu option to close the PathLinker plugin
 *
 * @author Daniel
 * @version Nov 4, 2015
 */
public class ClosePathLinkerMenuAction
    extends AbstractCyAction
{
    private static PathLinkerCytoPanel _panel;


    /**
     * Constructor for the menu option
     *
     * @param panel
     *            the panel to close
     * @param applicationManager
     *            the application manager to add this option into the menu
     */
    public ClosePathLinkerMenuAction(
        PathLinkerCytoPanel panel,
        CyApplicationManager applicationManager)
    {
        super("Close", applicationManager, null, null);
        setPreferredMenu("Apps.PathLinker");

        _panel = panel;
    }


    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        closePanel();
    }


    private void closePanel()
    {
        _panel.setPanelState(PanelState.CLOSED);
    }
}
