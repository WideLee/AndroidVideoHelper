package sysu.mobile.limk.pchelper;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.tbruyelle.rxpermissions.RxPermissions;

import java.lang.ref.WeakReference;

import rx.functions.Action1;

public class CheckPermissionActivity extends AppCompatActivity {

    private static class MainHandler extends Handler {
        // WeakReference to the outer class's instance.
        private WeakReference<CheckPermissionActivity> mOuter;

        MainHandler(CheckPermissionActivity activity) {
            mOuter = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CheckPermissionActivity activity = mOuter.get();
            if (activity != null) {
                // TODO: 2017/4/14
            }
        }
    }

    private MainHandler mHandler;

    private void checkPermission(String... permissions) {
        RxPermissions mRxPermission = new RxPermissions(this);

        mRxPermission.request(permissions).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                if (aBoolean) {
                    Toast.makeText(CheckPermissionActivity.this, "Granted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CheckPermissionActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CheckPermissionActivity.this, "This app will exit in 3 secs.", Toast.LENGTH_SHORT).show();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 3000);
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_permission);

        mHandler = new MainHandler(this);

        checkPermission(Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA);
    }
}
