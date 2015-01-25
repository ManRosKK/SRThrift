import SRBanking.ThriftInterface.NotEnoughMoney;
import SRBanking.ThriftInterface.Swarm;
import SRBanking.ThriftInterface.TransferData;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
                if (language1.equals(language2) || language1.equals("java") || language2.equals("java"))
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
                if (language1.equals(language2) || language1.equals("java") || language2.equals("java"))
                    continue;

                try {
                    //set up
                    Reporter.log("Connecting " + language1 + " and " + language2,true);
                    Util.runServer(IP, port, balance, configFile);
                    Util.runServer(IP2, port2, balance2, configFile);

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
                if (language1.equals(language2) || language1.equals("java") || language2.equals("java"))
                    continue;

                try {
                    //set up
                    Reporter.log("\n\n----------------------\n",true);
                    Reporter.log("Connecting " + language1 + " and " + language2,true);
                    Reporter.log("\n\n----------------------\n",true);
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
                if (language1.equals(language2) || language1.equals("java") || language2.equals("java"))
                    continue;

                for(int i=1;i>=0;--i)
                {
                    try {
                        Reporter.log("Connecting " + language1 + " and " + language2 + ", killing " + i + "...",true);

                        int lowerHalf = 5;
                        int upperHalf = 5;

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



}
