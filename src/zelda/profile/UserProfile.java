package zelda.profile;

public class UserProfile {
    private final String id;
    private String nickname;
    private String avatarPath;
    private int played;
    private int won;
    private int lost;

    public UserProfile(String id, String nickname, String avatarPath, int played, int won, int lost) {
        this.id = id;
        this.nickname = nickname;
        this.avatarPath = avatarPath;
        this.played = played;
        this.won = won;
        this.lost = lost;
    }

    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public String getAvatarPath() { return avatarPath; }
    public int getPlayed() { return played; }
    public int getWon() { return won; }
    public int getLost() { return lost; }

    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public void incPlayed() { played++; }
    public void incWon() { won++; }
    public void incLost() { lost++; }
}