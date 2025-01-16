package si.um.feri.kellner;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;


public class GameClient extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private Client client;
    private ArrayList<Player> players;
    private ArrayList<Obstacle> obstacles;

    private ArrayList<Projectile> projectiles;

    private boolean isGameOver;

    private int localPlayerId; // ID lokálního hráče

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        players = new ArrayList<>();
        obstacles = new ArrayList<>();
        projectiles = new ArrayList<>();
        client = new Client();

        registerClasses(client);
        startClient();
    }

    // Zpracování pohybu hráče
    private void handleInput() {
        // Najít lokálního hráče podle ID
        Player localPlayer = null;
        for (Player player : players) {
            if (player.id == client.getID()) {
                localPlayer = player;
                break;
            }
        }

        if (localPlayer != null) {
            float newX = localPlayer.x;
            float newY = localPlayer.y;
            // Posun hráče
            boolean positionChanged = false;
            if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                newY += 5;
                positionChanged = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                newY -= 5;
                positionChanged = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                newX -= 5;
                positionChanged = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                newX += 5;
                positionChanged = true;
            }

            if (canMoveTo(newX, newY)) {
                localPlayer.x = newX;
                localPlayer.y = newY;
            }

            // Výpočet úhlu rotace
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Obrácená osa Y
            float dx = mouseX - localPlayer.x;
            float dy = mouseY - localPlayer.y;
            localPlayer.angle = (float) Math.toDegrees(Math.atan2(dy, dx));

            // Odeslání dat na server
            if (positionChanged || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                client.sendTCP(localPlayer);
            }

            // Střelba
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                Projectile newProjectile = new Projectile(localPlayer.x, localPlayer.y, dx, dy, localPlayer.id);
                client.sendTCP(newProjectile);
            }
        }
    }
    private boolean canMoveTo(float newX, float newY) {
        Rectangle newBounds = new Rectangle(newX - 15, newY - 15, 30, 30);

        for (Obstacle obstacle : obstacles) {
            if (obstacle.getBounds().overlaps(newBounds)) {
                return false;
            }
        }
        return true;
    }


    private void updateProjectiles(float deltaTime) {
        Iterator<Projectile> iterator = projectiles.iterator();

        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update(deltaTime);

            // Odstranění projektilu, pokud opustí obrazovku
            if (projectile.x < 0 || projectile.x > Gdx.graphics.getWidth() ||
                projectile.y < 0 || projectile.y > Gdx.graphics.getHeight()) {
                iterator.remove();
            }
        }
    }



    @Override
    public void render() {
        Gdx.gl.glClearColor(0.2f, 0.5f, 0.2f, 1.0f);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (!isGameOver) {
            handleInput();
            drawObstacles();
            drawPlayers();
            drawProjectiles();
            updateProjectiles(Gdx.graphics.getDeltaTime());
        } else {
            // Zobrazení zprávy o konci hry
            String gameOverMessage;
            if (players.get(0).health <= 0) {
                gameOverMessage = "Team Red Won !";
            }else {
                gameOverMessage = "Team Blue Won !";
            }
            batch.begin();
            BitmapFont font = new BitmapFont();
            font.getData().setScale(2);
            font.setColor(Color.WHITE);
            font.draw(batch, gameOverMessage, Gdx.graphics.getWidth() / 2 - 100, Gdx.graphics.getHeight() / 2);
            batch.end();

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                client.sendTCP("RESET_GAME"); // Odeslání signálu na server
                System.out.println("reseting game");
            }
        }
        shapeRenderer.end();
    }

    public void drawPlayers() {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        for (Player player : players) {
            // Vykreslení hráče
            shapeRenderer.setColor(player.color);
            shapeRenderer.circle(player.x, player.y, 20);
            drawHealthBar(player);
            // Vykreslení pušky
            shapeRenderer.setColor(Color.GRAY);
            drawGun(player,mouseX,mouseY);

        }
    }

    public void drawObstacles() {
        for (Obstacle obstacle : obstacles) {
            shapeRenderer.setColor(obstacle.color);
            shapeRenderer.rect(obstacle.bounds.x, obstacle.bounds.y, obstacle.bounds.width, obstacle.bounds.height);
        }
    }

    public void drawProjectiles() {
        shapeRenderer.setColor(Color.YELLOW);
        for (Projectile projectile : projectiles) {
            shapeRenderer.circle(projectile.x, projectile.y, 10);
        }
    }

    public void drawGun(Player player, float mouseX, float mouseY) {
        float gunWidth = 40; // Délka pušky
        float gunHeight = 10; // Šířka pušky

        shapeRenderer.rect(
            player.x,
            player.y - gunHeight / 2, // Centrované na hráče
            0,
            gunHeight / 2, // Střed rotace
            gunWidth,
            gunHeight,
            1,
            1,
            player.angle // Úhel z hráče
        );
    }

    public void drawHealthBar (Player player) {
        // Vykreslení ukazatele zdraví
        float barWidth = 60; // Šířka ukazatele zdraví
        float barHeight = 10; // Výška ukazatele zdraví
        float barX = player.x - barWidth / 2; // Vodorovná pozice
        float barY = player.y + 40; // Svislá pozice nad hráčem

        // Pozadí ukazatele zdraví
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);

        // Zdraví hráče
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(barX, barY, barWidth * (player.health / 100f), barHeight);
    }

    public void registerClasses(Client client){
        Kryo kryo = client.getKryo();
        kryo.register(Player.class);
        kryo.register(ArrayList.class);
        kryo.register(Projectile.class);
        kryo.register(GameState.class);
        kryo.register(Obstacle.class);
        kryo.register(com.badlogic.gdx.math.Rectangle.class);
        kryo.register(Color.class);
    }

    public void startClient() {
        client.start();
        try {
            client.connect(5000, "localhost", 54555, 54777);
            System.out.println("Connected to server");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Přidání jednoho společného listeneru
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Player localPlayer = new Player();
                localPlayer.id = client.getID();
                localPlayer.x = 400; // Výchozí pozice X
                localPlayer.y = 300; // Výchozí pozice Y
                client.sendTCP(localPlayer);
                // Uložení lokálního ID
                localPlayerId = client.getID();
                System.out.println("Player registered with ID: " + localPlayerId);
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof GameState) {
                    GameState gameState = (GameState) object;
                    players = gameState.getPlayers();
                    projectiles = gameState.getProjectiles();
                    obstacles = gameState.getObstacles();
                    isGameOver = gameState.isGameOver();
                }
            }
        });
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        client.stop();  
        client.close();
    }
}



