/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.xml.transform.TransformerException;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.WMS;

/**
 * A GetFeatureInfo response handler specialized in producing GML 3 data for a GetFeatureInfo
 * request.
 * 
 * <p>
 * This class does not deals directly with GML encoding. Instead, it works by taking the
 * FeatureResults produced in <code>execute()</code> and constructs a <code>GetFeaturesResult</code>
 * wich is passed to a <code>GML2FeatureResponseDelegate</code>, as if it where the result of a
 * GetFeature WFS request.
 * </p>
 * 
 * @author carlo cancellieri - GeoSolutions
 */
public class XMLDescribeLayerResponse extends DescribeLayerResponse {

    public static final String DESCLAYER_MIME_TYPE = "application/vnd.ogc.wms_xml";

    protected final WMS wms;

    /**
     * Constructor for subclasses
     */
    public XMLDescribeLayerResponse(final WMS wms) {
        super(DESCLAYER_MIME_TYPE);
        this.wms = wms;
    }

    public XMLDescribeLayerResponse(final WMS wms, String type) {
        super(type);
        this.wms = wms;
    }

    /**
     * Takes the <code>FeatureResult</code>s generated by the <code>execute</code> method in the
     * superclass and constructs a <code>DescribeLayerResult</code> which is passed to a
     * <code>GML2FeatureResponseDelegate</code>.
     * 
     * @see AbstractFeatureInfoResponse#writeTo(OutputStream)
     */
    @Override
    public void write(DescribeLayerModel results, DescribeLayerRequest request, OutputStream out)
            throws ServiceException, IOException {
        DescribeLayerTransformer transformer;
        transformer = new DescribeLayerTransformer(request.getBaseUrl());
        Charset encoding = wms.getCharSet();
        transformer.setEncoding(encoding);
        if (wms.getGeoServer().getSettings().isVerbose()) {
            transformer.setIndentation(2);
        }

        try {
            transformer.transform(request, out);
            out.flush();
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
    }

}
