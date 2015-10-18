/*
 * Copyright (c) 2014-2015, Mark Mullin
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of Tango Tricorder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ntx24.tricorder;


import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.atap.tangoservice.TangoEvent;
/**
 * This class loads the Java Native Interface (JNI)
 * library, 'libTricorder.so', and provides access to the
 * exposed C functions.
 * The library is packaged and installed with the application.
 * See the C file, /jni/Tricorder.c file for the
 * implementations of the native methods.
 * <p/>
 * For more information on JNI, see: http://java.sun.com/docs/books/jni/
 */

public class Tricorder extends Activity {
    /**
     * Tango permission request type marker
     */
    public static final String EXTRA_KEY_PERMISSIONTYPE = "PERMISSIONTYPE";
    /**
     * Tango permission request for motion tracking
     */
    public static final String EXTRA_VALUE_VIO = "MOTION_TRACKING_PERMISSION";
    /**
     * Tango permission request for ADF access
     */
    public static final String EXTRA_VALUE_VIOADF = "ADF_LOAD_SAVE_PERMISSION";

    public static final String TAG = Tricorder.class.getSimpleName();
    // loaded from configuration
    public static String serviceBaseUrl;
    /**
     * Quality setting for JPEG conversion
     */
    public static int mJPEGQuality;
    /**
     * Singleton reference to self
     */
    public static Tricorder singleton = null;

    static {
        System.loadLibrary("TangoWrangler");
    }

    /**
     * ID of currently selected ADF
     */
    public String mSelectedADF;
    /**
     * Max entries allowed for the location queue
     */
    public int mLocationQueueCapacity;
    /**
     * Max entries allowed for the pose queue
     */
    public int mPoseQueueCapacity;
    /**
     * Max entries allowed for the processed point cloud queue
     */
    public int mPointsQueueCapacity;
    /**
     * Max entries allowed for the output JPEG queue
     */
    public int mPictureQueueCapacity;
    /**
     * Max entries allowed for the intake bitmap queue
     */
    public int mPictureCompressorQueueCapacity;
    /**
     * Max entries allowed for the intake point cloud queue
     */
    public int mPointCompressorQueueCapacity;
    /**
     * Bridge to the underlying data stream management
     */
    public Gibraltar managedGibraltarInstance;
    /**
     * Tango localization autorecovery flag state
     */
    public boolean mAutoRecoveryOn;
    /**
     * Tango ADF creation mode flag state
     */
    public boolean mLearningModeOn;
    /**
     * Tango positional reporting flag state
     */
    public boolean mMotionTrackingOn;
    /**
     * Tango 3D environment sensing flag state
     */
    public boolean mDepthPerceptionOn;
    /**
     * Tango localization autorecovery flag state
     */
    public boolean mAutosaveADF;
    public long startRecording;

    /**
     * Android source of GPS location data
     */
    public LocationManager locationManager;
    /**
     * List of ADFs returned via prior call to Tango Native Library
     */
    public String[] mAvailableADFs;
    /**
     * Basic listener attached to the location manager
     */
    SimpleLocationListener locationListener;
    @SuppressWarnings("deprecation")
    private ActionBarDrawerToggle mDrawerToggle;
    /**
     * Scaling factor for on screen depth point display
     */
    private double mCapturePointSize;
    /**
     * Scaling factor for on screen false color range
     */
    private double mCaptureDepthRange;
    /**
     * Flag to indicate whether permissions have been requested - handles recovery
     * from tombstone.
     * <p>
     *     May no longer be necessary with later Tango builds
     */
    private Boolean mTangoPermissionsRequested = false;
    /**
     * Current fragment being displayed
     */
    private int mCurrentPanel = 0;
    private Menu mActionMenu;
    private Boolean mLastLocalizationState = false;
    private TangoEvent mLastTangoEvent = null;
    public void SetTangoEvent(TangoEvent e) { mLastTangoEvent = e; }
    /**
     * Return scaling factor for on screen false color range
     */
    public double getCaptureDepthRange() {
        return mCaptureDepthRange;
    }

    /**
     * Return scaling factor for on screen depth point display
     */
    public double getCapturePointSize() {
        return mCapturePointSize;
    }

    /**
     * Incrementally change the scaling factor for on screen false color range
     */    public void adjustCaptureDepthRange(float delta) {
        mCaptureDepthRange += delta;
        if (mCaptureDepthRange < 1.0)
            mCaptureDepthRange = 1.0;
        else if (mCaptureDepthRange > 20.0)
            mCaptureDepthRange = 20.0;
    }

