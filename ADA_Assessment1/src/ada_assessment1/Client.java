package ada_assessment1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * @author Kieran
 */
public class Client extends Task {

    public final String HOST_NAME = "localhost";
    public final int HOST_PORT = 7777;
    private boolean requestStop;
    private boolean allowToSend;
    private Socket socket;
    //To server
    private PrintWriter pw;
    //From server
    private BufferedReader br;

    public Client() {
        super(null);
    }

    public boolean startClient() {
        boolean connectionMade = true;
        this.allowToSend = false;
        this.requestStop = false;
        this.socket = null;
        try {
            this.socket = new Socket(HOST_NAME, HOST_PORT);
            System.out.println("Connection made!");
        } catch (IOException e) {
            System.out.println("Client could not make connection!");
            connectionMade = false;
        }
        return connectionMade;
    }

    @Override
    public void run() {
        if (startClient() == true) {
            //Allow messages to be sent
            this.allowToSend = true;

            //GUI
            PaintGUI paintGUI = new PaintGUI(60, 40, 10, 10);
            paintGUI.createFrame();

            //Add GUI pixel listener
            addListener(paintGUI.getPixelListener());
            
            
            //Add GUI observer
            paintGUI.setDrawObserver(new DrawListener());

            try {
                this.pw = new PrintWriter(this.socket.getOutputStream(), true);
                this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                //Allow painting till the connect is ended or application is closed
                while (this.requestStop == false) {
                    //Get server response
                    String[] serverResponse = this.br.readLine().split(", ");
                    //Make sure only 5 index array responses are allowed
                    if (serverResponse.length == 5) {
                        try {
                            int x = Integer.parseInt(serverResponse[0]);
                            int y = Integer.parseInt(serverResponse[1]);
                            int r = Integer.parseInt(serverResponse[2]);
                            int g = Integer.parseInt(serverResponse[3]);
                            int b = Integer.parseInt(serverResponse[4]);

                            PixelPacket packet = new PixelPacket(x, y, r, g, b);
                            notifyAll(packet);
                        } catch (NumberFormatException e) {}
                    }
                }
                
                //Close streams
                this.pw.close();
                this.br.close();
                this.socket.close();
            } catch (IOException e) {
                System.err.println("Client error: " + e);
            }
        }
        System.exit(-1);
    }
    
    //Function to send the pixel data to the server
    private void sendToServer(PixelPacket p) {
        if (this.allowToSend == true) {
            String message = p.getX() + ", ";
            message += p.getY() + ", ";
            message += p.getR() + ", ";
            message += p.getG() + ", ";
            message += p.getB();
            this.pw.println(message);
        }
    }

    public void frameClosed() {
        this.requestStop = true;
    }

    class CloseListener implements TaskObserver<String> {
        @Override
        public void process(String progress) {
            frameClosed();
        }
    }

    class DrawListener implements TaskObserver<PixelPacket> {
        @Override
        public void process(PixelPacket progress) {
            sendToServer(progress);
        }
    }

    public static void main(String[] args) {
        ThreadPool pool = new ThreadPool(1);
        Client client = new Client();
        pool.perform(client);
    }
}