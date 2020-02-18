package com.hsun.screensaver;
// Leave your package name on line 1

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 날씨 정보 가져오기
 */

public class RemoteFetch {
    private static final String OPEN_WEATHER_MAP_API =
    "http://api.openweathermap.org/data/2.5/weather?&lat=%f&lon=%f&units=metric"; // openweathermap으로부터 가져오기

    public static JSONObject getJSON(Context context, double lat, double lon){

        try {
            URL url = new URL(String.format(OPEN_WEATHER_MAP_API, lat, lon));
            Log.d("json", String.valueOf(url)); // URL, 위도, 경도 확인

            HttpURLConnection connection =
                    (HttpURLConnection)url.openConnection();

            connection.addRequestProperty("x-api-key",
                    context.getString(R.string.APIkey));

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            StringBuffer json = new StringBuffer(1024);
            String tmp="";
            while((tmp=reader.readLine())!=null)
                json.append(tmp).append("\n");
            reader.close();

            JSONObject data = new JSONObject(json.toString());

            // This value will be 404 if the request was not
            // successful
            if(data.getInt("cod") != 200){
                return null;
            }
            Log.d("data", String.valueOf(data));

            return data;
        }catch(Exception e){
            return null;
        }
    }
}
