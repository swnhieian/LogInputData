package com.shiweinan.loginputdata;


import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Visibility;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    final int requestCode_STORAGE = 0;
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

    private Timer timer;
    private TimerTask task;


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

        final TextView text = findViewById(R.id.textView);
        taskText = findViewById(R.id.taskText);
        text.setText(bootTime() + " ms" + "," + SystemClock.uptimeMillis());


        final int WHAT = 102;
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case WHAT:
                        text.setText(bootTime() + " ms" + "," + SystemClock.uptimeMillis());
                        break;
                }
            }
        };

        task = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = WHAT;
                message.obj = System.currentTimeMillis();
                handler.sendMessage(message);
            }
        };

        //timer = new Timer();
        // 参数：
        // 1000，延时1秒后执行。
        // 2000，每隔2秒执行1次task。
        //timer.schedule(task, 0, 1);


        editText = findViewById(R.id.editText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                LogUtil.log("Input," + charSequence);
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

    }

    private void loadPhrases() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.chinese_phrases)));
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

        reader = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.english_t_40)));
        try {
            while ((line = reader.readLine()) != null) {
                english_phrases.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.e("load english phrases failed!");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.posture:
                final String[] postures = new String[] {"TwoThumbs", "LeftThumb", "RightThumb"};
                AlertDialog ad = new AlertDialog.Builder(MainActivity.this).setTitle("单选")
                    .setSingleChoiceItems(postures, Config.posture.ordinal(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Config.posture = Config.Posture.valueOf(postures[i]);
                            dialogInterface.dismiss();
                            updateUI();
                        }
                    }).setCancelable(false).create();
                ad.show();
                break;
            case R.id.language:
                final String[] languages = new String[] {"English", "Chinese"};
                AlertDialog adl = new AlertDialog.Builder(MainActivity.this).setTitle("单选")
                    .setSingleChoiceItems(languages, Config.language.ordinal(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Config.language = Config.Language.valueOf(languages[i]);
                            dialogInterface.dismiss();
                            updateUI();
                        }
                    }).setCancelable(false).create();
                adl.show();
                break;
            default:
                break;
        }
        Collections.shuffle(english_phrases);
        Collections.shuffle(chinese_phrases);
        updateUI();
        return super.onOptionsItemSelected(item);
    }

    private void updateUI() {
        setTitle(Config.posture.toString() + " " + Config.language.toString() + ":" +  currentTaskNo);
    }

    public void startLog(View v) {
        if (!Config.isStarted) {
            LogUtil.init();
            Config.isStarted = true;
            startButton.setText("FINISH");
            currentTaskNo = -1;
            nextTask(v);
            android.support.v7.app.ActionBar bar = this.getSupportActionBar();
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ff0000")));
        } else {
            LogUtil.finish();
            Config.isStarted = false;
            startButton.setText("START");
            currentTaskNo = -1;
            nextTask(v);
            android.support.v7.app.ActionBar bar = getSupportActionBar();
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0000ff")));
        }


    }
    public void hideTask(View v) {
        taskText.setVisibility(taskText.getVisibility() == View.VISIBLE?View.INVISIBLE:View.VISIBLE);
        LogUtil.log("Hide," + ((taskText.getVisibility() == View.VISIBLE)?"Visible":"Invisible"));
    }
    public void nextTask(View v) {
        if (Config.language == Config.Language.English) {
            currentTaskNo = (currentTaskNo + 1) % english_phrases.size();
            taskText.setText(english_phrases.get(currentTaskNo));
            LogUtil.log("Task," + english_phrases.get(currentTaskNo));
        } else {
            currentTaskNo = (currentTaskNo + 1) % chinese_phrases.size();
            taskText.setText(chinese_phrases.get(currentTaskNo));
            LogUtil.log("Task," + chinese_phrases.get(currentTaskNo));
        }
        editText.setText("");
        taskText.setVisibility(View.VISIBLE);
        updateUI();
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
            default:
                break;
        }
    }
    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
             != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode_STORAGE);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

    }
}
