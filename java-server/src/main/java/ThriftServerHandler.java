import SRBanking.ThriftInterface.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sven on 2015-01-09.
 */
public class ThriftServerHandler implements NodeService.Iface{

    private static Logger log = LoggerFactory.getLogger(ThriftServerHandler.class);
    private final NodeID nodeID;
    private long accountBalance;
    private String IP;
    private long messageCounter;

    private List<TransferData> pendingTransfers;

    public ThriftServerHandler(String ip, int port, long accountBalance) {
        this.accountBalance = accountBalance;

        this.nodeID = new NodeID();
        IPAddress ipAddress = new IPAddress();
        ipAddress.setIP(ip);
        this.nodeID.setPort(port);

        this.messageCounter = 0;

        pendingTransfers = new ArrayList<TransferData>();
    }

    @Override
    public void ping() throws TException {
        //implementation intentionally empty
    }

    @Override
    public void pingSwarm(NodeID leader, TransferID transfer) throws NotSwarmMemeber, TException {

    }

    @Override
    public void updateSwarmMembers(Swarm swarm) throws NotSwarmMemeber, WrongSwarmLeader, TException {

    }

    @Override
    public void addToSwarm(Swarm swarm) throws AlreadySwarmMemeber, TException {

    }

    @Override
    public void delSwarm(Swarm swarm) throws NotSwarmMemeber, WrongSwarmLeader, TException {

    }

    @Override
    public Swarm getSwarm(TransferID transfer) throws NotSwarmMemeber, TException {
        return null;
    }

    @Override
    public boolean electSwarmLeader(NodeID cadidate, TransferID Transfer) throws NotSwarmMemeber, TException {
        return false;
    }

    @Override
    public void electionEndedSwarm(Swarm swarm) throws NotSwarmMemeber, TException {

    }

    @Override
    public void deliverTransfer(TransferData transfer) throws TException {
        accountBalance += transfer.getValue();
        //TODO: Delete Swarm here.
    }


    @Override
    public void makeTransfer(NodeID receiver, long value) throws TException {
        //get params for connection
        String address = receiver.getAddress().getIP();
        int port = receiver.getPort();

        //set params
        TransferData transferData = new TransferData();
        TransferID transferID = new TransferID();
        transferID.setSender(this.nodeID);
        transferID.setReceiver(receiver);
        transferID.setCounter(messageCounter++);
        transferData.setTransferID(transferID);
        transferData.setValue(value);

        accountBalance -= value;

        //TODO: check if possible (enough money)

        //open connection
        TTransport transport = new TSocket(address, port);
        transport.open();
        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);

        //set params
        log.info("About to make transfer");


        try {
            client.deliverTransfer(transferData);
            log.info("Transfer delivered!");
        } catch (TTransportException e) {
            pendingTransfers.add(transferData);
            log.info("Transfer failed, added to pending transfers");
        }

        log.info("Transfer complete");

        transport.close();
    }

    @Override
    public long getAccountBalance() throws TException {

        return accountBalance;
    }

    @Override
    public List<Swarm> getSwarmList() throws TException {
        return null;
    }

    @Override
    public void startSwarmElection(TransferID transfer) throws NotSwarmMemeber, TException {

    }

    @Override
    public List<TransferData> getTransfers() throws TException {
        return null;
    }

    @Override
    public void stop() throws TException {
        log.info("Server stopped");
        System.exit(0);
    }
}