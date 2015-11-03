package com.dpgil.pathlinker.path_linker.internal;

import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import java.util.Properties;
import javax.swing.JOptionPane;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.BundleContext;

/**
 * // -------------------------------------------------------------------------
 * /** CyActivator class for the PathLinker Cytoscape Plugin. Runs when
 * Cytoscape is opened. Sets up the PathLinker menu option
 *
 * @author Daniel Gil
 * @version Apr 23, 2015
 */
public class CyActivator
    extends AbstractCyActivator
{

    @Override
    public void start(BundleContext context)
        throws Exception
    {
        // sets up pathlinker menu option
        CyApplicationManager cyApplicationManager =
            getService(context, CyApplicationManager.class);

        // grabs the table factory and manager from the bundle context
        // so they can be passed to the RPL menu option
        CyTableFactory tableFactory = getService(context, CyTableFactory.class);
        CyTableManager tableManager = getService(context, CyTableManager.class);

        PromptCytoPanel panel = new PromptCytoPanel(cyApplicationManager, tableFactory, tableManager);

        CyNetworkFactory networkFactory = getService(context, CyNetworkFactory.class);
        CyNetworkManager networkManager = getService(context, CyNetworkManager.class);
        CyNetworkViewFactory networkViewFactory = getService(context, CyNetworkViewFactory.class);
        CyNetworkViewManager networkViewManager = getService(context, CyNetworkViewManager.class);
        CyAppAdapter adapter = getService(context, CyAppAdapter.class);
        panel.initialize(networkFactory, networkManager, networkViewFactory, networkViewManager, adapter);

        // sets up the pathlinker menu option
//        RunPathLinkerMenuAction rplaction = new RunPathLinkerMenuAction(
//            panel,
//            cyApplicationManager,
//            "PathLinker",
//            tableFactory,
//            tableManager);

        OpenPathLinkerMenuAction oplaction = new OpenPathLinkerMenuAction(panel, cyApplicationManager);
        ClosePathLinkerMenuAction cplaction = new ClosePathLinkerMenuAction(panel, cyApplicationManager);

        // registers the services
//        registerAllServices(context, rplaction, new Properties());
        registerAllServices(context, oplaction, new Properties());
        registerAllServices(context, cplaction, new Properties());
        registerService(context, panel, CytoPanelComponent.class, new Properties());
    }

//    public static void openPanel()
//    {
//        registerService(context, )
//    }
}
