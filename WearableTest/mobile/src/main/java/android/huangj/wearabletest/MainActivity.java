package android.huangj.wearabletest;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


public class MainActivity extends FragmentActivity
        implements GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener
{
    public final static String TAG = "MainActivity";
    public final static String VERSION = "Version 0.0.3\n";
    public final static String PATH_DISTANCE = "friendtraker/distance";
    public final static String PATH_BEARING = "friendtraker/bearing";

    private final static double HOMELat = 32.821621;
    private final static double HOMELong = -117.128385;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Marker mPOIMarker;
    private Location mMyLocation;
    private Location mPOILocation;

    private TextView mVersion;
    private TextView mLatLongView;
    private TextView mPOILatLongView;

    private boolean mIsConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVersion = (TextView)findViewById(R.id.version);
        mLatLongView = (TextView)findViewById(R.id.myLatLong);
        mPOILatLongView = (TextView)findViewById(R.id.poiLatLong);

        mVersion.setText(VERSION);
        mLatLongView.setText("lat/lng: (" + HOMELat + "," + HOMELong + ")");

        mIsConnected = false;

        setupGoogleApiClient();
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMapClick(LatLng point)
    {
        mPOILocation.setLatitude(point.latitude);
        mPOILocation.setLongitude(point.longitude);

        // get distance
        float distance = (mMyLocation != null && mPOILocation != null) ?
                mMyLocation.distanceTo(mPOILocation): 0;
        float bearing = (mMyLocation != null && mPOILocation != null) ?
                mMyLocation.bearingTo(mPOILocation) : 0;
        //send data to watch
        sendDataToWearable(distance, bearing);

        // update UI
        mPOILatLongView.setText(point.toString());
        mMap.animateCamera(CameraUpdateFactory.newLatLng(point));
        mPOIMarker.setPosition(point);
    }
    @Override
    public void onMapLongClick(LatLng point){}

    private void sendDataToWearable(float distance, float bearing)
    {
        if (mIsConnected)
        {
            Log.i(TAG, "Sending To Wearable, distance=" + distance + " ,bearing=" + bearing);

            String path[] = new String[2];
            String data[] = new String[2];
            // store distance data
            path[0] = PATH_DISTANCE;
            data[0] = String.valueOf(distance);
            // store bearing data
            path[1] = PATH_BEARING;
            data[1] = String.valueOf(bearing);

            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(path, data).start();
        }
    }

    private void setupGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                        mIsConnected = true;
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                        // Request access only to the Wearable API
                .build();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded()
    {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap()
    {
        LatLng home = new LatLng(HOMELat, HOMELong);
        mMyLocation = new Location("My Location");
        mMyLocation.setLatitude(HOMELat);
        mMyLocation.setLongitude(HOMELong);
        mPOILocation = new Location("POI Location");
        mPOILocation.setLatitude(HOMELat);
        mPOILocation.setLongitude(HOMELong);

        mMap.setMyLocationEnabled(true);
        mMap.addMarker(new MarkerOptions().position(home).title("Me")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mPOIMarker = mMap.addMarker(new MarkerOptions().position(home).title("POI"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 15));

        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    private class SendToDataLayerThread extends Thread {
        String mPaths[];
        String mData[];

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String paths[], String data[]) {
            mPaths = paths;
            mData = data;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes())
            {
                for (int index = 0; index < mPaths.length; index++)
                {
                    if (index < mData.length)
                    {
                        String path = mPaths[index];
                        String data = mData[index];
                        MessageApi.SendMessageResult result =
                                Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, data.getBytes()).await();
                        if (result.getStatus().isSuccess()) {
                            Log.v("myTag", "Message: {" + mData + "} sent to: " + node.getDisplayName());
                        }
                        else {
                            // Log an error
                            Log.v("myTag", "ERROR: failed to send Message");
                        }
                    }
                }
            }
        }
    }
}
