
// Server2 class that
// receives data and sends data

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;



    public class Server {
        static ArrayList<ConnectionHandler> connections = new ArrayList<>();
        public class ConnectionHandler extends Thread {
            public Socket s;
            public Player p;
            public void start() {
                System.out.println("New connection!");
                try {
                    mainLoop();
                } catch (IOException e) {
                    if(p != null){
                        System.out.println(p.name + " has disconnected...");
                        players.remove(p);
                    } else{
                        System.out.println("a user has disconnected...");
                    }
                    stop();
                }
            }
            public ConnectionHandler(Socket s){
                this.s = s;
            }
            public void mainLoop() throws IOException {
                DataInputStream din=new DataInputStream(s.getInputStream());
                String name = din.readUTF();
                System.out.println("user " + ++ID + " registered as " + name + ".");
                p = new Player(s, name, ID);
                players.add(p);
//        Scanner sc = new Scanner(System.in);
                String str="";
                while(true){
                    str=din.readUTF();
                    System.out.println(str);
                    if(p.LinkedPlayer != null){
                        System.out.println("[DEBUG]" + str);
                        int id = Integer.parseInt(str.substring(0, str.indexOf("|")));
                        int col = Integer.parseInt(str.substring(str.indexOf("|") + 1));
                        p.sendMoveToLinkedPlayer(col);
                    } else if(str.substring(str.indexOf("|") + 1).equals("LIST")){
                        sendAllActivePlayers();
                    } else{
                        p.send(p.name + ", you are not currently in a game! Please wait for someone to join you.\nRun \"LIST\" to view all active players.");
                    }

                }
            }
            public Player findPlayerByID(int id){
                for(Player p : players)
                    if(id==p.ID)
                        return p;
                return null;
            }
            public void sendAllActivePlayers() throws IOException {
                StringBuilder sendstr = new StringBuilder();
                for(Player p : players){
                    sendstr.append("NAME: ").append(p.name).append("\nID: ").append(p.ID).append("\nIn game: ").append(p.LinkedPlayer != null).append("\n");
                }
                System.out.println(sendstr);
                p.send(sendstr.toString());
            }
        }
    static ArrayList<Player> players = new ArrayList<>();
    public HashMap<Integer, Integer> linkedPlayers = new HashMap<>();
    static int ID = 0;
    public Server() throws IOException {
        ServerSocket ss=new ServerSocket(8811);
        while(true){
            Socket clientSock = ss.accept();
            ConnectionHandler temp = new ConnectionHandler(clientSock);
            connections.add(temp);
            temp.start();
        }
    }
    }
