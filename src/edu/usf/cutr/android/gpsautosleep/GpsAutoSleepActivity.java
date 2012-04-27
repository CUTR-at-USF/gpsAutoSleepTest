/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.usf.cutr.android.gpsautosleep;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Iterator;

/**
 * This activity contains fields that allows the user to submit different GPS Auto-Sleep parameters.
 * More info on GPS Auto-Sleep here:
 * http://www.locationaware.usf.edu/ongoing-research/technology/gps-auto-sleep/
 * 
 * @author barbeau
 *
 */
public class GpsAutoSleepActivity extends Activity implements GpsTestActivity.SubActivity {
    private final static String TAG = "GpsAutoSleepActivity";

    private Resources mRes;

    private TextView mIntervals;
    private TextView mFirstFixTimeout;
    private TextView mHighSpeedThreshold;
    private TextView mHorAccThreshold;
    private TextView mLowSpeedThreshold;
    private TextView mBackOffTimerHorAcc;
    private TextView mBackOffTimerTimeThreshold;
    private TextView mBackOffTimerPossMove;
    private TextView mBackOffTimerProbMove;
    private TextView mBackOffTimerCertMove;
    private Button mSubmit;


    public void onLocationChanged(Location location) {
       
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // ignore
    }

    public void onProviderEnabled(String provider) {
        // ignore
    }

    public void onProviderDisabled(String provider) {
        // ignore
    }

    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mRes = getResources();
        setContentView(R.layout.gps_auto_sleep);

        mIntervals = (TextView)findViewById(R.id.intervals);
        mFirstFixTimeout = (TextView)findViewById(R.id.first_fix_timeout);
        mHighSpeedThreshold = (TextView)findViewById(R.id.high_speed_threshold);
        mHorAccThreshold = (TextView)findViewById(R.id.hor_acc_threshold);
        mLowSpeedThreshold = (TextView)findViewById(R.id.low_speed_threshold);
        mBackOffTimerHorAcc = (TextView)findViewById(R.id.backoff_timer_hor_acc);
        mBackOffTimerTimeThreshold = (TextView)findViewById(R.id.backoff_timer_time_threshold);        
        mBackOffTimerPossMove = (TextView)findViewById(R.id.backoff_timer_poss_move);
        mBackOffTimerProbMove = (TextView)findViewById(R.id.backoff_timer_prob_move);
        mBackOffTimerCertMove = (TextView)findViewById(R.id.backoff_timer_cert_move);
        mSubmit = (Button)findViewById(R.id.submit);
        
        mSubmit.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            //Submit any new parameters to the state machine
            Bundle extras = new Bundle();
            
            if(mIntervals.getText().length() != 0){
              extras.putString("interval", mIntervals.getText().toString());
            }
            
            if(mFirstFixTimeout.getText().length() != 0){
              extras.putString("first_fix_timeout", mFirstFixTimeout.getText().toString());
            }
            
            if(mHighSpeedThreshold.getText().length() != 0){
              extras.putString("high_speed_threshold", mHighSpeedThreshold.getText().toString());
            }
            
            if(mHorAccThreshold.getText().length() != 0){
              extras.putString("hor_acc_threshold", mHorAccThreshold.getText().toString());
            }
            
            if(mLowSpeedThreshold.getText().length() != 0){
              extras.putString("low_speed_threshold", mLowSpeedThreshold.getText().toString());
            }
            
            if(mBackOffTimerHorAcc.getText().length() != 0){
              extras.putString("back_off_timer_hor_acc_threshold", mBackOffTimerHorAcc.getText().toString());
            }
            
            if(mBackOffTimerTimeThreshold.getText().length() != 0){
              extras.putString("back_off_timer_time_threshold", mBackOffTimerTimeThreshold.getText().toString());
            }
            
            if(mBackOffTimerPossMove.getText().length() != 0){
              extras.putString("back_off_timer_possible_move_threshold", mBackOffTimerPossMove.getText().toString());
            }
            
            if(mBackOffTimerProbMove.getText().length() != 0){
              extras.putString("back_off_timer_probable_move_threshold", mBackOffTimerProbMove.getText().toString());
            }
            
            if(mBackOffTimerCertMove.getText().length() != 0){
              extras.putString("back_off_timer_certain_move_threshold", mBackOffTimerCertMove.getText().toString());
            }            
            
            boolean result = GpsTestActivity.getInstance().sendExtraCommand("set_auto_sleep_parameters", extras);
            
            if(!result){
              Toast toast = Toast.makeText(GpsAutoSleepActivity.this, "set_auto_sleep_parameters - returned false", Toast.LENGTH_SHORT);
              toast.show();
            }else{
              Toast toast = Toast.makeText(GpsAutoSleepActivity.this, "Submitted new settings", Toast.LENGTH_SHORT);
              toast.show();
            }
            
            //Print out current state machine parameters that the platform put in the extras bundle
            Log.d(TAG, "--- Current Auto-Sleep Settings --- ");
            Log.d(TAG, "Intervals: " + extras.getString("interval"));
            Log.d(TAG, "First Fix Timeout: " + extras.getString("first_fix_timeout"));
            Log.d(TAG, "High Speed Threshold: " + extras.getString("high_speed_threshold"));
            Log.d(TAG, "Hor. Acc. Threshold: " + extras.getString("hor_acc_threshold"));
            Log.d(TAG, "Low Speed Threshold: " + extras.getString("low_speed_threshold"));
            Log.d(TAG, "Back Off Timer - Hor. Acc. Threshold: " + extras.getString("back_off_timer_hor_acc_threshold"));
            Log.d(TAG, "Back Off Timer - Time Threshold: " + extras.getString("back_off_timer_time_threshold"));
            Log.d(TAG, "Back Off Timer - Possible Move Threshold: " + extras.getString("back_off_timer_possible_move_threshold"));
            Log.d(TAG, "Back Off Timer - Probable Move Threshold: " + extras.getString("back_off_timer_probable_move_threshold"));
            Log.d(TAG, "Back Off Timer - Certain Move Threshold: " + extras.getString("back_off_timer_certain_move_threshold"));            
            
          }
        });

        GpsTestActivity.getInstance().addSubActivity(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        GpsTestActivity gta = GpsTestActivity.getInstance();
        //setStarted(gta.mStarted);
    }

    public void gpsStart() {
    }

    public void gpsStop() {
    }

  
    public void onGpsStatusChanged(int event, GpsStatus status) {
   
    }
   
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return GpsTestActivity.getInstance().createOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return GpsTestActivity.getInstance().prepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return GpsTestActivity.getInstance().optionsItemSelected(item);
    }
}
