using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Thrift.Transport;
using Thrift.Protocol;
using System.Threading;
using log4net;
using System.Reflection;

namespace BankingNode
{
    class ClientManager
    {
        class ConnectionObject
        {
            public SRBanking.ThriftInterface.NodeService.Client client = null;
            public TSocket transport = null;
        }
        private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private bool isStopped = false;
        Dictionary<NodeID, ConnectionObject> clients = new Dictionary<NodeID,ConnectionObject>();
        Dictionary<NodeID, bool> blackList = new Dictionary<NodeID, bool>();
        public void connectToNode(NodeID node)
        {
            if (clients[node].client != null)
            {
                TSocket t = clients[node].transport;
                if (!t.IsOpen)
                {
                    try
                    {
                        t.Close();
                    }
                    catch
                    {
                    }
                    var protocol = new TBinaryProtocol(t);
                    clients[node].client = new SRBanking.ThriftInterface.NodeService.Client(protocol);
                    t.Open();
                }
            }
            else
            {
                clients[node].transport = new TSocket(node.IP, node.Port);
                var protocol = new TBinaryProtocol(clients[node].transport);
                clients[node].client = new SRBanking.ThriftInterface.NodeService.Client(protocol);
                clients[node].transport.Open();
            }
        }
        public SRBanking.ThriftInterface.NodeService.Client TakeClient(NodeID node)
        {

            lock (this)
            {
                if (!clients.ContainsKey(node))
                {
                    ConnectionObject xon = new ConnectionObject();
                    clients.Add(node, xon);
                }
            }
            Monitor.Enter(clients[node]);
            if(blackList.ContainsKey(node) || isStopped )
                throw new SRBanking.ThriftInterface.NotEnoughMoney();
            connectToNode(node);
            logerr.Warn("zajmuje: " + node);
            return clients[node].client;
        }
        public void ReturnClient(NodeID node)
        {
            logerr.Warn("oddaje: " + node);
            if(clients.ContainsKey(node))
                Monitor.Exit(clients[node]);    
        }
        public void SetBlackList(List<SRBanking.ThriftInterface.NodeID> list)
        {
            blackList.Clear();
            foreach (SRBanking.ThriftInterface.NodeID n in list)
            {
                blackList.Add(new NodeID(n),true);
            }
        }
        public bool IsOnBlackList(NodeID node)
        {
            return (blackList.ContainsKey(node) || isStopped);
        }

        internal void StopAll()
        {
            isStopped = true;
        }

        internal void StartAll()
        {
            isStopped = false;
        }
    }
}
