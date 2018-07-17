package com.shiweinan.loginputdata;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    final int requestCode_STORAGE = 0;
    final int requestCode_CAMERA = 1;
    final int requestCode_RECORD = 2;
    List<String> english_phrases = new ArrayList<>();
    List<String> chinese_phrases = new ArrayList<>();
    TextView taskText;
    int currentTaskNo = 0;
    EditText editText;
    Button startButton;
    SensorManager manager;
    Sensor acce;
    Sensor gyro;
    Sensor mag;
    float[] acceValues = new float[3];
    float[] magValues = new float[3];
    float[] gyroValues = new float[3];
    float[] RValues = new float[9];
    float[] oriValues = new float[3];
    String currentTask = "";
    Button nextButton;

    private Timer timer;
    private TimerTask task;
    private MediaRecorder mediaRecorder;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private boolean screenRecording = false;
    private int width, height, dpi;


    long bootTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtimeNanos() / 1000000;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        requestPermission();
        loadPhrases();

        //for recording video
        mSurfaceView = (SurfaceView)findViewById(R.id.cameraView);
        //mSurfaceView.setVisibility(View.INVISIBLE);
        //RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams)mSurfaceView.getLayoutParams();
        //param.height = 1;
        //mSurfaceView.setLayoutParams(param);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        nextButton = (Button)findViewById(R.id.buttonNext);


        //final TextView text = findViewById(R.id.textView);
        taskText = findViewById(R.id.taskText);
        //text.setText(bootTime() + " ms" + "," + SystemClock.uptimeMillis());


        editText = findViewById(R.id.editText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String input = charSequence.toString();
                LogUtil.log("Input," + input);
                if (input.equals(currentTask)) {
                    nextButton.setEnabled(true);
                } else {
                    nextButton.setEnabled(false);
                }
                //text.setText(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //text.setText(editable.toString());
            }
        });
        updateUI();
        startButton = findViewById(R.id.button);
        acce = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mag = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                switch (sensorEvent.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        acceValues = sensorEvent.values;
                        LogUtil.logSensor("ACCE," + acceValues[0] + "," + acceValues[1] + "," + acceValues[2]);
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        gyroValues = sensorEvent.values;
                        LogUtil.logSensor("GYRO," + gyroValues[0] + "," + gyroValues[1] + "," + gyroValues[2]);
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        magValues = sensorEvent.values;
                        LogUtil.logSensor("MAG," + magValues[0] + "," + magValues[1] + "," + magValues[2]);
                        break;
                    default:
                        break;
                }
                SensorManager.getRotationMatrix(RValues, null, acceValues, magValues);
                SensorManager.getOrientation(RValues, oriValues);
                LogUtil.logSensor("ORI," + oriValues[0] + "," + oriValues[1] + "," + oriValues[2]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        manager.registerListener(listener, acce, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(listener, mag, SensorManager.SENSOR_DELAY_GAME);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;
        dpi = displayMetrics.densityDpi;
        projectionManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, 0);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        }
    }

    private void createVirtualDisaplay() {
        VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                "MainScreen",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null, null);
    }

    private void initRecorder() {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            //mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setVideoSize(width, height);
            mediaRecorder.setOutputFile(LogUtil.screenRecordPath);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
            mediaRecorder.setVideoFrameRate(30);
        }
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startRecordScreen() {
        if (mediaProjection == null || screenRecording) {
            return;
        }

        initRecorder();
        createVirtualDisaplay();
        mediaRecorder.start();
        screenRecording = true;
    }

    public void stopRecordScreen() {
        if (!screenRecording) {
            return;
        }
        if (mediaRecorder != null) {
            mediaRecorder.stop();
        }
        screenRecording = false;
        mediaRecorder = null;
    }


    private void loadPhrases() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.msg)));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                chinese_phrases.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.e("load chinese phrases failed!");
        }
        Collections.shuffle(chinese_phrases);

