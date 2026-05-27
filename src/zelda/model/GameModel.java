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

    // shop message (es. soldi insufficienti)
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

    // ---- ENEMY ----
    private boolean enemyAlive = true;
    private int enemyHp = 3;

    private float enemyX = 8 * TILE_SIZE;
    private float enemyY = 4 * TILE_SIZE;

    private float enemyVx = 0f;
    private float enemyVy = 0f;
    private float enemyDirTimer = 0f;

    private float enemyInvulnT = 0f;
    private float enemyBlinkT = 0f;

    // ---- BOSS (final room) ----
    private boolean bossAlive = true;
    private int bossHp = 8;
    private float bossX = 8 * TILE_SIZE;
    private float bossY = 4 * TILE_SIZE;
    private Facing bossFacing = Facing.DOWN;
    private boolean bossMoving = false;
    private int bossAnimFrame = 0;
    private float bossAnimTimer = 0f;
    private boolean bossAttacking = false;
    private int bossAttackFrame = 0;
    private float bossAttackTimer = 0f;
    private float bossAttackCooldownT = 0f;
    private boolean bossIntroActive = false;
    private boolean bossIntroDone = false;
    private float bossIntroTimer = 0f;
    private int bossIntroFrame = 0;
    private boolean playerLocked = false;

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

    // ---- basic getters ----
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

        // respawn/reset quando entri nella room arena (ora è la 4)
        if (currentRoomIndex == RoomManager.SHOP_ROOM_INDEX) {
            respawnArenaEntities();
        }

        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
    }

    private void respawnArenaEntities() {
        enemyAlive = true;
        enemyHp = 3;
        enemyX = 8 * TILE_SIZE;
        enemyY = 4 * TILE_SIZE;
        enemyVx = 0f;
        enemyVy = 0f;
        enemyDirTimer = 0f;
        enemyInvulnT = 0f;
        enemyBlinkT = 0f;

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

    // 4 voci: 0..3 (3=Exit)
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

    // ---- enemy ----
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
    public void setEnemyVel(float vx, float vy) { enemyVx = vx; enemyVy = vy; }

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

    // ---- boss ----
    public boolean isBossAlive() { return bossAlive; }
    public int getBossHp() { return bossHp; }
    public float getBossX() { return bossX; }
    public float getBossY() { return bossY; }
    public Facing getBossFacing() { return bossFacing; }
    public boolean isBossMoving() { return bossMoving; }
    public int getBossAnimFrame() { return bossAnimFrame; }
    public float getBossAnimTimer() { return bossAnimTimer; }
    public boolean isBossAttacking() { return bossAttacking; }
    public int getBossAttackFrame() { return bossAttackFrame; }
    public float getBossAttackTimer() { return bossAttackTimer; }
    public float getBossAttackCooldownT() { return bossAttackCooldownT; }
    public boolean isBossIntroActive() { return bossIntroActive; }
    public boolean isBossIntroDone() { return bossIntroDone; }
    public float getBossIntroTimer() { return bossIntroTimer; }
    public int getBossIntroFrame() { return bossIntroFrame; }
    public boolean isPlayerLocked() { return playerLocked; }

    public void setBossPos(float x, float y) {
        bossX = x;
        bossY = y;
        requestRepaint();
    }

    public void setBossFacing(Facing facing) { this.bossFacing = facing; }
    public void setBossMoving(boolean moving) { this.bossMoving = moving; }
    public void setBossAnimFrame(int frame) { this.bossAnimFrame = frame; }
    public void setBossAnimTimer(float timer) { this.bossAnimTimer = timer; }
    public void setBossAttacking(boolean attacking) { this.bossAttacking = attacking; }
    public void setBossAttackFrame(int frame) { this.bossAttackFrame = frame; }
    public void setBossAttackTimer(float timer) { this.bossAttackTimer = timer; }
    public void setBossAttackCooldownT(float cooldown) { this.bossAttackCooldownT = cooldown; }
    public void setBossIntroActive(boolean active) { this.bossIntroActive = active; }
    public void setBossIntroDone(boolean done) { this.bossIntroDone = done; }
    public void setBossIntroTimer(float timer) { this.bossIntroTimer = timer; }
    public void setBossIntroFrame(int frame) { this.bossIntroFrame = frame; }
    public void setPlayerLocked(boolean locked) { this.playerLocked = locked; }

    public void startBossIntro() {
        bossIntroActive = true;
        bossIntroTimer = 0f;
        bossIntroFrame = 0;
        playerLocked = true;
        requestRepaint();
    }

    public void stopBossIntro() {
        bossIntroActive = false;
        bossIntroDone = true;
        bossIntroTimer = 0f;
        bossIntroFrame = 0;
        playerLocked = false;
        requestRepaint();
    }

    public void resetBossEncounter() {
        bossAlive = true;
        bossHp = 8;
        bossX = 8 * TILE_SIZE;
        bossY = 4 * TILE_SIZE;
        bossFacing = Facing.DOWN;
        bossMoving = false;
        bossAnimFrame = 0;
        bossAnimTimer = 0f;
        bossAttacking = false;
        bossAttackFrame = 0;
        bossAttackTimer = 0f;
        bossAttackCooldownT = 0f;
        bossIntroActive = false;
        bossIntroDone = false;
        bossIntroTimer = 0f;
        bossIntroFrame = 0;
        playerLocked = false;
    }

    public void hitBoss(int dmg) {
        if (!bossAlive) return;
        bossHp = Math.max(0, bossHp - dmg);
        if (bossHp == 0) {
            bossAlive = false;
            bossAttacking = false;
            bossMoving = false;
        }
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

        // reset drops
        rupeeAlive = false;
        rupeeAnimating = false;
        potionAlive = false;
        potionAnimating = false;

        // enemy reset (non per forza spawnato subito: ma ok)
        enemyAlive = true;
        enemyHp = 3;
        enemyX = 8 * TILE_SIZE;
        enemyY = 4 * TILE_SIZE;
        enemyVx = 0f;
        enemyVy = 0f;
        enemyDirTimer = 0f;
        enemyInvulnT = 0f;
        enemyBlinkT = 0f;
        resetBossEncounter();
        playerLocked = false;

        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
        fireEvent(new GameEvent(GameEventType.PLAYER_MOVED));
    }
}