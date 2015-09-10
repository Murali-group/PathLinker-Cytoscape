package com.dpgil.pathlinker.path_linker.internal;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

public class PromptCytoPanel extends JPanel implements CytoPanelComponent
{
    public PromptCytoPanel()
    {
        JLabel label = new JLabel("This is my control panel");

        this.add(label);
        this.setVisible(true);
    }

    @Override
    public Component getComponent()
    {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName()
    {
        return CytoPanelName.WEST;
    }

    @Override
    public Icon getIcon()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTitle()
    {
        return "PathLinker";
    }

}
