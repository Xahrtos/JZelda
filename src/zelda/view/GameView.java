package zelda.view;

import zelda.model.GameModel;
import javax.swing.ImageIcon;
import zelda.model.Room;
import zelda.model.SlideDir;
import zelda.root.Assets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;

/**
 * View (MVC): disegna HUD + stanza NES (16x11 tile) e gestisce la slide
 * tra stanze disegnando "current room" e "next room" contemporaneamente.
 *
 * Nota: la logica di "quando cambiare stanza" è nel Controller/Model.
 * Qui ci occupiamo solo della parte grafica (render + animazione slide).
 */
public class GameView {

    private final GameModel model;

    // Stato interno animazione slide (View)
    private boolean sliding = false;
    private float slideT = 0f; // 0..1
    private static final float SLIDE_DURATION = 2.00f; // secondi

    public GameView(GameModel model) {
        this.model = model;
    }

    /**
     * Render con dt (delta time) per avanzare l'animazione slide.
     */
    public void render(Graphics2D g, int screenW, int screenH, float dt) {
        // 1) Clear schermo
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, screenW, screenH);

        // 2) HUD (sopra)
        renderHud(g, screenW);

        // 3) Playfield (stanza) sotto HUD
        int offsetY = GameModel.HUD_HEIGHT;

        // Dimensione stanza in pixel (stile NES: 16x11 tile)
        int roomPixelW = Room.COLS * GameModel.TILE_SIZE; // 512
        int roomPixelH = Room.ROWS * GameModel.TILE_SIZE; // 352

        // Centriamo orizzontalmente (come “schermata NES” dentro finestra 800x600)
        int baseX = (screenW - roomPixelW) / 2;

        // 4) Se il model è in transizione e noi non abbiamo ancora avviato l'animazione, partiamo
        if (model.isTransitioning() && !sliding) {
            sliding = true;
            slideT = 0f;
        }

        // 5) Avanza l'animazione se stiamo slidando
        if (sliding) {
            slideT += dt / SLIDE_DURATION;

            // Quando raggiunge 1, la slide è finita: chiediamo al Model di "finalizzare" il cambio stanza
            if (slideT >= 1f) {
                slideT = 1f;
                sliding = false;
                model.finishRoomTransition();
            }
        }

        // 6) Calcoliamo di quanti pixel stiamo scorrendo (0..roomPixelW)
        int slidePx = Math.round(roomPixelW * slideT);

        // 7) Decidiamo dove disegnare stanza corrente e prossima
        int currX = baseX;
        int nextX = baseX;

        boolean drawTwoRooms = model.isTransitioning() || sliding;

        if (drawTwoRooms) {
            SlideDir dir = model.getSlideDir();

            // LEFT: la nuova stanza entra da destra, quella corrente esce a sinistra
            if (dir == SlideDir.LEFT) {
                currX = baseX - slidePx;
                nextX = baseX + roomPixelW - slidePx;
            } else { // RIGHT: nuova entra da sinistra
                currX = baseX + slidePx;
                nextX = baseX - roomPixelW + slidePx;
            }

            renderRoom(g, model.getRoom(), currX, offsetY);
            renderRoom(g, model.getNextRoom(), nextX, offsetY);
        } else {
            renderRoom(g, model.getRoom(), baseX, offsetY);
        }

        // 8) Player: per semplicità lo disegniamo nella stanza "target" durante transizione,
        //    altrimenti nella stanza corrente.
        float px = model.getPlayerX();
        float py = model.getPlayerY();

        int drawW = 32;
        int drawH = 32;

        Image img = Assets.player;

        int playerBaseX = (model.isTransitioning() || sliding) ? nextX : baseX;

        g.drawImage(img,
                playerBaseX + Math.round(px),
                offsetY + Math.round(py),
                drawW, drawH, null);

        // (opzionale) bordo playfield per debug
        g.setColor(new Color(255, 255, 255, 40));
        g.drawRect(baseX, offsetY, roomPixelW, roomPixelH);
    }

    private void renderHud(Graphics2D g, int screenW) {
        g.setColor(new Color(20, 20, 20));
        g.fillRect(0, 0, screenW, GameModel.HUD_HEIGHT);
        g.drawString("Player: " + model.getProfileNickname(), 10, 40);
        String ap = model.getProfileAvatarPath();
        if (ap != null && !ap.isBlank()) {
            ImageIcon ico = new ImageIcon(ap);
            Image img = ico.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            g.drawImage(img, screenW - 60, 8, null);
        }
        g.setColor(Color.WHITE);
        g.drawString("Lives: " + model.getLives(), 10, 20);
        g.drawString("Rupees: " + model.getRupees(), 110, 20);
        g.drawString("Score: " + model.getScore(), 230, 20);
        g.drawString("Room: " + (model.getCurrentRoomIndex() + 1) + "/" + model.getRoomsCount(), 340, 20);
    }

    /**
     * Disegna una stanza in offsetX/offsetY.
     * Per ora usa colori “placeholder” (floor/muro). Poi sostituiremo con tiles grafici.
     */
    private void renderRoom(Graphics2D g, Room r, int offsetX, int offsetY) {
        for (int y = 0; y < Room.ROWS; y++) {
            for (int x = 0; x < Room.COLS; x++) {
                int v = r.getTile(x, y);

                if (v == 1) g.setColor(Color.DARK_GRAY);
                else g.setColor(new Color(20, 80, 20));

                int px = offsetX + x * GameModel.TILE_SIZE;
                int py = offsetY + y * GameModel.TILE_SIZE;

                g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);

                // griglia leggera (debug)
                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
            }
        }
    }
}