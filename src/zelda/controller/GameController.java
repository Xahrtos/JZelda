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
 * - Interazione NPC con E (prompt + shop menu)
 * - Transizioni:
 *   - D: (0..6)->(1..7) con slide LEFT (orizzontale)
 *   - A: (1..7)->(0..6) con slide RIGHT (orizzontale)
 *   - W da stanza 7: entra shop (8) con slide UP (verticale: entra dal basso)
 *   - S da shop (8): torna stanza 7 con slide DOWN (verticale: entra dall'alto)
 *
 * Nota collisioni (stile Zelda top-down):
 * Sprite del player 32x32, collisione fatta SOLO sui piedi (hitbox più bassa).
 */
public class GameController {

    private final GameModel model;

    private boolean up, down, left, right;

    // one-shot presses (da GamePanel keybinds)
    private boolean interactPressed; // E
    private boolean escPressed;      // ESC

    private final float speed = 200f;

    // ---- PLAYER SPRITE / HITBOX ----
    private static final int SPRITE_W = 32;
    private static final int SPRITE_H = 32;

    // Hitbox player (piedi)
    private static final int HIT_W = 16;
    private static final int HIT_H = 10;

    // Offset hitbox rispetto al top-left dello sprite
    private static final int HIT_OFF_X = (SPRITE_W - HIT_W) / 2; // 8
    private static final int HIT_OFF_Y = SPRITE_H - HIT_H;       // 22

    private static final int PLAY_LAST_INDEX = 7;
    private static final int SHOP_INDEX = 8;

    private static final int SHOP_EXIT_INDEX = 3;

    // Shop item costs
    private static final int COST_LIFE = 15;
    private static final int COST_ITEM2 = 10;
    private static final int COST_ITEM3 = 25;

    private final ProfileStore profileStore = ProfileStore.getInstance();

    // ---- ANIMAZIONE PLAYER ----
    private int animFrame = 0;                 // 0..1
    private float animTimer = 0f;
    private static final float WALK_FRAME_TIME = 0.14f;

    public GameController(GameModel model) {
        this.model = model;
    }

    public void setUp(boolean v) { up = v; }
    public void setDown(boolean v) { down = v; }
    public void setLeft(boolean v) { left = v; }
    public void setRight(boolean v) { right = v; }

    // Called by keybinds (one-shot)
    public void pressInteract() { interactPressed = true; }
    public void pressEsc() { escPressed = true; }

    public void debugWin() { simulateEndGame(true); }
    public void debugLose() { simulateEndGame(false); }

    public void update(float dt) {
        if (model.isTransitioning()) {
            resetPlayerAnim();
            // Consuma press one-shot per non “accodare” input durante slide
            interactPressed = false;
            escPressed = false;
            return;
        }

        // Se shop aperto: blocca movimento e gestisci menu
        if (model.isShopOpen()) {
            resetPlayerAnim();
            updateShopInput();
            interactPressed = false;
            escPressed = false;
            return;
        }

        // Prompt "Premi E" quando vicino all'npc
        boolean nearNpc = canInteractWithNpc();
        model.setShowInteractPrompt(nearNpc);

        // Apri shop con E se vicino
        if (interactPressed && nearNpc) {
            model.openShop();
            interactPressed = false;
            escPressed = false;
            return;
        }

        // Consuma one-shot non usati
        interactPressed = false;
        escPressed = false;

        // ---- salva posizione prima del movimento ----
        float beforeX = model.getPlayerX();
        float beforeY = model.getPlayerY();

        // --- Movimento normale ---
        float vx = 0;
        float vy = 0;

        if (up) vy -= speed;
        if (down) vy += speed;
        if (left) vx -= speed;
        if (right) vx += speed;

        // collisione asse-per-asse (buona per scorrere lungo gli ostacoli)
        moveWithCollision(vx * dt, 0);
        moveWithCollision(0, vy * dt);

        // ---- animazione ----
        updatePlayerAnim(dt, beforeX, beforeY);

        checkRoomExit();
    }

    private void resetPlayerAnim() {
        animFrame = 0;
        animTimer = 0f;
        model.setMoving(false);
        model.setAnimFrame(0);
    }

    private void updatePlayerAnim(float dt, float beforeX, float beforeY) {
        // Facing: priorità verticale poi orizzontale
        if (up) model.setFacing(GameModel.Facing.UP);
        else if (down) model.setFacing(GameModel.Facing.DOWN);
        else if (left) model.setFacing(GameModel.Facing.LEFT);
        else if (right) model.setFacing(GameModel.Facing.RIGHT);

        // Movimento reale (se spingi contro un muro non animi)
        float afterX = model.getPlayerX();
        float afterY = model.getPlayerY();
        boolean moved = (Math.abs(afterX - beforeX) > 0.01f) || (Math.abs(afterY - beforeY) > 0.01f);

        model.setMoving(moved);

        if (!moved) {
            animFrame = 0;
            animTimer = 0f;
            model.setAnimFrame(0);
            return;
        }

        animTimer += dt;
        if (animTimer >= WALK_FRAME_TIME) {
            animTimer -= WALK_FRAME_TIME;
            animFrame = (animFrame + 1) % 2;
            model.setAnimFrame(animFrame);
        }
    }

    private void updateShopInput() {
        // ESC chiude
        if (escPressed) {
            model.closeShop();
            return;
        }

        int sel = model.getShopSelectionIndex();

        // Navigazione W/S: consumiamo un "colpo" per evitare scorrimento rapidissimo
        if (up) {
            model.setShopSelectionIndex(sel - 1);
            up = false;
            return;
        }
        if (down) {
            model.setShopSelectionIndex(sel + 1);
            down = false;
            return;
        }

        if (interactPressed) {
            handleShopConfirm();
        }
    }

    private void handleShopConfirm() {
        int sel = model.getShopSelectionIndex();

        if (sel == SHOP_EXIT_INDEX) {
            model.closeShop();
            return;
        }

        int cost;
        if (sel == 0) cost = COST_LIFE;
        else if (sel == 1) cost = COST_ITEM2;
        else cost = COST_ITEM3;

        if (model.getRupees() < cost) {
            // TODO (opzionale): messaggio UI "Non abbastanza rupie"
            return;
        }

        model.addRupees(-cost);

        // Applica effetto
        if (sel == 0) {
            model.addLives(1);
        } else if (sel == 1) {
            model.addScore(100);
        } else if (sel == 2) {
            model.addScore(250);
        }
    }

    private boolean canInteractWithNpc() {
        Room room = model.getRoom();
        if (!room.hasNpc() || room.getNpcBounds() == null) return false;

        Rectangle npc = room.getNpcBounds();

        int padding = 48;
        Rectangle interactionArea = new Rectangle(
                npc.x - padding,
                npc.y - padding,
                npc.width + padding * 2,
                npc.height + padding * 2
        );

        Rectangle playerFeet = new Rectangle(
                Math.round(model.getPlayerX() + HIT_OFF_X),
                Math.round(model.getPlayerY() + HIT_OFF_Y),
                HIT_W,
                HIT_H
        );

        return playerFeet.intersects(interactionArea);
    }

    private void moveWithCollision(float dx, float dy) {
        float nextX = model.getPlayerX() + dx;
        float nextY = model.getPlayerY() + dy;

        // Collisioni tile: hitbox piedi
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

        // Collisione con NPC: hitbox piedi
        if (collidesNpc(nextX, nextY)) return;

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

        // Centro della hitbox piedi per determinare tile corrente
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