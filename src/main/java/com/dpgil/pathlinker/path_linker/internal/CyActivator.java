package com.dpgil.pathlinker.path_linker.internal;

import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.ColumnCreatedListener;
import org.cytoscape.model.events.ColumnDeletedListener;
import org.cytoscape.model.events.ColumnNameChangedListener;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.NetworkDestroyedListener;
import org.cytoscape.model.events.RowsSetListener;

import java.util.Properties;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.dpgil.pathlinker.path_linker.internal.event.PathLinkerColumnUpdateListener;
import com.dpgil.pathlinker.path_linker.internal.event.PathLinkerNetworkEventListener;
import com.dpgil.pathlinker.path_linker.internal.event.PathLinkerNodeSelectionListener;
import com.dpgil.pathlinker.path_linker.internal.rest.PathLinkerImpl;
import com.dpgil.pathlinker.path_linker.internal.rest.PathLinkerResource;
import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;

/**
 * // -------------------------------------------------------------------------
 * /** CyActivator class for the PathLinker Cytoscape Plugin. Runs when
 * Cytoscape is opened. Sets up the PathLinker menu option
 *
 * @author Daniel Gil
 * @version Apr 23, 2015
 */
public class CyActivator extends AbstractCyActivator
{
    private PathLinkerControlPanel controlPanel;

    private PathLinkerNodeSelectionListener nodeViewEventListener;
    private PathLinkerColumnUpdateListener columnUpdateListener;
    private PathLinkerNetworkEventListener networkEventListener;

    private CyApplicationManager cyApplicationManager;
    private CyServiceRegistrar serviceRegistrar;
    private CyNetworkManager networkManager;
    private CyAppAdapter adapter;
    private CySwingApplication cySwingApp;

    private ServiceTracker ciExceptionFactoryTracker;
    private CIExceptionFactory ciExceptionFactory;
    private PathLinkerImpl cyRestClient;

	/** the version of the current PathLinker app */
	private String _version = "1.4.3";
	/** the build date of the current PathLinker app */
	private String _buildDate = "Nov. 12, 2020";

    @Override
    public void start(BundleContext context) throws Exception {

        // initializes all necessary components
        cyApplicationManager = getService(context, CyApplicationManager.class);
        serviceRegistrar = getService(context, CyServiceRegistrar.class);
        networkManager = getService(context, CyNetworkManager.class);
        adapter = getService(context, CyAppAdapter.class);
        cySwingApp = getService(context, CySwingApplication.class);

        ciExceptionFactoryTracker = new ServiceTracker(context, context.createFilter("(objectClass=org.cytoscape.ci.CIExceptionFactory)"), null);
        ciExceptionFactoryTracker.open();
        ciExceptionFactory = (CIExceptionFactory) ciExceptionFactoryTracker.getService();

        controlPanel = new PathLinkerControlPanel();

        // initializes control panel
        controlPanel.initialize(
                cySwingApp,
                serviceRegistrar,
                cyApplicationManager,
                networkManager,
                adapter,
                _version,
                _buildDate);

        nodeViewEventListener = new PathLinkerNodeSelectionListener(controlPanel, cyApplicationManager);
        columnUpdateListener = new PathLinkerColumnUpdateListener(controlPanel);
        networkEventListener = new PathLinkerNetworkEventListener(controlPanel);

        // register control panel
        registerService(context, controlPanel, CytoPanelComponent.class, new Properties());

        // Create PathLinker CyRest implementations
        cyRestClient = new PathLinkerImpl(
                controlPanel,
                cyApplicationManager, networkManager, adapter,
                serviceRegistrar, cySwingApp,
                ciExceptionFactory);

        // register all necessary services to the bundle
        registerService(context, adapter, CyAppAdapter.class, new Properties());
        registerService(context, cySwingApp, CySwingApplication.class, new Properties());
        registerService(context, networkManager, CyNetworkManager.class, new Properties());
        registerService(context, serviceRegistrar, CyServiceRegistrar.class, new Properties());
        registerService(context, cyApplicationManager, CyApplicationManager.class, new Properties());

        // handle load node to source/target button enable/disable events
        registerService(context, nodeViewEventListener, RowsSetListener.class, new Properties());

        // handle events triggered by editing table columns
        registerService(context, columnUpdateListener, ColumnCreatedListener.class, new Properties());
        registerService(context, columnUpdateListener, ColumnDeletedListener.class, new Properties());
        registerService(context, columnUpdateListener, ColumnNameChangedListener.class, new Properties());

        // handle events triggered by changing, add, delete current network
        registerService(context, networkEventListener, SetCurrentNetworkListener.class, new Properties());
        registerService(context, networkEventListener, NetworkAddedListener.class, new Properties());
        registerService(context, networkEventListener, NetworkDestroyedListener.class, new Properties());

        // register CyRest service
        registerService(context, cyRestClient, PathLinkerResource.class, new Properties());
    }

    @Override
    public void shutDown() {
        if (ciExceptionFactoryTracker != null) {
            ciExceptionFactoryTracker.close(); 
        }

        super.shutDown();
    }
}
