package com.dpgil.pathlinker.path_linker.internal;

import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.ColumnCreatedListener;
import org.cytoscape.model.events.ColumnDeletedListener;
import org.cytoscape.model.events.ColumnNameChangedListener;
import org.cytoscape.model.events.RowsSetListener;

import com.dpgil.pathlinker.path_linker.internal.PathLinkerPanel.PanelState;
import java.util.Properties;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
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

        // initializes the panel with the necessary components
        PathLinkerPanel panel = new PathLinkerPanel();
        CyNetworkManager networkManager =
            getService(context, CyNetworkManager.class);
        CyNetworkViewFactory networkViewFactory =
            getService(context, CyNetworkViewFactory.class);
        CyNetworkViewManager networkViewManager =
            getService(context, CyNetworkViewManager.class);
        CyAppAdapter adapter = getService(context, CyAppAdapter.class);
        CySwingApplication cySwingApp = getService(context, CySwingApplication.class);
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
        
        // sets up the about dialog menu option
        PathLinkerAboutMenuAction amaction =
        		new PathLinkerAboutMenuAction(cyApplicationManager, cySwingApp, networkViewManager, "1.3", "08.08.2017");
        registerAllServices(context, amaction, new Properties());
        
        // intializes panel
        panel.initialize(
            cySwingApp,
            cyApplicationManager,
            networkManager,
            networkViewFactory,
            networkViewManager,
            adapter);

        // starts off the panel in a closed state
        panel.setPanelState(PanelState.CLOSED);
        
        // handle load node to source/target button enable/disable events
        PathLinkerNodeSelectionListener nodeViewEventListener = new PathLinkerNodeSelectionListener();
        registerService(context, nodeViewEventListener, RowsSetListener.class, new Properties());
        
        // handle events triggered by editing table columns
        PathLinkerColumnUpdateListener columnUpdateListener = new PathLinkerColumnUpdateListener();
        registerService(context, columnUpdateListener, ColumnCreatedListener.class, new Properties());
        registerService(context, columnUpdateListener, ColumnDeletedListener.class, new Properties());
        registerService(context, columnUpdateListener, ColumnNameChangedListener.class, new Properties());
        
        // handle events triggered by changing current network
        PathLinkerNetworkEventListener networkEventListener = new PathLinkerNetworkEventListener();
        registerService(context, networkEventListener, SetCurrentNetworkListener.class, new Properties());
    }
}
