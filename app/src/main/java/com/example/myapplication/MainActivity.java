package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    private ScrollView sv;
    private LinearLayout ll;
    FusedLocationProviderClient mFusedLocationClient;
    int PERMISSION_ID = 44;
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;
    TextView txtLat;
    String lat;
    String provider;
    protected Double latitude, longitude;
    protected boolean gps_enabled, network_enabled;

    private double curLatitude, curLongitude;

    private Button button1;
    private EditText text1;
    private ScrollView sView1;

    private int id = (int) (Math.random() * ((9999 - 1000) + 1)) + 1000;
    private int RANGE_METER = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ll = findViewById(R.id.linearlayout1);
        sv = findViewById(R.id.scrollview1);
        text1 = findViewById(R.id.editText);

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        final DatabaseReference myRef = (DatabaseReference) database.getReference();

        button1 = findViewById(R.id.button);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if(text1.getText().toString().isEmpty())
                    return;


                DateFormat dateFormatDB = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                DateFormat dateFormatUI = new SimpleDateFormat("HH:mm");
                Date date = new Date();


//                TableRow tr = new TableRow(tl.getContext());
//                tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
//                TextView message = new TextView(tl.getContext());

//                String str = new String();
//              str = "\nanon-" + Integer.toString(id) + " " + dateFormatUI.format(date) + "\n" + text1.getText().toString() ;

                requestNewLocationData();


                Map<String, Object> newMessage = new HashMap<>();
                newMessage.put("username","anon"+Integer.toString(id));
                newMessage.put("time", dateFormatDB.format(date));
                newMessage.put("message",text1.getText().toString());
                newMessage.put("latitude",Double.toString(curLatitude));
                newMessage.put("longitude",Double.toString(curLongitude));
                Toast.makeText(getApplicationContext(),curLatitude + " , " + curLongitude,Toast.LENGTH_SHORT).show();


                myRef.child("messages").push().setValue(newMessage);


//                message.setText(str);
//                message.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
//                message.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
//
//                tr.addView(message);
//
//                tl.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
//



                Log.d( "mesaj: ",text1.getText().toString());
                text1.setText("");
                goBottom(sv);


            }
        });


        final ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ll.removeAllViews();
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    double distance = -1;
                    if(postSnapshot.child("latitude").exists())
                        distance = calculateDistance(curLatitude, curLongitude,
                            Double.valueOf( postSnapshot.child("latitude").getValue().toString() ),
                            Double.valueOf( postSnapshot.child("longitude").getValue().toString() )
                            );
                    if(distance <= RANGE_METER) {
                        boolean owner = false;
                        if(postSnapshot.child("username").getValue().toString().compareTo("anon"+String.valueOf(id)) == 0){
                            owner = true;
                        }
                        addMessage(postSnapshot.child("message").getValue().toString(), postSnapshot.child("time").getValue().toString(),
                                postSnapshot.child("username").getValue().toString(), distance, owner
                        );
                    }
                }
                goBottom(sv);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("err", "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        final DatabaseReference myRef2 = myRef.child("messages");
        myRef2.addValueEventListener(postListener);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestNewLocationData();
        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
//                Log.d("a","b");
                //getLastLocation();

                Log.d("konum",curLatitude + " , " + curLongitude);
            }
        },0,5000);
    }


    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location == null) {
                                    requestNewLocationData();
                                } else {
                                    curLatitude = location.getLatitude();
                                    curLongitude = location.getLongitude();

                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }


    @SuppressLint("MissingPermission")
    private void requestNewLocationData(){

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        //mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            curLatitude = mLastLocation.getLatitude();
            curLongitude = mLastLocation.getLongitude();

        }
    };

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2){
        double R = 6371000;
        double dLat = (lat1-lat2);
        double dLon = (lon1-lon2);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(lat1)*Math.cos(lat2)*Math.sin(dLon/2)*Math.sin(dLon/2);

        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R*c;
    }

    private void addMessage(String message, String date, String username, double distance, boolean owner){


        View custom_cell = LayoutInflater.from(this).inflate(R.layout.custom_message, ll, false);

        TextView message2 = (TextView) custom_cell.findViewById(R.id.body);
        TextView date2 = (TextView) custom_cell.findViewById(R.id.date);
        TextView username2 = (TextView) custom_cell.findViewById(R.id.nickname);
        TextView distance2 = (TextView) custom_cell.findViewById(R.id.distance);
        ImageView avatar2 = (ImageView) custom_cell.findViewById(R.id.avatar);

        message2.setText(message);
        date2.setText(date);
        username2.setText(username);
        if(owner == true) {
            distance2.setText("");
            custom_cell.setBackgroundColor(Color.rgb(216,238,195));
        }else {
            distance2.setText(String.valueOf((int) distance) + "m");
            custom_cell.setBackgroundColor(Color.rgb(255,255,255));
        }
        ll.addView(custom_cell);
    }

    private void goBottom(final ScrollView scrollView){
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
}
