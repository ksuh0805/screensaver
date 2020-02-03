package com.hsun.screensaver;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MyDreamService extends DreamService {

    public GpsTracker gpsTracker;

    private TextView location;
    private TextView weather;
    private TextView txt_time, txt_date, txt_battery;
    private ImageView img_charging_type;
    private Handler handler;
    private IntentFilter intentFilter;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm:ss", Locale.getDefault()),
            dateFormat = new SimpleDateFormat("YYYY년 MM월 dd일 (E)", Locale.getDefault());

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

        intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        txt_time = findViewById(R.id.txt_time);
        txt_date = findViewById(R.id.txt_date);
        txt_battery = findViewById(R.id.txt_battery);
        img_charging_type = findViewById(R.id.img_charging_type);
        location = findViewById(R.id.location);
       //weather = findViewById(R.id.weather);

        gpsTracker = new GpsTracker(this);

        animationView = (LottieAnimationView) findViewById(R.id.lottie);

        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();

        updateWeatherData(latitude, longitude);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            Animation anim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.translate);
            animationView.startAnimation(anim);
        }

        // 시계 랜덤 위치 변경 -> 일정시간마다 변경, 변경 범위 수정 예정
        Random Ran_move = new Random();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int xOffset = Ran_move.nextInt(size.x)/3;
        int yOffset = Ran_move.nextInt(size.y)/2;
        Log.d("display", "x: "+size.x+ " y: "+size.y);
        Log.d("OFFSET", "x: "+xOffset+" y: "+yOffset);

        LinearLayout.LayoutParams params = null;
        LinearLayout.LayoutParams params2 = null;
        params = (LinearLayout.LayoutParams)txt_time.getLayoutParams();
        params2 = (LinearLayout.LayoutParams)txt_date.getLayoutParams();
        params.leftMargin = xOffset;
        params.topMargin = yOffset;
        params2.leftMargin = xOffset;
        params2.topMargin = xOffset;

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
                        txt_time.setText(timeFormat.format(new Date()));
                        txt_date.setText(dateFormat.format(new Date()));
                        break;
                    case "updateBattery":
                        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                        txt_battery.setText(Math.round(level * 100 / scale) + "%");
                        break;
                    case "updateChargeType":
                        switch (batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                            case BatteryManager.BATTERY_PLUGGED_AC:
                                img_charging_type.setImageResource(R.drawable.charging_line);
                                break;
                            case BatteryManager.BATTERY_PLUGGED_USB:
                                img_charging_type.setImageResource(R.drawable.charging_usb);
                                break;
                            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                                img_charging_type.setImageResource(R.drawable.charging_wireless);
                                break;
                        }
                        break;
                }
            }
        };
    }
    private void updateWeatherData(final double lat, final double lon) { // update weather
        new Thread() {
            public void run() {
                final JSONObject json = RemoteFetch.getJSON(this, lat, lon);
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
            location.setText(main.getString("temp")+" °C");

        } catch (Exception e) {
            Log.e("SimpleWeather", "One or more fields not found in the JSON data" + e);
        }
    }
    private void setWeatherIcon(int actualId, String openIcon) { // 날씨 상태별 lottie image 변경
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            if (openIcon.equals("01d")) {
                //icon = this.getString(R.string.weather_sunny);
                animationView.setAnimation("sunny2.json");
            } else {
                //icon = this.getString(R.string.weather_clear_night);
                animationView.setAnimation("clear_night.json");
            }
        } else {
            switch (id) {
                case 2:
                    //icon = this.getString(R.string.weather_thunder);
                    animationView.setAnimation("thunder.json");
                    break;
                case 3:
                    //icon = this.getString(R.string.weather_drizzle);
                    animationView.setAnimation("drizzle.json");
                    break;
                case 7:
                    //icon = this.getString(R.string.weather_foggy);
                    animationView.setAnimation("haze.json");
                    break;
                case 8:
                    //icon = this.getString(R.string.weather_cloudy);
                    animationView.setAnimation("cloudy.json");
                    break;
                case 6:
                    //icon = this.getString(R.string.weather_snowy);
                    animationView.setAnimation("snow.json");
                    break;
                case 5:
                    //icon = this.getString(R.string.weather_rainy);
                    animationView.setAnimation("storm.json");
                    break;
            }
        }
        //weather.setText(icon);
        animationView.playAnimation();
    }
}
