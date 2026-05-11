package zelda.profile;

import zelda.model.GameEvent;
import zelda.model.GameEventType;
import zelda.model.ObservableModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileModel extends ObservableModel {

    private final ProfileStore store = ProfileStore.getInstance();

    private List<UserProfile> profiles = new ArrayList<>();
    private UserProfile selected;

    public void load() {
        profiles = new ArrayList<>(store.loadProfiles());
        if (!profiles.isEmpty()) selected = profiles.get(0);
        fireEvent(new GameEvent(GameEventType.PROFILE_CHANGED));
    }

    public List<UserProfile> getProfiles() {
        return profiles;
    }

    public Optional<UserProfile> getSelected() {
        return Optional.ofNullable(selected);
    }

    public void selectById(String id) {
        selected = profiles.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);

        fireEvent(new GameEvent(GameEventType.PROFILE_CHANGED));
    }

    public void createProfile(String nickname) {
        String id = "p" + System.currentTimeMillis();
        UserProfile p = new UserProfile(
                id,
                (nickname == null || nickname.isBlank()) ? "Player" : nickname.trim(),
                "", // avatar vuoto all’inizio
                0, 0, 0
        );
        profiles.add(p);
        selected = p;

        fireEvent(new GameEvent(GameEventType.PROFILE_CHANGED));
    }

    public void updateSelectedNickname(String nickname) {
        if (selected == null) return;
        selected.setNickname(nickname == null ? "" : nickname.trim());
        fireEvent(new GameEvent(GameEventType.PROFILE_CHANGED));
    }

    public void updateSelectedAvatar(String avatarPath) {
        if (selected == null) return;
        selected.setAvatarPath(avatarPath == null ? "" : avatarPath.trim());
        fireEvent(new GameEvent(GameEventType.PROFILE_CHANGED));
    }

    public void save() {
        store.saveProfiles(profiles);
    }
}