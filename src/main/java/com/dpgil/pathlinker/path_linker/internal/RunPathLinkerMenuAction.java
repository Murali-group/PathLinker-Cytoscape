package com.dpgil.pathlinker.path_linker.internal;

import java.util.Arrays;
import javax.swing.JOptionPane;
import com.dpgil.pathlinker.path_linker.internal.Algorithms;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;

/**
 * // -------------------------------------------------------------------------
 * /** PathLinker Menu Action class. Creates the Run PathLinker option under the
 * Apps menu. Runs PathLinker when called.
 *
 * @author Daniel Gil
 * @version Apr 23, 2015
 */
public class RunPathLinkerMenuAction
    extends AbstractCyAction
{
    private final CyApplicationManager applicationManager;
    private final CyTableFactory       tableFactory;
    private final CyTableManager       tableManager;


    /**
     * Constructor for the PathLinker Menu action.
     *
     * @param applicationManager
     *            the cytoscape application manager, used to set the table to be
     *            the current table
     * @param menuTitle
     *            what is displayed in the drop down menu that is pressed to
     *            activate PathLinker. In this case it is "Run PathLinker"
     * @param tableFactory
     *            cytoscape's table factory. used to create and edit tables
     * @param tableManager
     *            cytoscape's table manager. used to add tables to cytoscape's
     *            active tables
     */
    public RunPathLinkerMenuAction(
        CyApplicationManager applicationManager,
        final String menuTitle,
        CyTableFactory tableFactory,
        CyTableManager tableManager)
    {

        super(menuTitle, applicationManager, null, null);
        setPreferredMenu("Apps");

        this.applicationManager = applicationManager;
        this.tableFactory = tableFactory;
        this.tableManager = tableManager;
    }


    public void actionPerformed(ActionEvent e)
    {
        CyTable table =
            tableFactory.createTable(
                "PathLinker ",
                "#",
                Integer.class,
                true,
                true);

        // gets the network
        final CyNetworkView networkView =
            applicationManager.getCurrentNetworkView();
        final CyNetwork network = networkView.getModel();

        // sets up the table
        table.createColumn("k", Integer.class, false);
        table.createColumn("Source", String.class, false);
        table.createColumn("Target", String.class, false);
        table.createColumn("Length", Integer.class, false);
// table.createColumn("Distance", Integer.class, false);
        table.createColumn("Path", String.class, false);

        // adds the table to cytoscape
        applicationManager.setCurrentTable(table);
        tableManager.addTable(table);

        String sourceArrayString =
            JOptionPane
                .showInputDialog("Enter the names of the sources separated by spaces (ex. S1 S2 S3)");
        String targetArrayString =
            JOptionPane
                .showInputDialog("Enter the names of the targets separated by spaces (ex. T1 T2 T3)");


        // init and store a list of all the nodes' names
        ArrayList<String> nodeNames = new ArrayList<String>();
        for (CyNode node : network.getNodeList())
        {
            nodeNames.add(network.getRow(node).get(CyNetwork.NAME, String.class));
        }

        String[] sourceArray = sourceArrayString.split(" ");
        String[] targetArray = targetArrayString.split(" ");

        ArrayList<String> sources =
            new ArrayList<String>(Arrays.asList(sourceArray));
        ArrayList<String> targets =
            new ArrayList<String>(Arrays.asList(targetArray));

        // verifies all the sources are in the network
        StringBuilder srcNotInNet = new StringBuilder();
        for (String srcName : sources)
        {
            if (!nodeNames.contains(srcName))
            {
                srcNotInNet.append(srcName).append(" ");
            }
        }
        if (srcNotInNet.length() > 0)
        {
            int choice = JOptionPane.showConfirmDialog(null, "The sources "+srcNotInNet.toString()+" are not in the network. Continue anyway?");
            if (choice != 0)
            {
                // quit if they say no or cancel
                return;
            }
        }

        // verifies all the targets are in the network
        StringBuilder targNotInNet = new StringBuilder();
        for (String targName : targets)
        {
            if (!nodeNames.contains(targName))
            {
                targNotInNet.append(targName).append(" ");
            }
        }
        if (targNotInNet.length() > 0)
        {
            int choice = JOptionPane.showConfirmDialog(null, "The targets "+targNotInNet.toString()+" are not in the network. Continue anyway?");
            if (choice != 0)
            {
                // quit if they say no or cancel
                return;
            }
        }

        int num = 0;
        int k;
        String kInput = JOptionPane.showInputDialog("Enter k to compute up to k-shortest paths.");

        // takes in k as input
        try {
            k = Integer.parseInt(kInput);
        }
        catch (NumberFormatException exception)
        {
            JOptionPane.showMessageDialog(null, "Invalid number "+kInput+" entered. Using default k=5.");
            k = 5;
        }

        long startTime = System.currentTimeMillis();

        // goes through every s/t pair
        for (CyNode source : network.getNodeList())
        {
            String sourceName =
                network.getRow(source).get(CyNetwork.NAME, String.class);

            // makes sure it's a source
            if (!sources.contains(sourceName)) {
                continue;
            }

            for (CyNode target : network.getNodeList())
            {
                String targetName =
                    network.getRow(target).get(CyNetwork.NAME, String.class);

                if (!targets.contains(targetName)) {
                    continue;
                }

                if (!source.equals(target))
                {
                    ArrayList<ArrayList<CyNode>> paths =
                        Algorithms.ksp(table, network, source, target, k);

                    // TODO fix this so the i < k check isn't necessary
                    for (int i = 0; i < paths.size() && i < k; i++)
                    {
                        // empty path
                        if (paths.get(i).size() == 0)
                            continue;

                        CyRow row = table.getRow(num++);

                        String currPath = "";
                        for (CyNode node : paths.get(i))
                        {
                            currPath +=
                                network.getRow(node).get(
                                    CyNetwork.NAME,
                                    String.class)
                                    + " ";
                        }
                        row.set("k", i+1);
                        row.set("Source", sourceName);
                        row.set("Target", targetName);
                        row.set("Length", paths.get(i).size()-1);
                        row.set("Path", currPath);
                    }
                }
            }
        }

//        Algorithms.prepareNetForKSP(
//            network,
//            new ArrayList<CyNode>(),
//            new ArrayList<CyNode>(),
//            new ArrayList<Integer>());

        networkView.updateView();

        // time to execute the program
        long endTime = System.currentTimeMillis();
        long totalTimeMs = endTime - startTime;
        String timeMessage = "PathLinker took "+totalTimeMs+" milliseconds to execute.";
        JOptionPane.showMessageDialog(null, timeMessage);
    }
}
