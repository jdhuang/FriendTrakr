package android.huangj.wearabletest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends FragmentActivity implements
        SensorEventListener, GestureDetector.OnGestureListener {
    public final static String TAG = "MainActivity";
    public final static String TAG_TOUCH = "OnTouchEvent";

    private GestureDetectorCompat mDetector;
    private SensorManager mSensorManager;
    private TextView mCompassText;
    private TextView mVersion;
    private TextView mDirectionText;
    private TextView mDistanceText;
    private TextView mHRText;
    private float[] mGravData;
    private float[] mMagData;
    private float[] mHeartRate;
    private float mCompassBearing;
    private float mPOIBearing;
    private int mWidth;
    private int mHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            @Override
            public void onLayoutInflated(WatchViewStub stub)
            {
                // Keep the Wear screen always on (for testing only!)
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                // Get screen size
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                mWidth = size.x;
                mHeight = size.y;
                Log.i(TAG, "width=" + mWidth + ", height=" + mHeight);

                mVersion = (TextView) stub.findViewById(R.id.version);
                mCompassText = (TextView) stub.findViewById(R.id.compass);
                mDirectionText = (TextView) stub.findViewById(R.id.direction);
                mDistanceText = (TextView) stub.findViewById(R.id.distance);
                mHRText = (TextView) stub.findViewById(R.id.heart_rate);

                mVersion.setText(Common.VERSION);
                mCompassText.setText("Bearing(C) NA");
                mDirectionText.setText("Bearing(M) NA");
                mDistanceText.setText("Distance NA");
                mHRText.setText("HR NA");

                // set direction dial fragment
                setDirectionDialFragment();

                // register sensor
                registerSensor();

                UpdateDirectionDial rotateDirectionDial= new UpdateDirectionDial();
                new Thread(rotateDirectionDial).start();
            }
        });
        stub.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "onTouchEvent");
                mDetector.onTouchEvent(event);
                return false;
            }
        });
        // Initialize location data
        mCompassBearing = 0;
        mPOIBearing = 0;

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
        mDetector = new GestureDetectorCompat(this, this);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.i(TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.i(TAG, "onShowPress: " + event.toString());
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.i(TAG, "onLongPress: " + event.toString());
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        Log.i(TAG, "onFling: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        Log.i(TAG, "onScroll: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.i(TAG,"onDown: " + event.toString());
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        switch (event.sensor.getType())
        {
            case Sensor.TYPE_ACCELEROMETER:
                mGravData = event.values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagData = event.values;
                break;
            case Sensor.TYPE_HEART_RATE:
                mHeartRate = event.values;
                updateHeartRate("Sensor.TYPE_HEART_RATE");
//            case 65536:
//                mHeartRate = event.values;
//                updateHeartRate("Gesture Sensor");
//            case 65538:
//                mHeartRate = event.values;
//                updateHeartRate("Wellness Passive Sensor");
            default:
                break;
        }

        updateCompassData();
    }
    private void registerSensor()
    {
        // Get SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // Get list of available sensors
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList)
        {
            Log.i(TAG, sensor.toString());
        }

        // Register sensor
        Log.i(TAG, "Registering Sensor.TYPE_ACCELEROMETER");
        Sensor accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.i(TAG, "Registering Sensor.TYPE_MAGNETIC_FIELD");
        Sensor magSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.i(TAG, "Registering Sensor.TYPE_HEART_RATE");
        Sensor heartSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
//        Log.i(TAG, "Registering Gesture Sensor");
//        Sensor gestureSensor = mSensorManager.getDefaultSensor(65536);
//        mSensorManager.registerListener(this, gestureSensor, SensorManager.SENSOR_DELAY_NORMAL);
//        Log.i(TAG, "Registering Wellness Passive Sensor");
//        Sensor wellnessSensor = mSensorManager.getDefaultSensor(65538);
//        mSensorManager.registerListener(this, wellnessSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    private void setDirectionDialFragment()
    {
        DirectionDialFragment directionDialFragment = new DirectionDialFragment();

        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.replace(R.id.direction_dial_fragment, directionDialFragment);
        transaction.commit();
    }
    private void updateCompassData()
    {
        if (mGravData != null && mMagData != null)
        {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravData,
                    mMagData);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuthInRadians = orientation[0];
                float azimuthInDegrees = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
                mCompassText.setText("Bearing(C)=" + azimuthInDegrees);
                mCompassBearing = azimuthInDegrees;
            }
        }
        else if (mGravData != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Sensor.TYPE_ACCELEROMETER");
            sb.append("\nx=");
            sb.append(mGravData[0]);
            sb.append("\ny=");
            sb.append(mGravData[1]);
            sb.append("\nz=");
            sb.append(mGravData[2]);

            mCompassText.setText(sb.toString());
        }
    }
    private void updateHeartRate(String sensorType)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(sensorType);
        sb.append(" x=");
        sb.append(mHeartRate[0]);
        sb.append(", y=");
        sb.append(mHeartRate[1]);
        sb.append(", z=");
        sb.append(mHeartRate[2]);

        Log.i(TAG, sb.toString());
        if (mHRText != null)
        {
            mHRText.setText(sb.toString());
        }
    }
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(DataLayerListenerService.PATH_KEY);
            String data = intent.getStringExtra(DataLayerListenerService.DATA_KEY);
            if (message.equals(DataLayerListenerService.PATH_DISTANCE))
            {
                Log.i("myTag", "Distance=" + data);
                mDistanceText.setText(data + "m");
            }
            else if (message.equals(DataLayerListenerService.PATH_BEARING))
            {
                Log.i("myTag", "Bearing=" + data);
                mPOIBearing = Float.valueOf(data);
                mDirectionText.setText("Bearing(M)=" + data);
            }
        }
    }

    private class UpdateDirectionDial implements Runnable
    {
        @Override
        public void run() {
            float lastDegree = 0;
            float newDegree = 0;

            while(true)
            {
                // compute POI degree relative to Compass
                newDegree = mPOIBearing - mCompassBearing;
                if (mCompassBearing < -360)
                {
                    newDegree += 360;
                }
                // Update direction dial
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.direction_dial_fragment);
                if (fragment != null && (fragment instanceof DirectionDialFragment))
                {
                    ((DirectionDialFragment) fragment).updateDirectionDial(lastDegree, newDegree);
                }
                SystemClock.sleep(1000);
            }
        }
    }
}
