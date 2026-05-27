package zelda.controller;

import zelda.model.GameModel;
import zelda.model.Room;
import zelda.model.RoomManager;
import zelda.model.SlideDir;
import zelda.profile.LeaderboardEntry;
import zelda.profile.ProfileStore;

import java.awt.Rectangle;
import java.time.Instant;

public class GameController {

    private final GameModel model;

    private boolean up, down, left, right;

    private boolean interactPressed; // E
    private boolean escPressed;      // ESC
    private boolean attackPressed;   // SPACE
    private boolean retryPressed;    // X
    private boolean startPressed;    // ENTER

    private final float speed = 200f;

    // ---- PLAYER SPRITE / HITBOX ----
    private static final int SPRITE_W = 32;
    private static final int SPRITE_H = 32;

    private static final int HIT_W = 16;
    private static final int HIT_H = 10;
    
    private static final int SOLID_PAD = 4;

    private static final int HIT_OFF_X = (SPRITE_W - HIT_W) / 2; // 8
    private static final int HIT_OFF_Y = SPRITE_H - HIT_H;       // 22

    private static final int PLAY_LAST_INDEX = RoomManager.PLAY_LAST_INDEX;
    private static final int SHOP_INDEX = RoomManager.SHOP_INDEX;
    private static final int BOSS_ROOM_INDEX = RoomManager.PLAY_LAST_INDEX;

    private static final int ARENA_INDEX = RoomManager.SHOP_ROOM_INDEX; // 4

    // SHOP: 0..3 (3=Exit)
    private static final int SHOP_EXIT_INDEX = 3;

    // Shop costs
    private static final int COST_POTION = 5;
    private static final int COST_ITEM2 = 10;
    private static final int COST_ITEM3 = 25;
    private static final float SHOP_ERR_MSG_SECONDS = 1.2f;

    private final ProfileStore profileStore = ProfileStore.getInstance();

    // ---- WALK ANIM ----
    private int walkFrame = 0;
    private float walkTimer = 0f;
    private static final float WALK_FRAME_TIME = 0.14f;

    // ---- ATTACK ANIM ----
    private int atkFrame = 0;
    private float atkTimer = 0f;
    private static final float ATTACK_FRAME_TIME = 0.10f;
    private static final float ATTACK_TOTAL_TIME = 0.22f;

    // ---- ENEMY (arena room) ----
    private static final int ENEMY_DRAW_W = 28;
    private static final int ENEMY_DRAW_H = 29;

    private static final float ENEMY_SPEED = 55f;
    private static final float ENEMY_SPEED_JITTER = 25f;

    private static final float ENEMY_DIR_MIN = 0.6f;
    private static final float ENEMY_DIR_MAX = 1.6f;

    // ---- ATTACK HITBOX ----
    private static final float ENEMY_INVULN_SECONDS = 0.25f;
    private static final float ENEMY_BLINK_SECONDS = 0.25f;

    private static final int SWORD_W = 18;
    private static final int SWORD_H = 18;
    private static final int SWORD_REACH = 32;

    private boolean attackDidHitThisSwing = false;

    // ---- DROPS ----
    private static final float POTION_DROP_CHANCE = 0.25f;
    private static final int POTION_DRAW_W = 32; // 64x72 scaled 0.5
    private static final int POTION_DRAW_H = 36;
    private static final int DROP_SEPARATION_X = 14;

    private static final int RUPEE_HIT_W = 12;
    private static final int RUPEE_HIT_H = 20;

    // ---- PLAYER DAMAGE FROM ENEMY ----
    private static final float PLAYER_INVULN_SECONDS = 0.80f;
    private static final float PLAYER_BLINK_SECONDS = 0.80f;

    // ---- BOSS (final room) ----
    private static final int BOSS_W = 32;
    private static final int BOSS_H = 32;
    private static final float BOSS_SPEED = 160f;
    private static final float BOSS_WALK_FRAME_TIME = 0.28f;
    private static final float BOSS_ATTACK_TOTAL_TIME = 2.0f;
    private static final float BOSS_ATTACK_HIT_START = 1.0f;
    private static final float BOSS_ATTACK_HIT_END = 1.45f;
    private static final float BOSS_ATTACK_COOLDOWN = 0.55f;
    private static final float BOSS_ATTACK_RANGE = 48f;
    private static final int BOSS_BOUND_PAD = 4;
    private static final int BOSS_ATTACK_REACH = 26;
    private static final int BOSS_ATTACK_THICKNESS = 20;
    private static final float BOSS_INTRO_FRAME_TIME = 0.45f;

