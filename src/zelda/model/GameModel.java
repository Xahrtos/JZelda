package zelda.model;

/**
 * Model principale del gioco (MVC).
 * Contiene:
 * - HUD (vite, rupie, score + profilo attivo)
 * - Player position (in pixel dentro la stanza)
 * - Gestione stanze (8 stanze lineari + shop)
 * - Stato di transizione (slide) tra stanze
 *
 * Nota: TILE_SIZE=32, HUD sopra (HUD_HEIGHT=64).
 */
public class GameModel extends ObservableModel {

    // Stile NES
    public static final int TILE_SIZE = 32;
    public static final int HUD_HEIGHT = 64;

    // Stanze#
    int start = 8;
    private final RoomManager roomManager = new RoomManager();
    private int currentRoomIndex = start;

    // Transizione slide
    private boolean transitioning = false;
    private SlideDir slideDir = SlideDir.LEFT;
    private int nextRoomIndex = 0;

    // Player (pixel nella stanza)
    private float playerX = 6 * TILE_SIZE;
    private float playerY = 6 * TILE_SIZE;

    // HUD: stats run
    private int lives = 3;
    private int rupees = 0;
    private int score = 0;

    // HUD: profilo attivo
    private String profileNickname = "Player";
    private String profileAvatarPath = "";
    private String profileId = "";

    // ---- SHOP / UI ----
    private boolean showInteractPrompt = false;

    private boolean shopOpen = false;
    private int shopSelectionIndex = 0; // 0..3 (3 = Exit)

    // ---- ANIMAZIONE PLAYER ----
    public enum Facing { DOWN, UP, LEFT, RIGHT }

    private Facing facing = Facing.DOWN;
    private boolean moving = false;

    // 0..1 (perché hai 2 frame di camminata)
    private int animFrame = 0;

    // ---- ATTACK STATE ----
    private boolean attacking = false;
    private int attackFrame = 0; // 0..1

    // ---- ENEMY (semplice) ----
    private boolean enemyAlive = true;
    private int enemyHp = 3;

    private float enemyX = 8 * TILE_SIZE;
    private float enemyY = 4 * TILE_SIZE;

    // movimento AI
    private float enemyVx = 0f;
    private float enemyVy = 0f;
    private float enemyDirTimer = 0f;

    // invuln / blink
    private float enemyInvulnT = 0f;
    private float enemyBlinkT = 0f;

    // ---- RUPEE DROP ----
    private boolean rupeeAlive = false;
    private float rupeeX = 0f;
    private float rupeeY = 0f;

    // animazione hop
    private boolean rupeeAnimating = false;
    private float rupeeAnimT = 0f;
    private float rupeeAnimDur = 0.28f;
    private float rupeeHopHeight = 12f;
    private float rupeeLandY = 0f;

    // --- getters/setters anim player ---
    public Facing getFacing() { return facing; }
    public void setFacing(Facing f) { this.facing = f; }

    public boolean isMoving() { return moving; }
    public void setMoving(boolean v) { this.moving = v; }

    public int getAnimFrame() { return animFrame; }
    public void setAnimFrame(int f) { this.animFrame = f; }

    public boolean isAttacking() { return attacking; }
    public void setAttacking(boolean v) { this.attacking = v; }

    public int getAttackFrame() { return attackFrame; }
    public void setAttackFrame(int f) { this.attackFrame = f; }

    /**
     * Forza un repaint lato view anche se la posizione non cambia.
     */
    public void requestRepaint() {
        fireEvent(new GameEvent(GameEventType.PLAYER_MOVED));
    }

    // --- enemy API ---
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

