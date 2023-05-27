import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Server {
        static ArrayList<ConnectionHandler> connections = new ArrayList<>();
        public class ConnectionHandler extends Thread {
            public Socket s;
            public Player p;
            int ID;
            DataInputStream din;
            String name;
            public void run() {
                System.out.println("New connection!");
                try {
                    din=new DataInputStream(s.getInputStream());
                    name = din.readUTF();
                    System.out.println("user " + ++IDIncrementer + " registered as " + name + ".");
                    ID = IDIncrementer;
                    p = new Player(s, name, ID);
                    players.add(p);
                    mainLoop();
                } catch (IOException e) {
                    if(p != null){
                        System.out.println(p.name + " has disconnected...");
                        players.remove(p);
                        try {
                            p.abortGame();
                        } catch (IOException ex) {
                            System.out.println(ex);
                        }
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
                sendHelpMsg();
                String str="";
                while(true){
                    str=din.readUTF();
                    System.out.println(str);
                    processSocketInput(str);
                    System.out.println(ID);
                    //add new elseif for VIEW command
                }
            }

            void processSocketInput(String str) throws IOException {
                if(p.LinkedPlayer != null){
                    HandleGame(str);
                } else if(str.substring(str.indexOf("|") + 1).toLowerCase().startsWith("list")){
                    sendAllActivePlayers();
                } else if(str.toLowerCase().startsWith("challenge")){
                    int tempID = ID;
                    Player playertemp;
                    String idToChallenge = str.substring(str.indexOf("|") + 1);
                    int idtc = Integer.parseInt(idToChallenge);
                    if(idtc == p.ID){
                        p.send("\nYou cannot challenge yourself!\n");
                        return;
                    } else if(challengeListContainsID(idtc)){
                        p.send("\nYou already have an outgoing challenge to this player!\n");
                        return;
                    }
                    if((playertemp = findPlayerByID(idtc)) == null){
                        p.send("\nCould not find player with uid " + idtc + "...\n");
                    } else {
                        playertemp.addChallenge(p);
                        p.send("\nChallenge sent to " + playertemp.name + "!\n");
                        playertemp.send("\nYou have " + playertemp.challengers.size() + " new challenge request(s).\n" +
                                "Type \"VIEW\" to view them.\n");
                        //implement later
                    }
                    ID = tempID;
                } else if(str.toLowerCase().startsWith("view")){
                    sendAllChallenges();
                } else if(str.toLowerCase().startsWith("accept")){
                    Player opponent;
                    String idToAccept = str.substring(str.indexOf("|") + 1);
                    int idta = Integer.parseInt(idToAccept);
                    if((opponent = findPlayerByID(idta)) == null){
                        p.send("\ncould not find player with id " + idta + "...\n");
                    } else {
                        int status;
                        if((status = p.acceptChallenge(opponent)) == 0){
                            p.send("\nAccepted " + opponent.name + "'s challenge! \nPress [enter] to continue. \n");
                            p.send("SETOPPONENT|" + opponent.name);
                            p.send("SETIGID|" + p.inGameID);
                            p.send("SETNOTIDLE");
                            opponent.send(p.name + " has accepted your challenge! \nPress [enter] to continue.\n");
                            opponent.send("SETOPPONENT|" + p.name);
                            opponent.send("SETIGID|" + opponent.inGameID);
                            opponent.send("SETNOTIDLE");
                        } else if(status == -1){
                            p.send("\nYou do not have a challenge request from this player!\n");
                        } else{
                            p.send("\nThis player is currently in a game!\n");
                        }
                        //implement later
                    }
                } else{
                    sendHelpMsg();
                }
            }
            public boolean challengeListContainsID(int id){
                for (Player player : findPlayerByID(id).challengers) {
                    if (p.ID == player.ID)
                        return true;
                }
                return false;
            }

            public void sendHelpMsg() throws IOException {
                p.send("PLAYER: " + p.name + "\nUID: " + ID + "\nRun \"list\" to view all active players." +
                        "\nRun \"challenge [uid]\" to send a challenge." +
                        "\nRun \"view\" to view all incoming challenges." +
                        "\nRun \"accept [uid]\" to accept a challenge and join a game.");
            }

            public void HandleGame(String str) throws IOException {

                if(str.toLowerCase().startsWith("full")){
                    p.finishGame();
                    p.send("It's a tie!");
                    return;
                } else if(str.toLowerCase().startsWith("winner|")){
                    int winnerID = Integer.parseInt(str.substring(str.indexOf("|") + 1));
                    String wonmsg = findPlayerByID(winnerID).name + " wins!\nReturning to lobby (i'm too tired to add rematches)...\nPress [enter] to continue...\n";
                    p.send(wonmsg);
                    p.LinkedPlayer.send(wonmsg);
                    p.finishGame();
                    return;
                } else if(str.startsWith("ERROR")){
                    String error = str.substring(str.indexOf("|") + 1);
                    handleError(error);
                    return;
                } else if(str.startsWith("SUCCESS")){
                    p.gridInitialized = true;
                    System.out.println("player " + p.name + "'s grid has been initialized.");
                    return;
                }
                if(!p.LinkedPlayer.gridInitialized){
                    p.send("Your opponent has not started this game yet!");
                    return;
                }

                System.out.println("[DEBUG] " + str);
                int id = Integer.parseInt(str.substring(0, str.indexOf("|")));
                Player movingPlayer = findPlayerByID(id);
                try {
                    if (movingPlayer.canMove) {
                        int col = Integer.parseInt(str.substring(str.indexOf("|") + 1));
                        movingPlayer.LinkedPlayer.send(String.valueOf(col));
                        p.sendMoveToSelf(col);
                        p.sendMoveToLinkedPlayer(col);
                        movingPlayer.canMove = false;
                        movingPlayer.LinkedPlayer.canMove = true;
                    } else {
                        movingPlayer.send("It's not your turn!");
                    }
                } catch(NumberFormatException e){
                    movingPlayer.send("Something went wrong!");
                }
            }
            public Player findPlayerByID(int id){
                for(Player p : players)
                    if(id==p.ID)
                        return p;
                return null;
            }
            public void handleError(String errormsg) throws IOException {
                if(errormsg.equals("UNINITIALIZED")){
                    p.LinkedPlayer.send("Your opponent has not started this game!");
                }
            }
            public void sendAllActivePlayers() throws IOException {
                StringBuilder sendstr = new StringBuilder();
                sendstr.append("\n------------------------------------------\n");
                sendstr.append("ONLINE PLAYERS\n");
                sendstr.append("------------------------------------------\n");
                for(Player p : players){
                    if(p.ID == this.p.ID){
                        sendstr.append("\n(YOU)\n");
                    } else {
                        sendstr.append("\nPLAYER: ").append(p.name).append("\nUID: ").append(p.ID).append("\nIn game: ").append(p.LinkedPlayer != null).append("\n");
                    }
                }
                if(p.challengers.size() != 0){
                    sendstr.append("\n------------------------------------------\n");
                    sendstr.append("You have ").append(p.challengers.size()).append(" incoming challenges...\nType \"VIEW\" to view them.\n");
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
                    sendstr.append("\nPLAYER: ").append(p.name).append("\nUID: ").append(p.ID).append("\nIn game: ").append(p.LinkedPlayer != null).append("\n");
                }
                sendstr.append("\nUse \"ACCEPT [id]\" to accept a challenge request.\n");
                p.send(sendstr.toString());
            }
        }
    static ArrayList<Player> players = new ArrayList<>();
    public HashMap<Integer, Integer> linkedPlayers = new HashMap<>();
    static int IDIncrementer = 0;
    public Server() throws IOException {
        ServerSocket ss=new ServerSocket(8811);
        System.out.println("Ready to accept connections...");
        while(true){
            Socket clientSock = ss.accept();
            ConnectionHandler temp = new ConnectionHandler(clientSock);
            connections.add(temp);
            temp.start();
        }
    }
    }
