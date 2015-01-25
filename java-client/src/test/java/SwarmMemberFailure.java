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
public class SwarmMemberFailure {

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
    public void SwarmTestNewMember() throws Exception {
        //arrange
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        //act
        long value = 5;
        EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
        Util.runServer(IP3, port3, balance3, configFile);
        Util.killServerNoException(IP2,port2);

        Thread.sleep(15000);

        //assert
        List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
        List<Swarm> swarmList3 = EasyClient.getSwarmList(IP3, port3);
        assertEquals(swarmList.size(),1);
        assertEquals(swarmList3.size(),1);
        assertEquals(swarmList.get(0).getLeader().getPort(),port);
        assertEquals(swarmList3.get(0).getLeader().getPort(),port);
    }
}
