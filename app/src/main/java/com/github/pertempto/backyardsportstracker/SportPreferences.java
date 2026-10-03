package com.github.pertempto.backyardsportstracker;

import android.content.Context;
import android.content.SharedPreferences;

import com.github.pertempto.backyardsportstracker.data.Sports;

public final class SportPreferences {
    private static final String PREFERENCES_NAME = "app_preferences";
    private static final String KEY_SELECTED_SPORT = "selected_sport";
    private static final String LEGACY_KEY_SPORT = "sport";

    private SportPreferences() {}

    public static String getSelectedSport(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String selectedSport = preferences.getString(KEY_SELECTED_SPORT, null);
        if (isSupportedSport(selectedSport)) {
            return selectedSport;
        }

        SharedPreferences legacyPreferences = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String legacySport = legacyPreferences.getString(LEGACY_KEY_SPORT, Sports.sports.get(0));
        selectedSport = isSupportedSport(legacySport) ? legacySport : Sports.sports.get(0);
        setSelectedSport(context, selectedSport);
        return selectedSport;
    }

    public static void setSelectedSport(Context context, String sport) {
        if (!isSupportedSport(sport)) {
            throw new IllegalArgumentException("Unsupported sport: " + sport);
        }
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SELECTED_SPORT, sport)
                .apply();
    }

    private static boolean isSupportedSport(String sport) {
        return sport != null && Sports.sports.contains(sport);
    }
}
