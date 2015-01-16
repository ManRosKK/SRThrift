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
namespace BankingNode
{
    class NodeServicesHandler : SRBanking.ThriftInterface.NodeService.Iface
    {
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
        private void pingNode(NodeID node)
        {
            SRBanking.ThriftInterface.NodeService.Client client = null;
            TSocket transport = this.connectToServer(node,out client);
            client.ping(ConfigLoader.Instance.ConfigGetSelfId().ToBase());
            closeConnection(transport);
        }

        private List<NodeID> findNodes(long count)
        {
            string[] ips = ConfigLoader.Instance.ConfigGetStrings(ConfigLoader.ConfigLoaderKeys.IpList);
            long[][] ports = ConfigLoader.Instance.ConfigGetRanges(ConfigLoader.ConfigLoaderKeys.PortList);
            List<NodeID> l = new List<NodeID>();
            for (int i = 0; i < ips.Length; i++)
            {
                for (int j = 0; j < ports.Length; j++)
                {

                }
            }
            return null;
        }
        private void createSwarm(List<NodeID> nodes)
        {
            
        }

        public NodeServicesHandler()
        {
            balanceManager.Balance = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.Balance);
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
            logerr.Info("Add To Swarm"+swarm.ToString()+" | "+tr.ToString());
            sw.AddToSwarm(ConfigLoader.Instance.ConfigGetSelfId());
            swamManager.CreateSwarm(sw, tr);
        }

        public void delSwarm(SRBanking.ThriftInterface.TransferID swarmID)
        {
            swamManager.DeleteSwarm(new TransferID(swarmID));
        }

        public void deliverTransfer(SRBanking.ThriftInterface.TransferData transfer)
        {

            logerr.Info("Transfer Delivered ");
            balanceManager.CommitTransfer(new TransferData(transfer));
            logerr.Info("After Delivered ");
        }

        public bool electSwarmLeader(SRBanking.ThriftInterface.NodeID cadidate, SRBanking.ThriftInterface.TransferID Transfer)
        {
            throw new NotImplementedException();
        }

        public void electionEndedSwarm(SRBanking.ThriftInterface.Swarm swarm)
        {
            throw new NotImplementedException();
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

            logerr.Info("Making Transfer " + receiver.ToString() + " | " + value.ToString());
            NodeID rec = new NodeID( receiver);
            var transport = new TSocket(rec.IP, rec.Port);
            var protocol = new TBinaryProtocol(transport);
            var client = new SRBanking.ThriftInterface.NodeService.Client(protocol);


            logerr.Info("After open " + receiver.ToString() + " | " + value.ToString());
            TransferData data = new TransferData(null);
            
            data.TransferID = balanceManager.generateTransactionID();
            data.Value = value;
            data.Receiver = rec;
            balanceManager.checkTransfer(data);
            try
            {
                transport.Open();
                logerr.Info("In try" + receiver.ToString() + " | " + value.ToString());
                client.ping(ConfigLoader.Instance.ConfigGetSelfId().ToBase());
                client.deliverTransfer(data.ToBase());
                balanceManager.CommitTransfer(data);
            }
            catch(Exception ex)
            {
                logerr.Error("Exception ",ex);
               // throw new SRBanking.ThriftInterface.NotEnoughMembersToMakeTransfer();
            }
        }

        public void pingSwarm(SRBanking.ThriftInterface.NodeID leader, SRBanking.ThriftInterface.TransferID transfer)
        {
            if (blackList != null && blackList.Contains(leader))
            {
                throw new SRBanking.ThriftInterface.NotSwarmMemeber();
            }
            swamManager.PingSwarm(new NodeID(leader),new TransferID( transfer));
        }

        public void startSwarmElection(SRBanking.ThriftInterface.TransferID transfer)
        {
            throw new NotImplementedException();
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
            return;
        }
    }
}
