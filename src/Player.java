import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Player {
    public Socket sock;
    public String name;
    public int ID;
    public int inGameID;
    public boolean isAlive = true;
    public boolean canMove = false;
    public boolean successful = false;
    DataInputStream din;
    DataOutputStream dout;
    public Player LinkedPlayer;
    ArrayList<Player> challengers = new ArrayList<>();
    public Player(Socket sock, String name, int ID) throws IOException {
        this.sock = sock;
        this.name = name;
        this.ID = ID;
        din=new DataInputStream(sock.getInputStream());
        dout=new DataOutputStream(sock.getOutputStream());
//        dout.writeUTF("Set user name to: " + name);
//        dout.flush();
        //send uid to client
        dout.writeUTF(String.valueOf(ID));
        dout.flush();
    }

    public void send(String str) throws IOException {
        dout.writeUTF(str);
        dout.flush();
    }

    //players are linked to each other
    public boolean LinkPlayers(Player player){
        if(this.LinkedPlayer == null){
            this.LinkedPlayer = player;
            LinkedPlayer.LinkedPlayer = this;
            this.inGameID = 0;
            LinkedPlayer.inGameID = 1;
            this.canMove = true;
            return true;
        }
        return false;
    }

    public void sendMoveToLinkedPlayer(int col) throws IOException {
        LinkedPlayer.send(formatMoveData(inGameID, col));
    }

    public void sendMoveToSelf(int col) throws IOException {
        send(formatMoveData(inGameID, col));
    }

    public String formatMoveData(int playerInGameID, int column){
        return  "MOV:" + inGameID + "|" + column;
    }

    public void addChallenge(Player challenger){
        challengers.add(challenger);
        System.out.println(name);
        //FIX BUG
        //nevermind no bug
    }
    public int acceptChallenge(Player otherPlayer){
        if(otherPlayer.LinkedPlayer != null)
            return -2;
        for(Player x : challengers){
            System.out.println("checking if " + x.ID + " == " + otherPlayer.ID);
            if(x.ID == otherPlayer.ID){
                LinkPlayers(x);
                System.out.println("started game between " + this.name + " and " + x.name + ".") ;
                challengers.remove(x);
                return 0;
            }
        }
        return -1;
    }

    //only call this when someone exits the program.
    public void abortGame() throws IOException {
        if(LinkedPlayer != null){
            LinkedPlayer.send("GAME_ABORTED|PLAYER_DISCONNECT");
            LinkedPlayer.LinkedPlayer = null;
            System.out.println("terminating game...");
        }
    }

    public void finishGame(){
        if(LinkedPlayer != null){
            LinkedPlayer.LinkedPlayer = null;
            LinkedPlayer = null;
        }
    }
}
