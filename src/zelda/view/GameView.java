package zelda.view;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

import zelda.model.GameModel;
import zelda.model.Room;
import zelda.model.RoomManager;
import zelda.model.SlideDir;
import zelda.root.Assets;

public class GameView extends JPanel implements zelda.model.GameEventListener {

    private final GameModel model;

    private boolean sliding = false;
    private float slideT = 0f;

    private static final float SLIDE_DURATION = 0.80f;
    private static final int PLAYER_ZOOM = 2;

    // ---- ENEMY / DROPS RENDER ----
    private static final float ENEMY_SCALE = 0.5f;  // 56x58 -> 28x29
    private static final float POTION_SCALE = 0.5f; // 64x72 -> 32x36
    private static final int BOSS_ROOM_INDEX = 7;

    // ---- BOSS FLOAT ----
    private static final float BOSS_FLOAT_FREQ = 1.25f;
    private static final float BOSS_FLOAT_AMP = 4.0f;

    // ---- BOSS BULLET DRAW ----
    private static final int BOSS_BULLET_DRAW_W = 18;
    private static final int BOSS_BULLET_DRAW_H = 18;

    // ---- GAME OVER UI ----
    private boolean gameOverUiActive = false;
    private float gameOverUiT = 0f;

    private static final float GAME_OVER_DELAY = 2.0f;
    private static final float GAME_OVER_FADE_IN = 0.9f;

    // ---- GAME OVER DEATH SPRITE ANIM ----
    private boolean deathAnimInit = false;
    private float deathAnimT = 0f;
    private static final float DEATH_MOVE_DUR = 0.9f;

    // pos iniziale (pixel schermo) e finale (pixel schermo)
    private float deathFromX, deathFromY;
    private float deathToX, deathToY;

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
        // game over ui + anim (solo in PLAY)
        if (model.isPlaying() && model.getLives() <= 0) {
            if (!gameOverUiActive) {
                gameOverUiActive = true;
                gameOverUiT = 0f;

                initDeathAnimTargets();
                deathAnimInit = true;
                deathAnimT = 0f;
            } else {
                gameOverUiT += dt;

                if (deathAnimInit && deathAnimT < DEATH_MOVE_DUR) {
                    deathAnimT = Math.min(DEATH_MOVE_DUR, deathAnimT + dt);
                }
            }
            repaint();
        } else {
            gameOverUiActive = false;
            gameOverUiT = 0f;
            deathAnimInit = false;
            deathAnimT = 0f;
        }

