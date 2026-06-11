package zelda.model;

import zelda.root.SoundManager;

import java.util.*;

public class GameModel {

    public static final int TILE_SIZE = 32;
    public static final int HUD_HEIGHT = 60;

    private boolean isTitle = true;
    private boolean isPlaying = false;
    private boolean isTransitioning = false;
    private boolean isShopOpen = false;

    private int currentRoomIndex = 0;
    private int nextRoomIndex = 0;
    private SlideDir slideDir = SlideDir.LEFT;

    private final RoomManager roomManager = RoomManager.getInstance();
    private Room currentRoom;
    private Room nextRoom;

    // ---- PLAYER ----
    private float playerX = 96f;
    private float playerY = 144f;

    private int playerHp = 10;
    private int maxPlayerHp = 10;
    private int playerScore = 0;
    private int playerRupees = 0;
    private int playerArrows = 10;

    // ---- FACING / MOVEMENT ----
    public enum Facing { UP, DOWN, LEFT, RIGHT }
    private Facing facing = Facing.DOWN;
    private boolean isMoving = false;
    private int animFrame = 0;

    // ---- ATTACK ----
    private boolean isAttacking = false;
    private int attackFrame = 0;

    // ---- ARCHERY ----
    private boolean isArcheryActive = false;
    private int archeryFrame = 0;
    private static final int MAX_ARROWS = 20;
    private List<Arrow> arrows = new ArrayList<>();

    // ---- PLAYER TIMERS ----
    private float playerInvulnT = 0f;
    private float playerBlinkT = 0f;
    private boolean isPlayerInvulnerable = false;
    private boolean isPlayerBlinking = false;

    // ---- ENEMY 1 ----
    private float enemyX = 200f;
    private float enemyY = 200f;
    private float enemyVx = 0f;
    private float enemyVy = 0f;
    private int enemyHp = 1;
    private float enemyDirTimer = 0f;
    private float enemyInvulnT = 0f;
    private float enemyBlinkT = 0f;
    private boolean isEnemyAlive = true;
    private boolean isEnemyInvulnerable = false;
    private boolean isEnemyBlinking = false;

    // ---- ENEMY 2 ----
    private static final int ENEMY2_MAX_UNITS = 1;
    private float[] enemy2X = new float[ENEMY2_MAX_UNITS];
    private float[] enemy2Y = new float[ENEMY2_MAX_UNITS];
    private float[] enemy2Vx = new float[ENEMY2_MAX_UNITS];
    private float[] enemy2Vy = new float[ENEMY2_MAX_UNITS];
    private int[] enemy2Hp = new int[ENEMY2_MAX_UNITS];
    private float[] enemy2DirTimer = new float[ENEMY2_MAX_UNITS];
    private float[] enemy2InvulnT = new float[ENEMY2_MAX_UNITS];
    private float[] enemy2BlinkT = new float[ENEMY2_MAX_UNITS];
    private boolean[] isEnemy2Alive = new boolean[ENEMY2_MAX_UNITS];
    private boolean[] isEnemy2Charging = new boolean[ENEMY2_MAX_UNITS];
    private boolean[] isEnemy2Blinking = new boolean[ENEMY2_MAX_UNITS];
    private int[] enemy2AnimFrame = new int[ENEMY2_MAX_UNITS];
    private Facing[] enemy2Facing = new Facing[ENEMY2_MAX_UNITS];

    // ---- BOSS ----
    private float bossX = 256f;
    private float bossY = 192f;
    private int bossHp = 5;
    private boolean isBossAlive = true;
    private float bossFloatT = 0f;
    private float bossShootT = 0f;
    private boolean isBossBlinking = false;
    private float bossBlinkT = 0f;
    private Facing bossFacing = Facing.DOWN;
    private static final int MAX_BOSS_BULLETS = 10;
    private List<BossBullet> bossBullets = new ArrayList<>();
 // Aggiungi queste variabili dopo la sezione BOSS (intorno a linea 98)

    // ---- VICTORY STATE ----
    private boolean isVictoryActive = false;
    private float victoryT = 0f;
    private static final float VICTORY_DURATION = 3.0f;





// Nel metodo resetRun() (linea 197), aggiungi prima di resetAllEnemies():

