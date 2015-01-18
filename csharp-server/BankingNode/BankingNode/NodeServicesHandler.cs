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
        private List<SRBanking.ThriftInterface.NodeID> blackList = null;
        private TSocket connectToServer(NodeID node,out SRBanking.ThriftInterface.NodeService.Client client)
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
        }
        private bool PingNode(NodeID node)
        {
            try
            {
                SRBanking.ThriftInterface.NodeService.Client client = null;
                TSocket transport = this.connectToServer(node, out client);
                client.ping(ConfigLoader.Instance.ConfigGetSelfId().ToBase());
                closeConnection(transport);
            }
            catch (Exception ex)
            {
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
                    lock (_m)
                    {
                        SRBanking.ThriftInterface.NodeService.Client client = null;
                        TSocket transport = this.connectToServer(x, out client);
                        client.updateSwarmMembers(swarm.ToBase());
                        closeConnection(transport);
                    }
                }
                catch (Exception ex)
                {
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

                    lock (_m)
                    {
                        SRBanking.ThriftInterface.NodeService.Client client = null;
                        TSocket transport = null;
                        try
                        {
                            transport = this.connectToServer(n, out client);
                            client.ping(swarm.Leader.ToBase());
                        }
                        catch
                        {
                            //closeConnection(transport);
                            Thread.Sleep(300);
                            transport = this.connectToServer(n, out client);
                            client.ping(swarm.Leader.ToBase());
                        }
                        List<NodeID> tmp = new List<NodeID>();
                        swarm.Members = tmp;
                        client.addToSwarm(swarm.ToBase(), data.ToBase());
                        list.Add(n);
                        logerr.Info("STARTING CREATING SWARM(ADDED node:)" + n);
                        //swarm.AddToSwarm(n);
                        closeConnection(transport);
                    }

                }
                catch (Exception e)
                {
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
                    lock (_m)
                    {
                        SRBanking.ThriftInterface.NodeService.Client client = null;
                        TSocket transport = this.connectToServer(x, out client);
                        client.delSwarm(swarm.Transfer.ToBase());
                        closeConnection(transport);
                    }
                }
                catch (Exception ex)
                {

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
            for (int i = 0; i < nodes.Count; i++)
            {
                NodeID n = nodes[i].node;
                if (list.Contains(n)) continue;
                try
                {
                    lock (_m)
                    {
                        SRBanking.ThriftInterface.NodeService.Client client = null;
                        TSocket transport = this.connectToServer(n, out client);
                        client.ping(swarm.Leader.ToBase());

                        List<NodeID> tmp = new List<NodeID>();
                        swarm.Members = tmp;
                        client.addToSwarm(swarm.ToBase(), data.ToBase());
                        list.Add(n);
                        swamManager.DirtySwarm(data.TransferID);
                        closeConnection(transport);
                    }


                }
                catch (Exception)
                {
                }
            }
            swarm.Members = list;
            return swarm;
        }
        public void SwarmTimeoutDelegate(TransferID id, SwarmManager swarmManager)
        {
            Swarm swarm = swamManager.GetSwarm(id);
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
                    lock (_m)
                    {
                        SRBanking.ThriftInterface.NodeService.Client client = null;
                        TSocket transport = this.connectToServer(x, out client);
                        client.startSwarmElection(id.ToBase());
                        if (we < x)
                        {
                            if (!client.electSwarmLeader(we.ToBase(), id.ToBase()))
                            {
                                flag = false;
                                closeConnection(transport);
                                return;
                            }
                        }
                        else
                        {
                            toInform.Add(x);
                        }
                        closeConnection(transport);
                    }
                }
                catch (Exception ex)
                {
                    //closeConnection(transport);
                }
            }
            if (flag)
            {
                swarm.Leader = we;
                swarm.Members = toInform;
                swamManager.UpdateSwarm(swarm);
                foreach (NodeID x in toInform)
                {
                    try
                    {
                        lock (_m)
                        {
                            SRBanking.ThriftInterface.NodeService.Client client = null;
                            TSocket transport = this.connectToServer(x, out client);
                            client.electionEndedSwarm(swarm.ToBase());
                            closeConnection(transport);
                        }
                    }
                    catch (Exception ex)
                    {

                    }
                }
            }
        }
        public void SwarmLeaderTimeToPingDelegate(TransferID id, SwarmManager swarmManager)
        {
            Swarm swarm = swamManager.GetSwarm(id);
            logerr.Info("Try to make transfer " + id.ToString() + "|" + swarm.Leader.ToString());
           
            TransferData d = swamManager.GetTransferData(id);
            List<NodeID> list = swarm.Members;
            bool f = false;
            try
            {
                lock (_m)
                {
                    SRBanking.ThriftInterface.NodeService.Client client = null;
                    TSocket transport = this.connectToServer(d.Receiver, out client);
                    client.makeTransfer(d.TransferID.Sender.ToBase(), d.Value);
                    client.pingSwarm(swarm.Leader.ToBase(), swarm.Transfer.ToBase());
                    closeConnection(transport);
                }
                f = true;
            }
            catch (Exception)
            {

            }
            if (f)
            {

                foreach (NodeID x in list)
                {
                    try
                    {
                        lock (_m)
                        {
                            SRBanking.ThriftInterface.NodeService.Client client = null;
                            TSocket transport = this.connectToServer(d.Receiver, out client);
                            client.delSwarm(swarm.Transfer.ToBase());
                            closeConnection(transport);
                        }
                    }
                    catch (Exception)
                    {

                    }
                }
                swamManager.DeleteSwarm(id);
                return;
            }
            logerr.Info("Time to Ping all members " + id.ToString() + "|" + swarm.Leader.ToString());
            List<NodeID> ListToDel = new List<NodeID>();

            if (swamManager.IsDirtySwarm(id))
                if (updateSwarm(swarm))
                {
                    swamManager.CleanSwarm(id);
                }
            foreach (NodeID x in list)
            {
                if(x == ConfigLoader.Instance.ConfigGetSelfId())continue;
                try
                {

                    logerr.Info("try Pinged " + x.ToString()+" with "+swarm.Leader+"|"+swarm.Transfer);
                    lock (_m)
                    {
                        SRBanking.ThriftInterface.NodeService.Client client = null;
                        TSocket transport = this.connectToServer(x, out client);
                        //client.delSwarm(swarm.Transfer.ToBase());
                        client.pingSwarm(swarm.Leader.ToBase(), swarm.Transfer.ToBase());
                        closeConnection(transport);
                    }
                    logerr.Info("Pinged " + x.ToString() );
                }
                catch (Exception ex)
                {
                    logerr.Error("Ping Error " + x.ToString(),ex);
                    swamManager.DirtySwarm(id);
                    ListToDel.Add(x);
                }
            }
            if (swamManager.IsDirtySwarm(id) || swarm.Members.Count<ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.SwarmSize))
            {
                foreach( NodeID x in ListToDel)
                {
                    swarm.DeleteMember(x);
                }
                swarm = addNewToSwarm(swarm, findNodes(), swamManager.GetTransferData(id));
                swamManager.UpdateSwarm(swarm);
                if (updateSwarm(swarm))
                {
                    swamManager.CleanSwarm(id);
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
        public void addToSwarm(SRBanking.ThriftInterface.Swarm swarm, SRBanking.ThriftInterface.TransferData transferData)
        {
            Swarm sw = new Swarm(swarm);
            TransferData tr = new TransferData(transferData);
            logerr.Info("Add To Swarm "+swarm.ToString()+" | "+tr.ToString());
            sw.AddToSwarm(ConfigLoader.Instance.ConfigGetSelfId());
            swamManager.CreateSwarm(sw, tr);
            getSwarmList();
        }

        public void delSwarm(SRBanking.ThriftInterface.TransferID swarmID)
        {
            swamManager.DeleteSwarm(new TransferID(swarmID));
        }

        public void deliverTransfer(SRBanking.ThriftInterface.TransferData transfer)
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

        public bool electSwarmLeader(SRBanking.ThriftInterface.NodeID cadidate, SRBanking.ThriftInterface.TransferID Transfer)
        {
            swamManager.GetSwarm(new TransferID(Transfer));
            if (ConfigLoader.Instance.ConfigGetSelfId() > new NodeID(cadidate))
                return false;
            return true;
        }

        public void electionEndedSwarm(SRBanking.ThriftInterface.Swarm swarm)
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

        public SRBanking.ThriftInterface.Swarm getSwarm(SRBanking.ThriftInterface.TransferID transfer)
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
            
            var transport = new TSocket(rec.IP, rec.Port);
            var protocol = new TBinaryProtocol(transport);
            var client = new SRBanking.ThriftInterface.NodeService.Client(protocol);


            TransferData data = new TransferData(null);
            
            data.TransferID = balanceManager.generateTransactionID();
            data.Value = value;
            data.Receiver = rec;

           // logerr.Info("Transfer Prepared " + data.ToString());
            balanceManager.checkTransfer(data);
            try
            {
                logerr.Info("In try" + receiver.ToString() + " | " + value.ToString());
                balanceManager.CommitTransfer(data);
                logerr.Info("Before open " + receiver.ToString() + " | " + value.ToString());
                lock (_m)
                {
                    transport.Open();
                    logerr.Info("After open " + receiver.ToString() + " | " + value.ToString());
                    client.ping(ConfigLoader.Instance.ConfigGetSelfId().ToBase());
                    client.deliverTransfer(data.ToBase());
                    transport.Close();
                }
               // logerr.Info("Transfer Commited " + data.ToString());
            }
            catch (Exception ex)
            {
                transport.Close();
                logerr.Error("Exception ", ex);
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

        public void updateSwarmMembers(SRBanking.ThriftInterface.Swarm swarm)
        {
            swamManager.UpdateSwarm(new Swarm(swarm));
        }

        public void addBlackList(List<SRBanking.ThriftInterface.NodeID> blackList)
        {
            this.blackList = blackList;
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
    }
}
