package si.um.feri.kellner;

public class Projectile {
    public float x, y; // Aktuální pozice projektilu
    public float dx, dy; // Směr pohybu
    public int ownerId; // ID hráče, který projektil vystřelil
    public float speed = 25; // Rychlost projektilu

    public Projectile() {
        // Konstruktor pro Kryo
    }

    public Projectile(float x, float y, float dx, float dy, int ownerId) {
        this.x = x;
        this.y = y;
        // Normalizace směru
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        this.dx = (dx / length) * speed; // Směr vynásobený rychlostí
        this.dy = (dy / length) * speed; // Směr vynásobený rychlostí
        this.ownerId = ownerId;
    }

    // Aktualizace pozice projektilu
    public void update(float deltaTime) {
        x += dx * speed * deltaTime;
        y += dy * speed * deltaTime;
    }
}
