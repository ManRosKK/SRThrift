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
    private Map<String, Connection> connections;

    public ConnectionManager()
    {
        connections = new HashMap<String, Connection>();
    }

    private String makeKey(NodeID nodeID)
    {
        return nodeID.getIP() + ":" + nodeID.getPort();
    }

    public synchronized NodeService.Client getConnection(NodeID nodeID) throws TException
    {
        NodeService.Client client = null;
        String key = makeKey(nodeID);
        if(connections.containsKey(key))
        {
            Connection connection = connections.get(key);
            TTransport transport = connection.getTransport();
            if(!transport.isOpen())
            {
                log.info("Connection to "  + key + " closed. Opening new one.");
                TProtocol protocol = new TBinaryProtocol(transport);
                client = new NodeService.Client(protocol);
                connection.setClient(client);
                transport.open();
            }
            else
            {
                log.info("Client for " + key + " returned.");
                client = connection.getClient();
            }
        }
        else
        {
            log.info("Opening connection to " + key);
            TTransport transport = new TSocket(nodeID.getIP(), nodeID.getPort());
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new NodeService.Client(protocol);
            Connection connection = new Connection(client, transport);
            connections.put(key, connection);
            transport.open();
        }

        return client;
    }

    public synchronized void cleanUp()
    {
        for(Connection connection: connections.values())
        {
            connection.getTransport().close();
        }
        connections.clear();
    }
}