    public void resetRun() {
        isTitle = true;
        isPlaying = false;
        isTransitioning = false;
        isShopOpen = false;
        isVictoryActive = false;
        victoryT = 0f;
        currentRoomIndex = 0;
        currentRoom = roomManager.getRoom(currentRoomIndex);
        resetAllEnemies();
    }

    // ---- RUPEE / POTION ----
    private float rupeeX = 0f;
    private float rupeeY = 0f;
    private boolean isRupeeAlive = false;
    private boolean isRupeeAnimating = false;
    private float rupeeAnimT = 0f;
    private float rupeeAnimDur = 0.5f;
    private float rupeeLandY = 0f;
    private float rupeeHopHeight = 20f;

    private float potionX = 0f;
    private float potionY = 0f;
    private boolean isPotionAlive = false;
    private boolean isPotionAnimating = false;
    private float potionAnimT = 0f;
    private float potionAnimDur = 0.5f;
    private float potionLandY = 0f;
    private float potionHopHeight = 20f;

    // ---- SHOP ----
    private boolean shopOpen = false;
    private int shopSelectionIndex = 0;
    private String shopMessage = "";
    private float shopMessageT = 0f;
    private boolean showInteractPrompt = false;

    // ---- PROFILE ----
    private String profileId = null;
    private String profileNickname = "Player";

    // ---- LISTENER ----
    private GameEventListener listener;

    // ---- MUSIC TRACK ----
    public enum MusicTrack {
        TITLE, DUNGEON, BOSS
    }
    private MusicTrack currentMusicTrack = MusicTrack.TITLE;

    public GameModel() {
        initializeEnemy2();
        currentRoom = roomManager.getRoom(currentRoomIndex);
    }

    private void initializeEnemy2() {
        for (int i = 0; i < ENEMY2_MAX_UNITS; i++) {
            enemy2X[i] = 100f + (i * 150f);
            enemy2Y[i] = 200f;
            enemy2Vx[i] = 0f;
            enemy2Vy[i] = 0f;
            enemy2Hp[i] = 2;
            enemy2DirTimer[i] = 1f;
            enemy2InvulnT[i] = 0f;
            enemy2BlinkT[i] = 0f;
            isEnemy2Alive[i] = true;
            isEnemy2Charging[i] = false;
            isEnemy2Blinking[i] = false;
            enemy2AnimFrame[i] = 0;
            enemy2Facing[i] = Facing.DOWN;
        }
    }

    // =======================================
    // MUSIC CONTROL
    // =======================================
    public MusicTrack getCurrentMusicTrack() {
        return currentMusicTrack;
    }

    public void setMusicTrack(MusicTrack track) {
        if (currentMusicTrack != track) {
            currentMusicTrack = track;
            SoundManager.stopMusic();
            switch (track) {
                case TITLE -> SoundManager.playMusic("game_title");
                case DUNGEON -> SoundManager.playMusic("dungeon");
                case BOSS -> SoundManager.playMusic("boss_ost");
            }
        }
    }

    // =======================================
    // GAME STATE
    // =======================================
    public void startGame() {
        isTitle = false;
        isPlaying = true;
        playerX = 96f;
        playerY = 144f;
        playerHp = maxPlayerHp;
        playerScore = 0;
        playerRupees = 0;
        playerArrows = 10;
        currentRoomIndex = 0;
        currentRoom = roomManager.getRoom(currentRoomIndex);
        resetAllEnemies();
    }

    
    private void resetAllEnemies() {
        isEnemyAlive = true;
        enemyHp = 1;
        enemyX = 200f;
        enemyY = 200f;
        enemyVx = 0f;
        enemyVy = 0f;

        initializeEnemy2();

        isBossAlive = true;
        bossHp = 5;
        bossX = 256f;
        bossY = 192f;
        bossBullets.clear();
    }

    public boolean isTitle() { return isTitle; }
    public boolean isPlaying() { return isPlaying; }
    public boolean isTransitioning() { return isTransitioning; }
    public boolean isShopOpen() { return isShopOpen; }

    public int getCurrentRoomIndex() { return currentRoomIndex; }
    public Room getRoom() { return currentRoom; }
    public Room getNextRoom() { return nextRoom; }
    public SlideDir getSlideDir() { return slideDir; }

