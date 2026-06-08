package zelda.model;

public class GameModel extends ObservableModel {

    public static final int TILE_SIZE = 32;
    public static final int HUD_HEIGHT = 64;

    // ---- APP STATE ----
    public enum AppState { TITLE, PLAY }
    private AppState appState = AppState.TITLE;

    public AppState getAppState() { return appState; }
    public boolean isTitle() { return appState == AppState.TITLE; }
    public boolean isPlaying() { return appState == AppState.PLAY; }

    public void startGame() {
        appState = AppState.PLAY;
        resetRun();
    }

    public void goToTitle() {
        appState = AppState.TITLE;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
        fireEvent(new GameEvent(GameEventType.PLAYER_MOVED));
    }

    private final RoomManager roomManager = new RoomManager();
    private int currentRoomIndex = 0;

    private boolean transitioning = false;
    private SlideDir slideDir = SlideDir.LEFT;
    private int nextRoomIndex = 0;

    private float playerX = 6 * TILE_SIZE;
    private float playerY = 6 * TILE_SIZE;

    private int lives = 3;
    private int rupees = 0;
    private int score = 0;

    private String profileNickname = "Player";
    private String profileAvatarPath = "";
    private String profileId = "";

    private boolean showInteractPrompt = false;

    private boolean shopOpen = false;
    private int shopSelectionIndex = 0; // 0..3 (3=Exit)

    private String shopMessage = "";
    private float shopMessageT = 0f;

    // ---- PLAYER ANIM ----
    public enum Facing { DOWN, UP, LEFT, RIGHT }
    private Facing facing = Facing.DOWN;
    private boolean moving = false;
    private int animFrame = 0;

    // ---- ATTACK ----
    private boolean attacking = false;
    private int attackFrame = 0;

    // ---- ENEMY 1 ----
    private boolean enemyAlive = true;
    private int enemyHp = 3;

    private float enemyX = 4 * TILE_SIZE;
    private float enemyY = 4 * TILE_SIZE;

    private float enemyVx = 0f;
    private float enemyVy = 0f;
    private float enemyDirTimer = 0f;

    private float enemyInvulnT = 0f;
    private float enemyBlinkT = 0f;

    // ---- ENEMY 2 (Array di 2 Unità) ----
    private static final int MAX_ENEMY2_UNITS = 2;
    
    private final boolean[] enemy2Alive = new boolean[MAX_ENEMY2_UNITS];
    private final int[] enemy2Hp = new int[MAX_ENEMY2_UNITS];
    private final float[] enemy2X = new float[MAX_ENEMY2_UNITS];
    private final float[] enemy2Y = new float[MAX_ENEMY2_UNITS];
    private final float[] enemy2Vx = new float[MAX_ENEMY2_UNITS];
    private final float[] enemy2Vy = new float[MAX_ENEMY2_UNITS];
    private final Facing[] enemy2Facing = new Facing[MAX_ENEMY2_UNITS];
    private final float[] enemy2DirTimer = new float[MAX_ENEMY2_UNITS];
    private final boolean[] enemy2IsCharging = new boolean[MAX_ENEMY2_UNITS];
    private final float[] enemy2InvulnT = new float[MAX_ENEMY2_UNITS];
    private final float[] enemy2BlinkT = new float[MAX_ENEMY2_UNITS];
    private final int[] enemy2AnimFrame = new int[MAX_ENEMY2_UNITS];
    private final float[] enemy2AnimT = new float[MAX_ENEMY2_UNITS];

    // ---- PLAYER INVULN ----
    private float playerInvulnT = 0f;
    private float playerBlinkT = 0f;

    // ---- RUPEE DROP ----
    private boolean rupeeAlive = false;
    private float rupeeX = 0f;
    private float rupeeY = 0f;

    private boolean rupeeAnimating = false;
    private float rupeeAnimT = 0f;
    private float rupeeAnimDur = 0.28f;
    private float rupeeHopHeight = 12f;
    private float rupeeLandY = 0f;

