package com.dpgil.pathlinker.path_linker.internal;

import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import com.dpgil.pathlinker.path_linker.internal.PathLinkerPanel.PanelState;
import java.util.Properties;
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
        // so they can be passed to the panel
        CyTableFactory tableFactory = getService(context, CyTableFactory.class);
        CyTableManager tableManager = getService(context, CyTableManager.class);

        // initializes the panel with the necessary components
        PathLinkerPanel panel = new PathLinkerPanel(
            cyApplicationManager,
            tableFactory,
            tableManager);
        CyNetworkFactory networkFactory =
            getService(context, CyNetworkFactory.class);
        CyNetworkManager networkManager =
            getService(context, CyNetworkManager.class);
        CyNetworkViewFactory networkViewFactory =
            getService(context, CyNetworkViewFactory.class);
        CyNetworkViewManager networkViewManager =
            getService(context, CyNetworkViewManager.class);
        CyAppAdapter adapter = getService(context, CyAppAdapter.class);
        registerService(
            context,
            panel,
            CytoPanelComponent.class,
            new Properties());

        // sets up the pathlinker open and close menu options
        OpenPathLinkerMenuAction oplaction =
            new OpenPathLinkerMenuAction(panel, cyApplicationManager);
        ClosePathLinkerMenuAction cplaction =
            new ClosePathLinkerMenuAction(panel, cyApplicationManager);
        registerAllServices(context, oplaction, new Properties());
        registerAllServices(context, cplaction, new Properties());

        // intializes panel
        panel.initialize(
            networkFactory,
            networkManager,
            networkViewFactory,
            networkViewManager,
            adapter,
            oplaction,
            cplaction);

        // starts off the panel in a closed state
        panel.setPanelState(PanelState.CLOSED);
    }
}
