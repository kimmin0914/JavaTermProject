package kr.recovery;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static kr.recovery.RecoveryWindow.*;

final class RoomPhoto extends JPanel {
    private static final Map<Long,String> ROOM_FILES = Map.of(
            1L,"rooftop-entry.png",2L,"rooftop-exit.png",3L,"central-corridor.png",
            5L,"power-control.png",6L,"loading-dock.png");
    private final Map<Long,Plate> roomImages = new HashMap<>();
    private final Map<Plate,BufferedImage> extensions = new HashMap<>();
    private final BufferedImage security = load("/rooms/security-interactive.png");
    private final BufferedImage emptySecurity = load("/rooms/security-after-recovery.png");
    private final BufferedImage[] securityStates = new BufferedImage[4];
    private final Consumer<RoomTargets.Spot> activate;
    private long room;
    private String title = "";
    private String subtitle;
    private List<RoomTargets.Spot> spots = List.of();
    private List<RoomTargets.Spot> layoutSpots = List.of();
    private int removedEvidence;
    private RoomTargets.Spot hovered;
    private long enteredAt;
    private int topClearance;
    private int bottomClearance;
    private final Timer fade = new Timer(16,event -> {
        if (System.nanoTime()-enteredAt>=230_000_000L) ((Timer)event.getSource()).stop();
        repaint();
    });

