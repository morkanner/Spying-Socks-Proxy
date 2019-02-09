import java.io.*;
import java.net.*;

public class ProxyHandler implements Runnable {
    private Socks4 m_Socks4;
    private Socket m_ClientSocket;
    private Socket m_ServerSocket = null;
    private byte[] m_Buffer;
    private InputStream m_ClientInput = null;
    private OutputStream m_ClientOutput = null;
    private InputStream m_ServerInput = null;
    private OutputStream m_ServerOutput = null;

    /**
     * Creates a Socks proxy server which is connected to given client socket.
     *
     * @param i_ClientSocket
     *
     * @throws SocketException
     *
     */
    protected ProxyHandler(Socket i_ClientSocket){
        m_ClientSocket = i_ClientSocket;

        if (m_ClientSocket != null){
            try {
                m_ClientSocket.setSoTimeout(Constants.DEFAULT_PROXY_TIMEOUT);
            }
            catch (SocketException e){
                Utils.PrintErrorMessage("cannot set timeout to client");
            }
        }

        m_Buffer = new byte[Constants.DEFAULT_BUF_SIZE];
        m_Socks4 = new Socks4(this);
    }

    /**
     * Runs a thread that handles a proxy server.
     */
    public void run()
    {
        prepareSocket(m_ClientSocket);
        processRelay();
        closeConnections();
    }

    /**
     * Returns the client socket which is connected to the proxy server.
     */
    protected Socket GetClientSocket() { return m_ClientSocket; }

    /**
     * Initializes streams for given socket.
     */
    private void prepareSocket(Socket i_Socket){
        try {
            if (i_Socket != null) {
                if (i_Socket.equals(m_ClientSocket)) {
                    if (m_ClientSocket != null) {
                        m_ClientInput = m_ClientSocket.getInputStream();
                        m_ClientOutput = m_ClientSocket.getOutputStream();
                    }
                } else if (i_Socket.equals(m_ServerSocket)) {
                    if (m_ServerSocket != null) {
                        m_ServerInput = m_ServerSocket.getInputStream();
                        m_ServerOutput = m_ServerSocket.getOutputStream();
                    }
                }
            }
        }
        catch (IOException e) {
            Utils.PrintErrorMessage("Cannot establish a stream for socket");
        }
    }

    /**
     * Processes the packet which was received from the client.
     */
    private void processRelay()	{
        boolean isValidConnection;
        isValidConnection = m_Socks4.GetClientHeader();

        if (isValidConnection) {
            // Handles a connect command of the Socks protocol from the client:
            switch (m_Socks4.m_SocksCommand) {
                case Constants.CD_CONNECT:
                    try {
                        m_Socks4.Connect();
                    } catch (IOException e) {
                    }

                    if (connectionIsValid()) {
                        Utils.PrintSuccessMessage(m_ClientSocket, m_ServerSocket);
                        relay();
                    }

                    break;
                default:
                    Utils.PrintErrorMessage("program handles only connect command");
            }
        }
    }

    /**
     * Prints a relevant message before the closing operation. After that closes
     * the destination server socket and the client socket.
     */
    private void closeConnections() {
        Utils.PrintCloseMessage(m_ClientSocket, m_ServerSocket);
        closeSocketOutput();
        closeSockets();
    }

    /**
     * Closes streams of the destination server socket and the client socket.
     */
    private void closeSocketOutput() {
        try {
            if (m_ClientOutput != null) {
                m_ClientOutput.flush();
                m_ClientOutput.close();
            }
        }
        catch (IOException e) {
            Utils.PrintErrorMessage("client socket stream did not close properly");
        }

        try {
            if (m_ServerOutput != null) {
                m_ServerOutput.flush();
                m_ServerOutput.close();
            }
        }
        catch (IOException e) {
            Utils.PrintErrorMessage("server socket stream did not close properly");
        }
    }

    /**
     * Closes the connection to the destination server socket and the client socket.
     */
    private void closeSockets() {
        try {
            if (m_ClientSocket != null) {
                m_ClientSocket.close();
            }
        }
        catch (IOException e) {
            Utils.PrintErrorMessage("client socket did not close properly");
        }

        try {
            if (m_ServerSocket != null) {
                m_ServerSocket.close();
            }
        }
        catch (IOException e) {
            Utils.PrintErrorMessage("server socket did not close properly");
        }

        m_ServerSocket = null;
        m_ClientSocket = null;
    }

