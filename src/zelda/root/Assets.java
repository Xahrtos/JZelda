package zelda.root;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;


public class Assets {

	public static final int DIR_DOWN = 0;
	public static final int DIR_LEFT = 1;
	public static final int DIR_RIGHT = 2;
	public static final int DIR_UP = 3;

    // PROVA: frame 32x32
    public static final int FRAME_W = 32;
    public static final int FRAME_H = 32;

    public static BufferedImage player;
    public static BufferedImage sheet;

    public static void load() {
        sheet = loadImage("sprites/player_sheet.png");

        final int baseX = 0; // colonna frame dove inizia la riga del player
        final int baseY = 0; // riga frame dove sta il player

        // per ora mettiamo tutti uguali; poi li cambiamo quando identifichiamo i frame esatti
        player = frame(sheet, baseX, baseY);

        playerIdle[DIR_DOWN] = player;
        playerIdle[DIR_LEFT] = player;
        playerIdle[DIR_RIGHT] = player;
        playerIdle[DIR_UP] = player;
    }

    public static BufferedImage[] playerIdle = new BufferedImage[4];
    
    public static BufferedImage frame(BufferedImage sheet, int frameX, int frameY) {
        return sheet.getSubimage(frameX * FRAME_W, frameY * FRAME_H, FRAME_W, FRAME_H);
    }

    private static BufferedImage loadImage(String pathInsideAssets) {
        String fullPath = "/" + pathInsideAssets;
        try {
            var url = Assets.class.getResource(fullPath);
            if (url == null) {
                throw new RuntimeException(
                        "Immagine non trovata nel classpath: " + fullPath + "\n" +
                        "Controlla che il file sia in src/assets/sprites/ e che src/assets sia Source Folder."
                );
            }
            return ImageIO.read(url);
        } catch (IOException e) {
            throw new RuntimeException("Errore caricando immagine: " + fullPath, e);
        }
    }
}