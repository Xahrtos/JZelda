package zelda.controller;

import zelda.model.GameModel;
import zelda.model.Room;
import zelda.model.SlideDir;
import zelda.profile.LeaderboardEntry;
import zelda.profile.ProfileStore;

import java.time.Instant;

/**
 * Controller (MVC):
 * - WASD + collisioni tile-based
 * - Transizioni:
 *   - D: (0..6)->(1..7) con slide LEFT (orizzontale)
 *   - A: (1..7)->(0..6) con slide RIGHT (orizzontale)
 *   - W da stanza 7: entra shop (8) con slide UP (verticale: entra dal basso)
 *   - S da shop (8): torna stanza 7 con slide DOWN (verticale: entra dall'alto)
 */
public class GameController {

    private final GameModel model;

    private boolean up, down, left, right;

    private final float speed = 200f;

    private static final int HIT_W = 20;
    private static final int HIT_H = 20;

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

        moveWithCollision(vx * dt, 0);
        moveWithCollision(0, vy * dt);

        checkRoomExit();
    }

    private void moveWithCollision(float dx, float dy) {
        float nextX = model.getPlayerX() + dx;
        float nextY = model.getPlayerY() + dy;

        float leftEdge = nextX;
        float rightEdge = nextX + HIT_W - 1;
        float topEdge = nextY;
        float bottomEdge = nextY + HIT_H - 1;

        boolean collides =
                collidesAt(leftEdge, topEdge) ||
                collidesAt(rightEdge, topEdge) ||
                collidesAt(leftEdge, bottomEdge) ||
                collidesAt(rightEdge, bottomEdge);

        if (!collides) {
            model.movePlayerTo(nextX, nextY);
        }
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

        float cx = px + HIT_W / 2f;
        float cy = py + HIT_H / 2f;

        int tileX = (int) (cx / GameModel.TILE_SIZE);
        int tileY = (int) (cy / GameModel.TILE_SIZE);

        float clampXMin = 0;
        float clampXMax = roomW - HIT_W;
        float clampYMin = 0;
        float clampYMax = roomH - HIT_H;

        int curr = model.getCurrentRoomIndex();

        // ---- DESTRA (D) ----
        boolean atRightEdgeCol = (tileX >= Room.COLS - 1);
        if (right && atRightEdgeCol) {
            boolean isDoor = !model.getRoom().isSolidTile(Room.COLS - 1, tileY);

            if (isDoor && curr < PLAY_LAST_INDEX) {
                model.beginRoomTransition(curr + 1, SlideDir.LEFT);
                model.movePlayerTo(0, py);
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