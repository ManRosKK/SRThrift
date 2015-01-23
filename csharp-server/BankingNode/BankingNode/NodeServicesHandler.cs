using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Thrift;
using SRBanking;
using Thrift.Transport;
using Thrift.Protocol;
using log4net;
using System.Reflection;
using Thrift.Server;
using log4net.Repository.Hierarchy;
using log4net.Appender;
using System.Threading;
namespace BankingNode
{
    class NodeServicesHandler : SRBanking.ThriftInterface.NodeService.Iface
    {
        object _m = new object();
        class NodeInfo
        {
            public NodeID node;
            public bool isActive;
        }
        public TServer server;
        private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private SwarmManager swamManager = new SwarmManager();
        private BalanceManager balanceManager = new BalanceManager();
        private ClientManager clientManager = new ClientManager();
        private List<SRBanking.ThriftInterface.NodeID> blackList = null;
        /*private TSocket connectToServer(NodeID node,out SRBanking.ThriftInterface.NodeService.Client client)
        {
            var transport = new TSocket(node.IP, node.Port);
            var protocol = new TBinaryProtocol(transport);
            client = new SRBanking.ThriftInterface.NodeService.Client(protocol);
            
            transport.Open();
            return transport;
        }
        private void closeConnection(TSocket transport)
        {
            transport.Close();
        }*/
        private bool PingNode(NodeID node)
        {
            try
            {
                SRBanking.ThriftInterface.NodeService.Client client = clientManager.TakeClient(node);
                client.ping(ConfigLoader.Instance.ConfigGetSelfId().ToBase());
                clientManager.ReturnClient(node);
            }
            catch (Exception ex)
            {
                
                clientManager.ReturnClient(node);
                return false;
            }
            return true;
        }