    // ---- POTION DROP ----
    private boolean potionAlive = false;
    private float potionX = 0f;
    private float potionY = 0f;

    private boolean potionAnimating = false;
    private float potionAnimT = 0f;
    private float potionAnimDur = 0.32f;
    private float potionHopHeight = 14f;
    private float potionLandY = 0f;

    // =========================
    // BOSS + PROJECTILES
    // =========================
    private float bossBlinkT = 0f;
    private static final float BOSS_BLINK_SECONDS = 0.35f;

    public boolean isBossBlinking() { return bossBlinkT > 0f; }
    public float getBossBlinkT() { return bossBlinkT; }

    public void updateBossTimers(float dt) {
        if (bossBlinkT > 0f) bossBlinkT = Math.max(0f, bossBlinkT - dt);
    }

    private boolean bossAlive = true;
    private int bossHp = 10;

    private float bossX = 8 * TILE_SIZE;
    private float bossY = 4 * TILE_SIZE;
    private Facing bossFacing = Facing.DOWN;

    private float bossFloatT = 0f;
    private float bossShootT = 0f;

    private static final int MAX_BOSS_BULLETS = 16;

    private final boolean[] bossBulletAlive = new boolean[MAX_BOSS_BULLETS];
    private final float[] bossBulletX = new float[MAX_BOSS_BULLETS];
    private final float[] bossBulletY = new float[MAX_BOSS_BULLETS];
    private final float[] bossBulletVx = new float[MAX_BOSS_BULLETS];
    private final float[] bossBulletVy = new float[MAX_BOSS_BULLETS];

    // ---- ENEMY 2 getters/setters ----
    public int getEnemy2MaxUnits() { return MAX_ENEMY2_UNITS; }
    public boolean isEnemy2Alive(int i) { return enemy2Alive[i]; }
    public int getEnemy2Hp(int i) { return enemy2Hp[i]; }
    public float getEnemy2X(int i) { return enemy2X[i]; }
    public float getEnemy2Y(int i) { return enemy2Y[i]; }
    public float getEnemy2Vx(int i) { return enemy2Vx[i]; }
    public float getEnemy2Vy(int i) { return enemy2Vy[i]; }
    public Facing getEnemy2Facing(int i) { return enemy2Facing[i]; }
    public int getEnemy2AnimFrame(int i) { return enemy2AnimFrame[i]; }
    public boolean isEnemy2Charging(int i) { return enemy2IsCharging[i]; }
    public float getEnemy2DirTimer(int i) { return enemy2DirTimer[i]; }
    public boolean isEnemy2Blinking(int i) { return enemy2BlinkT[i] > 0f; }
    public float getEnemy2BlinkT(int i) { return enemy2BlinkT[i]; }
    public float getEnemy2AnimT(int i) { return enemy2AnimT[i]; }

    public void setEnemy2Pos(int i, float x, float y) {
        enemy2X[i] = x;
        enemy2Y[i] = y;
        requestRepaint();
    }

    public void setEnemy2Vel(int i, float vx, float vy) {
        enemy2Vx[i] = vx;
        enemy2Vy[i] = vy;
    }

    public void setEnemy2Facing(int i, Facing f) { enemy2Facing[i] = f; }
    public void setEnemy2DirTimer(int i, float t) { enemy2DirTimer[i] = t; }
    public void setEnemy2IsCharging(int i, boolean v) { enemy2IsCharging[i] = v; }
    public void setEnemy2AnimT(int i, float t) { enemy2AnimT[i] = t; }
    public void setEnemy2AnimFrame(int i, int f) { enemy2AnimFrame[i] = f; }

    public void updateEnemy2Timers(float dt) {
        for (int i = 0; i < MAX_ENEMY2_UNITS; i++) {
            if (enemy2InvulnT[i] > 0f) enemy2InvulnT[i] = Math.max(0f, enemy2InvulnT[i] - dt);
            if (enemy2BlinkT[i] > 0f) enemy2BlinkT[i] = Math.max(0f, enemy2BlinkT[i] - dt);
        }
    }

