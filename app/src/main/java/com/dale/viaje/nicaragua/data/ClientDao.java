package com.dale.viaje.nicaragua.data;

import android.util.Log;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.dale.viaje.nicaragua.MainActivity;

import org.oscim.core.GeoPoint;

import java.util.List;

import static com.dale.viaje.nicaragua.utils.MiscellaneousUtils.locToGeo;

@Dao
public abstract class ClientDao {

    //test
    @Query("select count(*) from clientBase")
    public abstract int getCurrentTaxis();

    //populate base table     ------filter out undesirables
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertNewData(List<ClientObject> newData);

    //populate base table     ------filter out undesirables
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertFullData(List<ClientNew> newData);

    //delete myself
    @Query("delete from clientBase where taxiId=:myId")
    abstract void deleteMySelf(int myId);

    public static final int ABOVE=1;
    public static final int BELOW=-1;

    //flag taxis that are active but out of range for disappearance... consider ignoring if too slow
    @Query("update clientBase set isActive=2 where taxiId in (select taxiId from clientNew)") //if data for taxi was received but it is too far away
    abstract void flagForDisappearance();

    //filter out irrelevant taxis, keep empty taxis and clicked taxis
    @Query("delete from clientNew where taxiId not in (:clickedItems) " +  //ensure clicked items are on
            "and (latitude!=0.0 or longitude!=0.0) " +  //ensure empty taxis are on
            "and ((:leftSign*destinationLatitude<:leftSign*(:bLeft+:mLeft*destinationLongitude) " + //destination filter left
            "or :rightSign*destinationLatitude<:rightSign*(:bRight+:mRight*destinationLongitude)) " + //destination filter right
            "or (:blockSign*latitude<:blockSign*(:bBlock+:mBlock*longitude)))") //position filter block: make sure taxis that have already passed by me are not shown
    abstract void doScopeReductionFiltering(double bLeft, double mLeft, double bRight, double mRight, double bBlock, double mBlock, int leftSign, int rightSign, int blockSign, List<Integer> clickedItems);

    //calculate the taxis pseudoDistance
    @Query("update clientNew set pseudoDistance=((:ownLat-latitude)*(:ownLat-latitude)+(:ownLon-longitude)*(:ownLon-longitude))") //this is ok since our pseudoDistance is a monotone transformation of the actual distance
    abstract void calculateDistances(double ownLat, double ownLon);

    //pass 50 closest taxis to taxiBase
    @Query("replace into clientBase(taxiId, latitude, longitude, locationTime, rotation, seats, extra, destinationLatitude, destinationLongitude, isActive) " +
            "select taxiId, latitude, longitude, locationTime, rotation, seats, extra, destinationLatitude, destinationLongitude, isActive " +
            "from clientNew order by pseudoDistance asc limit :lim") //
    abstract void feedCloseTaxis(int lim);

    //make sure active comms taxis are included
    @Query("replace into clientBase(taxiId, latitude, longitude, locationTime, rotation, seats, extra, destinationLatitude, destinationLongitude, isActive) " +
            "select taxiId, latitude, longitude, locationTime, rotation, seats, extra, destinationLatitude, destinationLongitude, case isActive when 2 then 1 else isActive end " +  //setting them to active so we overwrite their status after accepted
            "from clientNew where taxiId in (:clickedItems)")
    abstract void feedOpenComms(List<Integer> clickedItems);

    //clear taxiNew
    @Query(" delete from clientNew where 1=1")
    public abstract void clearTaxiNew();

    //clear taxiOld
    @Query("delete from clientOld where 1=1")
    public abstract void clearTaxiOld();

    //clear taxiBase to be used only on pause/delete
    @Query("delete from clientBase where 1=1")
    public abstract void clearTaxiBase();

    //delete inactive taxis  --------include taxis exiting the V shape i.e. change condition to !=1
    @Query("delete from clientBase where isActive!=1")
    abstract void clearInactiveTaxis();

    //delete inactive taxis that are new
    @Query("delete from clientBase where isActive=0 and taxiId not in (select taxiId from clientOld)")
    abstract void clearInactiveNewTaxis();

    //set old taxis to inactive   ---------include setting taxis to 2 that are filtered out
    @Query("update clientBase set isActive=0 where (:date-locationTime)>60000")
    abstract void setOldTaxisToInactive(long date);