    public void beginRoomTransition(int nextIdx, SlideDir dir) {
        isTransitioning = true;
        nextRoomIndex = nextIdx;
        slideDir = dir;
        nextRoom = roomManager.getRoom(nextIdx);
        fireGameEvent(GameEventType.ROOM_CHANGED);
    }

    public void finishRoomTransition() {
        currentRoomIndex = nextRoomIndex;
        currentRoom = nextRoom;
        isTransitioning = false;
        resetAllEnemies();
        fireGameEvent(GameEventType.ROOM_CHANGED);
    }

    // =======================================
    // PLAYER MOVEMENT
    // =======================================
    public float getPlayerX() { return playerX; }
    public float getPlayerY() { return playerY; }

    public void movePlayerTo(float x, float y) {
        playerX = x;
        playerY = y;
        fireGameEvent(GameEventType.PLAYER_MOVED);
    }

    public void setFacing(Facing f) { facing = f; }
    public Facing getFacing() { return facing; }

    public void setMoving(boolean v) { isMoving = v; }
    public boolean isMoving() { return isMoving; }

    public void setAnimFrame(int f) { animFrame = f; }
    public int getAnimFrame() { return animFrame; }

    // =======================================
    // ATTACK
    // =======================================
    public void setAttacking(boolean v) { isAttacking = v; }
    public boolean isAttacking() { return isAttacking; }

    public void setAttackFrame(int f) { attackFrame = f; }
    public int getAttackFrame() { return attackFrame; }

    // =======================================
    // ARCHERY
    // =======================================
    public void setArcheryActive(boolean v) { isArcheryActive = v; }
    public boolean isArcheryActive() { return isArcheryActive; }

    public void setArcheryFrame(int f) { archeryFrame = f; }
    public int getArcheryFrame() { return archeryFrame; }

    public int getArrows() { return playerArrows; }
    public void addArrows(int count) { playerArrows = Math.max(0, playerArrows + count); }

    public void spawnArrow(float x, float y, float vx, float vy, Facing facing) {
        if (arrows.size() < MAX_ARROWS) {
            arrows.add(new Arrow(x, y, vx, vy, facing));
            playerArrows = Math.max(0, playerArrows - 1);
        }
    }

    public void despawnArrow(int idx) {
        if (idx >= 0 && idx < arrows.size()) {
            arrows.remove(idx);
        }
    }

    public void updateArrows(float dt) {
        for (Arrow arrow : arrows) {
            arrow.update(dt);
        }
    }

    public int getMaxArrows() { return arrows.size(); }
    public boolean isArrowAlive(int idx) { return idx >= 0 && idx < arrows.size(); }
    public float getArrowX(int idx) { return idx >= 0 && idx < arrows.size() ? arrows.get(idx).x : 0; }
    public float getArrowY(int idx) { return idx >= 0 && idx < arrows.size() ? arrows.get(idx).y : 0; }
    public Facing getArrowFacing(int idx) { return idx >= 0 && idx < arrows.size() ? arrows.get(idx).facing : Facing.DOWN; }

    // =======================================
    // BOSS BULLETS
    // =======================================
    public void spawnBossBullet(float x, float y, float vx, float vy) {
        if (bossBullets.size() < MAX_BOSS_BULLETS) {
            bossBullets.add(new BossBullet(x, y, vx, vy));
        }
    }

    public void despawnBossBullet(int idx) {
        if (idx >= 0 && idx < bossBullets.size()) {
            bossBullets.remove(idx);
        }
    }

    public void updateBossBullets(float dt) {
        for (BossBullet bullet : bossBullets) {
            bullet.update(dt);
        }
    }

    public int getBossBulletCount() { return bossBullets.size(); }
    public boolean isBossBulletAlive(int idx) { return idx >= 0 && idx < bossBullets.size(); }
    public float getBossBulletX(int idx) { return idx >= 0 && idx < bossBullets.size() ? bossBullets.get(idx).x : 0; }
    public float getBossBulletY(int idx) { return idx >= 0 && idx < bossBullets.size() ? bossBullets.get(idx).y : 0; }

