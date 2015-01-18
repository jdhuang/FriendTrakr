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
    private UpdateDirectionDial mUpdateDirectionDial;
    private TextView mTarget;
    private TextView mCompassText;
    private TextView mDistanceText;
    private TextView mHRText;
    private float[] mGravData;
    private float[] mMagData;
    private float[] mHeartRate;
    private float mCompassBearing;
    private float mTargetBearing;
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

                mTarget = (TextView) stub.findViewById(R.id.version);
                mCompassText = (TextView) stub.findViewById(R.id.compass);
                mDistanceText = (TextView) stub.findViewById(R.id.distance);
                mHRText = (TextView) stub.findViewById(R.id.heart_rate);

                mTarget.setText(Common.VERSION);
                mCompassText.setText("Compass NA");
                mDistanceText.setText("Distance NA");
                mHRText.setText("HR NA");

                // set direction dial fragment
                setDirectionDialFragment();

                // register sensor
                registerSensor();

                mUpdateDirectionDial = new UpdateDirectionDial();
                new Thread(mUpdateDirectionDial).start();
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
        mTargetBearing = 0;

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
                updateHeartRate();
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
        StringBuilder sb = new StringBuilder();

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
                sb.append(azimuthInDegrees);
                sb.append("\u00B0");
                mCompassBearing = azimuthInDegrees;
            }
        }
        else if (mGravData == null)
        {
            sb.append("ACCELEROMETER NA");
        }
        else if (mMagData == null)
        {
            sb.append("MAGNETIC FIELD NA");
        }

        if (mCompassText != null && sb.toString() != "")
        {
            mCompassText.setText(sb.toString());
        }
    }
    private void updateHeartRate()
    {
        if (mHRText != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(mHeartRate[0]);
            sb.append(" bpm");
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
                mTargetBearing = Float.valueOf(data);
            }
        }
    }

    private class UpdateDirectionDial implements Runnable
    {
        private boolean mIsEnabled;

        public void stop()
        {
            mIsEnabled = false;
        }

        @Override
        public void run()
        {
            float lastDegree = 0;
            float newDegree = 0;
            mIsEnabled = true;

            while(mIsEnabled)
            {
                // compute POI degree relative to Compass
                newDegree = mTargetBearing - mCompassBearing;
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
