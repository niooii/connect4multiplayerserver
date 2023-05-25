
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
            public void run() {
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
                    interrupt();
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
                    processSocketInput(str);
                    //add new elseif for VIEW command
                }
            }

            void processSocketInput(String str) throws IOException {
                if(p.LinkedPlayer != null){
                    HandleGame(str);
                } else if(str.substring(str.indexOf("|") + 1).toLowerCase().startsWith("list")){
                    sendAllActivePlayers();
                } else if(str.toLowerCase().startsWith("challenge")){
                    Player playertemp;
                    String idToChallenge = str.substring(str.indexOf("|") + 1);
                    int idtc = Integer.parseInt(idToChallenge);
                    if((playertemp = findPlayerByID(idtc)) == null){
                        p.send("could not find player with id " + idtc + "...");
                    } else {
                        playertemp.addChallenge(p);
                        p.send("Challenge sent to player " + playertemp.name + "!\n");
                        playertemp.send("You have " + playertemp.challengers.size() + " new challenge request(s).\n" +
                                "Type \"VIEW\" to view them.");
                        //implement later
                    }
                } else if(str.toLowerCase().startsWith("view")){
                    sendAllChallenges();
                } else if(str.toLowerCase().startsWith("accept")){
                    Player playertemp;
                    String idToAccept = str.substring(str.indexOf("|") + 1);
                    int idta = Integer.parseInt(idToAccept);
                    if((playertemp = findPlayerByID(idta)) == null){
                        p.send("could not find player with id " + idta + "...");
                    } else {
                        if(playertemp.acceptChallenge(playertemp)){
                            p.send("Accepted " + playertemp.name + "'s challenge!\n");
                            p.send("SETOPPONENT|" + playertemp.name);
                            p.send("SETNOTIDLE");
                            playertemp.send("SETNOTIDLE");
                        } else {
                            p.send("You do not have a challenge request from this player!");
                        }
                        //implement later
                    }
                } else{
                    p.send(p.name + ", you are not currently in a game! Please wait for someone to challenge you.\nRun \"LIST\" to view all active players.");
                }
            }

            public void HandleGame(String str) throws IOException {
                System.out.println("[DEBUG] " + str);
                int id = Integer.parseInt(str.substring(0, str.indexOf("|")));
                int col = Integer.parseInt(str.substring(str.indexOf("|") + 1));
                p.sendMoveToLinkedPlayer(col);
                p.sendMoveToSelf(col);
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
                if(p.challengeCount != 0){
                    sendstr.append("\n------------------------------------------\n");
                    sendstr.append("You have ").append(p.challengeCount).append(" incoming challenges...\nType \"VIEW\" to view them.\n");
                    sendstr.append("------------------------------------------\n");
                }
                p.send(sendstr.toString());
            }

            public void sendAllChallenges() throws IOException {
                StringBuilder sendstr = new StringBuilder();
                if(p.challengers.size() == 0){
                    p.send("you have no friends...");
                    return;
                }
                sendstr.append("\n------------------------------------------\n");
                sendstr.append("INCOMING CHALLENGES\n");
                sendstr.append("------------------------------------------\n\n");
                for(Player p : p.challengers){
                    sendstr.append("NAME: ").append(p.name).append("\nID: ").append(p.ID).append("\nIn game: ").append(p.LinkedPlayer != null).append("\n");
                }
                sendstr.append("\nUse \"ACCEPT [id]\" to accept a challenge request.");
                p.send(sendstr.toString());
            }
        }
    static ArrayList<Player> players = new ArrayList<>();
    public HashMap<Integer, Integer> linkedPlayers = new HashMap<>();
    static int ID = 0;
    public Server() throws IOException {
        ServerSocket ss=new ServerSocket(8811);
        while(true){
            System.out.println("execute");
            Socket clientSock = ss.accept();
            ConnectionHandler temp = new ConnectionHandler(clientSock);
            connections.add(temp);
            temp.start();
        }
    }
    }
