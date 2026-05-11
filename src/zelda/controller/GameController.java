package zelda.controller;

import zelda.model.GameModel;
import zelda.model.Room;
import zelda.model.SlideDir;
import zelda.profile.LeaderboardEntry;
import zelda.profile.ProfileStore;

import java.time.Instant;

/**
 * Controller (MVC):
 * - Input WASD
 * - Movimento + collisioni tile-based
 * - Transizione stanze in sequenza lineare (0..7) con slide:
 *      - D (destra): vai a stanza successiva se esiste
 *      - A (sinistra): vai a stanza precedente se esiste
 * - W/S (su/giù): nessuna stanza sopra/sotto (lineare), quindi blocco.
 *
 * Debug temporaneo (finché non esiste ancora un "fine partita" reale):
 * - F5: simula vittoria (played++, won++, append leaderboard, reset run)
 * - F6: simula sconfitta (played++, lost++, append leaderboard, reset run)
 *
 * Nota: durante la transizione (model.isTransitioning()) blocchiamo l'input/movimento.
 */
public class GameController {

    private final GameModel model;

    // input
    private boolean up, down, left, right;

    // velocità movimento (px/sec)
    private final float speed = 200f;

    // hitbox usata per collisioni e clamp ai bordi (in px)
    private static final int HIT_W = 20;
    private static final int HIT_H = 20;

    // profili/leaderboard
    private final ProfileStore profileStore = ProfileStore.getInstance();

    public GameController(GameModel model) {
        this.model = model;
    }

    // ---- input setters (chiamati da GamePanel) ----
    public void setUp(boolean v) { up = v; }
    public void setDown(boolean v) { down = v; }
    public void setLeft(boolean v) { left = v; }
    public void setRight(boolean v) { right = v; }

    // ---- debug endgame hooks (chiamati da GamePanel con F5/F6) ----
    public void debugWin() { simulateEndGame(true); }
    public void debugLose() { simulateEndGame(false); }

    /**
     * Update a step fisso (es. 60Hz).
     */
    public void update(float dt) {
        // Durante slide: niente input/movimento
        if (model.isTransitioning()) return;

        float vx = 0;
        float vy = 0;

        if (up) vy -= speed;
        if (down) vy += speed;
        if (left) vx -= speed;
        if (right) vx += speed;

        // movimento con collisione separata per assi (semplice e stabile)
        moveWithCollision(vx * dt, 0);
        moveWithCollision(0, vy * dt);

        // controllo "porte" + cambio stanza (o blocco)
        checkRoomExit();
    }

    /**
     * Muove il player con collisione tile-based.
     * La hitbox è più piccola della sprite (che può essere 32x32).
     */
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

    /**
     * Converte pixel -> tile e chiede se è solido.
     */
    private boolean collidesAt(float px, float py) {
        int tx = (int) (px / GameModel.TILE_SIZE);
        int ty = (int) (py / GameModel.TILE_SIZE);
        return model.getRoom().isSolidTile(tx, ty);
    }

    /**
     * Porta + sto spingendo:
     * Avvia transizione se sono sul bordo e il tile della porta su quel bordo è "aperto" (tile=0).
     *
     * Per stanze lineari:
     * - Sinistra/Destra: cambiano stanza
     * - Su/Giù: blocco (nessuna stanza)
     */
    private void checkRoomExit() {
        int roomW = Room.COLS * GameModel.TILE_SIZE;
        int roomH = Room.ROWS * GameModel.TILE_SIZE;

        float px = model.getPlayerX();
        float py = model.getPlayerY();

        // centro hitbox
        float cx = px + HIT_W / 2f;
        float cy = py + HIT_H / 2f;

        int tileX = (int) (cx / GameModel.TILE_SIZE);
        int tileY = (int) (cy / GameModel.TILE_SIZE);

        // clamp bounds in pixel (così la hitbox resta dentro)
        float clampXMin = 0;
        float clampXMax = roomW - HIT_W;
        float clampYMin = 0;
        float clampYMax = roomH - HIT_H;

        // ---- DESTRA (D) ----
        boolean atRightEdgeCol = (tileX >= Room.COLS - 1);
        if (right && atRightEdgeCol) {
            boolean isDoor = !model.getRoom().isSolidTile(Room.COLS - 1, tileY);

            if (isDoor) {
                int curr = model.getCurrentRoomIndex();
                if (curr < model.getRoomsCount() - 1) {
                    model.beginRoomTransition(curr + 1, SlideDir.LEFT);
                    // ingresso nella nuova stanza: lato sinistro
                    model.movePlayerTo(0, py);
                    return;
                }
            }

            // non porta / ultima stanza => blocco
            model.movePlayerTo(clampXMax, py);
            return;
        }

        // ---- SINISTRA (A) ----
        boolean atLeftEdgeCol = (tileX <= 0);
        if (left && atLeftEdgeCol) {
            boolean isDoor = !model.getRoom().isSolidTile(0, tileY);

            if (isDoor) {
                int curr = model.getCurrentRoomIndex();
                if (curr > 0) {
                    model.beginRoomTransition(curr - 1, SlideDir.RIGHT);
                    // ingresso nella nuova stanza: lato destro
                    model.movePlayerTo(clampXMax, py);
                    return;
                }
            }

            // non porta / prima stanza => blocco
            model.movePlayerTo(clampXMin, py);
            return;
        }

        // ---- SU (W) ----
        boolean atTopEdgeRow = (tileY <= 0);
        if (up && atTopEdgeRow) {
            // anche se ci fosse una "porta", sopra non c'è stanza: blocco
            model.movePlayerTo(px, clampYMin);
            return;
        }

        // ---- GIU (S) ----
        boolean atBottomEdgeRow = (tileY >= Room.ROWS - 1);
        if (down && atBottomEdgeRow) {
            // anche se ci fosse una "porta", sotto non c'è stanza: blocco
            model.movePlayerTo(px, clampYMax);
        }
    }

    /**
     * Simula fine partita:
     * - aggiorna profilo (played++, won++/lost++)
     * - scrive leaderboard entry
     * - reset run completo
     */
    private void simulateEndGame(boolean won) {
        String pid = model.getProfileId();
        if (pid == null || pid.isBlank()) return;

        // 1) Stats profilo
        profileStore.recordMatchResult(pid, won);

        // 2) Leaderboard entry (ordinata poi per score)
        profileStore.appendLeaderboardEntry(new LeaderboardEntry(
                Instant.now(),
                pid,
                model.getScore(),
                won,
                model.getCurrentRoomIndex() + 1, // livello raggiunto (1..8)
                model.getRupees()
        ));

        // 3) Reset run (come da specifica: resetta tutto)
        model.resetRun();
    }
}