package model;

import SRBanking.ThriftInterface.NodeID;
import SRBanking.ThriftInterface.NodeService;
import SRBanking.ThriftInterface.Swarm;
import SRBanking.ThriftInterface.TransferData;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.ConnectionManager;
import service.SwarmManager;

import java.util.TimerTask;

/**
 * Created by Marcin Janicki on 26.01.15.
 */
//this timer tries to deliver a transfer to destination node
public class DeliverTask extends TimerTask {
    private static Logger log = LoggerFactory.getLogger(DeliverTask.class);

    private NodeID sender;
    private NodeID destinationNode;
    private TransferData transferData;
    private ConnectionManager connectionManager;
    private SwarmManager swarmManager;

    public DeliverTask(NodeID sender, NodeID destinationNode, TransferData transferData, ConnectionManager connectionManager, SwarmManager swarmManager)
    {
        this.sender = sender;
        this.destinationNode = destinationNode;
        this.transferData = transferData;
        this.connectionManager = connectionManager;
        this.swarmManager = swarmManager;
    }


    private String makeKey()
    {
        return transferData.transferID.getSender().getIP() + transferData.transferID.getSender().getPort() + transferData.transferID.getCounter();
    }

    @Override
    public void run() {
        try{
            //try to deliver a transfer
            log.info("Trying to deliver transfer " +  transferData.getTransferID().getSender().getIP() + " "
                    + transferData.getTransferID().getSender().getPort() + " "
                    + transferData.getTransferID().getCounter());
            Connection connection = connectionManager.getConnection(destinationNode);
            NodeService.Client client = connection.getClient();
            client.deliverTransfer(sender, transferData);
            log.info("Transfer " + transferData.getTransferID().getSender().getIP() + " "
                    + transferData.getTransferID().getSender().getPort() + " "
                    + transferData.getTransferID().getCounter() + " delivered");
            connectionManager.closeConnection(connection);
            //if we succeeded we kill the swarm

            for(NodeID member: swarmManager.getSwarm(makeKey()).getMembers())
            {
                try
                {
                    connection = connectionManager.getConnection(member);
                    client = connection.getClient();
                    client.delSwarm(sender, transferData.transferID);
                    connectionManager.closeConnection(connection);
                }
                catch(TException ex)
                {
                    log.info("I guess member " + member.getIP() + ":" + member.getPort() + " is dead");
                }
            }

            //stop the timer
            cancel();
            return;
        }
        catch(TException e)
        {
            log.info("Couldn't deliver a transfer");
        }
    }
}
