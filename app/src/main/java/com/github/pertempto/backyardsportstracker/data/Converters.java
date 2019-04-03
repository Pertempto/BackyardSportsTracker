package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;

import java.util.Date;
import java.util.HashMap;

public class Converters {
//    @TypeConverter
//    public static ArrayList<Player> playersFromString(String value) {
//        ArrayList<Player> instance = new ArrayList<>();
//        return new Gson().fromJson(value, instance.getClass());
//    }
//
//    @TypeConverter
//    public static String playersToString(ArrayList<Player> players) {
//        return new Gson().toJson(players);
//    }

    @TypeConverter
    public static Date dateFromTimestamp(Long value) {
        return new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date.getTime();
    }

    @TypeConverter
    public static HashMap<Long, Double> gameRatingsFromString(String value) {
        HashMap<String, Double> instance = new HashMap<>();
        HashMap<String, Double> stringMap = new Gson().fromJson(value, instance.getClass());
        HashMap<Long, Double> outMap = new HashMap<>();
        // convert string keys to integer keys
        for (String key: stringMap.keySet()) {
            outMap.put(Long.parseLong(key), stringMap.get(key));
        }
        return outMap;
    }

    @TypeConverter
    public static String gameRatingsToString(HashMap<Long, Double> map) {
        return new Gson().toJson(map);
    }

    @TypeConverter
    public static HashMap<String, Double> playerRatingsFromString(String value) {
        HashMap<String, Double> instance = new HashMap<>();
        return new Gson().fromJson(value, instance.getClass());
    }

    @TypeConverter
    public static String playerRatingsToString(HashMap<String, Double> map) {
        return new Gson().toJson(map);
    }
}