    public void hitEnemy2(int i, int dmg, float invulnSeconds, float blinkSeconds) {
        if (!enemy2Alive[i]) return;
        if (enemy2InvulnT[i] > 0f) return;

        enemy2Hp[i] = Math.max(0, enemy2Hp[i] - dmg);
        if (enemy2Hp[i] == 0) enemy2Alive[i] = false;

        enemy2InvulnT[i] = Math.max(enemy2InvulnT[i], invulnSeconds);
        enemy2BlinkT[i] = Math.max(enemy2BlinkT[i], blinkSeconds);

        requestRepaint();
    }

    public Room getRoom() { return roomManager.getRoom(currentRoomIndex); }
    public Room getNextRoom() { return roomManager.getRoom(nextRoomIndex); }
    public int getCurrentRoomIndex() { return currentRoomIndex; }

    public boolean isTransitioning() { return transitioning; }
    public SlideDir getSlideDir() { return slideDir; }

    public float getPlayerX() { return playerX; }
    public float getPlayerY() { return playerY; }

    public int getLives() { return lives; }
    public int getRupees() { return rupees; }
    public int getScore() { return score; }

    public String getProfileNickname() { return profileNickname; }
    public String getProfileAvatarPath() { return profileAvatarPath; }
    public String getProfileId() { return profileId; }

    public Facing getFacing() { return facing; }
    public void setFacing(Facing f) { facing = f; }

    public boolean isMoving() { return moving; }
    public void setMoving(boolean v) { moving = v; }

    public int getAnimFrame() { return animFrame; }
    public void setAnimFrame(int f) { animFrame = f; }

    public boolean isAttacking() { return attacking; }
    public void setAttacking(boolean v) { attacking = v; }

    public int getAttackFrame() { return attackFrame; }
    public void setAttackFrame(int f) { attackFrame = f; }

    public void requestRepaint() {
        fireEvent(new GameEvent(GameEventType.PLAYER_MOVED));
    }

    // ---- room transition ----
    public void beginRoomTransition(int targetRoomIndex, SlideDir dir) {
        if (transitioning) return;
        if (targetRoomIndex < 0 || targetRoomIndex >= roomManager.count()) return;

        transitioning = true;
        slideDir = dir;
        nextRoomIndex = targetRoomIndex;

        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
    }

    public void finishRoomTransition() {
        if (!transitioning) return;

        currentRoomIndex = nextRoomIndex;
        transitioning = false;

        if (currentRoomIndex == RoomManager.SHOP_ROOM_INDEX || currentRoomIndex % 2 != 0) {
            respawnArenaEntities();
        }

        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
    }

    private void respawnArenaEntities() {
        enemyAlive = true;
        enemyHp = 3;
        // Spostato sicuro al centro stanza (Colonna 4, Riga 4)
        enemyX = 4 * TILE_SIZE;
        enemyY = 4 * TILE_SIZE;
        enemyVx = 0f;
        enemyVy = 0f;
        enemyDirTimer = 0f;
        enemyInvulnT = 0f;
        enemyBlinkT = 0f;

        for (int i = 0; i < MAX_ENEMY2_UNITS; i++) {
            enemy2Alive[i] = true;
            enemy2Hp[i] = 2;
            // Spawn al centro (Colonna 7 e 9, Riga 5) per non incastrarsi nei muri a causa della nuova dimensione 64x64
            enemy2X[i] = (7 + (i * 2)) * TILE_SIZE;
            enemy2Y[i] = 5 * TILE_SIZE;
            enemy2Vx[i] = 0f;
            enemy2Vy[i] = 0f;
            enemy2Facing[i] = Facing.RIGHT;
            enemy2DirTimer[i] = 0f;
            enemy2IsCharging[i] = false;
            enemy2InvulnT[i] = 0f;
            enemy2BlinkT[i] = 0f;
            enemy2AnimFrame[i] = 0;
            enemy2AnimT[i] = 0f;
        }

        rupeeAlive = false;
        rupeeAnimating = false;
        potionAlive = false;
        potionAnimating = false;
    }

