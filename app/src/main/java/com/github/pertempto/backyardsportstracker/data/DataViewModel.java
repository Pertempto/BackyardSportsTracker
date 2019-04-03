package com.github.pertempto.backyardsportstracker.data;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;

import java.util.ArrayList;
import java.util.List;

public class DataViewModel extends AndroidViewModel {
    private DataRepository repository;

    public DataViewModel(Application application) {
        super(application);
        repository = new DataRepository(application);
    }

    public List<Game> getAllGames() {
        return repository.getAllGames();
    }
    public List<Grouping> getAllGroupings() {
        return repository.getAllGroupings();
    }
    public List<Player> getAllPlayers() {
        return repository.getAllPlayers();
    }

    public List<Game> getAllGamesBySport(final String sport) {
        List<Game> out = new ArrayList<>();
        for (Game game : getAllGames()) {
            if (game.sport.equals(sport)) {
                out.add(game);
            }
        }
        return out;
    }

    public List<Player> getAllPlayersBySport(final String sport) {
        List<Player> out = new ArrayList<>();
        for (Player player : getAllPlayers()) {
            if (player.ratings.containsKey(sport)) {
                out.add(player);
            }
        }
        return out;
    }

    public Game getGame(long id) {
        return repository.getGame(id);
    }
    public Grouping getGrouping(long id) {
        return repository.getGrouping(id);
    }
    public Player getPlayer(long id) {
        return repository.getPlayer(id);
    }

    public List<Game> getPlayerGames(long id) {
        return repository.getPlayerGames(id);
    }

    public void insert(Game game) {
        repository.insert(game);
    }
    public void insert(Grouping grouping) {
        repository.insert(grouping);
    }
    public void insert(Player player) {
        repository.insert(player);
    }

    public void delete(Game game) {
        repository.delete(game);
    }
    public void delete(Grouping grouping) {
        repository.delete(grouping);
    }
    public void delete(Player player) {
        repository.delete(player);
    }

    public void update(Player player) {
        repository.update(player);
    }
}
