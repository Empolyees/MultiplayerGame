package si.um.feri.kellner;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

public class Obstacle {
    public Rectangle bounds;

    public Color color;

    public Obstacle(float x, float y, float width, float height,Color color) {
        this.bounds = new Rectangle(x, y, width, height);
        this.color = new Color(color);
    }

    public Obstacle() {

    }

    // Metoda pro kontrolu kolize s hráčem
    public boolean collidesWithPlayer(Player player) {
        return bounds.overlaps(player.getBounds());
    }

    // Metoda pro kontrolu kolize s projektily
    public boolean collidesWithProjectile(Projectile projectile) {
        return bounds.contains(projectile.x, projectile.y);
    }

    public Rectangle getBounds() {
        return bounds;
    }
}

