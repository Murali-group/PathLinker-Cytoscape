package com.dpgil.pathlinker.path_linker.internal;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JOptionPane;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.view.model.CyNetworkView;

public class InitializeGraphMenuAction extends AbstractCyAction
{
    private final CyApplicationManager applicationManager;

    public InitializeGraphMenuAction(
        CyApplicationManager applicationManager,
        final String menuTitle)
    {

        super(menuTitle, applicationManager, null, null);
        setPreferredMenu("Apps");

        this.applicationManager = applicationManager;
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        // gets the network
        final CyNetworkView networkView =
            applicationManager.getCurrentNetworkView();
        final CyNetwork network = networkView.getModel();

        String nodeListString =
            JOptionPane
                .showInputDialog("Enter the names of the nodes in the graph separated by spaces (ex. N1 N2 N3)");
        String edgeListString =
            JOptionPane
                .showInputDialog("Enter the edges in the graph separated by spaces (ex. E1S E1T E2S E2T)");

        ArrayList<String> nodeNames = new ArrayList<String>(Arrays.asList(nodeListString.split("\\s+")));
        ArrayList<String> edges = new ArrayList<String>(Arrays.asList(edgeListString.split("\\s+")));
        HashMap<String, CyNode> idToNode = new HashMap<String, CyNode>();

        for (String nodeName : nodeNames)
        {
            CyNode newNode = network.addNode();
            network.getRow(newNode).set(CyNetwork.NAME, nodeName);
            idToNode.put(nodeName, newNode);
        }

        for (int i = 0; i < edges.size(); i += 2)
        {
            CyNode node1 = idToNode.get(edges.get(i));
            CyNode node2 = idToNode.get(edges.get(i+1));

            network.addEdge(node1, node2, true);
        }

        networkView.updateView();
    }

}