    private int lastRoomIndex = -1;

    public GameController(GameModel model) {
        this.model = model;
    }

    public void setUp(boolean v) { up = v; }
    public void setDown(boolean v) { down = v; }
    public void setLeft(boolean v) { left = v; }
    public void setRight(boolean v) { right = v; }

    // one-shot binds
    public void pressInteract() { interactPressed = true; } // E
    public void pressEsc() { escPressed = true; }           // ESC
    public void pressAttack() { attackPressed = true; }     // SPACE
    public void pressRetry() { retryPressed = true; }       // X
    public void pressStart() { startPressed = true; }       // ENTER

    public void debugWin() { simulateEndGame(true); }
    public void debugLose() { simulateEndGame(false); }

    public void update(float dt) {
        // TITLE: start con ENTER
        if (model.isTitle()) {
            if (startPressed) {
                model.startGame();
            }
            lastRoomIndex = model.getCurrentRoomIndex();
            startPressed = false;
            retryPressed = false;
            interactPressed = false;
            escPressed = false;
            attackPressed = false;
            return;
        }

        // GAME OVER: retry con X
        if (model.getLives() <= 0) {
            if (retryPressed) model.resetRun();

            lastRoomIndex = model.getCurrentRoomIndex();
            startPressed = false;
            retryPressed = false;
            interactPressed = false;
            escPressed = false;
            attackPressed = false;
            return;
        }

        int roomIndex = model.getCurrentRoomIndex();
        if (roomIndex != lastRoomIndex) {
            onRoomEntered(roomIndex);
            lastRoomIndex = roomIndex;
        }

        if (model.isTransitioning()) {
            resetWalkAnim();
            resetAttackAnim();
            startPressed = false;
            retryPressed = false;
            interactPressed = false;
            escPressed = false;
            attackPressed = false;
            return;
        }

        if (model.isShopOpen()) {
            resetWalkAnim();
            resetAttackAnim();
            model.updateShopTimers(dt);
            updateShopInput();

            startPressed = false;
            retryPressed = false;
            interactPressed = false;
            escPressed = false;
            attackPressed = false;
            return;
        }

        model.updateEnemyTimers(dt);
        model.updateRupeeAnim(dt);
        model.updatePotionAnim(dt);
        model.updatePlayerTimers(dt);
        updateBoss(dt);
        checkBossAttackDamage();

        if (model.isPlayerLocked()) {
            resetWalkAnim();
            resetAttackAnim();
            startPressed = false;
            retryPressed = false;
            interactPressed = false;
            escPressed = false;
            attackPressed = false;
            return;
        }

        boolean nearNpc = canInteractWithNpc();
        model.setShowInteractPrompt(nearNpc);

        if (interactPressed && nearNpc) {
            model.openShop();
            interactPressed = false;
            escPressed = false;
            attackPressed = false;
            retryPressed = false;
            startPressed = false;
            return;
        }

        if (model.isAttacking()) {
            updateAttack(dt);
            updateEnemy(dt);
            checkEnemyTouchDamage();
            checkRupeePickup();
            checkPotionPickup();

            startPressed = false;
            retryPressed = false;
            interactPressed = false;
            escPressed = false;
            attackPressed = false;
            return;
        }

        if (attackPressed) {
            startAttack();
            updateEnemy(dt);
            checkEnemyTouchDamage();
            checkRupeePickup();
            checkPotionPickup();

            startPressed = false;
            retryPressed = false;
            interactPressed = false;
            escPressed = false;
            attackPressed = false;
            return;
        }

        startPressed = false;
        retryPressed = false;
        interactPressed = false;
        escPressed = false;
        attackPressed = false;

        float beforeX = model.getPlayerX();
        float beforeY = model.getPlayerY();

        float vx = 0;
        float vy = 0;

        if (up) vy -= speed;
        if (down) vy += speed;
        if (left) vx -= speed;
        if (right) vx += speed;

        moveWithCollision(vx * dt, 0);
        moveWithCollision(0, vy * dt);

        updateWalkAnim(dt, beforeX, beforeY);

        checkRoomExit();

        updateEnemy(dt);
        checkEnemyTouchDamage();
        checkRupeePickup();
        checkPotionPickup();
    }

