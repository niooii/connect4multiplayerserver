import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Player {
    public Socket sock;
    public String name;
    public int ID;
    public int inGameID;
    public boolean isAlive = true;
    DataInputStream din;
    DataOutputStream dout;
    public Player LinkedPlayer;
    public Player(Socket sock, String name, int ID) throws IOException {
        this.sock = sock;
        this.name = name;
        this.ID = ID;
        din=new DataInputStream(sock.getInputStream());
        dout=new DataOutputStream(sock.getOutputStream());
        dout.writeUTF("Set user name to: " + name);
        dout.flush();
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
            LinkedPlayer.inGameID = 1;
            this.inGameID = 0;
            return true;
        }
        return false;
    }

    public void sendMoveToLinkedPlayer(int col) throws IOException {
        LinkedPlayer.send(formatMoveData(inGameID, col));
    }

    public String formatMoveData(int playerInGameID, int column){
        return ID + "|" + inGameID + "|" + column;
    }
}
