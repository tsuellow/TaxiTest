package com.example.android.taxitest;

import android.content.Context;

import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.connection.WebSocketDriverLocations;
import com.example.android.taxitest.vectorLayer.BarriosLayer;
import com.example.android.taxitest.vectorLayer.ConnectionLineLayer2;
import com.example.android.taxitest.vtmExtension.OtherTaxiLayer;
import com.example.android.taxitest.vtmExtension.TaxiMarker;

import org.oscim.map.Map;

import java.util.List;

public class OtherDriversLayer extends OtherTaxiLayer {
    public OtherDriversLayer(Context context, BarriosLayer barriosLayer, Map map, List<TaxiMarker> list, WebSocketDriverLocations webSocketConnection, ConnectionLineLayer2 connectionLineLayer, CommunicationsAdapter commsAdapter) {
        super(context, barriosLayer, map, list, webSocketConnection, connectionLineLayer, commsAdapter);
    }


}
