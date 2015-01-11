import SRBanking.ThriftInterface.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * Created by sven on 2015-01-09.
 */
public class ThriftTestClient {

    private static Logger log = LoggerFactory.getLogger(ThriftTestClient.class);
    public static Preferences prefs;
    static {
        //String filename = "C:\\currentProjects\\SR\\SRThrift\\java-client\\src\\main\\resources\\localSystem.ini";
        String filename = "localSystem.ini";
        try {
            prefs = new IniPreferences(new Ini(new File(filename)));
        } catch (IOException e) {
            System.out.println("Preferences load failure");
            System.exit(1);
        }
    }

    public static void main(String[] args)
    {
        try {
            pingserver(9090);
            System.out.println(getBalance(9090));
            makeTransfer("127.0.0.1",9090,"127.0.0.1",9090,20);
            killserver(9090);
        } catch (TException e) {
            e.printStackTrace();
        }
    }

    public static void pingserver(int port) throws TException {
        TTransport transport;

        transport = new TSocket("localhost", port);
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);

        log.info("About to ping server");

        try {
            client.ping();
        } catch (TException e) {
            e.printStackTrace();
            log.error("Connected but unable to ping??");
            throw e;
        }

        log.info("Server pigned");

        transport.close();
    }

    public static void killserver(int port) throws TException {
        TTransport transport;

        transport = new TSocket("localhost", port);
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);

        log.info("About to kill server");

        client.stop();

        log.info("Server killed");

        transport.close();
    }

    public static long getBalance(int port) throws TException {
        TTransport transport;

        transport = new TSocket("localhost", port);
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);

        long balance = client.getAccountBalance();

        transport.close();

        return balance;
    }

    public static void makeTransfer(String IPS, int portS,String IPR, int portR, long value) throws TException {
        TTransport transport;

        transport = new TSocket(IPS, portS);
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);

        NodeID receiver = new NodeID();
        receiver.setPort(portR);
        receiver.setIP(IPR);

        client.makeTransfer(receiver,value);

        transport.close();
    }
}
