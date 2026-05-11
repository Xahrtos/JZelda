package zelda.profile;

import java.time.Instant;

public class LeaderboardEntry {
    private final Instant timestamp;
    private final String profileId;
    private final int score;
    private final boolean won;
    private final int roomReached;
    private final int rupees;

    public LeaderboardEntry(Instant timestamp, String profileId, int score, boolean won, int roomReached, int rupees) {
        this.timestamp = timestamp;
        this.profileId = profileId;
        this.score = score;
        this.won = won;
        this.roomReached = roomReached;
        this.rupees = rupees;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getProfileId() { return profileId; }
    public int getScore() { return score; }
    public boolean isWon() { return won; }
    public int getRoomReached() { return roomReached; }
    public int getRupees() { return rupees; }
}