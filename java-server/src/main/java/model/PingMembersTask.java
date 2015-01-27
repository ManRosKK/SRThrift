package model;

import SRBanking.ThriftInterface.*;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.ConfigService;
import service.ConnectionManager;

import java.util.List;
import java.util.TimerTask;

/**
 * Created by Marcin Janicki on 26.01.15.
 */
//pings all of the swarm members
public class PingMembersTask extends TimerTask{
    private static Logger log = LoggerFactory.getLogger(PingMembersTask.class);

    private NodeID sender;
    private TransferData transferData;
    private ConnectionManager connectionManager;
    private Swarm swarm;
    private ConfigService configService;

    public PingMembersTask(NodeID sender, TransferData transferData, ConnectionManager connectionManager, Swarm swarm, ConfigService configService) {
        this.sender = sender;
        this.transferData = transferData;
        this.connectionManager = connectionManager;
        this.swarm = swarm;
        this.configService = configService;
    }

    private void addSwarmMember()
    {
        List<NodeID> potentialMembers = configService.getShuffledNodes();
        Configuration config = configService.getConfig();
        for(NodeID node : potentialMembers)
        {
                try
                {
                    //open connection
                    NodeService.Client client = connectionManager.getConnection(node);

                    log.info("Ping and add " + node.getIP() + ":" + node.getPort() + " to swarm after member death");

                    client.ping(this.sender);
                    client.addToSwarm(this.sender, swarm, transferData);
                    swarm.addToMembers(node);
                    log.info("Added " + node.getIP() + ":" + node.getPort() + " to swarm after member death");
                    if(swarm.getMembers().size() == config.getSwarmSize())
                    {
                        break;
                    }
                }
                catch(TException e)
                {
                    log.info("Can't add node " + node.getIP()+ ":" + node.getPort() + " to the swarm after member death");
                }
        }
    }

    private void updateMembers()
    {
        for(NodeID member: swarm.getMembers())
        {
            try
            {
                NodeService.Client client = connectionManager.getConnection(member);
                client.updateSwarmMembers(sender, swarm);
            }
            catch(TException e)
            {
                log.error("Couldn't update member " + member.getIP() + ":" + member.getPort());
            }
        }
    }

    @Override
    public void run()
    {
        //ping all the swarm members
        for(NodeID member: swarm.getMembers())
        {
            //log.info("Just pinging " + member.getIP() + ":" + member.getPort());
            try
            {
                NodeService.Client client = connectionManager.getConnection(member);
                //client.pingSwarm(sender, transferData.getTransferID());
            }
            catch(TException e)
            {
//                swarm.getMembers().remove(member);
//                addSwarmMember();
//                updateMembers();
            }
        }
    }
}
