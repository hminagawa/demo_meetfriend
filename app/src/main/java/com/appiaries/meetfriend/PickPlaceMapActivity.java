/*
 * Copyright 2013 Team EGG. Co.ltd.
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
package com.appiaries.meetfriend;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.appiaries.baas.sdk.AB;
import com.appiaries.baas.sdk.ABDBObject;
import com.appiaries.baas.sdk.ABException;
import com.appiaries.baas.sdk.ABResult;
import com.appiaries.baas.sdk.ResultCallback;
import com.appiaries.meetfriend.content.PushBroadcastReceiver;
import com.appiaries.meetfriend.fragment.PushRegistrationFragment;
import com.appiaries.meetfriend.util.AddressSearchUtils;
import com.appiaries.meetfriend.util.Installation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This is where you pick a certain point from the map.
 *
 * @author yoshihide-sogawa
 */
public class PickPlaceMapActivity extends AppCompatActivity implements GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {

    // Request code when connection fails.
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    // ID used to disconnect from Google Play Services.
    private static final int GOOGLE_API_CLIENT_ID = 908;

    // Collection ID
    private static final String COLLECTION_ID = "user_location";

    // Object ID
    private static final String OBJECT_ID = "place_data";

    /**
     * {@link com.google.android.gms.common.api.GoogleApiClient}
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * {@link com.google.android.gms.maps.GoogleMap}
     */
    private GoogleMap mMap;

    // Markers list on the map.
    private List<Marker> mMarkerList;

    // Shows where YOU are.
    // {@link Marker}
    // Making it a "list" to switch markers.
    private List<Marker> mMyMarker;

    // AsyncTask to retrieve the place name.
    private AsyncTask<Void, Void, Address> mMarkerAddressTask;

    /** Notification */
    private View mNotificationInfoView;

    // Message entry field.
    private EditText mMessageText;

    // Submit button.
    private View mAddButton;

