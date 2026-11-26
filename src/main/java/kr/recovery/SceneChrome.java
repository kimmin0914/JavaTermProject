package kr.recovery;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.util.List;

import static kr.recovery.RecoveryWindow.*;

final class SceneChrome {
    private SceneChrome() {}

    static JPanel shade(LayoutManager layout,int opacity) {
        JPanel panel = new JPanel(layout) {
            @Override protected void paintComponent(Graphics graphics) {
                if (getWidth()<1 || getHeight()<1) return;
                Graphics2D g = antialias(graphics);
                g.setPaint(new GradientPaint(0,0,new Color(12,17,16,opacity),
                        0,getHeight(),new Color(9,13,12,Math.min(235,opacity+17))));
                g.fillRoundRect(0,0,getWidth(),getHeight(),13,13);
                g.setColor(new Color(175,187,172,50));
                g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,13,13);
                g.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    static JPanel clear(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JTextArea transcript() {
        JTextArea area=prose(false);
        area.setUI(new BasicTextAreaUI());
        area.setOpaque(false);
        area.setForeground(INK);
        area.setCaretColor(INK);
        area.setSelectedTextColor(INK);
        area.setSelectionColor(new Color(62,80,72));
        area.setFont(area.getFont().deriveFont(16f));
        return area;
    }

    static final Color NOTE_MARK = new Color(101,133,110,133);

    static void annotate(JTextArea area,String content,List<String> marks,Color color) {
        var highlighter=area.getHighlighter();
        highlighter.removeAllHighlights();
        area.setText(content);
        var marker=new DefaultHighlighter.DefaultHighlightPainter(color);
        for (String phrase : marks) {
            if (phrase.isBlank()) continue;
            for (int from=0;;) {
                int match=content.indexOf(phrase,from);
                if (match<0) break;
                from=match+phrase.length();
                try {
                    highlighter.addHighlight(match,from,marker);
                } catch (BadLocationException error) {
                    throw new IllegalArgumentException("단서 문장 표시 범위가 올바르지 않습니다.",error);
                }
            }
        }
        area.setCaretPosition(0);
    }

    static JTextField answerInput() {
        JTextField field=new JTextField(8);
        field.setUI(new BasicTextFieldUI());
        field.setCaretColor(INK);
        field.setForeground(INK);
        field.setBackground(new Color(16,22,20));
        field.setSelectionColor(new Color(68,89,79));
        field.setSelectedTextColor(INK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(81,98,90)),
                BorderFactory.createEmptyBorder(7,9,7,9)));
        return field;
    }

    static JScrollPane scroll(JTextArea text) {
        JScrollPane pane=new JScrollPane(text,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setUI(new BasicScrollPaneUI());
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setViewportBorder(null);
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        JScrollBar bar=pane.getVerticalScrollBar();
        bar.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor=new Color(79,97,88);
                trackColor=new Color(19,26,23);
            }
            @Override protected JButton createIncreaseButton(int orientation) { return invisibleArrow(); }
            @Override protected JButton createDecreaseButton(int orientation) { return invisibleArrow(); }
            private JButton invisibleArrow() {
                JButton arrow=new JButton();
                arrow.setPreferredSize(new Dimension());
                return arrow;
            }
        });
        bar.setPreferredSize(new Dimension(7,1));
        return pane;
    }

    static JButton control(String text,boolean primary) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = antialias(graphics);
                boolean active = isEnabled() && (getModel().isRollover() || hasFocus());
                Color fill = !isEnabled() ? new Color(16,20,19,108)
                        : active ? new Color(71,88,79,224)
                        : primary ? new Color(45,60,53,218) : new Color(20,28,25,163);
                g.setColor(fill);
                g.fillRoundRect(0,0,getWidth(),getHeight(),9,9);
                g.setColor(active ? new Color(198,213,199,204) : new Color(154,176,161,primary ? 134 : 73));
                g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,9,9);
                g.setColor(isEnabled() ? INK : new Color(145,155,146));
                g.setFont(getFont());
                FontMetrics m=g.getFontMetrics();
                g.drawString(getText(),(getWidth()-m.stringWidth(getText()))/2,
                        (getHeight()-m.getHeight())/2+m.getAscent());
                g.dispose();
            }
        };
        styleButton(button,primary);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(7,12,7,12));
        return button;
    }

    static Icon checkMark(boolean selected) {
        return new Icon() {
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
            @Override public void paintIcon(Component owner,Graphics graphics,int x,int y) {
                Graphics2D g=antialias(graphics);
                g.setColor(new Color(21,31,26,224));
                g.fillRoundRect(x+1,y+1,14,14,4,4);
                g.setColor(new Color(136,160,148));
                g.drawRoundRect(x+1,y+1,14,14,4,4);
                if (selected) {
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    g.drawLine(x+4,y+8,x+7,y+11);
                    g.drawLine(x+7,y+11,x+12,y+5);
                }
                g.dispose();
            }
        };
    }

    private static Graphics2D antialias(Graphics input) {
        Graphics2D g=(Graphics2D)input.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }
}

