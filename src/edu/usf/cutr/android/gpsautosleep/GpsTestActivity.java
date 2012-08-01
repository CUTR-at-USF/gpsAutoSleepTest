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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;

/**
 * This Activity contains several sub activities to show different GPS info.
 * The intent of this application is to allow a user to test different GPS Auto-Sleep 
 * settings.
 * 
 * More info on GPS Auto-Sleep here:
 * http://www.locationaware.usf.edu/ongoing-research/technology/gps-auto-sleep/
 * 
 * @author lockwood
 * @author barbeau (GPS Auto Sleep integration)
 *
 */
public class GpsTestActivity extends TabActivity
        implements LocationListener, GpsStatus.Listener {
    private static final String TAG = "GpsTestActivity";


	private boolean isRecording = false;
    private int currentInterval = -1;
    private int fixCount = 0;
    private LocationManager mService;
    private LocationProvider mProvider;
    private GpsStatus mStatus;
    private ArrayList<SubActivity> mSubActivities = new ArrayList<SubActivity>();
    boolean mStarted;
    private Location mLastLocation;
    private float curBattLevel = -1;
    private BroadcastReceiver batteryReceiver;
    private File root;
    private File fileDir;
    private File file;
    private FileWriter filewriter;
    private BufferedWriter out;
    private final Timer outputFileTimer = new Timer();
    private long lastFixTime = System.currentTimeMillis();
    
    private TimerTask write2File = new TimerTask() {

		@Override
		public void run() {
			try {
				out.write(Calendar.getInstance().get(Calendar.HOUR)+":"+Calendar.getInstance().get(Calendar.MINUTE)
						+":"+Calendar.getInstance().get(Calendar.SECOND)+","+fixCount+","+((lastFixTime > 0)?DateUtils.getRelativeTimeSpanString(
								lastFixTime, mLastLocation.getTime(), DateUtils.SECOND_IN_MILLIS):-1)+","+curBattLevel);
				;
				out.newLine();
			} catch (Exception e) {
				
			}
			
		}
    	
    };

    private static GpsTestActivity sInstance;

    interface SubActivity extends LocationListener {
        public void gpsStart();
        public void gpsStop();
        public void onGpsStatusChanged(int event, GpsStatus status);
    }

    static GpsTestActivity getInstance() {
        return sInstance;
    }

    void addSubActivity(SubActivity activity) {
        mSubActivities.add(activity);
    }

//    private void gpsStart() {
//        if (!mStarted) {
//            mService.requestLocationUpdates(mProvider.getName(), 1000, 0.0f, this);
//            mStarted = true;
//        }
//        for (SubActivity activity : mSubActivities) {
//            activity.gpsStart();
//        }
//    }
//    
//    private void gpsStop() {
//        if (mStarted) {
//            mService.removeUpdates(this);
//            mStarted = false;
//        }
//        for (SubActivity activity : mSubActivities) {
//            activity.gpsStop();
//        }
//    }

    public boolean sendExtraCommand(String command, Bundle extras) {
        return mService.sendExtraCommand(LocationManager.GPS_PROVIDER, command, extras);
    }
    
    private void startRecording() {
    	if(isRecording == true)
    		return;
        isRecording = !isRecording;
    	root = Environment.getExternalStorageDirectory();

		//check sdcard permission
    	try {
    		if (root.canWrite()){ 

    			fileDir = new File(root.getAbsolutePath()+"/battery_data/");  
    			fileDir.mkdirs();  

    			file= new File(fileDir, "outputFile.csv");  
    			filewriter = new FileWriter(file);  
    			out = new BufferedWriter(filewriter);
    			out.write("Current Interval: "+currentInterval);
    			out.newLine();
    			out.write((new Date()).toString()+"-> STARTED\n--------------------------------------------------------------------------------\n");
    			out.write("Time, Fixes, LastFix, Level\n");
    			/*
    		     * After 60 seconds, write battery level & other info. to file
    		     */
    		    outputFileTimer.schedule(write2File,1000 * 10, 1000 * 10);
    		}
    	}
    	catch(Exception e) {
    		Log.d(TAG,"Failed to create file");
    		return;
    	}
    }
    private void stopRecording() {
    	if(isRecording == false)
    		return;
        isRecording = !isRecording;
    	try {
    		write2File.cancel();
    		write2File = null;
    		outputFileTimer.cancel();
    		outputFileTimer.purge();
    		out.write("--------------------------------------------------------------------------------\n"+(new Date()).toString()+"-> COMPLETED");
    		out.flush();
    		out.close();
    		filewriter.close();
    	}
    	catch (Exception e) {
    	}
    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        sInstance = this;

        mService = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mProvider = mService.getProvider(LocationManager.GPS_PROVIDER);
        if (mProvider == null) {
            // FIXME - fail gracefully here
            Log.e(TAG, "Unable to get GPS_PROVIDER");
        }
        mService.addGpsStatusListener(this);

        final TabHost tabHost = getTabHost();
        final Resources res = getResources();

        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator(res.getString(R.string.gps_status_tab))
                .setContent(new Intent(this, GpsStatusActivity.class)));

        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator(res.getString(R.string.gps_map_tab))
                .setContent(new Intent(this, GpsMapActivity.class)));

        tabHost.addTab(tabHost.newTabSpec("tab3")
                .setIndicator(res.getString(R.string.gps_sky_tab))
                .setContent(new Intent(this, GpsSkyActivity.class)));
        
        tabHost.addTab(tabHost.newTabSpec("tab4")
            .setIndicator(res.getString(R.string.gps_auto_sleep_tab))
            .setContent(new Intent(this, GpsAutoSleepActivity.class)));
        
        
        /*
         * Creates a Receiver to listen to changes in the battery state
         *  sets curBattLevel to current battery level
         */
        batteryReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            curBattLevel=((float)intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1))/intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)*100;
            //Log.d(TRACITConstants.TAG,"Current Battery Level: "+curBattLevel);
          }
        }; 
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
        
        
        
        //Always start GPS on startup
        currentInterval = 300;
        mService.requestLocationUpdates(mProvider.getName(), currentInterval*1000, 0.0f, this);
    }

    @Override
    protected void onDestroy() {
        mService.removeGpsStatusListener(this);
        mService.removeUpdates(this);
        unregisterReceiver(batteryReceiver);
        stopRecording();
        super.onDestroy();
    }

   boolean createOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gps_menu, menu);
        return true;
    }

    boolean prepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.gps_start);
        if (item != null) {
            if (mStarted) {
                item.setTitle(R.string.gps_stop);
            } else {
                item.setTitle(R.string.gps_start);
            }
        }

        item = menu.findItem(R.id.delete_aiding_data);
        if (item != null) {
            item.setEnabled(!mStarted);
        }

        item = menu.findItem(R.id.send_location);
        if (item != null) {
            item.setEnabled(mLastLocation != null);
        }

        return true;
    }

    boolean optionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.gps_start:
                if (mStarted) {
                    stopAutoSleep();
                } else {
                    startAutoSleep();
                }
                return true;
            case R.id.change:
            	final LocationListener o = this;
            	AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            	builder2.setMessage("Record or Change Interval?")
            	       .setPositiveButton(isRecording?"Stop Recording":"Start Recording", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                if(!isRecording)
            	                	startRecording();
            	                else
            	                	stopRecording();
            	           }
            	       })
            	       .setNegativeButton("Change Interval", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	        	   final CharSequence[] intervals = {"4","8","15","30","60","150","300"};
            	            	mService.removeUpdates(o);//Try without removing updates
            	            	AlertDialog.Builder builder = new AlertDialog.Builder((Context) o);
            	            	builder.setTitle("Pick an interval");
            	            	builder.setCancelable(false);
            	            	builder.setItems(intervals, new DialogInterface.OnClickListener() {
            	            	    public void onClick(DialogInterface dialog, int item) {
            	            	        Toast.makeText(getApplicationContext(), intervals[item], Toast.LENGTH_SHORT).show();
            	            	        currentInterval =  Integer.parseInt(intervals[item].toString());
            							mService.requestLocationUpdates(mProvider.getName(),currentInterval*1000,0.0f, o);
            	            	    }
            	            	});
            	            	AlertDialog alert = builder.create();
            	            	alert.show();
            	           }
            	           /*
            	            	        	AlertDialog.Builder alert = new AlertDialog.Builder((Context) o);

                	            	        alert.setTitle("Interval: "); 
                	            	        //alert.setMessage("Enter page number:"); 

                	            	        // Set an EditText view to get user input 
                	            	        final EditText input = new EditText((Context) o);
                	            	        input.setInputType(InputType.TYPE_CLASS_NUMBER);
                	            	        alert.setView(input);

                	            	        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                	            	        public void onClick(DialogInterface dialog, int whichButton) {
                	            	            currentInterval = Integer.parseInt(input.getText().toString());
                	            	        }
                	            	        });
                	            	        alert.show();
            	            */
            	       });
            	AlertDialog alert2 = builder2.create();
            	alert2.show();
            	
            	
            	
            	
            	
            	
            	return true;
            	
            case R.id.delete_aiding_data:
                sendExtraCommand("delete_aiding_data", null);
                return true;

            case R.id.send_location:
                sendLocation();
                return true;

            case R.id.force_time_injection:
                sendExtraCommand("force_time_injection", null);
                return true;

            case R.id.force_xtra_injection:
                sendExtraCommand("force_xtra_injection", null);
                return true;
        }

        return false;
    }

    public void onLocationChanged(Location location) {
    	++fixCount;
    	lastFixTime = location.getTime();
        mLastLocation = location;

        for (SubActivity activity : mSubActivities) {
            activity.onLocationChanged(location);
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        for (SubActivity activity : mSubActivities) {
            activity.onStatusChanged(provider, status, extras);
        }
    }

    public void onProviderEnabled(String provider) {
        for (SubActivity activity : mSubActivities) {
            activity.onProviderEnabled(provider);
        }
    }

    public void onProviderDisabled(String provider) {
        for (SubActivity activity : mSubActivities) {
            activity.onProviderDisabled(provider);
        }
    }

    public void onGpsStatusChanged(int event) {
        mStatus = mService.getGpsStatus(mStatus);
        for (SubActivity activity : mSubActivities) {
            activity.onGpsStatusChanged(event, mStatus);
        }
    }

    private void sendLocation() {
        if (mLastLocation != null) {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                                Uri.fromParts("mailto", "", null));
            String location = "http://maps.google.com/maps?geocode=&q=" +
                    Double.toString(mLastLocation.getLatitude()) + "," +
                    Double.toString(mLastLocation.getLongitude());
            intent.putExtra(Intent.EXTRA_TEXT, location);
            startActivity(intent);
        }
    }
    
    /**
     * Start the GPS Auto-Sleep feature
     */    
    private void startAutoSleep(){
      if (!mStarted) {
        
        boolean result = sendExtraCommand("start_auto_sleep", null);
        mStarted = true;

        if(!result){
          Toast toast = Toast.makeText(this, "start_auto_sleep - returned false", Toast.LENGTH_SHORT);
          toast.show();
        }
        
      }
    }
    
    /**
     * Stop the GPS Auto-Sleep feature
     */  
    private void stopAutoSleep(){
      if (mStarted) {
        boolean result = sendExtraCommand("stop_auto_sleep", null);
        mStarted = false;
        
        if(!result){
          Toast toast = Toast.makeText(this, "stop_auto_sleep - returned false", Toast.LENGTH_SHORT);
          toast.show();
        }
      }
    }
}
