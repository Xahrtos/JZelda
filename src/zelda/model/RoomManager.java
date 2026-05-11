package zelda.model;

import java.util.ArrayList;
import java.util.List;

public class RoomManager {

    private final List<Room> rooms = new ArrayList<>();

    public RoomManager() {
        // Crea 8 stanze “demo” (poi le personalizzi)
        for (int i = 0; i < 8; i++) {
            rooms.add(makeDemoRoom(i));
        }
    }

    public Room getRoom(int index) {
        return rooms.get(index);
    }

    public int count() {
        return rooms.size();
    }

    private Room makeDemoRoom(int id) {
        Room r = new Room();

        // Muri di bordo
        for (int x = 0; x < Room.COLS; x++) {
            r.setTile(x, 0, 1);
            r.setTile(x, Room.ROWS - 1, 1);
        }
        for (int y = 0; y < Room.ROWS; y++) {
            r.setTile(0, y, 1);
            r.setTile(Room.COLS - 1, y, 1);
        }

     // “Porte” aperte al centro dei lati (2 tile di larghezza)
        int midX = Room.COLS / 2;   // 8
        int midY = Room.ROWS / 2;   // 5

        // sopra e sotto: apriamo due tile (midX-1 e midX)
        r.setTile(midX - 1, 0, 0);
        r.setTile(midX,     0, 0);
        r.setTile(midX - 1, Room.ROWS - 1, 0);
        r.setTile(midX,     Room.ROWS - 1, 0);

        // sinistra e destra: apriamo due tile (midY-1 e midY)
        r.setTile(0, midY - 1, 0);
        r.setTile(0, midY,     0);
        r.setTile(Room.COLS - 1, midY - 1, 0);
        r.setTile(Room.COLS - 1, midY,     0);
        // sopra e sotto
        r.setTile(midX, 0, 0);
        r.setTile(midX, Room.ROWS - 1, 0);

        // sinistra e destra
        r.setTile(0, midY, 0);
        r.setTile(Room.COLS - 1, midY, 0);

        // Ostacolo diverso per stanza (così vedi che cambiano)
        int ox = 3 + (id % 10);
        int oy = 3 + (id % 4);
        for (int x = ox; x < Math.min(ox + 6, Room.COLS - 1); x++) {
            r.setTile(x, oy, 1);
        }

        return r;
    }
}