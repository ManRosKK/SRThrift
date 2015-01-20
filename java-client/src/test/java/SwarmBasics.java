import SRBanking.ThriftInterface.NotEnoughMembersToMakeTransfer;
import SRBanking.ThriftInterface.Swarm;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class SwarmBasics {

    String IP = "127.0.0.1";
    int port = 9080;
    long balance = 501;
    String IP2 = "127.0.0.1";
    int port2 = 9081;
    long balance2 = 502;
    String IP3 = "127.0.0.1";
    int port3 = 9082;
    long balance3 = 503;
    String IPReceiver = "127.0.0.1";
    int portReceiver = 13467;
    long balanceReceiver = 13467;
    String configFile = "config\\testSwarmBasics.ini";
    String configFileTooBig = "config\\testSwarmBasicsTooBig.ini";


    @BeforeMethod
    public void setUp() throws Exception {
        //multiple config files so
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Util.killServerNoException(IP,port);
        Util.killServerNoException(IP2, port2);
        Util.killServerNoException(IP3, port3);
    }

     @Test
      public void CreateSwarm() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        int value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);

        List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
        assertEquals(swarmList.size(),1);
        assertEquals(swarmList.get(0).getMembersSize(),2);
        assertEquals(swarmList.get(0).getLeader().getPort(),port);
        assert((port == swarmList.get(0).getMembers().get(0).getPort())
                || port == swarmList.get(0).getMembers().get(1).getPort());
        assert((port2 == swarmList.get(0).getMembers().get(0).getPort())
                || port2 == swarmList.get(0).getMembers().get(1).getPort());
    }

    @Test
    public void CreateSwarmNotTooBig() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);
        Util.runServer(IP3, port3, balance3, configFile);

        int value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
        List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
        assertEquals(swarmList.size(),1);
        assertEquals(swarmList.get(0).getMembersSize(),2);
    }
    @Test
    public void CreateTwoSwarmsDifferentSenders() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        int value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
        EasyClient.makeTransfer(IP2, port2, IPReceiver, portReceiver, value);
        List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
        assertEquals(swarmList.size(),2);
        assertEquals(swarmList.get(0).getMembersSize(),2);
    }

    @Test
    public void CreateTwoSwarmsSameSender() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        int value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
        List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
        assertEquals(swarmList.size(),2);
        assertEquals(swarmList.get(0).getMembersSize(),2);
    }

    @Test(expectedExceptions = NotEnoughMembersToMakeTransfer.class)
    public void CreateTooBigSwarm() throws Exception {
        Util.runServer(IP, port, balance, configFileTooBig);
        Util.runServer(IP2, port2, balance2, configFileTooBig);

        int value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
    }
}