    private void onRoomEntered(int roomIndex) {
        if (roomIndex == BOSS_ROOM_INDEX &&
                model.isBossAlive() &&
                !model.isBossIntroActive() &&
                !model.isBossIntroDone()) {
            model.startBossIntro();
        }
    }

    private void updateBoss(float dt) {
        if (model.getCurrentRoomIndex() != BOSS_ROOM_INDEX) return;
        if (!model.isBossAlive()) return;

        if (model.isBossIntroActive()) {
            float introT = model.getBossIntroTimer() + dt;
            model.setBossIntroTimer(introT);

            int frame = 0;
            if (introT >= BOSS_INTRO_FRAME_TIME) frame = 1;
            if (introT >= BOSS_INTRO_FRAME_TIME * 2f) frame = 2;
            model.setBossIntroFrame(frame);

            if (introT >= BOSS_INTRO_FRAME_TIME * 3f) {
                model.stopBossIntro();
            }
            model.requestRepaint();
            return;
        }

        if (model.getBossAttackCooldownT() > 0f) {
            model.setBossAttackCooldownT(Math.max(0f, model.getBossAttackCooldownT() - dt));
        }

        if (model.isBossAttacking()) {
            float attackT = model.getBossAttackTimer() + dt;
            model.setBossAttackTimer(attackT);
            model.setBossAttackFrame(attackT >= BOSS_ATTACK_HIT_START ? 1 : 0);
            model.setBossMoving(false);

            if (attackT >= BOSS_ATTACK_TOTAL_TIME) {
                model.setBossAttacking(false);
                model.setBossAttackFrame(0);
                model.setBossAttackTimer(0f);
                model.setBossAttackCooldownT(BOSS_ATTACK_COOLDOWN);
            }
            model.requestRepaint();
            return;
        }

        float px = model.getPlayerX();
        float py = model.getPlayerY();
        float bx = model.getBossX();
        float by = model.getBossY();

        float dx = px - bx;
        float dy = py - by;
        float dist2 = dx * dx + dy * dy;
        float attackR2 = BOSS_ATTACK_RANGE * BOSS_ATTACK_RANGE;

        if (dist2 <= attackR2 && model.getBossAttackCooldownT() <= 0f) {
            model.setBossAttacking(true);
            model.setBossAttackTimer(0f);
            model.setBossAttackFrame(0);
            model.setBossMoving(false);
            model.requestRepaint();
            return;
        }

        float dist = (float) Math.sqrt(Math.max(0.0001f, dist2));
        float vx = (dx / dist) * BOSS_SPEED;
        float vy = (dy / dist) * BOSS_SPEED;

        float nextX = bx + vx * dt;
        float nextY = by + vy * dt;

        int roomW = Room.COLS * GameModel.TILE_SIZE;
        int roomH = Room.ROWS * GameModel.TILE_SIZE;
        float minX = GameModel.TILE_SIZE + BOSS_BOUND_PAD;
        float minY = GameModel.TILE_SIZE + BOSS_BOUND_PAD;
        float maxX = roomW - GameModel.TILE_SIZE - BOSS_W - BOSS_BOUND_PAD;
        float maxY = roomH - GameModel.TILE_SIZE - BOSS_H - BOSS_BOUND_PAD;

        if (nextX < minX) nextX = minX;
        if (nextY < minY) nextY = minY;
        if (nextX > maxX) nextX = maxX;
        if (nextY > maxY) nextY = maxY;

        model.setBossPos(nextX, nextY);
        model.setBossMoving(true);

        if (Math.abs(vx) > Math.abs(vy)) {
            model.setBossFacing(vx < 0f ? GameModel.Facing.LEFT : GameModel.Facing.RIGHT);
        } else {
            model.setBossFacing(vy < 0f ? GameModel.Facing.UP : GameModel.Facing.DOWN);
        }

        float frameT = model.getBossAnimTimer() + dt;
        if (frameT >= BOSS_WALK_FRAME_TIME) {
            frameT = 0f;
            model.setBossAnimFrame((model.getBossAnimFrame() + 1) % 2);
        }
        model.setBossAnimTimer(frameT);
    }

