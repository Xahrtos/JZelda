package zelda.model;

public class Room {

    public static final int COLS = 16;
    public static final int ROWS = 11;

    // 0 = floor, 1 = wall
    private final int[][] tiles = new int[ROWS][COLS];

    public Room() {
        // di default tutto floor
    }

    public void setTile(int x, int y, int v) {
        tiles[y][x] = v;
    }

    public int getTile(int x, int y) {
        return tiles[y][x];
    }

    public boolean isSolidTile(int x, int y) {
        // fuori stanza = solido (così non esci se non dalle “porte” gestite dal controller)
        if (x < 0 || y < 0 || x >= COLS || y >= ROWS) return true;
        return tiles[y][x] == 1;
    }
}