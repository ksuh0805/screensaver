package com.hsun.screensaver;

import android.annotation.SuppressLint;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.service.dreams.DreamService;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Display;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.ACTION_BATTERY_CHANGED;

public class MyDreamService extends DreamService {

    public GpsTracker gpsTracker;
    private double latitude;
    private double longitude;

    private TextView temperature;
    private TextView txt_time, txt_battery;
    private ImageView img_charging_type;
    private Handler handler;
    private IntentFilter intentFilter;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("a hh:mm:ss", Locale.getDefault()),
            dateFormat = new SimpleDateFormat("YYYY/MM/dd (E)", Locale.getDefault());

    private LottieAnimationView animationView;

    @Override //onAttachedToWindow() 해체
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override // 초기 설정
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setFullscreen(true); // 상태바 숨기기
        setInteractive(false); // 터치 후 종료
        setScreenBright(true); // 밝은 화면 설정
        setContentView(R.layout.dream_page);

        intentFilter = new IntentFilter(ACTION_BATTERY_CHANGED);

        txt_time = findViewById(R.id.txt_time);
        txt_battery = findViewById(R.id.txt_battery);
        img_charging_type = findViewById(R.id.img_charging_type);
        temperature = findViewById(R.id.temperature);

        gpsTracker = new GpsTracker(this);

        animationView = (LottieAnimationView) findViewById(R.id.lottie);

        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();

        updateWeatherData(latitude, longitude);

        Animation translate_anim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.translate);
        animationView.startAnimation(translate_anim);
        Animation rotate_anim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate);
        img_charging_type.startAnimation(rotate_anim);

        Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                HandleMessage.set(handler, "updateTime");
                HandleMessage.set(handler, "updateBattery");
            }
        }, 0, 1000);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                HandleMessage.set(handler, "updateWeather");
            }
        }, 0, 600000); // 10분마다
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                HandleMessage.set(handler, "move");
            }
        }, 0, 60000); // 1분마다
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
    }

    @SuppressLint("HandlerLeak")
    private void Handler() {
        handler = new Handler() {
            public void handleMessage(Message msg) {
                Intent batteryStatus = registerReceiver(null, intentFilter);
                switch (msg.getData().getString("title", "")) {
                    case "updateTime":
                        SpannableStringBuilder time = new SpannableStringBuilder(timeFormat.format(new Date())+
                                "\n"+dateFormat.format(new Date()));
                        time.setSpan(new AbsoluteSizeSpan(35, true), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        time.setSpan(new AbsoluteSizeSpan(23, true), 11, time.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        time.setSpan(new StyleSpan(Typeface.BOLD), 11, time.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        time.setSpan(new ForegroundColorSpan(Color.GRAY), 11, time.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        txt_time.setText(time);
                        break;
                    case "updateBattery":
                        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                        txt_battery.setText(Math.round(level * 100 / scale) + "%");
                        break;
                    case "move": // 시계, 날짜 랜덤 위치 지정
                        Random ranMove = new Random();
                        Display display = getWindowManager().getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        int xOffset;
                        int yOffset;
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                            do{
                                xOffset = ranMove.nextInt(size.x - 820)+20;
                                yOffset = ranMove.nextInt(size.y - 220)+20;
                                Log.d("OFFSET", "x: "+xOffset+" y: "+yOffset);
                            }while(xOffset>160 && xOffset<1020 && yOffset>600); //배터리 뷰와 겹치는 범위
                        }else{
                            do{
                                xOffset = ranMove.nextInt(size.x - 750) + 30;
                                yOffset = ranMove.nextInt(size.y - 450) + 30;
                                Log.d("OFFSET", "x: " + xOffset + " y: " + yOffset);
                            }while(yOffset>600 && yOffset<1100); //날씨 뷰와 겹치는 범위
                        }
                        Log.d("display", "x: "+size.x+ " y: "+size.y);

                        LinearLayout.LayoutParams params;
                        params = (LinearLayout.LayoutParams)txt_time.getLayoutParams();
                        params.leftMargin = xOffset;
                        params.topMargin = yOffset;
                        txt_time.setLayoutParams(params);
                        break;
                    case "updateWeather": // 날씨 업데이트
                        updateWeatherData(latitude, longitude);
                        break;
                }
            }
        };
    }
    private void updateWeatherData(final double lat, final double lon) { // update weather
        new Thread() {
            public void run() {
                final JSONObject json = RemoteFetch.getJSON(MyDreamService.this, lat, lon);
                    if (json == null) {
                        handler.post(() -> {
                                Toast.makeText(MyDreamService.this,
                                        R.string.place_not_found,
                                        Toast.LENGTH_LONG).show();
                        });
                } else {
                    handler.post((() -> renderWeather(json)));
                }
            }
        }.start();
    }

    private void renderWeather(JSONObject json) { // json에서 날씨 정보 추출
        try {
            // Parse JSON object and array using json object
            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");

            // Use setWeatherIcon method - pass 2 parameters id and icon
            setWeatherIcon(details.getInt("id"), details.getString("icon"));
            temperature.setText(main.getString("temp")+" °C");

        } catch (Exception e) {
            Log.e("SimpleWeather", "One or more fields not found in the JSON data" + e);
        }
    }
    private void setWeatherIcon(int actualId, String openIcon) { // 날씨 상태별 lottie image 변경
        int id = actualId / 100;
        if (actualId == 800) {
            if (openIcon.equals("01d")) {
                animationView.setAnimation("sunny2.json");
            } else {
                animationView.setAnimation("clear_night.json");
            }
        } else {
            switch (id) {
                case 2:
                    animationView.setAnimation("thunder.json");
                    break;
                case 3:
                    animationView.setAnimation("rain.json");
                    break;
                case 7:
                    animationView.setAnimation("haze.json");
                    break;
                case 8:
                    animationView.setAnimation("cloudy.json");
                    break;
                case 6:
                    animationView.setAnimation("snow.json");
                    break;
                case 5:
                    if(actualId == 502 || actualId == 503 || actualId == 504){
                        animationView.setAnimation("storm.json");}
                    else{
                        animationView.setAnimation("rain.json");
                    }
                    break;
            }
        }
        animationView.playAnimation();
    }
}
