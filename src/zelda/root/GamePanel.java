package zelda.root;

import zelda.controller.GameController;
import zelda.model.GameModel;
import zelda.profile.UserProfile;
import zelda.view.GameView;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final int width;
    private final int height;

    private final GameModel model;
    private final GameController controller;
    private final GameView view;

    private Timer timer;
    private long lastNs;

    public GamePanel(int width, int height) {
        this.width = width;
        this.height = height;

        setPreferredSize(new Dimension(width, height));
        setFocusable(true);
        setBackground(Color.BLACK);

        model = new GameModel();
        controller = new GameController(model);
        view = new GameView(model);

        setLayout(new BorderLayout());
        add(view, BorderLayout.CENTER);

        setupKeyBinds();
    }

    public void setActiveProfile(UserProfile profile) {
        if (profile == null) return;
        model.setActiveProfile(profile.getId(), profile.getNickname(), profile.getAvatarPath());
    }

    public void start() {
        if (timer != null && timer.isRunning()) return;

        lastNs = System.nanoTime();

        timer = new Timer(16, e -> tick());
        timer.start();

        requestFocusInWindow();
    }

    public void stop() {
        if (timer != null) timer.stop();
    }

    private void tick() {
        long now = System.nanoTime();
        float dt = (now - lastNs) / 1_000_000_000f;
        lastNs = now;

        if (dt > 0.05f) dt = 0.05f;

        controller.update(dt);
        view.tick(dt);
    }

    // -------- Key Bindings --------
    private void setupKeyBinds() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        bind(im, am, "W_P", KeyStroke.getKeyStroke("pressed W"), () -> controller.setUp(true));
        bind(im, am, "W_R", KeyStroke.getKeyStroke("released W"), () -> controller.setUp(false));

        bind(im, am, "S_P", KeyStroke.getKeyStroke("pressed S"), () -> controller.setDown(true));
        bind(im, am, "S_R", KeyStroke.getKeyStroke("released S"), () -> controller.setDown(false));

        bind(im, am, "A_P", KeyStroke.getKeyStroke("pressed A"), () -> controller.setLeft(true));
        bind(im, am, "A_R", KeyStroke.getKeyStroke("released A"), () -> controller.setLeft(false));

        bind(im, am, "D_P", KeyStroke.getKeyStroke("pressed D"), () -> controller.setRight(true));
        bind(im, am, "D_R", KeyStroke.getKeyStroke("released D"), () -> controller.setRight(false));

        bind(im, am, "E_P", KeyStroke.getKeyStroke("pressed E"), controller::pressInteract);
        bind(im, am, "ESC_P", KeyStroke.getKeyStroke("pressed ESCAPE"), controller::pressEsc);

        bind(im, am, "SPACE_P", KeyStroke.getKeyStroke("pressed SPACE"), controller::pressAttack);

        bind(im, am, "Q_P", KeyStroke.getKeyStroke("pressed Q"), controller::pressArchery);

        bind(im, am, "X_P", KeyStroke.getKeyStroke("pressed X"), controller::pressRetry);

        bind(im, am, "ENTER_P", KeyStroke.getKeyStroke("pressed ENTER"), controller::pressStart);
        bind(im, am, "KP_ENTER_P", KeyStroke.getKeyStroke("pressed KP_ENTER"), controller::pressStart);
    }

    private static void bind(InputMap im, ActionMap am, String name, KeyStroke ks, Runnable r) {
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                r.run();
            }
        });
    }
}