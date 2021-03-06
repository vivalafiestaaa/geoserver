/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.phoops.geoserver.ols.routing.otp;

import it.phoops.geoserver.ols.OLSAbstractServiceProvider;
import it.phoops.geoserver.ols.OLSException;
import it.phoops.geoserver.ols.OLSService;
import it.phoops.geoserver.ols.routing.Language;
import it.phoops.geoserver.ols.routing.RoutingServiceProvider;
import it.phoops.geoserver.ols.routing.otp.client.ns0.AbsoluteDirection;
import it.phoops.geoserver.ols.routing.otp.client.ns0.EncodedPolylineBean;
import it.phoops.geoserver.ols.routing.otp.client.ns0.Itinerary;
import it.phoops.geoserver.ols.routing.otp.client.ns0.Leg;
import it.phoops.geoserver.ols.routing.otp.client.ns0.Leg.Steps;
import it.phoops.geoserver.ols.routing.otp.client.ns0.PlannerError;
import it.phoops.geoserver.ols.routing.otp.client.ns0.RelativeDirection;
import it.phoops.geoserver.ols.routing.otp.client.ns0.Response;
import it.phoops.geoserver.ols.routing.otp.client.ns0.TripPlan;
import it.phoops.geoserver.ols.routing.otp.client.ns0.TripPlan.Itineraries;
import it.phoops.geoserver.ols.routing.otp.client.ns0.WalkStep;
import it.phoops.geoserver.ols.routing.otp.component.OTPTab;
import it.phoops.geoserver.ols.routing.otp.component.OTPTabFactory;
import it.phoops.geoserver.ols.util.SRSTransformer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import net.opengis.www.xls.AbstractLocationType;
import net.opengis.www.xls.DetermineRouteRequestType;
import net.opengis.www.xls.DetermineRouteResponseType;
import net.opengis.www.xls.DistanceType;
import net.opengis.www.xls.DistanceUnitType;
import net.opengis.www.xls.EnvelopeType;
import net.opengis.www.xls.LineStringType;
import net.opengis.www.xls.ObjectFactory;
import net.opengis.www.xls.Pos;
import net.opengis.www.xls.PositionType;
import net.opengis.www.xls.RouteGeometryType;
import net.opengis.www.xls.RouteInstruction;
import net.opengis.www.xls.RouteInstructionsListType;
import net.opengis.www.xls.RouteInstructionsRequest;
import net.opengis.www.xls.RoutePlan;
import net.opengis.www.xls.RoutePreferenceType;
import net.opengis.www.xls.RouteSummaryType;
import net.opengis.www.xls.WayPointList;
import net.opengis.www.xls.WayPointType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.config.ServiceInfo;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.util.PolylineEncoder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class OTPServiceProvider extends OLSAbstractServiceProvider implements RoutingServiceProvider, Serializable {

    private final Log logger = LogFactory.getLog(getClass());
    
    public static final double EPSILON = 1.0;
    
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    //Properties Name
    private static final String  PN_ENDPOINT_ADDRESS    = "OLS.serviceProvider.geocoding.otp.service.endpointAddress";
    private static final String  PN_NAVIGATION_INFO     = "OLS.serviceProvider.geocoding.otp.service.navigationInfo";
    private static final String  PN_NAVIGATION_S_INFO   = "OLS.serviceProvider.geocoding.otp.service.navigationShortInfo";
    private static final String  PN_NAVIGATION_REL      = "OLS.serviceProvider.geocoding.otp.service.navigationInfo.relative";
    private static final String  PN_LANGUAGE_INFO       = "OLS.serviceProvider.geocoding.otp.service.languageInfo";
    private static final String  PN_ACTIVE_SERVICE      = "OLS.serviceProvider.service.active";
    
    private static final String OTP_CRS = "EPSG:4326";
    
    private String      descriptionKey;
    private Properties  properties = new Properties();

    private CoordinateReferenceSystem   otpCrs = null;
    
    private CoordinateReferenceSystem getOTPCrs() throws OLSException
    {
        CoordinateReferenceSystem       retval = otpCrs;
        
        if (retval == null) {
            synchronized (this) {
                retval = otpCrs;
                
                if (retval == null) {
                    try {
                        retval = otpCrs = CRS.decode(OTP_CRS);
                    } catch (NoSuchAuthorityCodeException e) {
                        throw new OLSException("Unknown authority in SRS", e);
                    } catch (FactoryException e) {
                        throw new OLSException("Factory exception converting SRS", e);
                    }
                }
            }
        }
        
        return retval;
    }
    
    @Override
    public String getDescriptionKey() {
        return descriptionKey;
    }
    
    public void setDescriptionKey(String description) {
        this.descriptionKey = description;
    }
    
    public String getEndpointAddress() {
        return properties.getProperty(PN_ENDPOINT_ADDRESS);
    }

    public void setEndpointAddress(String endpointAddress) {
        properties.setProperty(PN_ENDPOINT_ADDRESS, endpointAddress);
    }
    
    public String getNavigationInfo() {
        return properties.getProperty(PN_NAVIGATION_INFO);
    }

    public void setNavigationInfo(String navigationInfo) {
        properties.setProperty(PN_NAVIGATION_INFO, navigationInfo);
    }
    
    public String getNavigationInfoShort() {
        return properties.getProperty(PN_NAVIGATION_S_INFO);
    }
    
    public void setNavigationInfoShort(String navigationInfoShort){
        properties.setProperty(PN_NAVIGATION_S_INFO, navigationInfoShort);
    }
    
    public String getNavigationInfoRel(){
        return properties.getProperty(PN_NAVIGATION_REL);
    }
    
    public void setNavigationInfoRel(String navigationInfoRel){
        properties.setProperty(PN_NAVIGATION_REL, navigationInfoRel);
    }
    
    public String getActive(){
        return properties.getProperty(PN_ACTIVE_SERVICE);
    }
    
    public void setActive(String activeService){
        properties.setProperty(PN_ACTIVE_SERVICE, activeService);
    }
    
    public String getLanguage(){
        return properties.getProperty(PN_LANGUAGE_INFO);
    }
    
    public void setLanguage(String language){
        properties.setProperty(PN_LANGUAGE_INFO, language);
    }
    
    @Override
    public Properties getProperties() {
        return properties;
    }
    
    @Override
    public OLSService getServiceType() {
        return OLSService.ROUTING_NAVIGATION;
    }

    @Override
    public ITab getTab() {
        IModel<String> title = new ResourceModel("OTP ", "OTP");
        return OTPTabFactory.getOTPTabFactory().getOTPTab(title);
    }

    @Override
    public void handleServiceChange(ServiceInfo service,
            List<String> propertyNames, List<Object> oldValues,
            List<Object> newValues) {
        String url              = ((OTPTab)getTab()).getUrlOTP();
        String navigation       = ((OTPTab)getTab()).getNavigationInfoOTP();
        String navigationS      = ((OTPTab)getTab()).getNavigationInfoShortOTP();
        String navigationR      = ((OTPTab)getTab()).getNavigationInfoRelOTP();
        String language         = ((OTPTab)getTab()).getSelectedLanguage().getCode();
        String active           = ((OTPTab)getTab()).getActiveOTP();
        
        setEndpointAddress(url);
        setNavigationInfo(navigation);
        setNavigationInfoShort(navigationS);
        setNavigationInfoRel(navigationR);
        setLanguage(language);
        setActive(active);
        
    }

    @Override
    public boolean isServiceActive() {
        return Boolean.parseBoolean(this.getActive());
    }

    @Override
    public void setPropertiesTab(ITab otpTab) {
        ((OTPTab)otpTab).setUrlOTP(this.getEndpointAddress());
        ((OTPTab)otpTab).setActiveOTP(this.getActive());
        ((OTPTab)otpTab).setNavigationInfoOTP(this.getNavigationInfo());
        ((OTPTab)otpTab).setNavigationInfoShortOTP(this.getNavigationInfoShort());
        ((OTPTab)otpTab).setNavigationInfoRelOTP(this.getNavigationInfoRel());
        Language language = Language.get(this.getLanguage());
        if(language == null){
            ((OTPTab)otpTab).setCodeLanguageSelected(1);
        }else{
            ((OTPTab)otpTab).setCodeLanguageSelected(Integer.parseInt(language.getCode()));
        }
    }

    @Override
    public JAXBElement<DetermineRouteResponseType> route(DetermineRouteRequestType input, String lang, String srsName) throws OLSException
    {
        JAXBElement<DetermineRouteResponseType> retval = null;
        DatatypeFactory                         df;
        
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new OLSException("Datatype factory configuration exception", e);
        }
        
        // Parse request parameters
        RoutePlan routePlan = input.getRoutePlan();
        
        if (routePlan == null) {
            throw new OLSException("Route plan is missing");
        }
        
        WayPointList                                    wpList = routePlan.getWayPointList();
        WayPointType                                    startPoint = wpList.getStartPoint();
        JAXBElement<? extends AbstractLocationType>     startLocation = startPoint.getLocation();
        
        if (!(startLocation.getValue() instanceof PositionType)) {
            throw new OLSException("Unsupported start point location");
        }
        
        PositionType                                    startPosition = (PositionType)startLocation.getValue();
        String                                          declaredSrs;
        
        if (srsName != null) {
            declaredSrs = srsName;
        } else {
            if (startPosition.getPoint().getPos().getSrsName() != null) {
                declaredSrs = startPosition.getPoint().getPos().getSrsName();
            } else {
                declaredSrs = "EPSG:4326";
            }
        }
        
        WayPointType                                    endPoint = wpList.getEndPoint();
        JAXBElement<? extends AbstractLocationType>     endLocation = endPoint.getLocation();
        
        if (!(endLocation.getValue() instanceof PositionType)) {
            throw new OLSException("Unsupported end point location");
        }

        PositionType            endPosition = (PositionType)endLocation.getValue();
        List<WayPointType>      viaPointsList = wpList.getViaPoints();
        List<PositionType>      viaPosition = new ArrayList<PositionType>();
        
        if (viaPointsList.size() != 0) {
            for (WayPointType viaPoint : viaPointsList) {
                JAXBElement<? extends AbstractLocationType>      viaLocation = viaPoint.getLocation();
                viaPosition.add((PositionType)viaLocation.getValue());
            }
        }
        
        XMLGregorianCalendar    cal = routePlan.getExpectedEndTime();
        boolean                 endTime = true;
        
        if (cal == null) {
            cal = routePlan.getExpectedStartTime();
            endTime = false;
            
            if (cal == null) {
                cal = df.newXMLGregorianCalendar(new GregorianCalendar());
            }
        }
        
        RoutePreferenceType     preference = routePlan.getRoutePreference();
        
        if (routePlan.isUseRealTimeTraffic()) {
            throw new OLSException("Use of real time traffic information is unsupported");
        }
        
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat timeFormat = new SimpleDateFormat("hh:mm a");
        
        RouteInstructionsRequest        instructionRequest = input.getRouteInstructionsRequest();
        String                          format = "text/html";
        
        if (instructionRequest != null) {
            format = instructionRequest.getFormat();
        }

        // Build RESTful OPT request
        Client  client = Client.create();
        
//        client.setConnectTimeout(interval);
//        client.setReadTimeout(interval);
        
        MultivaluedMap<String,String>  queryParams = new MultivaluedMapImpl();
        
        queryParams.add("fromPlace",    formatPosition(startPosition));
        if (viaPosition != null) {
            queryParams.add("intermediatePlacesOrdered", "true");
            
            for (PositionType via : viaPosition) {
                queryParams.add("intermediatePlaces", formatPosition(via));
            }
        }
        
        queryParams.add("toPlace",      formatPosition(endPosition));
        queryParams.add("date",         dateFormat.format(cal.toGregorianCalendar().getTime()));
        queryParams.add("time",         timeFormat.format(cal.toGregorianCalendar().getTime()));
        queryParams.add("arriveBy",     Boolean.toString(endTime));
        
        switch (preference) {
        case FASTEST:
            queryParams.add("optimize", "QUICK");
//            queryParams.add("mode",     "CAR,CUSTOM_MOTOR_VEHICLE"); -> non funziona con la versione 0.9.2-SNAPSHOT
            queryParams.add("mode",     "CAR");
            break;
        case SHORTEST:
            queryParams.add("optimize", "QUICK"); // FIXME ???
//            queryParams.add("mode",     "CAR,CUSTOM_MOTOR_VEHICLE"); -> non funziona con la versione 0.9.2-SNAPSHOT
            queryParams.add("mode",     "CAR");
            break;
        case PEDESTRIAN:
            queryParams.add("optimize", "QUICK"); // FIXME ???
            queryParams.add("mode",     "WALK");
            break;
        }
        
        Locale  locale = null;
        
        if (lang != null) {
            locale = Locale.forLanguageTag(lang);
        }
        
        if (locale == null) {
            String languageInfo = properties.getProperty(PN_LANGUAGE_INFO);
            
            if (languageInfo.equals("1")) {
                locale = Locale.ITALIAN;
            } else if (languageInfo.equals("2")) {
                locale = Locale.ENGLISH;
            }
        }
        
        ResourceBundle  messages = ResourceBundle.getBundle("GeoServerApplication", locale);
        WebResource     resource = client.resource(getEndpointAddress()).path("ws/plan");
        Response        response = resource.queryParams(queryParams).accept(MediaType.TEXT_XML).get(Response.class);

        // Parse OTP Response
        PlannerError    error = response.getError();
        
        if (error != null) {
            throw new OLSException("OTP planner error"); // FIXME articulate
        }
        
        TripPlan        tripPlan = response.getPlan();
        
        if (tripPlan == null) {
            throw new OLSException("Missing OTP trip plan");
        }
        
        Itineraries     itineraries = tripPlan.getItineraries();
        
        if (itineraries == null) {
            throw new OLSException("Missing OTP itineraries");
        }
        
        List<Itinerary> itineraryList = itineraries.getItinerary();
        
        if (itineraryList == null) {
            throw new OLSException("Missing OTP itinerary list");
        }
        
        Itinerary       itinerary = itineraryList.get(0);
        
        if (itinerary == null) {
            throw new OLSException("Missing OTP itinerary");
        }
        
        ObjectFactory                   of = new ObjectFactory();
        DetermineRouteResponseType      determineRouteResponse = of.createDetermineRouteResponseType();
        RouteSummaryType                routeSummary = of.createRouteSummaryType();
        RouteInstructionsListType       routeInstructions = of.createRouteInstructionsListType();
        
        routeSummary.setTotalTime(df.newDuration(itinerary.getDuration()));
        
        double                  totalDistance = 0.0;
        EncodedPolylineBean     legGeometry;
        Geometry                geometry = null;
//        Geometry                geometryComplete = null;
        GeometryFactory         gf = new GeometryFactory();
        Steps                   legSteps;
        RouteInstruction        routeInstruction;
        DistanceType            distance;
//        ArrayList<Geometry>     geometrySegmentList = new ArrayList<Geometry>();
        Coordinate[]            legCoordiantesArray;
        RouteGeometryType       routeGeometry = null;
        LineStringType          lineStringType;
        List<Pos>               posList;
        Coordinate              transformedCoords;
        Pos                     posInstruction;
        List<Double>            posValues;
        int                     index = 0;
        int                     oldIndex = 0;
        double                  factor;
        
        for (Leg leg : itinerary.getLegs().getLeg()) {
            totalDistance += leg.getDistance();
            legGeometry = leg.getLegGeometry();
            
            legCoordiantesArray = decode(legGeometry).toArray(new Coordinate[0]);
            
            if (geometry == null) {
                geometry = gf.createLineString(legCoordiantesArray);
            } else {
                geometry = geometry.union(gf.createLineString(legCoordiantesArray));
            }
            legSteps = leg.getSteps();
            
            routeGeometry = null;
            index = 0;
            oldIndex = 0;
            
            for (WalkStep walkStep : legSteps.getWalkSteps()) {
                routeInstruction = of.createRouteInstruction();
                
                distance = of.createDistanceType();
                BigDecimal bdValue = BigDecimal.valueOf(walkStep.getDistance() * 0.001);
                bdValue = bdValue.setScale(2, BigDecimal.ROUND_DOWN);
                
                factor = DEGREES_TO_METERS_FACTOR * Math.cos(walkStep.getLat() * DEGREES_TO_RADIANS_FACTOR);
                oldIndex = index;
                
                for (; index < legCoordiantesArray.length; index++) {
                    if (Math.abs((legCoordiantesArray[index].x - walkStep.getLat())*factor) < EPSILON && Math.abs((legCoordiantesArray[index].y - walkStep.getLon())*factor) < EPSILON) {
                        break;
                    }
                }
                
                if (routeGeometry != null) {
                    // oldIndex -> index - 1
                    
                    lineStringType = of.createLineStringType();
                    lineStringType.setSrsName(declaredSrs);
                    posList = lineStringType.getPos(); 
                    
                    for (int i = oldIndex ; i <= index; i++) {
                        posInstruction = new Pos();
                        posValues = posInstruction.getValues();
                        
                        if (!OTP_CRS.equals(declaredSrs)) {
                            transformedCoords = SRSTransformer.transform(legCoordiantesArray[i].y, legCoordiantesArray[i].x, getOTPCrs(), declaredSrs);
                            posValues.add(transformedCoords.x);
                            posValues.add(transformedCoords.y);
                        } else {
                            posValues.add(legCoordiantesArray[i].x);
                            posValues.add(legCoordiantesArray[i].y);
                        }
                        
                        posList.add(posInstruction);
                    }
                    
                    routeGeometry.setLineString(lineStringType);
                }
                
                distance.setValue(bdValue);
                routeInstruction.setDistance(distance);
                
                String relativeDirection = null;
                String absoluteDirection = null;
                if (walkStep.getRelativeDirection() != null) {
                    relativeDirection = messages.getString(walkStep.getRelativeDirection().toString());
                } else {
                    absoluteDirection = messages.getString(walkStep.getAbsoluteDirection().toString());
                }
                
                String resultFormatter = "";
                if (walkStep.getStreetName() != null || !walkStep.getStreetName().equalsIgnoreCase("")) {
                    if (relativeDirection != null) {
                        resultFormatter = MessageFormat.format(properties.getProperty(PN_NAVIGATION_REL), relativeDirection, walkStep.getStreetName(), bdValue);
                        
                        if ("text/html".equals(format)) {
                            String imgRel = walkStep.getRelativeDirection().toString().toLowerCase();
                            resultFormatter = "<span class='GeoServerOpenLS "+imgRel+"'></span><span class='GeoServerOpenLS routingInstruction'>"+resultFormatter+"</span>";
                        }
                    } else {
                        resultFormatter = MessageFormat.format(properties.getProperty(PN_NAVIGATION_INFO), absoluteDirection, bdValue, walkStep.getStreetName());
                        
                        if ("text/html".equals(format)) {
                            resultFormatter = "<span class='GeoServerOpenLS routingInstruction'>" + resultFormatter + "</span>";
                        }
                    }
                } else {
                    if (relativeDirection != null) {
                        resultFormatter = MessageFormat.format(properties.getProperty(PN_NAVIGATION_REL), relativeDirection, walkStep.getStreetName(), bdValue);
                        
                        if ("text/html".equals(format)) {
                            String imgRel = walkStep.getRelativeDirection().toString().toLowerCase();
                            resultFormatter = "<span class='GeoServerOpenLS "+imgRel+"'></span><span class='GeoServerOpenLS routingInstruction'>"+resultFormatter+"</span>";
                        }
                    } else {
                        resultFormatter = MessageFormat.format(properties.getProperty(PN_NAVIGATION_S_INFO), absoluteDirection, bdValue);
                        
                        if ("text/html".equals(format)) {
                            resultFormatter = "<span class='GeoServerOpenLS routingInstruction'>" + resultFormatter + "</span>";
                        }
                    }
                }
                
                routeInstruction.setInstruction(resultFormatter);
                routeGeometry = of.createRouteGeometryType();
                routeInstruction.setRouteInstructionGeometry(routeGeometry);
                routeInstructions.getRouteInstructions().add(routeInstruction);
            }
            
            //Aggiungi l'ultima tratta del percorso
            lineStringType = of.createLineStringType();
            lineStringType.setSrsName(declaredSrs);
            posList = lineStringType.getPos(); 
            
            for (int i = index ; i < legCoordiantesArray.length; i++) {
                posInstruction = new Pos();
                posValues = posInstruction.getValues();
                
                if (!OTP_CRS.equals(declaredSrs)) {
                    transformedCoords = SRSTransformer.transform(legCoordiantesArray[i].y, legCoordiantesArray[i].x, getOTPCrs(), declaredSrs);
                    posValues.add(transformedCoords.x);
                    posValues.add(transformedCoords.y);
                } else {
                    posValues.add(legCoordiantesArray[i].x);
                    posValues.add(legCoordiantesArray[i].y);
                }
                
                posList.add(posInstruction);
            }
            
            routeGeometry.setLineString(lineStringType);
        }
        
        distance = of.createDistanceType();
        distance.setValue(BigDecimal.valueOf(totalDistance));
        distance.setAccuracy(BigDecimal.ONE); // FIXME
        distance.setUom(DistanceUnitType.M); // FIXME ???
        routeSummary.setTotalDistance(distance);
        
        RouteGeometryType       fullRouteGeometry = of.createRouteGeometryType();
        LineStringType          lineString = of.createLineStringType();
        Pos                     pos;
        double                  minX = Double.POSITIVE_INFINITY;
        double                  maxX = Double.NEGATIVE_INFINITY;
        double                  minY = Double.POSITIVE_INFINITY;
        double                  maxY = Double.NEGATIVE_INFINITY;
        
        lineString.setSrsName(declaredSrs);
        posList = lineString.getPos();
        
        for (Coordinate coord : geometry.getCoordinates()) {
            pos = new Pos();
            posValues = pos.getValues();
            
            if (!OTP_CRS.equals(declaredSrs)) {
                transformedCoords = SRSTransformer.transform(coord.y, coord.x, getOTPCrs(), declaredSrs);
                posValues.add(transformedCoords.x);
                posValues.add(transformedCoords.y);
            } else {
                posValues.add(coord.x);
                posValues.add(coord.y);
            }
            
            posList.add(pos);
            
            // Bounding box extraction:
            if (posValues.get(0) < minX) {
                minX = posValues.get(0);
            }
            if (posValues.get(1) < minY) {
                minY = posValues.get(1);
            }
            if (posValues.get(0) > maxX) {
                maxX = posValues.get(0);
            }
            if (posValues.get(1) > maxY) {
                maxY = posValues.get(1);
            }
        }
        
        fullRouteGeometry.setLineString(lineString);
        
        EnvelopeType    boundingBox = of.createEnvelopeType();
        
        boundingBox.setSrsName(declaredSrs);
        posList = boundingBox.getPos();
        
        pos = new Pos();
        posValues = pos.getValues();
        posValues.add(minX);
        posValues.add(minY);
        posList.add(pos);
        
        pos = new Pos();
        posValues = pos.getValues();
        posValues.add(maxX);
        posValues.add(maxY);
        posList.add(pos);
        
        routeSummary.setBoundingBox(boundingBox);
        
        determineRouteResponse.setRouteSummary(routeSummary);
        determineRouteResponse.setRouteGeometry(fullRouteGeometry);
        determineRouteResponse.setRouteInstructionsList(routeInstructions);
        
        retval = of.createDetermineRouteResponse(determineRouteResponse);
        
        return retval;
    }
    
    private String formatPosition(PositionType position) throws OLSException
    {
        StringBuffer    sb = new StringBuffer();
        Pos             pos = position.getPoint().getPos();
        String          srsName = pos.getSrsName();
        
        if (!OTP_CRS.equals(srsName)) {
            List<Double>        coordinates = pos.getValues();
            Coordinate          coords = SRSTransformer.transform(coordinates.get(0), coordinates.get(1), srsName, getOTPCrs());
            
            sb.append(coords.y).
               append(",").
               append(coords.x);
        } else {
            for (Double coord : pos.getValues()) {
                if (sb.length() != 0) {
                    sb.append(",");
                }
                
                sb.append(coord);
            }
        }
        
        return sb.toString();
    }
    
    private static List<Coordinate> decode(EncodedPolylineBean polyline)
    {
        String pointString = polyline.getPoints();

        double lat = 0;
        double lon = 0;

        int strIndex = 0;
        List<Coordinate> points = new ArrayList<Coordinate>();

        while (strIndex < pointString.length()) {
            int[] rLat = PolylineEncoder.decodeSignedNumberWithIndex(pointString, strIndex);
            lat = lat + rLat[0] * 1e-5;
            strIndex = rLat[1];

            int[] rLon = PolylineEncoder.decodeSignedNumberWithIndex(pointString, strIndex);
            lon = lon + rLon[0] * 1e-5;
            strIndex = rLon[1];

            points.add(new Coordinate(lat, lon));
        }

        return points;
    }
    
    public RelativeDirection windRoseInformation(AbsoluteDirection direction1, AbsoluteDirection direction2)
    {
        return null;
    }
    
    public static RelativeDirection getRelativeDirection(double lastAngle, double thisAngle, boolean roundabout) {

        double angleDiff = thisAngle - lastAngle;
        if (angleDiff < 0) {
            angleDiff += Math.PI * 2;
        }
        double ccwAngleDiff = Math.PI * 2 - angleDiff;

        if (roundabout) {
            // roundabout: the direction we turn onto it implies the circling direction
            if (angleDiff > ccwAngleDiff) {
                return RelativeDirection.CIRCLE_CLOCKWISE; 
            } else {
                return RelativeDirection.CIRCLE_COUNTERCLOCKWISE;
            }            
        }

        // less than 0.3 rad counts as straight, to simplify walking instructions
        if (angleDiff < 0.3 || ccwAngleDiff < 0.3) {
            return RelativeDirection.CONTINUE;
        } else if (angleDiff < 0.7) {
            return RelativeDirection.SLIGHTLY_RIGHT;
        } else if (ccwAngleDiff < 0.7) {
            return RelativeDirection.SLIGHTLY_LEFT;
        } else if (angleDiff < 2) {
            return RelativeDirection.RIGHT;
        } else if (ccwAngleDiff < 2) {
            return RelativeDirection.LEFT;
        } else if (angleDiff < Math.PI) {
            return RelativeDirection.HARD_RIGHT;
        } else {
            return RelativeDirection.HARD_LEFT;
        }
    }
}
