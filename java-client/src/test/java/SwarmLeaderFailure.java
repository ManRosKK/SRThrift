import SRBanking.ThriftInterface.Swarm;
import SRBanking.ThriftInterface.TransferData;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class SwarmLeaderFailure {

    String IP = "127.0.0.1";
    int port = 9080;
    long balance = 501;
    String IP2 = "127.0.0.1";
    int port2 = 9081;
    long balance2 = 502;
    String IP3 = "127.0.0.1";
    int port3 = 9082;
    long balance3 = 502;

    String IPReceiver = "127.0.0.1";
    int portReceiver = 13467;
    long balanceReceiver = 503;

    String configFile = "config\\testSwarmBasics.ini";

    @BeforeMethod
    public void setUp() throws Exception {
        //multiple config files so
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Util.killServerNoException(IP,port);
        Util.killServerNoException(IP2, port2);
        Util.killServerNoException(IP3, port3);
        Util.killServerNoException(IPReceiver, portReceiver);
    }

    @Test
    public void TheKingIsDeadLongLiveTheKing() throws Exception {
        //arrange
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        //act
        int value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
        Util.killServerNoException(IP,port);

        Thread.sleep(5000);

        //assert
        List<Swarm> swarmList = EasyClient.getSwarmList(IP2, port2);
        assertEquals(swarmList.size(),1);
        assertEquals(swarmList.get(0).getLeader().getPort(),port2);
    }

    @Test
    public void TheNewReignDeliver() throws Exception {
        //arrange
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        //act
        int value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
        Util.killServerNoException(IP,port);

        //wait for new leader election
        Thread.sleep(4000);

        Util.runServer(IPReceiver, portReceiver, balance2, configFile);

        //wait for transfer delivery
        Thread.sleep(4000);

        //assert
        List<TransferData> historyR = EasyClient.getHistory(IPReceiver, portReceiver);
        assertEquals(historyR.size(), 1);
        assertEquals(historyR.get(0).getValue(), value);
        assertEquals(historyR.get(0).getTransferID().getSender().getIP(), IP);
        assertEquals(historyR.get(0).getTransferID().getSender().getPort(), port);

        List<Swarm> swarmList = EasyClient.getSwarmList(IP2, port2);
        assertEquals(swarmList.size(),0);

    }
}
