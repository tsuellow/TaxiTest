/*
 * Copyright 2013 Ahmad Saleem
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2017 devemux86
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.example.android.taxitest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.LocationRenderer;
import org.oscim.utils.FastMath;

import java.util.List;

@SuppressWarnings("deprecation")
public class Compass extends Layer implements SensorEventListener, Map.UpdateListener,
        LocationRenderer.Callback {

    // static final Logger log = LoggerFactory.getLogger(Compass.class);

    public enum Mode {
        OFF, C2D, C3D,
    }

    private final SensorManager mSensorManager;
    private boolean hasMagneticSensor=false;
    private boolean hasOrientationSensor=false;
    private final ImageView mArrowView;


    private final float[] mRotationV = new float[3];

    private float mCorrectionFactor=0.0f;
    private Context mContext;
    private Location mCurrLocation;

    private float mCurRotation;
    private float mCurMapRotation;
    private float mCurSensorRotation;


    private boolean mControlOrientation;

    private Mode mMode = Mode.OFF;
    private int mListeners;

    public Compass(Context context, Map map, ImageView imageView) {
        super(map);

        mContext=context;

        mSensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> s = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : s){
            if (sensor.getType()==Sensor.TYPE_ORIENTATION){
                hasOrientationSensor=true;
            }
        }
        PackageManager manager = mContext.getPackageManager();
        hasMagneticSensor=manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);

        Log.d("hasSensors","ori:"+hasOrientationSensor+" mag:"+hasMagneticSensor);

        mArrowView = imageView;

        setEnabled(false);
    }

    //defines how to handle rotation from manual input
    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (!mControlOrientation) {
            float rotation = -mapPosition.bearing;
            adjustArrow(mCurMapRotation, rotation);
            mCurMapRotation=rotation;
        }

    }

    //hands location to the compass in order to leverage location bearing
    public void setCurrLocation(Location location){
        mCurrLocation=location;
        //find the error in the sensor feedback by comparing it to bearing
        mCorrectionFactor=location.getSpeed()>2.7 ? location.getBearing()-mCurSensorRotation:mCorrectionFactor;
        Log.d("correction factor",mCorrectionFactor+"");
    }

    //if no sensors are available use bearing as alternative compass
    public void setCompassFromBearing(double adv){

        float rotation=0.0f;
        float rotationMap=0.0f;

        float bearing;

        if (mCurrLocation.getSpeed()>2){
            bearing=mCurrLocation.getBearing();
        }else{
            bearing=mCurRotation;
        }

        if (mCurrLocation!=null){
            float change = bearing-mCurRotation;
            change = (float) FastMath.clampDegree(change);
            float changeMap = bearing-mCurMapRotation;
            changeMap = (float) FastMath.clampDegree(changeMap);
            //mCurSensorRotation=mCurrLocation.getBearing();
            rotation=(float) (change*adv+mCurRotation);
            rotationMap =(float) (changeMap*adv+mCurMapRotation);
        }

        if (mMode != Mode.OFF) {
            boolean redraw = false;
            if (Math.abs(rotationMap-mCurMapRotation) > 0.01) {
                adjustArrow(mCurMapRotation, rotationMap);
                mMap.viewport().setRotation(-rotationMap);
                redraw = true;
            }
            if (redraw){
                mMap.updateMap(true);
            }
            mCurMapRotation=rotationMap;
        }
        mCurRotation = rotation;
        mCompassListener.onCompassChanged(mCurRotation);
        Log.d("compass","is using bearing"+mCurRotation);
    }



    public boolean hasNeededSensors(){
        if (hasMagneticSensor&&hasOrientationSensor){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean hasRotation() {
        return true;
    }

    @Override
    public synchronized float getRotation() {
        return mCurRotation;
    }

    public synchronized float getMapRotation(){return mCurMapRotation;}

    public void setMapRotation(float rotation){
        mCurMapRotation=rotation;
    }

    public void controlView(boolean enable) {
        mControlOrientation = enable;
    }

    public boolean controlView() {
        return mControlOrientation;
    }

    public void setMode(Mode mode) {
        if (mode == mMode)
            return;

        if (mode == Mode.OFF) {
            setEnabled(false);

            mMap.getEventLayer().enableRotation(true);
            mMap.getEventLayer().enableTilt(true);
        } else if (mMode == Mode.OFF) {
            setEnabled(true);
        }

        if (mode == Mode.C3D) {
            mMap.getEventLayer().enableRotation(false);
            mMap.getEventLayer().enableTilt(false);
        } else if (mode == Mode.C2D) {
            mMap.getEventLayer().enableRotation(false);
            mMap.getEventLayer().enableTilt(true);
        }

        mMode = mode;
    }

    public Mode getMode() {
        return mMode;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mListeners += enabled ? 1 : -1;

        if (mListeners == 1) {
            resume();
        } else if (mListeners == 0) {
            pause();

        } else if (mListeners < 0) {
            // then bad
            mListeners = 0;
        }
    }

    public void resume() {
        if (mListeners <= 0)
            return;

        super.setEnabled(true);

        Sensor sensor;

        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, sensor,
                SensorManager.SENSOR_DELAY_UI);
    }

    public void pause() {
        if (mListeners <= 0)
            return;

        super.setEnabled(false);
        mSensorManager.unregisterListener(this);
    }

    public void adjustArrow(float prev, float cur) {
        Animation an = new RotateAnimation(-prev,
                -cur,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        an.setDuration(100);
        an.setRepeatCount(0);
        an.setFillAfter(true);
        Log.d("compass", "prev:"+prev+"cur:"+cur);

        mArrowView.startAnimation(an);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //check that magnetometer and orientation sensors are available
        if (!hasNeededSensors())
            return;
        //check that sensor signal is of type orientation
        if (event.sensor.getType() != Sensor.TYPE_ORIENTATION)
            return;
        System.arraycopy(event.values, 0, mRotationV, 0, event.values.length);

        float rotation = mRotationV[0];
        float rotationMap;

        rotation=adjustScreenRotation(rotation);
        mCurSensorRotation=rotation;

        if (mCurrLocation!=null){
            if (mCurrLocation.getSpeed()>=2.7
                    //&& (mCurrLocation.getBearing()-rotation)<45
            ){
                rotation=mCurrLocation.getBearing();
                Log.d("speedBearing","speed:"+mCurrLocation.getSpeed()+" bearing:"+mCurrLocation.getBearing()+" , "+rotation);
            }else{
                //ad statement invalidating this if user is not taxi
                rotation=rotation+mCorrectionFactor;
            }
        }

        float change = rotation - mCurRotation;
        change = (float) FastMath.clampDegree(change);
        float changeMap = rotation - mCurMapRotation;
        changeMap = (float) FastMath.clampDegree(changeMap);
        // low-pass
        change *= 0.05;
        changeMap *= 0.05;

        rotation = mCurRotation + change;
        rotation = (float) FastMath.clampDegree(rotation);
        rotationMap = mCurMapRotation + changeMap;
        rotationMap = (float) FastMath.clampDegree(rotationMap);

//        float tilt = mRotationV[1];
//
//        mCurTilt = mCurTilt + 0.2f * (tilt - mCurTilt);

        if (mMode != Mode.OFF) {
            boolean redraw = false;

            if (Math.abs(changeMap) > 0.01) {
                adjustArrow(mCurMapRotation, rotationMap);

                mMap.viewport().setRotation(-rotationMap);
                //mMap.viewport().setMapViewCenter(0.75f);
                redraw = true;
            }

//            if (mMode == Mode.C3D)
//                redraw |= mMap.viewport().setTilt(-mCurTilt * 1.5f);

            if (redraw){
                mMap.updateMap(true);
            }
            mCurMapRotation=rotationMap;
        }
        mCurRotation = rotation;
        mCompassListener.onCompassChanged(mCurRotation);
    }

    //consider deleting as screen is restricted to vertical mode
    public float adjustScreenRotation(float rotation){
        final int screenRotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        switch (screenRotation) {
            case Surface.ROTATION_0:
                return rotation;
            case Surface.ROTATION_90:
                return rotation+90;
            case Surface.ROTATION_180:
                return rotation+180;
            default:
                return rotation+270;
        }
    }

    //callback for people needing compass data
    public interface CompassUpdateListener{
        void onCompassChanged(float rotation);
    }

    public CompassUpdateListener mCompassListener;

    public void setCompassUpdateListener(CompassUpdateListener compassUpdateListener){
        mCompassListener=compassUpdateListener;
    }



    // from http://stackoverflow.com/questions/16317599/android-compass-that-
    // can-compensate-for-tilt-and-pitch/16386066#16386066

    // private int mGravityAccuracy;
    // private int mMagneticFieldAccuracy;

    // private float[] mGravityV = new float[3];
    // private float[] mMagFieldV = new float[3];
    // private float[] mEastV = new float[3];
    // private float[] mNorthV = new float[3];
    //
    // private float mNormGravity;
    // private float mNormMagField;
    //
    // private boolean mOrientationOK;
    // private float mAzimuthRadians;
    // private float mPitchRadians;
    // private float mPitchAxisRadians;
    //
    // private void handle(SensorEvent event) {
    // int SensorType = event.sensor.getType();
    // switch (SensorType) {
    // case Sensor.TYPE_GRAVITY:
    // mLastAccelerometerSet = true;
    // System.arraycopy(event.values, 0, mGravityV, 0, mGravityV.length);
    // mNormGravity = (float) Math.sqrt(mGravityV[0] * mGravityV[0]
    // + mGravityV[1] * mGravityV[1] + mGravityV[2]
    // * mGravityV[2]);
    // for (int i = 0; i < mGravityV.length; i++)
    // mGravityV[i] /= mNormGravity;
    // break;
    // case Sensor.TYPE_MAGNETIC_FIELD:
    // mLastMagnetometerSet = true;
    // System.arraycopy(event.values, 0, mMagFieldV, 0, mMagFieldV.length);
    // mNormMagField = (float) Math.sqrt(mMagFieldV[0] * mMagFieldV[0]
    // + mMagFieldV[1] * mMagFieldV[1] + mMagFieldV[2]
    // * mMagFieldV[2]);
    // for (int i = 0; i < mMagFieldV.length; i++)
    // mMagFieldV[i] /= mNormMagField;
    // break;
    // }
    // if (!mLastAccelerometerSet || !mLastMagnetometerSet)
    // return;
    //
    // // first calculate the horizontal vector that points due east
    // float ex = mMagFieldV[1] * mGravityV[2] - mMagFieldV[2] * mGravityV[1];
    // float ey = mMagFieldV[2] * mGravityV[0] - mMagFieldV[0] * mGravityV[2];
    // float ez = mMagFieldV[0] * mGravityV[1] - mMagFieldV[1] * mGravityV[0];
    // float normEast = (float) Math.sqrt(ex * ex + ey * ey + ez * ez);
    //
    // if (mNormGravity * mNormMagField * normEast < 0.1f) { // Typical values
    // are > 100.
    // // device is close to free fall (or in space?), or close to magnetic
    // north pole.
    // mOrientationOK = false;
    // return;
    // }
    //
    // mEastV[0] = ex / normEast;
    // mEastV[1] = ey / normEast;
    // mEastV[2] = ez / normEast;
    //
    // // next calculate the horizontal vector that points due north
    // float mdotG = (mGravityV[0] * mMagFieldV[0]
    // + mGravityV[1] * mMagFieldV[1]
    // + mGravityV[2] * mMagFieldV[2]);
    //
    // float nx = mMagFieldV[0] - mGravityV[0] * mdotG;
    // float ny = mMagFieldV[1] - mGravityV[1] * mdotG;
    // float nz = mMagFieldV[2] - mGravityV[2] * mdotG;
    // float normNorth = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
    //
    // mNorthV[0] = nx / normNorth;
    // mNorthV[1] = ny / normNorth;
    // mNorthV[2] = nz / normNorth;
    //
    // // take account of screen rotation away from its natural rotation
    // //int rotation =
    // App.activity.getWindowManager().getDefaultDisplay().getRotation();
    // float screenDirection = 0;
    // //switch(rotation) {
    // // case Surface.ROTATION_0: screenDirection = 0; break;
    // // case Surface.ROTATION_90: screenDirection = (float)Math.PI/2; break;
    // // case Surface.ROTATION_180: screenDirection = (float)Math.PI; break;
    // // case Surface.ROTATION_270: screenDirection = 3*(float)Math.PI/2;
    // break;
    // //}
    // // NB: the rotation matrix has now effectively been calculated. It
    // consists of
    // // the three vectors mEastV[], mNorthV[] and mGravityV[]
    //
    // // calculate all the required angles from the rotation matrix
    // // NB: see
    // http://math.stackexchange.com/questions/381649/whats-the-best-3d-angular-
    // // co-ordinate-system-for-working-with-smartfone-apps
    // float sin = mEastV[1] - mNorthV[0], cos = mEastV[0] + mNorthV[1];
    // mAzimuthRadians = (float) (sin != 0 && cos != 0 ? Math.atan2(sin, cos) :
    // 0);
    // mPitchRadians = (float) Math.acos(mGravityV[2]);
    //
    // sin = -mEastV[1] - mNorthV[0];
    // cos = mEastV[0] - mNorthV[1];
    //
    // float aximuthPlusTwoPitchAxisRadians =
    // (float) (sin != 0 && cos != 0 ? Math.atan2(sin, cos) : 0);
    //
    // mPitchAxisRadians = (float) (aximuthPlusTwoPitchAxisRadians -
    // mAzimuthRadians) / 2;
    // mAzimuthRadians += screenDirection;
    // mPitchAxisRadians += screenDirection;
    //
    // mOrientationOK = true;
    // }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // int type = sensor.getType();
        // switch (type) {
        // case Sensor.TYPE_GRAVITY:
        // mGravityAccuracy = accuracy;
        // break;
        // case Sensor.TYPE_MAGNETIC_FIELD:
        // mMagneticFieldAccuracy = accuracy;
        // break;
        // }
    }

}
