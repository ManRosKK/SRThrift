import SRBanking.ThriftInterface.Swarm;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class SwarmBasics {

    String IP = "localhost";
    int port = 9080;
    long balance = 501;
    String IP2 = "localhost";
    int port2 = 9081;
    long balance2 = 502;
    String IPReceiver = "localhost";
    int portReceiver = 13467;
    String configFile = "config\\default.ini";


    @BeforeMethod
    public void setUp() throws Exception {
        //arrange
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Util.killServerNoException(IP,port);
        Util.killServerNoException(IP2,port2);
    }

    @Test
    public void CreateSwarm() throws Exception {
        int value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
        Thread.sleep(500);
        List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
        assertEquals(1,swarmList.size());
        assertEquals(2,swarmList.get(0).getMembersSize());
        assertEquals(port,swarmList.get(0).getLeader().getPort());
        assert((port == swarmList.get(0).getMembers().get(0).getPort())
                || port == swarmList.get(0).getMembers().get(1).getPort());
        assert((port2 == swarmList.get(0).getMembers().get(0).getPort())
                || port2 == swarmList.get(0).getMembers().get(1).getPort());
    }
}
