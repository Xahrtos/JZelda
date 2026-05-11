package zelda.model;

import java.util.ArrayList;
import java.util.List;

public class ObservableModel {
    private final List<GameEventListener> listeners = new ArrayList<>();

    public void addListener(GameEventListener l) {
        listeners.add(l);
    }

    public void removeListener(GameEventListener l) {
        listeners.remove(l);
    }

    protected void fireEvent(GameEvent event) {
        for (var l : listeners) l.onGameEvent(event);
    }
}