//        reader = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.english_t_40)));
//        try {
//            while ((line = reader.readLine()) != null) {
//                english_phrases.add(line);
//            }
//            reader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            LogUtil.e("load english phrases failed!");
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.posture:
//                final String[] postures = new String[] {"TwoThumbs", "LeftThumb", "RightThumb"};
//                AlertDialog ad = new AlertDialog.Builder(MainActivity.this).setTitle("单选")
//                    .setSingleChoiceItems(postures, Config.posture.ordinal(), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            Config.posture = Config.Posture.valueOf(postures[i]);
//                            dialogInterface.dismiss();
//                            updateUI();
//                        }
//                    }).setCancelable(false).create();
//                ad.show();
//                break;
//            case R.id.language:
//                final String[] languages = new String[] {"English", "Chinese"};
//                AlertDialog adl = new AlertDialog.Builder(MainActivity.this).setTitle("单选")
//                    .setSingleChoiceItems(languages, Config.language.ordinal(), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            Config.language = Config.Language.valueOf(languages[i]);
//                            dialogInterface.dismiss();
//                            updateUI();
//                        }
//                    }).setCancelable(false).create();
//                adl.show();
//                break;
//            case R.id.woz:
//                if (Config.woz) {
//                    Config.woz = false;
//                    item.setTitle("Wizard Of Oz: Off");
//                } else {
//                    Config.woz = true;
//                    item.setTitle("Wizard Of Oz: On");
//                }
//                update();
            case R.id.mode:
                final String[] modes = new String[] {"Normal", "Fast", "Random"};
                AlertDialog adm = new AlertDialog.Builder(MainActivity.this).setTitle("单选")
                        .setSingleChoiceItems(modes, Config.mode.ordinal(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Config.mode = Config.Mode.valueOf(modes[i]);
                                dialogInterface.dismiss();
                                updateUI();
                            }
                        }).setCancelable(false).create();
                adm.show();
                break;
            case R.id.camera:
                RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams)mSurfaceView.getLayoutParams();

                param.height = 375 - param.height;
                param.width = 211 - param.width;
                mSurfaceView.setLayoutParams(param);
                break;
            default:
                break;
        }
        //Collections.shuffle(english_phrases);
        Collections.shuffle(chinese_phrases);
        updateUI();
        if (Config.mode == Config.Mode.Normal) {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }
        return super.onOptionsItemSelected(item);
    }

    private void update() {
        if (Config.woz) {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
        }

    }
    private String generateRandomTask() {
        Random rand = new Random();
        String res = "";
        String symbols = "!@#$%^&*()-=_+<>;'\",.?/~";
        //generate a random string len: 6 - 10
        int len = 6 + rand.nextInt(5);
        for (int i=0; i<len; i++) {
            int mode = rand.nextInt(4);//0: a-z, 1: A-Z, 2: !@#$%^&*()-=_+<>;'",.?/~ 3:0-9
            if (mode == 0) {
                res += (char)('a' + rand.nextInt(26));
            } else if (mode == 1) {
                res += (char)('A' + rand.nextInt(26));
            } else if (mode == 2) {
                res += symbols.charAt(rand.nextInt(symbols.length()));
            } else {
                assert (mode == 3);
                res += rand.nextInt(10);
            }
        }
        return res;
    }

    private void updateUI() {
        setTitle("Mode:" + Config.mode + "(" +  (currentTaskNo+1) + "/" + Config.totalTaskNo + ")");
    }

    public void startLog(View v) {
        if (!Config.isStarted) {
            LogUtil.init();
            Config.isStarted = true;
            startButton.setText("FINISH");
            currentTaskNo = -1;
            nextTask(v);
            //android.support.v7.app.ActionBar bar = this.getSupportActionBar();
            //bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ff0000")));
            //note startLog after LogUtil.init()
            startRecord();
            startRecordScreen();
        } else {
           stopTask();
        }


    }
    public void stopTask() {
        if (Config.isStarted) {
            LogUtil.finish();
            Config.isStarted = false;
            startButton.setText("START");
            currentTaskNo = -1;
            nextTask();
            //android.support.v7.app.ActionBar bar = getSupportActionBar();
            //bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0000ff")));
            stopRecord();
            stopRecordScreen();


        }
    }
    public void resetInput() {
        editText.setText("");
        editText.setEnabled(false);
        LogUtil.log("Reset");
    }
    public void hideTask(View v) {
        taskText.setVisibility(taskText.getVisibility() == View.VISIBLE?View.INVISIBLE:View.VISIBLE);
        LogUtil.log("Hide," + ((taskText.getVisibility() == View.VISIBLE)?"Visible":"Invisible"));
        if (taskText.getVisibility() == View.VISIBLE) {
            resetInput();
        } else {
            editText.setEnabled(true);
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
        }
    }
    public void nextTask() {
        if (currentTaskNo + 1 == Config.totalTaskNo) {
            stopTask();
        }
        currentTaskNo = (currentTaskNo + 1) % Config.totalTaskNo;
        if (Config.mode != Config.Mode.Random) {
            currentTask = chinese_phrases.get(currentTaskNo).trim();
        } else {
            currentTask = generateRandomTask();
        }
        taskText.setText(currentTask);
        LogUtil.log("Task," + currentTask);
        editText.setText("");
        editText.setEnabled(false);
        taskText.setVisibility(View.VISIBLE);
        updateUI();
    }
    public void nextTask(View v) {
//        if (Config.language == Config.Language.English) {
//            currentTaskNo = (currentTaskNo + 1) % english_phrases.size();
//            taskText.setText(english_phrases.get(currentTaskNo));
//            LogUtil.log("Task," + english_phrases.get(currentTaskNo));
//        } else {
//            currentTaskNo = (currentTaskNo + 1) % chinese_phrases.size();
//            taskText.setText(chinese_phrases.get(currentTaskNo));
//            LogUtil.log("Task," + chinese_phrases.get(currentTaskNo));
//        }
        nextTask();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case requestCode_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogUtil.i("Permission granted!");
                } else {
                    LogUtil.e("Storage permission rejected!");
                }
                break;
            case requestCode_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogUtil.i("Permission granted!");
                } else {
                    LogUtil.e("Camera permission rejected!");
                }
                break;
            case requestCode_RECORD:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogUtil.i("Permission granted!");
                } else {
                    LogUtil.e("Record permission rejected!");
                }
                break;
            default:
                break;
        }
        requestPermission();
    }
    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
             != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, requestCode_STORAGE);
        }
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, requestCode_CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, requestCode_RECORD);
        }*/
    }
    @Override
    protected void onResume() {
        super.onResume();

    }
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MediaRecorder mMediaRecorder;
    private Camera mCamera;

    private void initCamera() {
        //missing step: check if there exists front camera
        if (mCamera == null) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            mCamera.setDisplayOrientation(90);
        }
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
            } catch (IOException e) {
                LogUtil.e("init camera error!");
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }

    private void startRecord() {
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOrientationHint(270); // 视频旋转90度，因为前置摄像头本身就旋转了180度？
            CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
//          camcorderProfile.videoFrameWidth = VIDEO_HEIGHT; // 摄像头不支持的录像尺寸会导致录像花屏
//          camcorderProfile.videoFrameHeight = VIDEO_WIDTH;
            //camcorderProfile.videoCodec = MediaRecorder.VideoEncoder.DEFAULT;
            //camcorderProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
            //camcorderProfile.videoFrameRate = 25;
            mMediaRecorder.setProfile(camcorderProfile);
            mMediaRecorder.setOutputFile(LogUtil.recordPath);
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        }
        mCamera.unlock();
        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "已开始录制", Toast.LENGTH_SHORT).show();
    }

    private void stopRecord() {
        if (mMediaRecorder != null) {
           // mMediaRecorder.release();
            mMediaRecorder.stop();
        }
        if (mCamera != null) {
            mCamera.lock();
        }
        mMediaRecorder = null;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        initCamera();
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceHolder = holder;
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

    }
}
