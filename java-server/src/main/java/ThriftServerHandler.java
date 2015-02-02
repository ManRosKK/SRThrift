import SRBanking.ThriftInterface.*;
import model.*;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.ConfigService;
import service.ConnectionManager;
import service.SwarmManager;

import java.util.*;

/**
 * Created by sven on 2015-01-09.
 */
public class ThriftServerHandler implements NodeService.Iface{

    private static Logger log = LoggerFactory.getLogger(ThriftServerHandler.class);
    private final NodeID nodeID;
    private Account account;
    private long messageCounter;
    private ConfigService configService;
    private ConnectionManager connectionManager;
    private SwarmManager swarmManager;

    public ThriftServerHandler(String ip, int port, long accountBalance, String iniPath) {
        account = new Account(accountBalance);
        configService  = new ConfigService(iniPath);
        configService.readConfiguration();
        this.nodeID = new NodeID();
        this.nodeID.setIP(ip);
        this.nodeID.setPort(port);
        this.messageCounter = 0;

        connectionManager = new ConnectionManager();
        swarmManager = new SwarmManager();
    }

    private Swarm createSwarm(TransferData transferData) throws TException
    {
        Swarm swarm = new Swarm();
        List<NodeID> members = new ArrayList<NodeID>();
        members.add(this.nodeID);
        getPotentialMemberList(members);
        Configuration config = configService.getConfig();

        if(members.size() < config.getSwarmSize())
        {
            log.error("Not enough members to make a swarm!");
            throw new NotEnoughMembersToMakeTransfer(members.size(), config.getSwarmSize());
        }

        swarm.setTransfer(transferData.getTransferID());
        swarm.setLeader(this.nodeID);
        swarm.setMembers(members);

        //i've got to add them here because of swarm in add method
        for(NodeID member: swarm.getMembers())
        {
            try
            {
                //I don't have to check if I'm alive - I just add myself to the swarm
                if(!member.equals(this.nodeID))
                {
                    connectionManager.checkIfNodeIsAlive(member);
                    Connection connection = connectionManager.getConnection(member);
                    NodeService.Client client = connection.getClient();
                    client.addToSwarm(this.nodeID, swarm, transferData);
                    connectionManager.closeConnection(connection);
                }

            }
            catch(TException e)
            {
                log.error("Man down after pinged - I can't make a swarm " + member.getIP() + ":" + member.getPort());
                log.error(e.getMessage());
                throw new NotEnoughMembersToMakeTransfer(members.size(), config.getSwarmSize());
            }
        }
        log.info("Swarm created.");
        swarmManager.updateSwarm(account.makeTransferKey(transferData.getTransferID()),swarm);
        return swarm;
    }

    private void createDeliverTask(NodeID destinationNode, TransferData transferData)
    {
        Timer timer = new Timer();
        timer.schedule(new DeliverTask(this.nodeID, destinationNode, transferData, connectionManager, swarmManager), new Date(), configService.getConfig().getDeliveryInterval());
    }

    private void createPingSwarmTask(TransferData transferData)
    {
        Timer timer = new Timer();
        String key = account.makeTransferKey(transferData.getTransferID());
        timer.schedule(new PingMembersTask(key, this.nodeID, connectionManager, configService, swarmManager), new Date(), configService.getConfig().getSwarmPingInterval());
        swarmManager.updateTimer(key, timer);
    }

    private void createStartElectionTask(TransferID transferID)
    {
        Timer timer = new Timer();
        String key = account.makeTransferKey(transferID);
        timer.schedule(new StartElectionTask(this.nodeID ,key, swarmManager, connectionManager, configService), configService.getConfig().getSwarmPingTimeout());
        swarmManager.updateElectionTimer(key, timer);
    }

    private boolean compareNodeID(NodeID nodeA, NodeID nodeB)
    {
        String keyA = nodeA.getIP() + nodeA.getPort();
        String keyB = nodeB.getIP() + nodeB.getPort();
        return keyA.compareTo(keyB) < 0;
    }