    /**
     * Sends data to given socket.
     *
     * @param i_Socket - a socket to send the given data.
     * @param i_Buffer - the data to be transferred.
     *
     */
    protected void SendData(Socket i_Socket, byte[] i_Buffer){
        sendData(i_Socket, i_Buffer, i_Buffer.length);
    }

    /**
     * Sends data to given socket.
     */
    private void sendData(Socket i_Socket, byte[] i_Buffer, int i_Length) {
        if (i_Socket.equals(m_ClientSocket)) {
            if (m_ClientSocket == null) return;
        }
        else if (i_Socket.equals(m_ServerSocket)) {
            if (m_ServerOutput == null) return;
        }

        if (i_Length <= 0 || i_Length > i_Buffer.length) return;

        try	{
            if (i_Socket.equals(m_ClientSocket)) {
                m_ClientOutput.write(i_Buffer, 0, i_Length);
                m_ClientOutput.flush();
            }
            else if (i_Socket.equals(m_ServerSocket)) {
                m_ServerOutput.write( i_Buffer, 0, i_Length);
                m_ServerOutput.flush();
            }
        }
        catch (IOException e) {
            Utils.PrintErrorMessage("I/O error occurred while sending data");
        }
    }

    /**
     * Connects to remote host.
     *
     * @param i_Server - a remote host to connect.
     * @param i_Port - the destination port.
     *
     * @throws IOException
     *
     */
    protected void ConnectToServer(String i_Server, int i_Port) throws IOException{
        m_ServerSocket = new Socket();
        m_ServerSocket.connect(new InetSocketAddress(i_Server, i_Port),
                Constants.DEFAULT_PROXY_TIMEOUT);
        m_ServerSocket.setSoTimeout(Constants.DEFAULT_PROXY_TIMEOUT);

        prepareSocket(m_ServerSocket);
    }

    private boolean connectionIsValid(){
        return m_ClientSocket != null && m_ClientSocket.isConnected()
                && m_ServerSocket != null && m_ServerSocket.isConnected();
    }

    /**
     * Reads bytes from client socket.
     *
     * @throws Exception
     * @throws InterruptedIOException
     *
     */
    protected byte GetByteFromClient() throws Exception {
        int	b;

        while (m_ClientSocket != null) {
            try	{
                b = m_ClientInput.read();
            }
            catch (InterruptedIOException e) {
                continue;
            }

            return (byte)b;
        }

        throw new Exception("Connection error: Interrupted reading from client");
    }

    /**
     * Handles connection between client and server sockets.
     */
    private void relay() {
        boolean	isActive = true;
        int dataLength;

        while (isActive) {
            // Checks for client data:
            dataLength = checkSocketData(m_ClientSocket);

            if (dataLength < 0) isActive = false;
            if (dataLength > 0) {
                int dstPort = Utils.CalcPort(m_Socks4.m_DSTPort[0], m_Socks4.m_DSTPort[1]);
                if (dstPort == (byte)80) Utils.ExtractUserAndPassword(m_Buffer);
                sendData(m_ServerSocket, m_Buffer, dataLength);
            }

            // Checks for server data:
            dataLength = checkSocketData(m_ServerSocket);

            if (dataLength < 0)	isActive = false;
            if (dataLength > 0) {
                sendData(m_ClientSocket, m_Buffer, dataLength);
            }
        }
    }

    /**
     * Checks data that was read from given socket.
     */
    private int checkSocketData(Socket i_Socket) {
        // The client side is not opened.
        if (i_Socket == null) return -1;

        int	dataLength = 0;

        try
        {
            if (i_Socket.equals(m_ClientSocket)) dataLength =
                    m_ClientInput.read( m_Buffer, 0, Constants.DEFAULT_BUF_SIZE);
            else if (i_Socket.equals(m_ServerSocket)) dataLength =
                    m_ServerInput.read( m_Buffer, 0, Constants.DEFAULT_BUF_SIZE);
        }
        catch (IOException e) {}

        if (dataLength < 0) closeConnections();

        return dataLength;
    }
}