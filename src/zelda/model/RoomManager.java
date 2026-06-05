package zelda.model;

import java.util.ArrayList;
import java.util.List;

public class RoomManager {

    private final List<Room> rooms = new ArrayList<>();

    public static final int PLAY_LAST_INDEX = 7;
    public static final int SHOP_INDEX = 8;

    // stanza sotto lo shop (arena)
    public static final int SHOP_ROOM_INDEX = 4;

    public RoomManager() {
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

        // porte laterali per arrivare (puoi cambiare se vuoi)
        int midY = Room.ROWS / 2;
        r.setTile(0, midY - 1, Room.TILE_FLOOR);
        r.setTile(0, midY, Room.TILE_FLOOR);

        r.setTile(Room.COLS - 1, midY - 1, Room.TILE_FLOOR);
        r.setTile(Room.COLS - 1, midY, Room.TILE_FLOOR);

        // porta top-center: inizialmente CHIUSA => lasciamo SOLID
        // (verrà aperta dal GameModel quando il boss muore)
        // midX definito comunque qui per chiarezza:
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