    // --- rupee API ---
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
            t = 1f;
            rupeeAnimating = false;
            rupeeY = rupeeLandY;
            requestRepaint();
            return;
        }

        // parabola: y = land - 4h * t(1-t)
        float arc = 4f * rupeeHopHeight * t * (1f - t);
        rupeeY = rupeeLandY - arc;
        requestRepaint();
    }

    // ---- ROOM API ----

    public Room getRoom() {
        return roomManager.getRoom(currentRoomIndex);
    }

    public Room getNextRoom() {
        return roomManager.getRoom(nextRoomIndex);
    }

    public int getCurrentRoomIndex() {
        return currentRoomIndex;
    }

    public int getRoomsCount() {
        return roomManager.count();
    }

    // ---- TRANSITION API ----

    public boolean isTransitioning() {
        return transitioning;
    }

    public SlideDir getSlideDir() {
        return slideDir;
    }

    public int getNextRoomIndex() {
        return nextRoomIndex;
    }

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

        // respawn/reset quando entri nella room 7 (arena)
        if (currentRoomIndex == RoomManager.PLAY_LAST_INDEX) {
            // nemico
            enemyAlive = true;
            enemyHp = 3;
            enemyX = 8 * TILE_SIZE;
            enemyY = 4 * TILE_SIZE;
            enemyVx = 0f;
            enemyVy = 0f;
            enemyDirTimer = 0f;
            enemyInvulnT = 0f;
            enemyBlinkT = 0f;

            // drop reset
            rupeeAlive = false;
            rupeeAnimating = false;
        }

        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
    }

    // ---- PLAYER API ----

    public float getPlayerX() {
        return playerX;
    }

    public float getPlayerY() {
        return playerY;
    }

    public void movePlayerTo(float x, float y) {
        this.playerX = x;
        this.playerY = y;
        fireEvent(new GameEvent(GameEventType.PLAYER_MOVED));
    }

    // ---- HUD API (run stats) ----

    public int getLives() {
        return lives;
    }

    public int getRupees() {
        return rupees;
    }

    public int getScore() {
        return score;
    }

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

    // ---- HUD API (profilo attivo) ----

    public String getProfileNickname() {
        return profileNickname;
    }

    public String getProfileAvatarPath() {
        return profileAvatarPath;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setActiveProfile(String id, String nickname, String avatarPath) {
        this.profileId = (id == null) ? "" : id.trim();
        this.profileNickname = (nickname == null || nickname.isBlank()) ? "Player" : nickname.trim();
        this.profileAvatarPath = (avatarPath == null) ? "" : avatarPath.trim();
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }
 // ---- PLAYER DAMAGE / INVULN ----
    private float playerInvulnT = 0f;
    private float playerBlinkT = 0f;

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
    public void resetRun() {
        currentRoomIndex = 0;
        transitioning = false;
        nextRoomIndex = 0;
        slideDir = SlideDir.LEFT;

        playerX = 6 * TILE_SIZE;
        playerY = 6 * TILE_SIZE;

        lives = 3;
        rupees = 0;
        score = 0;
        
        playerInvulnT = 0f;
        playerBlinkT = 0f;

        shopOpen = false;
        shopSelectionIndex = 0;
        showInteractPrompt = false;

        // reset animazioni player
        facing = Facing.DOWN;
        moving = false;
        animFrame = 0;

        attacking = false;
        attackFrame = 0;

        // reset enemy
        enemyAlive = true;
        enemyHp = 3;
        enemyX = 8 * TILE_SIZE;
        enemyY = 4 * TILE_SIZE;
        enemyVx = 0f;
        enemyVy = 0f;
        enemyDirTimer = 0f;
        enemyInvulnT = 0f;
        enemyBlinkT = 0f;

        // reset drop
        rupeeAlive = false;
        rupeeAnimating = false;

        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
        fireEvent(new GameEvent(GameEventType.PLAYER_MOVED));
    }

    // ---- UI / SHOP API ----

    public boolean isShowInteractPrompt() {
        return showInteractPrompt;
    }

    public void setShowInteractPrompt(boolean v) {
        if (showInteractPrompt == v) return;
        showInteractPrompt = v;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public boolean isShopOpen() {
        return shopOpen;
    }

    public int getShopSelectionIndex() {
        return shopSelectionIndex;
    }

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
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public void closeShop() {
        if (!shopOpen) return;
        shopOpen = false;
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }
}