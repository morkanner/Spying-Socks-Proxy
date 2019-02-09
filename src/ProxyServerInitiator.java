import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServerInitiator implements Runnable {
    private ServerSocket m_ListenSocket = null;
    private int m_Port;
    private ExecutorService m_ThreadPool;

    /**
     * Initializes a Socks proxy server that listens at given port with thread pool
     * which At any point, at most predefined number of threads will be active
     * processing tasks (i.e it supports predefined number of concurrent connections).
     *
     * @param i_ListenPort - the port number to listen.
     *
     */
    protected ProxyServerInitiator(int i_ListenPort) {
        m_Port = i_ListenPort;
        m_ThreadPool = Executors.newFixedThreadPool(Constants.MAXIMUM_CONNECTIONS);
    }

    /**
     * Runs the thread which listens and accept connections from clients.
     */
    public void run() {
        listen();
    }

    /**
     * Socks proxy server listens at port mentioned before.
     */
    private void listen() {
        prepareToListen();

        while(isActive()) { checkClientConnection(); }
    }

    private boolean	isActive()	{
        return (m_ListenSocket != null);
    }

    /**
     * Socks server listens at port mentioned before.
     */
    private	void prepareToListen() {
        try {
            m_ListenSocket = new ServerSocket(m_Port);
            m_ListenSocket.setSoTimeout(Constants.DEFAULT_LISTEN_SERVER_TIMEOUT);
        }
        catch (IOException e){
            Utils.PrintErrorMessage("cannot establish proxy server");
        }

        if (m_Port == 0) m_Port = m_ListenSocket.getLocalPort();
    }

    /**
     * Listens for a connection from a client to be made to the listen socket
     * and accepts it. Furthermore, specifies timeout (in milliseconds)
     * to prevent faulty clients from blocking the proxy server.
     * After establishing a connection from a client, the ProxyHandler class
     * handles the connection.
     */
    private void checkClientConnection(){
        if (m_ListenSocket == null) return;

        try {
            Socket clientSocket = m_ListenSocket.accept();
            clientSocket.setSoTimeout(Constants.DEFAULT_SERVER_TIMEOUT);
            m_ThreadPool.execute(new ProxyHandler(clientSocket));
        }
        // Keeps handle connection to client:
        catch (IOException e) {}
    }
}