    private boolean amILeader(Swarm swarm, TransferID transfer)
    {
        boolean leader = true;
        for(NodeID member : swarm.getMembers())
        {
            //if someone would be a better tyrant than me I've got to let him know
            if(compareNodeID(member, this.nodeID))
            {
                try
                {
                    connectionManager.checkIfNodeIsAlive(member);
                    Connection connection = connectionManager.getConnection(member);
                    NodeService.Client client = connection.getClient();
                    leader &= client.electSwarmLeader(this.nodeID, this.nodeID, transfer);
                    connectionManager.closeConnection(connection);
                }
                catch(Exception e)
                {
                    log.info("I guess member " + member.getIP() + ":" + member.getPort() + " is dead");
                }
            }
        }
        return leader;
    }

    private void removeMembers(Swarm swarm)
    {
        log.info("Removing dead members");
        List<NodeID> membersToRemove = new ArrayList<NodeID>();
        for(NodeID member : swarm.getMembers())
        {
            try
            {
                connectionManager.checkIfNodeIsAlive(member);
                Connection connection = connectionManager.getConnection(member);
                NodeService.Client client = connection.getClient();
                client.ping(this.nodeID);
                connectionManager.closeConnection(connection);
            }
            catch(Exception e)
            {
                log.info("I guess member " + member.getIP() + ":" + member.getPort() + " is dead.Let's remove him from a swarm.");
                membersToRemove.add(member);
            }
        }
        swarm.getMembers().removeAll(membersToRemove);
    }

    private void getPotentialMemberList(List<NodeID> currentMembers)
    {
        List<NodeID> potentialMembers = configService.getShuffledNodes();
        Configuration config = configService.getConfig();
        for(NodeID node : potentialMembers)
        {
            //if potential member is not myself try to add a new member to the swarm
            if(!currentMembers.contains(node))
            {
                try
                {
                    //open connection
                    connectionManager.checkIfNodeIsAlive(node);
                    Connection connection = connectionManager.getConnection(node);
                    NodeService.Client client = connection.getClient();
                    client.ping(this.nodeID);
                    currentMembers.add(node);
                    connectionManager.closeConnection(connection);
                    if(currentMembers.size() == config.getSwarmSize())
                    {
                        break;
                    }
                }
                catch(TException e)
                {
                }
            }
        }
    }

    private void refillSwarm(Swarm swarm, TransferData transferData)
    {
        log.info("Reffilling swarm");
        getPotentialMemberList(swarm.getMembers());

        for(NodeID member: swarm.getMembers())
        {
            try
            {
                //I don't have to check if I'm alive - I just add myself to the swarm
                if(!member.equals(this.nodeID))
                {
                    connectionManager.checkIfNodeIsAlive(member);
                    Connection connection = connectionManager.getConnection(member);
                    NodeService.Client client = connection.getClient();
                    client.addToSwarm(this.nodeID, swarm, transferData);
                    connectionManager.closeConnection(connection);
                    log.info("Added " + member.getIP() + ":" + member.getPort() + " during refill");
                }

            }
            catch(TException e)
            {
            }
        }
    }

    private void endElection(Swarm swarm)
    {
        log.info("End election");
        //Now let's update only
        for(NodeID member : swarm.getMembers())
        {
            try
            {
                connectionManager.checkIfNodeIsAlive(member);
                Connection connection = connectionManager.getConnection(member);
                NodeService.Client client = connection.getClient();
                client.electionEndedSwarm(this.nodeID, swarm);
                connectionManager.closeConnection(connection);
            }
            catch(Exception e)
            {
                log.info("I guess member " + member.getIP() + ":" + member.getPort() + " is dead");
            }
        }
    }

    @Override
    public void ping(NodeID sender) throws TException {
        //intentionally empty
        connectionManager.checkIfNodeIsAlive(sender);
        //log.info("I'm " + this.nodeID.getIP() + ":" + this.nodeID.getPort() + " and " + sender.getIP() + ":" + sender.getPort() + " has pinged me");
    }

    @Override
    public void pingSwarm(NodeID leader, TransferID transfer) throws NotSwarmMemeber, TException {
        String key = account.makeTransferKey(transfer);
        connectionManager.checkIfNodeIsAlive(leader);
        if(swarmManager.getPendingTransfer(key) == null || swarmManager.getSwarm(key) == null)
        {
            throw new NotSwarmMemeber(leader, transfer);
        }
        log.info("My leader " +  leader.getIP() + ":" + leader.getPort() + " has pinged me " + this.nodeID.getIP() + ":" + this.nodeID.getPort());
        createStartElectionTask(transfer);
    }

