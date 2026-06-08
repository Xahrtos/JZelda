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
    private static final int HIT_OFF_Y = SPRITE_H - HIT_H;      // 22

    private static final int PLAY_LAST_INDEX = RoomManager.PLAY_LAST_INDEX;
    private static final int SHOP_INDEX = RoomManager.SHOP_INDEX;
    private static final int ARENA_INDEX = RoomManager.SHOP_ROOM_INDEX; // 4
    private static final int BOSS_ROOM_INDEX = RoomManager.PLAY_LAST_INDEX; // 7

    private static final int SHOP_EXIT_INDEX = 3;

    private static final int COST_POTION = 5;
    private static final int COST_ITEM2 = 10;
    private static final int COST_ITEM3 = 25;
    private static final float SHOP_ERR_MSG_SECONDS = 1.2f;

    private final ProfileStore profileStore = ProfileStore.getInstance();
    
    // ---- WALK WALK ANIM ----
    private int walkFrame = 0;
    private float walkTimer = 0f;
    private static final float WALK_FRAME_TIME = 0.14f;

    // ---- ATTACK ANIM ----
    private int atkFrame = 0;
    private float atkTimer = 0f;
    private static final float ATTACK_FRAME_TIME = 0.10f;
    private static final float ATTACK_TOTAL_TIME = 0.22f;

    // ---- ENEMY (Arena - RADDOPPIATO) ----
    private static final int ENEMY_DRAW_W = 56;
    private static final int ENEMY_DRAW_H = 58;

    private static final float ENEMY_SPEED = 55f;
    private static final float ENEMY_SPEED_JITTER = 25f;

    private static final float ENEMY_DIR_MIN = 0.6f;
    private static final float ENEMY_DIR_MAX = 1.6f;

    private static final float ENEMY_INVULN_SECONDS = 0.25f;
    private static final float ENEMY_BLINK_SECONDS = 0.25f;
    
    // ---- ENEMY 2 (Patrol + Charge - RADDOPPIATO) ----
    private static final int ENEMY2_DRAW_W = 64; 
    private static final int ENEMY2_DRAW_H = 64;
    private static final float ENEMY2_PATROL_SPEED = 40f;
    private static final float ENEMY2_CHARGE_SPEED = 140f;
    private static final float ENEMY2_DIR_MIN = 1.0f;
    private static final float ENEMY2_DIR_MAX = 2.5f;
    private static final float ENEMY2_INVULN_SECONDS = 0.30f;
    private static final float ENEMY2_BLINK_SECONDS = 0.30f;
    private static final float ENEMY2_DETECTION_RADIUS = 150f;

    // ---- SWORD ----
    private static final int SWORD_W = 18;
    private static final int SWORD_H = 18;
    private static final int SWORD_REACH = 32;

    private boolean attackDidHitThisSwing = false;
    
    // ---- DROPS ----
    private static final float POTION_DROP_CHANCE = 0.25f;
    private static final int POTION_DRAW_W = 32;
    private static final int POTION_DRAW_H = 36;
    private static final int DROP_SEPARATION_X = 14;

    private static final int RUPEE_HIT_W = 12;
    private static final int RUPEE_HIT_H = 20;

    // ---- PLAYER DAMAGE FROM ENEMY / BULLETS ----
    private static final float PLAYER_INVULN_SECONDS = 0.80f;
    private static final float PLAYER_BLINK_SECONDS = 0.80f;

    // ======================
    // BOSS
    // ======================
    private static final int BOSS_DRAW_W = 32;
    private static final int BOSS_DRAW_H = 32;

    private static final float BOSS_SPEED = 85f;
    private static final float BOSS_FLOAT_FREQ = 1.25f;
    private static final float BOSS_FLOAT_AMP = 4.0f;

    private static final float BOSS_SHOOT_INTERVAL = 0.7f;
    private static final float BOSS_BULLET_SPEED = 220f;

    private static final int BOSS_BULLET_W = 18;
    private static final int BOSS_BULLET_H = 18;

    public GameController(GameModel model) {
        this.model = model;
    }

    public void setUp(boolean v) { up = v; }
    public void setDown(boolean v) { down = v; }
    public void setLeft(boolean v) { left = v; }
    public void setRight(boolean v) { right = v; }

    public void pressInteract() { interactPressed = true; } 
    public void pressEsc() { escPressed = true; }           
    public void pressAttack() { attackPressed = true; }     
    public void pressRetry() { retryPressed = true; }       
    public void pressStart() { startPressed = true; }       

    public void debugWin() { simulateEndGame(true); }
    public void debugLose() { simulateEndGame(false); }

    public void update(float dt) {
        if (model.isTitle()) {
            if (startPressed) model.startGame();
            clearOneShots();
            return;
        }

        if (model.getLives() <= 0) {
            if (retryPressed) model.resetRun();
            clearOneShots();
            return;
        }

        if (model.isTransitioning()) {
            resetWalkAnim();
            resetAttackAnim();
            clearOneShots();
            return;
        }

        if (model.isShopOpen()) {
            resetWalkAnim();
            resetAttackAnim();
            model.updateShopTimers(dt);
            updateShopInput();
            clearOneShots();
            return;
        }

        model.updateEnemyTimers(dt);
        model.updateEnemy2Timers(dt);
        model.updateRupeeAnim(dt);
        model.updatePotionAnim(dt);
        model.updatePlayerTimers(dt);
        
        updateBossRefactor(dt);
        updateBossShooting(dt);
        updateBossProjectiles(dt);

        boolean nearNpc = canInteractWithNpc();
        model.setShowInteractPrompt(nearNpc);
        if (interactPressed && nearNpc) {
            model.openShop();
            clearOneShots();
            return;
        }

        if (model.isAttacking()) {
            updateAttack(dt);
            updateEnemy(dt);
            updateEnemy2(dt);
            checkEnemy2TouchDamage();
            checkEnemyTouchDamage();
            checkRupeePickup();
            checkPotionPickup();
            clearOneShots();
            return;
        }

        if (attackPressed) {
            startAttack();
            updateEnemy(dt);
            updateEnemy2(dt);
            checkEnemy2TouchDamage();
            checkEnemyTouchDamage();
            checkRupeePickup();
            checkPotionPickup();
            clearOneShots();
            return;
        }

        clearOneShots();
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
        updateEnemy2(dt);
        checkEnemy2TouchDamage();
        checkEnemyTouchDamage();
        checkRupeePickup();
        checkPotionPickup();
    }

    private void clearOneShots() {
        startPressed = false;
        retryPressed = false;
        interactPressed = false;
        escPressed = false;
        attackPressed = false;
    }

    private void updateBossRefactor(float dt) {
        if (model.getCurrentRoomIndex() != BOSS_ROOM_INDEX) return;
        model.addBossFloatT(dt);
        if (!model.isBossAlive()) return;

        float px = model.getPlayerX();
        float py = model.getPlayerY();

        float bx = model.getBossX();
        float by = model.getBossY();
        float dx = px - bx;
        float dy = py - by;
        float dist2 = dx * dx + dy * dy;
        float dist = (float) Math.sqrt(Math.max(0.0001f, dist2));
        float vx = (dx / dist) * BOSS_SPEED;
        float vy = (dy / dist) * BOSS_SPEED;
        float nextX = bx + vx * dt;
        float nextY = by + vy * dt;
        
        int roomW = Room.COLS * GameModel.TILE_SIZE;
        int roomH = Room.ROWS * GameModel.TILE_SIZE;

        float minX = GameModel.TILE_SIZE;
        float minY = GameModel.TILE_SIZE;
        float maxX = roomW - GameModel.TILE_SIZE - BOSS_DRAW_W;
        float maxY = roomH - GameModel.TILE_SIZE - BOSS_DRAW_H;
        if (nextX < minX) nextX = minX;
        if (nextY < minY) nextY = minY;
        if (nextX > maxX) nextX = maxX;
        if (nextY > maxY) nextY = maxY;

        model.setBossPos(nextX, nextY);
        if (Math.abs(vx) > Math.abs(vy)) {
            model.setBossFacing(vx < 0 ? GameModel.Facing.LEFT : GameModel.Facing.RIGHT);
        } else {
            model.setBossFacing(vy < 0 ? GameModel.Facing.UP : GameModel.Facing.DOWN);
        }
    }

    private void updateBossShooting(float dt) {
        if (model.getCurrentRoomIndex() != BOSS_ROOM_INDEX) return;
        if (!model.isBossAlive()) return;

        float t = model.getBossShootT() + dt;
        if (t >= BOSS_SHOOT_INTERVAL) {
            t -= BOSS_SHOOT_INTERVAL;
            spawnBossBulletTowardsPlayer();
        }
        model.setBossShootT(t);
    }

    private void spawnBossBulletTowardsPlayer() {
        float bx = model.getBossX() + BOSS_DRAW_W / 2f;
        float by = model.getBossY() + BOSS_DRAW_H / 2f;

        float px = model.getPlayerX() + SPRITE_W / 2f;
        float py = model.getPlayerY() + SPRITE_H / 2f;

        float dx = px - bx;
        float dy = py - by;
        float dist = (float) Math.sqrt(Math.max(0.0001f, dx * dx + dy * dy));

        float vx = (dx / dist) * BOSS_BULLET_SPEED;
        float vy = (dy / dist) * BOSS_BULLET_SPEED;

        float spawnX = bx - BOSS_BULLET_W / 2f;
        float spawnY = by - BOSS_BULLET_H / 2f;

        model.spawnBossBullet(spawnX, spawnY, vx, vy);
    }

    private void updateBossProjectiles(float dt) {
        if (model.getCurrentRoomIndex() != BOSS_ROOM_INDEX) return;
        model.updateBossBullets(dt);

        Rectangle playerFeet = new Rectangle(
                Math.round(model.getPlayerX() + HIT_OFF_X),
                Math.round(model.getPlayerY() + HIT_OFF_Y),
                HIT_W,
                HIT_H
        );
        for (int i = 0; i < model.getBossBulletCount(); i++) {
            if (!model.isBossBulletAlive(i)) continue;
            float x = model.getBossBulletX(i);
            float y = model.getBossBulletY(i);

            if (bulletHitsSolidTile(x, y)) {
                model.despawnBossBullet(i);
                continue;
            }

            if (!model.isPlayerInvulnerable()) {
                Rectangle b = new Rectangle(Math.round(x), Math.round(y), BOSS_BULLET_W, BOSS_BULLET_H);
                if (b.intersects(playerFeet)) {
                    model.despawnBossBullet(i);
                    model.hitPlayer(1, PLAYER_INVULN_SECONDS, PLAYER_BLINK_SECONDS);
                }
            }
        }
    }

    private boolean bulletHitsSolidTile(float x, float y) {
        float cx = x + BOSS_BULLET_W / 2f;
        float cy = y + BOSS_BULLET_H / 2f;

        int tx = (int) (cx / GameModel.TILE_SIZE);
        int ty = (int) (cy / GameModel.TILE_SIZE);

        if (tx < 0 || ty < 0 || tx >= Room.COLS || ty >= Room.ROWS) return true;
        return model.getRoom().isSolidTile(tx, ty);
    }

    // -------------------- ENEMY 1 --------------------
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

        // Limiti basati sui muri interni piastrellati per evitare incastri visivi
        float minX = GameModel.TILE_SIZE;
        float minY = GameModel.TILE_SIZE;
        float maxX = roomW - GameModel.TILE_SIZE - ENEMY_DRAW_W;
        float maxY = roomH - GameModel.TILE_SIZE - ENEMY_DRAW_H;

        boolean bounced = false;
        if (ex < minX) { ex = minX; bounced = true; }
        if (ey < minY) { ey = minY; bounced = true; }
        if (ex > maxX) { ex = maxX; bounced = true; }
        if (ey > maxY) { ey = maxY; bounced = true; }

        model.setEnemyPos(ex, ey);
        if (bounced) {
            pickNewEnemyDirection();
            model.setEnemyDirTimer(randRange(ENEMY_DIR_MIN, ENEMY_DIR_MAX));
        }
    }
    
    // -------------------- ENEMY 2 (PATROL + CHARGE) --------------------
    private void updateEnemy2(float dt) {
        if (model.getCurrentRoomIndex() != ARENA_INDEX) return;

        for (int i = 0; i < model.getEnemy2MaxUnits(); i++) {
            if (!model.isEnemy2Alive(i)) continue;

            float px = model.getPlayerX();
            float py = model.getPlayerY();
            float e2x = model.getEnemy2X(i);
            float e2y = model.getEnemy2Y(i);

            float dx = px - e2x;
            float dy = py - e2y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < ENEMY2_DETECTION_RADIUS) {
                model.setEnemy2IsCharging(i, true);
                float vx = (dx / Math.max(0.001f, dist)) * ENEMY2_CHARGE_SPEED;
                float vy = (dy / Math.max(0.001f, dist)) * ENEMY2_CHARGE_SPEED;
                model.setEnemy2Vel(i, vx, vy);

                if (Math.abs(vx) > Math.abs(vy)) {
                    model.setEnemy2Facing(i, vx < 0 ? GameModel.Facing.LEFT : GameModel.Facing.RIGHT);
                } else {
                    model.setEnemy2Facing(i, vy < 0 ? GameModel.Facing.UP : GameModel.Facing.DOWN);
                }
            } else {
                if (model.isEnemy2Charging(i)) {
                    model.setEnemy2IsCharging(i, false);
                    model.setEnemy2DirTimer(i, 0f);
                }

                float timer = model.getEnemy2DirTimer(i) - dt;
                if (timer <= 0f || (Math.abs(model.getEnemy2Vx(i)) < 0.001f && Math.abs(model.getEnemy2Vy(i)) < 0.001f)) {
                    pickNewEnemy2Direction(i);
                    timer = randRange(ENEMY2_DIR_MIN, ENEMY2_DIR_MAX);
                }
                model.setEnemy2DirTimer(i, timer);
            }

            float nextX = e2x + model.getEnemy2Vx(i) * dt;
            float nextY = e2y + model.getEnemy2Vy(i) * dt;

            int roomW = Room.COLS * GameModel.TILE_SIZE;
            int roomH = Room.ROWS * GameModel.TILE_SIZE;
            float minX = GameModel.TILE_SIZE;
            float minY = GameModel.TILE_SIZE;
            float maxX = roomW - GameModel.TILE_SIZE - ENEMY2_DRAW_W;
            float maxY = roomH - GameModel.TILE_SIZE - ENEMY2_DRAW_H;

            boolean bounced = false;
            if (nextX < minX) { nextX = minX; bounced = true; }
            if (nextY < minY) { nextY = minY; bounced = true; }
            if (nextX > maxX) { nextX = maxX; bounced = true; }
            if (nextY > maxY) { nextY = maxY; bounced = true; }

            model.setEnemy2Pos(i, nextX, nextY);
            if (bounced && !model.isEnemy2Charging(i)) {
                pickNewEnemy2Direction(i);
                model.setEnemy2DirTimer(i, randRange(ENEMY2_DIR_MIN, ENEMY2_DIR_MAX));
            }

            if (Math.abs(model.getEnemy2Vx(i)) > 0.1f || Math.abs(model.getEnemy2Vy(i)) > 0.1f) {
                int currentFrame = (int) ((System.currentTimeMillis() / 160) % 2);
                model.setEnemy2AnimFrame(i, currentFrame);
            }
        }
    }

    private void pickNewEnemy2Direction(int i) {
        int dir = (int) (Math.random() * 4);
        float vx = 0f, vy = 0f;
        switch (dir) {
            case 0 -> { vy = -ENEMY2_PATROL_SPEED; model.setEnemy2Facing(i, GameModel.Facing.UP); }
            case 1 -> { vy = ENEMY2_PATROL_SPEED;  model.setEnemy2Facing(i, GameModel.Facing.DOWN); }
            case 2 -> { vx = -ENEMY2_PATROL_SPEED; model.setEnemy2Facing(i, GameModel.Facing.LEFT); }
            case 3 -> { vx = ENEMY2_PATROL_SPEED;  model.setEnemy2Facing(i, GameModel.Facing.RIGHT); }
        }
        model.setEnemy2Vel(i, vx, vy);
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
        tryHitEnemy2WithSword();
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
    
    private void tryHitEnemy2WithSword() {
        if (model.getCurrentRoomIndex() != ARENA_INDEX) return;
        if (!model.isAttacking() || model.getAttackFrame() != 1) return;
        if (attackDidHitThisSwing) return;

        Rectangle sword = computeSwordHitbox();

        for (int i = 0; i < model.getEnemy2MaxUnits(); i++) {
            if (!model.isEnemy2Alive(i)) continue;
            if (model.isEnemy2Blinking(i)) continue; 

            Rectangle enemy2 = new Rectangle(
                    Math.round(model.getEnemy2X(i)),
                    Math.round(model.getEnemy2Y(i)),
                    ENEMY2_DRAW_W,
                    ENEMY2_DRAW_H
            );
            if (sword.intersects(enemy2)) {
                attackDidHitThisSwing = true;
                int hpBefore = model.getEnemy2Hp(i);
                
                model.hitEnemy2(i, 1, ENEMY2_INVULN_SECONDS, ENEMY2_BLINK_SECONDS);
                if (hpBefore > 0 && !model.isEnemy2Alive(i)) {
                    model.addScore(200);
                    float centerX = model.getEnemy2X(i) + ENEMY2_DRAW_W / 2f;
                    float centerY = model.getEnemy2Y(i) + ENEMY2_DRAW_H / 2f;
                    model.spawnRupee(centerX - 6, centerY - 10);
                }
                break; 
            }
        }
    }

    private void checkEnemy2TouchDamage() {
        if (model.getCurrentRoomIndex() != ARENA_INDEX) return;
        if (model.isPlayerInvulnerable()) return;

        Rectangle playerFeet = new Rectangle(
                Math.round(model.getPlayerX() + HIT_OFF_X),
                Math.round(model.getPlayerY() + HIT_OFF_Y),
                HIT_W,
                HIT_H
        );

        for (int i = 0; i < model.getEnemy2MaxUnits(); i++) {
            if (!model.isEnemy2Alive(i)) continue;

            Rectangle enemy2 = new Rectangle(
                    Math.round(model.getEnemy2X(i)),
                    Math.round(model.getEnemy2Y(i)),
                    ENEMY2_DRAW_W,
                    ENEMY2_DRAW_H
            );
            if (playerFeet.intersects(enemy2)) {
                model.hitPlayer(1, PLAYER_INVULN_SECONDS, PLAYER_BLINK_SECONDS);
                break; 
            }
        }
    }

    private void tryHitBossWithSword() {
        if (model.getCurrentRoomIndex() != BOSS_ROOM_INDEX) return;
        if (!model.isBossAlive()) return;

        if (!model.isAttacking() || model.getAttackFrame() != 1) return;
        if (attackDidHitThisSwing) return;

        Rectangle sword = computeSwordHitbox();
        Rectangle boss = new Rectangle(
                Math.round(model.getBossX()),
                Math.round(model.getBossY()),
                BOSS_DRAW_W,
                BOSS_DRAW_H
        );
        if (sword.intersects(boss)) {
            attackDidHitThisSwing = true;
            model.hitBoss(1);
            if (!model.isBossAlive()) model.addScore(1500);
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

        int visualPadY = 14;
        int visualPadX = 4;

        float hbX = nextX + HIT_OFF_X;
        float hbY = nextY + HIT_OFF_Y;

        float leftEdge = hbX - SOLID_PAD;
        float rightEdge = hbX + HIT_W - 1 + SOLID_PAD;
        float topEdge = hbY - SOLID_PAD;
        float bottomEdge = hbY + HIT_H - 1 + SOLID_PAD;

        if (dy < 0) topEdge -= visualPadY;
        if (dx < 0) leftEdge -= visualPadX;
        if (dx > 0) rightEdge += visualPadX;
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

        if (tx < 0 || ty < 0 || tx >= Room.COLS || ty >= Room.ROWS) return true;
        return model.getRoom().isSolidTile(tx, ty);
    }

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