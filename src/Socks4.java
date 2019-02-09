import java.io.*;
import java.net.*;

public class Socks4 {
    private ProxyHandler m_ProxyHandler;

    protected byte m_SocksVersion;
    protected byte m_SocksCommand;
    protected byte m_DSTPort[];
    protected byte m_DSTIP[];
    protected StringBuilder m_HostName;

    protected InetAddress m_ServerIP = null;
    protected int m_ServerPort = 0;

    protected InetAddress m_ClientIP = null;
    protected int m_ClientPort = 0;

    /**
     * Creates a Socks4 instance.
     *
     * @param i_ProxyHandler - the proxy handler that uses protocol Socks 4.
     *
     */
    protected Socks4(ProxyHandler i_ProxyHandler){
        m_ProxyHandler = i_ProxyHandler;
        m_DSTIP = new byte[4];
        m_DSTPort = new byte[2];
        m_HostName = new StringBuilder();
    }

    /**
     * Reads header request from the client.
     */
    protected boolean GetClientHeader(){
        boolean isValidConnection = false;

        m_SocksVersion = getByte();

        if (m_SocksVersion != Constants.SOCKS4_Version){
            replyCommand(Constants.REQUEST_REJECTED_OR_FAILED_CODE);
            Utils.PrintErrorMessage(String.format("while parsing request: " +
                            "Unsupported SOCKS protocol version (got %02X)",
                    m_SocksVersion));
        }
        else {
            m_SocksCommand = getByte();
            m_DSTPort[0] = getByte();
            m_DSTPort[1] = getByte();

            for (int i = 0; i < 4; i++) {
                m_DSTIP[i] = getByte();
            }

            // Skips User ID:
            while (getByte() != 0x00){}

            // In case that the request packet is Socks version 4A:
            if (isVersion4A()){
                /* BONUS */
                byte b;
                // Reads the domain name from the header request:
                while ((b = getByte()) != 0x00) {
                    m_HostName.append((char) b);
                }
            }

            setAddressAndPort();

            isValidConnection = true;
        }

        return isValidConnection;
    }

    /**
     * Reads a byte from the client.
     */
    private byte getByte(){
        byte byteToReturn;

        try	{
            byteToReturn = m_ProxyHandler.GetByteFromClient();
        }
        catch(Exception e){
            Utils.PrintErrorMessage("cannot read data from client");
            byteToReturn = 0;
        }

        return byteToReturn;
    }

    /**
     * Composes a replay packet which is sent to the client
     * according to the given reply code.
     */
    private void replyCommand(byte i_ReplyCode)
    {
        byte[] reply = new byte[8];
        reply[0]= 0;
        reply[1]= i_ReplyCode;
        reply[2]= m_DSTPort[0];
        reply[3]= m_DSTPort[1];
        reply[4]= m_DSTIP[0];
        reply[5]= m_DSTIP[1];
        reply[6]= m_DSTIP[2];
        reply[7]= m_DSTIP[3];

        m_ProxyHandler.SendData(m_ProxyHandler.GetClientSocket(), reply);
    }

    /**
     * Checks if the given request is Socks 4A version.
     */
    private boolean isVersion4A(){
        return m_DSTIP[0] == 0 && m_DSTIP[1] == 0
                && m_DSTIP[2] == 0 && m_DSTIP[3] != 0;
    }

    /**
     * Sets the IP addresses and ports according to the given request.
     */
    private boolean setAddressAndPort() {
        // Sets server IP address and port:
        if (isVersion4A()){ // In case that the request packet is Socks version 4A
            try {
                /* BONUS */
                if (m_HostName.toString() != Constants.EMPTY_STRING) {
                    // Resolves the domain name:
                    m_ServerIP = InetAddress.getByName(m_HostName.toString());
                }
            }
            catch (UnknownHostException e) {
                Utils.PrintErrorMessage("cannot resolve the domain name");
            }
        }
        else {
            m_ServerIP = calcInetAddress(m_DSTIP);
        }

        m_ServerPort = Utils.CalcPort(m_DSTPort[0], m_DSTPort[1]);

        m_ClientIP = m_ProxyHandler.GetClientSocket().getInetAddress();
        m_ClientPort = m_ProxyHandler.GetClientSocket().getPort();

        return ((m_ServerIP != null) && (m_ServerPort >= 0));
    }

    /**
     * Calculates the Inet IP address.
     */
    private InetAddress calcInetAddress(byte[] i_Address){
        InetAddress	inetAddress;
        StringBuilder inetAddressString = new StringBuilder();

        if (i_Address.length < 4) {
            replyCommand((byte)(91));
            Utils.PrintErrorMessage("Invalid length of IPV4 - "
                    + i_Address.length + " bytes");
            return null;
        }

        for (int i = 0; i < 4; i++) {
            inetAddressString.append(Utils.Byte2int(i_Address[i]));
            if (i < 3) inetAddressString.append(".");
        }

        try	{
            inetAddress = InetAddress.getByName(inetAddressString.toString());
        }
        catch (UnknownHostException e) {
            Utils.PrintErrorMessage("cannot resolve the domain name");
            return null;
        }

        return inetAddress;
    }

    /**
     * Connects to the remote host.
     */
    protected void Connect() throws IOException {
        try	{
            m_ProxyHandler.ConnectToServer(m_ServerIP.getHostAddress(), m_ServerPort);
            replyCommand(Constants.REQUEST_GRANTED_CODE);
        }
        catch (SocketTimeoutException e){
            replyCommand(Constants.REQUEST_REJECTED_OR_FAILED_CODE);
            Utils.PrintErrorMessage("while connecting to destination: " +
                    "connect timed out");
        }
    }
}