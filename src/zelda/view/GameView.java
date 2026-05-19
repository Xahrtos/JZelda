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
    private static final int PLAYER_ZOOM = 2;

    // ---- ENEMY RENDER ----
    private static final int ENEMY_ROOM_INDEX = 7;
    private static final float ENEMY_SCALE = 0.5f; // 56x58 -> 28x29

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
            event.type() == zelda.model.GameEventType.HUD_CHANGED ||
            event.type() == zelda.model.GameEventType.GAME_OVER) {
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
            }

            // ---- 1) FLOOR LAYER ----
            if (drawTwoRooms) {
                renderRoomFloor(g, model.getRoom(), currX, currY);
                renderRoomFloor(g, model.getNextRoom(), nextX, nextY);
            } else {
                renderRoomFloor(g, model.getRoom(), baseX, baseY);
            }

            // ---- 2) ENTITIES ----
            int roomX = drawTwoRooms ? nextX : baseX;
            int roomY = drawTwoRooms ? nextY : baseY;
            Room roomForEntities = drawTwoRooms ? model.getNextRoom() : model.getRoom();

            float px = model.getPlayerX();
            float py = model.getPlayerY();

            int pDrawX = roomX + Math.round(px);
            int pDrawY = roomY + Math.round(py);

            // player sprite normal
            BufferedImage playerSprite = getPlayerSprite();

            // dimensioni draw del player (in base allo sprite scelto)
            int sw = playerSprite.getWidth();
            int sh = playerSprite.getHeight();
            int pDrawW = sw * PLAYER_ZOOM;
            int pDrawH = sh * PLAYER_ZOOM;

            // se game over, sostituisci sprite con death (24x15)
            boolean gameOver = model.getLives() <= 0;
            if (gameOver && Assets.playerDeath != null) {
                playerSprite = Assets.playerDeath;
                sw = playerSprite.getWidth();   // 24
                sh = playerSprite.getHeight();  // 15
                pDrawW = sw * PLAYER_ZOOM;
                pDrawH = sh * PLAYER_ZOOM;

                // centra lo sprite di morte rispetto al player
                int normalW = 32 * PLAYER_ZOOM;
                int normalH = 32 * PLAYER_ZOOM;
                pDrawX = pDrawX + (normalW - pDrawW) / 2;
                pDrawY = pDrawY + (normalH - pDrawH) / 2;
            }

            int playerFeetY = pDrawY + pDrawH;

            int merchantFeetY = Integer.MIN_VALUE;
            if (roomForEntities.hasNpc() && roomForEntities.getNpcBounds() != null) {
                var b = roomForEntities.getNpcBounds();
                merchantFeetY = roomY + b.y + b.height;
            }

            // Draw order: enemy + rupee (sotto)
            drawEnemy(g, roomX, roomY);
            drawRupee(g, roomX, roomY);

            // disegno player con blink se necessario
            if (merchantFeetY != Integer.MIN_VALUE && playerFeetY < merchantFeetY) {
                drawPlayerWithBlink(g, playerSprite, pDrawX, pDrawY, pDrawW, pDrawH);
                drawMerchant(g, roomForEntities, roomX, roomY);
            } else {
                drawMerchant(g, roomForEntities, roomX, roomY);
                drawPlayerWithBlink(g, playerSprite, pDrawX, pDrawY, pDrawW, pDrawH);
            }

            // ---- 3) SOLIDS LAYER ----
            if (drawTwoRooms) {
                renderRoomSolids(g, model.getRoom(), currX, currY);
                renderRoomSolids(g, model.getNextRoom(), nextX, nextY);
            } else {
                renderRoomSolids(g, model.getRoom(), baseX, baseY);
            }

            // ---- PROMPT ----
            if (!model.isShopOpen() && model.isShowInteractPrompt() && !gameOver) {
                int msgW = 240;
                int msgH = 26;
                int x = baseX + 10;
                int y = baseY + roomPixelH - 10 - msgH;

                g.setColor(new Color(0, 0, 0, 170));
                g.fillRoundRect(x, y, msgW, msgH, 8, 8);

                g.setColor(Color.WHITE);
                g.drawString("Premi E per interagire", x + 10, y + 18);
            }

            // ---- SHOP OVERLAY ----
            if (model.isShopOpen() && !gameOver) {
                renderShopOverlay(g, baseX, baseY, roomPixelW, roomPixelH);
            }

            // ---- GAME OVER OVERLAY ----
            if (gameOver) {
                drawGameOverOverlay(g, baseX, baseY, roomPixelW, roomPixelH);
            }

            g.setColor(new Color(255, 255, 255, 40));
            g.drawRect(baseX, baseY, roomPixelW, roomPixelH);

        } finally {
            g.dispose();
        }
    }

    private void drawPlayerWithBlink(Graphics2D g, BufferedImage sprite, int x, int y, int w, int h) {
        Composite old = g.getComposite();
        try {
            if (model.isPlayerBlinking()) {
                float t = model.getPlayerBlinkT();
                boolean dim = ((int) (t * 12f)) % 2 == 0;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, dim ? 0.35f : 1.0f));
            }
            g.drawImage(sprite, x, y, w, h, null);
        } finally {
            g.setComposite(old);
        }
    }

    private void drawGameOverOverlay(Graphics2D g, int baseX, int baseY, int roomPixelW, int roomPixelH) {
        String msg = "premere X per riprovare";

        int w = 360;
        int h = 90;
        int x = baseX + (roomPixelW - w) / 2;
        int y = baseY + (roomPixelH - h) / 2;

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(x, y, w, h, 12, 12);

        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, w, h, 12, 12);

        // testo (font di default)
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth(msg)) / 2;
        int ty = y + (h + fm.getAscent()) / 2 - 4;

        g.drawString(msg, tx, ty);
    }

    private void drawEnemy(Graphics2D g, int roomX, int roomY) {
        if (!model.isEnemyAlive()) return;
        if (Assets.enemy == null) return;

        if (model.getCurrentRoomIndex() != ENEMY_ROOM_INDEX) return;
        if (model.isTransitioning() || sliding) return;

        int ex = roomX + Math.round(model.getEnemyX());
        int ey = roomY + Math.round(model.getEnemyY());

        int sw = Assets.enemy.getWidth();   // 56
        int sh = Assets.enemy.getHeight();  // 58

        int dw = Math.round(sw * ENEMY_SCALE); // 28
        int dh = Math.round(sh * ENEMY_SCALE); // 29

        Composite old = g.getComposite();
        try {
            if (model.isEnemyBlinking()) {
                float t = model.getEnemyBlinkT();
                boolean dim = ((int) (t * 12f)) % 2 == 0;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, dim ? 0.35f : 1.0f));
            }
            g.drawImage(Assets.enemy, ex, ey, dw, dh, null);
        } finally {
            g.setComposite(old);
        }
    }

    private void drawRupee(Graphics2D g, int roomX, int roomY) {
        if (!model.isRupeeAlive()) return;
        if (Assets.rupee == null) return;

        if (model.getCurrentRoomIndex() != 7) return;
        if (model.isTransitioning() || sliding) return;

        int rx = roomX + Math.round(model.getRupeeX());
        int ry = roomY + Math.round(model.getRupeeY());

        int sw = Assets.rupee.getWidth();   // 8
        int sh = Assets.rupee.getHeight();  // 14

        int zoom = 2; // 16x28
        g.drawImage(Assets.rupee, rx, ry, sw * zoom, sh * zoom, null);
    }

    private BufferedImage getPlayerSprite() {
        int dir = switch (model.getFacing()) {
            case DOWN -> Assets.DIR_DOWN;
            case UP -> Assets.DIR_UP;
            case LEFT -> Assets.DIR_LEFT;
            case RIGHT -> Assets.DIR_RIGHT;
        };

        if (model.isAttacking()) {
            int f = model.getAttackFrame(); // 0..1
            return Assets.playerAttack[dir][f];
        }

        if (model.isMoving()) {
            int f = model.getAnimFrame(); // 0..1
            return Assets.playerWalk[dir][f];
        }

        return Assets.playerIdle[dir];
    }

    private void renderHud(Graphics2D g, int screenW) {
        g.setColor(new Color(20, 20, 20));
        g.fillRect(0, 0, screenW, GameModel.HUD_HEIGHT);

        g.setColor(Color.WHITE);
        g.drawString("Player: " + model.getProfileNickname(), 10, 22);
        g.drawString("Vite: " + model.getLives() +
                "  Rupie: " + model.getRupees() +
                "  Score: " + model.getScore(), 10, 44);
    }

    private void renderRoomFloor(Graphics2D g, Room room, int ox, int oy) {
        for (int y = 0; y < Room.ROWS; y++) {
            for (int x = 0; x < Room.COLS; x++) {
                int t = room.getTile(x, y);
                if (t != Room.TILE_FLOOR) continue;

                int px = ox + x * GameModel.TILE_SIZE;
                int py = oy + y * GameModel.TILE_SIZE;

                g.setColor(new Color(20, 80, 20));
                g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);

                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
            }
        }
    }

    private void renderRoomSolids(Graphics2D g, Room room, int ox, int oy) {
        for (int y = 0; y < Room.ROWS; y++) {
            for (int x = 0; x < Room.COLS; x++) {
                int t = room.getTile(x, y);
                if (t != Room.TILE_SOLID) continue;

                int px = ox + x * GameModel.TILE_SIZE;
                int py = oy + y * GameModel.TILE_SIZE;

                g.setColor(Color.DARK_GRAY);
                g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);

                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
            }
        }
    }

    private void renderShopOverlay(Graphics2D g, int baseX, int baseY, int roomPixelW, int roomPixelH) {
        int w = 360;
        int h = 200;
        int x = baseX + (roomPixelW - w) / 2;
        int y = baseY + (roomPixelH - h) / 2;

        g.setColor(new Color(0, 0, 0, 210));
        g.fillRoundRect(x, y, w, h, 12, 12);

        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, w, h, 12, 12);

        g.drawString("Negozio", x + 14, y + 24);

        String[] items = {
                "Cuore (+1 vita)          - 15 rupie",
                "Item 2 (+100 score)      - 10 rupie",
                "Item 3 (+250 score)      - 25 rupie",
                "Esci"
        };

        int sel = model.getShopSelectionIndex();

        int lineY = y + 58;
        for (int i = 0; i < items.length; i++) {
            if (i == sel) {
                g.setColor(new Color(255, 255, 255, 40));
                g.fillRoundRect(x + 10, lineY - 14, w - 20, 22, 8, 8);
                g.setColor(Color.WHITE);
                g.drawString("> " + items[i], x + 16, lineY);
            } else {
                g.setColor(new Color(220, 220, 220));
                g.drawString(items[i], x + 26, lineY);
            }
            lineY += 28;
        }

        g.setColor(new Color(200, 200, 200));
        g.drawString("W/S: scegli   E: compra   ESC: esci", x + 14, y + h - 14);
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