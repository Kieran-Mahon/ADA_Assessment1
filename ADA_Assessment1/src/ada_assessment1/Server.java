package ada_assessment1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashSet;

/*
 * @author Kieran
 */
public class Server {

    public final int SERVER_PORT = 7777;
    private boolean requestStop;

    private ServerSocket serverSocket;
    
    private final ThreadPool pool;

    private MasterCanvas masterCanvas;
    
    private int connections;

    public Server() {
        this.requestStop = false;
        this.pool = new ThreadPool(5);
        this.connections = 0;
    }

    public void startServer() {
        //Try to set up the server (returns false if it cannot)
        if (setUpServer() == true) {
            this.masterCanvas = new MasterCanvas(60, 40);
            try {
                while (this.requestStop == false) {
                    //Wait for socket
                    Socket socket = serverSocket.accept();
                    System.out.println("Connection made with " + socket.getInetAddress());
                    
                    //Increase connection count
                    changeConnections(1);
                    
                    //Start a client Interactor (observer)
                    ClientInteractor client = new ClientInteractor(socket);
                    this.pool.perform(client);
                }
                this.serverSocket.close();
            } catch (IOException e) {
                System.out.println("Can't accept client connection: " + e);
            }
        }
        closeServer();
    }

    public boolean setUpServer() {
        boolean connectionMade = true;
        this.serverSocket = null;
        try {
            this.serverSocket = new ServerSocket(this.SERVER_PORT);
            System.out.println("Server started at " + InetAddress.getLocalHost() + " on port " + this.SERVER_PORT);
        } catch (IOException e) {
            System.out.println("Server can't listen on port: " + e);
            connectionMade = false;
        }
        return connectionMade;
    }
    
    public void requestStop() {
        this.requestStop = true;
    }
    
    public synchronized void changeConnections(int num) {
        this.connections = this.connections + num;
        //Add 2 more threads to make sure the server has more threads than connections
        if (this.connections == (this.pool.getSize() - 1)) {
            this.pool.resize(this.pool.getSize() + 2);
        } else if (this.connections < (this.pool.getSize() - 5)) {
            //Remove 2 threads if the num of connections is 5 less than the
            //total thread count but only if thread count after the change is
            //more or equal to 5 to make sure there is enough threads
            if ((this.pool.getSize() - 2) >= 5) {
                this.pool.resize(this.pool.getSize() - 2);
            }
        }
    }
    
    public synchronized void checkForCloseNeeded() {
        if (this.connections == 0) {
            closeServer();
        }
    }
    
    private void closeServer() {
        this.pool.destroyPool();
        System.out.println("Server Closed");
        System.exit(-1);
    }
    
    class MasterCanvas {
        private final Pixel[][] pixels;
        private final LinkedHashSet<TaskObserver<PixelPacket>> listeners;
        
        public MasterCanvas(int xNum, int yNum) {
            //Set up new pixels
            this.pixels = new Pixel[xNum][yNum];
            for (int x = 0; x < xNum; x++) {
                for (int y = 0; y < yNum; y++) {
                    this.pixels[x][y] = new Pixel();
                }
            }
            //Initialise listener set
            this.listeners = new LinkedHashSet<>();
        }
        
        public synchronized Pixel getPixel(int x, int y) {
            return this.pixels[x][y];
        }
        
        public synchronized void setPixel(PixelPacket packet) {
            int x = packet.getX();
            int y = packet.getY();
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
            //Notify other clients
            notifyAll(packet);
        }
        
        public void addListener(TaskObserver<PixelPacket> o) {
            this.listeners.add(o);
        }

        public void removeListener(TaskObserver<PixelPacket> o) {
            this.listeners.remove(o);
        }

        protected void notifyAll(PixelPacket progress) {
            for (TaskObserver<PixelPacket> listener : this.listeners) {
                listener.process(progress);
            }
        }
    }
    
    //Class used to queue pixel updates on the server so the thread which
    //is listening doesn't freeze
    class ClientPixelUpdater extends Task {

        private final PixelPacket packet;

        public ClientPixelUpdater(PixelPacket packet) {
            super(null);
            this.packet = packet;
        }
        
        @Override
        public void run() {
            masterCanvas.setPixel(packet);
        }
        
    }
    
    class ClientInteractor extends Task {

        private final Socket socket;
        private boolean requestStop;
        //To client
        private PrintWriter pw;
        //From client
        private BufferedReader br;
        //Observer
        private final MasterCanvasObserver masterCanvasObserver;

        public ClientInteractor(Socket socket) {
            super(null);
            this.socket = socket;
            this.requestStop = false;
            this.masterCanvasObserver = new MasterCanvasObserver();
            masterCanvas.addListener(this.masterCanvasObserver);
        }

        @Override
        public void run() {
            try {
                this.pw = new PrintWriter(this.socket.getOutputStream(), true);
                this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                
                while (this.requestStop == false) {
                    String[] clientResponse = br.readLine().split(", ");
                    if (clientResponse.length == 5) { //5 because x, y, r, g, b
                        try {
                            int x = Integer.parseInt(clientResponse[0]);
                            int y = Integer.parseInt(clientResponse[1]);
                            int r = Integer.parseInt(clientResponse[2]);
                            int g = Integer.parseInt(clientResponse[3]);
                            int b = Integer.parseInt(clientResponse[4]);

                            PixelPacket packet = new PixelPacket(x, y, r, g, b);
                            
                            //Create new task
                            pool.perform(new ClientPixelUpdater(packet));
                        } catch (NumberFormatException e) {}
                    }
                }
                //Close streams
                this.pw.close();
                this.br.close();
                this.socket.close();
            } catch (IOException e) {}
            
            //Remove listener
            masterCanvas.removeListener(this.masterCanvasObserver);
            //Decrease connection count
            changeConnections(-1);
            //Check if this is the last client on the server and if so then close the server
            checkForCloseNeeded();
        }
        
        public void sendToClient(PixelPacket p) {
            String message = p.getX() + ", ";
            message += p.getY() + ", ";
            message += p.getR() + ", ";
            message += p.getG() + ", ";
            message += p.getB();
            this.pw.println(message);
        }
        
        public void requestStop() {
            this.requestStop = true;
        }
        
        class MasterCanvasObserver implements TaskObserver<PixelPacket> {
            @Override
            public void process(PixelPacket progress) {
                sendToClient(progress);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }
}