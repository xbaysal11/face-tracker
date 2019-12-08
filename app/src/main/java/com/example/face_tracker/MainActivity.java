package com.example.face_tracker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.face_tracker.event.LeftEyeClosedEvent;
import com.example.face_tracker.event.NeutralFaceEvent;
import com.example.face_tracker.event.RightEyeClosedEvent;
import com.example.face_tracker.tracker.FaceTracker;
import com.example.face_tracker.util.PlayServicesUtil;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERM = 69;
    private static final String TAG = "FaceTracker";
    private SwitchCompat mSwitch;
    private ImageView mLight;
    private FaceDetector mFaceDetector;
    private CameraSource mCameraSource;
    private final AtomicBoolean updating = new AtomicBoolean(false);
    private TextView mEmoticon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwitch = (SwitchCompat) findViewById(R.id.switchButton);
        mLight = findViewById(R.id.light);
//        mEmoticon = (TextView) findViewById(R.id.emoticon);

        // check that the play services are installed
        PlayServicesUtil.isPlayServicesAvailable(this, 69);

        // permission granted...?
        if (isCameraPermissionGranted()) {
            // ...create the camera resource
            createCameraResources();
        } else {
            // ...else request the camera permission
            requestCameraPermission();
        }
    }

//    Check camera permission
    private boolean isCameraPermissionGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

//    Request the camera permission
    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_PERM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CAMERA_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createCameraResources();
            return;
        }

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("EyeControl")
                .setMessage("No camera permission")
                .setPositiveButton("Ok", listener)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register the event bus
        EventBus.getDefault().register(this);

        // start the camera feed
        if (mCameraSource != null && isCameraPermissionGranted()) {
            try {
                //noinspection MissingPermission
                mCameraSource.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "onResume: Camera.start() error");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // unregister from the event bus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        // stop the camera source
        if (mCameraSource != null) {
            mCameraSource.stop();
        } else {
            Log.e(TAG, "onPause: Camera.stop() error");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // release them all...
        if (mFaceDetector != null) {
            mFaceDetector.release();
        } else {
            Log.e(TAG, "onDestroy: FaceDetector.release() error");
        }
        if (mCameraSource != null) {
            mCameraSource.release();
        } else {
            Log.e(TAG, "onDestroy: Camera.release() error");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLeftEyeClosed(LeftEyeClosedEvent e) {
//        setEmoji(R.string.emoji_winking);
//        if (mSwitch.isChecked() && catchUpdatingLock()) {
//            mSwitch.setChecked(false);
//            mLight.setBackgroundColor(ContextCompat.getColor(this, R.color.green));
            mLight.setImageResource(R.drawable.face_tracker_right);
            releaseUpdatingLock();
//        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRightEyeClosed(RightEyeClosedEvent e) {
//        setEmoji(R.string.emoji_winking);
//        if (!mSwitch.isChecked() && catchUpdatingLock()) {
//            mSwitch.setChecked(true);
//            mLight.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
            mLight.setImageResource(R.drawable.face_tracker_left);

            releaseUpdatingLock();
//        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNeutralFace(NeutralFaceEvent e) {
//        setEmoji(R.string.emoji_neutral);
        mLight.setImageResource(R.drawable.face_tracker);

    }

//    private void setEmoji(int resource) {
//        if (!mEmoticon.getText().equals(getString(resource)) && catchUpdatingLock()) {
//            mEmoticon.setText(resource);
//            releaseUpdatingLock();
//        }
//    }

    private boolean catchUpdatingLock() {
        // set updating and return previous value
        return !updating.getAndSet(true);
    }
    private void releaseUpdatingLock() {
        updating.set(false);
    }

    private void createCameraResources() {
        Context context = getApplicationContext();

        // create and setup the face detector
        mFaceDetector = new FaceDetector.Builder(context)
                .setProminentFaceOnly(true) // optimize for single, relatively large face
                .setTrackingEnabled(true) // enable face tracking
                .setClassificationType(/* eyes open and smile */ FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE) // for one face this is OK
                .build();

        // now that we've got a detector, create a processor pipeline to receive the detection
        // results
        mFaceDetector.setProcessor(new LargestFaceFocusingProcessor(mFaceDetector, new FaceTracker()));

        // operational...?
        if (!mFaceDetector.isOperational()) {
            Log.w(TAG, "createCameraResources: detector NOT operational");
        } else {
            Log.d(TAG, "createCameraResources: detector operational");
        }

        // Create camera source that will capture video frames
        // Use the front camera
        mCameraSource = new CameraSource.Builder(this, mFaceDetector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30f)
                .build();
    }
}
