package zelda.model;

import java.util.ArrayList;
import java.util.List;

public class RoomManager {

    private static RoomManager instance;
    private final List<Room> rooms = new ArrayList<>();

    public static final int PLAY_LAST_INDEX = 7;
    public static final int SHOP_INDEX = 8;

    // stanza sotto lo shop (arena)
    public static final int SHOP_ROOM_INDEX = 4;

    private RoomManager() {
        rooms.add(makeLinearRoom(0, false, true, false, false));

        for (int i = 1; i <= 6; i++) {
            boolean topDoor = (i == SHOP_ROOM_INDEX);
            rooms.add(makeLinearRoom(i, true, true, topDoor, false));
        }

        // stanza 7: boss room (vuota, porta in alto gestita dalla logica boss)
        rooms.add(makeBossRoom7());

        // shop
        rooms.add(makeLinearRoom(8, false, false, false, true));
    }

    public static RoomManager getInstance() {
        if (instance == null) {
            instance = new RoomManager();
        }
        return instance;
    }

    public Room getRoom(int index) {
        return rooms.get(index);
    }

    public int count() {
        return rooms.size();
    }

    private Room makeBossRoom7() {
        Room r = new Room();

        // perimetro solido
        for (int x = 0; x < Room.COLS; x++) {
            r.setTile(x, 0, Room.TILE_SOLID);
            r.setTile(x, Room.ROWS - 1, Room.TILE_SOLID);
        }
        for (int y = 0; y < Room.ROWS; y++) {
            r.setTile(0, y, Room.TILE_SOLID);
            r.setTile(Room.COLS - 1, y, Room.TILE_SOLID);
        }

        // porte laterali per arrivare
        int midY = Room.ROWS / 2;
        r.setTile(0, midY - 1, Room.TILE_FLOOR);
        r.setTile(0, midY, Room.TILE_FLOOR);

        // porta top-center: inizialmente CHIUSA
        int midX = Room.COLS / 2;
        r.setTile(midX, 0, Room.TILE_SOLID);

        return r;
    }

    private Room makeLinearRoom(int id, boolean leftDoor, boolean rightDoor, boolean topDoor, boolean bottomDoor) {
        Room r = new Room();

        for (int x = 0; x < Room.COLS; x++) {
            r.setTile(x, 0, Room.TILE_SOLID);
            r.setTile(x, Room.ROWS - 1, Room.TILE_SOLID);
        }
        for (int y = 0; y < Room.ROWS; y++) {
            r.setTile(0, y, Room.TILE_SOLID);
            r.setTile(Room.COLS - 1, y, Room.TILE_SOLID);
        }

        int midX = Room.COLS / 2;
        int midY = Room.ROWS / 2;

        if (leftDoor) {
            r.setTile(0, midY - 1, Room.TILE_FLOOR);
            r.setTile(0, midY, Room.TILE_FLOOR);
        }

        if (rightDoor) {
            r.setTile(Room.COLS - 1, midY - 1, Room.TILE_FLOOR);
            r.setTile(Room.COLS - 1, midY, Room.TILE_FLOOR);
        }

        if (topDoor) {
            r.setTile(midX - 1, 0, Room.TILE_FLOOR);
            r.setTile(midX, 0, Room.TILE_FLOOR);
        }

        if (bottomDoor) {
            r.setTile(midX - 1, Room.ROWS - 1, Room.TILE_FLOOR);
            r.setTile(midX, Room.ROWS - 1, Room.TILE_FLOOR);
        }

        int pattern = id % 4;

        if (id == SHOP_INDEX) {
            int counterY = 4;
            for (int x = 4; x <= 11; x++) r.setTile(x, counterY, Room.TILE_SOLID);

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

        // arena room vuota (solo perimetro)
        if (id == SHOP_ROOM_INDEX) return r;

        // Gruppi di 4 tile (2x2) con variazioni per stanza
        switch (pattern) {
            case 0 -> {
                // Pattern 0: due blocchi 2x2 in alto
                for (int x = 3; x < 5; x++)
                    for (int y = 2; y < 4; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
                
                for (int x = 11; x < 13; x++)
                    for (int y = 2; y < 4; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
            }
            case 1 -> {
                // Pattern 1: due blocchi 2x2 al centro (verticalmente spostati)
                for (int x = 5; x < 7; x++)
                    for (int y = 3; y < 5; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
                
                for (int x = 9; x < 11; x++)
                    for (int y = 6; y < 8; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
            }
            case 2 -> {
                // Pattern 2: tre blocchi 2x2 (uno al centro, due ai lati)
                for (int x = 7; x < 9; x++)
                    for (int y = 4; y < 6; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
                
                for (int x = 2; x < 4; x++)
                    for (int y = 6; y < 8; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
                
                for (int x = 12; x < 14; x++)
                    for (int y = 5; y < 7; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
            }
            case 3 -> {
                // Pattern 3: due blocchi 2x2 diagonali
                for (int x = 4; x < 6; x++)
                    for (int y = 3; y < 5; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
                
                for (int x = 10; x < 12; x++)
                    for (int y = 6; y < 8; y++)
                        r.setTile(x, y, Room.TILE_SOLID);
            }
        }

        return r;
    }
}