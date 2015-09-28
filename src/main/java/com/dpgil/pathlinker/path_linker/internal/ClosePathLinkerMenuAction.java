package com.dpgil.pathlinker.path_linker.internal;

import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;

public class ClosePathLinkerMenuAction extends AbstractCyAction
{
    private PromptCytoPanel panel;
    private CyApplicationManager applicationManager;

    public ClosePathLinkerMenuAction(PromptCytoPanel panel, CyApplicationManager applicationManager)
    {
        super("Close", applicationManager, null, null);
        setPreferredMenu("Apps.PathLinker");

        this.panel = panel;
        this.applicationManager = applicationManager;
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        JOptionPane.showMessageDialog(null, "Closing PathLinker");
        panel.setVisible(false);
    }
}
