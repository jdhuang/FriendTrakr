package android.huangj.wearabletest;

import android.app.Activity;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MotionEventCompat;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {
    public final static String TAG = "wearable::MainActivity";
    public final static String TAG_TOUCH = "OnTouchEvent";

    public final static String VERSION = "Version 0.0.10\n";

    private SensorManager mSensorManager;
    private TextView mTextView;
    private TextView mVersion;
    private TextView mBearingText;
    private TextView mDistanceText;
    private TextView mHRText;
    private ImageView mDirectionDial;
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
                mTextView = (TextView) stub.findViewById(R.id.text);
                mBearingText = (TextView) stub.findViewById(R.id.bearing);
                mDistanceText = (TextView) stub.findViewById(R.id.distance);
                mHRText = (TextView) stub.findViewById(R.id.heart_rate);
                mDirectionDial = (ImageView) stub.findViewById(R.id.directionDial);

                mVersion.setText(VERSION);
                mBearingText.setText("Bearing(M) NA");
                mDistanceText.setText("Distance NA");
                mHRText.setText("HR NA");

                UpdateDirectionDial rotateDirectionDial= new UpdateDirectionDial();
                new Thread(rotateDirectionDial).start();
            }
        });

        // Initialize location data
        mCompassBearing = 0;
        mPOIBearing = 0;

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        // Get SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        // Get list of available sensors
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
                debugHeartRate();
            default:
                break;
        }

        updateCompassData();
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
                mTextView.setText("Bearing(C)="+ azimuthInDegrees);
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

            mTextView.setText(sb.toString());
        }
    }
    private void debugHeartRate()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Sensor.TYPE_HEART_RATE");
        sb.append("\nx=");
        sb.append(mHeartRate[0]);
        sb.append("\ny=");
        sb.append(mHeartRate[1]);
        sb.append("\nz=");
        sb.append(mHeartRate[2]);

        Log.i(TAG, sb.toString());
        mHRText.setText(sb.toString());
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
                mBearingText.setText("(M)Bearing=" + data);
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
                newDegree = mPOIBearing - mCompassBearing;
                if (mCompassBearing < -360)
                {
                    newDegree += 360;
                }
//                newDegree = lastDegree + 10;
//                if (newDegree >= 360) {
//                    newDegree = 0;
//                }
                //Log.i(TAG, "lastDegree=" + lastDegree + " ,newDegree=" + newDegree);
                final RotateAnimation rotate = new RotateAnimation((float) lastDegree, (float) newDegree,
                        RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                        RotateAnimation.RELATIVE_TO_SELF, 0.5f);
                rotate.setDuration(100);
                rotate.setFillEnabled(true);
                rotate.setFillAfter(true);

                lastDegree = newDegree;

                runOnUiThread(new Runnable() {
                    public void run() {
                        mDirectionDial.startAnimation(rotate);
                    }
                });

                SystemClock.sleep(500);
            }
        }
    }
}
