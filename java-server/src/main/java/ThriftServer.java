import SRBanking.ThriftInterface.NodeService;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sven on 2015-01-09.
 */
public class ThriftServer {

    private static Logger log = LoggerFactory.getLogger(ThriftServer.class);

    public static void main(String [] args) {
        try {
            ThriftServerHandler handler = new ThriftServerHandler();
            final NodeService.Processor processor = new NodeService.Processor(handler);

            Runnable simple = new Runnable() {
                public void run() {
                    simple(processor,9090);
                }
            };

            new Thread(simple).start();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private static void simple(NodeService.Processor processor, int port) {
        try {
            TServerTransport serverTransport = new TServerSocket(port);
            TServer server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));

            log.info("Starting the simple server " + port);
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}