package zelda.profile;

import zelda.model.GameEvent;
import zelda.model.GameEventListener;
import zelda.model.GameEventType;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ProfilePanel extends JPanel implements GameEventListener {

    // SOLO DEFAULT: per ora 1 avatar disponibile
    private static final String[] DEFAULT_AVATARS = {
            "", // nessun avatar
            "assets/sprites/player_sheet1.png"
    };

    private final ProfileModel model;
    private final ProfileController controller;

    private final DefaultListModel<UserProfile> listModel = new DefaultListModel<>();
    private final JList<UserProfile> profileList = new JList<>(listModel);

    private final JTextField nicknameField = new JTextField();

    private final JComboBox<String> avatarCombo = new JComboBox<>(DEFAULT_AVATARS);
    private final JLabel avatarLabel = new JLabel("No avatar", SwingConstants.CENTER);

    private boolean updatingUI = false;

    private Consumer<UserProfile> onPlay = p -> {};

    public ProfilePanel(ProfileModel model, ProfileController controller) {
        this.model = model;
        this.controller = controller;

        setLayout(new BorderLayout(10, 10));

        // ---- Left: lista profili ----
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel l = new JLabel(value.getNickname() + " (" + value.getId() + ")");
            l.setOpaque(true);
            l.setBackground(isSelected ? new Color(60, 60, 90) : new Color(30, 30, 30));
            l.setForeground(Color.WHITE);
            l.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            return l;
        });

        profileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                UserProfile sel = profileList.getSelectedValue();
                if (sel != null) controller.onSelectProfile(sel.getId());
            }
        });

        JPanel left = new JPanel(new BorderLayout());
        left.add(new JScrollPane(profileList), BorderLayout.CENTER);

        JButton addProfile = new JButton("Crea profilo");
        addProfile.addActionListener(e -> {
            String nick = JOptionPane.showInputDialog(
                    this, "Nickname:", "Nuovo profilo", JOptionPane.QUESTION_MESSAGE
            );
            controller.onCreateProfile(nick);
        });
        left.add(addProfile, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(280, 400));

        // ---- Right: editor profilo ----
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        right.add(new JLabel("Nickname:"));
        right.add(nicknameField);

        JButton saveNick = new JButton("Salva nickname");
        saveNick.addActionListener(e -> {
            controller.onChangeNickname(nicknameField.getText());
            controller.onSave();
        });

        avatarLabel.setPreferredSize(new Dimension(200, 200));
        avatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        avatarCombo.addActionListener(e -> {
            if (updatingUI) return;
            String path = (String) avatarCombo.getSelectedItem();
            controller.onChooseDefaultAvatarPath(path);
        });

        JButton play = new JButton("Gioca");
        play.addActionListener(e -> model.getSelected().ifPresent(p -> onPlay.accept(p)));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(saveNick);
        buttons.add(play);

        right.add(Box.createVerticalStrut(10));
        right.add(new JLabel("Avatar (default):"));
        right.add(avatarCombo);
        right.add(Box.createVerticalStrut(8));
        right.add(avatarLabel);
        right.add(Box.createVerticalStrut(10));
        right.add(buttons);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        // Observer hook (tuo metodo)
        model.addListener(this);

        refreshFromModel();
    }

    public void setOnPlay(Consumer<UserProfile> onPlay) {
        this.onPlay = (onPlay == null) ? p -> {} : onPlay;
    }

    private void refreshFromModel() {
        updatingUI = true;
        try {
            listModel.clear();
            for (UserProfile p : model.getProfiles()) listModel.addElement(p);

            model.getSelected().ifPresentOrElse(sel -> {
                profileList.setSelectedValue(sel, true);
                nicknameField.setText(sel.getNickname());

                String path = sel.getAvatarPath();
                avatarCombo.setSelectedItem(path == null ? "" : path);

                if (path == null || path.isBlank()) {
                    avatarLabel.setIcon(null);
                    avatarLabel.setText("No avatar");
                } else {
                    ImageIcon ico = new ImageIcon(path);
                    Image img = ico.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    avatarLabel.setIcon(new ImageIcon(img));
                    avatarLabel.setText("");
                }
            }, () -> {
                nicknameField.setText("");
                avatarCombo.setSelectedItem("");
                avatarLabel.setIcon(null);
                avatarLabel.setText("No avatar");
            });

            revalidate();
            repaint();
        } finally {
            updatingUI = false;
        }
    }

    @Override
    public void onGameEvent(GameEvent event) {
        if (event.type() == GameEventType.PROFILE_CHANGED) {
            SwingUtilities.invokeLater(this::refreshFromModel);
        }
    }
}