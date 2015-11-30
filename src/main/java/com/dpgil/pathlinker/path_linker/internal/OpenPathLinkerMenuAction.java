package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.PathLinkerPanel.PanelState;
import java.awt.event.ActionEvent;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;

/**
 * // -------------------------------------------------------------------------
 * /** Menu option to open the PathLinker plugin
 *
 * @author Daniel Gil
 * @version Nov 4, 2015
 */
public class OpenPathLinkerMenuAction
    extends AbstractCyAction
{
    private PathLinkerPanel _panel;
    private boolean         _isEnabled;


    /**
     * Constructor for the menu option
     *
     * @param panel
     *            the panel to be opened
     * @param applicationManager
     *            the application manager to add this option into the menu
     */
    public OpenPathLinkerMenuAction(
        PathLinkerPanel panel,
        CyApplicationManager applicationManager)
    {
        super("Open", applicationManager, null, null);
        setPreferredMenu("Apps.PathLinker");

        _panel = panel;
        _isEnabled = true;
//        this.setEnabled(true);
// this.updateEnableState();
    }


    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        openPanel();
    }


//    @Override
//    public void setEnabled(boolean enable)
//    {
//        _isEnabled = enable;
//// this.updateEnableState();
//    }
//
//
//    @Override
//    public boolean isEnabled()
//    {
//        return _isEnabled;
//    }


    private void openPanel()
    {
        _panel.setPanelState(PanelState.OPEN);
    }
}