    @Override
    public void updateSwarmMembers(NodeID sender, Swarm swarm) throws NotSwarmMemeber, WrongSwarmLeader, TException {
        String key = account.makeTransferKey(swarm.getTransfer());
        connectionManager.checkIfNodeIsAlive(sender);
        if(swarmManager.getPendingTransfer(key) == null || swarmManager.getSwarm(key) == null)
        {
            throw new NotSwarmMemeber(sender, swarm.getTransfer());
        }
        swarmManager.updateSwarm(key, swarm);
        createStartElectionTask(swarm.getTransfer());
    }

    @Override
    public void addToSwarm(NodeID sender, Swarm swarm, TransferData transferData) throws AlreadySwarmMemeber, TException {
        connectionManager.checkIfNodeIsAlive(sender);
        //if transferData is null
        if(transferData == null)
        {
            throw new TException("Swarm has been deleted in a meantime");
        }
        //I added myself to the swarm earlier
        String key = account.makeTransferKey(transferData.getTransferID());
        if(swarmManager.getPendingTransfer(key) != null)
        {
            log.error("Node " + this.nodeID.getIP() + ":" + this.nodeID.getPort() + " is already a swarm member");
            throw new AlreadySwarmMemeber(this.nodeID, swarm.getLeader(), swarm.getTransfer());
        }
        swarmManager.updatePendingTransfers(key, transferData);
        swarmManager.updateSwarm(key, swarm);
        createStartElectionTask(transferData.getTransferID());
        log.info("I (" + this.nodeID.getIP() + ":" + this.nodeID.getPort() + ") am a swarm member now");
    }

    @Override
    public void delSwarm(NodeID sender, TransferID swarmID) throws NotSwarmMemeber, WrongSwarmLeader, TException {
        connectionManager.checkIfNodeIsAlive(sender);
        String key = account.makeTransferKey(swarmID);
        if(swarmManager.getPendingTransfer(key) == null || swarmManager.getSwarm(key) == null)
        {
            throw new NotSwarmMemeber(sender, swarmID);
        }
        Swarm swarm = swarmManager.getSwarm(key);
        if(!swarm.getLeader().getIP().equals(sender.getIP())
                || swarm.getLeader().getPort() != sender.getPort())
        {
            throw new WrongSwarmLeader(this.nodeID, sender, swarmID);
        }
        log.info("Transfer delivered - cleaning swarm");
        swarmManager.deliverTransfer(key);
        swarmManager.killSwarm(key);
        swarmManager.stopAndKillTimer(key);
        swarmManager.stopAndKillElectionTimer(key);
    }

    @Override
    public Swarm getSwarm(NodeID sender, TransferID transfer) throws NotSwarmMemeber, TException {
        Swarm swarm = swarmManager.getSwarm(account.makeTransferKey(transfer));
        if(swarm == null)
        {
            throw new NotSwarmMemeber(this.nodeID, transfer);
        }
        return swarm;
    }

    @Override
    public boolean electSwarmLeader(NodeID sender, NodeID cadidate, TransferID Transfer) throws NotSwarmMemeber, TException {
        connectionManager.checkIfNodeIsAlive(sender);
        String key = account.makeTransferKey(Transfer);
        if(!swarmManager.isElectionPending(key))
        {
            return false;
        }
        Swarm swarm = swarmManager.getSwarm(key);
        if(swarm == null)
        {
            throw new NotSwarmMemeber(this.nodeID, Transfer);
        }
        if((this.nodeID.getIP().equals(cadidate.getIP()) && this.nodeID.getPort() == cadidate.getPort() //I'm the sender - i've got to start an election
                || compareNodeID(this.nodeID, cadidate))) //or my nodeID is smaller than candidate's one
        {
            boolean amILeader = amILeader(swarm, Transfer);
            if(amILeader)
            {
                swarmManager.stopElection(key);
                TransferData transferData = swarmManager.getPendingTransfer(key);
                swarm.setLeader(this.nodeID);
                //let's get rid of inactive nodes
                removeMembers(swarm);
                //add new ones if neccessary
                refillSwarm(swarm, transferData);
                endElection(swarm);
                swarmManager.updateSwarm(key, swarm);
                createDeliverTask(transferData.getReceiver(), transferData);
                createPingSwarmTask(transferData);
                log.info("Election has ended, I am new leader");
            }
            return false;
        }
        return true; //if my nodeID is bigger than he's more despotic than I am
    }

