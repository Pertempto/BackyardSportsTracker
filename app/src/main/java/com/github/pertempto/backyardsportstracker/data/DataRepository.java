package com.github.pertempto.backyardsportstracker.data;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DataRepository {
    private static final String LOG_TAG = "DataRepository";
    private GameDao gameDao;
    private GroupingDao groupingDao;
    private PlayerDao playerDao;
    private GroupingPlayerJoinDao groupingPlayerJoinDao;
    private GamePlayerJoinDao gamePlayerJoinDao;

    DataRepository(Application application) {
        final Database db = Database.getInstance(application);
        gameDao = db.gameDao();
        groupingDao = db.groupingDao();
        playerDao = db.playerDao();
        groupingPlayerJoinDao = db.groupingPlayerJoinDao();
        gamePlayerJoinDao = db.gamePlayerJoinDao();
    }

    public void delete(Game game) {
        new deleteAsyncTask(gameDao).execute(game.toEntity());
    }

    public void delete(Grouping grouping) {
        new deleteAsyncTask(groupingDao).execute(grouping.toEntity());
    }

    public void delete(Player player) {
        new deleteAsyncTask(playerDao).execute(player.toEntity());
    }

    List<Game> getAllGames() {
        List<Game> out = new ArrayList<>();
        for (GameEntity entity : gameDao.getAll()) {
            out.add(entityToGame(entity));
        }
        return out;
    }

    List<Grouping> getAllGroupings() {
        List<Grouping> out = new ArrayList<>();
        for (GroupingEntity entity : groupingDao.getAll()) {
            out.add(entityToGrouping(entity));
        }
        return out;
    }

    List<Player> getAllPlayers() {
        List<Player> out = new ArrayList<>();
        for (PlayerEntity entity : playerDao.getAll()) {
            out.add(entityToPlayer(entity));
        }
        return out;
    }

    Game getGame(long id) {
        return entityToGame(gameDao.getById(id));
    }

    Grouping getGrouping(long id) {
        return entityToGrouping(groupingDao.getById(id));
    }

    Player getPlayer(long id) {
        return entityToPlayer(playerDao.getById(id));
    }

    List<Game> getPlayerGames(long id) {
        List<Game> out = new ArrayList<>();
        for (GameEntity entity : gamePlayerJoinDao.getPlayerGames(id)) {
            out.add(entityToGame(entity));
        }
        return out;
    }

    private Player entityToPlayer(PlayerEntity playerEntity) {
        Player player = new Player(playerEntity.name, playerEntity.ratings);
        player.id = playerEntity.id;
        player.deleted = playerEntity.deleted;
        return player;
    }

    private Game entityToGame(GameEntity gameEntity) {
        long id = gameEntity.id;
        List<Player> team1 = new ArrayList<>();
        for (PlayerEntity entity : gamePlayerJoinDao.getTeam1Players(id)) {
            team1.add(entityToPlayer(entity));
        }
        List<Player> team2 = new ArrayList<>();
        for (PlayerEntity entity : gamePlayerJoinDao.getTeam2Players(id)) {
            team2.add(entityToPlayer(entity));
        }
        Game game = new Game(gameEntity.sport, gameEntity.date, gameEntity.initialRatings,
                gameEntity.team1Score, gameEntity.team2Score, team1, team2);
        game.id = gameEntity.id;
        game.deleted = gameEntity.deleted;
        return game;
    }

    private Grouping entityToGrouping(GroupingEntity groupingEntity) {
        long id = groupingEntity.id;
        List<Player> team1 = new ArrayList<>();
        for (PlayerEntity entity : groupingPlayerJoinDao.getTeam1Players(id)) {
            team1.add(entityToPlayer(entity));
        }
        List<Player> team2 = new ArrayList<>();
        for (PlayerEntity entity : groupingPlayerJoinDao.getTeam2Players(id)) {
            team2.add(entityToPlayer(entity));
        }
        Grouping grouping = new Grouping(groupingEntity.sport, team1, team2);
        grouping.id = id;
        grouping.deleted = groupingEntity.deleted;
        return grouping;
    }

    public void insert(final Game game) {
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                GameEntity entity = game.toEntity();
                Log.d(LOG_TAG, String.format("id: %d, deleted: %b, sport: %s", entity.id, entity.deleted, entity.sport));
                long id = gameDao.insert(entity);
                Log.d(LOG_TAG, String.format("new game id: %d", id));
                for (Player player : game.team1) {
                    gamePlayerJoinDao.insert(new GamePlayerJoin(id, player.id, 1));
                }
                for (Player player : game.team2) {
                    gamePlayerJoinDao.insert(new GamePlayerJoin(id, player.id, 2));
                }
            }
        }).execute();
    }

    public void insert(final Grouping grouping) {
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                GroupingEntity entity = grouping.toEntity();
                Log.d(LOG_TAG, String.format("id: %d, deleted: %b, sport: %s", entity.id, entity.deleted, entity.sport));
                long id = groupingDao.insert(entity);
                Log.d(LOG_TAG, String.format("new grouping id: %d", id));
                for (Player player : grouping.team1) {
                    groupingPlayerJoinDao.insert(new GroupingPlayerJoin(id, player.id, 1));
                }
                for (Player player : grouping.team2) {
                    groupingPlayerJoinDao.insert(new GroupingPlayerJoin(id, player.id, 2));
                }
            }
        }).execute();
    }

    public void insert(Player player) {
        new insertAsyncTask(playerDao).execute(player.toEntity());
    }

    public void update(Player player) {
        new updateAsyncTask(playerDao).execute(player.toEntity());
    }

    private static class deleteAsyncTask extends AsyncTask<BaseEntity, Void, Void> {
        private DataDao dataDao;

        deleteAsyncTask(DataDao dao) {
            dataDao = dao;
        }

        @Override
        protected Void doInBackground(final BaseEntity... params) {
            params[0].deleted = true;
            dataDao.update(params[0]);
            return null;
        }
    }

    private static class insertAsyncTask extends AsyncTask<BaseEntity, Void, Void> {
        private DataDao dataDao;

        insertAsyncTask(DataDao dao) {
            dataDao = dao;
        }

        @Override
        protected Void doInBackground(final BaseEntity... params) {
            dataDao.insert(params[0]);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<BaseEntity, Void, Void> {
        private DataDao dataDao;

        updateAsyncTask(DataDao dao) {
            dataDao = dao;
        }

        @Override
        protected Void doInBackground(final BaseEntity... params) {
            dataDao.update(params[0]);
            return null;
        }
    }
}
