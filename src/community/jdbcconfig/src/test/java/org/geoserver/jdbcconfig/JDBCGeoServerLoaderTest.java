package org.geoserver.jdbcconfig;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;
import static org.geoserver.jdbcconfig.JDBCConfigTestSupport.*;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerFactoryImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.jdbcconfig.config.JDBCGeoServerFacade;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wms.WMSXStreamLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.collect.Maps;


public class JDBCGeoServerLoaderTest {

    JDBCConfigTestSupport testSupport;

    @Before
    public void setUp() throws Exception {
        testSupport = new JDBCConfigTestSupport() {
            @Override
            protected void configureAppContext(WebApplicationContext appContext) {
                expect(appContext.getBeanNamesForType(XStreamServiceLoader.class))
                    .andReturn(new String[]{"wmsLoader"}).anyTimes();
                expect(appContext.getBeanNamesForType((Class)anyObject()))
                    .andReturn(new String[]{}).anyTimes();
                expect(appContext.getBean("wmsLoader"))
                    .andReturn(new WMSXStreamLoader(getResourceLoader())).anyTimes();
                
            }
        };
        testSupport.setUp();
    }

    @After
    public void tearDown() throws Exception {
        testSupport.tearDown();
    }

    @Test
    public void testLoadEmptyNoImport() throws Exception {
        JDBCConfigProperties config = createNiceMock(JDBCConfigProperties.class);
        expect(config.isInitDb()).andReturn(true).anyTimes();
        expect(config.isImport()).andReturn(false).anyTimes();
        replay(config);

        JDBCGeoServerLoader loader = 
            new JDBCGeoServerLoader(testSupport.getResourceLoader(), config);
        loader.setGeoServerFacade(new JDBCGeoServerFacade(testSupport.getDatabase()));
        loader.setApplicationContext(testSupport.getApplicationContext());

        //create a mock and ensure a global, logging, and service config are set
        GeoServerImpl geoServer = createNiceMock(GeoServerImpl.class);
        expect(geoServer.getFactory()).andReturn(new GeoServerFactoryImpl(geoServer)).anyTimes();

        geoServer.setGlobal((GeoServerInfo) anyObject());
        expectLastCall().once();

        geoServer.setLogging((LoggingInfo) anyObject());
        expectLastCall().once();

        geoServer.add((ServiceInfo)anyObject());
        expectLastCall().once();

        replay(geoServer);
        loader.postProcessBeforeInitialization(geoServer, "geoServer");

        verify(geoServer);
    }
}
