package service;

import SRBanking.ThriftInterface.NodeID;
import SRBanking.ThriftInterface.NodeService;
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
    private Map<String, TTransport> connections;

    public ConnectionManager()
    {
        connections = new HashMap<String, TTransport>();
    }

    private String makeKey(NodeID nodeID)
    {
        return nodeID.getIP() + ":" + nodeID.getPort();
    }

    public synchronized NodeService.Client getConnection(NodeID nodeID) throws TException
    {
        TTransport transport = null;
        String key = makeKey(nodeID);
        if(connections.containsKey(key))
        {
            transport = connections.get(key);
        }
        else
        {
            log.info("Opening connection to " + key);
            transport = new TSocket(nodeID.getIP(), nodeID.getPort());
            transport.open();
            connections.put(key, transport);
        }
        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);
        return client;
    }

    public synchronized void cleanUp()
    {
        for(TTransport transport: connections.values())
        {
            transport.close();
        }
    }
}
