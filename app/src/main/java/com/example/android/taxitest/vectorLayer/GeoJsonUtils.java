package com.example.android.taxitest.vectorLayer;

import android.graphics.Color;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.Style;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

//import org.json.simple.parser.JSONParser;


public class GeoJsonUtils {


//    public JSONObject loadGeoJson(InputStream is){
//        JSONObject geoJson=new JSONObject();
//        JSONParser jsonParser = new JSONParser();
//        try {
//            geoJson = (JSONObject) jsonParser.parse(
//                    new InputStreamReader(is, StandardCharsets.UTF_8));
//
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return geoJson;
//    }


    static Style.Builder sb = Style.builder()
            .fillColor(Color.RED)
            .strokeColor(Color.RED)
            .strokeWidth(3)
            .fillAlpha(0.5f);

    static Style mStyle = sb.build();

    public static FeatureCollection loadFeatureCollection(InputStream is) {
        try {
            FeatureCollection fc = new ObjectMapper().readValue(is,FeatureCollection.class);
            return fc;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void addSingleBarrio(BarriosLayer barriosLayer, Feature feature) {
            //barrio identifying data
            String title = feature.getProperty("name");
            String idStr = (String)feature.getProperty("@id");
            int id=Integer.valueOf(idStr.replaceAll("\\D+",""));
            int color = Color.parseColor((String)feature.getProperty("fill"));
            //coordinates
            Polygon geom = (Polygon) feature.getGeometry();
            List<LngLatAlt> coords = geom.getExteriorRing();
            //set style
            Style style=sb.fillColor(color).strokeColor(Color.BLACK).build();
            //add barrio
            barriosLayer.addBarrio(new BarrioPolygonDrawable(GeoJsonUtils.convertToGeo(coords), style, title,id));

    }

    public static void addBarrios(BarriosLayer barriosLayer, FeatureCollection features){
        for (int i=0;i<features.getFeatures().size();i++){
            if (features.getFeatures().get(i).getGeometry() instanceof Polygon) {
                addSingleBarrio(barriosLayer,features.getFeatures().get(i));
            }
        }
    }

    public HexagonQuadrantDrawable createSingleHex(Feature feature){
        String quadrantId= feature.getProperty("id");
        String neigh = (String)feature.getProperty("neighbors");
        String[] neighbors=neigh.replace("[","").replace("]","")
                .replace("\"","").split(",");
        int bit=feature.getProperty("bite");
        //coordinates
        Polygon geom = (Polygon) feature.getGeometry();
        List<LngLatAlt> coords = geom.getExteriorRing();
        return new HexagonQuadrantDrawable(convertToGeo(coords),quadrantId,neighbors,bit);

    }


    public static List<GeoPoint> convertToGeo(List<LngLatAlt> lnglatList){
        List<GeoPoint> result=new ArrayList<GeoPoint>();
        for (int i=0;i<lnglatList.size();i++){
            LngLatAlt lnglat=lnglatList.get(i);
            result.add(new GeoPoint(lnglat.getLatitude(),lnglat.getLongitude()));
        }
        return result;
    }



}
