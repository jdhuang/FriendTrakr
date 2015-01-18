package android.huangj.wearabletest;

import android.app.Fragment;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

/**
 * Created by noizytribe on 1/17/15.
 */
public class DirectionDialFragment extends android.support.v4.app.Fragment
{
    private ImageView mDirectionDial;

    @Override
    public View onCreateView(LayoutInflater inflater,
        @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_direction_dial,
            container, false);
        mDirectionDial = (ImageView) view.findViewById(R.id.directionDial);
        return view;
    }

    public void updateDirectionDial(float lastDegree, float newDegree)
    {
        final RotateAnimation rotate = new RotateAnimation(lastDegree, newDegree,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(100);
        rotate.setFillEnabled(true);
        rotate.setFillAfter(true);

        lastDegree = newDegree;

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mDirectionDial.startAnimation(rotate);
            }
        });

        SystemClock.sleep(800);
    }
}
