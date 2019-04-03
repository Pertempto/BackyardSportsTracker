package com.github.pertempto.backyardsportstracker.data;

import android.content.Context;

import com.github.pertempto.backyardsportstracker.R;
import com.github.pertempto.backyardsportstracker.Util;

import java.util.List;

public class Grouping extends BaseDataObject {
    public String sport;
    public List<Player> team1;
    public List<Player> team2;

    public Grouping(String sport, List<Player> team1, List<Player> team2) {
        super();
        this.sport = sport;
        this.team1 = team1;
        this.team2 = team2;
    }

    public String getName(Context context) {
        String team1CaptainName = team1.get(0).name;
        String team2CaptainName = team2.get(0).name;
        return String.format(context.getString(R.string.groupingNameFormat), team1CaptainName, team1.size(), team2CaptainName, team2.size());
    }

    public GroupingEntity toEntity() {
        GroupingEntity entity = new GroupingEntity(sport);
        entity.id = id;
        entity.deleted = deleted;
        return entity;
    }

    public double getTeam1Rating() {
        return Util.rateTeam(team1, sport);
    }

    public double getTeam2Rating() {
        return Util.rateTeam(team2, sport);
    }
}
