package com.dale.viaje.nicaragua.vectorLayer;

import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.PolygonDrawable;

import java.util.List;

public class HexagonQuadrantDrawable extends PolygonDrawable {

    private String quadrantId;
    private String[] neighbors;
    private int bit;

    public HexagonQuadrantDrawable(List<GeoPoint> points, String quadrantId, String[] neighbors, int bit){
        super(points);
        this.quadrantId=quadrantId;
        this.neighbors=neighbors;
        this.bit=bit;
    }

    public String getQuadrantId() {
        return quadrantId;
    }

    public void setQuadrantId(String quadrantId) {
        this.quadrantId = quadrantId;
    }

    public String[] getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(String[] neighbors) {
        this.neighbors = neighbors;
    }

    public int getBit() {
        return bit;
    }

    public void setBit(int bit) {
        this.bit = bit;
    }
}