    // =======================================
    // PLAYER HEALTH
    // =======================================
    public int getLives() { return playerHp; }
    public void addLives(int count) { playerHp = Math.min(playerHp + count, maxPlayerHp); fireGameEvent(GameEventType.HUD_CHANGED); }

    public void hitPlayer(int damage, float invulnSeconds, float blinkSeconds) {
        playerHp -= damage;
        playerInvulnT = invulnSeconds;
        playerBlinkT = blinkSeconds;
        isPlayerInvulnerable = true;
        isPlayerBlinking = true;
        fireGameEvent(GameEventType.HUD_CHANGED);
        if (playerHp <= 0) {
        	SoundManager.stopMusic();
            SoundManager.playSound("player_death");
            fireGameEvent(GameEventType.GAME_OVER);
        }
    }

    public boolean isPlayerInvulnerable() { return isPlayerInvulnerable; }
    public boolean isPlayerBlinking() { return isPlayerBlinking; }
    public float getPlayerBlinkT() { return playerBlinkT; }

    public void updatePlayerTimers(float dt) {
        if (playerInvulnT > 0) {
            playerInvulnT -= dt;
            if (playerInvulnT <= 0) isPlayerInvulnerable = false;
        }
        if (playerBlinkT > 0) {
            playerBlinkT -= dt;
            if (playerBlinkT <= 0) isPlayerBlinking = false;
        }
    }

    // =======================================
    // SCORE & RUPEES
    // =======================================
    public int getScore() { return playerScore; }
    public void addScore(int amount) { playerScore += amount; fireGameEvent(GameEventType.HUD_CHANGED); }

    public int getRupees() { return playerRupees; }
    public void addRupees(int count) { playerRupees = Math.max(0, playerRupees + count); fireGameEvent(GameEventType.HUD_CHANGED); }

    // =======================================
    // ENEMY 1
    // =======================================
    public float getEnemyX() { return enemyX; }
    public float getEnemyY() { return enemyY; }
    public float getEnemyVx() { return enemyVx; }
    public float getEnemyVy() { return enemyVy; }
    public int getEnemyHp() { return enemyHp; }

    public void setEnemyPos(float x, float y) { enemyX = x; enemyY = y; }
    public void setEnemyVel(float vx, float vy) { enemyVx = vx; enemyVy = vy; }

    public float getEnemyDirTimer() { return enemyDirTimer; }
    public void setEnemyDirTimer(float t) { enemyDirTimer = t; }

    public boolean isEnemyAlive() { return isEnemyAlive; }
    public boolean isEnemyInvulnerable() { return isEnemyInvulnerable; }
    public boolean isEnemyBlinking() { return isEnemyBlinking; }
    public float getEnemyBlinkT() { return enemyBlinkT; }

    public void hitEnemy(int damage, float invulnSeconds, float blinkSeconds) {
        enemyHp -= damage;
        enemyInvulnT = invulnSeconds;
        enemyBlinkT = blinkSeconds;
        isEnemyInvulnerable = true;
        isEnemyBlinking = true;
        if (enemyHp <= 0) {
            isEnemyAlive = false;
        }
    }

    public void updateEnemyTimers(float dt) {
        if (enemyInvulnT > 0) {
            enemyInvulnT -= dt;
            if (enemyInvulnT <= 0) isEnemyInvulnerable = false;
        }
        if (enemyBlinkT > 0) {
            enemyBlinkT -= dt;
            if (enemyBlinkT <= 0) isEnemyBlinking = false;
        }
    }

    // =======================================
    // ENEMY 2
    // =======================================
    public int getEnemy2MaxUnits() { return ENEMY2_MAX_UNITS; }

    public float getEnemy2X(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? enemy2X[i] : 0; }
    public float getEnemy2Y(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? enemy2Y[i] : 0; }
    public float getEnemy2Vx(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? enemy2Vx[i] : 0; }
    public float getEnemy2Vy(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? enemy2Vy[i] : 0; }
    public int getEnemy2Hp(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? enemy2Hp[i] : 0; }

