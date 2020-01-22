package com.example.android.taxitest.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class TaxiDao {

    //test
    @Query("select count(*) from taxiBase")
    public abstract int getCurrentTaxis();

    //populate base table
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertNewData(List<TaxiObject> newData);

    //delete myself
    @Query("delete from taxiBase where taxiId=:myId")
    abstract void deleteMySelf(int myId);

    //clear taxiOld
    @Query("delete from taxiOld where 1=1")
    public abstract void clearTaxiOld();

    //clear taxiBase to be used only on pause/delete
    @Query("delete from taxiBase where 1=1")
    public abstract void clearTaxiBase();

    //delete inactive taxis
    @Query("delete from taxiBase where isActive=0")
    abstract void clearInactiveTaxis();

    //delete inactive taxis that are new
    @Query("delete from taxiBase where isActive=0 and taxiId not in (select taxiId from taxiOld)")
    abstract void clearInactiveNewTaxis();

    //set old taxis to inactive
    @Query("update taxiBase set isActive=0 where (:date-locationTime)>60000")
    abstract void setOldTaxisToInactive(long date);

    //move data from taxiBase to taxiStart post execution
    @Query("insert into taxiOld select * from taxiBase order by taxiId ")
    abstract void populateTaxiOld();


    //ORDER IN WHICH THINGS SHOULD HAPPEN
    @Transaction
    public void runPreOutputTransactions(List<TaxiObject> newData, int myId){
        insertNewData(newData);
        deleteMySelf(myId);
        clearInactiveNewTaxis();
    }

    //retrieve matching moved or dissapearing taxis
    @Query("select * from taxiBase where taxiId in (select taxiId from taxiOld) order by taxiId")
    public abstract List<TaxiObject> getMatchingTaxiBase();

    //retrieve new taxis to be added
    @Query("select * from taxiBase where taxiId not in (select taxiId from taxiOld) order by taxiId")
    public abstract List<TaxiObject> getNewTaxis();


    @Transaction
    public void runPostOutputTransactions(long date){
        clearTaxiOld();
        clearInactiveTaxis();
        populateTaxiOld();
        setOldTaxisToInactive(date);
    }

}


