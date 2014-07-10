/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.MapResource;
import org.geoserver.rest.RestletException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.restlet.data.Status;

/**
 * REST Resource for reading and changing the master password
 * 
 * @author christian
 *
 */
public class MasterPasswordResource extends MapResource {

    static final String MP_CURRENT_KEY="MasterPassword"; 
    static final String MP_NEW_KEY="NewMasterPassword";
    
    Map putMap;
    
    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }
    
  

    /** 
     * PUT is allowed if {@link MasterPasswordProviderConfig#isReadOnly()}
     * evaluates to <code>false</code> and the principal has administrative
     * access
     */
    @Override
    public boolean allowPut() {
        
        if (getManager().checkAuthenticationForAdminRole()==false)
            return false;
        
        String providerName;
        try {
            providerName = getManager().loadMasterPasswordConfig().getProviderName();
            return getManager().loadMasterPassswordProviderConfig(providerName).isReadOnly()==false;
        } catch (IOException e) {
            throw new RestletException("Cannot calculate if PUT is allowed", 
                    Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e);
        }        
    }


    @Override
    public void handleGet() {
        if (getManager().checkAuthenticationForAdminRole()==false)
            throw new RestletException("Amdinistrative privelges required", Status.CLIENT_ERROR_FORBIDDEN);

        super.handleGet();
    }
    
    @Override
    public Map getMap() throws Exception {

        
        char[] masterpw = getManager().getMasterPasswordForREST();
        Map m = new HashMap();
        m.put(MP_CURRENT_KEY, new String(masterpw));
        getManager().disposePassword(masterpw);        
        return m;
    }
    
    @Override
    protected void putMap(Map map) throws Exception {
        putMap=map;
    }
    
    /**
     * Trigger a master password change
     */
    @Override
    public void handlePut() {
        super.handlePut();
        
        String current = (String) putMap.get(MP_CURRENT_KEY);
        String newpass = (String) putMap.get(MP_NEW_KEY);
        
        if (StringUtils.isNotBlank(current)==false)
            throw new RestletException("no master password", 
                    Status.CLIENT_ERROR_BAD_REQUEST);
        
        if (StringUtils.isNotBlank(newpass)==false)
            throw new RestletException("no master password", 
                    Status.CLIENT_ERROR_BAD_REQUEST);
        
        char[] currentArray = current.trim().toCharArray();
        char[] newpassArray = newpass.trim().toCharArray();
        
        
        GeoServerSecurityManager m = getManager();
        try {
            m.saveMasterPasswordConfig(m.loadMasterPasswordConfig(), currentArray, 
                    newpassArray, newpassArray);
        } catch (Exception e) {
            throw new RestletException("Cannot change master password", 
                    Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e);
        } finally {
            m.disposePassword(currentArray);
            m.disposePassword(newpassArray);
        }
    }

}