    //move data from taxiBase to taxiStart post execution
    @Query("insert into clientOld select * from clientBase order by taxiId ")
    abstract void populateTaxiOld();


    //ORDER IN WHICH THINGS SHOULD HAPPEN
    SocketFilterClients sf;
    @Transaction
    public void runNewPreOutputTransactions(List<ClientNew> newData, boolean filter, List<Integer> clickedItems, double phi, int limit){
        insertFullData(newData);
        flagForDisappearance();
        if (filter){
            sf = new SocketFilterClients(locToGeo(MainActivity.mMarkerLoc), MainActivity.destGeo, phi);
            doScopeReductionFiltering(sf.bLeft, sf.mLeft, sf.bRight, sf.mRight, sf.bBlock, sf.mBlock, sf.signLeft, sf.signRight, sf.signBlock,clickedItems);
        }
        calculateDistances(locToGeo(MainActivity.mMarkerLoc).getLatitude(),locToGeo(MainActivity.mMarkerLoc).getLongitude());
        feedCloseTaxis(limit);
        feedOpenComms(clickedItems);
        clearTaxiNew();
        clearInactiveNewTaxis();
    }

    //retrieve matching moved or dissapearing taxis
    @Query("select * from clientBase where taxiId in (select taxiId from clientOld) order by taxiId")
    public abstract List<ClientObject> getMatchingTaxiBase();

    //retrieve new taxis to be added
    @Query("select * from clientBase where taxiId not in (select taxiId from clientOld) and isActive=1 order by taxiId")
    public abstract List<ClientObject> getNewTaxis();


    @Transaction
    public void runPostOutputTransactions(long date){
        clearTaxiOld();
        clearInactiveTaxis();
        populateTaxiOld();
        setOldTaxisToInactive(date);
    }

    public class SocketFilterClients {
        public double bRight;
        public double mRight;
        public double bLeft;
        public double mLeft;
        public int signRight;
        public int signLeft;

        public double bBlock;
        public double mBlock;
        public int signBlock;

        public SocketFilterClients(GeoPoint geo, GeoPoint dest, double phi){
            double tinyCorrection=0.0;
            if (dest.getLongitude()-geo.getLongitude()==0.0)
                tinyCorrection=1E-11;

            //v shaped dest filter and block filter
            double deltaLat=dest.getLatitude()-geo.getLatitude();
            double deltaLon=dest.getLongitude()-geo.getLongitude()+tinyCorrection;
            double slope=(deltaLat)/(deltaLon);
            double theta=Math.toDegrees(Math.atan(slope));
            if (deltaLon<0) theta = 180 + theta; //account for inversed vectors
            Log.d("filterS theta",""+theta);


            mLeft=Math.tan(Math.toRadians(theta+phi/2));
            mRight=Math.tan(Math.toRadians(theta-phi/2));
            Log.d("filterS slopeR",""+mRight);
            Log.d("filterS slopeL",""+mLeft);

            bLeft=geo.getLatitude()-mLeft*geo.getLongitude();
            bRight=geo.getLatitude()-mRight*geo.getLongitude();

            if (theta<90-phi/2 || theta>270+phi/2){
                signLeft= BELOW;
                signRight=ABOVE;
            }else if (theta>90-phi/2 && theta<90+phi/2){
                signLeft=ABOVE;
                signRight=ABOVE;
            }else if (theta>90+phi/2 && theta<270-phi/2){
                signLeft=ABOVE;
                signRight=BELOW;
            }else{
                signLeft=BELOW;
                signRight=BELOW;
            }

            //block filter
            double blockDistance=0.002; //more or less 220 mts in front of current position
            double normalizationFactor=blockDistance/Math.sqrt(Math.pow(deltaLat,2)+Math.pow(deltaLon,2));
            double blockIntersectionLat=geo.getLatitude()-normalizationFactor*deltaLat;
            double blockIntersectionLon=geo.getLongitude()-normalizationFactor*deltaLon;

            mBlock=-1/slope;
            bBlock= blockIntersectionLat-mBlock*blockIntersectionLon;

            if (theta>0 && theta<180){
                signBlock=ABOVE;
            }else{
                signBlock=BELOW;
            }



        }
    }

}
