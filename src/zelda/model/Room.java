package zelda.model;

import java.awt.Rectangle;

public class Room {

    public static final int COLS = 16;
    public static final int ROWS = 11;

    // 0 = floor, 1 = wall
    private final int[][] tiles = new int[ROWS][COLS];

    // ---- Shop NPC (semplice) ----
    private Rectangle npcBounds;     // in pixel, coordinate stanza (non schermo)
    private String npcName;

    public Room() {}

    public void setTile(int x, int y, int v) {
        tiles[y][x] = v;
    }

    public int getTile(int x, int y) {
        return tiles[y][x];
    }

    public boolean isSolidTile(int x, int y) {
        if (x < 0 || y < 0 || x >= COLS || y >= ROWS) return true;
        return tiles[y][x] == 1;
    }

    // ---- NPC API ----
    public void setNpc(String name, Rectangle boundsPx) {
        this.npcName = name;
        this.npcBounds = boundsPx;
    }

    public Rectangle getNpcBounds() {
        return npcBounds;
    }

    public String getNpcName() {
        return npcName;
    }

    public boolean hasNpc() {
        return npcBounds != null;
    }
}