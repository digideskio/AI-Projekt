import java.io.*;
import java.net.*;

public class Main
{
    public static void main(String[] args)
    {
        if (args.length < 1) {
            System.err.println("You need to supply board number as argument");
            return;
        }
        try {
            byte[] boardbytes = new byte[1024];
            String result;
            int boardNumber = Integer.parseInt(args[0]);
            
            Socket socket = new Socket("cvap103.nada.kth.se", 5555);
            InputStream inRaw = socket.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inRaw));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(boardNumber);

            inRaw.read(boardbytes);
            Board board = BoardParser.parse(boardbytes);
            
            String solution = new String(
                    "U R R D U U L D L L U L L D R R R R L D D R U R U D L L U R");

            out.println(solution);

            result = in.readLine();
            
            out.close();
            in.close();
            socket.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
