package com.dale.viaje.nicaragua;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.dale.viaje.nicaragua.vtmExtension.AndroidGraphicsCustom;
import com.dale.viaje.nicaragua.vtmExtension.OwnMarker;
import com.dale.viaje.nicaragua.vtmExtension.OwnMarkerLayer;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;
import com.sdsmdg.harjot.vectormaster.models.PathModel;

import org.oscim.android.MapView;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.MotionEvent;

import java.util.ArrayList;

public class SettingsZoomTiltDialog extends DialogFragment {

    SharedPreferences preferences;
    float pivot;
    float tilt;
    double zoom;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        preferences= PreferenceManager.getDefaultSharedPreferences(requireContext());
        pivot=preferences.getFloat("pivot",75.0f);
        tilt=preferences.getFloat("tilt",30.0f);
        zoom=preferences.getFloat("zoom",17.0f);

        TiltZoomSettingFragment fragment = new TiltZoomSettingFragment();
        View v = inflater.inflate(R.layout.dialog_settings_zoom_tilt, container, false);

        getChildFragmentManager().beginTransaction().add(R.id.map_container, fragment).commit();
        SeekBar pivotSeek=v.findViewById(R.id.sb_pivot);
        SeekBar zoomSeek=v.findViewById(R.id.sb_zoom);
        SeekBar tiltSeek=v.findViewById(R.id.sb_tilt);
        Button cancelBtn=v.findViewById(R.id.bt_dialog_cancel);
        Button acceptBtn=v.findViewById(R.id.bt_dialog_accept);

        pivotSeek.setProgress((int)(pivot*100));
        pivotSeek.refreshDrawableState();
        pivotSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fragment.setPivot(((float)progress)/100f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        zoomSeek.setProgress((int)((zoom-15)*100));
        zoomSeek.refreshDrawableState();
        zoomSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fragment.setZoom(15.0+((double)progress)/100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tiltSeek.setProgress((int)tilt);
        tiltSeek.refreshDrawableState();
        tiltSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fragment.setTilt(((float)progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });




        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                 dismiss();
            }
        });

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor=preferences.edit();
                editor.putFloat("pivot",fragment.mPivot);
                editor.putFloat("tilt",fragment.mTilt);
                editor.putFloat("zoom",(float)fragment.mZoom);
                editor.apply();

                 dismiss();
            }
        });
        return v;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.WideDialog);
    }



    public static class TiltZoomSettingFragment extends BasicMapFragment{

        @Override
        public void setOwnIcon() {
            ownIcon=new VectorMasterDrawable(getContext(),R.drawable.taxi_marker);
        }

        @Override
        public void doBeforeInflation() {
            SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(requireContext());
            super.doBeforeInflation();
            mCompass.setMode(Compass.Mode.C2D);
            setPivot(preferences.getFloat("pivot",75.0f));
            setTilt(preferences.getFloat("tilt",30.0f));
            setZoom((double) preferences.getFloat("zoom",17.0f));
        }

        @Override
        public void setOwnMarkerLayer() {
            mOwnMarkerLayer=new OwnMarkerLayer(getContext(), mBarriosLayer,mapView.map(),new ArrayList<OwnMarker>(),ownIcon, Constants.lastLocation, new GeoPoint(0,0),mCompass){
                @Override
                public VectorMasterDrawable modifyDrawable(int color, boolean isClicked) {
                    VectorMasterDrawable result = new VectorMasterDrawable(context,ownIcon.getResID());
                    PathModel pathModelArrow=result.getPathModelByName("arrow");
                    pathModelArrow.setFillColor(Color.WHITE);
                    pathModelArrow.setStrokeWidth(2.0f);
                    return result;
                }

                @Override
                public Bitmap fetchBitmap(GeoPoint geoPoint, boolean isClicked) {
                    VectorMasterDrawable drawable = fetchDrawable(geoPoint,isClicked);
                    Bitmap bitmap = AndroidGraphicsCustom.drawableToBitmap(drawable, 200);//keep icon at one size for simplicity
                    return bitmap;
                }

                @Override
                public Bitmap fetchBitmap(GeoPoint geoPoint, boolean isClicked, double zoom) {
                    VectorMasterDrawable drawable = fetchDrawable(geoPoint,isClicked);
                    Bitmap bitmap = AndroidGraphicsCustom.drawableToBitmap(drawable, 200);//keep icon at one size for simplicity
                    return bitmap;
                }

                @Override
                public void scaleIcon(int scale) {
                    //do nothing
                }
            };
        }
        //        Context mContext;
//        public void setContext


        @Override
        public void setMapEventsReceiver(MapView mapView) {
            mMapEventsReceiver=new EmptyReceiver(mapView);
        }

        public class EmptyReceiver extends BasicMapFragment.MapEventsReceiver{

            EmptyReceiver(MapView mapView) {
                super(mapView);
            }

            @Override
            public boolean onGesture(Gesture g, MotionEvent e) {
                return false;
            }

            @Override
            public void onMapEvent(Event e, MapPosition mapPosition) {

            }
        }
    };
}
