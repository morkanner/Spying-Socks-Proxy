public class Sockspy {
    public static void main(String[] args){
        ProxyServerInitiator proxyServerInitiator = new ProxyServerInitiator(Constants.PORT);
        new Thread(proxyServerInitiator).start();
    }
}
