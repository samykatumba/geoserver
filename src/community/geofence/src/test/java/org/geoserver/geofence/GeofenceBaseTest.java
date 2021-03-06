/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;

public abstract class GeofenceBaseTest extends GeoServerSystemTestSupport {

    protected static Catalog catalog;

    protected static XpathEngine xp;

    protected static Boolean IS_GEOFENCE_AVAILABLE = isGeoFenceAvailable();

    protected GeofenceAccessManager accessManager;

    protected GeoFenceConfigurationManager configManager;

    protected static RuleReaderService geofenceService;

    GeoServerDataDirectory dd;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<String, String>();

        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");

        testData.registerNamespaces(namespaces);
        registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();

        catalog = getCatalog();

        testData.copyTo(
                this.getClass().getClassLoader().getResourceAsStream("geofence-server.properties"),
                "geofence/geofence-server.properties");

        testData.setUp();

        // add test geofence properties file to the temporary data dir. For testing purposes only
        dd = new GeoServerDataDirectory(testData.getDataDirectoryRoot());
        GeoServerExtensionsHelper.singleton("dataDirectory", dd, GeoServerDataDirectory.class);

        // get the beans we use for testing
        accessManager =
                (GeofenceAccessManager) applicationContext.getBean("geofenceRuleAccessManager");

        configManager =
                (GeoFenceConfigurationManager)
                        applicationContext.getBean("geofenceConfigurationManager");

        if (IS_GEOFENCE_AVAILABLE) {
            System.setProperty("IS_GEOFENCE_AVAILABLE", "True");
        } else {
            System.out.println(
                    "Skipping test in "
                            + getClass().getSimpleName()
                            + " as GeoFence service is down: "
                            + "in order to run this test you need the services to be running on port 9191");
            // TODO: use Assume when using junit >=3
        }
    }

    /** subclass hook to register additional namespaces. */
    protected void registerNamespaces(Map<String, String> namespaces) {}

    @After
    public void after() {
        getCatalog().dispose();
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);

        try {
            if (System.getProperty("IS_GEOFENCE_AVAILABLE") != null) {
                System.clearProperty("IS_GEOFENCE_AVAILABLE");
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not remove System ENV variable {IS_GEOFENCE_AVAILABLE}",
                    e);
        }
    }

    protected static boolean isGeoFenceAvailable() {
        try {
            geofenceService =
                    (RuleReaderService)
                            applicationContext.getBean(
                                    applicationContext
                                            .getBeanFactory()
                                            .resolveEmbeddedValue("${ruleReaderBackend}"));

            geofenceService.getMatchingRules(null, null, null, null, null, null, null, null);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error connecting to GeoFence", e);
            geofenceService = null;
            return false;
        }
    }
}
