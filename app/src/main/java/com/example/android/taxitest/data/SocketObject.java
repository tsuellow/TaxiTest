package com.example.android.taxitest.data;

public interface SocketObject {
    int getTaxiId();

    void setTaxiId(int taxiId);

    double getLatitude();

    void setLatitude(double latitude);

    double getLongitude();

    void setLongitude(double longitude);

    long getLocationTime();

    void setLocationTime(long locationTime);

    float getRotation();

    void setRotation(float rotation);

    double getDestinationLatitude();

    void setDestinationLatitude(double destinationLatitude);

    double getDestinationLongitude();

    void setDestinationLongitude(double destinationLongitude);

    int getIsActive();

    void setIsActive(int isActive);

    public String objectToCsv();
}
