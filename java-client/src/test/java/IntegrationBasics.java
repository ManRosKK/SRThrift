import SRBanking.ThriftInterface.NodeID;
import SRBanking.ThriftInterface.NotEnoughMoney;
import SRBanking.ThriftInterface.Swarm;
import SRBanking.ThriftInterface.TransferData;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Created by sven on 2015-01-09.
 */
public class IntegrationBasics {

    String IP = "127.0.0.1";
    int port = 9080;
    long balance = 501;
    String IP2 = "127.0.0.1";
    int port2 = 9081;
    long balance2 = 502;
    String IPReceiver = "127.0.0.1";
    int portReceiver = 13465;
    long balanceReceiver = 503;

    String IPN = "127.0.0.1";
    int portLow = 9080;
    int count = 10;

    String configFile = "config\\testSwarmBasics.ini";
    String configBig = "config\\testSwarmBig.ini";
    String configp10s3 = "config\\testSwarm_p10_s3.ini";
    String configp10s6 = "config\\testSwarm_p10_s6.ini";

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void BasicTransfer() throws Exception {
        for(String language1: Util.shellStrings.keySet())
        {
            for(String language2: Util.shellStrings.keySet())
            {
                if (language1.equals(language2) || language1.equals("java") || language2.equals("java"))
                    continue;

                try {
                    //set up
                    Reporter.log("Connecting " + language1 + " and " + language2,true);
                    Util.runServer(IP, port, balance, configFile,language1 );
                    Util.runServer(IP2, port2, balance2,configFile,language2);

                    //arrange
                    long value = 30;
                    EasyClient.makeTransfer(IP, port, IP2, port2, value);

                    long balanceRead = EasyClient.getBalance(IP,port);
                    long balance2Read = EasyClient.getBalance(IP2,port2);
                    //assert
                    assertEquals(balanceRead, balance-value);
                    assertEquals(balance2Read, balance2+value);
                    Reporter.log("Test " + language1 + " and " + language2 + " succeeded!",true);
                }
                finally
                {
                    //tear down
                    Util.killServerNoException(IP, port);
                    Util.killServerNoException(IP2, port2);
                }

            }
        }
        Reporter.log("Success!",true);
    }

