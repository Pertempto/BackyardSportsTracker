package com.github.pertempto.backyardsportstracker.data;

import com.github.pertempto.backyardsportstracker.R;

import java.util.ArrayList;
import java.util.HashMap;

public class Sports {
    public static final String ALL = "all-sports";
    public static final String BASKETBALL = "sport-basketball";
    public static final String ULTIMATE = "sport-ultimate";
    public static final String TAGBALL = "sport-tagball";

    static public ArrayList<String> sports = new ArrayList<>();
    static public HashMap<String, Integer> icons = new HashMap<>();
    static public HashMap<String, Integer> names = new HashMap<>();
    static public HashMap<String, Integer> targetScores = new HashMap<>();
    static public HashMap<String, Double> changeFactors = new HashMap<>();

    static {
        sports.add(BASKETBALL);
        icons.put(BASKETBALL, R.drawable.ic_basketball);
        names.put(BASKETBALL, R.string.basketball);
        targetScores.put(BASKETBALL, 10);
        changeFactors.put(BASKETBALL, 0.2);

        sports.add(ULTIMATE);
        icons.put(ULTIMATE, R.drawable.ic_disc);
        names.put(ULTIMATE, R.string.ultimate);
        targetScores.put(ULTIMATE, 3);
        changeFactors.put(ULTIMATE, 0.2);

        sports.add(TAGBALL);
        icons.put(TAGBALL, R.drawable.ic_tagball);
        names.put(TAGBALL, R.string.tagball);
        targetScores.put(TAGBALL, 15);
        changeFactors.put(TAGBALL, 0.2);
    }
}
