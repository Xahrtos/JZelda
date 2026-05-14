package zelda.model;

/**
 * Model principale del gioco (MVC).
 * Contiene:
 * - HUD (vite, rupie, score + profilo attivo)
 * - Player position (in pixel dentro la stanza)
 * - Gestione stanze (8 stanze lineari)
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

    // ---- ROOM API ----

    /** Stanza corrente (quella “ufficiale” durante gioco normale) */
    public Room getRoom() {
        return roomManager.getRoom(currentRoomIndex);
    }

    /** Stanza target durante una transizione (quella che sta entrando) */
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
    private String profileId = "";

    public String getProfileId() {
        return profileId;
    }
    
    public int getNextRoomIndex() {
        return nextRoomIndex;
    }

    /**
     * Inizia transizione verso un'altra stanza.
     * Non cambiamo subito currentRoomIndex: la View disegna current+next in slide.
     */
    public void beginRoomTransition(int targetRoomIndex, SlideDir dir) {
        if (transitioning) return;
        if (targetRoomIndex < 0 || targetRoomIndex >= roomManager.count()) return;

        transitioning = true;
        slideDir = dir;
        nextRoomIndex = targetRoomIndex;

        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
    }

    /**
     * Conclude la transizione: la stanza corrente diventa quella target.
     * Chiamato dalla View quando l'animazione slide arriva a fine.
     */
    public void finishRoomTransition() {
        if (!transitioning) return;

        currentRoomIndex = nextRoomIndex;
        transitioning = false;

        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
    }

    // ---- PLAYER API ----

    public float getPlayerX() {
        return playerX;
    }

    public float getPlayerY() {
        return playerY;
    }

    /**
     * Sposta il player (chiamato dal Controller dopo collisioni).
     */
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
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    public void addScore(int amount) {
        score += amount;
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

    /**
     * Imposta i dati del profilo attivo (mostrati nell'HUD).
     * Chiamato tipicamente da GamePanel quando premi "Gioca" dopo selezione profilo.
     */
    public void setActiveProfile(String id, String nickname, String avatarPath) {
        this.profileId = (id == null) ? "" : id.trim();
        this.profileNickname = (nickname == null || nickname.isBlank()) ? "Player" : nickname.trim();
        this.profileAvatarPath = (avatarPath == null) ? "" : avatarPath.trim();
        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
    }

    /**
     * Reset minimale run (utile per "nuova partita"/"continua" più avanti).
     * NON tocca il profilo attivo: quello resta selezionato.
     */
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

        fireEvent(new GameEvent(GameEventType.HUD_CHANGED));
        fireEvent(new GameEvent(GameEventType.ROOM_CHANGED));
        fireEvent(new GameEvent(GameEventType.PLAYER_MOVED));
    }
}