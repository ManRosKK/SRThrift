import SRBanking.ThriftInterface.NodeService;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
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

        String arg0 = "";
        Integer arg1 = 0;
        Integer arg2 = 0;
        String arg3 = "";

        if (args.length ==  4)
        {
            try {
                arg0 = args[0];
                arg1 = Integer.parseInt(args[1]);
                arg2 = Integer.parseInt(args[2]);
                arg3 = args[3];
            } catch (NumberFormatException e) {
                System.err.println("Argument 1 and argument 2 " + args[0] + " must be integers.");
                System.exit(1);
            }
        }
        else
        {
            System.err.println("Usage: ./server ip port balance .iniFilePath");
            System.exit(1);
        }

        final String IP = arg0;
        final Integer port = arg1;
        final Integer balance = arg2;
        final String iniPath = arg3;

        try {
            ThriftServerHandler handler = new ThriftServerHandler(IP, port, balance, iniPath);
            final NodeService.Processor processor = new NodeService.Processor(handler);

            Runnable simple = new Runnable() {
                public void run() {
                    simple(processor,port);
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
            TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

            log.info("Starting the simple server " + port);
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}