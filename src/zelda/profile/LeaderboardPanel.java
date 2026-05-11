package zelda.profile;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardPanel extends JPanel {

    private final ProfileStore store = ProfileStore.getInstance();

    private final LeaderboardTableModel tableModel = new LeaderboardTableModel();
    private final JTable table = new JTable(tableModel);

    public LeaderboardPanel() {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Leaderboard (Top 10)", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Aggiorna");
        refresh.addActionListener(e -> reload());
        bottom.add(refresh);
        add(bottom, BorderLayout.SOUTH);

        reload();
    }

    public void reload() {
        List<LeaderboardRow> rows = store.topRows(10);
        tableModel.setRows(rows);
    }

    private static class LeaderboardTableModel extends AbstractTableModel {
        private final String[] cols = {"#", "Nickname", "Score", "Timestamp"};
        private List<LeaderboardRow> rows = new ArrayList<>();

        public void setRows(List<LeaderboardRow> rows) {
            this.rows = (rows == null) ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LeaderboardRow r = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.getRank();
                case 1 -> r.getNickname();
                case 2 -> r.getScore();
                case 3 -> r.getTimestamp().toString();
                default -> "";
            };
        }
    }
}