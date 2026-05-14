package zelda.view;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

import zelda.model.GameModel;
import zelda.model.Room;
import zelda.model.SlideDir;
import zelda.root.Assets;

public class GameView extends JPanel implements zelda.model.GameEventListener {

    private final GameModel model;

    private boolean sliding = false;
    private float slideT = 0f;

    // più basso = più veloce
    private static final float SLIDE_DURATION = 0.80f;

    // ---- PLAYER ZOOM ----
    // 1 = normale, 2 = x2, 3 = x3 ...
    private static final int PLAYER_ZOOM = 2;

    public GameView(GameModel model) {
        this.model = model;
        setBackground(Color.BLACK);
        setFocusable(true);
        model.addListener(this);
    }

    @Override
    public void onGameEvent(zelda.model.GameEvent event) {
        if (event.type() == zelda.model.GameEventType.ROOM_CHANGED) {
            if (model.isTransitioning()) {
                sliding = true;
                slideT = 0f;
            }
            repaint();
            return;
        }

        if (event.type() == zelda.model.GameEventType.PLAYER_MOVED ||
            event.type() == zelda.model.GameEventType.HUD_CHANGED) {
            repaint();
        }
    }

    public void tick(float dt) {
        if (sliding) {
            slideT += dt / SLIDE_DURATION;

            if (slideT >= 1f) {
                slideT = 1f;
                sliding = false;
                model.finishRoomTransition();
            }
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);

        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            int screenW = getWidth();

            renderHud(g, screenW);

            int offsetY = GameModel.HUD_HEIGHT;

            int roomPixelW = Room.COLS * GameModel.TILE_SIZE;
            int roomPixelH = Room.ROWS * GameModel.TILE_SIZE;

            int baseX = (screenW - roomPixelW) / 2;
            int baseY = offsetY;

            boolean drawTwoRooms = model.isTransitioning() || sliding;

            int currX = baseX, currY = baseY;
            int nextX = baseX, nextY = baseY;

            if (drawTwoRooms) {
                SlideDir dir = model.getSlideDir();

                if (dir == SlideDir.LEFT || dir == SlideDir.RIGHT) {
                    int slidePx = Math.round(roomPixelW * slideT);
                    if (dir == SlideDir.LEFT) {
                        currX = baseX - slidePx;
                        nextX = baseX + roomPixelW - slidePx;
                    } else { // RIGHT
                        currX = baseX + slidePx;
                        nextX = baseX - roomPixelW + slidePx;
                    }
                } else {
                    int slidePx = Math.round(roomPixelH * slideT);
                    if (dir == SlideDir.UP) {
                        currY = baseY - slidePx;
                        nextY = baseY + roomPixelH - slidePx;
                    } else { // DOWN
                        currY = baseY + slidePx;
                        nextY = baseY - roomPixelH + slidePx;
                    }
                }

                renderRoom(g, model.getRoom(), currX, currY);
                renderRoom(g, model.getNextRoom(), nextX, nextY);
            } else {
                renderRoom(g, model.getRoom(), baseX, baseY);
            }

            // ---- PLAYER ----
            float px = model.getPlayerX();
            float py = model.getPlayerY();

            // Per ora sempre idle down
            BufferedImage sprite = Assets.playerIdle[0];

            int sw = sprite.getWidth();   // es. 18
            int sh = sprite.getHeight();  // es. 22

            int drawW = sw * PLAYER_ZOOM;
            int drawH = sh * PLAYER_ZOOM;

            int playerBaseX = drawTwoRooms ? nextX : baseX;
            int playerBaseY = drawTwoRooms ? nextY : baseY;

            // Centra orizzontalmente rispetto alla tile e appoggia i "piedi" a fondo tile
            int drawX = playerBaseX + Math.round(px) + (GameModel.TILE_SIZE - drawW) / 2;
            int drawY = playerBaseY + Math.round(py) + GameModel.TILE_SIZE - drawH;

            g.drawImage(sprite, drawX, drawY, drawW, drawH, null);

            // bordo playfield (debug)
            g.setColor(new Color(255, 255, 255, 40));
            g.drawRect(baseX, baseY, roomPixelW, roomPixelH);

        } finally {
            g.dispose();
        }
    }

    private void renderHud(Graphics2D g, int screenW) {
        g.setColor(new Color(20, 20, 20));
        g.fillRect(0, 0, screenW, GameModel.HUD_HEIGHT);

        g.setColor(Color.WHITE);
        g.drawString("Player: " + model.getProfileNickname(), 10, 22);
        g.drawString("Vite: " + model.getLives() + "  Rupie: " + model.getRupees() + "  Score: " + model.getScore(), 10, 44);
    }

    private void renderRoom(Graphics2D g, Room room, int ox, int oy) {
        for (int y = 0; y < Room.ROWS; y++) {
            for (int x = 0; x < Room.COLS; x++) {
                int t = room.getTile(x, y);
                g.setColor(t == 1 ? Color.DARK_GRAY : new Color(20, 80, 20));

                int px = ox + x * GameModel.TILE_SIZE;
                int py = oy + y * GameModel.TILE_SIZE;

                g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);

                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
            }
        }

        // ---- NPC Merchant ----
        if (room.hasNpc()) {
            var b = room.getNpcBounds();
            int nx = ox + b.x;
            int ny = oy + b.y;

            

            
            if (Assets.merchant != null) {
                int mw = Assets.merchant.getWidth();
                int mh = Assets.merchant.getHeight();

                // Se vuoi anche lo zoom del merchant:
                int merchantZoom = 1; // metti 2 se lo vuoi più grande
                int drawW = mw * merchantZoom;
                int drawH = mh * merchantZoom;

                int sx = nx + (b.width - drawW) / 2;
                int sy = ny + (b.height - drawH) / 2;

                g.drawImage(Assets.merchant, sx, sy, drawW, drawH, null);
            } else {
                // fallback
                g.setColor(new Color(200, 180, 60));
                g.fillRect(nx, ny, b.width, b.height);
                g.setColor(Color.BLACK);
                g.drawRect(nx, ny, b.width, b.height);
            }
        }
    }
}