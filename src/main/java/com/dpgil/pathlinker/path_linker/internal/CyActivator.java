package com.dpgil.pathlinker.path_linker.internal;

import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyTableFactory;
import java.util.Properties;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.service.util.AbstractCyActivator;
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

        PromptCytoPanel panel = new PromptCytoPanel();

        // sets up the pathlinker menu option
        RunPathLinkerMenuAction rplaction = new RunPathLinkerMenuAction(
            panel,
            cyApplicationManager,
            "Run PathLinker",
            tableFactory,
            tableManager);

        // registers the services
        registerAllServices(context, rplaction, new Properties());
        registerService(context, panel, CytoPanelComponent.class, new Properties());
    }
}
