package zelda.model;

import java.util.ArrayList;
import java.util.List;
import zelda.model.GameModel;

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
            r.setTile(x, 0, 1);
            r.setTile(x, Room.ROWS - 1, 1);
        }
        for (int y = 0; y < Room.ROWS; y++) {
            r.setTile(0, y, 1);
            r.setTile(Room.COLS - 1, y, 1);
        }

        // ---- 2) Porte (aperture 2-tile) ----
        int midX = Room.COLS / 2; // 8
        int midY = Room.ROWS / 2; // 5

        // sinistra
        if (leftDoor) {
            r.setTile(0, midY - 1, 0);
            r.setTile(0, midY, 0);
        }

        // destra
        if (rightDoor) {
            r.setTile(Room.COLS - 1, midY - 1, 0);
            r.setTile(Room.COLS - 1, midY, 0);
        }

        // alto
        if (topDoor) {
            r.setTile(midX - 1, 0, 0);
            r.setTile(midX, 0, 0);
        }

        // basso
        if (bottomDoor) {
            r.setTile(midX - 1, Room.ROWS - 1, 0);
            r.setTile(midX, Room.ROWS - 1, 0);
        }

        // ---- 3) Ostacoli diversi per stanza ----
        // (semplici pattern deterministici: bastano per far vedere che sono diverse)
        int pattern = id % 4;

        // nello shop mettiamo un "bancone" e basta
        if (id == SHOP_INDEX) {
            int y = 4;
            for (int x = 4; x <= 11; x++) r.setTile(x, y, 1);
         

           

         // NPC al centro (in pixel, coordinate stanza)
            int npcW = 24;
            int npcH = 24;
            int npcX = (Room.COLS * GameModel.TILE_SIZE) / 2 - npcW / 2; // serve import GameModel
            int npcY = (Room.ROWS * GameModel.TILE_SIZE) / 2 - npcH / 2;

             r.setNpc("Mercante", new java.awt.Rectangle(npcX, npcY, npcW, npcH));
               
         
            return r;
        }

        switch (pattern) {
            case 0 -> {
                // barra orizzontale
                int oy = 3;
                for (int x = 3; x < 13; x++) r.setTile(x, oy, 1);
            }
            case 1 -> {
                // colonna verticale
                int ox = 8;
                for (int y = 2; y < 9; y++) r.setTile(ox, y, 1);
            }
            case 2 -> {
                // due blocchi
                for (int x = 3; x < 6; x++)
                    for (int y = 3; y < 6; y++)
                        r.setTile(x, y, 1);

                for (int x = 10; x < 13; x++)
                    for (int y = 6; y < 9; y++)
                        r.setTile(x, y, 1);
            }
            case 3 -> {
                // zig-zag
                for (int i = 0; i < 6; i++) {
                    r.setTile(4 + i, 2 + (i % 2), 1);
                    r.setTile(4 + i, 7 + (i % 2), 1);
                }
            }
        }

        return r;
    }
}