    @Test
    public void CreateSwarm() throws Exception {
        for(String language1: Util.shellStrings.keySet())
        {
            for(String language2: Util.shellStrings.keySet())
            {
                if (language1.equals(language2))
                    continue;

                try {
                    //set up
                    Reporter.log("Connecting " + language1 + " and " + language2,true);
                    Util.runServer(IP, port, balance, configFile,language1);
                    Util.runServer(IP2, port2, balance2, configFile,language2);

                    //test
                    long value = 5;
                    EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);

                    List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
                    assertEquals(swarmList.size(),1);
                    assertEquals(swarmList.get(0).getMembersSize(),2);
                    assertEquals(swarmList.get(0).getLeader().getPort(),port);
                    assert((port == swarmList.get(0).getMembers().get(0).getPort())
                            || port == swarmList.get(0).getMembers().get(1).getPort());
                    assert((port2 == swarmList.get(0).getMembers().get(0).getPort())
                            || port2 == swarmList.get(0).getMembers().get(1).getPort());
                    Reporter.log("Test " + language1 + " and " + language2 + " succeeded!",true);
                }
                finally
                {
                    //tear down
                    Util.killServerNoException(IP, port);
                    Util.killServerNoException(IP2, port2);
                }
            }

        }
    }

    @Test
    public void SwarmDeliver() throws Exception {
        for(String language1: Util.shellStrings.keySet())
        {
            for(String language2: Util.shellStrings.keySet())
            {
                if (language1.equals(language2) )
                    continue;

                try {
                    //set up
                    Reporter.log("Connecting " + language1 + " and " + language2,true);
                    Util.runServer(IP, port, balance, configFile,language1);
                    Util.runServer(IP2, port2, balance2, configFile,language2);

                    //test
                    long value = 5;
                    EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);

                    Util.runServer(IPReceiver, portReceiver, balanceReceiver, configFile);

                    Thread.sleep(1000);

                    List<Swarm> swarmList = EasyClient.getSwarmList(IP, port);
                    List<Swarm> swarmList2 = EasyClient.getSwarmList(IP2, port2);
                    List<Swarm> swarmListR = EasyClient.getSwarmList(IPReceiver, portReceiver);
                    assertEquals(swarmList.size(),0);
                    assertEquals(swarmList2.size(),0);
                    assertEquals(swarmListR.size(),0);

                    List<TransferData> history = EasyClient.getHistory(IP, port);
                    List<TransferData> history2 = EasyClient.getHistory(IP2, port2);
                    List<TransferData> historyR = EasyClient.getHistory(IPReceiver, portReceiver);
                    assertEquals(history.size(), 0);
                    assertEquals(history2.size(), 0);
                    assertEquals(historyR.size(), 1);
                    assertEquals(historyR.get(0).getValue(), value);
                    assertEquals(historyR.get(0).getTransferID().getSender().getIP(), IP);
                    assertEquals(historyR.get(0).getTransferID().getSender().getPort(), port);
                    assertEquals(historyR.get(0).getReceiver().getIP(), IPReceiver);
                    assertEquals(historyR.get(0).getReceiver().getPort(), portReceiver);

                    //report
                    Reporter.log("Test " + language1 + " and " + language2 + " succeeded!",true);
                }
                finally
                {
                    //tear down
                    Util.killServerNoException(IP, port);
                    Util.killServerNoException(IP2, port2);
                    Util.killServerNoException(IPReceiver, portReceiver);
                }
            }

        }
    }

    @Test
    public void TheKingIsDeadLongLiveTheKing() throws Exception {
        for(String language1: Util.shellStrings.keySet())
        {
            for(String language2: Util.shellStrings.keySet())
            {
                if (language1.equals(language2))
                    continue;

                try {
                    //set up
                    Reporter.log("\n\n----------------------\n",true);
                    Reporter.log("Connecting " + language1 + " and " + language2,true);
                    Reporter.log("\n\n----------------------\n",true);
                    Util.runServer(IP, port, balance, configFile,language1);
                    Util.runServer(IP2, port2, balance2, configFile,language2);

                    //act
                    long value = 5;
                    EasyClient.makeTransfer(IP, port, IPReceiver, portReceiver, value);
                    Util.killServerNoException(IP,port);

                    Thread.sleep(10000);

                    //assert
                    List<Swarm> swarmList = EasyClient.getSwarmList(IP2, port2);
                    assertEquals(swarmList.size(),1);
                    assertEquals(swarmList.get(0).getLeader().getPort(),port2);

                    //report
                    Reporter.log("\n\n----------------------\n",true);
                    Reporter.log("Test " + language1 + " and " + language2 + " succeeded!",true);
                    Reporter.log("\n\n----------------------\n",true);
                }
                finally
                {
                    //tear down
                    Util.killServerNoException(IP, port);
                    Util.killServerNoException(IP2, port2);
                }
            }
        }
    }

    @Test
    public void BigElection() throws Exception {

        for(String language1: Util.shellStrings.keySet())
        {
            for(String language2: Util.shellStrings.keySet())
            {
                if (language1.equals(language2))
                    continue;

                for(int i=1;i>=0;--i)
                {
                    try {
                        int lowerHalf = 5;
                        int upperHalf = 5;

                        Reporter.log("\n\n----------------------\n",true);
                        Reporter.log("Connecting " + language1 + " and " + language2 + ", killing " + i + "...",true);
                        Reporter.log("\n\n----------------------\n",true);


                        //servers will range <portLow;portLow+lowerHalf+i)
                        Util.runNServers(IPN,portLow,Util.defaultBalance,configBig,language1,lowerHalf+i);
                        //servers will range <portLow+lowerHalf+i;portLow+count)
                        Util.runNServers(IPN,portLow+lowerHalf+i,Util.defaultBalance,configBig,language2,upperHalf-i);

                        //act
                        long value = 5;
                        EasyClient.makeTransfer(IPN, portLow+lowerHalf, IPReceiver, portReceiver, value);
                        Thread.sleep(5000);
                        Util.killServerNoException(IPN,portLow+lowerHalf);

                        //wait for new leader election and adding to swarm
                        Thread.sleep(10000);

                        //assert
                        List<Swarm> swarmList = EasyClient.getSwarmList(IP2, portLow+count-1);
                        assertEquals(swarmList.size(),1);
                        assertEquals(swarmList.get(0).getMembersSize(),9);
                        assertEquals(swarmList.get(0).getLeader().getPort(),portLow);

                        //report
                        Reporter.log("Test " + language1 + " and " + language2 + " succeeded!",true);
                    }
                    finally
                    {
                        Reporter.log("Tear Down!",true);
                        //tear down
                        Util.killNServers(IPN,portLow,count);
                    }
                }
            }
        }
    }

    /**
     * Create three servers , one for each language
     */
    @Test
    public void ThreeOfAKind() throws Exception {

        for(String language1: Util.shellStrings.keySet()) {
            for(String language2: Util.shellStrings.keySet()) {
                for (String language3 : Util.shellStrings.keySet()) {

                    if (language1.equals(language2) || language1.equals(language3) || language2.equals(language3))
                        continue;

                    try {

                        Reporter.log("\n\n----------------------\n", true);
                        Reporter.log("Connecting " + language1 + ", " + language2 + " and " + language3, true);
                        Reporter.log("\n\n----------------------\n", true);

                        //run three servers
                        Util.runServer(IPN, portLow, Util.defaultBalance, configp10s3, language1);
                        Util.runServer(IPN, portLow+1, Util.defaultBalance, configp10s3, language2);
                        Util.runServer(IPN, portLow+2, Util.defaultBalance, configp10s3, language3);

                        //act
                        long value = 5;
                        EasyClient.makeTransfer(IPN, portLow, IPReceiver, portReceiver, value);
                        Thread.sleep(5000);

                        //assert
                        List<Swarm> swarmList = null;

                        swarmList = EasyClient.getSwarmList(IPN, portLow);
                        assertEquals(swarmList.size(), 1);
                        assertEquals(swarmList.get(0).getMembersSize(), 3);
                        assertEquals(swarmList.get(0).getLeader().getPort(), portLow);

                        swarmList = EasyClient.getSwarmList(IPN, portLow+1);
                        assertEquals(swarmList.size(), 1);
                        assertEquals(swarmList.get(0).getMembersSize(), 3);
                        assertEquals(swarmList.get(0).getLeader().getPort(), portLow);

                        swarmList = EasyClient.getSwarmList(IPN, portLow+2);
                        assertEquals(swarmList.size(), 1);
                        assertEquals(swarmList.get(0).getMembersSize(), 3);
                        assertEquals(swarmList.get(0).getLeader().getPort(), portLow);

                        //report
                        Reporter.log("Test  " + language1 + "," + language2 + " and " + language3 + "succeeded!", true);
                    } finally {
                        Reporter.log("\n\n----------------------\n", true);
                        Reporter.log("Tear Down!", true);
                        Reporter.log("\n\n----------------------\n", true);

                        //tear down
                        Util.killServerNoException(IPN,portLow);
                        Util.killServerNoException(IPN,portLow+1);
                        Util.killServerNoException(IPN,portLow+2);
                    }


                }
            }
        }
    }

    /**
     * 3-3 split brain test
     */
    @Test
    public void SplitBrain33() throws Exception {

        for(String language1: Util.shellStrings.keySet()) {
            for(String language2: Util.shellStrings.keySet()) {
                for (String language3 : Util.shellStrings.keySet()) {

                    if (language1.equals(language2) || language1.equals(language3) || language2.equals(language3))
                        continue;
                    if(!language1.equals("python") || !language2.equals("java") || !language3.equals("csharp"))
                        continue;

                    try {

                        Reporter.log("\n\n----------------------\n", true);
                        Reporter.log("Connecting " + language1 + ", " + language2 + " and " + language3, true);
                        Reporter.log("\n\n----------------------\n", true);

                        //run three servers
                        Util.runServer(IPN, portLow, Util.defaultBalance, configp10s6, language1);
                        Util.runServer(IPN, portLow+1, Util.defaultBalance, configp10s6, language2);
                        Util.runServer(IPN, portLow+2, Util.defaultBalance, configp10s6, language3);

                        //run three more servers
                        Util.runServer(IPN, portLow+3, Util.defaultBalance, configp10s6, language3);
                        Util.runServer(IPN, portLow+4, Util.defaultBalance, configp10s6, language1);
                        Util.runServer(IPN, portLow+5, Util.defaultBalance, configp10s6, language2);


                        //act: transfer
                        long value = 5;
                        EasyClient.makeTransfer(IPN, portLow, IPReceiver, portReceiver, value);
                        Thread.sleep(5000);

                        Reporter.log("\n\n----------------------\n", true);
                        Reporter.log("BLACKLIST", true);
                        Reporter.log("\n\n----------------------\n", true);

                        //act splitBrain
                        List<NodeID> blacklist0 = Arrays.asList();

                        List<NodeID> blacklist1 = Arrays.asList(
                                new NodeID(IPN, portLow),
                                new NodeID(IPN, portLow + 1),
                                new NodeID(IPN, portLow + 2)
                        );

                        List<NodeID> blacklist2 = Arrays.asList(
                                new NodeID(IPN, portLow + 3),
                                new NodeID(IPN, portLow + 4),
                                new NodeID(IPN, portLow + 5)
                        );
                        EasyClient.setBlacklist(IPN, portLow, blacklist2);
                        EasyClient.setBlacklist(IPN, portLow+1, blacklist2);
                        EasyClient.setBlacklist(IPN, portLow+2, blacklist2);
                        EasyClient.setBlacklist(IPN, portLow+3, blacklist1);
                        EasyClient.setBlacklist(IPN, portLow+4, blacklist1);
                        EasyClient.setBlacklist(IPN, portLow+5, blacklist1);

                        Thread.sleep(20000);

                        //assert
                        List<Swarm> swarmList = null;

                        //assert first miniswarm
                        swarmList = EasyClient.getSwarmList(IPN, portLow);
                        assertEquals(swarmList.size(), 1);
                        assertEquals(swarmList.get(0).getLeader().getPort(), portLow,swarmList.get(0).toString());
                        assertEquals(swarmList.get(0).getMembersSize(), 3);


                        swarmList = EasyClient.getSwarmList(IPN, portLow+1);
                        assertEquals(swarmList.size(), 1);
                        assertEquals(swarmList.get(0).getLeader().getPort(), portLow,swarmList.get(0).toString());
                        assertEquals(swarmList.get(0).getMembersSize(), 3);


                        swarmList = EasyClient.getSwarmList(IPN, portLow+2);
                        assertEquals(swarmList.size(), 1);
                        assertEquals(swarmList.get(0).getLeader().getPort(), portLow,swarmList.get(0).toString());
                        assertEquals(swarmList.get(0).getMembersSize(), 3);

                        //assert second miniswarm
                        swarmList = EasyClient.getSwarmList(IPN, portLow+3);
                        assertEquals(swarmList.size(), 1);
                        assertEquals(swarmList.get(0).getLeader().getPort(), portLow+3,swarmList.get(0).toString());
                        assertEquals(swarmList.get(0).getMembersSize(), 3);

                        swarmList = EasyClient.getSwarmList(IPN, portLow+4);
                        assertEquals(swarmList.size(), 1);
                        assertEquals(swarmList.get(0).getLeader().getPort(), portLow+3,swarmList.get(0).toString());
                        assertEquals(swarmList.get(0).getMembersSize(), 3);

                        swarmList = EasyClient.getSwarmList(IPN, portLow+5);
                        assertEquals(swarmList.size(), 1);
                        assertEquals(swarmList.get(0).getLeader().getPort(), portLow+3,swarmList.get(0).toString());
                        assertEquals(swarmList.get(0).getMembersSize(), 3);


                        //report
                        Reporter.log("Test  " + language1 + "," + language2 + " and " + language3 + "succeeded!", true);
                    } finally {
                        Reporter.log("\n\n----------------------\n", true);
                        Reporter.log("Tear Down  " + language1 + ", " + language2 + " and " + language3, true);
                        Reporter.log("\n\n----------------------\n", true);

                        //tear down
                        Util.killServerNoException(IPN,portLow);
                        Util.killServerNoException(IPN,portLow+1);
                        Util.killServerNoException(IPN,portLow+2);
                        Util.killServerNoException(IPN,portLow+3);
                        Util.killServerNoException(IPN,portLow+4);
                        Util.killServerNoException(IPN,portLow+5);
                    }


                }
            }
        }
    }

}
