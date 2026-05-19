package zelda.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Stanze:
 * - 0..7: livelli giocabili
 *   - tutte hanno porta a destra
 *   - tutte (tranne la 0) hanno porta a sinistra per tornare indietro
 *   - la 7 ha anche porta in alto per andare allo shop
 * - 8: shop
 *   - ha porta in basso per tornare alla 7
 */
public class RoomManager {

    private final List<Room> rooms = new ArrayList<>();

    public static final int PLAY_LAST_INDEX = 7;
    public static final int SHOP_INDEX = 8;

    public RoomManager() {
        // 0: solo destra
        rooms.add(makeLinearRoom(0, false, true, false, false));

        // 1..6: sinistra + destra
        for (int i = 1; i <= 6; i++) {
            rooms.add(makeLinearRoom(i, true, true, false, false));
        }

        // 7: sinistra + destra + top (shop)
        rooms.add(makeLinearRoom(7, true, true, true, false));

        // 8: shop: solo bottom per tornare a 7
        rooms.add(makeLinearRoom(8, false, false, false, true));
    }

    public Room getRoom(int index) {
        return rooms.get(index);
    }

    public int count() {
        return rooms.size();
    }

    private Room makeLinearRoom(int id, boolean leftDoor, boolean rightDoor, boolean topDoor, boolean bottomDoor) {
        Room r = new Room();

        // ---- 1) Muri di bordo ----
        for (int x = 0; x < Room.COLS; x++) {
            r.setTile(x, 0, Room.TILE_SOLID);
            r.setTile(x, Room.ROWS - 1, Room.TILE_SOLID);
        }
        for (int y = 0; y < Room.ROWS; y++) {
            r.setTile(0, y, Room.TILE_SOLID);
            r.setTile(Room.COLS - 1, y, Room.TILE_SOLID);
        }

        // ---- 2) Porte (aperture 2-tile) ----
        int midX = Room.COLS / 2; // 8
        int midY = Room.ROWS / 2; // 5

        // sinistra
        if (leftDoor) {
            r.setTile(0, midY - 1, Room.TILE_FLOOR);
            r.setTile(0, midY, Room.TILE_FLOOR);
        }

        // destra
        if (rightDoor) {
            r.setTile(Room.COLS - 1, midY - 1, Room.TILE_FLOOR);
            r.setTile(Room.COLS - 1, midY, Room.TILE_FLOOR);
        }

        // alto
        if (topDoor) {
            r.setTile(midX - 1, 0, Room.TILE_FLOOR);
            r.setTile(midX, 0, Room.TILE_FLOOR);
        }

        // basso
        if (bottomDoor) {
            r.setTile(midX - 1, Room.ROWS - 1, Room.TILE_FLOOR);
            r.setTile(midX, Room.ROWS - 1, Room.TILE_FLOOR);
        }

        // ---- 3) Ostacoli diversi per stanza ----
        int pattern = id % 4;

        // nello shop mettiamo un "bancone" (OCCLUDER) + NPC
        if (id == SHOP_INDEX) {
            int counterY = 4;
            for (int x = 4; x <= 11; x++) r.setTile(x, counterY, Room.TILE_SOLID);

            // NPC dietro il bancone (hitbox più piccola dello sprite)
            int npcW = 32;
            int npcH = 32;

            int counterLeftPx  = 4 * GameModel.TILE_SIZE;
            int counterRightPx = (11 + 1) * GameModel.TILE_SIZE;

            int npcX = (counterLeftPx + counterRightPx) / 2 - npcW / 2;

            int counterTopPx = counterY * GameModel.TILE_SIZE;
            int npcY = counterTopPx - npcH - 4;

            r.setNpc("Mercante", new java.awt.Rectangle(npcX, npcY, npcW, npcH));
            return r;
        }

        // ---- ROOM 7: arena vuota (mantiene solo perimetro + porte) ----
        if (id == PLAY_LAST_INDEX) {
            return r;
        }

        switch (pattern) {
            case 0 -> {
                int oy = 3;
                for (int x = 3; x < 13; x++) r.setTile(x, oy, Room.TILE_SOLID);
            }
            case 1 -> {
                int ox = 8;
                for (int y = 2; y < 9; y++) r.setTile(ox, y, Room.TILE_SOLID);
            }
            case 2 -> {
                for (int x = 3; x < 6; x++)
                    for (int y = 3; y < 6; y++)
                        r.setTile(x, y, Room.TILE_SOLID);

                for (int x = 10; x < 13; x++)
                    for (int y = 6; y < 9; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
            }
            case 3 -> {
                for (int i = 0; i < 6; i++) {
                    r.setTile(4 + i, 2 + (i % 2), Room.TILE_SOLID);
                    r.setTile(4 + i, 7 + (i % 2), Room.TILE_SOLID);
                }
            }
        }

        return r;
    }
}