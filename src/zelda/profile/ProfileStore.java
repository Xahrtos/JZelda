package zelda.profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ProfileStore {

    private static final ProfileStore INSTANCE = new ProfileStore();

    public static ProfileStore getInstance() {
        return INSTANCE;
    }

    private final Path dataDir = Paths.get("data");
    private final Path profilesFile = dataDir.resolve("profiles.csv");
    private final Path leaderboardFile = dataDir.resolve("leaderboard.csv");

    private ProfileStore() {}

    // ---------- Public API ----------

    public List<UserProfile> loadProfiles() {
        ensureDataDir();

        if (!Files.exists(profilesFile)) {
            writeDefaultProfilesFile();
        }

        try (Stream<String> lines = Files.lines(profilesFile, StandardCharsets.UTF_8)) {
            return lines
                    .skip(1) // header
                    .filter(l -> !l.isBlank())
                    .map(Csv::parseLine)
                    .filter(cols -> cols.size() >= 6)
                    .map(cols -> new UserProfile(
                            cols.get(0),
                            cols.get(1),
                            cols.get(2),
                            parseInt(cols.get(3), 0),
                            parseInt(cols.get(4), 0),
                            parseInt(cols.get(5), 0)
                    ))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Errore lettura " + profilesFile.toAbsolutePath(), e);
        }
    }

    public void saveProfiles(List<UserProfile> profiles) {
        ensureDataDir();

        List<String> out = new ArrayList<>();
        out.add("id,nickname,avatar,played,won,lost");

        for (UserProfile p : profiles) {
            out.add(String.join(",",
                    Csv.escape(p.getId()),
                    Csv.escape(p.getNickname()),
                    Csv.escape(p.getAvatarPath()),
                    Integer.toString(p.getPlayed()),
                    Integer.toString(p.getWon()),
                    Integer.toString(p.getLost())
            ));
        }

        try {
            Files.write(profilesFile, out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Errore scrittura " + profilesFile.toAbsolutePath(), e);
        }
    }

    public java.util.List<LeaderboardRow> topRows(int n) {
        var entries = topScores(n); // già ordinato per score desc

        java.util.List<LeaderboardRow> rows = new java.util.ArrayList<>();
        int rank = 1;
        for (var e : entries) {
            String nick = nicknameForProfileId(e.getProfileId());
            rows.add(new LeaderboardRow(rank++, nick, e.getScore(), e.getTimestamp()));
        }
        return rows;
    }

    public void recordMatchResult(String profileId, boolean won) {
        List<UserProfile> profiles = loadProfiles();

        boolean updated = false;
        for (UserProfile p : profiles) {
            if (p.getId().equals(profileId)) {
                p.incPlayed();
                if (won) p.incWon();
                else p.incLost();
                updated = true;
                break;
            }
        }

        if (updated) saveProfiles(profiles);
    }

    public String nicknameForProfileId(String profileId) {
        return loadProfiles().stream()
                .filter(p -> p.getId().equals(profileId))
                .map(UserProfile::getNickname)
                .findFirst()
                .orElse("Unknown");
    }

    public List<LeaderboardEntry> loadLeaderboard() {
        ensureDataDir();

        if (!Files.exists(leaderboardFile)) {
            writeDefaultLeaderboardFile();
        }

        try (Stream<String> lines = Files.lines(leaderboardFile, StandardCharsets.UTF_8)) {
            return lines
                    .skip(1)
                    .filter(l -> !l.isBlank())
                    .map(Csv::parseLine)
                    .filter(cols -> cols.size() >= 6)
                    .map(cols -> new LeaderboardEntry(
                            Instant.parse(cols.get(0)),
                            cols.get(1),
                            parseInt(cols.get(2), 0),
                            Boolean.parseBoolean(cols.get(3)),
                            parseInt(cols.get(4), 0),
                            parseInt(cols.get(5), 0)
                    ))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Errore lettura " + leaderboardFile.toAbsolutePath(), e);
        }
    }

    public void appendLeaderboardEntry(LeaderboardEntry e) {
        ensureDataDir();

        boolean exists = Files.exists(leaderboardFile);
        List<String> lines = new ArrayList<>();

        if (!exists) {
            lines.add("timestamp,profileId,score,won,roomReached,rupees");
        }

        lines.add(String.join(",",
                Csv.escape(e.getTimestamp().toString()),
                Csv.escape(e.getProfileId()),
                Integer.toString(e.getScore()),
                Boolean.toString(e.isWon()),
                Integer.toString(e.getRoomReached()),
                Integer.toString(e.getRupees())
        ));

        try {
            Files.write(leaderboardFile, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new RuntimeException("Errore append " + leaderboardFile.toAbsolutePath(), ex);
        }
    }

    // --- Stream<T> "visibile": top N punteggi ---
    public List<LeaderboardEntry> topScores(int n) {
        return loadLeaderboard().stream()
                .sorted(Comparator.comparingInt(LeaderboardEntry::getScore).reversed())
                .limit(n)
                .toList();
    }

    // --- Stream<T>: trova profilo per id ---
    public Optional<UserProfile> findProfileById(String id) {
        return loadProfiles().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    // ========== DELETE PROFILE ==========
    public void deleteProfile(String profileId) {
        // 1) Rimuovi profilo da profiles.csv
        List<UserProfile> profiles = loadProfiles();
        boolean profileRemoved = profiles.removeIf(p -> p.getId().equals(profileId));

        if (profileRemoved) {
            saveProfiles(profiles);
        }

        // 2) Rimuovi tutte le entry di leaderboard per questo profilo
        List<LeaderboardEntry> leaderboard = loadLeaderboard();
        boolean entriesRemoved = leaderboard.removeIf(e -> e.getProfileId().equals(profileId));

        if (entriesRemoved) {
            // Riscrivi il file leaderboard intero (senza le entry del profilo eliminato)
            saveLeaderboard(leaderboard);
        }
    }

    private void saveLeaderboard(List<LeaderboardEntry> entries) {
        ensureDataDir();

        List<String> out = new ArrayList<>();
        out.add("timestamp,profileId,score,won,roomReached,rupees");

        for (LeaderboardEntry e : entries) {
            out.add(String.join(",",
                    Csv.escape(e.getTimestamp().toString()),
                    Csv.escape(e.getProfileId()),
                    Integer.toString(e.getScore()),
                    Boolean.toString(e.isWon()),
                    Integer.toString(e.getRoomReached()),
                    Integer.toString(e.getRupees())
            ));
        }

        try {
            Files.write(leaderboardFile, out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Errore scrittura " + leaderboardFile.toAbsolutePath(), e);
        }
    }

    // ---------- Helpers ----------

    private void ensureDataDir() {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile creare cartella data/: " + dataDir.toAbsolutePath(), e);
        }
    }

    private void writeDefaultProfilesFile() {
        List<String> out = List.of(
                "id,nickname,avatar,played,won,lost",
                "p1,Player1,,0,0,0"
        );
        try {
            Files.write(profilesFile, out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Errore init " + profilesFile.toAbsolutePath(), e);
        }
    }

    private void writeDefaultLeaderboardFile() {
        List<String> out = List.of(
                "timestamp,profileId,score,won,roomReached,rupees"
        );
        try {
            Files.write(leaderboardFile, out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Errore init " + leaderboardFile.toAbsolutePath(), e);
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return def; }
    }
}