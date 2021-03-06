/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.phoops.geoserver.ols.routing.otp.component;

import org.apache.wicket.model.IModel;

public class OTPTabFactory {
    private static OTPTabFactory factory = null;
    
    private OTPTab instance = null;
    
    public static OTPTabFactory getOTPTabFactory() {
        if (factory == null) {
            factory = new OTPTabFactory();
        }
    
        return factory;
    }
    
    public OTPTab getOTPTab(IModel<String> title) {
        if (instance == null) {
            instance = new OTPTab(title);
        }
    
        return instance;
    }
}
