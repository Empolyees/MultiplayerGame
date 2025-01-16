package si.um.feri.kellner;

import java.util.ArrayList;

public class GameState {
    private ArrayList<Player> players = new ArrayList<>();
    private ArrayList<Projectile> projectiles = new ArrayList<>();

    private ArrayList<Obstacle> obstacles = new ArrayList<>();
    private boolean isGameOver;
    public GameState() {

    }
    public GameState(ArrayList<Player> players, ArrayList<Projectile> projectiles, ArrayList<Obstacle> obstacles) {
        this.players = players;
        this.projectiles = projectiles;
        this.obstacles = obstacles;
        this.isGameOver = false;
    }

    public ArrayList<Player> getPlayers(){
        return players;
    }

    public ArrayList<Projectile> getProjectiles(){
        return projectiles;
    }

    public void setPlayers(ArrayList<Player> players){
        players= players;
    }

    public void setProjectiles(ArrayList<Projectile> players){
        players= players;
    }

    public ArrayList<Obstacle> getObstacles() {
        return obstacles;
    }

    public void setObstacles(ArrayList<Obstacle> obstacles) {
        this.obstacles = obstacles;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }
}