    RoomPhoto(Consumer<RoomTargets.Spot> activate) {
        this.activate = activate;
        setBackground(new Color(10,15,18));
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        setFocusable(true);
        MouseAdapter pickup = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) { highlight(hit(event.getX(),event.getY())); }
            @Override public void mouseExited(MouseEvent event) { highlight(null); }
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getButton()!=MouseEvent.BUTTON1 || event.getClickCount()!=1) return;
                RoomTargets.Spot target = hit(event.getX(),event.getY());
                if (target != null) activate.accept(target);
            }
        };
        addMouseListener(pickup);
        addMouseMotionListener(pickup);
        getAccessibleContext().setAccessibleName("클릭해서 조사하는 현장 사진");
        getAccessibleContext().setAccessibleDescription("마우스로 사물을 찾거나 사진에 초점을 둔 뒤 화살표와 Enter를 사용하세요.");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("RIGHT"),"next");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("LEFT"),"previous");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"),"activate");
        getActionMap().put("next",new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { cycle(1); }
        });
        getActionMap().put("previous",new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { cycle(-1); }
        });
        getActionMap().put("activate",new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (hovered!=null) activate.accept(hovered);
            }
        });
    }

    void showRoom(long room,String title,String subtitle,List<RoomTargets.Spot> spots) {
        showRoom(room,title,subtitle,spots,Set.of(),Set.of());
    }

    void showRoom(long room,String title,String subtitle,List<RoomTargets.Spot> spots,Set<Long> read,
                  Set<Long> recovered) {
        boolean changedRoom = this.room!=room;
        if (changedRoom && this.room!=0 && isShowing()) {
            enteredAt = System.nanoTime();
            fade.restart();
        }
        this.room = room;
        this.title = title;
        this.subtitle = subtitle;
        layoutSpots = spots;
        this.spots = spots.stream().filter(spot -> switch (spot.kind()) {
            case SCENE -> !read.contains(spot.id());
            case EVIDENCE -> !recovered.contains(spot.id());
            default -> true;
        }).toList();
        removedEvidence = (recovered.contains(1L) ? 1 : 0) | (recovered.contains(2L) ? 2 : 0);
        if (changedRoom || (hovered!=null && !this.spots.contains(hovered))) highlight(null);
        repaint();
    }

    void clearTarget() { highlight(null); }

    void setClearance(int top,int bottom) {
        topClearance=top;
        bottomClearance=bottom;
    }

    private void cycle(int step) {
        if (spots.isEmpty()) return;
        int index = hovered==null ? (step>0 ? -1 : 0) : spots.indexOf(hovered);
        highlight(spots.get(Math.floorMod(index+step,spots.size())));
    }

    private void highlight(RoomTargets.Spot spot) {
        if (hovered==spot) return;
        hovered = spot;
        setCursor(Cursor.getPredefinedCursor(spot==null ? Cursor.CROSSHAIR_CURSOR : Cursor.HAND_CURSOR));
        repaint();
    }

    RoomTargets.Spot hit(int x,int y) {
        Layout layout = layoutForPhoto();
        if (layout==null) return null;
        RoomTargets.Spot found = null;
        double best = Double.POSITIVE_INFINITY;
        for (RoomTargets.Spot spot : spots) {
            Point2D point = layout.anchor(spot);
            double widthFactor = switch (spot.kind()) {
                case PASSAGE, EXIT -> 0.068;
                case EVIDENCE -> 0.054;
                default -> 0.045;
            };
            double radiusX = Math.max(22,Math.min(68,layout.width()*widthFactor));
            double radiusY = Math.max(21,Math.min(66,layout.height()*widthFactor*1.55));
            double dx = (x-point.getX())/radiusX, dy = (y-point.getY())/radiusY;
            double distance = dx*dx + dy*dy;
            if (distance <= 1 && distance<best) {
                found = spot;
                best = distance;
            }
        }
        return found;
    }

    Point2D anchor(RoomTargets.Spot spot) {
        Layout layout = layoutForPhoto();
        return layout==null ? null : layout.anchor(spot);
    }

    private record Plate(BufferedImage image,int x,int width,int height) {}

    private record Layout(Plate plate,double left,double top,double width,double height) {
        Point2D anchor(RoomTargets.Spot spot) {
            return new Point2D.Double(left+spot.x()*width,top+spot.y()*height);
        }
    }

    private Layout layoutForPhoto() {
        int w = getWidth(), h = getHeight();
        if (w<=0 || h<=0) return null;
        Plate plate = room==4 && security!=null ? fullPlate(securityFrame())
                : roomImages.computeIfAbsent(room,RoomPhoto::loadRoom);
        if (plate==null) return null;
        double minX=1,maxX=0,minY=1,maxY=0;
        for (RoomTargets.Spot target : layoutSpots) {
            minX=Math.min(minX,target.x()); maxX=Math.max(maxX,target.x());
            minY=Math.min(minY,target.y()); maxY=Math.max(maxY,target.y());
        }
        int safeTop=Math.min(Math.max(23,topClearance),h/3);
        int safeBottom=h-Math.min(Math.max(28,bottomClearance),h/3);
        int safeLeft=Math.min(33,w/8), safeRight=w-safeLeft;
        double scale=Math.max((double)w/plate.width(),(double)h/plate.height());
        if (maxX>minX) scale=Math.min(scale,(safeRight-safeLeft)/((maxX-minX)*plate.width()));
        if (maxY>minY) scale=Math.min(scale,(safeBottom-safeTop)/((maxY-minY)*plate.height()));
        double width=plate.width()*scale, height=plate.height()*scale;
        return new Layout(plate,placeAxis(w,width,minX,maxX,safeLeft,safeRight),
                placeAxis(h,height,minY,maxY,safeTop,safeBottom),width,height);
    }

    private static double placeAxis(int extent,double size,double first,double last,int safeFrom,int safeTo) {
        double centered=(extent-size)/2;
        if (last<first) return centered;
        double safeStart=safeFrom-first*size, safeEnd=safeTo-last*size;
        double fillStart=Math.min(0,extent-size), fillEnd=Math.max(0,extent-size);
        double start=Math.max(safeStart,fillStart), end=Math.min(safeEnd,fillEnd);
        return start<=end ? clamp(centered,start,end) : clamp(centered,safeStart,safeEnd);
    }

    private static double clamp(double value,double low,double high) {
        return Math.max(low,Math.min(high,value));
    }

    private static Plate fullPlate(BufferedImage picture) {
        return new Plate(picture,0,picture.getWidth(),picture.getHeight());
    }

    private static Plate loadRoom(long room) {
        String file=ROOM_FILES.get(room);
        BufferedImage picture=file==null ? null : load("/rooms/"+file);
        if (picture!=null) return fullPlate(picture);
        // Retain the earlier panoramas as a fallback for missing standalone art.
        boolean first=room==1 || room==4 || room==5;
        BufferedImage atlas=load(first ? "/rooms/rooftop-security-control.png" : "/rooms/corridor-exit-loading.png");
        if (atlas==null) return null;
        int width=atlas.getWidth()/3;
        int index=switch ((int)room) { case 4,2 -> 1; case 5,6 -> 2; default -> 0; };
        return new Plate(atlas,width*index,width,atlas.getHeight());
    }

    private void fillEdges(Graphics2D g,Layout layout,int width,int height) {
        if (layout.left()<=0 && layout.top()<=0 && layout.left()+layout.width()>=width
                && layout.top()+layout.height()>=height) return;
        BufferedImage soft=extensions.computeIfAbsent(layout.plate(),plate -> {
            BufferedImage small=new BufferedImage(32,18,BufferedImage.TYPE_INT_RGB);
            Graphics2D capture=small.createGraphics();
            capture.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            capture.drawImage(plate.image(),0,0,32,18,plate.x(),0,plate.x()+plate.width(),plate.height(),null);
            capture.dispose();
            return small;
        });
        g.drawImage(soft,0,0,width,height,null);
        g.setColor(new Color(9,14,12,150));
        g.fillRect(0,0,width,height);
        Plate p=layout.plate();
        int x0=(int)Math.round(layout.left()), x1=(int)Math.round(layout.left()+layout.width());
        int y0=(int)Math.round(layout.top()), y1=(int)Math.round(layout.top()+layout.height());
        if (y0>0) {
            int rows=edgePixels(y0,p.height(),layout.height());
            g.drawImage(p.image(),x0,0,x1,y0,p.x(),rows,p.x()+p.width(),0,null);
        }
        if (y1<height) {
            int rows=edgePixels(height-y1,p.height(),layout.height());
            g.drawImage(p.image(),x0,y1,x1,height,p.x(),p.height(),p.x()+p.width(),p.height()-rows,null);
        }
        if (x0>0) {
            int columns=edgePixels(x0,p.width(),layout.width());
            g.drawImage(p.image(),0,y0,x0,y1,p.x()+columns,0,p.x(),p.height(),null);
        }
        if (x1<width) {
            int columns=edgePixels(width-x1,p.width(),layout.width());
            g.drawImage(p.image(),x1,y0,width,y1,p.x()+p.width(),0,p.x()+p.width()-columns,p.height(),null);
        }
    }

    private static int edgePixels(int gap,int source,double displayed) {
        return Math.max(1,Math.min(source,(int)Math.ceil(gap*source/displayed)));
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        int w = getWidth(), h = getHeight();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Layout layout = layoutForPhoto();
        if (layout!=null) {
            fillEdges(g,layout,w,h);
            Plate plate=layout.plate();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(plate.image(),(int)Math.round(layout.left()),(int)Math.round(layout.top()),
                    (int)Math.round(layout.left()+layout.width()),(int)Math.round(layout.top()+layout.height()),
                    plate.x(),0,plate.x()+plate.width(),plate.height(),null);
        }
        if (h>0) {
            g.setPaint(new LinearGradientPaint(0,0,0,h,new float[]{0,.28f,.70f,1},new Color[]{
                    new Color(8,13,11,133),new Color(8,13,11,6),new Color(8,13,11,14),new Color(8,13,11,126)
            }));
            g.fillRect(0,0,w,h);
        }
        if (fade.isRunning()) {
            float remaining = (float)Math.max(0,1 - (System.nanoTime()-enteredAt)/230_000_000.0);
            if (remaining>0) {
                g.setColor(new Color(5,11,14,Math.round(180*remaining*remaining)));
                g.fillRect(0,0,w,h);
            }
        }
        int captionLine = Math.max(32,h-Math.max(20,bottomClearance+1));
        g.setFont(getFont().deriveFont(Font.BOLD,22f));
        g.setColor(new Color(7,12,10,180));
        g.drawString(title,24,captionLine+1);
        g.setColor(INK);
        g.drawString(title,23,captionLine);
        if (subtitle!=null) {
            g.setColor(ACCENT);
            g.setFont(getFont().deriveFont(Font.PLAIN,13f));
            g.drawString(subtitle,24,captionLine-25);
        }
        if (layout!=null && hovered!=null) drawTarget(g,layout,w,h);
        g.dispose();
    }

    private BufferedImage securityFrame() {
        if (removedEvidence==0 || emptySecurity==null) return security;
        BufferedImage frame = securityStates[removedEvidence];
        if (frame!=null) return frame;
        frame = new BufferedImage(security.getWidth(),security.getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = frame.createGraphics();
        painter.drawImage(security,0,0,null);
        painter.dispose();
        if ((removedEvidence&1)!=0) replaceRegion(frame,0.148,0.550,0.307,0.704);
        if ((removedEvidence&2)!=0) replaceRegion(frame,0.672,0.467,0.853,0.640);
        securityStates[removedEvidence] = frame;
        return frame;
    }

    private void replaceRegion(BufferedImage frame,double left,double top,double right,double bottom) {
        int w=frame.getWidth(), h=frame.getHeight();
        int x0=(int)(w*left), y0=(int)(h*top), x1=(int)(w*right), y1=(int)(h*bottom);
        for (int y=y0;y<y1;y++) {
            for (int x=x0;x<x1;x++) {
                int edge = Math.min(Math.min(x-x0,x1-1-x),Math.min(y-y0,y1-1-y));
                int patch = emptySecurity.getRGB(x*emptySecurity.getWidth()/w,y*emptySecurity.getHeight()/h);
                if (edge>=6) {
                    frame.setRGB(x,y,patch);
                } else {
                    int original = frame.getRGB(x,y);
                    int color = 0xff000000;
                    for (int shift : new int[]{16,8,0}) {
                        int combined = (((original>>shift)&255)*(6-edge) + ((patch>>shift)&255)*edge)/6;
                        color |= combined<<shift;
                    }
                    frame.setRGB(x,y,color);
                }
            }
        }
    }

    private void drawTarget(Graphics2D g,Layout layout,int width,int height) {
        Point2D position = layout.anchor(hovered);
        int x = (int)Math.round(position.getX()), y = (int)Math.round(position.getY());
        g.setColor(new Color(8,19,23,210));
        g.fillOval(x-12,y-12,24,24);
        g.setColor(ACCENT);
        g.setStroke(new BasicStroke(2));
        g.drawOval(x-10,y-10,20,20);
        g.drawLine(x-3,y,x+3,y);
        g.drawLine(x,y-3,x,y+3);
        g.setFont(getFont().deriveFont(Font.BOLD,13f));
        String caption = hovered.object();
        int badgeWidth = g.getFontMetrics().stringWidth(caption)+18;
        int badgeX = Math.min(width-badgeWidth-5,Math.max(5,x-badgeWidth/2));
        int badgeY = Math.min(height-27,Math.max(5,y-42));
        g.setColor(new Color(8,19,23,230));
        g.fillRoundRect(badgeX,badgeY,badgeWidth,24,8,8);
        g.setColor(INK);
        g.drawString(caption,badgeX+9,badgeY+16);
    }

    private static BufferedImage load(String path) {
        try (var source = RoomPhoto.class.getResourceAsStream(path)) {
            return source == null ? null : ImageIO.read(source);
        } catch (IOException e) { return null; }
    }
}

