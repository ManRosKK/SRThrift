import SRBanking.ThriftInterface.NodeService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sven on 2015-01-09.
 */
public class ThriftTestClient {

    private static Logger log = LoggerFactory.getLogger(ThriftTestClient.class);


    public static void pingserver(int port) throws TException {
        TTransport transport;

        transport = new TSocket("localhost", port);
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);

        log.info("About to ping server");

        try {
            client.Ping();
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
}