    public void setEnemy2Pos(int i, float x, float y) {
        if (i >= 0 && i < ENEMY2_MAX_UNITS) {
            enemy2X[i] = x;
            enemy2Y[i] = y;
        }
    }
    public void setEnemy2Vel(int i, float vx, float vy) {
        if (i >= 0 && i < ENEMY2_MAX_UNITS) {
            enemy2Vx[i] = vx;
            enemy2Vy[i] = vy;
        }
    }

    public float getEnemy2DirTimer(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? enemy2DirTimer[i] : 0; }
    public void setEnemy2DirTimer(int i, float t) {
        if (i >= 0 && i < ENEMY2_MAX_UNITS) enemy2DirTimer[i] = t;
    }

    public boolean isEnemy2Alive(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? isEnemy2Alive[i] : false; }
    public boolean isEnemy2Blinking(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? isEnemy2Blinking[i] : false; }
    public boolean isEnemy2Charging(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? isEnemy2Charging[i] : false; }
    public Facing getEnemy2Facing(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? enemy2Facing[i] : Facing.DOWN; }
    public int getEnemy2AnimFrame(int i) { return i >= 0 && i < ENEMY2_MAX_UNITS ? enemy2AnimFrame[i] : 0; }

    public void setEnemy2Facing(int i, Facing f) {
        if (i >= 0 && i < ENEMY2_MAX_UNITS) enemy2Facing[i] = f;
    }
    public void setEnemy2IsCharging(int i, boolean v) {
        if (i >= 0 && i < ENEMY2_MAX_UNITS) isEnemy2Charging[i] = v;
    }
    public void setEnemy2AnimFrame(int i, int f) {
        if (i >= 0 && i < ENEMY2_MAX_UNITS) enemy2AnimFrame[i] = f;
    }

    public void hitEnemy2(int i, int damage, float invulnSeconds, float blinkSeconds) {
        if (i >= 0 && i < ENEMY2_MAX_UNITS) {
            enemy2Hp[i] -= damage;
            enemy2InvulnT[i] = invulnSeconds;
            enemy2BlinkT[i] = blinkSeconds;
            isEnemy2Blinking[i] = true;
            if (enemy2Hp[i] <= 0) {
                isEnemy2Alive[i] = false;
            }
        }
    }

    public void updateEnemy2Timers(float dt) {
        for (int i = 0; i < ENEMY2_MAX_UNITS; i++) {
            if (enemy2InvulnT[i] > 0) {
                enemy2InvulnT[i] -= dt;
            }
            if (enemy2BlinkT[i] > 0) {
                enemy2BlinkT[i] -= dt;
                if (enemy2BlinkT[i] <= 0) isEnemy2Blinking[i] = false;
            }
        }
    }

    // =======================================
    // RUPEE
    // =======================================
    public boolean isRupeeAlive() { return isRupeeAlive; }
    public float getRupeeX() { return rupeeX; }
    public float getRupeeY() { return rupeeY; }
    public boolean isRupeeAnimating() { return isRupeeAnimating; }

    public void spawnRupee(float x, float y) {
        rupeeX = x;
        rupeeY = y;
        isRupeeAlive = true;
        isRupeeAnimating = true;
        rupeeAnimT = 0f;
        rupeeLandY = y;
    }

    public void despawnRupee() {
        isRupeeAlive = false;
        isRupeeAnimating = false;
    }

    public void updateRupeeAnim(float dt) {
        if (!isRupeeAnimating) return;
        rupeeAnimT += dt;
        if (rupeeAnimT >= rupeeAnimDur) {
            isRupeeAnimating = false;
            return;
        }
        float u = rupeeAnimT / rupeeAnimDur;
        float u2 = 1f - u;
        float height = u2 * u2 * rupeeHopHeight;
        rupeeY = rupeeLandY - height;
    }

    // =======================================
    // POTION
    // =======================================
    public boolean isPotionAlive() { return isPotionAlive; }
    public float getPotionX() { return potionX; }
    public float getPotionY() { return potionY; }
    public boolean isPotionAnimating() { return isPotionAnimating; }

    public void spawnPotion(float x, float y) {
        potionX = x;
        potionY = y;
        isPotionAlive = true;
        isPotionAnimating = true;
        potionAnimT = 0f;
        potionLandY = y;
    }

    public void despawnPotion() {
        isPotionAlive = false;
        isPotionAnimating = false;
    }

