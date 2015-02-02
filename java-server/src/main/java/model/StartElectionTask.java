package model;

import SRBanking.ThriftInterface.*;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.ConfigService;
import service.ConnectionManager;
import service.SwarmManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Marcin Janicki on 31.01.15.
 */
public class StartElectionTask extends TimerTask{
    private static Logger log = LoggerFactory.getLogger(StartElectionTask.class);

    private NodeID sender;
    private String transferKey;
    private SwarmManager swarmManager;
    private ConnectionManager connectionManager;
    private ConfigService configService;

    public StartElectionTask(NodeID sender, String transferKey, SwarmManager swarmManager, ConnectionManager connectionManager, ConfigService configService) {
        this.sender = sender;
        this.transferKey = transferKey;
        this.swarmManager = swarmManager;
        this.connectionManager = connectionManager;
        this.configService = configService;
    }

    private void createStartElectionTask()
    {
        Timer timer = new Timer();
        timer.schedule(new StartElectionTask(sender, transferKey, swarmManager, connectionManager, configService), configService.getConfig().getSwarmPingTimeout());
        swarmManager.updateElectionTimer(transferKey, timer);
    }

    @Override
    public void run() {
        Swarm swarm = swarmManager.getSwarm(transferKey);
        TransferData transferData = swarmManager.getPendingTransfer(transferKey);
        //Let the succession war begin...
        log.info("Let's check if my leader is alive - he doesn't ping me");
        try {
            connectionManager.checkIfNodeIsAlive(swarm.getLeader());
            Connection connection = connectionManager.getConnection(swarm.getLeader());
            NodeService.Client client = connection.getClient();
            client.ping(sender);
            connectionManager.closeConnection(connection);
        }
        catch (TException e)
        {
            log.info("My leader is dead! I've got to choose a new one");
            for(NodeID member: swarm.getMembers())
            {
                try
                {
                    connectionManager.checkIfNodeIsAlive(member);
                    Connection connection = connectionManager.getConnection(member);
                    NodeService.Client client = connection.getClient();
                    client.startSwarmElection(transferData.getTransferID());
                    connectionManager.closeConnection(connection);
                }
                catch(TException ex)
                {
                    log.info("Can't deliver start election message to " + member.getIP()+ ":" + member.getPort());
                }
            }

            log.info("Let's candidate first");
            try
            {
                connectionManager.checkIfNodeIsAlive(sender);
                Connection connection = connectionManager.getConnection(sender);
                NodeService.Client client = connection.getClient();
                client.electSwarmLeader(sender, sender, transferData.getTransferID());
                connectionManager.closeConnection(connection);
            }
            catch(TException ex)
            {
                log.error("Weird - I can't deliver a message to myself...");
            }
            return;
        }

        //leader is not dead - let's restart the election timer
        createStartElectionTask();
    }
}
