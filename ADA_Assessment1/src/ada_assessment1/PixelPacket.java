package ada_assessment1;

/*
 * @author Kieran
 */
public class PixelPacket {
    private final int x;
    private final int y;
    private final int r;
    private final int g;
    private final int b;

    public PixelPacket(int x, int y, int r, int g, int b) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getR() {
        return this.r;
    }

    public int getG() {
        return this.g;
    }

    public int getB() {
        return this.b;
    }
}