        if (sliding) {
            slideT += dt / SLIDE_DURATION;

            if (slideT >= 1f) {
                slideT = 1f;
                sliding = false;
                model.finishRoomTransition();
            }
            repaint();
        } else if (model.isTitle()) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);

        Graphics2D g = (Graphics2D) g0.create();
        try {
            // FIX: hints coerenti (NN + AA OFF)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            if (model.isTitle()) {
                renderTitleScreen(g);
                return;
            }

            boolean gameOver = model.getLives() <= 0;
            if (gameOver) {
                renderGameOverScreen(g);
                return;
            }

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

            BufferedImage playerSprite = getPlayerSprite();
            int sw = playerSprite.getWidth();
            int sh = playerSprite.getHeight();

            int pDrawW = sw * PLAYER_ZOOM;
            int pDrawH = sh * PLAYER_ZOOM;

            int pDrawX = roomX + Math.round(px);
            int pDrawY = roomY + Math.round(py);

            int playerFeetY = pDrawY + pDrawH;

            int merchantFeetY = Integer.MIN_VALUE;
            if (roomForEntities.hasNpc() && roomForEntities.getNpcBounds() != null) {
                var b = roomForEntities.getNpcBounds();
                merchantFeetY = roomY + b.y + b.height;
            }

            // draw order: enemy/drops sotto
            drawEnemy(g, roomX, roomY);
            
            // Rendering dei 2 Nuovi Nemici (Stanze Dispari)
            drawEnemy2Units(g, roomX, roomY);

            // boss + bullets
            drawBoss(g, roomX, roomY);
            drawBossBullets(g, roomX, roomY);

            drawRupee(g, roomX, roomY);
            drawPotion(g, roomX, roomY);

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

            // ---- SHOP OVERLAY ----
            if (model.isShopOpen()) {
                renderShopOverlay(g, baseX, baseY, roomPixelW, roomPixelH);
            }

            g.setColor(new Color(255, 255, 255, 40));
            g.drawRect(baseX, baseY, roomPixelW, roomPixelH);

        } finally {
            g.dispose();
        }
    }

    // ===========================
    // BOSS render (new refactor)
    // ===========================

    private void drawBoss(Graphics2D g, int roomX, int roomY) {
        if (model.getCurrentRoomIndex() != BOSS_ROOM_INDEX) return;
        if (model.isTransitioning() || sliding) return;

        BufferedImage sprite = model.isBossAlive() ? getBossSprite() : Assets.bossDeath;
        if (sprite == null) return;

        float t = model.getBossFloatT();
        int bob = Math.round((float) Math.sin(t * (float) Math.PI * 2f * BOSS_FLOAT_FREQ) * BOSS_FLOAT_AMP);

        int bx = roomX + Math.round(model.getBossX());
        int by = roomY + Math.round(model.getBossY()) + bob;

        g.drawImage(sprite, bx, by, sprite.getWidth(), sprite.getHeight(), null);
    }

    private BufferedImage getBossSprite() {
        GameModel.Facing f = model.getBossFacing();
        return switch (f) {
            case UP -> Assets.bossWalkUp;
            case DOWN -> Assets.bossWalkDown;
            case LEFT -> Assets.bossWalkLRLeft;
            case RIGHT -> Assets.bossWalkLR;
        };
    }

    private void drawBossBullets(Graphics2D g, int roomX, int roomY) {
        if (model.getCurrentRoomIndex() != BOSS_ROOM_INDEX) return;
        if (model.isTransitioning() || sliding) return;
        if (Assets.bossAttack == null) return;

        for (int i = 0; i < model.getBossBulletCount(); i++) {
            if (!model.isBossBulletAlive(i)) continue;

            int x = roomX + Math.round(model.getBossBulletX(i));
            int y = roomY + Math.round(model.getBossBulletY(i));

            g.drawImage(Assets.bossAttack, x, y, BOSS_BULLET_DRAW_W, BOSS_BULLET_DRAW_H, null);
        }
    }

    // ===========================
    // TITLE
    // ===========================

    private void renderTitleScreen(Graphics2D g) {
        int screenW = getWidth();
        int screenH = getHeight();

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, screenW, screenH);

        if (Assets.tsBg != null) {
            BufferedImage img = Assets.tsBg;
            int iw = img.getWidth();
            int ih = img.getHeight();

            float s = Math.min(screenW / (float) iw, screenH / (float) ih);
            int dw = Math.round(iw * s);
            int dh = Math.round(ih * s);

            int dx = (screenW - dw) / 2;
            int dy = (screenH - dh) / 2;

            g.drawImage(img, dx, dy, dw, dh, null);
        }

        String hint = "PREMI ENTER PER INIZIARE";

        Font old = g.getFont();
        Composite oldC = g.getComposite();
        try {
            g.setFont(new Font("Monospaced", Font.BOLD, 22));
            FontMetrics fm = g.getFontMetrics();

            int x = (screenW - fm.stringWidth(hint)) / 2;
            int y = screenH - 40;

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));

            g.setColor(new Color(0, 0, 0, 200));
            g.drawString(hint, x + 2, y + 2);

            g.setColor(Color.WHITE);
            g.drawString(hint, x, y);
        } finally {
            g.setComposite(oldC);
            g.setFont(old);
        }
    }

    // ===========================
    // GAME OVER
    // ===========================

    private void renderGameOverScreen(Graphics2D g) {
        int screenW = getWidth();
        int screenH = getHeight();

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, screenW, screenH);

        BufferedImage death = Assets.playerDeath;
        if (death != null) {
            int zoom = PLAYER_ZOOM;

            int dw = death.getWidth() * zoom;
            int dh = death.getHeight() * zoom;

            if (!deathAnimInit) {
                initDeathAnimTargets();
                deathAnimInit = true;
                deathAnimT = 0f;
            }

            float u = (DEATH_MOVE_DUR <= 0f) ? 1f : (deathAnimT / DEATH_MOVE_DUR);
            u = Math.max(0f, Math.min(1f, u));

            float eased = u * u * (3f - 2f * u); // smoothstep

            int x = Math.round(deathFromX + (deathToX - deathFromX) * eased);
            int y = Math.round(deathFromY + (deathToY - deathFromY) * eased);

            g.drawImage(death, x, y, dw, dh, null);
        }

        drawGameOverText(g, screenW, screenH);
    }

    private void initDeathAnimTargets() {
        int screenW = getWidth();
        int screenH = getHeight();

        BufferedImage death = Assets.playerDeath;
        if (death == null) {
            deathFromX = screenW / 2f;
            deathFromY = screenH / 2f;
            deathToX = deathFromX;
            deathToY = deathFromY;
            return;
        }

        int dw = death.getWidth() * PLAYER_ZOOM;
        int dh = death.getHeight() * PLAYER_ZOOM;

        int roomPixelW = Room.COLS * GameModel.TILE_SIZE;
        int baseX = (screenW - roomPixelW) / 2;
        int baseY = GameModel.HUD_HEIGHT;

        float px = model.getPlayerX();
        float py = model.getPlayerY();

        int normalW = 32 * PLAYER_ZOOM;
        int normalH = 32 * PLAYER_ZOOM;

        deathFromX = baseX + px + (normalW - dw) / 2f;
        deathFromY = baseY + py + (normalH - dh) / 2f;

        deathToX = (screenW - dw) / 2f;
        deathToY = (screenH - dh) / 2f + 30f;
    }

    private void drawGameOverText(Graphics2D g, int screenW, int screenH) {
        if (!gameOverUiActive) return;

        float t = gameOverUiT - GAME_OVER_DELAY;
        if (t <= 0f) return;

        float a = Math.min(1f, t / GAME_OVER_FADE_IN);
        float alpha = a;

        Color fg = new Color(155, 130, 255);
        Color shadow = new Color(0, 0, 0, 200);

        Font oldFont = g.getFont();
        Font titleFont = new Font("Monospaced", Font.BOLD, 64);
        Font hintFont = new Font("Monospaced", Font.BOLD, 22);

        Composite old = g.getComposite();
        try {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            String titleLeft = "GAME";
            String titleRight = "OVER";

            g.setFont(titleFont);
            FontMetrics fm = g.getFontMetrics();

            int gap = 110;
            int totalW = fm.stringWidth(titleLeft) + gap + fm.stringWidth(titleRight);

            int x0 = (screenW - totalW) / 2;
            int y0 = screenH / 2 - 90;

            g.setColor(shadow);
            g.drawString(titleLeft, x0 + 3, y0 + 3);
            g.drawString(titleRight, x0 + fm.stringWidth(titleLeft) + gap + 3, y0 + 3);

            g.setColor(fg);
            g.drawString(titleLeft, x0, y0);
            g.drawString(titleRight, x0 + fm.stringWidth(titleLeft) + gap, y0);

            String hint = "PREMI X PER RIPROVARE";
            g.setFont(hintFont);
            FontMetrics fm2 = g.getFontMetrics();

            int hx = (screenW - fm2.stringWidth(hint)) / 2;
            int hy = y0 + 70;

            g.setColor(shadow);
            g.drawString(hint, hx + 2, hy + 2);

            g.setColor(new Color(235, 235, 235));
            g.drawString(hint, hx, hy);

        } finally {
            g.setComposite(old);
            g.setFont(oldFont);
        }
    }

    // ===========================
    // PLAYER / ENEMY / DROPS
    // ===========================

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

    private void drawEnemy(Graphics2D g, int roomX, int roomY) {
        if (!model.isEnemyAlive()) return;
        if (Assets.enemy == null) return;

        if (model.getCurrentRoomIndex() != RoomManager.SHOP_ROOM_INDEX) return;
        if (model.isTransitioning() || sliding) return;

        int ex = roomX + Math.round(model.getEnemyX());
        int ey = roomY + Math.round(model.getEnemyY());

        int sw = Assets.enemy.getWidth();
        int sh = Assets.enemy.getHeight();

        int dw = Math.round(sw * ENEMY_SCALE);
        int dh = Math.round(sh * ENEMY_SCALE);

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

    /**
     * Disegna le 2 unità del Nemico 2 in tutte le stanze ad indice dispari
     */
    private void drawEnemy2Units(Graphics2D g, int roomX, int roomY) {
        // Controllo stanza dispari
        if (model.getCurrentRoomIndex() % 2 == 0) return;
        if (model.isTransitioning() || sliding) return;

        // Iteriamo per le 2 unità richieste
        for (int i = 0; i < 2; i++) {
            if (!model.isEnemy2Alive(i)) continue;

            // Logica del Blink adattata all'unità (tramite il tempo o flag del model)
            if (!model.isEnemy2Blinking(i) || (System.currentTimeMillis() / 50) % 2 == 0) {
                
                int dirIndex = Assets.DIR_DOWN;
                switch (model.getEnemy2Facing(i)) {
                    case UP    -> dirIndex = Assets.DIR_UP;
                    case DOWN  -> dirIndex = Assets.DIR_DOWN;
                    case LEFT  -> dirIndex = Assets.DIR_LEFT;
                    case RIGHT -> dirIndex = Assets.DIR_RIGHT;
                }

                int frameIndex = model.getEnemy2AnimFrame(i);
                BufferedImage enemy2Sprite = Assets.enemy2Walk[dirIndex][frameIndex];

                if (enemy2Sprite != null) {
                    int spriteW = enemy2Sprite.getWidth();
                    int spriteH = enemy2Sprite.getHeight();

                    int ex = roomX + Math.round(model.getEnemy2X(i));
                    int ey = roomY + Math.round(model.getEnemy2Y(i));

                    g.drawImage(enemy2Sprite, ex, ey, spriteW, spriteH, null);
                } else {
                    // Fallback se le grafiche non sono pronte
                    g.setColor(Color.ORANGE);
                    g.fillRect(roomX + Math.round(model.getEnemy2X(i)), roomY + Math.round(model.getEnemy2Y(i)), 32, 32);
                }
            }
        }
    }

    private void drawRupee(Graphics2D g, int roomX, int roomY) {
        if (!model.isRupeeAlive()) return;
        if (Assets.rupee == null) return;

        if (model.getCurrentRoomIndex() != RoomManager.SHOP_ROOM_INDEX) return;
        if (model.isTransitioning() || sliding) return;

        int rx = roomX + Math.round(model.getRupeeX());
        int ry = roomY + Math.round(model.getRupeeY());

        int sw = Assets.rupee.getWidth();   
        int sh = Assets.rupee.getHeight();  

        int zoom = 2; 
        g.drawImage(Assets.rupee, rx, ry, sw * zoom, sh * zoom, null);
    }

    private void drawPotion(Graphics2D g, int roomX, int roomY) {
        if (!model.isPotionAlive()) return;
        if (Assets.potion == null) return;

        if (model.getCurrentRoomIndex() != RoomManager.SHOP_ROOM_INDEX) return;
        if (model.isTransitioning() || sliding) return;

        int px = roomX + Math.round(model.getPotionX());
        int py = roomY + Math.round(model.getPotionY());

        int sw = Assets.potion.getWidth();   
        int sh = Assets.potion.getHeight();  

        int dw = Math.round(sw * POTION_SCALE);
        int dh = Math.round(sh * POTION_SCALE);

        g.drawImage(Assets.potion, px, py, dw, dh, null);
    }

    private BufferedImage getPlayerSprite() {
        int dir = switch (model.getFacing()) {
            case DOWN -> Assets.DIR_DOWN;
            case UP -> Assets.DIR_UP;
            case LEFT -> Assets.DIR_LEFT;
            case RIGHT -> Assets.DIR_RIGHT;
        };

        if (model.isAttacking()) {
            int f = model.getAttackFrame();
            return Assets.playerAttack[dir][f];
        }

        if (model.isMoving()) {
            int f = model.getAnimFrame();
            return Assets.playerWalk[dir][f];
        }

        return Assets.playerIdle[dir];
    }

    // ===========================
    // HUD / ROOM RENDER
    // ===========================

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

                if (Assets.floor != null) {
                    g.drawImage(Assets.floor, px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE, null);
                } else {
                    g.setColor(new Color(20, 80, 20));
                    g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
                }
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

                drawWallTile(g, x, y, px, py);
            }
        }
    }

    private void drawWallTile(Graphics2D g, int tx, int ty, int px, int py) {
        boolean left = (tx == 0);
        boolean right = (tx == Room.COLS - 1);
        boolean top = (ty == 0);
        boolean bottom = (ty == Room.ROWS - 1);

        if (left || right || top || bottom) {
            BufferedImage img = null;

            if (top && left) img = Assets.wallAngle270;
            else if (top && right) img = Assets.wallAngle0;
            else if (bottom && right) img = Assets.wallAngle90;
            else if (bottom && left) img = Assets.wallAngle180;
            else {
                if (top) img = Assets.wallStraight0;
                else if (right) img = Assets.wallStraight90;
                else if (bottom) img = Assets.wallStraight180;
                else if (left) img = Assets.wallStraight270;
            }

            if (img == null) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
            } else {
                g.drawImage(img, px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE, null);
            }
            return;
        }

        if (Assets.obstacles != null) {
            g.drawImage(Assets.obstacles, px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE, null);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(px, py, GameModel.TILE_SIZE, GameModel.TILE_SIZE);
        }
    }
    
    // ===========================
    // SHOP OVERLAY
    // ===========================

    private void renderShopOverlay(Graphics2D g, int baseX, int baseY, int roomPixelW, int roomPixelH) {
        int w = 360;
        int h = 220;
        int x = baseX + (roomPixelW - w) / 2;
        int y = baseY + (roomPixelH - h) / 2;

        g.setColor(new Color(0, 0, 0, 210));
        g.fillRoundRect(x, y, w, h, 12, 12);

        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, w, h, 12, 12);

        g.drawString("Negozio", x + 14, y + 24);

        String[] items = {
                "Pozione (+1 vita)        - 5 rupie",
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

        if (!model.getShopMessage().isBlank() && model.getShopMessageT() > 0f) {
            String msg = model.getShopMessage();
            FontMetrics fm = g.getFontMetrics();
            int mx = x + (w - fm.stringWidth(msg)) / 2;
            int my = y + h - 36;

            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(x + 12, y + h - 54, w - 24, 26, 10, 10);

            g.setColor(new Color(255, 60, 60));
            g.drawString(msg, mx, my);
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