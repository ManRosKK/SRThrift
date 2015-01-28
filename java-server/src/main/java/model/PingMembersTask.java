package model;

import SRBanking.ThriftInterface.*;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.ConfigService;
import service.ConnectionManager;
import service.SwarmManager;

import java.util.List;
import java.util.TimerTask;

/**
 * Created by Marcin Janicki on 26.01.15.
 */
//pings all of the swarm members
public class PingMembersTask extends TimerTask{
    private static Logger log = LoggerFactory.getLogger(PingMembersTask.class);

    private NodeID sender;
    private ConnectionManager connectionManager;
    private ConfigService configService;
    private SwarmManager swarmManager;
    private String key;

    public PingMembersTask(String key, NodeID sender, ConnectionManager connectionManager, ConfigService configService, SwarmManager swarmManager) {
        this.key = key;
        this.sender = sender;
        this.connectionManager = connectionManager;
        this.configService = configService;
        this.swarmManager = swarmManager;
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
                    swarmManager.addMemberToSwarm(key, node);
                    client.addToSwarm(this.sender, swarmManager.getSwarm(key), swarmManager.getPendingTransfer(key));
                    log.info("Added " + node.getIP() + ":" + node.getPort() + " to swarm after member death");
                    if( swarmManager.getSwarm(key).getMembers().size() == config.getSwarmSize())
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
        Swarm swarm = swarmManager.getSwarm(key);
        for(NodeID member:  swarm.getMembers())
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
        Swarm swarm = swarmManager.getSwarm(key);
        TransferData transferData = swarmManager.getPendingTransfer(key);
        NodeID testNode = swarm.getMembers().size() > 0 ? swarm.getMembers().get(0) : null;
        if(testNode != null)
        {
            try
            {
                NodeService.Client client = connectionManager.getConnection(testNode);
                client.pingSwarm(sender, transferData.getTransferID());
            }
            catch(TException e)
            {

            }

        }
        //ping all the swarm members
//        for(NodeID member: swarm.getMembers())
//        {
//            //log.info("Just pinging " + member.getIP() + ":" + member.getPort());
//            try
//            {
//                NodeService.Client client = connectionManager.getConnection(member);
//                //client.pingSwarm(sender, transferData.getTransferID());
//            }
//            catch(TException e)
//            {
////                swarm.getMembers().remove(member);
////                addSwarmMember();
////                updateMembers();
//            }
//        }
    }
}