    /**
     * Incrementally change the scaling factor for on screen depth point display
     */
    public void adjustCapturePointSize(float delta) {
        mCapturePointSize += delta;
        if (mCapturePointSize < 0.1)
            mCapturePointSize = 0.1;
        else if (mCapturePointSize > 10.0)
            mCapturePointSize = 10.0;
    }

    /**
     * The thread that handles the splash screen transition
     */
    private void setupSplashTransitionThread() {
        Thread background = new Thread() {
            public void run() {

                try {
                    Thread.sleep(3500);
                    SplashFragment.rendered.acquire();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Tricorder.singleton.selectPanel(0);
                        }
                    });


                    //Remove activity
                    //finish();

                } catch (Exception ignored) {

                }
            }
        };

        // start thread
        background.start();
    }

    /**
     * Do everything possible to somehow screw up and crash whilst trying to start everything
     * @param savedInstanceState  previous tombstone state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSplashTransitionThread();
        singleton = this;
        // must come first - quaestor needs this to start web service check
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        serviceBaseUrl = prefs.getString("baseServiceUrl", "http://192.168.0.42/PublicTangoServices");
        mJPEGQuality = Integer.parseInt(prefs.getString("jpegQuality", "80"));
        mLocationQueueCapacity = Integer.parseInt(prefs.getString("locationQueueCap", "15"));
        mPoseQueueCapacity = Integer.parseInt(prefs.getString("PoseQueueCap", "25"));
        mPointsQueueCapacity = Integer.parseInt(prefs.getString("PointsQueueCap", "15"));
        mPictureQueueCapacity = Integer.parseInt(prefs.getString("PictureQueueCap", "50"));
        mPictureCompressorQueueCapacity = Integer.parseInt(prefs.getString("PictureCompressorQueueCap", "90"));
        mPointCompressorQueueCapacity = Integer.parseInt(prefs.getString("PointCompressorQueueCap", "50"));
        mAutoRecoveryOn = prefs.getBoolean("AutoRecoveryOn", true);
        mLearningModeOn = prefs.getBoolean("LearningModeOn", true);
        mMotionTrackingOn = prefs.getBoolean("MotionTrackingOn", true);
        mDepthPerceptionOn = prefs.getBoolean("DepthPerceptionOn", true);
        mAutosaveADF = prefs.getBoolean("AutosaveADF", true);

        mCapturePointSize = Double.parseDouble(prefs.getString("CapturePointSize", "5.0"));
        mCaptureDepthRange = Double.parseDouble(prefs.getString("CaptureDistanceRange", "5.0"));

        Quaestor.Singleton();        // trigger quaestor into life

        if (managedGibraltarInstance == null)
            managedGibraltarInstance = new Gibraltar(Tricorder.singleton);
        setContentView(R.layout.activity_tricorder);

        String[] mPlanetTitles = getResources().getStringArray(R.array.tricorderPanels);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ListView mDrawerList = (ListView) findViewById(R.id.left_drawer);
        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // do not focus on the first damn text box you see!
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // enable ActionBar app icon to behave as action to toggle nav drawer
        ActionBar b = getActionBar();
        if(b != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        //

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        //noinspection deprecation
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        drawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectPanel(-1);
        }


        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setTitle(R.string.app_name);

        Tricorder.singleton.RequestTangoPermissions();

        updateCloudServiceStatus();
        String[] adflist = TangoJNINative.getAvailableADFs(Tricorder.singleton);
        setADFList(adflist);

    }

    /**
     * Request tango permissions if they have not already been requested
     */
    public void RequestTangoPermissions() {
        if (mTangoPermissionsRequested)
            return;

        Intent intent1 = new Intent();
        intent1.setAction("android.intent.action.REQUEST_TANGO_PERMISSION");
        intent1.putExtra(EXTRA_KEY_PERMISSIONTYPE, EXTRA_VALUE_VIO);
        startActivityForResult(intent1, 0);

        Intent intent2 = new Intent();
        intent2.setAction("android.intent.action.REQUEST_TANGO_PERMISSION");
        intent2.putExtra(EXTRA_KEY_PERMISSIONTYPE, EXTRA_VALUE_VIOADF);
        startActivityForResult(intent2, 0);
        mTangoPermissionsRequested = true;
    }

    /**
     * Show a toast message for the given short or long length
     * @param msg   What is to be shown
     * @param msgLength  For how long it is to be shown
     */
    public void showMessage(String msg, int msgLength) {
        //noinspection RedundantStringConstructorCall
        final String pass = new String(msg);
        final int passlen = msgLength;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //noinspection ResourceType
                Toast.makeText(Tricorder.singleton, pass, passlen).show();
            }
        });
    }

    /**
     * Boilerplate
     */
    @Override
    protected void onResume() {
        super.onResume();
//	        getWindow().getDecorView().setSystemUiVisibility(
//	                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//	                );
    }

    /**
     * Disconnect from the tango service on any pause
     */
    @Override
    protected void onPause() {
        super.onPause();
        TangoJNINative.disconnectService();
    }

    /**
     * Boilerplate
     */
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Back always takes you to the home fragment.
     * The UI is effectively a simple star where Home (fragment 0) is the root
     * and all other fragment are connected only to it, by exactly 1 hop.
     */
    @Override
    public void onBackPressed() {
        selectPanel(0);
    }

    /**
     * Set which fragment will be displayed.
     * I think this has a bug.  If you change the panel while the pumps are running
     * the system will lock up!
     */
    private void selectPanel(int position) {
        // do NOT let them kill the tango fragment while the pumps are running
        int ctr = 1;
        if (Gibraltar.Singleton().isPumping) {
            while (Gibraltar.Singleton().IsPumpSystemEmpty()) {
                try {
                    Thread.sleep(500);
                    if (ctr-- <= 0) {
                        showMessage("Queues draining", Toast.LENGTH_SHORT);
                        ctr = 10;
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }
        // update the main content by replacing fragments
        Bundle args;
        Fragment fragment = null;
        String title = "unknown";
        mCurrentPanel = position;
        switch (position) {
            case -1:
                fragment = new SplashFragment();
                title = "Booting";
                break;
            case 0:
                fragment = new ManagerFragment();
                title = "Manager";
                break;
            case 1:
                fragment = new ScannerFragment();
                args = new Bundle();
                args.putInt(ScannerFragment.ARG_FRAGMENT_ID, position);
                fragment.setArguments(args);
                title = "Tango Scanner";
                break;
            case 2:
                fragment = new PumpFragment();
                args = new Bundle();
                args.putInt(ScannerFragment.ARG_FRAGMENT_ID, position);
                fragment.setArguments(args);
                title = "Pump Status";
                break;
        }
        int retry = 5;
        int delay = 0;
        boolean completed = false;
        while (retry-- > 0 && !completed) {
            try {
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
                ActionBar b = getActionBar();
                if(b != null)
                    getActionBar().setTitle(title);
                completed = true;
            } catch (Exception ex) {
                try {
                    Thread.sleep(delay += 500);
                } catch (InterruptedException ignored) {
                }
            }
        }
        if (!completed) {
            showMessage("Uggh - use nav pane to manager - I blew it", Toast.LENGTH_LONG);
        }
    }

    /**
     * Present the user interface that allows the user to modify the application configuration.
     */
    public void showPrefs() {
        Fragment fragment = new PrefsFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = fragmentManager
                .beginTransaction();
        mFragmentTransaction.replace(R.id.content_frame, fragment).commit();
        ActionBar b = getActionBar();
        if(b != null)
            getActionBar().setTitle("Preferences");
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_bar, menu);
        mActionMenu = menu;
        updateCloudStatus();
        restoreLocalizationIndicator();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.about:
                AboutDialog about = new AboutDialog(this);
                about.setTitle("About Tango Tricorder");
                about.show();
            case R.id.action_settings:
                showPrefs();
                break;
            case R.id.tangoRecording:
                if (mCurrentPanel != 1)
                    break;
                // ok, change the recording state
                if (!Gibraltar.Singleton().IsPumping()) {
                    startRecording = System.currentTimeMillis();
                    Gibraltar.Singleton().startPumps();
                } else {
                    Gibraltar.Singleton().stopPumps();
                }
                break;
            case R.id.networkStatus:
                Quaestor.Singleton().mEnableCloud = !Quaestor.Singleton().mEnableCloud;
                if (!Quaestor.Singleton().mCloudServiceAvailable)
                    showMessage("No cloud service available", Toast.LENGTH_LONG);
                updateCloudStatus();
                break;
            case R.id.tangoLocalized:
                TangoJNINative.resetMotionTracking();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Handles complaints from activities involving tango, all due to permission failures
     * @param requestCode x
     * @param resultCode x
     * @param data x
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this,
                        "Motion Tracking Permission Needed!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (requestCode == 1) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this,
                        "ADF Permission Needed!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void updateCloudStatus() {
        if (mActionMenu == null)
            return;
        Boolean isAvailable = Quaestor.Singleton().mCloudServiceAvailable;
        Boolean isEnabled = Quaestor.Singleton().mEnableCloud;
        MenuItem cloudMenuItem = mActionMenu.findItem(R.id.networkStatus);
        cloudMenuItem.setChecked(Quaestor.Singleton().isUsingCloud());
        Drawable newIcon = cloudMenuItem.getIcon();
        int tintColor;
        if (isAvailable && isEnabled)
            tintColor = Color.GREEN;
        else
            tintColor = Color.RED;

        newIcon.mutate().setColorFilter(tintColor, android.graphics.PorterDuff.Mode.MULTIPLY);
        cloudMenuItem.setIcon(newIcon);
    }

    private void updateCloudServiceStatus() {
        if (Quaestor.Singleton().mCloudServiceChecked) {
            updateCloudStatus();
            return;
        }
        Thread t = new Thread() {
            public void run() {
                while (!Quaestor.Singleton().mCloudServiceChecked)
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                Tricorder.singleton.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCloudStatus();
                    }
                });

            }
        };
        t.start();
    }

    public void updateLocalizationIndicator(boolean theState) {
        new Thread() {
            public void run() {
                MediaPlayer mp = MediaPlayer.create(Tricorder.singleton, R.raw.pulse);
                try {
                    mp.start();
                    while (mp.isPlaying()) {
                        Thread.sleep(500);
                    }
                    mp.release();
                } catch (IllegalStateException ignored) {
                } catch (InterruptedException ignored) {
                }
            }
        }.start();
        mLastLocalizationState = theState;
        if (mActionMenu == null)
            return;
        restoreLocalizationIndicator();
    }

    public void restoreLocalizationIndicator() {
        MenuItem cloudMenuItem = mActionMenu.findItem(R.id.tangoLocalized);
        int tintColor;
        if (mLastLocalizationState)
            tintColor = Color.GREEN;
        else
            tintColor = Color.RED;

        Drawable newIcon = cloudMenuItem.getIcon();
        newIcon.mutate().setColorFilter(tintColor, android.graphics.PorterDuff.Mode.MULTIPLY);
        cloudMenuItem.setIcon(newIcon);

    }

    /**
     * start the GPS location reporting.
     *
     * @param host Activity to use as context for the request to access the location service
     */
    public void startLocationService(Activity host) {
        locationManager = (LocationManager) host.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new SimpleLocationListener();
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 0, locationListener);
    }

    /**
     * Terminate the GPS location reporting
     */
    public void stopLocationService() {
        locationManager.removeUpdates(locationListener);
        locationListener = null;
        locationManager = null;
    }

    /**
     * Change on screen recording indicator to match recording state
     */
    public void updateRecordingIndicator() {
        if (mActionMenu == null)
            return;
        MenuItem cloudMenuItem = mActionMenu.findItem(R.id.tangoRecording);
        if (Gibraltar.Singleton().IsPumping()) {
            String msg = Quaestor.Singleton().isUsingCloud() ? "Recording to cloud" : "Recording to memory";
            showMessage(msg, Toast.LENGTH_SHORT);
        } else
            showMessage("End Recording", Toast.LENGTH_SHORT);

        Drawable myIcon = getResources().getDrawable(Gibraltar.Singleton().IsPumping() ? R.drawable.recordingon : R.drawable.recordingoff);
        cloudMenuItem.setIcon(myIcon);
    }

    /**
     * Transfer a list of ADF files received from tango to the list of ADFs used to populate
     * the display
     * @param availableADFs List received from native library
     */
    public void setADFList(String[] availableADFs) {
        mAvailableADFs = new String[1 + availableADFs.length];
        mAvailableADFs[0] = "NONE";

        System.arraycopy(availableADFs, 0, mAvailableADFs, 1, availableADFs.length);
    }

    public enum TricorderSensorStream {Session, Location, Pose, Points, Picture}

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectPanel(position);
        }
    }

}

