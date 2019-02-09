import java.net.*;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    /**
     * Prints to console connection success message.
     *
     * @param i_ClientSocket - the given client socket.
     * @param i_ServerSocket - the given server socket.
     *
     */
    protected static void PrintSuccessMessage(Socket i_ClientSocket,
                                              Socket i_ServerSocket){
        String message = String.format("Successful connection from %s to %s\n",
                getSocketDetailsString(i_ClientSocket),
                getSocketDetailsString(i_ServerSocket));
        System.err.printf(message);
    }

    /**
     * Prints to console closing connection message.
     *
     * @param i_ClientSocket - the given client socket.
     * @param i_ServerSocket - the given server socket.
     *
     */
    protected static void PrintCloseMessage(Socket i_ClientSocket,
                                            Socket i_ServerSocket){
        String message;

        if (i_ClientSocket != null && i_ClientSocket.isConnected()) {
            if (i_ServerSocket != null && i_ServerSocket.isConnected()) {
                message = String.format("Closing connection from %s to %s",
                        getSocketDetailsString(i_ClientSocket),
                        getSocketDetailsString(i_ServerSocket));
            }
            else {
                message = String.format("Closing connection from %s",
                        getSocketDetailsString(i_ClientSocket));
            }

            System.err.println(message);
        }
    }

    /**
     * Prints to console error message with default template.
     *
     * @param i_Message - the given error message to print.
     *
     */
    protected static void PrintErrorMessage(String i_Message){
        System.err.println("Connection error: " + i_Message);
    }

    /**
     * Gets the required socket details as a string.
     */
    private static String getSocketDetailsString(Socket i_Socket){
        String socketConnectionDetails = Constants.EMPTY_STRING;

        try{
            if (i_Socket != null && i_Socket.isConnected()) {
                // Resolves localhost 0:0:0:0:0:0:0:1 as 127.0.0.1:
                if (i_Socket.getInetAddress().equals
                        (InetAddress.getByName("0:0:0:0:0:0:0:1"))) {
                    socketConnectionDetails =
                            "127.0.0.1" + ":" + i_Socket.getPort();
                }
                else {
                    socketConnectionDetails =
                            i_Socket.getInetAddress().toString()
                                    .replace("/", Constants.EMPTY_STRING)
                                    + ":" + i_Socket.getPort();
                }
            }
        }
        catch (UnknownHostException e) {
            Utils.PrintErrorMessage("cannot resolve the domain name");
        }

        return socketConnectionDetails;
    }

    /**
     * Calculates the port number.
     *
     * @param i_Hi - the first byte that composes the port number.
     * @param i_Lo - the second byte that composes the port number.
     *
     */
    protected static int CalcPort(byte i_Hi, byte i_Lo){
        return ((Byte2int(i_Hi) << 8) | Byte2int(i_Lo));
    }

    /**
     * Converts a byte to integer.
     *
     * @param i_Byte - the given byte to convert.
     *
     */
    protected static int Byte2int(byte i_Byte){
        int	res = i_Byte;
        if (res < 0) res = 0x100 + res;
        return res;
    }

    /**
     * Extracts username and password from HTTP Basic Authentication field.
     *
     * @param i_Buffer - the given request data from the client.
     *
     */
    protected static void ExtractUserAndPassword(byte[] i_Buffer)
    {
        String encodedUsernamePassword = null;
        String host = null;
        String path = null;
        String request = new String(i_Buffer);

        Pattern p1 = Pattern.compile("GET \\/(.*) ");
        Matcher m1 = p1.matcher(request);

        while (m1.find()){
            path = m1.group(1);
        }

        Pattern p2 = Pattern.compile("Host: ([\\w\\[.\\]\\\\]+)");
        Matcher m2 = p2.matcher(request);

        while (m2.find()){
            host = m2.group(1);
        }

        Pattern p3 = Pattern.compile("Authorization: Basic (\\w*==)");
        Matcher m3 = p3.matcher(request);

        while (m3.find()){
            encodedUsernamePassword = m3.group(1);
        }

        if (encodedUsernamePassword != null) {
            String usernamePassword = new String
                    (Base64.getDecoder().decode(encodedUsernamePassword));
            System.err.println(String.format("Password Found! http://%s@%s/%s",
                    usernamePassword, host, path));
        }
    }
}
