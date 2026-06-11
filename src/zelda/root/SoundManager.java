package zelda.root;

import javax.sound.sampled.*;

public class SoundManager {
    
    private static Clip musicClip = null;
    
    public static void playSound(String soundName) {
        new Thread(() -> {
            try {
                String resourcePath = "/sounds/" + soundName + ".wav";
                var url = SoundManager.class.getResource(resourcePath);
                
                if (url == null) {
                    System.err.println("File audio non trovato nel classpath: " + resourcePath);
                    return;
                }
                
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
                
            } catch (Exception e) {
                System.err.println("Errore durante la riproduzione del suono: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    public static void playMusic(String musicName) {
        new Thread(() -> {
            try {
                // Ferma la musica precedente se in riproduzione
                if (musicClip != null && musicClip.isRunning()) {
                    musicClip.stop();
                    musicClip.close();
                }
                
                String resourcePath = "/sounds/" + musicName + ".wav";
                var url = SoundManager.class.getResource(resourcePath);
                
                if (url == null) {
                    System.err.println("File audio non trovato nel classpath: " + resourcePath);
                    return;
                }
                
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
                musicClip = AudioSystem.getClip();
                musicClip.open(audioInputStream);
                musicClip.loop(Clip.LOOP_CONTINUOUSLY); // Loop infinito
                musicClip.start();
                
            } catch (Exception e) {
                System.err.println("Errore durante la riproduzione della musica: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    public static void stopMusic() {
        if (musicClip != null) {
            if (musicClip.isRunning()) {
                musicClip.stop();
            }
            musicClip.close();
            musicClip = null;
        }
    }
}