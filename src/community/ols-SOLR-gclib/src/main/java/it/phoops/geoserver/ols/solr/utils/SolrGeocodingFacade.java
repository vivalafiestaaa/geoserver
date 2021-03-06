/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.phoops.geoserver.ols.solr.utils;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;

public interface SolrGeocodingFacade {
    public static final int     MAX_ROWS_ALL            = -1;
    public static final int     MAX_ROWS_SOLR_DEFAULT   = 0;
    
    // HttpSolrServer
    public abstract void setSolrServerURL(String baseURL);
    public abstract void setSolrServerURL(String baseURL, HttpClient client);
    public abstract void setSolrServerURL(String baseURL, HttpClient client, ResponseParser parser);
    
    // CloudSolrServer
    public abstract void setSolrServerHost(String zkHost) throws SolrGeocodingFacadeException;
    public abstract void setSolrServerHost(String zkHost, LBHttpSolrServer lbServer);
    public abstract void setSolrServerHost(String zkHost, LBHttpSolrServer lbServer, boolean updatesToLeaders);
    
    // ConcurrentUpdateSolrServer
    public abstract void setSolrServerUrl(String solrServerUrl, int queueSize, int threadCount);
    public abstract void setSolrServerUrl(String solrServerUrl, HttpClient client, int queueSize, int threadCount);
    
    // LBHttpSolrServer
    public abstract void setSolrServerUrls(String... solrServerUrls) throws SolrGeocodingFacadeException;
    public abstract void setSolrServerUrls(HttpClient httpClient, String... solrServerUrls) throws SolrGeocodingFacadeException;
    public abstract void setSolrServerUrls(HttpClient httpClient, ResponseParser parser, String... solrServerUrls) throws SolrGeocodingFacadeException;
    
    // Parameters
    public abstract void setNumberDelimiter(String numberDelimiter);
    public abstract void setNumberAfterAddress(boolean numberAfterAddress);
    public abstract void setAddressTokenDelim(String addressTokenDelim);
    public abstract void setStreetTypeWeigth(float streetTypeWeigth);
    public abstract void setStreetNameWeigth(float streetNameWeigth);
    public abstract void setNumberSubdivisionSeparator(String numberSubdivisionSeparator);
    public abstract void setNumberWeigth(float numberWeigth);
    public abstract void setMunicipalityWeigth(float municipalityWeigth);
    public abstract void setCountrySubdivisionWeigth(float countrySubdivisionWeigth);
    public abstract void setMaxRows(int maxRows);
    public abstract void setFuzzySearchStreetType(boolean fuzzySearchStreetType);
    public abstract void setFuzzySearchStreetName(boolean fuzzySearchStreetName);
    public abstract void setFuzzySearchNumber(boolean fuzzySearchNumber);
    public abstract void setFuzzySearchMunicipality(boolean fuzzySearchMunicipality);
    public abstract void setFuzzySearchCountrySubdivision(boolean fuzzySearchCountrySubdivision);
    public abstract void setAndNameTerms(boolean andNameTerms);
    
    // Functions
    public abstract SolrBeanResultsList geocodeAddress(String freeFormAddress, String municipality, String countrySubdivision) throws SolrGeocodingFacadeException;
    public abstract SolrBeanResultsList geocodeAddress(String freeFormAddress, String number, String subdivision, String municipality, String countrySubdivision) throws SolrGeocodingFacadeException;
    public abstract SolrBeanResultsList geocodeAddress(String typePrefix, String streetName, String municipality, String countrySubdivision) throws SolrGeocodingFacadeException;
    public abstract SolrBeanResultsList geocodeAddress(String typePrefix, String streetName, String number, String subdivision, String municipality, String countrySubdivision) throws SolrGeocodingFacadeException;

    public abstract SolrBeanResultsList solrQuery(String dug, String address, String number, String municipality, String countrySubdivision) throws SolrServerException, SolrGeocodingFacadeException;


    }
