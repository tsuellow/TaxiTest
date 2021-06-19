package com.dale.viaje.nicaragua.vtmExtension;

import com.dale.viaje.nicaragua.City;
import com.dale.viaje.nicaragua.R;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;

public class CitySupport {
    ArrayList<City> cityList=new ArrayList<>();

    public CitySupport(){
        City esteli=new City("esteli","Estelí",R.raw.barrios_esteli,R.raw.quadrants_esteli,
                new GeoPoint(13.210772,-86.269176),new GeoPoint(13.025877,-86.445336));
        City leon=new City("leon","León",R.raw.barrios_leon,R.raw.quadrants_leon,
                new GeoPoint(12.544518,-86.783017),new GeoPoint(12.381017,-86.959163));

        cityList.add(esteli);
        cityList.add(leon);
    }

    public ArrayList<String> getCityDropdown(){
        ArrayList<String> list=new ArrayList<>();
        for (City city:cityList){
            list.add(city.prettyName);
        }
        return  list;
    }

    public City getCityByName(String name){
        for (City city:cityList){
            if (city.name.contentEquals(name) ||city.prettyName.contentEquals(name)){
                return city;
            }
        }
        return null;
    }
}