    private void checkBossAttackDamage() {
        if (model.getCurrentRoomIndex() != BOSS_ROOM_INDEX) return;
        if (!model.isBossAlive()) return;
        if (!model.isBossAttacking()) return;
        if (model.isBossIntroActive()) return;
        if (model.isPlayerInvulnerable()) return;

        float t = model.getBossAttackTimer();
        if (t < BOSS_ATTACK_HIT_START || t > BOSS_ATTACK_HIT_END) return;

        Rectangle hitbox = computeBossAttackHitbox();
        Rectangle playerFeet = new Rectangle(
                Math.round(model.getPlayerX() + HIT_OFF_X),
                Math.round(model.getPlayerY() + HIT_OFF_Y),
                HIT_W,
                HIT_H
        );

        if (hitbox.intersects(playerFeet)) {
            model.hitPlayer(1, PLAYER_INVULN_SECONDS, PLAYER_BLINK_SECONDS);
        }
    }

    private Rectangle computeBossAttackHitbox() {
        int bx = Math.round(model.getBossX());
        int by = Math.round(model.getBossY());
        GameModel.Facing facing = model.getBossFacing();

        return switch (facing) {
            case LEFT -> new Rectangle(
                    bx - BOSS_ATTACK_REACH,
                    by + (BOSS_H - BOSS_ATTACK_THICKNESS) / 2,
                    BOSS_ATTACK_REACH,
                    BOSS_ATTACK_THICKNESS
            );
            case RIGHT -> new Rectangle(
                    bx + BOSS_W,
                    by + (BOSS_H - BOSS_ATTACK_THICKNESS) / 2,
                    BOSS_ATTACK_REACH,
                    BOSS_ATTACK_THICKNESS
            );
            case UP -> new Rectangle(
                    bx + (BOSS_W - BOSS_ATTACK_THICKNESS) / 2,
                    by - BOSS_ATTACK_REACH,
                    BOSS_ATTACK_THICKNESS,
                    BOSS_ATTACK_REACH
            );
            case DOWN -> new Rectangle(
                    bx + (BOSS_W - BOSS_ATTACK_THICKNESS) / 2,
                    by + BOSS_H,
                    BOSS_ATTACK_THICKNESS,
                    BOSS_ATTACK_REACH
            );
        };
    }

    // -------------------- ENEMY --------------------

    private void updateEnemy(float dt) {
        if (!model.isEnemyAlive()) return;
        if (model.getCurrentRoomIndex() != ARENA_INDEX) return;

        if (Math.abs(model.getEnemyVx()) < 0.001f && Math.abs(model.getEnemyVy()) < 0.001f) {
            pickNewEnemyDirection();
            model.setEnemyDirTimer(randRange(ENEMY_DIR_MIN, ENEMY_DIR_MAX));
        }

        float timer = model.getEnemyDirTimer() - dt;
        if (timer <= 0f) {
            pickNewEnemyDirection();
            timer = randRange(ENEMY_DIR_MIN, ENEMY_DIR_MAX);
        }
        model.setEnemyDirTimer(timer);

        float ex = model.getEnemyX() + model.getEnemyVx() * dt;
        float ey = model.getEnemyY() + model.getEnemyVy() * dt;

        int roomW = Room.COLS * GameModel.TILE_SIZE;
        int roomH = Room.ROWS * GameModel.TILE_SIZE;

        boolean bounced = false;

        if (ex < 0) { ex = 0; bounced = true; }
        if (ey < 0) { ey = 0; bounced = true; }
        if (ex > roomW - ENEMY_DRAW_W) { ex = roomW - ENEMY_DRAW_W; bounced = true; }
        if (ey > roomH - ENEMY_DRAW_H) { ey = roomH - ENEMY_DRAW_H; bounced = true; }

        model.setEnemyPos(ex, ey);

        if (bounced) {
            pickNewEnemyDirection();
            model.setEnemyDirTimer(randRange(ENEMY_DIR_MIN, ENEMY_DIR_MAX));
        }
    }

