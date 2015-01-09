import SRBanking.ThriftInterface.*;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by sven on 2015-01-09.
 */
public class ThriftServerHandler implements NodeService.Iface{

    private static Logger log = LoggerFactory.getLogger(ThriftServerHandler.class);

    @Override
    public void Ping() throws TException {

    }

    @Override
    public void PingSwarm(NodeID leader, TransferID transfer) throws NotSwarmMemeber, TException {

    }

    @Override
    public void UpdateSwarmMembers(Swarm swarm) throws NotSwarmMemeber, WrongSwarmLeader, TException {

    }

    @Override
    public void AddToSwarm(Swarm swarm) throws AlreadySwarmMemeber, TException {

    }

    @Override
    public void DelSwarm(Swarm swarm) throws NotSwarmMemeber, WrongSwarmLeader, TException {

    }

    @Override
    public Swarm GetSwarm(TransferID transfer) throws NotSwarmMemeber, TException {
        return null;
    }

    @Override
    public boolean ElectSwarmLeader(NodeID cadidate, TransferID Transfer) throws NotSwarmMemeber, TException {
        return false;
    }

    @Override
    public void ElectionEndedSwarm(Swarm swarm) throws NotSwarmMemeber, TException {

    }

    @Override
    public void MakeTransfer(TransferData transfer) throws TException {

    }

    @Override
    public List<Swarm> GetSwarmList() throws TException {
        return null;
    }

    @Override
    public void startSwarmElection(TransferID transfer) throws NotSwarmMemeber, TException {

    }

    @Override
    public List<TransferData> GetTransfers() throws TException {
        return null;
    }

    @Override
    public void stop() throws TException {
        log.info("Server stopped");
        System.exit(0);
    }
}
