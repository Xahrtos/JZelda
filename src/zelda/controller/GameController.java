package zelda.controller;

import zelda.model.GameModel;
import zelda.model.Room;
import zelda.model.SlideDir;
import zelda.profile.LeaderboardEntry;
import zelda.profile.ProfileStore;

import java.awt.Rectangle;
import java.time.Instant;

/**
 * Controller (MVC):
 * - WASD + collisioni tile-based
 * - Collisione con NPC (merchant)
 * - Transizioni:
 *   - D: (0..6)->(1..7) con slide LEFT (orizzontale)
 *   - A: (1..7)->(0..6) con slide RIGHT (orizzontale)
 *   - W da stanza 7: entra shop (8) con slide UP (verticale: entra dal basso)
 *   - S da shop (8): torna stanza 7 con slide DOWN (verticale: entra dall'alto)
 *
 * Nota collisioni (stile Zelda top-down):
 * Lo sprite del player è 32x32, ma la collisione deve essere fatta SOLO sui "piedi"
 * (una hitbox più bassa), così la testa può sovrapporsi ai tile senza "penetrarli".
 */
public class GameController {

    private final GameModel model;

    private boolean up, down, left, right;

    private final float speed = 200f;

    // ---- PLAYER SPRITE / HITBOX ----
    // Sprite del player: 32x32 (disegno)
    // Hitbox: solo piedi, più piccola e spostata verso il basso
    private static final int SPRITE_W = 32;
    private static final int SPRITE_H = 32;

    // Hitbox player (in pixel, coordinate stanza) - SOLO PIEDI
    private static final int HIT_W = 16;
    private static final int HIT_H = 10;

    // Offset hitbox rispetto al top-left dello sprite (playerX/playerY)
    // Centra in X, e posiziona in basso in Y.
    private static final int HIT_OFF_X = (SPRITE_W - HIT_W) / 2; // 8
    private static final int HIT_OFF_Y = SPRITE_H - HIT_H;       // 22

    private static final int PLAY_LAST_INDEX = 7;
    private static final int SHOP_INDEX = 8;

    private final ProfileStore profileStore = ProfileStore.getInstance();

    public GameController(GameModel model) {
        this.model = model;
    }

    public void setUp(boolean v) { up = v; }
    public void setDown(boolean v) { down = v; }
    public void setLeft(boolean v) { left = v; }
    public void setRight(boolean v) { right = v; }

    public void debugWin() { simulateEndGame(true); }
    public void debugLose() { simulateEndGame(false); }

    public void update(float dt) {
        if (model.isTransitioning()) return;

        float vx = 0;
        float vy = 0;

        if (up) vy -= speed;
        if (down) vy += speed;
        if (left) vx -= speed;
        if (right) vx += speed;

        // collisione asse-per-asse (buona per scorrere lungo gli ostacoli)
        moveWithCollision(vx * dt, 0);
        moveWithCollision(0, vy * dt);

        checkRoomExit();
    }

    private void moveWithCollision(float dx, float dy) {
        float nextX = model.getPlayerX() + dx;
        float nextY = model.getPlayerY() + dy;

        // 1) collisione tile-based (stile Zelda): controlliamo SOLO la hitbox dei piedi.
        float hbX = nextX + HIT_OFF_X;
        float hbY = nextY + HIT_OFF_Y;

        float leftEdge = hbX;
        float rightEdge = hbX + HIT_W - 1;
        float topEdge = hbY;
        float bottomEdge = hbY + HIT_H - 1;

        boolean collidesTiles =
                collidesAt(leftEdge, topEdge) ||
                collidesAt(rightEdge, topEdge) ||
                collidesAt(leftEdge, bottomEdge) ||
                collidesAt(rightEdge, bottomEdge);

        if (collidesTiles) return;

        // 2) collisione con NPC (merchant): anche qui usiamo la hitbox dei piedi
        if (collidesNpc(nextX, nextY)) return;

        // ok, possiamo muovere
        model.movePlayerTo(nextX, nextY);
    }