    private void pickNewEnemyDirection() {
        double a = Math.random() * Math.PI * 2.0;
        float sp = ENEMY_SPEED + randRange(-ENEMY_SPEED_JITTER, ENEMY_SPEED_JITTER);

        float vx = (float) (Math.cos(a) * sp);
        float vy = (float) (Math.sin(a) * sp);

        model.setEnemyVel(vx, vy);
    }

    private float randRange(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    // -------------------- WALK ANIM --------------------

    private void resetWalkAnim() {
        walkFrame = 0;
        walkTimer = 0f;
        model.setMoving(false);
        model.setAnimFrame(0);
    }

    private void updateWalkAnim(float dt, float beforeX, float beforeY) {
        if (up) model.setFacing(GameModel.Facing.UP);
        else if (down) model.setFacing(GameModel.Facing.DOWN);
        else if (left) model.setFacing(GameModel.Facing.LEFT);
        else if (right) model.setFacing(GameModel.Facing.RIGHT);

        float afterX = model.getPlayerX();
        float afterY = model.getPlayerY();
        boolean moved = (Math.abs(afterX - beforeX) > 0.01f) || (Math.abs(afterY - beforeY) > 0.01f);

        model.setMoving(moved);

        if (!moved) {
            walkFrame = 0;
            walkTimer = 0f;
            model.setAnimFrame(0);
            return;
        }

        walkTimer += dt;
        if (walkTimer >= WALK_FRAME_TIME) {
            walkTimer -= WALK_FRAME_TIME;
            walkFrame = (walkFrame + 1) % 2;
            model.setAnimFrame(walkFrame);
        }
    }

    // -------------------- ATTACK --------------------

    private void resetAttackAnim() {
        model.setAttacking(false);
        model.setAttackFrame(0);
        atkFrame = 0;
        atkTimer = 0f;
        attackDidHitThisSwing = false;
    }

    private void startAttack() {
        resetWalkAnim();

        model.setAttacking(true);
        model.setAttackFrame(0);

        atkFrame = 0;
        atkTimer = 0f;
        attackDidHitThisSwing = false;

        model.requestRepaint();
    }

    private void updateAttack(float dt) {
        if (up) model.setFacing(GameModel.Facing.UP);
        else if (down) model.setFacing(GameModel.Facing.DOWN);
        else if (left) model.setFacing(GameModel.Facing.LEFT);
        else if (right) model.setFacing(GameModel.Facing.RIGHT);

        atkTimer += dt;

        if (atkTimer >= ATTACK_FRAME_TIME && atkFrame == 0) {
            atkFrame = 1;
            model.setAttackFrame(1);
            model.requestRepaint();
        }

        tryHitEnemyWithSword();
        tryHitBossWithSword();

        if (atkTimer >= ATTACK_TOTAL_TIME) {
            resetAttackAnim();
            model.requestRepaint();
        }
    }

    private void tryHitEnemyWithSword() {
        if (!model.isEnemyAlive()) return;
        if (model.getCurrentRoomIndex() != ARENA_INDEX) return;

        if (!model.isAttacking() || model.getAttackFrame() != 1) return;
        if (attackDidHitThisSwing) return;
        if (model.isEnemyInvulnerable()) return;

        Rectangle sword = computeSwordHitbox();
        Rectangle enemy = new Rectangle(
                Math.round(model.getEnemyX()),
                Math.round(model.getEnemyY()),
                ENEMY_DRAW_W,
                ENEMY_DRAW_H
        );

        if (sword.intersects(enemy)) {
            attackDidHitThisSwing = true;

            int hpBefore = model.getEnemyHp();
            model.hitEnemy(1, ENEMY_INVULN_SECONDS, ENEMY_BLINK_SECONDS);

            if (hpBefore > 0 && !model.isEnemyAlive()) {
                float centerX = model.getEnemyX() + ENEMY_DRAW_W / 2f;
                float centerY = model.getEnemyY() + ENEMY_DRAW_H / 2f;

                float rupeeX = centerX - DROP_SEPARATION_X - 4f;
                float rupeeY = centerY - 7f;
                model.spawnRupee(rupeeX, rupeeY);

                if (Math.random() < POTION_DROP_CHANCE) {
                    float potionX = centerX + DROP_SEPARATION_X - (POTION_DRAW_W / 2f);
                    float potionY = centerY - (POTION_DRAW_H / 2f);
                    model.spawnPotion(potionX, potionY);
                }
            }
        }
    }

    private Rectangle computeSwordHitbox() {
        float px = model.getPlayerX();
        float py = model.getPlayerY();

        int cx = Math.round(px + SPRITE_W / 2f);
        int cy = Math.round(py + SPRITE_H / 2f);

        int x = cx - SWORD_W / 2;
        int y = cy - SWORD_H / 2;

        switch (model.getFacing()) {
            case UP -> y -= SWORD_REACH;
            case DOWN -> y += SWORD_REACH;
            case LEFT -> x -= SWORD_REACH;
            case RIGHT -> x += SWORD_REACH;
        }
        return new Rectangle(x, y, SWORD_W, SWORD_H);
    }

    private void tryHitBossWithSword() {
        if (!model.isBossAlive()) return;
        if (model.getCurrentRoomIndex() != BOSS_ROOM_INDEX) return;
        if (model.isBossIntroActive()) return;
        if (!model.isAttacking() || model.getAttackFrame() != 1) return;
        if (attackDidHitThisSwing) return;

        Rectangle sword = computeSwordHitbox();
        Rectangle boss = new Rectangle(
                Math.round(model.getBossX()),
                Math.round(model.getBossY()),
                BOSS_W,
                BOSS_H
        );

        if (sword.intersects(boss)) {
            attackDidHitThisSwing = true;
            model.hitBoss(1);
            if (!model.isBossAlive()) {
                model.addScore(1000);
            }
        }
    }

    // -------------------- PLAYER DAMAGE --------------------

    private void checkEnemyTouchDamage() {
        if (!model.isEnemyAlive()) return;
        if (model.getCurrentRoomIndex() != ARENA_INDEX) return;
        if (model.isPlayerInvulnerable()) return;

        Rectangle enemy = new Rectangle(
                Math.round(model.getEnemyX()),
                Math.round(model.getEnemyY()),
                ENEMY_DRAW_W,
                ENEMY_DRAW_H
        );

        Rectangle playerFeet = new Rectangle(
                Math.round(model.getPlayerX() + HIT_OFF_X),
                Math.round(model.getPlayerY() + HIT_OFF_Y),
                HIT_W,
                HIT_H
        );

        if (playerFeet.intersects(enemy)) {
            model.hitPlayer(1, PLAYER_INVULN_SECONDS, PLAYER_BLINK_SECONDS);
        }
    }

    // -------------------- PICKUPS --------------------

    private void checkRupeePickup() {
        if (!model.isRupeeAlive()) return;
        if (model.getCurrentRoomIndex() != ARENA_INDEX) return;
        if (model.isRupeeAnimating()) return;

        Rectangle rupeeRect = new Rectangle(
                Math.round(model.getRupeeX()),
                Math.round(model.getRupeeY()),
                RUPEE_HIT_W,
                RUPEE_HIT_H
        );

        Rectangle playerFeet = new Rectangle(
                Math.round(model.getPlayerX() + HIT_OFF_X),
                Math.round(model.getPlayerY() + HIT_OFF_Y),
                HIT_W,
                HIT_H
        );

        if (playerFeet.intersects(rupeeRect)) {
            model.addRupees(1);
            model.despawnRupee();
        }
    }

    private void checkPotionPickup() {
        if (!model.isPotionAlive()) return;
        if (model.getCurrentRoomIndex() != ARENA_INDEX) return;
        if (model.isPotionAnimating()) return;

        Rectangle potionRect = new Rectangle(
                Math.round(model.getPotionX()) + 6,
                Math.round(model.getPotionY()) + 8,
                20,
                22
        );

        Rectangle playerFeet = new Rectangle(
                Math.round(model.getPlayerX() + HIT_OFF_X),
                Math.round(model.getPlayerY() + HIT_OFF_Y),
                HIT_W,
                HIT_H
        );

        if (playerFeet.intersects(potionRect)) {
            model.addLives(1);
            model.despawnPotion();
        }
    }

    // -------------------- SHOP --------------------

    private void updateShopInput() {
        if (escPressed) {
            model.closeShop();
            return;
        }

        int sel = model.getShopSelectionIndex();

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
        if (sel == 0) cost = COST_POTION;
        else if (sel == 1) cost = COST_ITEM2;
        else cost = COST_ITEM3;

        if (model.getRupees() < cost) {
            model.showShopMessage("NON HAI ABBASTANZA RUPIE!", SHOP_ERR_MSG_SECONDS);
            return;
        }

        model.addRupees(-cost);

        if (sel == 0) model.addLives(1);
        else if (sel == 1) model.addScore(100);
        else if (sel == 2) model.addScore(250);
    }

    // -------------------- COLLISION / NPC --------------------

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

        // ---- CUSCINETTI VISIVI PER EVITARE COMPENETRAZIONI ----
        // Aumenta questi valori se noti che lo sprite sfora ancora di qualche pixel
        int visualPadY = 14; // Protegge la testa del PG quando cammina verso l'alto
        int visualPadX = 4;  // Protegge le spalle quando cammina verso sinistra

        float hbX = nextX + HIT_OFF_X;
        float hbY = nextY + HIT_OFF_Y;

        // Applichiamo i pad standard di gioco
        float leftEdge = hbX - SOLID_PAD;
        float rightEdge = hbX + HIT_W - 1 + SOLID_PAD;
        float topEdge = hbY - SOLID_PAD;
        float bottomEdge = hbY + HIT_H - 1 + SOLID_PAD;

        // Modifichiamo i bordi di controllo in base alla direzione per anticipare il blocco visivo
        if (dy < 0) { // Se si muove verso l'ALTO
            topEdge -= visualPadY;
        }
        if (dx < 0) { // Se si muove verso SINISTRA
            leftEdge -= visualPadX;
        }
        if (dx > 0) { // Se si muove verso DESTRA
            rightEdge += visualPadX;
        }

        // Controllo collisioni sui 4 angoli modificati
        boolean collidesTiles =
                collidesAt(leftEdge, topEdge) ||
                collidesAt(rightEdge, topEdge) ||
                collidesAt(leftEdge, bottomEdge) ||
                collidesAt(rightEdge, bottomEdge);

        if (collidesTiles) return;
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

        // fuori bounds = solido
        if (tx < 0 || ty < 0 || tx >= Room.COLS || ty >= Room.ROWS) return true;

        return model.getRoom().isSolidTile(tx, ty);
    }

