public interface Constants {
    // Proxy server initialization parameters:
    int PORT = 8080;
    int MAXIMUM_CONNECTIONS = 20;
    int DEFAULT_LISTEN_SERVER_TIMEOUT = 200;
    int DEFAULT_SERVER_TIMEOUT = 200;
    int DEFAULT_BUF_SIZE = 8192;
    int DEFAULT_PROXY_TIMEOUT = 200;

    // Socks version 4 parameters:
    byte SOCKS4_Version = 0x04;
    byte REQUEST_GRANTED_CODE = 90;
    byte REQUEST_REJECTED_OR_FAILED_CODE = 91;
    byte CD_CONNECT = 0x01;

    // A special parameter for an empty string:
    String EMPTY_STRING = "";
}
