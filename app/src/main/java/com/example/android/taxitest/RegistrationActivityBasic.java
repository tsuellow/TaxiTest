package com.example.android.taxitest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.android.taxitest.connection.DataPart;
import com.example.android.taxitest.connection.MultipartRequest;
import com.example.android.taxitest.connection.MySingleton;
import com.example.android.taxitest.utils.CustomTextView;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.vtmExtension.CitySupport;
import com.google.android.material.textfield.TextInputLayout;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class RegistrationActivityBasic extends AppCompatActivity {

    public EditText firstName, lastName, phoneNr, dob;
    TextInputLayout loFirstName, loGender, loDob, loPhone;
    public AutoCompleteTextView gender, city;
    public TextView  textWarning; //textPhotoFace,
    CustomTextView textPhotoFace, salute;
    public CheckBox sharePhone, agreeToTerms;
    CardView cardFace;
    CitySupport cities;

    public ImageView photoFace, infoFirst, infoLast, infoPhone, infoSharePhone;
    public Button register, readTerms;
    ScrollView svParent;
    public Toolbar mToolbar;

    Date dateOfBirth;
    public SharedPreferences preferences;

    public static int REQUEST_TAKE_FACE=1011;
    public static int REQUEST_TAKE_CAR=1111;
    public static int REQUEST_CODE=223;

    public Activity mCurrActivity;
    public Class mNextActivity;

    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_registration);
        mToolbar = (Toolbar) findViewById(R.id.toolbar_registration);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("User Registration");
        salute=(CustomTextView) findViewById(R.id.tv_salute);
        salute.setText("Personal Data");
        initViews();


        setActivities();

        preferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        buildTooltips(infoFirst,"using your real name will increase\n the trust of your clients",0.9f);
        buildTooltips(infoLast,"adding a last name will increase\n the trust of your clients",0.9f);
        buildTooltips(infoPhone,"i.e. 8765-4321 or +1-179-123456",0.90f);
        buildTooltips(infoSharePhone,"optional, your phone will only\n be shared with drivers you have\n traveled with",0.90f);

        //gender
        List<String> sexes= Arrays.asList("male","female");
        ArrayAdapter<String> occupationAdapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, sexes);
        gender.setAdapter(occupationAdapter);
        gender.setKeyListener(null);
        gender.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ((AutoCompleteTextView) view).showDropDown();
                gender.setError(null);
                return false;
            }
        });

        //city
        city=(AutoCompleteTextView) findViewById(R.id.actv_residence_city);
        cities=new CitySupport();
        ArrayAdapter<String> cityAdapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, cities.getCityDropdown());

        city.setAdapter(cityAdapter);
        city.setKeyListener(null);
        city.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ((AutoCompleteTextView) view).showDropDown();
                city.setError(null);
                return false;
            }

        });

        //dob
        dob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal=Calendar.getInstance();
                cal.set(2000,0,1);
                Date defaultDate=cal.getTime();
                displayDatePickerDialog(defaultDate);
                dob.setError(null);
            }
        });

        // check permissions CONSIDER MOVING
        ActivityCompat.requestPermissions(mCurrActivity, new String[]{READ_EXTERNAL_STORAGE,ACCESS_FINE_LOCATION, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO,CAMERA},112);


        phoneNr.addTextChangedListener(new PhoneNumberFormattingTextWatcher("NI"));

        //photo face
        photoFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"wait while camera opens", Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "onClick: we should be here");
                        dispatchTakePictureIntent(REQUEST_TAKE_FACE);
                    } else {
                        Log.d(TAG, "onClick: nor here");
                        String[] permissionRequested = {CAMERA};
                        ActivityCompat.requestPermissions(mCurrActivity,permissionRequested,REQUEST_CODE);
                    }

                } else {
                    Log.d(TAG, "onClick: not here");
                    dispatchTakePictureIntent(REQUEST_TAKE_FACE);

                }
            }


        });


        readTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog=makeTermsOfUseDialog(mCurrActivity);
                dialog.show();
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit();
            }
        });

    }

    public void setActivities(){
        mCurrActivity=RegistrationActivityBasic.this;
        mNextActivity=EntryActivity.class;
    }

    public void initViews(){
        firstName=(EditText) findViewById(R.id.et_first_name);
        lastName=(EditText) findViewById(R.id.et_last_name);
        phoneNr=(EditText) findViewById(R.id.et_phone);
        dob=(EditText) findViewById(R.id.et_dob);
        gender=(AutoCompleteTextView) findViewById(R.id.actv_gender);
        photoFace=(ImageView) findViewById(R.id.iv_photo_face);
        register=(Button) findViewById(R.id.bt_register);
        textPhotoFace=(CustomTextView) findViewById(R.id.tv_face);
        sharePhone=(CheckBox) findViewById(R.id.cb_share_phone);
        agreeToTerms=(CheckBox) findViewById(R.id.cb_terms);
        readTerms=(Button) findViewById(R.id.bt_terms);
        cardFace=(CardView) findViewById(R.id.cv_border_color_face);


        loFirstName=(TextInputLayout) findViewById(R.id.lo_first_name);
        loGender=(TextInputLayout) findViewById(R.id.lo_gender);
        loDob =(TextInputLayout) findViewById(R.id.lo_dob);
        loPhone=(TextInputLayout) findViewById(R.id.lo_phone);

        infoFirst=(ImageView) findViewById(R.id.iv_info_first_name);
        infoLast=(ImageView) findViewById(R.id.iv_info_last_name);
        infoPhone =(ImageView) findViewById(R.id.iv_info_nr_plate);
        infoSharePhone =(ImageView) findViewById(R.id.iv_info_share_phone);
        textWarning=(TextView) findViewById(R.id.tv_warning);

        svParent=(ScrollView) findViewById(R.id.sv_parent);
    }

    public void buildTooltips(ImageView view, String text, float arrow){
        final Balloon balloonFirst=new Balloon.Builder(getApplicationContext())
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowVisible(true)
                .setWidthRatio(0.7f)
                .setPadding(4)
                .setTextSize(12f)
                .setArrowPosition(arrow)
                .setAlpha(0.9f)
                .setText(text)
                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorSelected))
                .setBalloonAnimation(BalloonAnimation.FADE)
                .setAutoDismissDuration(4000)
                .build();

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                balloonFirst.showAlignBottom(view);
            }
        });

    }

    private void displayDatePickerDialog(Date date){
        AlertDialog.Builder mBuilder=new AlertDialog.Builder(mCurrActivity);
        View mView=getLayoutInflater().inflate(R.layout.dialog_date_picker,null);
        final DatePicker mDatePicker=(DatePicker) mView.findViewById(R.id.dp_date_picker);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        mDatePicker.init(year,month,day,null);

        Button mOk=(Button) mView.findViewById(R.id.btn_ok_dp);
        Button mCancel=(Button) mView.findViewById(R.id.btn_cancel_dp);
        mBuilder.setTitle("date of birth");
        mBuilder.setView(mView);

        final AlertDialog dialog=mBuilder.create();
        //button to dismiss dialog
        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal=Calendar.getInstance();
                int year, month, day;
                year=mDatePicker.getYear();
                month=mDatePicker.getMonth();
                day=mDatePicker.getDayOfMonth();
                cal.set(year,month,day);
                dob.setText(MiscellaneousUtils.getShortDateString(cal));
                dateOfBirth=MiscellaneousUtils.getRoundDate(cal.getTime());
                dialog.dismiss();
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public enum Size{FULL, MED, THUMB}

    public void dispatchTakePictureIntent(int type) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            photoFile = imageFile(type,Size.FULL);
            Log.d(TAG, "dispatchTakePictureIntent: "+photoFile.getAbsolutePath());
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getResources().getString(R.string.file_provider),
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent,type);
                Log.d(TAG,  photoURI.toString());

            }

        }

    }

    public File imageFile(int requestType, Size size){
        String name=requestType==REQUEST_TAKE_FACE?"face":"car";
        name=size==Size.FULL?name+"_full":size==Size.MED?name+"_med":name+"_thumb";
        File photoFile;
        photoFile = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), ""+name+".jpg");
        return photoFile;
    }

    //code to frame and display pic
    private void takePic(int code){
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile(code,Size.FULL).getAbsolutePath());
        Bitmap cropImg = MiscellaneousUtils.makeSquaredImage(bitmap);

        savePhotoThumbMed(code, cropImg);
        if (imageFile(code,Size.FULL).exists()){
            imageFile(code,Size.FULL).delete();
        }
    }

    private void savePhotoThumbMed(int code,final Bitmap bitmap) {
        try {
            File thumbFile = imageFile(code,Size.THUMB);
            File mediumFile = imageFile(code,Size.MED);

            if (thumbFile.exists()){
                thumbFile.delete();
            }
            if (mediumFile.exists()){
                mediumFile.delete();
            }

            FileOutputStream thumbOut = new FileOutputStream(thumbFile);
            FileOutputStream mediumOut = new FileOutputStream(mediumFile);

            Bitmap thumb = Bitmap.createScaledBitmap(bitmap, 96, 96, false);
            Bitmap medium = Bitmap.createScaledBitmap(bitmap, 480, 480, false);

            mBase64= MiscellaneousUtils.base64Bitmap(thumb);


            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            medium.compress(Bitmap.CompressFormat.JPEG, 100, mediumOut);

            thumbOut.flush();
            mediumOut.flush();

            thumbOut.close();
            mediumOut.close();
            Log.d("ThumbMed saved", "Thumb and Medium ok");
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.d("ThumbMed saved", "Thumb Medium IOException");
        }
    }

    public void setPicture(int code){
        File medium=imageFile(code, Size.MED);
        String clientMedium=medium.getAbsolutePath();
        ImageView view=photoFace;
        if (medium.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(clientMedium);
            view.setImageBitmap(bitmap);
        }else{
            view.setImageResource(R.drawable.clooney);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_TAKE_FACE || requestCode == REQUEST_TAKE_CAR) && resultCode == RESULT_OK) {
            takePic(requestCode);
            setPicture(requestCode);
        }
    }

    //check field input validity
    public boolean checkIsEmpty(EditText editView, TextInputLayout layout){
        if (editView.getText().toString().trim().isEmpty()){
            //layout.setErrorEnabled(true);
            editView.setError("input required");
            layout.setHintTextColor(ColorStateList.valueOf(Color.RED));
            return false;
        }
        loFirstName.setErrorEnabled(false);
        return true;
    }

    private boolean checkPhoneNumber(){
        if (phoneNr.getText().toString().length()==0){
            loPhone.setErrorEnabled(false);
            return true;
        }
        String phoneText= MiscellaneousUtils.depuratePhone(phoneNr.getText().toString());
        try{
            String checkText=phoneText.replaceFirst("^0+(?!$)", "");
            Long.parseLong(checkText);
            if (checkText.length()>=11){
                loPhone.setErrorEnabled(false);
                return true;
            }else{
                //loPhone.setErrorEnabled(false);
                phoneNr.setError("invalid phone number");
                return false;
            }
        }catch(Exception e){
            Toast.makeText(getApplicationContext(),phoneText,Toast.LENGTH_SHORT).show();
            //loPhone.setErrorEnabled(false);
            phoneNr.setError("phone number faulty");
            return  false;
        }
    }

    public boolean checkPhotos(int code, CardView cardView){
        if(imageFile(code,Size.MED).exists()){
            cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.colorSelected));
            return true;
        }else{
            cardView.setCardBackgroundColor(Color.RED);
            return false;
        }
    }

    private boolean checkAgreed(){
        if (!agreeToTerms.isChecked()){
            agreeToTerms.setTextColor(Color.RED);
            return false;
        }
        return true;
    }

    public boolean checkAllEntries(){
        boolean checkFirst  =checkIsEmpty(firstName,loFirstName);
        boolean checkGender =checkIsEmpty(gender,loGender);
        boolean checkDob   =checkIsEmpty(dob,loDob);
        boolean checkPhone =checkPhoneNumber();
        boolean checkFace  =checkPhotos(REQUEST_TAKE_FACE,cardFace);
        boolean checkAgree =checkAgreed();
        return (checkFirst&&
                checkGender&&
                checkDob&&
                checkPhone&&
                checkFace&&
                checkAgree);
    }


    long timestamp;
    public void onSubmit(){
        if (checkAllEntries()){
            timestamp=new Date().getTime();
            JSONObject json=writeJson();
            setSubmissionParams();
            Map<String,DataPart> files= getSubmittableFiles();
            Map<String,String> headers= getHeaders();
            if (json!=null){
                //registerUser(json,this);


                registerWithFormData(json, files, headers,this);
                //send json
            }
        }else{
            textWarning.setVisibility(View.VISIBLE);
            svParent.fullScroll(View.FOCUS_UP);
        }
    }

    public Map<String, DataPart> getSubmittableFiles(){
        Map<String, DataPart> files=new HashMap<>();
        files.put("FACE",new DataPart(timestamp+".jpg",MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_FACE,Size.MED)),"image/jpeg"));
        files.put("THUMB",new DataPart("THUMB.jpg",MiscellaneousUtils.readFileToBytes(imageFile(REQUEST_TAKE_FACE,Size.THUMB)),"image/jpeg"));
        return files;
    }

    public Map<String, String> getHeaders(){
        return null;
    }

    String mBase64;
    public JSONObject writeJson(){
        try {
            String lastNameStr=firstName.getText()==null?"":lastName.getText().toString();
            String phoneStr=phoneNr.getText()==null?"":MiscellaneousUtils.depuratePhone(phoneNr.getText().toString());
            String cityStr=city.getText()==null?"":city.getText().toString();

            JSONObject json=new JSONObject();
            json.put("firstName",firstName.getText().toString());
            json.put("lastName",lastNameStr);
            json.put("dob",MiscellaneousUtils.getDateString(dateOfBirth));
            json.put("gender",gender.getText().toString().substring(0,1));
            json.put("phone",phoneStr);
            json.put("city",cityStr);
            json.put("sharePhone",sharePhone.isChecked()?1:0);
            json.put("extra","");
            json.put("timestamp",timestamp);
            return json;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


    //sync to server
    public int requestMethod;
    public String apiExtension;

    public void setSubmissionParams(){
        requestMethod=Request.Method.POST;
        apiExtension="client";
    }

    public int doOnSuccess(JSONObject json) throws JSONException{
        int id=json.getInt("taxiId");
        String token=json.getString("token");
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString("taxiId",CustomUtils.getOwnStringId(id));
        editor.putString("token",token);
        editor.apply();
        return id;
    }


    public void registerWithFormData(JSONObject json, Map<String,DataPart> files, Map<String,String> headers, Context context){
        //response dialog
        final Dialog loadingDialog=makeRegistrationDialog(context,true,false,"null");
        loadingDialog.show();
        //request part
        MultipartRequest multipartRequest=new MultipartRequest(requestMethod, Constants.SERVER_URL + apiExtension, headers,  new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                String resultResponse = new String(response.data);
                try {
                    JSONObject result = new JSONObject(resultResponse);
                    String res = result.getString("response");

                    if (res.equals("OK")) {
                        register.setEnabled(false);
                        int id=doOnSuccess(result);
                        savePrefs();
                        if (loadingDialog.isShowing())
                            loadingDialog.dismiss();
                        Dialog successDialog=makeRegistrationDialog(context,false,false,CustomUtils.getOwnStringId(id));
                        successDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                Intent i = new Intent(mCurrActivity, mNextActivity);
                                startActivity(i);
                            }
                        });
                        successDialog.show();
                        //all ok display id
                    }else{
                        //error on server side fix
                        Toast.makeText(context,"json could not be read",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                if (loadingDialog.isShowing())
                    loadingDialog.dismiss();
                Dialog errorDialog=makeRegistrationDialog(context,false,true,"null");
                errorDialog.show();
                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {

                        errorMessage = "Failed to connect server or timeout, check your internet connection";

                } else {
                    String result = new String(networkResponse.data);
                    Log.d("volleyErrorResponse",result);
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message+" Please login again";
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message+ " Check your inputs";
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message+" Something is getting wrong";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(context,errorMessage,Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                Log.d("volleyText",json.toString());
                params.put("textData", json.toString());
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>(files);
                //params.put("thumb", new DataPart("test_thumb.jpg", MiscellaneousUtils.readFileToBytes(thumb), "image/jpeg"));
                //params.put("cover", new DataPart("file_cover.jpg", AppHelper.getFileDataFromDrawable(getBaseContext(), mCoverImage.getDrawable()), "image/jpeg"));
                return params;
            }
        };

        MySingleton.getInstance(context).addToRequestQueue(multipartRequest);
    }


    public void  savePrefs(){
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString("firstname",firstName.getText().toString());
        editor.putString("lastname",lastName.getText().toString());
        editor.putString("gender",gender.getText().toString().contentEquals("male")?"m":"f");
        editor.putString("phone",phoneNr.getText()==null?"":MiscellaneousUtils.depuratePhone(phoneNr.getText().toString()));
        editor.putString("residencecity",city.getText().toString());
        editor.putString("photofacepath",imageFile(REQUEST_TAKE_FACE,Size.MED).getAbsolutePath());
        editor.putLong("dob",dateOfBirth.getTime());
        editor.putBoolean("sharephone",sharePhone.isChecked());
        editor.putBoolean("agreetoterms",agreeToTerms.isChecked());
        editor.apply();
    }

    public String titleOngoing, textOngoing, titleSuccess, textSuccess, titleError, textError;

    public void defineRegistrationDialogStrings(String id){
        titleOngoing="Registration ongoing";
        textOngoing="Please wait while the registration process finishes";
        titleSuccess="Registration complete";
        textSuccess="You have been registered successfully.<br>Your user ID is "+"<b>" + id + "</b> <br>This ID will be useful if you ever lose your phone on a ride";
        titleError="Error";
        textError="An error occurred while registering.<br> Please make sure your internet connection is working and try again later";
    }

    public Dialog makeRegistrationDialog(Context context, final boolean loading, final boolean error, String id){
        defineRegistrationDialogStrings(id);
        final Dialog dialog=new Dialog(context);
        dialog.setContentView(R.layout.dialog_register);
        TextView titleView=dialog.findViewById(R.id.tv_title_dialog);
        TextView textView=dialog.findViewById(R.id.tv_text);
        ImageView imageView=(ImageView) dialog.findViewById(R.id.iv_check);
        ProgressBar loadingView=(ProgressBar) dialog.findViewById(R.id.pb_loading);
        Button closeBtn=dialog.findViewById(R.id.bt_close);
        String title;
        String text;
        if(loading){
            title=titleOngoing;
            text=textOngoing;
            imageView.setVisibility(View.GONE);
            loadingView.setVisibility(View.VISIBLE);
        }else{
            imageView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
            if (!error){
                title=titleSuccess;
                text= textSuccess;
                imageView.setImageResource(R.drawable.confirm);
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorGreen), android.graphics.PorterDuff.Mode.MULTIPLY);
            }else{
                title=titleError;
                text= textError;
                imageView.setImageResource(R.drawable.close);
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorRed), android.graphics.PorterDuff.Mode.MULTIPLY);
            }
        }
        textView.setText(Html.fromHtml(text));
        titleView.setText(title);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!loading && !error) {
                    Intent i = new Intent(mCurrActivity, mNextActivity);
                    startActivity(i);
                }
                dialog.dismiss();
            }
        });
        return dialog;
    }

    public Dialog makeTermsOfUseDialog(Context context){
        final Dialog dialog=new Dialog(context);
        dialog.setContentView(R.layout.dialog_register);
        TextView titleView=dialog.findViewById(R.id.tv_title_dialog);
        TextView textView=dialog.findViewById(R.id.tv_text);
        ImageView imageView=(ImageView) dialog.findViewById(R.id.iv_check);
        ProgressBar loadingView=(ProgressBar) dialog.findViewById(R.id.pb_loading);
        Button closeBtn=dialog.findViewById(R.id.bt_close);

        imageView.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);

        titleView.setText("Terms of Use");
        textView.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
                "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
                "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis " +
                "aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat " +
                "nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui " +
                "officia deserunt mollit anim id est laborum.");

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        return dialog;
    }


}