    public void updatePotionAnim(float dt) {
        if (!isPotionAnimating) return;
        potionAnimT += dt;
        if (potionAnimT >= potionAnimDur) {
            isPotionAnimating = false;
            return;
        }
        float u = potionAnimT / potionAnimDur;
        float u2 = 1f - u;
        float height = u2 * u2 * potionHopHeight;
        potionY = potionLandY - height;
    }

    // =======================================
    // BOSS
    // =======================================
    public float getBossX() { return bossX; }
    public float getBossY() { return bossY; }
    public boolean isBossAlive() { return isBossAlive; }
    public boolean isBossBlinking() { return isBossBlinking; }
    public float getBossBlinkT() { return bossBlinkT; }
    public Facing getBossFacing() { return bossFacing; }
    public float getBossFloatT() { return bossFloatT; }
    public float getBossShootT() { return bossShootT; }

    public void setBossPos(float x, float y) { bossX = x; bossY = y; }
    public void setBossFacing(Facing f) { bossFacing = f; }
    public void addBossFloatT(float dt) { bossFloatT += dt; }
    public void setBossShootT(float t) { bossShootT = t; }
    
    public boolean isVictoryActive() { return isVictoryActive; }
    public float getVictoryT() { return victoryT; }

    public void updateVictoryTimer(float dt) {
        if (isVictoryActive) {
            victoryT += dt;
            if (victoryT >= VICTORY_DURATION) {
                victoryT = VICTORY_DURATION;
            }
        }
    }

    public void hitBoss(int damage) {
        bossHp -= damage;
        isBossBlinking = true;
        bossBlinkT = 0.25f;
        if (bossHp <= 0) {
            isBossAlive = false;
            isVictoryActive = true;
            victoryT = 0f;
        }
    }

    public void updateBossTimers(float dt) {
        if (bossBlinkT > 0) {
            bossBlinkT -= dt;
            if (bossBlinkT <= 0) isBossBlinking = false;
        }
    }

    // =======================================
    // SHOP
    // =======================================
    public void openShop() { shopOpen = true; fireGameEvent(GameEventType.HUD_CHANGED); }
    public void closeShop() { shopOpen = false; fireGameEvent(GameEventType.HUD_CHANGED); }
    public int getShopSelectionIndex() { return shopSelectionIndex; }
    public void setShopSelectionIndex(int idx) {
        shopSelectionIndex = Math.max(0, Math.min(3, idx));
        fireGameEvent(GameEventType.HUD_CHANGED);
    }

    public String getShopMessage() { return shopMessage; }
    public float getShopMessageT() { return shopMessageT; }
    public void showShopMessage(String msg, float duration) { shopMessage = msg; shopMessageT = duration; }

    public void updateShopTimers(float dt) {
        if (shopMessageT > 0) shopMessageT -= dt;
    }

    // =======================================
    // PROFILE
    // =======================================
    public String getProfileId() { return profileId; }
    public String getProfileNickname() { return profileNickname; }
    public void setActiveProfile(String id, String nickname, String serverCode) {
        profileId = id;
        profileNickname = nickname;
    }

    // =======================================
    // INTERACT PROMPT
    // =======================================
    public void setShowInteractPrompt(boolean v) { showInteractPrompt = v; }
    public boolean shouldShowInteractPrompt() { return showInteractPrompt; }

    // =======================================
    // EVENTS
    // =======================================
    public void addListener(GameEventListener l) { listener = l; }
    public void requestRepaint() { fireGameEvent(GameEventType.PLAYER_MOVED); }

    private void fireGameEvent(GameEventType type) {
        if (listener != null) {
            listener.onGameEvent(new GameEvent(type));
        }
    }

    // =======================================
    // INNER CLASSES
    // =======================================
    public static class Arrow {
        public float x, y, vx, vy;
        public Facing facing;

        public Arrow(float x, float y, float vx, float vy, Facing facing) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.facing = facing;
        }

        public void update(float dt) {
            x += vx * dt;
            y += vy * dt;
        }
    }

    public static class BossBullet {
        public float x, y, vx, vy;

        public BossBullet(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        public void update(float dt) {
            x += vx * dt;
            y += vy * dt;
        }
    }
}
