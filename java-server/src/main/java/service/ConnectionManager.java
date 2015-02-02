package service;

import SRBanking.ThriftInterface.NodeID;
import SRBanking.ThriftInterface.NodeService;
import SRBanking.ThriftInterface.NotEnoughMembersToMakeTransfer;
import model.Connection;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Marcin Janicki on 26.01.15.
 */
public class ConnectionManager {
    private static Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private List<NodeID> blackList;
    private boolean stopped;

    public ConnectionManager()
    {
        blackList = new ArrayList<NodeID>();
    }

    private String makeKey(NodeID nodeID)
    {
        return nodeID.getIP() + ":" + nodeID.getPort();
    }

    public synchronized Connection getConnection(NodeID nodeID) throws TException
    {
        NodeService.Client client = null;
        String key = makeKey(nodeID);
        //log.info("Opening connection to " + key);
        TTransport transport = new TSocket(nodeID.getIP(), nodeID.getPort());
        TProtocol protocol = new TBinaryProtocol(transport);
        client = new NodeService.Client(protocol);
        Connection connection = new Connection(client, transport);
        transport.open();
        return connection;
    }

    public synchronized void setBlackList(List<NodeID> blackList)
    {
        this.blackList = blackList;
    }

    private synchronized void checkBlackList(NodeID nodeID) throws NotEnoughMembersToMakeTransfer
    {
        if(blackList.contains(nodeID))
        {
            throw new NotEnoughMembersToMakeTransfer();
        }
    }

    public synchronized void setStopped(boolean stopped)
    {
        this.stopped = stopped;
    }

    private synchronized void isStopped() throws NotEnoughMembersToMakeTransfer
    {
        if(stopped)
        {
            throw new NotEnoughMembersToMakeTransfer();
        }
    }

    public synchronized void checkIfNodeIsAlive(NodeID node) throws NotEnoughMembersToMakeTransfer
    {
        checkBlackList(node);
        isStopped();
    }

    public synchronized void closeConnection(Connection connection)
    {
        connection.getTransport().close();
    }
}
