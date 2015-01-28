package service;

import SRBanking.ThriftInterface.NodeID;
import SRBanking.ThriftInterface.NodeService;
import model.Connection;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Marcin Janicki on 26.01.15.
 */
public class ConnectionManager {
    private static Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    public ConnectionManager()
    {
    }

    private String makeKey(NodeID nodeID)
    {
        return nodeID.getIP() + ":" + nodeID.getPort();
    }

    public synchronized Connection getConnection(NodeID nodeID) throws TException
    {
        NodeService.Client client = null;
        String key = makeKey(nodeID);
        log.info("Opening connection to " + key);
        TTransport transport = new TSocket(nodeID.getIP(), nodeID.getPort());
        TProtocol protocol = new TBinaryProtocol(transport);
        client = new NodeService.Client(protocol);
        Connection connection = new Connection(client, transport);
        transport.open();
        return connection;
    }

    public synchronized void closeConnection(Connection connection)
    {
        connection.getTransport().close();
    }
}