        private Dictionary<int, NodeInfo> findNodes()
        {
            string[] ips = ConfigLoader.Instance.ConfigGetStrings(ConfigLoader.ConfigLoaderKeys.IpList);
            long[][] ports = ConfigLoader.Instance.ConfigGetRanges(ConfigLoader.ConfigLoaderKeys.PortList);
            List<NodeID> l = new List<NodeID>();
            Random rng = new Random();
            Dictionary<int, NodeInfo> nodes = new Dictionary<int, NodeInfo>();
            int nodesCount = 0;
            for (int j = 0; j < ports.Length; j++)
            {
                int currPort;
                int k = 0;
                int kstop = 0;
                if (ports[j].Length == 1)
                {
                    k=kstop = (int)ports[j][0];
                }
                else
                {
                    k = (int)ports[j][0];
                    kstop = (int)ports[j][1];
                }
                    for (; k <= kstop; k++)
                    {
                        currPort = k;
                        for (int i = 0; i < ips.Length; i++)
                        {
                            nodesCount++;
                        }
                    }
                
            }


            for (int j = 0; j < ports.Length; j++)
            {
                int currPort;
                int k = 0;
                int kstop = 0;
                if (ports[j].Length == 1)
                {
                    k = kstop = (int)ports[j][0];
                }
                else
                {
                    k = (int)ports[j][0];
                    kstop = (int)ports[j][1];
                }
                for (; k <= kstop; k++)
                {
                    currPort = k;
                    for (int i = 0; i < ips.Length; i++)
                    {
                        int cc = rng.Next(0, nodesCount);
                        if (nodes.ContainsKey(cc))
                        {
                            for (int y = 0; y < nodesCount; y++)
                            {
                                if (!nodes.ContainsKey(y))
                                {
                                    cc = y;
                                    break;
                                }
                            }
                        }
                        NodeID tmp = new NodeID();
                        tmp.IP = ips[i];
                        tmp.Port = currPort;

                        NodeInfo inf = new NodeInfo();
                        inf.isActive = false;
                        inf.node = tmp;
                        nodes.Add(cc, inf);
                    }
                }

            }
            
            return nodes;
        }
        private bool updateSwarm(Swarm swarm)
        {
            bool flag = true;
            List<NodeID> list = swarm.Members;
            foreach (NodeID x in list)
            {
                try
                {
                        SRBanking.ThriftInterface.NodeService.Client client = clientManager.TakeClient(x);
                        client.updateSwarmMembers(ConfigLoader.Instance.ConfigGetSelfId().ToBase() ,swarm.ToBase());
                    clientManager.ReturnClient(x);
                }
                catch (Exception ex)
                {
                    logerr.Error("Nieudany update: " + x);
                    clientManager.ReturnClient(x);
                    
                    flag = false;
                }
            }
            return flag;
        }
        private Swarm createSwarm(Dictionary<int, NodeInfo> nodes,TransferData data)
        {
            Swarm swarm = new Swarm();
            swarm.Leader = ConfigLoader.Instance.ConfigGetSelfId();
            swarm.Transfer = data.TransferID;
            List<NodeID> list = new List<NodeID>();
            list.Add(ConfigLoader.Instance.ConfigGetSelfId());
            logerr.Info("STARTING CREATING SWARM" + nodes.Count + "|" + data.TransferID);
            for (int i = 0; i < nodes.Count; i++)
            {
                NodeID n = nodes[i].node;
                logerr.Info("STARTING CREATING SWARM(CURR node:)" + n);
                if (n == ConfigLoader.Instance.ConfigGetSelfId())
                    continue;
                try
                {
                        SRBanking.ThriftInterface.NodeService.Client client  =clientManager.TakeClient(n);
                        try
                        {
                            client.ping(swarm.Leader.ToBase());
                        }
                        catch
                        {
                            //closeConnection(transport);
                            clientManager.connectToNode(n);
                            client.ping(swarm.Leader.ToBase());
                        }
                        List<NodeID> tmp = new List<NodeID>();
                        swarm.Members = tmp;
                        client.addToSwarm(swarm.Leader.ToBase(), swarm.ToBase(), data.ToBase());
                        list.Add(n);
                        logerr.Info("STARTING CREATING SWARM(ADDED node:)" + n);
                        //swarm.AddToSwarm(n);
                        clientManager.ReturnClient(n);

                }
                catch (Exception e)
                {
                    clientManager.ReturnClient(n);
                    logerr.Error("pinging beafore ad to swarm",e);
                }
                if (list.Count >= ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.SwarmSize))
                {
                    swarm.Members = list;
                    return swarm;
                }
            }
            foreach (NodeID x in list)
            {
                try
                {
                        SRBanking.ThriftInterface.NodeService.Client client = clientManager.TakeClient(x);
                        client.delSwarm(swarm.Leader.ToBase(),swarm.Transfer.ToBase());
                        clientManager.ReturnClient(x);
                }
                catch (Exception ex)
                {
                    clientManager.ReturnClient(x);
                }
            }
            logerr.Error("COS nie tak CREATE SWARM");
            throw new SRBanking.ThriftInterface.NotEnoughMembersToMakeTransfer();
        }
        private Swarm addNewToSwarm(Swarm swarm,Dictionary<int, NodeInfo> nodes, TransferData data)
        {
            //swarm.Leader = ConfigLoader.Instance.ConfigGetSelfId();
            //swarm.Transfer = data.TransferID;
            List<NodeID> list = swarm.Members ;
            long needed= ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.SwarmSize);
            for (int i = 0; i < nodes.Count && list.Count<needed; i++)
            {
                NodeID n = nodes[i].node;
                logerr.Info("szukanie nowego " + n + "|" + nodes.Count + "|");
                if (list.Contains(n)) continue;
                logerr.Info("PO---szukanie nowego " + n + "|" + nodes.Count + "|");
                try
                {
                        SRBanking.ThriftInterface.NodeService.Client client = clientManager.TakeClient(n);
                        client.ping(swarm.Leader.ToBase());

                        List<NodeID> tmp = new List<NodeID>();
                        swarm.Members = tmp;
                        client.addToSwarm(swarm.Leader.ToBase(),swarm.ToBase(), data.ToBase());
                        list.Add(n);
                        swamManager.DirtySwarm(data.TransferID);
                        clientManager.ReturnClient(n);
                    


                }
                catch (Exception)
                {
                    clientManager.ReturnClient(n);
                }
            }
            swarm.Members = list;
            return swarm;
        }
        public void SwarmTimeoutDelegate(TransferID id, SwarmManager swarmManager)
        {
            Swarm swarm = swamManager.GetSwarm(id);
            lock (swarm)
            {
                logerr.Info("Not Pinged " + id.ToString() + "|" + swarm.Leader.ToString());

                swamManager.BeginElection(id);
                List<NodeID> list = swarm.Members;
                bool flag = true;
                NodeID we = ConfigLoader.Instance.ConfigGetSelfId();
                List<NodeID> toInform = new List<NodeID>();
                foreach (NodeID x in list)
                {
                    try
                    {
                        SRBanking.ThriftInterface.NodeService.Client client = clientManager.TakeClient(x);
                        client.startSwarmElection(id.ToBase());
                        if (we > x)
                        {
                            if (!client.electSwarmLeader(we.ToBase(),we.ToBase(), id.ToBase()))
                            {
                                flag = false;
                                clientManager.ReturnClient(x);
                                return;
                            }
                        }
                        else
                        {
                            toInform.Add(x);
                        }
                        clientManager.ReturnClient(x);

                    }
                    catch (Exception ex)
                    {
                        clientManager.ReturnClient(x);
                        //closeConnection(transport);
                    }
                }
                if (flag)
                {
                    swarm.Leader = we;
                    swarm.Members = toInform;
                    swamManager.UpdateSwarm(swarm);
                    swamManager.EndElection(id);
                    foreach (NodeID x in toInform)
                    {
                        try
                        {
                            lock (_m)
                            {
                                SRBanking.ThriftInterface.NodeService.Client client = clientManager.TakeClient(x);
                                client.electionEndedSwarm(we.ToBase(),swarm.ToBase());
                                clientManager.ReturnClient(x);
                            }
                        }
                        catch (Exception ex)
                        {
                            clientManager.ReturnClient(x);

                        }
                    }
                }
            }
        }
        public void SwarmLeaderTimeToPingDelegate(TransferID id, SwarmManager swarmManager)
        {
            Swarm swarm = swamManager.GetSwarm(id);
            lock (swarm)
            {
                TransferData d = swamManager.GetTransferData(id);
                List<NodeID> list = swarm.Members;
                logerr.Info("Time to Ping all members " + id.ToString() + "|" + swarm.Leader.ToString());
                List<NodeID> ListToDel = new List<NodeID>();

                if (swamManager.IsDirtySwarm(id))
                    if (updateSwarm(swarm))
                    {
                        swamManager.CleanSwarm(id);
                    }
                foreach (NodeID x in list)
                {
                    if (x == ConfigLoader.Instance.ConfigGetSelfId()) continue;
                    try
                    {

                        logerr.Info("try Pinged " + x.ToString() + " with " + swarm.Leader + "|" + swarm.Transfer);
                        SRBanking.ThriftInterface.NodeService.Client client = clientManager.TakeClient(x);
                        //client.delSwarm(swarm.Transfer.ToBase());
                        client.pingSwarm(swarm.Leader.ToBase(), swarm.Transfer.ToBase());
                        clientManager.ReturnClient(x);

                        logerr.Info("Pinged " + x.ToString());
                    }
                    catch (Exception ex)
                    {
                        clientManager.ReturnClient(x);
                        logerr.Error("Ping Error " + x.ToString(), ex);
                        logerr.Error("Ping Error info" + swamManager.IsDirtySwarm(id));
                        swamManager.DirtySwarm(id);
                        logerr.Error("Ping Error after info" + swamManager.IsDirtySwarm(id));
                        ListToDel.Add(x);
                    }
                }
                logerr.Error("Przed warunkiem " + swamManager.IsDirtySwarm(id) + "|" + swarm);
                if (swamManager.IsDirtySwarm(id) || swarm.Members.Count < ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.SwarmSize))
                {
                    logerr.Error("w Warunku ");
                    foreach (NodeID x in ListToDel)
                    {
                        logerr.Error("usuwamy " + x.ToString());
                        swarm.DeleteMember(x);
                    }
                    logerr.Error("szukamy nowego ");
                    swarm = addNewToSwarm(swarm, findNodes(), swamManager.GetTransferData(id));
                    logerr.Error("znaleziono: " + swarm);
                    swamManager.UpdateSwarm(swarm);
                    if (updateSwarm(swarm))
                    {
                        swamManager.CleanSwarm(id);
                    }
                }

                logerr.Info("Try to make transfer " + id.ToString() + "|" + swarm.Leader.ToString());


                bool f = false;
                try
                {
                    SRBanking.ThriftInterface.NodeService.Client client = clientManager.TakeClient(d.Receiver);
                    client.deliverTransfer(ConfigLoader.Instance.ConfigGetSelfId().ToBase(),d.ToBase());
                    //client.pingSwarm(swarm.Leader.ToBase(), swarm.Transfer.ToBase());
                    //client.delSwarm(swarm);
                    clientManager.ReturnClient(d.Receiver);

                    f = true;
                }
                catch (Exception)
                {
                    clientManager.ReturnClient(d.Receiver);

                }
                if (f)
                {

                    foreach (NodeID x in list)
                    {
                        try
                        {
                            SRBanking.ThriftInterface.NodeService.Client client = clientManager.TakeClient(x);
                            client.delSwarm(swarm.Leader.ToBase(),swarm.Transfer.ToBase());
                            clientManager.ReturnClient(x);

                        }
                        catch (Exception)
                        {

                        }
                    }
                    swamManager.DeleteSwarm(id);
                    return;
                }
            }
        }
        public NodeServicesHandler()
        {
            balanceManager.Balance = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.Balance);
            swamManager.SwarmLeaderTimeToPing = new SwarmManager.SwarmLeaderTimeToPingDelegate(this.SwarmLeaderTimeToPingDelegate);
            swamManager.SwarmTimeout = new SwarmManager.SwarmTimeoutDelegate(this.SwarmTimeoutDelegate);
            
        }
        ///INTERFACE

        /// <summary>
        /// 
        /// </summary>
        /// <param name="swarm"></param>
        /// <param name="transferData"></param>
        public void addToSwarm(SRBanking.ThriftInterface.NodeID sender,SRBanking.ThriftInterface.Swarm swarm, SRBanking.ThriftInterface.TransferData transferData)
        {
            Swarm sw = new Swarm(swarm);
            TransferData tr = new TransferData(transferData);
            logerr.Info("Add To Swarm "+swarm.ToString()+" | "+tr.ToString());
            sw.AddToSwarm(ConfigLoader.Instance.ConfigGetSelfId());
            swamManager.CreateSwarm(sw, tr);
            getSwarmList();
        }

        public void delSwarm(SRBanking.ThriftInterface.NodeID sender,SRBanking.ThriftInterface.TransferID swarmID)
        {
            swamManager.DeleteSwarm(new TransferID(swarmID));
        }

        public void deliverTransfer(SRBanking.ThriftInterface.NodeID sender, SRBanking.ThriftInterface.TransferData transfer)
        {
            foreach (IAppender appender in (logerr.Logger as Logger).Appenders)
            {
                var buffered = appender as BufferingAppenderSkeleton;
                if (buffered != null)
                {
                    buffered.Flush();
                }
            }
            logerr.Info("Transfer Delivered " + transfer.ToString());
            foreach (IAppender appender in (logerr.Logger as Logger).Appenders)
            {
                var buffered = appender as BufferingAppenderSkeleton;
                if (buffered != null)
                {
                    buffered.Flush();
                }
            }
            balanceManager.CommitTransfer(new TransferData(transfer));
            logerr.Info("Transfer Commited " + transfer.ToString());
            foreach (IAppender appender in (logerr.Logger as Logger).Appenders)
            {
                var buffered = appender as BufferingAppenderSkeleton;
                if (buffered != null)
                {
                    buffered.Flush();
                }
            }
        }

        public bool electSwarmLeader(SRBanking.ThriftInterface.NodeID sender, SRBanking.ThriftInterface.NodeID cadidate, SRBanking.ThriftInterface.TransferID Transfer)
        {
            swamManager.GetSwarm(new TransferID(Transfer));
            if (ConfigLoader.Instance.ConfigGetSelfId() < new NodeID(cadidate))
                return false;
            return true;
        }

        public void electionEndedSwarm(SRBanking.ThriftInterface.NodeID sender, SRBanking.ThriftInterface.Swarm swarm)
        {
            Swarm sw = new Swarm(swarm);

            if (sw.Leader == ConfigLoader.Instance.ConfigGetSelfId())
            {
                swamManager.DirtySwarm(sw.Transfer);
            }
            else
            {
                swamManager.UpdateSwarm(sw);
                swamManager.EndElection(sw.Transfer);
            }
        }

        public long getAccountBalance()
        {
            logerr.Info("Balanced1 ");
            return balanceManager.Balance;
        }

        public SRBanking.ThriftInterface.Swarm getSwarm(SRBanking.ThriftInterface.NodeID sender, SRBanking.ThriftInterface.TransferID transfer)
        {
            return swamManager.GetSwarm(new TransferID(transfer)).ToBase();
        }

        public List<SRBanking.ThriftInterface.Swarm> getSwarmList()
        {
            return swamManager.GetSwarmList();
        }

        public List<SRBanking.ThriftInterface.TransferData> getTransfers()
        {
            return balanceManager.BaseTransactions;
        }

        public void makeTransfer(SRBanking.ThriftInterface.NodeID receiver, long value)
        {
            foreach (IAppender appender in (logerr.Logger as Logger).Appenders)
            {
                var buffered = appender as BufferingAppenderSkeleton;
                if (buffered != null)
                {
                    buffered.Flush();
                }
            }
            logerr.Info("Making Transfer " + receiver.ToString() + " | " + value.ToString());
            foreach (IAppender appender in (logerr.Logger as Logger).Appenders)
            {
                var buffered = appender as BufferingAppenderSkeleton;
                if (buffered != null)
                {
                    buffered.Flush();
                }
            }
            NodeID rec = new NodeID( receiver);
            


            TransferData data = new TransferData(null);
            
            data.TransferID = balanceManager.generateTransactionID();
            data.Value = value;
            data.Receiver = rec;

           // logerr.Info("Transfer Prepared " + data.ToString());
            balanceManager.checkTransfer(data);
            SRBanking.ThriftInterface.NodeService.Client client = null; 
            try
            {
                client = clientManager.TakeClient(rec);
                logerr.Info("In try" + receiver.ToString() + " | " + value.ToString());
                balanceManager.CommitTransfer(data);
                logerr.Info("Before open " + receiver.ToString() + " | " + value.ToString());
                
                logerr.Info("After open " + receiver.ToString() + " | " + value.ToString());
                client.ping(ConfigLoader.Instance.ConfigGetSelfId().ToBase());
                client.deliverTransfer(ConfigLoader.Instance.ConfigGetSelfId().ToBase() ,data.ToBase());
                clientManager.ReturnClient(rec);
                
               // logerr.Info("Transfer Commited " + data.ToString());
            }
            catch (Exception ex)
            {
                logerr.Info(rec);
                logerr.Error("Exception ", ex);
                foreach (IAppender appender in (logerr.Logger as Logger).Appenders)
                {
                    var buffered = appender as BufferingAppenderSkeleton;
                    if (buffered != null)
                    {
                        buffered.Flush();
                    }
                }

                logerr.Info("przed klientem");
                clientManager.ReturnClient(rec);
                Swarm swarm = createSwarm(findNodes(), data);
                logerr.Error("----1 ");
                swamManager.CreateSwarm(swarm, data);
                if(swamManager.IsDirtySwarm(data.TransferID) )
                    if (updateSwarm(swarm))
                    {
                        swamManager.CleanSwarm(data.TransferID);
                    }
            }
            finally
            {
            }
            swamManager.GetSwarmList();
            logerr.Info("koniec");
            foreach (IAppender appender in (logerr.Logger as Logger).Appenders)
            {
                var buffered = appender as BufferingAppenderSkeleton;
                if (buffered != null)
                {
                    buffered.Flush();
                }
            }
        }

        public void pingSwarm(SRBanking.ThriftInterface.NodeID leader, SRBanking.ThriftInterface.TransferID transfer)
        {
            /*if (blackList != null && blackList.Contains(leader))
            {

                logerr.Info("try Pinged " + x.ToString() + " with " + swarm.Leader + "|" + swarm.Transfer);
                throw new SRBanking.ThriftInterface.NotSwarmMemeber();
            }*/
            logerr.Info("tiny pinged ");
            swamManager.PingSwarm(new NodeID(leader),new TransferID( transfer));
        }

        public void startSwarmElection(SRBanking.ThriftInterface.TransferID transfer)
        {
            swamManager.BeginElection(new TransferID(transfer));
        }

        public void stop()
        {
            server.Stop();
        }

        public void updateSwarmMembers(SRBanking.ThriftInterface.NodeID sender, SRBanking.ThriftInterface.Swarm swarm)
        {
            swamManager.UpdateSwarm(new Swarm(swarm));
        }

        public void setBlacklist(List<SRBanking.ThriftInterface.NodeID> blacklist)
        {
            this.blackList = blacklist;
        }

        public void ping(SRBanking.ThriftInterface.NodeID sender)
        {
            if (blackList != null && blackList.Contains(sender))
            {
                throw new SRBanking.ThriftInterface.NotSwarmMemeber();
            }

            logerr.Info("simple Pinged "+ new NodeID(sender));
            return;
        }

        public void virtualStop(bool shouldStop)
        {
            throw new NotImplementedException();
        }
    }
}
