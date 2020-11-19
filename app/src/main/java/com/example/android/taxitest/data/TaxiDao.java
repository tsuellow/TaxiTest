package com.example.android.taxitest.data;

import android.util.Log;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.android.taxitest.MainActivity;

import org.oscim.core.GeoPoint;

import java.util.Date;
import java.util.List;

import static com.example.android.taxitest.utils.MiscellaneousUtils.locToGeo;

@Dao
public abstract class TaxiDao {

    //test DEPRECATED
    @Query("select count(*) from taxiBase")
    public abstract int getCurrentTaxis();

    //populate base table DEPRECATED
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertNewData(List<TaxiObject> newData);

    //populate base table     ------filter out undesirables
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertFullData(List<TaxiNew> newData);

    //delete myself DEPRECATED
    @Query("delete from taxiBase where taxiId=:myId")
    abstract void deleteMySelf(int myId);

    public static final int ABOVE=1;
    public static final int BELOW=-1;

    //flag taxis that are active but out of range for disappearance... consider ignoring if too slow
    @Query("update taxiBase set isActive=2 where taxiId in (select taxiId from taxiNew)") //if data for taxi was received but it is too far away
    abstract void flagForDisappearance();

    //filter out irrelevant taxis, keep empty taxis and clicked taxis
    @Query("delete from taxiNew where taxiId not in (:clickedItems) " +  //ensure clicked items are on
            "and (latitude!=0.0 or longitude!=0.0) " +  //ensure empty taxis are on
            "and ((:leftSign*destinationLatitude<:leftSign*(:bLeft+:mLeft*destinationLongitude) " + //destination filter left
            "or :rightSign*destinationLatitude<:rightSign*(:bRight+:mRight*destinationLongitude)) " + //destination filter right
            "or (:blockSign*latitude>:blockSign*(:bBlock+:mBlock*longitude)))") //position filter block: make sure taxis that have already passed by me are not shown
    abstract void doScopeReductionFiltering(double bLeft, double mLeft, double bRight, double mRight, double bBlock, double mBlock, int leftSign, int rightSign, int blockSign, List<Integer> clickedItems);

    //calculate the taxis pseudoDistance
    @Query("update taxiNew set pseudoDistance=((:ownLat-latitude)*(:ownLat-latitude)+(:ownLon-longitude)*(:ownLon-longitude))") //this is ok since our pseudoDistance is a monotone transformation of the actual distance
    abstract void calculateDistances(double ownLat, double ownLon);

    //pass 50 closest taxis to taxiBase
    @Query("replace into taxiBase(taxiId, latitude, longitude, locationTime, rotation, type, destinationLatitude, destinationLongitude, isActive) " +
            "select taxiId, latitude, longitude, locationTime, rotation, type, destinationLatitude, destinationLongitude, isActive " +
            "from taxiNew order by pseudoDistance asc limit :lim") //
    abstract void feedCloseTaxis(int lim);

    //make sure active comms taxis are included
    @Query("replace into taxiBase(taxiId, latitude, longitude, locationTime, rotation, type, destinationLatitude, destinationLongitude, isActive) " +
            "select taxiId, latitude, longitude, locationTime, rotation, type, destinationLatitude, destinationLongitude, case isActive when 2 then 1 else isActive end  " + //setting them to active so we overwrite their status after accepted
            "from taxiNew where taxiId in (:clickedItems)")
    abstract void feedOpenComms(List<Integer> clickedItems);

    //clear taxiNew
    @Query(" delete from taxiNew where 1=1")
    public abstract void clearTaxiNew();

//    /**
//     * This method filters out taxis headed to destinations that do not lie on our directional path
//     * @param bRight: intersection of line to the right;
//     * @param mRight: slope of line to the right;
//     * @param rightSign: set to 1 to discard points below the line to the right and keep those above and to -1 to discard those above;
//     * @param leftSign: set to 1 to discard points below the line to the left and keep those above and to -1 to discard those above;
//     * */
//    @Query("update taxiBase set isActive=2 where (:leftSign*destinationLatitude<:leftSign*(:bLeft+:mLeft*destinationLongitude) " +
//            "or :rightSign*destinationLatitude<:rightSign*(:bRight+:mRight*destinationLongitude)) " +
//            "and taxiId not in (:clickedItems) and isActive!=0")
//    public abstract void applyDirectionalFilter(double bLeft, double mLeft, double bRight, double mRight, int leftSign, int rightSign, List<Integer> clickedItems);