    /** Tag for logs */
    private static final String TAG = "AppiariesReg";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_place_map);

        Log.i(TAG, "PickPlaceMapActivity - onCreate()");

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(new PushRegistrationFragment(), "registration").commit();
        }

        // Appiaries initialization.
        AB.Config.setDatastoreID(Config.DATA_STORE_ID);
        AB.Config.setApplicationID(Config.APP_ID);
        AB.Config.setApplicationToken(Config.APP_TOKEN);
        AB.activate(getApplicationContext());

        // Notification View
        mNotificationInfoView = findViewById(R.id.notification_info);
        mNotificationInfoView.setVisibility(View.GONE);

        final Intent intent = getIntent();
        Log.i(TAG, "intent action: " + intent.getAction());

        // Show notification view upon when notification is tapped.
        if (intent != null && PushBroadcastReceiver.ACTION_NOTIFICATION_OPEN.equals(intent.getAction())) {

            Log.i(TAG, "Seems like the notification is tapped.");

            mNotificationInfoView.setVisibility(View.VISIBLE);
            final String title = intent.getStringExtra(Config.NOTIFICATION_KEY_TITLE);
            final String message = intent.getStringExtra(Config.NOTIFICATION_KEY_MESSAGE);
            ((TextView) findViewById(R.id.notification_title)).setText(title);
            ((TextView) findViewById(R.id.notification_message)).setText(message);

            // Hide the notification view after 10 seconds.
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, e.getMessage());
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mNotificationInfoView.setVisibility(View.GONE);
                        }
                    });
                }
            };
            thread.start();
        }

        mMessageText = (EditText) findViewById(R.id.place_message);

        mAddButton = findViewById(R.id.add_place);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PickPlaceMapActivity.this, R.string.wait_for_minute, Toast.LENGTH_SHORT).show();
                execUpdateFlow(true);
            }
        });
        mAddButton.setEnabled(false);

        // Handling geological information.
        setupLocation();
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        if (mMarkerAddressTask != null) {
            mMarkerAddressTask.cancel(true);
        }
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pick_place_map, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        // Updating.
        if (id == R.id.action_refresh) {
            // Showing the message.
            Toast.makeText(this, R.string.wait_for_minute, Toast.LENGTH_SHORT).show();
            execUpdateFlow(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Request to resolve the connection error for Geo services.
        if (requestCode == CONNECTION_FAILURE_RESOLUTION_REQUEST) {
            if (isServicesAvailable()) {
                mGoogleApiClient.connect();
            }
        }
    }

    /**
     * Creating Geo information retrieval feature.
     */
    private void setupLocation() {
        // GoogleApiClientの構築
        final GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this);
        builder.addApi(LocationServices.API);
        builder.addConnectionCallbacks(this);
        builder.addOnConnectionFailedListener(this);
        builder.enableAutoManage(this, GOOGLE_API_CLIENT_ID, this);
        mGoogleApiClient = builder.build();
    }

    /**
     * Handling everything associated with Geo information updates.
     *
     * @param isUpdateMyPlace "true" if updating your own poistion.
     */
    private void execUpdateFlow(final boolean isUpdateMyPlace) {
        ABDBObject dbObject = new ABDBObject(COLLECTION_ID);
        dbObject.setID(OBJECT_ID);
        AB.DBService.fetch(dbObject, new ResultCallback<ABDBObject>() {
            @Override
            public void done(ABResult<ABDBObject> abResult, ABException e) {
                if (e != null) {
                    showErrorMessage("Failure:" + e);
                    return;
                }

                // Get the upper most part of JSON.
                @SuppressWarnings("unchecked")
                HashMap<String, Object> rootMap = (HashMap<String, Object>) abResult.getData().getOriginalData();
                // Update the markers.
                handleMarkers(rootMap);

                // In case you are not updating your own position.
                if (!isUpdateMyPlace) {
                    Toast.makeText(PickPlaceMapActivity.this, R.string.place_data_refresh, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Updating your own position.
                updateMyPlace(rootMap);

            }
        });
    }

    /**
     * Updating your own position.
     * TODO: Omitting procedures for simultaneous accesses.
     * TODO: For precisions, make sure to check if the data is updated.
     *
     * @param rootMap {@link HashMap} for the root.
     */
    private void updateMyPlace(HashMap<String, Object> rootMap) {
        // Deleting the params automatically added by the server.
        rootMap.remove("_uby");
        rootMap.remove("_id");
        rootMap.remove("_uts");
        rootMap.remove("_cts");
        rootMap.remove("_cby");
        rootMap.remove("_coord");
        // Overwriting your position.
        rootMap.put(Installation.id4Ap(this), createMyPlaceData());

        // Register rootMap
        ABDBObject placeData = new ABDBObject(COLLECTION_ID);
        // Set the object ID.
        placeData.setID(OBJECT_ID);
        // apply for the update.
        placeData.apply();
        // Create a data for update (based on data retrieved).
        for (String key : rootMap.keySet()) {
            placeData.put(key, rootMap.get(key));
        }
        placeData.save(new ResultCallback<ABDBObject>() {
            @Override
            public void done(ABResult<ABDBObject> abResult, ABException e) {
                if (e != null) {
                    showErrorMessage("Failure:" + e);
                    return;
                }
                Toast.makeText(PickPlaceMapActivity.this, R.string.place_data_success, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Showing markers / deleting markers.
     *
     * @param rootMap JSON parameters
     */
    private void handleMarkers(HashMap<String, Object> rootMap) {
        // Retrieving Geo information of everyone else.
        String myUuid = Installation.id4Ap(PickPlaceMapActivity.this);
        Set<String> uuids = rootMap.keySet();

        // Retrieving already existing markers (for deletion).
        List<Marker> deleteMarkerList = new ArrayList<>();
        for (Marker marker : mMarkerList) {
            deleteMarkerList.add(marker);
        }

        // Adding new markers.
        for (String uuid : uuids) {
            // Not showing your own position.
            if (myUuid.equals(uuid)) {
                continue;
            }

            // Ignore the keys automatically given (from the server).
            Object rootMapObject = rootMap.get(uuid);
            if (!(rootMapObject instanceof HashMap)) {
                continue;
            }

            // Create a marker.
            @SuppressWarnings("unchecked")
            HashMap<String, String> placeData = (HashMap<String, String>) rootMapObject;
            addUsersMarker(placeData.get("message"), placeData.get("lat"), placeData.get("long"));
        }

        // Delete old markers.
        for (Marker marker : deleteMarkerList) {
            marker.remove();
            mMarkerList.remove(marker);
        }

        // Show markers.
        for (Marker marker : mMarkerList) {
            marker.showInfoWindow();
        }
    }

    /**
     * Show markers.
     *
     * @param title Title
     * @param point {@link LatLng}
     */
    private void showMyMarker(final String title, final LatLng point) {
        // Delete all markers.
        for (Marker marker : mMyMarker) {
            marker.remove();
            mMyMarker.remove(marker);
        }
        // Temporally add markers.
        Marker marker = mMap.addMarker(new MarkerOptions()//
                .title(title)//
                .position(point));//
        mMyMarker.add(marker);
        marker.showInfoWindow();
    }

    /**
     * Create Geo position for your own.
     * Creating Latitude, Longitude, and a message.
     *
     * @return {@link HashMap} to show the Geo position of yourself.
     */
    private HashMap<String, String> createMyPlaceData() {
        HashMap<String, String> myPlaceMap = new HashMap<>();
        // Latitude + Longitude
        Marker targetMarker = mMyMarker.get(0);
        LatLng latlng = targetMarker.getPosition();
        myPlaceMap.put("long", String.valueOf(latlng.longitude));
        myPlaceMap.put("lat", String.valueOf(latlng.latitude));
        // Message (add default message if empty)
        String message = mMessageText.getText().toString();
        if (TextUtils.isEmpty(message)) {
            message = getString(R.string.no_message);
        }
        myPlaceMap.put("message", message);
        return myPlaceMap;
    }

    /**
     * Add the user markers.
     *
     * @param title Title
     * @param lat   Latitude
     * @param lng   Longitude
     */
    private void addUsersMarker(String title, String lat, String lng) {
        final LatLng point = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
        final MarkerOptions options = new MarkerOptions();
        options.title(title);
        options.position(point);
        final Marker marker = mMap.addMarker(options);
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        mMarkerList.add(marker);
    }

    /**
     * Show error messages.
     *
     * @param message Message
     */
    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Check the service connection.
     *
     * @return If the services connected.
     */
    private boolean isServicesAvailable() {
        // Check if Google Play Service is available.
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        return resultCode == ConnectionResult.SUCCESS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onConnectionFailed(final ConnectionResult result) {
        // If able to resolve the error...
        if (result.hasResolution()) {
            try {
                // Begin Activity to resolve the error.
                result.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                showErrorMessage(e.getLocalizedMessage());
            }
        }
        // In case not resolved.
        else {
            showErrorMessage("onConnectionFailed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnected(final Bundle connectionHint) {
        // Retrieve Geo pos last retrieved.
        final Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LatLng latLng;
        if (location == null) {
            // Show TOKYO
            latLng = new LatLng(35.689887, 139.693945);
        } else {
            latLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
        final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12);
        mMap.moveCamera(cameraUpdate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionSuspended(int cause) {
        // Write something if needed.
        System.out.println("suspend");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMarkerList = new ArrayList<>();
        mMyMarker = new ArrayList<>();
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        final UiSettings settings = mMap.getUiSettings();
        settings.setCompassEnabled(false);
        settings.setMyLocationButtonEnabled(false);
        settings.setMapToolbarEnabled(false);
        execUpdateFlow(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapClick(LatLng point) {
        // Shows Geo pos names and markers.
        showMarkerWithPlaceName(point);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapLongClick(final LatLng point) {
        // Shows Geo pos names and markers.
        showMarkerWithPlaceName(point);
    }

    /**
     * Shows markers with their names.
     *
     * @param point {@link LatLng}
     */
    private void showMarkerWithPlaceName(final LatLng point) {
        // Stop if the retrieval is in progress.
        if (mMarkerAddressTask != null) {
            mMarkerAddressTask.cancel(true);
        }

        showMyMarker(getString(R.string.show_marker_default), point);
        mAddButton.setEnabled(true);

        // Async task for retrieving Geo pos name.
        mMarkerAddressTask = new AsyncTask<Void, Void, Address>() {
            @Override
            protected Address doInBackground(Void... params) {
                return AddressSearchUtils.searchAddressSync(PickPlaceMapActivity.this, point.longitude, point.latitude);
            }

            @Override
            protected void onPostExecute(Address address) {
                // Do noting if no results.
                if (address == null) {
                    return;
                }

                // Show the marker.
                showMyMarker(address.getAddressLine(1), point);
                mAddButton.setEnabled(true);
            }
        }.execute();
    }


}
