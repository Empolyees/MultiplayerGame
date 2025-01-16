package si.um.feri.kellner;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameServer {
    ArrayList<Player> players = new ArrayList<>();
    ArrayList<Projectile> projectiles = new ArrayList<>();

    ArrayList<Obstacle> obstacles = new ArrayList<>();

    private boolean isGameOver = false;
    private final GameState gameState = new GameState(players,projectiles,obstacles);

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        Kryo kryo = server.getKryo();

        kryo.register(Player.class);
        kryo.register(ArrayList.class);
        kryo.register(Projectile.class);
        kryo.register(GameState.class);
        kryo.register(Obstacle.class);
        kryo.register(kryo.register(com.badlogic.gdx.math.Rectangle.class));
        kryo.register(Color.class);

        GameServer gameServer = new GameServer();
        gameServer.initializeObstacles();
        server.addListener(new Listener() {
            public void connected(Connection connection) {
                if (gameServer.players.size() >= 2) {
                    System.out.println("Connection refused: Maximum players reached");
                    connection.close();
                    return;
                }

                Player newPlayer = new Player();
                newPlayer.id = connection.getID();

                // Určení počáteční pozice hráče
                if (gameServer.players.isEmpty()) {
                    // Hráč 1 - levý dolní roh
                    newPlayer.team = 1;
                    newPlayer.x = 50; // Trochu od kraje
                    newPlayer.y = 50;
                    newPlayer.color = Color.BLUE;
                } else if (gameServer.players.size() == 1) {
                    // Hráč 2 - pravý horní roh
                    newPlayer.team = 2;
                    newPlayer.x = 640 - 50; // Trochu od kraje (pro šířku 800)
                    newPlayer.y = 480 - 50; // Trochu od kraje (pro výšku 600)
                    newPlayer.color = Color.RED;
                }

                gameServer.players.add(newPlayer);
                System.out.println("Player connected: " + newPlayer.id);
            }

            public void received(Connection connection, Object object) {
                if (object instanceof Player) {
                    Player updatedPlayer = (Player) object;
                    for (Player p : gameServer.players) {
                        if (p.id == updatedPlayer.id) {
                            if (gameServer.canMoveTo(updatedPlayer.x, updatedPlayer.y)) {
                            p.x = updatedPlayer.x;
                            p.y = updatedPlayer.y;
                        }

                            for (Obstacle obstacle : gameServer.obstacles) {
                                if (obstacle.collidesWithPlayer(p)) {
                                    // Pokud je kolize, vrátit hráče na předchozí pozici
                                    p.x = p.prevX;
                                    p.y = p.prevY;
                                    break;
                                }
                            }
                            p.health = updatedPlayer.health;
                            p.angle = updatedPlayer.angle;

                        }
                    }
                }else if (object instanceof Projectile) {
                    Projectile newProjectile = (Projectile) object;
                    System.out.println("Dostavam Projektil: " + newProjectile);
                    gameServer.projectiles.add(newProjectile);

                }else if (object instanceof String) {
                        System.out.println("reseting gameeeeee");
                        gameServer.resetGame(); // Resetování hry
                        server.sendToAllTCP(gameServer.gameState); // Poslání nového stavu hry


                }
            }

            public void disconnected(Connection connection) {
                gameServer.players.removeIf(p -> p.id == connection.getID());
                System.out.println("Player disconnected: " + connection.getID());
            }
        });

        server.bind(54555, 54777);
        server.start();

        // Periodicky posílání dat o všech hráčích
        new Thread(() -> {
            while (true) {
                gameServer.syncGameState();
                gameServer.updateProjectiles();
                gameServer.checkCollisions(); // Kontrola kolizí
                //gameServer.checkPlayerCollisions(gameServer.g);
                gameServer.checkProjectileCollisions();
                server.sendToAllTCP(gameServer.gameState);


                try {
                    Thread.sleep(50); // Každých 50 ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        System.out.println("Server started...");
    }

    private void updateProjectiles() {
        Iterator<Projectile> iterator = projectiles.iterator();

        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.x += projectile.dx * projectile.speed * 0.05f; // 50 ms interval
            projectile.y += projectile.dy * projectile.speed * 0.05f;

            // Odstranění projektilu, pokud opustí obrazovku
            if (projectile.x < 0 || projectile.x > 800 || projectile.y < 0 || projectile.y > 600) {
                iterator.remove();
            }
        }
    }

    private void checkCollisions() {
        Iterator<Projectile> iterator = projectiles.iterator();

        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();

            // Kontrola kolize projektilu s hráči
            for (Player player : players) {
                if (player.id != projectile.ownerId) { // Nezasahovat vlastníka projektilu
                    float distance = (float) Math.sqrt(
                        Math.pow(player.x - projectile.x, 2) + Math.pow(player.y - projectile.y, 2)
                    );

                    if (distance < 30) { // Kolize, pokud je projektil blízko hráče (radius = 30)
                        player.health = Math.max(0, player.health - 20); // Snížení zdraví
                        System.out.println("Player " + player.id + " hit! Health: " + player.health);

                        // Odstranění projektilu
                        iterator.remove();
                        break; // Další hráči nemohou být zasaženi stejným projektilem
                    }
                }
            }
        }
    }

    private void syncGameState() {
        gameState.setPlayers(players);
        gameState.setProjectiles(projectiles);
        gameState.setObstacles(obstacles);
        checkGameOver();
        //System.out.println("STATE OF GAME OVER: "+ gameState.isGameOver());
    }

    public GameState getGameState() {
        return gameState;
    }

    private boolean canMoveTo(float newX, float newY) {
        Rectangle futureBounds = new Rectangle(newX - 15, newY - 15, 30, 30); // Poloměr hráče = 15
        for (Obstacle obstacle : obstacles) {
            if (obstacle.getBounds().overlaps(futureBounds)) {
                return false; // Pokud budoucí pozice koliduje s překážkou, vrátí false
            }
        }
        return true; // Pokud žádná kolize, vrátí true
    }

    private void initializeObstacles() {
        // Přidejte překážky (x, y, šířka, výška,barva)
        obstacles.add(new Obstacle(200, 200, 50, 100,Color.BROWN)); // Např. obdelink v levo
        obstacles.add(new Obstacle(400, 300, 100, 50,Color.BROWN)); // Např. obdélník v pravo
        obstacles.add(new Obstacle(400, 150, 50, 50,Color.BROWN));
        obstacles.add(new Obstacle(0,0,5,480,Color.GRAY));
        obstacles.add(new Obstacle(0,0,640,5,Color.GRAY));
        obstacles.add(new Obstacle(640 - 5, 0, 5, 480,Color.GRAY));
        obstacles.add(new Obstacle(0, 480 - 5, 640, 5,Color.GRAY));
    }

    private void checkPlayerCollisions(Player player) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.collidesWithPlayer(player)) {
                // Vraťte hráče na původní pozici
                player.x -= 5;
                player.y -= 5;
            }
        }
    }

    private void checkProjectileCollisions() {
        Iterator<Projectile> projectileIterator = projectiles.iterator();

        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();

            for (Obstacle obstacle : obstacles) {
                if (obstacle.collidesWithProjectile(projectile)) {
                    System.out.println("Projectile destroyed by obstacle!");
                    projectileIterator.remove();
                    break;
                }
            }
        }
    }

    private void resetGame() {
        gameState.setGameOver(false);

        // Resetování hráčů
        for (Player player : players) {
            player.health = 100;
            if (player.team == 1) {
                player.x = 50;
                player.y = 50;
                player.color = Color.BLUE;
            } else if (player.team == 2) {
                player.x = 640 - 50;
                player.y = 480 - 50;
                player.color = Color.RED;
            }
        }

        // Vyčištění střel a překážek
        projectiles.clear();
        obstacles.clear();
        initializeObstacles();

        System.out.println("Game has been reset.");
    }

    private void checkGameOver() {

        if (players.size() > 1) {
            boolean team1Alive = false;
            boolean team2Alive = false;

            for (Player player : players) {
                if (player.team == 1 && player.health > 0) {
                    team1Alive = true;
                }
                if (player.team == 2 && player.health > 0) {
                    team2Alive = true;
                }
            }

            if (!team1Alive) {
                gameState.setGameOver(true);
                //System.out.println("Team 2 wins!");

            } else if (!team2Alive) {
                gameState.setGameOver(true);
                //System.out.println("Team 1 wins!");
            }
        }

    }
}


class Player {
    public int id;
    public float x, y;
    public int health = 100;
    public float prevX, prevY;
    public float angle = 0;

    public int team;

    public Color color;

    // Metoda pro získání oblasti hráče
    public Rectangle getBounds() {
        return new Rectangle(x - 15, y - 15, 30, 30); // Poloměr hráče = 15
    }

    // Metoda pro získání oblasti pušky
    public Rectangle getGunBounds(float mouseX, float mouseY) {
        float gunWidth = 40; // Délka pušky
        float gunHeight = 10; // Šířka pušky

        // Výpočet směru k myši
        float dx = mouseX - x;
        float dy = mouseY - y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        // Normalizovaný směr
        float nx = dx / length;
        float ny = dy / length;

        // Výpočet pozice pušky relativně k hráči
        float gunX = x + nx * 15; // Puška začíná u okraje hráče
        float gunY = y + ny * 15;

        return new Rectangle(gunX, gunY, gunWidth, gunHeight);
    }

}