    //clear taxiOld
    @Query("delete from taxiOld where 1=1")
    public abstract void clearTaxiOld();

    //clear taxiBase to be used only on pause/delete
    @Query("delete from taxiBase where 1=1")
    public abstract void clearTaxiBase();

    //delete inactive taxis  --------include taxis exiting the V shape i.e. change condition to !=1
    @Query("delete from taxiBase where isActive!=1")
    abstract void clearInactiveTaxis();

    //delete inactive taxis that are new
    @Query("delete from taxiBase where isActive=0 and taxiId not in (select taxiId from taxiOld)")
    abstract void clearInactiveNewTaxis();

    //set old taxis to inactive   ---------include setting taxis to 2 that are filtered out
    @Query("update taxiBase set isActive=0 where (:date-locationTime)>60000")
    abstract void setOldTaxisToInactive(long date);

    //move data from taxiBase to taxiStart post execution
    @Query("insert into taxiOld select * from taxiBase order by taxiId ")
    abstract void populateTaxiOld();

    SocketFilterDrivers sf;
    //ORDER IN WHICH THINGS SHOULD HAPPEN
    long t1;
    long t2;
    @Transaction
    public void runNewPreOutputTransactions(List<TaxiNew> newData, boolean filter, List<Integer> clickedItems, double phi, int limit){
        long t01=new Date().getTime();
        t1=t01;

        insertFullData(newData);
        flagForDisappearance();
        if (filter){
            sf = new SocketFilterDrivers(locToGeo(MainActivity.mMarkerLoc), MainActivity.destGeo, phi);
            doScopeReductionFiltering(sf.bLeft, sf.mLeft, sf.bRight, sf.mRight, sf.bBlock, sf.mBlock, sf.signLeft, sf.signRight, sf.signBlock,clickedItems);
        }
        calculateDistances(locToGeo(MainActivity.mMarkerLoc).getLatitude(),locToGeo(MainActivity.mMarkerLoc).getLongitude());
        feedCloseTaxis(limit);
        feedOpenComms(clickedItems);
        clearTaxiNew();
        clearInactiveNewTaxis();

        long t02=new Date().getTime();
        long t0=t02-t01;
        Log.d("timing", "preNew: "+t0);
    }

//    @Transaction
//    public void runPreOutputTransactions(List<TaxiObject> newData, int myId){
//        insertNewData(newData);
//        //deleteMySelf(myId);
//        clearInactiveNewTaxis();
//    }

    //retrieve matching moved or dissapearing taxis
    @Query("select * from taxiBase where taxiId in (select taxiId from taxiOld) order by taxiId")
    public abstract List<TaxiObject> getMatchingTaxiBase();

    //retrieve new taxis to be added
    @Query("select * from taxiBase where taxiId not in (select taxiId from taxiOld) and isActive=1 order by taxiId")
    public abstract List<TaxiObject> getNewTaxis();


    @Transaction
    public void runPostOutputTransactions(long date){
        long t01=new Date().getTime();
        t2=t01;
        Log.d("timing", "total: "+(t2-t1));

        clearTaxiOld();
        clearInactiveTaxis();
        populateTaxiOld();
        setOldTaxisToInactive(date);

        long t02=new Date().getTime();
        long t0=t02-t01;
        Log.d("timing", "post: "+t0);
    }


    public class SocketFilterDrivers {
        public double bRight;
        public double mRight;
        public double bLeft;
        public double mLeft;
        public int signRight;
        public int signLeft;

        public double bBlock;
        public double mBlock;
        public int signBlock;

        public SocketFilterDrivers(GeoPoint geo, GeoPoint dest, double phi){
            double tinyCorrection=0.0;
            if (dest.getLongitude()-geo.getLongitude()==0.0)
                tinyCorrection=1E-11;

            //v shaped dest filter and block filter
            double deltaLat=dest.getLatitude()-geo.getLatitude();
            double deltaLon=dest.getLongitude()-geo.getLongitude()+tinyCorrection;
            double slope=(deltaLat)/(deltaLon);
            double theta=Math.toDegrees(Math.atan(slope));//not working right
            if (deltaLon<0) theta = 180 + theta;
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
            double blockIntersectionLat=geo.getLatitude()+normalizationFactor*deltaLat;
            double blockIntersectionLon=geo.getLongitude()+normalizationFactor*deltaLon;

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


