package com.dpgil.pathlinker.path_linker.internal;

import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;

public class OpenPathLinkerMenuAction extends AbstractCyAction
{
    private PromptCytoPanel panel;
    private CyApplicationManager applicationManager;

    public OpenPathLinkerMenuAction(PromptCytoPanel panel, CyApplicationManager applicationManager)
    {
        super("Open", applicationManager, null, null);
        setPreferredMenu("Apps.PathLinker");

        this.panel = panel;
        this.applicationManager = applicationManager;
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        JOptionPane.showMessageDialog(null, "Opening PathLinker");
        panel.setVisible(true);

    }
}
