package ada_assessment1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/*
 * @author Kieran
 */
public class PaintGUI extends JPanel {
    private final ServerPixelListerner pixelListener; //Listens for info from the server
    private TaskObserver<String> frameCloseObserver;
    private TaskObserver<PixelPacket> drawObserver; //Notifies the server with info
    
    private final DrawArea drawArea;
    private final Pixel[][] pixels;
    private final int pixelXSize;
    private final int pixelYSize;
    
    private final JSlider redSlider;
    private final JSlider greenSlider;
    private final JSlider blueSlider;

    public PaintGUI(int xNum, int yNum, int xSize, int ySize) {
        super(new BorderLayout());
        
        //Size variables
        this.pixelXSize = xSize;
        this.pixelYSize = ySize;
        
        //Set up new pixels
        this.pixels = new Pixel[xNum][yNum];
        for (int x = 0; x < xNum; x++) {
            for (int y = 0; y < yNum; y++) {
                this.pixels[x][y] = new Pixel();
            }
        }
        
        //Set up GUI elements
        JPanel tempPanel = new JPanel(new GridLayout());
        JLabel redLabel = new JLabel("Red", JLabel.CENTER);
        JLabel greenLabel = new JLabel("Green", JLabel.CENTER);
        JLabel blueLabel = new JLabel("Blue", JLabel.CENTER);
        this.redSlider = setUpSlider();
        this.greenSlider = setUpSlider();
        this.blueSlider = setUpSlider();
        this.drawArea = new DrawArea();
        
        //Create listener for mouse click/hold and add it to the draw area
        CustomMouseListener customMouseListener = new CustomMouseListener();
        this.drawArea.addMouseMotionListener(customMouseListener);
        this.drawArea.addMouseListener(customMouseListener);
        
        //Create listener for interaction from server
        this.pixelListener = new ServerPixelListerner();
        
        //Add GUI elements to panel
        tempPanel.add(redLabel);
        tempPanel.add(greenLabel);
        tempPanel.add(blueLabel);
        super.add(tempPanel, BorderLayout.NORTH);
        super.add(this.redSlider, BorderLayout.LINE_START);
        super.add(this.greenSlider, BorderLayout.CENTER);
        super.add(this.blueSlider, BorderLayout.LINE_END);
        super.add(this.drawArea, BorderLayout.SOUTH);
    }
    
    //Creates the frame this panel is in
    public void createFrame() {
        JFrame frame = new JFrame("Multiplayer Paint");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(this);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (frameCloseObserver != null) {
                    frameCloseObserver.process("");
                }
            }
        });
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    //Set up function for the sliders
    private JSlider setUpSlider() {
        JSlider tempSlider = new JSlider();
        tempSlider.setMaximum(255);
        tempSlider.setMinimum(0);
        tempSlider.setValue(255);
        return tempSlider;
    }
    
    private void changePixel(PixelPacket packet) {
        int x = packet.getX();
        int y = packet.getY();
        x = x / this.pixelXSize;
        y = y / this.pixelYSize;
        //Return if x is not within the draw area array size
        if ((x < 0) || (x >= this.pixels.length)) {
            return;
        }
        //Return if y is not within the draw area array size
        if ((y < 0) || (y >= this.pixels[0].length)) {
            return;
        }
        //Update pixel
        this.pixels[x][y].setPixel(packet.getR(), packet.getG(), packet.getB());
        //Draw pixel onto screen
        this.drawArea.repaint();
    }

    public TaskObserver getPixelListener() {
        return this.pixelListener;
    }

    public void setDrawObserver(TaskObserver<PixelPacket> drawObserver) {
        this.drawObserver = drawObserver;
    }
    
    public void setFrameCloseObserver(TaskObserver<String> frameCloseObserver) {
        this.frameCloseObserver = frameCloseObserver;
    }
    
    class DrawArea extends JPanel {
        
        public DrawArea() {
            super.setPreferredSize(new Dimension(pixels.length * pixelXSize, pixels[0].length * pixelYSize));
            super.setBackground(Color.lightGray);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int x = 0; x < pixels.length; x++) {
                for (int y = 0; y < pixels[0].length; y++) {
                    g.setColor(pixels[x][y].getColor());
                    g.fillRect(x * pixelXSize, y * pixelYSize, pixelXSize, pixelYSize);
                }
            }
        }
    }
    
    class ServerPixelListerner implements TaskObserver<PixelPacket>{
        @Override
        public void process(PixelPacket p) {
            PixelPacket packet = new PixelPacket(p.getX() * pixelXSize, p.getY() * pixelYSize, p.getR(), p.getG(), p.getB());
            changePixel(packet);
        }        
    }
    
    class CustomMouseListener implements MouseMotionListener, MouseListener {
        @Override
        public void mouseDragged(MouseEvent e) {
            drawPacket(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            drawPacket(e);
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {}
        
        @Override
        public void mouseMoved(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}
        
        private void drawPacket(MouseEvent e) {
            PixelPacket packet = new PixelPacket(e.getX(), e.getY(), redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            changePixel(packet);
            //Notify the server
            packet = new PixelPacket(e.getX() / pixelXSize, e.getY() /  pixelYSize, redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            drawObserver.process(packet);
        }
    }
}