    private boolean collidesNpc(float nextX, float nextY) {
        Room room = model.getRoom();
        if (!room.hasNpc()) return false;

        Rectangle npc = room.getNpcBounds();
        if (npc == null) return false;

        Rectangle playerRect = new Rectangle(
                Math.round(nextX + HIT_OFF_X),
                Math.round(nextY + HIT_OFF_Y),
                HIT_W,
                HIT_H
        );

        return playerRect.intersects(npc);
    }

    private boolean collidesAt(float px, float py) {
        int tx = (int) (px / GameModel.TILE_SIZE);
        int ty = (int) (py / GameModel.TILE_SIZE);
        return model.getRoom().isSolidTile(tx, ty);
    }

    private void checkRoomExit() {
        int roomW = Room.COLS * GameModel.TILE_SIZE;
        int roomH = Room.ROWS * GameModel.TILE_SIZE;

        float px = model.getPlayerX();
        float py = model.getPlayerY();

        // Usiamo il centro della hitbox piedi per capire in che tile siamo
        float cx = (px + HIT_OFF_X) + HIT_W / 2f;
        float cy = (py + HIT_OFF_Y) + HIT_H / 2f;

        int tileX = (int) (cx / GameModel.TILE_SIZE);
        int tileY = (int) (cy / GameModel.TILE_SIZE);

        // Clamp della POSIZIONE DELLO SPRITE, tenendo conto della hitbox
        float clampXMin = -HIT_OFF_X;
        float clampXMax = (roomW - HIT_W) - HIT_OFF_X;
        float clampYMin = -HIT_OFF_Y;
        float clampYMax = (roomH - HIT_H) - HIT_OFF_Y;

        int curr = model.getCurrentRoomIndex();

        // ---- DESTRA (D) ----
        boolean atRightEdgeCol = (tileX >= Room.COLS - 1);
        if (right && atRightEdgeCol) {
            boolean isDoor = !model.getRoom().isSolidTile(Room.COLS - 1, tileY);

            if (isDoor && curr < PLAY_LAST_INDEX) {
                model.beginRoomTransition(curr + 1, SlideDir.LEFT);
                model.movePlayerTo(clampXMin, py);
                return;
            }

            model.movePlayerTo(clampXMax, py);
            return;
        }

        // ---- SINISTRA (A) ----
        boolean atLeftEdgeCol = (tileX <= 0);
        if (left && atLeftEdgeCol) {
            boolean isDoor = !model.getRoom().isSolidTile(0, tileY);

            if (isDoor && curr != SHOP_INDEX && curr > 0) {
                model.beginRoomTransition(curr - 1, SlideDir.RIGHT);
                model.movePlayerTo(clampXMax, py);
                return;
            }

            model.movePlayerTo(clampXMin, py);
            return;
        }

        // ---- SU (W) ----
        boolean atTopEdgeRow = (tileY <= 0);
        if (up && atTopEdgeRow) {
            boolean isDoor = !model.getRoom().isSolidTile(tileX, 0);

            // 7 -> 8 con slide UP (entra dal basso)
            if (curr == PLAY_LAST_INDEX && isDoor) {
                model.beginRoomTransition(SHOP_INDEX, SlideDir.UP);
                model.movePlayerTo(px, clampYMax);
                return;
            }

            model.movePlayerTo(px, clampYMin);
            return;
        }

        // ---- GIU (S) ----
        boolean atBottomEdgeRow = (tileY >= Room.ROWS - 1);
        if (down && atBottomEdgeRow) {
            boolean isDoor = !model.getRoom().isSolidTile(tileX, Room.ROWS - 1);

            // 8 -> 7 con slide DOWN (entra dall'alto)
            if (curr == SHOP_INDEX && isDoor) {
                model.beginRoomTransition(PLAY_LAST_INDEX, SlideDir.DOWN);
                model.movePlayerTo(px, clampYMin);
                return;
            }

            model.movePlayerTo(px, clampYMax);
        }
    }

    private void simulateEndGame(boolean won) {
        String pid = model.getProfileId();
        if (pid == null || pid.isBlank()) return;

        profileStore.recordMatchResult(pid, won);

        profileStore.appendLeaderboardEntry(new LeaderboardEntry(
                Instant.now(),
                pid,
                model.getScore(),
                won,
                Math.min(model.getCurrentRoomIndex() + 1, PLAY_LAST_INDEX + 1),
                model.getRupees()
        ));

        model.resetRun();
    }
}
