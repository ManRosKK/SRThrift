import SRBanking.ThriftInterface.NodeID;
import SRBanking.ThriftInterface.NotEnoughMembersToMakeTransfer;
import SRBanking.ThriftInterface.Swarm;
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
public class SplitBrainTest {

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
    int maxcount = 4;

    String config_p10_s2 = "config\\testSwarm_p10_s2.ini";

    @BeforeMethod
    public void setUp() throws Exception {
        //multiple config files so
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Util.killNServers(IPN,portLow,maxcount);
        Util.killServerNoException(IPReceiver,portReceiver);
    }

    @Test
    public void FirstSurgery() throws Exception {
        Util.runNServers(IPN, portLow,Util.defaultBalance,config_p10_s2,2);
        long value = 1;
        EasyClient.makeTransfer(IP,port,IPReceiver,portReceiver,value);
        // nodes 0 and 1 should make a swarm

        //run addtional servers and split the network: 0+2;1+3
        Util.runNServers(IPN, portLow+2,Util.defaultBalance,config_p10_s2,2);

        //set first zone
        List<NodeID> blacklist1 = Arrays.asList(
                new NodeID(IPN,portLow+1),
                new NodeID(IPN,portLow+3)
        );
        EasyClient.setBlacklist(IPN,portLow+0, blacklist1);
        EasyClient.setBlacklist(IPN,portLow+2, blacklist1);

        //set second zone
        List<NodeID> blacklist2 = Arrays.asList(
                new NodeID(IPN,portLow+0),
                new NodeID(IPN,portLow+2)
        );
        EasyClient.setBlacklist(IPN,portLow+1, blacklist2);
        EasyClient.setBlacklist(IPN,portLow+3, blacklist2);

        //wait to see what will happen
        Thread.sleep(10000);

        List<Swarm> swarmList = null;

        swarmList = EasyClient.getSwarmList(IPN, portLow + 0);
        assertEquals(swarmList.size(),1);
        swarmList = EasyClient.getSwarmList(IPN, portLow + 1);
        assertEquals(swarmList.size(),1);
        swarmList = EasyClient.getSwarmList(IPN, portLow + 2);
        assertEquals(swarmList.size(),1);
        swarmList = EasyClient.getSwarmList(IPN, portLow + 3);
        assertEquals(swarmList.size(),1);
    }

    @Test
    public void FirstSurgeryDeliver() throws Exception {
        Util.runNServers(IPN, portLow,Util.defaultBalance,config_p10_s2,2);
        long value = 1;
        EasyClient.makeTransfer(IP,port,IPReceiver,portReceiver,value);
        // nodes 0 and 1 should make a swarm

        //run addtional servers and split the network: 0+2;1+3
        Util.runNServers(IPN, portLow+2,Util.defaultBalance,config_p10_s2,2);

        //set first zone
        List<NodeID> blacklist1 = Arrays.asList(
                new NodeID(IPN,portLow+1),
                new NodeID(IPN,portLow+3)
        );
        EasyClient.setBlacklist(IPN,portLow+0, blacklist1);
        EasyClient.setBlacklist(IPN,portLow+2, blacklist1);

        //set second zone
        List<NodeID> blacklist2 = Arrays.asList(
                new NodeID(IPN,portLow+0),
                new NodeID(IPN,portLow+2)
        );
        EasyClient.setBlacklist(IPN,portLow+1, blacklist2);
        EasyClient.setBlacklist(IPN,portLow+3, blacklist2);

        //wait to see what will happen
        Thread.sleep(10000);

        long balance = 400;
        Util.runServer(IPReceiver,portReceiver,balanceReceiver,config_p10_s2);
        Thread.sleep(5000);
        //transfer should be delivered

        //assert
        long balanceRead = EasyClient.getBalance(IPReceiver, portReceiver);
        assertEquals(balanceReceiver+value,balanceRead);

        List<Swarm> swarmList = null;
        swarmList = EasyClient.getSwarmList(IPN, portLow + 0);
        assertEquals(swarmList.size(),0);
        swarmList = EasyClient.getSwarmList(IPN, portLow + 1);
        assertEquals(swarmList.size(),0);
        swarmList = EasyClient.getSwarmList(IPN, portLow + 2);
        assertEquals(swarmList.size(),0);
        swarmList = EasyClient.getSwarmList(IPN, portLow + 3);
        assertEquals(swarmList.size(),0);


    }


}
