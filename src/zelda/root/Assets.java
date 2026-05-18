package zelda.root;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Assets {

    // 0=DOWN, 1=UP, 2=LEFT, 3=RIGHT
    public static final int DIR_DOWN = 0;
    public static final int DIR_UP = 1;
    public static final int DIR_LEFT = 2;
    public static final int DIR_RIGHT = 3;

    public static final int PLAYER_DIRECTIONS = 4;
    public static final int PLAYER_WALK_FRAMES = 2;

    public static BufferedImage[] playerIdle = new BufferedImage[PLAYER_DIRECTIONS];
    public static BufferedImage[][] playerWalk = new BufferedImage[PLAYER_DIRECTIONS][PLAYER_WALK_FRAMES];

    public static BufferedImage merchant;

    public static void load() {
        // ---- IDLE ----
        BufferedImage idleDown = loadImage("sprites/idle_down.png");
        BufferedImage idleUp = loadImage("sprites/idle_up.png");
        BufferedImage idleLR = loadImage("sprites/idle_leftright.png"); // BASE = LEFT (come richiesto)

        playerIdle[DIR_DOWN] = idleDown;
        playerIdle[DIR_UP] = idleUp;

        // INVERTITO: file -> LEFT, mirror -> RIGHT
        playerIdle[DIR_LEFT] = idleLR;
        playerIdle[DIR_RIGHT] = mirrorHorizontally(idleLR);

        // ---- WALK ----
        playerWalk[DIR_DOWN][0] = loadImage("sprites/move_down1.png");
        playerWalk[DIR_DOWN][1] = loadImage("sprites/move_down2.png");

        playerWalk[DIR_UP][0] = loadImage("sprites/move_up1.png");
        playerWalk[DIR_UP][1] = loadImage("sprites/move_up2.png");

        // BASE = LEFT (come richiesto), mirror -> RIGHT
        BufferedImage moveL1 = loadImage("sprites/move_leftright1.png");
        BufferedImage moveL2 = loadImage("sprites/move_leftright2.png");

        playerWalk[DIR_LEFT][0] = moveL1;
        playerWalk[DIR_LEFT][1] = moveL2;

        playerWalk[DIR_RIGHT][0] = mirrorHorizontally(moveL1);
        playerWalk[DIR_RIGHT][1] = mirrorHorizontally(moveL2);

        // ---- MERCHANT ----
        merchant = loadImage("sprites/merchant.png");
    }

    private static BufferedImage mirrorHorizontally(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            AffineTransform at = new AffineTransform();
            at.scale(-1, 1);
            at.translate(-src.getWidth(), 0);
            g.drawImage(src, at, null);
        } finally {
            g.dispose();
        }
        return dst;
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

            BufferedImage raw = ImageIO.read(url);
            if (raw == null) throw new RuntimeException("Immagine non leggibile: " + fullPath);

            BufferedImage argb = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = argb.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(raw, 0, 0, null);
            } finally {
                g.dispose();
            }
            return argb;

        } catch (IOException e) {
            throw new RuntimeException("Errore caricando immagine: " + fullPath, e);
        }
    }
}