package android.huangj.wearabletest;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;


/**
 * Created by noizytribe on 1/17/15.
 */
public class DataLayerListenerService extends WearableListenerService {
    public static final String TAG = "DataLayerListenerService";
    public final static String PATH_KEY = "PATH_KEY";
    public final static String PATH_DISTANCE = "friendtraker/distance";
    public final static String PATH_BEARING = "friendtraker/bearing";
    public final static String DATA_KEY = "DATA_KEY";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String messagePath = messageEvent.getPath();
        final String data = new String(messageEvent.getData());

        Log.i("myTag", "Message path received on watch is: " + messagePath);
        Log.i("myTag", "Message received on watch is: " + data);

        // Distance data
        if (messagePath.equals(PATH_DISTANCE))
        {
            // Broadcast data to wearable MainActivity
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra(PATH_KEY, PATH_DISTANCE);
            messageIntent.putExtra(DATA_KEY, data);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        // Bearing data
        else if (messagePath.equals(PATH_BEARING))
        {
            // Broadcast data to wearable MainActivity
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra(PATH_KEY, PATH_BEARING);
            messageIntent.putExtra(DATA_KEY, data);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }
}
