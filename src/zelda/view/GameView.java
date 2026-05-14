package zelda.view;

import zelda.model.GameModel;
import zelda.model.Room;
import zelda.model.SlideDir;
import zelda.root.Assets;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GameView extends JPanel implements zelda.model.GameEventListener {

    private final GameModel model;

    private boolean sliding = false;
    private float slideT = 0f;

    private static final float SLIDE_DURATION = 0.80f;

    private static final int PLAYER_ZOOM = 2;

    // Occluder tuning
    private static final int OCCLUDER_FRONT_STRIP_PX = 12;
    // linea di test: usare il bordo basso tile (top + 32) è ok; se vuoi che occluda "più tardi" usa 24 o 16
    private static final int OCCLUDE_LINE_OFFSET_PX = GameModel.TILE_SIZE;

    // Grid tuning
    private static final boolean DRAW_GRID = true;
    private static final Color GRID_COLOR = new Color(0, 0, 0, 18); // molto più leggera

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

    private static final class PlayerDraw {
        final int drawX, drawY, drawW, drawH;
        final int feetYScreen;
        PlayerDraw(int x, int y, int w, int h) {
            this.drawX = x; this.drawY = y; this.drawW = w; this.drawH = h;
            this.feetYScreen = y + h;
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
            }

            // 1) BASE TILES (sempre sotto)
            if (drawTwoRooms) {
                renderRoomBase(g, model.getRoom(), currX, currY);
                renderRoomBase(g, model.getNextRoom(), nextX, nextY);
            } else {
                renderRoomBase(g, model.getRoom(), baseX, baseY);
            }

            // 2) GRID SOTTO gli sprite (opzionale)
           // if (DRAW_GRID) {
               // if (drawTwoRooms) {
                  //  drawGrid(g, currX, currY);
                  //  drawGrid(g, nextX, nextY);
             //   } else {
                  //  drawGrid(g, baseX, baseY);
             //   }
         //   }

            // 3) ENTITIES (nella stanza target durante slide come fai ora)
            int roomX = drawTwoRooms ? nextX : baseX;
            int roomY = drawTwoRooms ? nextY : baseY;
            Room roomForEntities = drawTwoRooms ? model.getNextRoom() : model.getRoom();

            PlayerDraw pd = computePlayerDraw(roomX, roomY);
            drawEntitiesSorted(g, roomForEntities, roomX, roomY, pd);

            // 4) OCCLUDER FRONT STRIPS (solo tile=2, sopra entities, condizionato su feetY)
            // IMPORTANTE: qui non disegniamo mai tile pieni, solo strip.
            if (drawTwoRooms) {
                renderOccluderFrontStrips(g, model.getRoom(), currX, currY, pd.feetYScreen);
                renderOccluderFrontStrips(g, model.getNextRoom(), nextX, nextY, pd.feetYScreen);
            } else {
                renderOccluderFrontStrips(g, model.getRoom(), baseX, baseY, pd.feetYScreen);
            }

            // bordo playfield (debug)
            g.setColor(new Color(255, 255, 255, 40));
            g.drawRect(baseX, baseY, roomPixelW, roomPixelH);

        } finally {
            g.dispose();
        }
    }

    private PlayerDraw computePlayerDraw(int roomX, int roomY) {
        float px = model.getPlayerX();
        float py = model.getPlayerY();

        BufferedImage playerSprite = Assets.playerIdle[0];
        int sw = playerSprite.getWidth();
        int sh = playerSprite.getHeight();

        int w = sw * PLAYER_ZOOM;
        int h = sh * PLAYER_ZOOM;

        int x = roomX + Math.round(px) + (GameModel.TILE_SIZE - w) / 2;
        int y = roomY + Math.round(py) + GameModel.TILE_SIZE - h;

        return new PlayerDraw(x, y, w, h);
    }

    private void drawEntitiesSorted(Graphics2D g, Room room, int roomX, int roomY, PlayerDraw pd) {
        BufferedImage playerSprite = Assets.playerIdle[0];

        int merchantFeetY = Integer.MIN_VALUE;
        if (room.hasNpc() && room.getNpcBounds() != null) {
            var b = room.getNpcBounds();
            merchantFeetY = roomY + b.y + b.height;
        }

        if (merchantFeetY != Integer.MIN_VALUE && pd.feetYScreen < merchantFeetY) {
            // player dietro -> merchant davanti
            g.drawImage(playerSprite, pd.drawX, pd.drawY, pd.drawW, pd.drawH, null);
            drawMerchant(g, room, roomX, roomY);
        } else {
            // player davanti
            drawMerchant(g, room, roomX, roomY);
            g.drawImage(playerSprite, pd.drawX, pd.drawY, pd.drawW, pd.drawH, null);
        }
    }

    private void renderHud(Graphics2D g, int screenW) {
        g.setColor(new Color(20, 20, 20));
        g.fillRect(0, 0, screenW, GameModel.HUD_HEIGHT);

        g.setColor(Color.WHITE);
        g.drawString("Player: " + model.getProfileNickname(), 10, 22);
        g.drawString("Vite: " + model.getLives() + "  Rupie: " + model.getRupees() + "  Score: " + model.getScore(), 10, 44);
    }

    private void renderRoomBase(Graphics2D g, Room room, int ox, int oy) {
        for (int y = 0; y < Room.ROWS; y++) {
            for (int x = 0; x < Room.COLS; x++) {
                int t = room.getTile(x, y);

                int px = ox + x * GameModel.TILE_SIZE;
                int py = oy + y * GameModel.TILE_SIZE;

                if (t == Room.TILE_FLOOR) {
                    g.setColor(new Color(20, 80, 20));
                } else {
                    // stesso colore per SOLID e OCCLUDER
                    g.setColor(Color.DARK_GRAY);
                }
                g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
            }
        }
    }

    private void renderOccluderFrontStrips(Graphics2D g, Room room, int ox, int oy, int playerFeetYScreen) {
        int cover = Math.max(1, Math.min(GameModel.TILE_SIZE, OCCLUDER_FRONT_STRIP_PX));
        int stripYInside = GameModel.TILE_SIZE - cover;

        g.setColor(Color.DARK_GRAY);

        for (int y = 0; y < Room.ROWS; y++) {
            for (int x = 0; x < Room.COLS; x++) {
                if (!room.isOccluderTile(x, y)) continue;

                int tileTop = oy + y * GameModel.TILE_SIZE;
                int occludeLineY = tileTop + OCCLUDE_LINE_OFFSET_PX;

                // il player è dietro se i piedi sono più su della linea
                if (playerFeetYScreen < occludeLineY) {
                    int px = ox + x * GameModel.TILE_SIZE;
                    g.fillRect(px, tileTop + stripYInside, GameModel.TILE_SIZE, cover);
                }
            }
        }
    }

    //private void drawGrid(Graphics2D g, int ox, int oy) {
       // g.setColor(GRID_COLOR);
       // for (int y = 0; y < Room.ROWS; y++) {
          //  for (int x = 0; x < Room.COLS; x++) {
            //    int px = ox + x * GameModel.TILE_SIZE;
            //    int py = oy + y * GameModel.TILE_SIZE;
             //   g.drawRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
         //   }
       // }
   // }

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