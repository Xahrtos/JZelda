package zelda.root;

import java.awt.Color;
import java.awt.Graphics2D;


public class World {

    public static final int TILE_SIZE = 32;

    private final int cols = 60;
    private final int rows = 40;

    // 0 = vuoto, 1 = muro
    private final int[][] tiles = new int[rows][cols];

    public World() {
        // bordo muro
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                boolean border = (x == 0 || y == 0 || x == cols - 1 || y == rows - 1);
                tiles[y][x] = border ? 1 : 0;
            }
        }

        // ostacoli demo
        for (int x = 10; x < 30; x++) tiles[10][x] = 1;
        for (int y = 12; y < 25; y++) tiles[y][20] = 1;
    }

    public boolean isSolidAtPixel(float px, float py) {
        int tx = (int) (px / TILE_SIZE);
        int ty = (int) (py / TILE_SIZE);

        if (tx < 0 || ty < 0 || tx >= cols || ty >= rows) return true;
        return tiles[ty][tx] == 1;
    }

    public void render(Graphics2D g) {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (tiles[y][x] == 1) g.setColor(Color.DARK_GRAY);
                else g.setColor(new Color(20, 80, 20));

                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }
}