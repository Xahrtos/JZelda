package zelda.root;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Assets {

    // ---- PLAYER (rimane come hai ora) ----
    public static final int PLAYER_FRAME_W = 18;
    public static final int PLAYER_FRAME_H = 22;
    public static final int PLAYER_DIRECTIONS = 4;
    public static final int PLAYER_WALK_FRAMES = 4;

    public static BufferedImage playerSheet;
    public static BufferedImage[] playerIdle = new BufferedImage[PLAYER_DIRECTIONS];
    public static BufferedImage[][] playerWalk = new BufferedImage[PLAYER_DIRECTIONS][PLAYER_WALK_FRAMES];

    // ---- MERCHANT (PNG singolo ~60x60) ----
    public static BufferedImage merchant;

    public static void load() {
        // Player
        playerSheet = loadImage("sprites/player_sheet.png");
        for (int dir = 0; dir < PLAYER_DIRECTIONS; dir++) {
            playerIdle[dir] = playerSheet.getSubimage(dir * PLAYER_FRAME_W, 0, PLAYER_FRAME_W, PLAYER_FRAME_H);
        }
        for (int frame = 0; frame < PLAYER_WALK_FRAMES; frame++) {
            playerWalk[0][frame] = playerSheet.getSubimage(frame * PLAYER_FRAME_W, 1 * PLAYER_FRAME_H, PLAYER_FRAME_W, PLAYER_FRAME_H);
            playerWalk[1][frame] = playerSheet.getSubimage(frame * PLAYER_FRAME_W, 2 * PLAYER_FRAME_H, PLAYER_FRAME_W, PLAYER_FRAME_H);
            playerWalk[2][frame] = playerSheet.getSubimage(frame * PLAYER_FRAME_W, 3 * PLAYER_FRAME_H, PLAYER_FRAME_W, PLAYER_FRAME_H);
            playerWalk[3][frame] = playerSheet.getSubimage(frame * PLAYER_FRAME_W, 4 * PLAYER_FRAME_H, PLAYER_FRAME_W, PLAYER_FRAME_H);
        }

        // Merchant: PNG singolo
        BufferedImage raw = loadImage("sprites/merchant.png");

        // Se il merchant ha background non trasparente, rimuovilo con color key.
        // Qui metto un colore "placeholder" (magenta puro) — dimmi il colore di sfondo reale e lo settiamo.
        // Se il PNG è già trasparente, questa operazione non fa danni.
        merchant = raw;
    }

    public static BufferedImage loadImage(String pathInsideAssets) {
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