    @Override
    public void electionEndedSwarm(NodeID sender, Swarm swarm) throws NotSwarmMemeber, TException {
        connectionManager.checkIfNodeIsAlive(sender);
        String key = account.makeTransferKey(swarm.getTransfer());
        if(!swarmManager.isElectionPending(key))
        {
            return;
        }
        Swarm localSwarm = swarmManager.getSwarm(key);
        if(localSwarm == null)
        {
            throw new NotSwarmMemeber(this.nodeID, swarm.getTransfer());
        }
        log.info("Election has ended, new leader is " + swarm.getLeader().getIP() + ":" + swarm.getLeader().getPort());
        swarmManager.updateSwarm(key, swarm);
        swarmManager.stopElection(key);
        createStartElectionTask(swarm.getTransfer());
    }

    @Override
    public void deliverTransfer(NodeID sender, TransferData transfer) throws TException {
        connectionManager.checkIfNodeIsAlive(sender);
        if(!account.isTransferInHistory(transfer))
        {
            account.takeTransfer(transfer);
            log.info("Transfer " + account.makeTransferKey(transfer.getTransferID()) + " delivered");
        }
    }

    @Override
    public void makeTransfer(NodeID receiver, long value) throws TException {
        connectionManager.checkIfNodeIsAlive(receiver);
        //set params
        TransferData transferData = new TransferData();
        TransferID transferID = new TransferID();
        transferID.setSender(this.nodeID);
        transferID.setCounter(messageCounter++);
        transferData.setReceiver(receiver);
        transferData.setTransferID(transferID);
        transferData.setValue(value);

        if(account.isTransferPossible(value))
        {
            try {
                //open connection
                Connection connection = connectionManager.getConnection(receiver);
                NodeService.Client client = connection.getClient();

                //set params
                log.info("About to make transfer");

                client.deliverTransfer(this.nodeID, transferData);
                log.info("Transfer delivered!");
                connectionManager.closeConnection(connection);
                log.info("Transfer complete");
            } catch (TException e) {
                swarmManager.updatePendingTransfers(account.makeTransferKey(transferData.getTransferID()), transferData);
                Swarm swarm = createSwarm(transferData);
                createDeliverTask(receiver, transferData);
                createPingSwarmTask(transferData);
                log.info("Transfer failed, added to pending transfers");
            }
            account.setBalance(account.getBalance() - value);
        }
        else
        {
            log.info("I don't have enough money to make a transfer");
            throw new NotEnoughMoney(account.getBalance(), value);
        }
    }

    @Override
    public long getAccountBalance() throws TException {
        return account.getBalance();
    }

    @Override
    public List<Swarm> getSwarmList() throws TException {
        return swarmManager.getSwarms();
    }

    @Override
    public void startSwarmElection(TransferID transfer) throws NotSwarmMemeber, TException {
        log.info("Start the succession war - candidate " + this.nodeID.getIP() + ":" + this.nodeID.getPort());
        String key = account.makeTransferKey(transfer);
        //we don't need election timer any more - an election has just started
        swarmManager.stopAndKillElectionTimer(key);
        swarmManager.startElection(key);
    }

    @Override
    public List<TransferData> getTransfers() throws TException {
        return account.getTransferHistory();
    }

    @Override
    public void setBlacklist(List<NodeID> blacklist) throws TException {
        connectionManager.setBlackList(blacklist);
    }

    @Override
    public void virtualStop(boolean shouldStop) throws TException {
        connectionManager.setStopped(shouldStop);
    }

    @Override
    public void stop() throws TException {
        log.info("Server " + this.nodeID.getIP() + ":" + this.nodeID.getPort() + " stopped");
        System.exit(0);
    }
}
