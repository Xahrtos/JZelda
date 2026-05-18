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

    private static final float SLIDE_DURATION = 0.80f;

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
                    } else {
                        currX = baseX + slidePx;
                        nextX = baseX - roomPixelW + slidePx;
                    }
                } else {
                    int slidePx = Math.round(roomPixelH * slideT);
                    if (dir == SlideDir.UP) {
                        currY = baseY - slidePx;
                        nextY = baseY + roomPixelH - slidePx;
                    } else {
                        currY = baseY + slidePx;
                        nextY = baseY - roomPixelH + slidePx;
                    }
                }

                // ---- 1) FLOOR LAYER (sotto) ----
                renderRoomFloor(g, model.getRoom(), currX, currY);
                renderRoomFloor(g, model.getNextRoom(), nextX, nextY);
            } else {
                // ---- 1) FLOOR LAYER (sotto) ----
                renderRoomFloor(g, model.getRoom(), baseX, baseY);
            }

            // ---- 2) ENTITIES (player + npc) ----
            // Il player lo disegniamo nella stanza target durante slide (come stavi facendo)
            int roomX = drawTwoRooms ? nextX : baseX;
            int roomY = drawTwoRooms ? nextY : baseY;
            Room roomForEntities = drawTwoRooms ? model.getNextRoom() : model.getRoom();

            float px = model.getPlayerX();
            float py = model.getPlayerY();

            BufferedImage playerSprite = Assets.playerIdle[0];
            int sw = playerSprite.getWidth();
            int sh = playerSprite.getHeight();

            int pDrawW = sw * PLAYER_ZOOM;
            int pDrawH = sh * PLAYER_ZOOM;

            int pDrawX = roomX + Math.round(px) + (GameModel.TILE_SIZE - pDrawW) / 2;
            int pDrawY = roomY + Math.round(py) + GameModel.TILE_SIZE - pDrawH;

            int playerFeetY = pDrawY + pDrawH;

            int merchantFeetY = Integer.MIN_VALUE;
            if (roomForEntities.hasNpc() && roomForEntities.getNpcBounds() != null) {
                var b = roomForEntities.getNpcBounds();
                merchantFeetY = roomY + b.y + b.height;
            }

            if (merchantFeetY != Integer.MIN_VALUE && playerFeetY < merchantFeetY) {
                // player dietro, merchant davanti
                g.drawImage(playerSprite, pDrawX, pDrawY, pDrawW, pDrawH, null);
                drawMerchant(g, roomForEntities, roomX, roomY);
            } else {
                // player davanti (o nessun npc)
                drawMerchant(g, roomForEntities, roomX, roomY);
                g.drawImage(playerSprite, pDrawX, pDrawY, pDrawW, pDrawH, null);
            }

            // ---- 3) SOLIDS / FOREGROUND LAYER (sopra entities) ----
            if (drawTwoRooms) {
                renderRoomSolids(g, model.getRoom(), currX, currY);
                renderRoomSolids(g, model.getNextRoom(), nextX, nextY);
            } else {
                renderRoomSolids(g, model.getRoom(), baseX, baseY);
            }

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

    // ---- LAYER 1: floor only (tile == 0) ----
    private void renderRoomFloor(Graphics2D g, Room room, int ox, int oy) {
        for (int y = 0; y < Room.ROWS; y++) {
            for (int x = 0; x < Room.COLS; x++) {
                int t = room.getTile(x, y);
                if (t != 0) continue;

                g.setColor(new Color(20, 80, 20));

                int px = ox + x * GameModel.TILE_SIZE;
                int py = oy + y * GameModel.TILE_SIZE;

                g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);

                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
            }
        }
    }

    // ---- LAYER 3: solids/foreground only (tile == 1) ----
    private void renderRoomSolids(Graphics2D g, Room room, int ox, int oy) {
        for (int y = 0; y < Room.ROWS; y++) {
            for (int x = 0; x < Room.COLS; x++) {
                int t = room.getTile(x, y);
                if (t != 1) continue;

                g.setColor(Color.DARK_GRAY);

                int px = ox + x * GameModel.TILE_SIZE;
                int py = oy + y * GameModel.TILE_SIZE;

                g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);

                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
            }
        }
    }

    private void drawMerchant(Graphics2D g, Room room, int roomX, int roomY) {
        if (!room.hasNpc() || Assets.merchant == null) return;

        var b = room.getNpcBounds();

        int nx = roomX + b.x;
        int ny = roomY + b.y;

        int mw = Assets.merchant.getWidth();
        int mh = Assets.merchant.getHeight();

        int sx = nx + (b.width - mw) / 2;
        int sy = ny + (b.height - mh) / 2;

        g.drawImage(Assets.merchant, sx, sy, mw, mh, null);
    }
}