    // -------------------- ROOM EXIT --------------------

    private void checkRoomExit() {
        int roomW = Room.COLS * GameModel.TILE_SIZE;
        int roomH = Room.ROWS * GameModel.TILE_SIZE;

        float px = model.getPlayerX();
        float py = model.getPlayerY();

        float cx = (px + HIT_OFF_X) + HIT_W / 2f;
        float cy = (py + HIT_OFF_Y) + HIT_H / 2f;

        int tileX = (int) (cx / GameModel.TILE_SIZE);
        int tileY = (int) (cy / GameModel.TILE_SIZE);

        float clampXMin = -HIT_OFF_X;
        float clampXMax = (roomW - HIT_W) - HIT_OFF_X;
        float clampYMin = -HIT_OFF_Y;
        float clampYMax = (roomH - HIT_H) - HIT_OFF_Y;

        int curr = model.getCurrentRoomIndex();

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

        boolean atTopEdgeRow = (tileY <= 0);
        if (up && atTopEdgeRow) {
            boolean isDoor = !model.getRoom().isSolidTile(tileX, 0);

            if (curr == ARENA_INDEX && isDoor) {
                model.beginRoomTransition(SHOP_INDEX, SlideDir.UP);
                model.movePlayerTo(px, clampYMax);
                return;
            }

            model.movePlayerTo(px, clampYMin);
            return;
        }

        boolean atBottomEdgeRow = (tileY >= Room.ROWS - 1);
        if (down && atBottomEdgeRow) {
            boolean isDoor = !model.getRoom().isSolidTile(tileX, Room.ROWS - 1);

            if (curr == SHOP_INDEX && isDoor) {
                model.beginRoomTransition(ARENA_INDEX, SlideDir.DOWN);
                model.movePlayerTo(px, clampYMin);
                return;
            }

            model.movePlayerTo(px, clampYMax);
        }
    }

    // -------------------- DEBUG / PROFILE --------------------

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