package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.PromptCytoPanel.PanelState;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.Properties;
import javax.swing.JOptionPane;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.osgi.framework.BundleContext;

public class OpenPathLinkerMenuAction extends AbstractCyAction
{
    private PromptCytoPanel _panel;

    public OpenPathLinkerMenuAction(PromptCytoPanel panel, CyApplicationManager applicationManager)
    {
        super("Open", applicationManager, null, null);
        setPreferredMenu("Apps.PathLinker");

        _panel = panel;
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        openPanel();
    }

    private void openPanel()
    {
        _panel.setPanelState(PanelState.OPEN);
    }
}
