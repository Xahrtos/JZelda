package zelda.profile;

import java.time.Instant;

public class LeaderboardRow {
    private final int rank;
    private final String nickname;
    private final int score;
    private final Instant timestamp;

    public LeaderboardRow(int rank, String nickname, int score, Instant timestamp) {
        this.rank = rank;
        this.nickname = nickname;
        this.score = score;
        this.timestamp = timestamp;
    }

    public int getRank() { return rank; }
    public String getNickname() { return nickname; }
    public int getScore() { return score; }
    public Instant getTimestamp() { return timestamp; }
}