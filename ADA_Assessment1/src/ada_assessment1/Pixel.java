package ada_assessment1;

import java.awt.Color;

/*
 * @author Kieran
 */
public class Pixel {
    private int red;
    private int green;
    private int blue;

    public Pixel() {
        this.red = 200;
        this.green = 200;
        this.blue = 200;
    }

    public void setPixel(int newRed, int newGreen, int newBlue) {
        this.red = newRed;
        this.green = newGreen;
        this.blue = newBlue;
    }

    public Color getColor() {
        return new Color(this.red, this.green, this.blue);
    }
}