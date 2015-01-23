import SRBanking.ThriftInterface.NodeID;
import SRBanking.ThriftInterface.NotEnoughMembersToMakeTransfer;
import SRBanking.ThriftInterface.Swarm;
import SRBanking.ThriftInterface.TransferData;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class SwarmBlacklistTest {

    String IP = "127.0.0.1";
    int port = 9080;
    long balance = 501;
    NodeID node1 = new NodeID(IP,port);

    String IP2 = "127.0.0.1";
    int port2 = 9081;
    long balance2 = 502;
    NodeID node2 = new NodeID(IP2,port2);

    String IP3 = "127.0.0.1";
    int port3 = 9082;
    long balance3 = 502;
    NodeID node3 = new NodeID(IP3,port3);

    String IPReceiver = "127.0.0.1";
    int portReceiver = 13467;
    long balanceReceiver = 503;

    String IPN = "127.0.0.1";
    int portLow = 9080;
    int count = 10;

    String configFile = "config\\testSwarmBasics.ini";

    @BeforeMethod
    public void setUp() throws Exception {
        //multiple config files so
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Util.killServerNoException(IP,port);
        Util.killServerNoException(IP2, port2);
    }

    @Test(expectedExceptions = NotEnoughMembersToMakeTransfer.class)
    public void BlacklistFrom() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        EasyClient.setBlacklist(IP,port, Arrays.asList(node2));

        long value = 3;
        EasyClient.makeTransfer(IP,port,IP2,port2,value);
    }

    @Test(expectedExceptions = NotEnoughMembersToMakeTransfer.class)
    public void BlacklistTo() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        EasyClient.setBlacklist(IP2,port2, Arrays.asList(node1));

        long value = 3;
        EasyClient.makeTransfer(IP,port,IP2,port2,value);
    }

    @Test
    public void BlacklistUnset() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        EasyClient.setBlacklist(IP,port, Arrays.asList(node2));
        EasyClient.setBlacklist(IP,port, new ArrayList());

        long value = 3;
        EasyClient.makeTransfer(IP,port,IP2,port2,value);
    }

    @Test(expectedExceptions = NotEnoughMembersToMakeTransfer.class)
    public void VirtualStopOne() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        EasyClient.virtualStop(IP2,port2, true);

        long value = 3;
        EasyClient.makeTransfer(IP,port,IP2,port2,value);
    }

    @Test()
    public void VirtualStopStart() throws Exception {
        Util.runServer(IP, port, balance, configFile);
        Util.runServer(IP2, port2, balance2, configFile);

        EasyClient.virtualStop(IP2,port2, true);
        EasyClient.virtualStop(IP2,port2, false);

        long value = 3;
        EasyClient.makeTransfer(IP,port,IP2,port2,value);
    }




}
