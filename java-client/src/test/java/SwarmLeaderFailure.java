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

    String IPN = "127.0.0.1";
    int portLow = 9080;
    int count = 10;

    String configFile = "config\\testSwarmBasics.ini";
    String configBig = "config\\testSwarmBig.ini";

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
        Util.killNServers(IPN,portLow,count);
    }

    @Test
    public void TheKingIsDeadLongLiveTheKing() throws Exception {
        //arrange
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        //act
        long value = 5;
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
        long value = 5;
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

    /*
    1+2 -> kill1 run3 -> 2+3 -> kill2 run1 -> 3+1
     */
    @Test
    public void Carousel() throws Exception {
        //arrange
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        //act
        long value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
        Util.killServerNoException(IP,port);
        Util.runServer(IP3, port3, balance3, configFile);

        //wait for new leader election and adding to swarm
        Thread.sleep(8000);

        //assert
        List<Swarm> swarmList = EasyClient.getSwarmList(IP2, port2);
        assertEquals(swarmList.size(),1);
        assertEquals(swarmList.get(0).getMembersSize(),2);
        assert((swarmList.get(0).getMembers().get(0).getPort() == port3) || (swarmList.get(0).getMembers().get(1).getPort() == port3));

        //act again
        Util.killServerNoException(IP2,port2);
        Util.runServer(IP, port, balance, configFile);

        //wait for new leader election and adding to swarm
        Thread.sleep(8000);

        List<Swarm> swarmList3 = EasyClient.getSwarmList(IP3, port3);
        assertEquals(swarmList3.size(),1);
        assertEquals(swarmList3.get(0).getMembersSize(),2);
        assertEquals(swarmList3.get(0).getLeader().getPort(),port3);
        assert((swarmList3.get(0).getMembers().get(0).getPort() == port) || (swarmList3.get(0).getMembers().get(1).getPort() == port));
    }

    @Test
    public void BigElection() throws Exception {
        //arrange
        Util.runNServers(IPN,portLow,Util.defaultBalance,configBig,count);

        //act
        long value = 5;
        EasyClient.makeTransfer(IPN, portLow+count/2, IPReceiver, portReceiver, value);
        Util.killServerNoException(IPN,portLow+count/2);

        //wait for new leader election and adding to swarm
        Thread.sleep(10000);

        //assert
        List<Swarm> swarmList = EasyClient.getSwarmList(IP2, portLow+count/2+1);
        assertEquals(swarmList.size(),1);
        assertEquals(swarmList.get(0).getMembersSize(),9);
        assertEquals(swarmList.get(0).getLeader().getPort(),portLow);
    }
}
