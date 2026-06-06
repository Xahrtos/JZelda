package zelda.profile;

public class ProfileController {

    private final ProfileModel model;

    public ProfileController(ProfileModel model) {
        this.model = model;
    }

    public void onCreateProfile(String nickname) {
        model.createProfile(nickname == null || nickname.isBlank() ? "Player" : nickname.trim());
        model.save();
    }

    public void onSelectProfile(String id) {
        model.selectById(id);
    }

    public void onChangeNickname(String nickname) {
        model.updateSelectedNickname(nickname == null ? "" : nickname.trim());
    }

   

    public void onSave() {
        model.save();
    }

    // ---- DELETE PROFILE ----
    public void onDeleteProfile(String profileId) {
        model.deleteProfile(profileId);
        model.save();
    }
}