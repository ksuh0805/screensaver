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

/**
 * 화면보호기 메인
 */
public class MyDreamService extends DreamService {

    /**
     * 현재 위치 추적
     */
    public GpsTracker gpsTracker;
    /**
     * 위도
     */
    private double latitude;
    /**
     * 경도
     */
    private double longitude;

    /**
     * 현재 온도, 시간, 배터리
     */
    private TextView temperature, txt_time, txt_battery;
    /**
     * 충전 상태 이미지
     */
    private ImageView img_charging_type;
    /**
     * 주기적 변경 핸들러
     */
    private Handler handler;
    /**
     * 배터리 상태
     */
    private IntentFilter intentFilter;
    /**
     * 시간 포맷 변환
     */
    private SimpleDateFormat timeFormat = new SimpleDateFormat("a hh:mm:ss", Locale.getDefault()),
            dateFormat = new SimpleDateFormat("YYYY/MM/dd (E)", Locale.getDefault());

    /**
     * 날씨 애니메이션 이미지
     */
    private LottieAnimationView animationView;

    /**
     * onAttachedToWindow() 해체
     */
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /**
     * 초기 설정
     */
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        /**
         * 상태바 숨기기 - On
         */
        setFullscreen(true);
        /**
         * 터치 후 종료 - On
         */
        setInteractive(false);
        /**
         * 밝은 화면 설정 - Off
         */
        setScreenBright(true);
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
        /**
         * 날씨 업데이트
         */
        updateWeatherData(latitude, longitude);

        /**
         * 날씨 이미지 좌우 이동 애니메이션 추가
         */
        Animation translate_anim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.translate);
        animationView.startAnimation(translate_anim);
        /**
         * 충전 이미지 회전 애니메이션 추가
         */
        Animation rotate_anim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate);
        img_charging_type.startAnimation(rotate_anim);

        Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 화면보호기 켜진 후 핸들러 작동
     */
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

    /**
     * 화면보호기 내 시간, 배터리, 날씨 핸들러
     */
    @SuppressLint("HandlerLeak")
    private void Handler() {
        handler = new Handler() {
            public void handleMessage(Message msg) {
                Intent batteryStatus = registerReceiver(null, intentFilter);
                switch (msg.getData().getString("title", "")) {
                    /**
                     * 시간 업데이트
                     */
                    case "updateTime":
                        SpannableStringBuilder time = new SpannableStringBuilder(timeFormat.format(new Date())+
                                "\n"+dateFormat.format(new Date()));
                        time.setSpan(new AbsoluteSizeSpan(35, true), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        time.setSpan(new AbsoluteSizeSpan(23, true), 11, time.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        time.setSpan(new StyleSpan(Typeface.BOLD), 11, time.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        time.setSpan(new ForegroundColorSpan(Color.GRAY), 11, time.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        txt_time.setText(time);
                        break;
                    /**
                     * 배터리 잔량
                     */
                    case "updateBattery":
                        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                        txt_battery.setText(Math.round(level * 100 / scale) + "%");
                        break;
                    /**
                     * 시계, 날짜 랜덤 위치 지정
                     */
                    case "move":
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
                            }
                            while(yOffset>600 && yOffset<1100); //날씨 뷰와 겹치는 범위
                        }
                        Log.d("display", "x: "+size.x+ " y: "+size.y);

                        LinearLayout.LayoutParams params;
                        params = (LinearLayout.LayoutParams)txt_time.getLayoutParams();
                        params.leftMargin = xOffset;
                        params.topMargin = yOffset;
                        txt_time.setLayoutParams(params);
                        break;
                    /**
                     * 날씨 업데이트
                     */
                    case "updateWeather":
                        updateWeatherData(latitude, longitude);
                        break;
                }
            }
        };
    }

    /**
     * 날씨 업데이트
     * @param lat 위도
     * @param lon 경도
     */
    private void updateWeatherData(final double lat, final double lon) {
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

    /**
     * json에서 날씨 정보 추출
     * @param json 웹서버로부터 받아온 날씨정보 json
     */
    private void renderWeather(JSONObject json) {
        try {
            /**
             * Parse JSON object and array using json object
             */
            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");

            /**
             * Use setWeatherIcon method - pass 2 parameters id and icon
             */
            setWeatherIcon(details.getInt("id"), details.getString("icon"));
            temperature.setText(main.getString("temp")+" °C");

        } catch (Exception e) {
            Log.e("SimpleWeather", "One or more fields not found in the JSON data" + e);
        }
    }

    /**
     * 날씨 상태별 lottie image 변경
     * @param actualId 날씨정보 구분
     * @param openIcon 날씨정보 구분
     */
    private void setWeatherIcon(int actualId, String openIcon) {
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
