/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.phoops.geoserver.ols.geocoding.solr.component;

import org.apache.wicket.model.IModel;

public class SOLRTabFactory {
    private static SOLRTabFactory factory = null;

    private SOLRTab instance = null;

    public static SOLRTabFactory getSOLRTabFactory() {
        if (factory == null) {
            factory = new SOLRTabFactory();
        }

        return factory;
    }

    public SOLRTab getSOLRTab(IModel<String> title) {
        if (instance == null) {
            instance = new SOLRTab(title);
        }

        return instance;
    }
}
