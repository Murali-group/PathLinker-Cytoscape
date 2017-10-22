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
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.osgi.framework.BundleContext;

import com.dpgil.pathlinker.path_linker.internal.event.PathLinkerColumnUpdateListener;
import com.dpgil.pathlinker.path_linker.internal.event.PathLinkerNetworkEventListener;
import com.dpgil.pathlinker.path_linker.internal.event.PathLinkerNodeSelectionListener;
import com.dpgil.pathlinker.path_linker.internal.view.PathLinkerControlPanel;

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
	public void start(BundleContext context) throws Exception {
		// sets up pathlinker menu option
		CyApplicationManager cyApplicationManager =
				getService(context, CyApplicationManager.class);

		// initializes the panel with the necessary components
		PathLinkerControlPanel panel = new PathLinkerControlPanel();
		CyServiceRegistrar serviceRegistrar = 
				getService(context, CyServiceRegistrar.class);
		CyNetworkManager networkManager =
				getService(context, CyNetworkManager.class);
		CyAppAdapter adapter = getService(context, CyAppAdapter.class);
		CySwingApplication cySwingApp = getService(context, CySwingApplication.class);
		registerService(
				context,
				panel,
				CytoPanelComponent.class,
				new Properties());

		// sets up the pathlinker menu option
		PathLinkerMenuAction panelMenuAction =
				new PathLinkerMenuAction(panel, cyApplicationManager);
		registerAllServices(context, panelMenuAction, new Properties());

		// initializes panel
		panel.initialize(
				cySwingApp,
				serviceRegistrar,
				cyApplicationManager,
				networkManager,
				adapter,
				"v1.3", 
				"Oct. 03, 2017");

		// starts off the panel in a closed state
		panel.getParent().remove(panel);

		// register all necessary services to the bundle
	    registerService(context, adapter, CyAppAdapter.class, new Properties());
		registerService(context, cySwingApp, CySwingApplication.class, new Properties());
		registerService(context, networkManager, CyNetworkManager.class, new Properties());
		registerService(context, serviceRegistrar, CyServiceRegistrar.class, new Properties());
		registerService(context, cyApplicationManager, CyApplicationManager.class, new Properties());

		// handle load node to source/target button enable/disable events
		PathLinkerNodeSelectionListener nodeViewEventListener = new PathLinkerNodeSelectionListener();
		registerService(context, nodeViewEventListener, RowsSetListener.class, new Properties());

		// handle events triggered by editing table columns
		PathLinkerColumnUpdateListener columnUpdateListener = new PathLinkerColumnUpdateListener();
		registerService(context, columnUpdateListener, ColumnCreatedListener.class, new Properties());
		registerService(context, columnUpdateListener, ColumnDeletedListener.class, new Properties());
		registerService(context, columnUpdateListener, ColumnNameChangedListener.class, new Properties());

		// handle events triggered by changing, add, delete current network
		PathLinkerNetworkEventListener networkEventListener = new PathLinkerNetworkEventListener();
		registerService(context, networkEventListener, SetCurrentNetworkListener.class, new Properties());
		registerService(context, networkEventListener, NetworkAddedListener.class, new Properties());
		registerService(context, networkEventListener, NetworkDestroyedListener.class, new Properties());
	}
}
