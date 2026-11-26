package kr.recovery;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); }
            catch (Exception ignored) {}
            for (Object key : UIManager.getDefaults().keySet().toArray()) {
                if (UIManager.get(key) instanceof FontUIResource) {
                    UIManager.put(key, new FontUIResource("Malgun Gothic", Font.PLAIN, 14));
                }
            }
            new RecoveryWindow().setVisible(true);
        });
    }
}

