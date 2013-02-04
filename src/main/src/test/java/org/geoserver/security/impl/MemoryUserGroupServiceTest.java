/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.test.SystemTest;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class MemoryUserGroupServiceTest extends AbstractUserGroupServiceTest {

    @After
    public void clearUserGroupService() throws IOException {
        store.clear();
        store.store();
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String name) throws Exception {
        MemoryUserGroupServiceConfigImpl config = (MemoryUserGroupServiceConfigImpl ) createConfigObject(name);         
        getSecurityManager().saveUserGroupService(config);
        return getSecurityManager().loadUserGroupService(name);
   }
    
    @Override
    protected SecurityUserGroupServiceConfig createConfigObject( String name ) {
        MemoryUserGroupServiceConfigImpl config = new MemoryUserGroupServiceConfigImpl();
        config.setClassName(MemoryUserGroupService.class.getName());
         config.setName(name);        
         config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
         config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        return config;

     }

    @Test
    public void testInsert() {
        super.testInsert();
        try {
            for (GeoServerUser user : store.getUsers()) {
                assertTrue(user.getClass()==MemoryGeoserverUser.class);
            }
            for (GeoServerUserGroup group : store.getUserGroups()) {
                assertTrue(group.getClass()==MemoryGeoserverUserGroup.class);
            }

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
    

}
