import SRBanking.ThriftInterface.*;
import model.Account;
import model.Configuration;
import model.DeliverTask;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.ConfigService;
import service.ConnectionManager;

import java.util.*;

/**
 * Created by sven on 2015-01-09.
 */
public class ThriftServerHandler implements NodeService.Iface{

    private static Logger log = LoggerFactory.getLogger(ThriftServerHandler.class);
    private final NodeID nodeID;
    private Configuration config;
    private Account account;
    private long messageCounter;
    private ConfigService configService;
    private ConnectionManager connectionManager;

    private Map<String, TransferData> pendingTransfers;
    private Map<String, Swarm> mySwarms;

    public ThriftServerHandler(String ip, int port, long accountBalance, String iniPath) {
        account = new Account(accountBalance);
        configService  = new ConfigService(iniPath);
        config = configService.readConfiguration();
        this.nodeID = new NodeID();
        this.nodeID.setIP(ip);
        this.nodeID.setPort(port);
        this.messageCounter = 0;

        connectionManager = new ConnectionManager();

        //key in both maps is transferID saved as string
        pendingTransfers = new HashMap<String, TransferData>();
        mySwarms = new HashMap<String, Swarm>();
    }

    private Swarm createSwarm(TransferData transferData) throws TException
    {
        Swarm swarm = new Swarm();
        List<NodeID> members = new ArrayList<NodeID>();
        members.add(this.nodeID);
        List<NodeID> potentialMembers = configService.getShuffledNodes(config);
        for(NodeID node : potentialMembers)
        {
            //if potential member is not myself try to add a new member to the swarm
            if(!(this.nodeID.getIP().equals(node.getIP()) && this.nodeID.getPort() == node.getPort()))
            {
                try
                {
                    //open connection
                    NodeService.Client client = connectionManager.getConnection(node);

                    log.info("Ping and add " + node.getIP() + ":" + node.getPort() + " to swarm");

                    client.ping(this.nodeID);
                    client.addToSwarm(this.nodeID, swarm, transferData);
                    members.add(node);
                    log.info("Added " + node.getIP() + ":" + node.getPort() + " to swarm");
                    if(members.size() == config.getSwarmSize())
                    {
                        break;
                    }
                }
                catch(TException e)
                {
                    log.info("Can't add node " + node.getIP()+ ":" + node.getPort() + " to the swarm");
                }
            }
        }

        if(members.size() < config.getSwarmSize())
        {
            log.error("Not enough members to make a swarm!");
            throw new NotEnoughMembersToMakeTransfer(members.size(), config.getSwarmSize());
        }
        log.info("Swarm created.");
        swarm.setTransfer(transferData.getTransferID());
        swarm.setLeader(this.nodeID);
        swarm.setMembers(members);
        mySwarms.put(account.makeTransferKey(transferData.getTransferID()),swarm);
        return swarm;
    }

    private void createDeliverTask(NodeID destinationNode, TransferData transferData, Swarm swarm)
    {
        Timer timer = new Timer();
        timer.schedule(new DeliverTask(this.nodeID, destinationNode, transferData, connectionManager, swarm), new Date(), config.getDeliveryInterval());
    }

    @Override
    public void ping(NodeID sender) throws TException {
        //intentionally empty
        log.info(sender.getIP() + ":" + sender.getPort() + " has pinged me");
    }

    @Override
    public void pingSwarm(NodeID leader, TransferID transfer) throws NotSwarmMemeber, TException {

    }

    @Override
    public void updateSwarmMembers(NodeID sender, Swarm swarm) throws NotSwarmMemeber, WrongSwarmLeader, TException {

    }

    @Override
    public void addToSwarm(NodeID sender, Swarm swarm, TransferData transferData) throws AlreadySwarmMemeber, TException {
        //I added myself to the swarm earlier
        if(this.nodeID.getIP().equals(sender.getIP())
                && this.nodeID.getPort() == sender.getPort())
        {
            return;
        }

        if(pendingTransfers.containsKey(account.makeTransferKey(transferData.getTransferID())))
        {
            log.error("Node " + this.nodeID.getIP() + ":" + this.nodeID.getPort() + " is already a swarm member");
            throw new AlreadySwarmMemeber(this.nodeID, swarm.getLeader(), swarm.getTransfer());
        }
        pendingTransfers.put(account.makeTransferKey(transferData.getTransferID()), transferData);
        mySwarms.put(account.makeTransferKey(transferData.getTransferID()), swarm);
    }

    @Override
    public void delSwarm(NodeID sender, TransferID swarmID) throws NotSwarmMemeber, WrongSwarmLeader, TException {
        String key = account.makeTransferKey(swarmID);
        pendingTransfers.remove(key);
        mySwarms.remove(key);
    }

    @Override
    public Swarm getSwarm(NodeID sender, TransferID transfer) throws NotSwarmMemeber, TException {
        return null;
    }

    @Override
    public boolean electSwarmLeader(NodeID sender, NodeID cadidate, TransferID Transfer) throws NotSwarmMemeber, TException {
        return false;
    }

    @Override
    public void electionEndedSwarm(NodeID sender, Swarm swarm) throws NotSwarmMemeber, TException {

    }

    @Override
    public void deliverTransfer(NodeID sender, TransferData transfer) throws TException {
        if(!account.isTransferInHistory(transfer))
        {
            account.takeTransfer(transfer);
        }
    }

    @Override
    public void makeTransfer(NodeID receiver, long value) throws TException {
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
            account.setBalance(account.getBalance() - value);

            try {
                //open connection
                NodeService.Client client = connectionManager.getConnection(receiver);

                //set params
                log.info("About to make transfer");

                client.deliverTransfer(this.nodeID, transferData);
                log.info("Transfer delivered!");

                log.info("Transfer complete");
            } catch (TTransportException e) {
                pendingTransfers.put(account.makeTransferKey(transferData.getTransferID()), transferData);
                Swarm swarm = createSwarm(transferData);
                createDeliverTask(receiver, transferData, swarm);
                log.info("Transfer failed, added to pending transfers");
            }

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
        return new ArrayList<Swarm>(mySwarms.values());
    }

    @Override
    public void startSwarmElection(TransferID transfer) throws NotSwarmMemeber, TException {

    }

    @Override
    public List<TransferData> getTransfers() throws TException {
        return account.getTransferHistory();
    }

    @Override
    public void setBlacklist(List<NodeID> blacklist) throws TException {

    }

    @Override
    public void virtualStop(boolean shouldStop) throws TException {

    }

    @Override
    public void stop() throws TException {
        log.info("Server stopped");
        connectionManager.cleanUp();
        System.exit(0);
    }
}
