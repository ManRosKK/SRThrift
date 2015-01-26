package model;

import SRBanking.ThriftInterface.NodeID;
import SRBanking.ThriftInterface.NodeService;
import SRBanking.ThriftInterface.Swarm;
import SRBanking.ThriftInterface.TransferData;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.ConnectionManager;

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
    private Swarm swarm;

    public DeliverTask(NodeID sender, NodeID destinationNode, TransferData transferData, ConnectionManager connectionManager, Swarm swarm)
    {
        this.sender = sender;
        this.destinationNode = destinationNode;
        this.transferData = transferData;
        this.connectionManager = connectionManager;
        this.swarm = swarm;
    }

    @Override
    public void run() {
        try{
            //try to deliver a transfer
            log.info("Trying to deliver transfer " +  transferData.getTransferID().getSender().getIP() + " "
                    + transferData.getTransferID().getSender().getPort() + " "
                    + transferData.getTransferID().getCounter());
            NodeService.Client client = connectionManager.getConnection(destinationNode);
            client.deliverTransfer(sender, transferData);
            log.info("Transfer " +  transferData.getTransferID().getSender().getIP() + " "
                    + transferData.getTransferID().getSender().getPort() + " "
                    + transferData.getTransferID().getCounter() + " delivered");
            //if we succeeded we kill the swarm
            for(NodeID member: swarm.getMembers())
            {
                client = connectionManager.getConnection(member);
                client.delSwarm(sender, transferData.transferID);
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
