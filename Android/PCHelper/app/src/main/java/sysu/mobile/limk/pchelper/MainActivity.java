package sysu.mobile.limk.pchelper;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static final int MESSAGE_COMMAND = 0xffffffff;
    public static final int MESSAGE_START_RECORD = 0xfffffffe;
    public static final int MESSAGE_STOP_RECORD = 0xfffffffd;
    public static final int MESSAGE_SAVE_DONE = 0xfffffffc;
    public static final DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    static private class MainHandler extends Handler {
        // WeakReference to the outer class's instance.
        private WeakReference<MainActivity> mOuter;

        MainHandler(MainActivity activity) {
            mOuter = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity activity = mOuter.get();
            if (activity != null) {
                Bundle data = msg.getData();
                switch (msg.what) {
                    case MESSAGE_COMMAND:
                        int command = data.getInt("command");

                        if (command == 1) {
                            Toast.makeText(activity, "Begin recording video", Toast.LENGTH_SHORT).show();
                            activity.startRecord();
                        } else if (command == 2) {
                            Toast.makeText(activity, "Stop recording video", Toast.LENGTH_SHORT).show();
                            activity.stopRecord();

                            HiThread thread = new HiThread() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(500);
                                        if (activity.mSocket != null && activity.mSocket.isConnected()) {
                                            activity.mOutputStream.writeInt(0);
                                        }
                                    } catch (IOException | InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            thread.start();

                        }
                        break;
                    case MESSAGE_START_RECORD:
                        String filename = activity.mVideoFile.getAbsolutePath();

                        HiThread thread = new HiThread() {
                            @Override
                            public void run() {
                                try {
                                    String filename = (String) getParams().get(0);
                                    if (activity.mSocket != null && activity.mSocket.isConnected()) {
                                        activity.mOutputStream.writeUTF(filename);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        ArrayList<Object> params = new ArrayList<>();
                        params.add(filename);
                        thread.start(params);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private MainHandler mHandler;
    private ServerSocket mServerSocket;
    private DataInputStream mInputStream;
    private DataOutputStream mOutputStream;
    private Socket mSocket;

    private HiThread mServerThread = new HiThread() {
        boolean flag = true;

        @Override
        public void stop() {
            flag = false;
            try {
                if (mServerSocket != null) {
                    mServerSocket.close();
                }
                if (mSocket != null) {
                    mSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.stop();
        }

        @Override
        public void run() {

            try {
                mServerSocket = new ServerSocket(9888);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (flag) {

                try {
                    mSocket = mServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    mInputStream = new DataInputStream(mSocket.getInputStream());
                    mOutputStream = new DataOutputStream(mSocket.getOutputStream());

                    while (flag) {
                        int command = mInputStream.readInt();

                        Message msg = new Message();
                        msg.what = MESSAGE_COMMAND;
                        Bundle bundle = new Bundle();
                        bundle.putInt("command", command);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    };

    private File mVideoFile;
    private MediaRecorder mRecorder;
    private SurfaceView mCameraSurfaceView;
    private TextView mStatusTextView;
    private boolean isRecording = false;

    private Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MainHandler(this);
        mServerThread.start();

        mCameraSurfaceView = (SurfaceView) findViewById(R.id.sv_camera);
        mCameraSurfaceView.getHolder().setFixedSize(1280, 720);
        mCameraSurfaceView.getHolder().setKeepScreenOn(true);
        mCamera = Tools.getCameraInstance();

        mCameraSurfaceView.setClickable(true);
        mCameraSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        // TODO: 2017/4/14
                    }
                });
            }
        });

        mRecorder = new MediaRecorder();

        mCameraSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    mCamera.setDisplayOrientation(90);
                    mCamera.setPreviewDisplay(holder);
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d("surfaceChanged", "format: " + format);
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        // TODO: 2017/4/14
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        mStatusTextView = (TextView) findViewById(R.id.tv_status);
    }

    private void prepare() {
        try {
            String date = format.format(new Date());
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    + "/MyVideos/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            mVideoFile = new File(dir.getAbsoluteFile() + "/" + date + ".mp4");

            mRecorder.reset();

            mCamera.lock();
            mCamera.unlock();

            mRecorder.setCamera(mCamera);
            mRecorder.setAudioSource(MediaRecorder
                    .AudioSource.MIC);
            mRecorder.setVideoSource(MediaRecorder
                    .VideoSource.CAMERA);

            mRecorder.setProfile(CamcorderProfile
                    .get(CamcorderProfile.QUALITY_HIGH));

            mRecorder.setOutputFile(mVideoFile.getAbsolutePath());
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecord() {
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                prepare();
                mRecorder.start();

                mStatusTextView.setVisibility(View.VISIBLE);

                isRecording = true;
                mHandler.sendEmptyMessage(MESSAGE_START_RECORD);
            }
        });
    }

    private void stopRecord() {
        if (isRecording) {
            mStatusTextView.setVisibility(View.GONE);
            mRecorder.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
