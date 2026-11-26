package kr.recovery;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

final class ClueExhibit extends JLayeredPane {
    private record Asset(String file,boolean screen,double x,double y,double width,double height) {}

    private static final Map<Long,Asset> ART=Map.of(
            2L,new Asset("incident-board.png",false,.27,.23,.48,.52),
            3L,new Asset("lock-memo.png",false,.27,.23,.48,.52),
            5L,new Asset("cctv-log.png",true,.27,.23,.48,.52),
            6L,new Asset("entry-orders.png",false,.27,.23,.48,.52),
            7L,new Asset("power-rules.png",false,.27,.23,.48,.52),
            8L,new Asset("circuit-loads.png",true,.27,.23,.48,.52));

    private final Asset asset;
    private final BufferedImage picture;
    private final JScrollPane writing;

    ClueExhibit(long caseId,Scenario.Scene scene) {
        asset=caseId==1 ? ART.get(scene.id()) : null;
        picture=asset==null ? null : load(asset.file());
        boolean dark=picture==null || asset.screen();
        JTextArea text=SceneChrome.transcript();
        text.setForeground(dark ? new Color(226,239,228) : new Color(28,38,30));
        text.setCaretColor(text.getForeground());
        text.setBorder(BorderFactory.createEmptyBorder(5,4,4,4));
        text.setFont(text.getFont().deriveFont(15f));
        List<String> marks=ClueCues.marks(caseId,scene);
        Color inkMarker=dark ? new Color(101,139,111,137) : new Color(201,167,62,109);
        SceneChrome.annotate(text,ClueCues.exhibitText(scene),marks,inkMarker);
        writing=SceneChrome.scroll(text);
        add(writing,JLayeredPane.DEFAULT_LAYER);
        setOpaque(false);
    }

    @Override public void doLayout() {
        Rectangle frame=pictureBounds();
        if (picture==null) {
            writing.setBounds(13,9,Math.max(0,frame.width-26),Math.max(0,frame.height-18));
        } else {
            writing.setBounds(frame.x+(int)(frame.width*asset.x()),frame.y+(int)(frame.height*asset.y()),
                    (int)(frame.width*asset.width()),(int)(frame.height*asset.height()));
        }
    }

    private Rectangle pictureBounds() {
        if (picture==null) return new Rectangle(0,0,getWidth(),getHeight());
        double scale=Math.min((double)getWidth()/picture.getWidth(),(double)getHeight()/picture.getHeight());
        int width=(int)Math.round(picture.getWidth()*scale);
        int height=(int)Math.round(picture.getHeight()*scale);
        return new Rectangle((getWidth()-width)/2,(getHeight()-height)/2,width,height);
    }

    @Override protected void paintComponent(Graphics graphics) {
        if (picture==null) return;
        Graphics2D g=(Graphics2D)graphics.create();
        Rectangle box=pictureBounds();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(picture,box.x,box.y,box.width,box.height,null);
        g.dispose();
    }

    private static BufferedImage load(String file) {
        try (var source=ClueExhibit.class.getResourceAsStream("/clues/"+file)) {
            return source==null ? null : ImageIO.read(source);
        } catch(IOException ignored) { return null; }
    }
}

