package com.dale.viaje.nicaragua;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;
import com.skydoves.balloon.ArrowConstraints;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    Preference zoomTilt, taxiAmount, profileData, profileEdit, filterAmplitude;
    SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.app_settings, rootKey);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        zoomTilt = (Preference) findPreference("zoomtilt");
        taxiAmount = (Preference) findPreference("taxiamount");
        filterAmplitude= (Preference) findPreference("filteramplitude");
        profileData = (Preference) findPreference("profiledata");
        profileEdit = (Preference) findPreference("profileedit");

        zoomTilt.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsZoomTiltDialog dialog=new SettingsZoomTiltDialog();
                dialog.show(getChildFragmentManager(),"tiltzoom");
                return false;
            }
        });

        filterAmplitude.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showFilterDialog();
                return false;
            }
        });

        taxiAmount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showTaxiAmountDialog();
                return false;
            }
        });

        profileEdit.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i =new Intent(getContext(),ModifyProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            }
        });


    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    public static class TiltZoomSettingFragment extends BasicMapFragment{
        @Override
        public void setOwnIcon() {
            ownIcon=new VectorMasterDrawable(getContext(),R.drawable.taxi_marker);
        }

        @Override
        public void doBeforeInflation() {
            super.doBeforeInflation();
        }
    };


    public void showFilterDialog(){
        final Dialog dialog=new Dialog(requireActivity());
        dialog.setContentView(R.layout.dialog_setting_seekbar);
        TextView title=dialog.findViewById(R.id.tv_title_dialog);
        TextView barText=dialog.findViewById(R.id.tv_bar_text);
        ImageButton info=dialog.findViewById(R.id.ib_info);
        SeekBar seekBar=dialog.findViewById(R.id.sb_bar);
        Button cancelBtn=dialog.findViewById(R.id.bt_dialog_cancel);
        Button acceptBtn=dialog.findViewById(R.id.bt_dialog_accept);

        title.setText(R.string.settingsfragment_filteramplitude);
        String barSuffix=getString(R.string.settingsfragment_degrees);
        float value=sharedPreferences.getFloat("filteramplitude",45.0f);
        barText.setText(Math.round(value)+" "+barSuffix);
        seekBar.setMax(79);
        seekBar.setProgress((int)value-10);
        seekBar.refreshDrawableState();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                barText.setText((10+progress)+" "+barSuffix);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        buildTooltips(dialog.getContext(), info,getString(R.string.settingsfragment_filterhint),0.1f);


        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dialog.dismiss();
            }
        });

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor=sharedPreferences.edit();
                editor.putFloat("filteramplitude",(float)(10+seekBar.getProgress()));
                editor.apply();

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void showTaxiAmountDialog(){
        final Dialog dialog=new Dialog(requireActivity());
        dialog.setContentView(R.layout.dialog_setting_seekbar);
        TextView title=dialog.findViewById(R.id.tv_title_dialog);
        TextView barText=dialog.findViewById(R.id.tv_bar_text);
        ImageButton info=dialog.findViewById(R.id.ib_info);
        SeekBar seekBar=dialog.findViewById(R.id.sb_bar);
        Button cancelBtn=dialog.findViewById(R.id.bt_dialog_cancel);
        Button acceptBtn=dialog.findViewById(R.id.bt_dialog_accept);

        title.setText(R.string.settingsfragment_numberoftaxis);
        String barSuffix=getString(R.string.target_type_plural);
        int value=sharedPreferences.getInt("taxiamount",20);
        barText.setText(value+" "+barSuffix);
        seekBar.setMax(40);
        seekBar.setProgress(value-10);
        seekBar.refreshDrawableState();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                barText.setText((10+progress)+" "+barSuffix);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        buildTooltips(dialog.getContext(), info,getString(R.string.settingsfragment_hinttaxis),0.1f);


        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dialog.dismiss();
            }
        });

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor=sharedPreferences.edit();
                editor.putInt("taxiamount",(10+seekBar.getProgress()));
                editor.apply();

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void buildTooltips(Context context, ImageButton view, String text, float arrow){
        final Balloon balloonFirst=new Balloon.Builder(context)
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.BOTTOM)
                .setArrowVisible(true)
                .setWidthRatio(0.7f)
                .setPadding(4)
                .setTextSize(12f)
                .setArrowConstraints(ArrowConstraints.ALIGN_ANCHOR)
                .setAlpha(0.9f)
                .setText(text)
                .setTextIsHtml(true)
                .setBackgroundColor(ContextCompat.getColor(context, R.color.colorSelected))
                .setBalloonAnimation(BalloonAnimation.FADE)
                .setAutoDismissDuration(10000)
                .build();

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (balloonFirst.isShowing()){
                    balloonFirst.dismiss();
                }else{
                    balloonFirst.showAlignTop(view);

                }

            }
        });

    }

}
