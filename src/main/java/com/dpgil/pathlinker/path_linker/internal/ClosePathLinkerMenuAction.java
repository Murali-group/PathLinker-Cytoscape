package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.PromptCytoPanel.PanelState;
import java.awt.Container;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;

public class ClosePathLinkerMenuAction extends AbstractCyAction
{
    private static PromptCytoPanel _panel;

    public ClosePathLinkerMenuAction(PromptCytoPanel panel, CyApplicationManager applicationManager)
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

    public void closePanel()
    {
        _panel.setPanelState(PanelState.CLOSED);
    }
}
