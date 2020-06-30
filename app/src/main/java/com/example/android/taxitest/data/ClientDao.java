package com.example.android.taxitest.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class ClientDao {

    //test
    @Query("select count(*) from clientBase")
    public abstract int getCurrentTaxis();

    //populate base table     ------filter out undesirables
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertNewData(List<ClientObject> newData);

    //delete myself
    @Query("delete from clientBase where taxiId=:myId")
    abstract void deleteMySelf(int myId);

    public static final int ABOVE=1;
    public static final int BELOW=-1;
    //apply directional filters
    /**
     * This method filters out taxis headed to destinations that do not lie on our directional path
     * @param bRight: intersection of line to the right;
     * @param mRight: slope of line to the right;
     * @param rightSign: set to 1 to discard points below the line to the right and keep those above and to -1 to discard those above;
     * @param leftSign: set to 1 to discard points below the line to the left and keep those above and to -1 to discard those above;
     *
     * */
    @Query("update clientBase set isActive=2 where (:leftSign*destinationLatitude<:leftSign*(:bLeft+:mLeft*destinationLongitude) " +
            "or :rightSign*destinationLatitude<:rightSign*(:bRight+:mRight*destinationLongitude)) " +
            "and taxiId not in (:clickedItems) and isActive!=0")
    public abstract void applyDirectionalFilter(double bLeft, double mLeft, double bRight, double mRight, int leftSign, int rightSign, List<Integer> clickedItems);

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
    @Transaction
    public void runPreOutputTransactions(List<ClientObject> newData, int myId){
        insertNewData(newData);
        deleteMySelf(myId);
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

}
