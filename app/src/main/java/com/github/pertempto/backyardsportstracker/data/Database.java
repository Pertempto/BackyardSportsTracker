package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.support.annotation.NonNull;

@android.arch.persistence.room.Database(version = 8, entities = {GameEntity.class, GroupingEntity.class, PlayerEntity.class, GroupingPlayerJoin.class, GamePlayerJoin.class})
@TypeConverters({Converters.class})
public abstract class Database extends RoomDatabase {
    public abstract GameDao gameDao();
    public abstract GroupingDao groupingDao();
    public abstract PlayerDao playerDao();
    public abstract GroupingPlayerJoinDao groupingPlayerJoinDao();
    public abstract GamePlayerJoinDao gamePlayerJoinDao();

    private static volatile Database INSTANCE;

    public static Database getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context, Database.class, "database")
                            .addCallback(roomDatabaseCallback)
                            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
        }
    };
}