    // ---- move player ----
    public void movePlayerTo(float x, float y) {
        playerX = x;
        playerY = y;
        fireEvent(new GameEvent(GameEventType.PLAYER_MOVED));
    }

    // ---- HUD changes ----
    public void addRupees(int amount) {
        rupees += amount;
        if (rupees < 0) rupees = 0;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public void addScore(int amount) {
        score += amount;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public void addLives(int amount) {
        lives += amount;
        if (lives < 0) lives = 0;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public void damagePlayer(int dmg) {
        lives -= dmg;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
        if (lives <= 0) fireEvent(new GameEvent(GameEventType.GAME_OVER));
    }

    public void setActiveProfile(String id, String nickname, String avatarPath) {
        profileId = (id == null) ? "" : id.trim();
        profileNickname = (nickname == null || nickname.isBlank()) ? "Player" : nickname.trim();
        profileAvatarPath = (avatarPath == null) ? "" : avatarPath.trim();
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    // ---- shop ui ----
    public boolean isShowInteractPrompt() { return showInteractPrompt; }
    public void setShowInteractPrompt(boolean v) {
        if (showInteractPrompt == v) return;
        showInteractPrompt = v;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public boolean isShopOpen() { return shopOpen; }
    public int getShopSelectionIndex() { return shopSelectionIndex; }

    public void setShopSelectionIndex(int idx) {
        int clamped = Math.max(0, Math.min(idx, 3));
        if (shopSelectionIndex == clamped) return;
        shopSelectionIndex = clamped;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public void openShop() {
        if (shopOpen) return;
        shopOpen = true;
        shopSelectionIndex = 0;
        shopMessage = "";
        shopMessageT = 0f;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public void closeShop() {
        if (!shopOpen) return;
        shopOpen = false;
        shopMessage = "";
        shopMessageT = 0f;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public String getShopMessage() { return shopMessage; }
    public float getShopMessageT() { return shopMessageT; }

    public void showShopMessage(String msg, float seconds) {
        shopMessage = (msg == null) ? "" : msg;
        shopMessageT = Math.max(0f, seconds);
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public void updateShopTimers(float dt) {
        if (shopMessageT > 0f) {
            shopMessageT = Math.max(0f, shopMessageT - dt);
            if (shopMessageT == 0f) shopMessage = "";
        }
    }

    // ---- enemy 1 ----
    public boolean isEnemyAlive() { return enemyAlive; }
    public int getEnemyHp() { return enemyHp; }

    public float getEnemyX() { return enemyX; }
    public float getEnemyY() { return enemyY; }

    public void setEnemyPos(float x, float y) {
        enemyX = x;
        enemyY = y;
        requestRepaint();
    }

    public float getEnemyVx() { return enemyVx; }
    public float getEnemyVy() { return enemyVy; }
    public void setEnemyVel(float vx, float vy) {
        enemyVx = vx;
        enemyVy = vy;
    }

    public float getEnemyDirTimer() { return enemyDirTimer; }
    public void setEnemyDirTimer(float t) { enemyDirTimer = t; }

    public boolean isEnemyInvulnerable() { return enemyInvulnT > 0f; }
    public boolean isEnemyBlinking() { return enemyBlinkT > 0f; }
    public float getEnemyBlinkT() { return enemyBlinkT; }

    public void updateEnemyTimers(float dt) {
        if (enemyInvulnT > 0f) enemyInvulnT = Math.max(0f, enemyInvulnT - dt);
        if (enemyBlinkT > 0f) enemyBlinkT = Math.max(0f, enemyBlinkT - dt);
    }

    public void hitEnemy(int dmg, float invulnSeconds, float blinkSeconds) {
        if (!enemyAlive) return;
        if (enemyInvulnT > 0f) return;

        enemyHp = Math.max(0, enemyHp - dmg);
        if (enemyHp == 0) enemyAlive = false;

        enemyInvulnT = Math.max(enemyInvulnT, invulnSeconds);
        enemyBlinkT = Math.max(enemyBlinkT, blinkSeconds);

        requestRepaint();
    }

    // ---- player invuln ----
    public boolean isPlayerInvulnerable() { return playerInvulnT > 0f; }
    public boolean isPlayerBlinking() { return playerBlinkT > 0f; }
    public float getPlayerBlinkT() { return playerBlinkT; }

    public void updatePlayerTimers(float dt) {
        if (playerInvulnT > 0f) playerInvulnT = Math.max(0f, playerInvulnT - dt);
        if (playerBlinkT > 0f) playerBlinkT = Math.max(0f, playerBlinkT - dt);
    }

    public void hitPlayer(int dmg, float invulnSeconds, float blinkSeconds) {
        if (playerInvulnT > 0f) return;
        damagePlayer(dmg);
        playerInvulnT = Math.max(playerInvulnT, invulnSeconds);
        playerBlinkT = Math.max(playerBlinkT, blinkSeconds);
        requestRepaint();
    }

    // ---- rupee ----
    public boolean isRupeeAlive() { return rupeeAlive; }
    public float getRupeeX() { return rupeeX; }
    public float getRupeeY() { return rupeeY; }
    public boolean isRupeeAnimating() { return rupeeAnimating; }

    public void spawnRupee(float x, float y) {
        rupeeAlive = true;
        rupeeX = x;
        rupeeY = y;

        rupeeAnimating = true;
        rupeeAnimT = 0f;
        rupeeAnimDur = 0.28f;
        rupeeHopHeight = 12f;
        rupeeLandY = y;

        requestRepaint();
    }

    public void despawnRupee() {
        rupeeAlive = false;
        rupeeAnimating = false;
        requestRepaint();
    }

    public void updateRupeeAnim(float dt) {
        if (!rupeeAlive || !rupeeAnimating) return;

        rupeeAnimT += dt;
        float t = rupeeAnimT / rupeeAnimDur;
        if (t >= 1f) {
            rupeeAnimating = false;
            rupeeY = rupeeLandY;
            requestRepaint();
            return;
        }

        float arc = 4f * rupeeHopHeight * t * (1f - t);
        rupeeY = rupeeLandY - arc;
        requestRepaint();
    }

    // ---- potion ----
    public boolean isPotionAlive() { return potionAlive; }
    public float getPotionX() { return potionX; }
    public float getPotionY() { return potionY; }
    public boolean isPotionAnimating() { return potionAnimating; }

    public void spawnPotion(float x, float y) {
        potionAlive = true;
        potionX = x;
        potionY = y;

        potionAnimating = true;
        potionAnimT = 0f;
        potionAnimDur = 0.32f;
        potionHopHeight = 14f;
        potionLandY = y;

        requestRepaint();
    }

    public void despawnPotion() {
        potionAlive = false;
        potionAnimating = false;
        requestRepaint();
    }

    public void updatePotionAnim(float dt) {
        if (!potionAlive || !potionAnimating) return;

        potionAnimT += dt;
        float t = potionAnimT / potionAnimDur;
        if (t >= 1f) {
            potionAnimating = false;
            potionY = potionLandY;
            requestRepaint();
            return;
        }

        float arc = 4f * potionHopHeight * t * (1f - t);
        potionY = potionLandY - arc;
        requestRepaint();
    }

    // =========================
    // BOSS API
    // =========================
    public boolean isBossAlive() { return bossAlive; }
    public int getBossHp() { return bossHp; }
    public float getBossX() { return bossX; }
    public float getBossY() { return bossY; }
    public Facing getBossFacing() { return bossFacing; }
    public float getBossFloatT() { return bossFloatT; }
    public float getBossShootT() { return bossShootT; }

    public void setBossPos(float x, float y) {
        bossX = x;
        bossY = y;
        requestRepaint();
    }

    public void setBossFacing(Facing f) { bossFacing = f; }

    public void addBossFloatT(float dt) { bossFloatT += dt; }
    public void setBossShootT(float t) { bossShootT = t; }

    public void hitBoss(int dmg) {
        if (!bossAlive) return;

        bossHp = Math.max(0, bossHp - dmg);
        bossBlinkT = Math.max(bossBlinkT, BOSS_BLINK_SECONDS);

        if (bossHp == 0) {
            bossAlive = false;
        }
        requestRepaint();
    }

    // =========================
    // PROJECTILES API
    // =========================
    public int getBossBulletCount() { return MAX_BOSS_BULLETS; }
    public boolean isBossBulletAlive(int i) { return bossBulletAlive[i]; }
    public float getBossBulletX(int i) { return bossBulletX[i]; }
    public float getBossBulletY(int i) { return bossBulletY[i]; }

    public void despawnBossBullet(int i) {
        bossBulletAlive[i] = false;
    }

    public boolean spawnBossBullet(float x, float y, float vx, float vy) {
        for (int i = 0; i < MAX_BOSS_BULLETS; i++) {
            if (!bossBulletAlive[i]) {
                bossBulletAlive[i] = true;
                bossBulletX[i] = x;
                bossBulletY[i] = y;
                bossBulletVx[i] = vx;
                bossBulletVy[i] = vy;
                requestRepaint();
                return true;
            }
        }
        return false;
    }

    public void updateBossBullets(float dt) {
        for (int i = 0; i < MAX_BOSS_BULLETS; i++) {
            if (!bossBulletAlive[i]) continue;
            bossBulletX[i] += bossBulletVx[i] * dt;
            bossBulletY[i] += bossBulletVy[i] * dt;
        }
    }

    // ---- reset ----
    public void resetRun() {
        transitioning = false;
        nextRoomIndex = 0;
        slideDir = SlideDir.LEFT;

        currentRoomIndex = 0;

        playerX = 6 * TILE_SIZE;
        playerY = 6 * TILE_SIZE;

        lives = 3;
        rupees = 0;
        score = 0;

        shopOpen = false;
        shopSelectionIndex = 0;
        showInteractPrompt = false;

        shopMessage = "";
        shopMessageT = 0f;

        facing = Facing.DOWN;
        moving = false;
        animFrame = 0;

        attacking = false;
        attackFrame = 0;

        playerInvulnT = 0f;
        playerBlinkT = 0f;

        rupeeAlive = false;
        rupeeAnimating = false;
        potionAlive = false;
        potionAnimating = false;

        enemyAlive = true;
        enemyHp = 3;
        enemyX = 4 * TILE_SIZE;
        enemyY = 4 * TILE_SIZE;
        enemyVx = 0f;
        enemyVy = 0f;
        enemyDirTimer = 0f;
        enemyInvulnT = 0f;
        enemyBlinkT = 0f;

        for (int i = 0; i < MAX_ENEMY2_UNITS; i++) {
            enemy2Alive[i] = true;
            enemy2Hp[i] = 2;
            enemy2X[i] = (7 + (i * 2)) * TILE_SIZE;
            enemy2Y[i] = 5 * TILE_SIZE;
            enemy2Vx[i] = 0f;
            enemy2Vy[i] = 0f;
            enemy2Facing[i] = Facing.RIGHT;
            enemy2DirTimer[i] = 0f;
            enemy2IsCharging[i] = false;
            enemy2InvulnT[i] = 0f;
            enemy2BlinkT[i] = 0f;
            enemy2AnimFrame[i] = 0;
            enemy2AnimT[i] = 0f;
        }

        bossBlinkT = 0f;
        bossAlive = true;
        bossHp = 10;
        bossX = 8 * TILE_SIZE;
        bossY = 4 * TILE_SIZE;
        bossFacing = Facing.DOWN;
        bossFloatT = 0f;
        bossShootT = 0f;

        for (int i = 0; i < MAX_BOSS_BULLETS; i++) bossBulletAlive[i] = false;

        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
        fireEvent(new GameEvent(GameEventType.PLAYER_MOVED));
    }
}