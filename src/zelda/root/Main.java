package zelda.root;

import zelda.profile.LeaderboardPanel;
import zelda.profile.ProfileController;
import zelda.profile.ProfileModel;
import zelda.profile.ProfilePanel;
import zelda.profile.UserProfile;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::startApp);
    }

    private static void startApp() {
        // IMPORTANTISSIMO: carica gli asset una sola volta
        Assets.load();

        JFrame frame = new JFrame("JZelda");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);

        CardLayout cards = new CardLayout();
        JPanel rootCards = new JPanel(cards);

        // --- PROFILE MVC ---
        ProfileModel profileModel = new ProfileModel();
        profileModel.load();
        ProfileController profileController = new ProfileController(profileModel);
        ProfilePanel profilePanel = new ProfilePanel(profileModel, profileController);

        // --- LEADERBOARD VIEW ---
        LeaderboardPanel leaderboardPanel = new LeaderboardPanel();

        // --- GAME SCREEN HOLDER ---
        JPanel gameScreen = new JPanel(new BorderLayout());

        rootCards.add(profilePanel, "profile");
        rootCards.add(gameScreen, "game");
        rootCards.add(leaderboardPanel, "leaderboard");

        // Top bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnProfiles = new JButton("Profili");
        JButton btnLeaderboard = new JButton("Leaderboard");
        JButton btnGame = new JButton("Gioco");

        topBar.add(btnProfiles);
        topBar.add(btnLeaderboard);
        topBar.add(btnGame);

        btnProfiles.addActionListener(e -> cards.show(rootCards, "profile"));
        btnLeaderboard.addActionListener(e -> {
            leaderboardPanel.reload();
            cards.show(rootCards, "leaderboard");
        });
        btnGame.addActionListener(e -> cards.show(rootCards, "game"));

        // Play
        profilePanel.setOnPlay((UserProfile selected) -> {
            gameScreen.removeAll();

            GamePanel game = new GamePanel(800, 600);
            game.setActiveProfile(selected);

            gameScreen.add(game, BorderLayout.CENTER);
            gameScreen.revalidate();
            gameScreen.repaint();

            cards.show(rootCards, "game");
            frame.pack();

            game.start();
        });

        JPanel container = new JPanel(new BorderLayout());
        container.add(topBar, BorderLayout.NORTH);
        container.add(rootCards, BorderLayout.CENTER);

        frame.setContentPane(container);
        cards.show(rootCards, "profile");

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}