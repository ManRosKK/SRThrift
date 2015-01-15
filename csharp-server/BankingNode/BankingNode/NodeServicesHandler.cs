using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Thrift;
using SRBanking;
namespace BankingNode
{
    class NodeServicesHandler : SRBanking.ThriftInterface.NodeService.Iface
    {
        public void addToSwarm(SRBanking.ThriftInterface.Swarm swarm, SRBanking.ThriftInterface.TransferData transferData)
        {
            throw new NotImplementedException();
        }

        public void delSwarm(SRBanking.ThriftInterface.TransferID swarmID)
        {
            throw new NotImplementedException();
        }

        public void deliverTransfer(SRBanking.ThriftInterface.TransferData transfer)
        {
            throw new NotImplementedException();
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
            throw new NotImplementedException();
        }

        public SRBanking.ThriftInterface.Swarm getSwarm(SRBanking.ThriftInterface.TransferID transfer)
        {
            throw new NotImplementedException();
        }

        public List<SRBanking.ThriftInterface.Swarm> getSwarmList()
        {
            throw new NotImplementedException();
        }

        public List<SRBanking.ThriftInterface.TransferData> getTransfers()
        {
            throw new NotImplementedException();
        }

        public void makeTransfer(SRBanking.ThriftInterface.NodeID receiver, long value)
        {
            throw new NotImplementedException();
        }

        public void pingSwarm(SRBanking.ThriftInterface.NodeID leader, SRBanking.ThriftInterface.TransferID transfer)
        {
            throw new NotImplementedException();
        }

        public void startSwarmElection(SRBanking.ThriftInterface.TransferID transfer)
        {
            throw new NotImplementedException();
        }

        public void stop()
        {
            throw new NotImplementedException();
        }

        public void updateSwarmMembers(SRBanking.ThriftInterface.Swarm swarm)
        {
            throw new NotImplementedException();
        }

        public void AddBlackList(List<SRBanking.ThriftInterface.NodeID> blackList)
        {
            throw new NotImplementedException();
        }

        public void ping(SRBanking.ThriftInterface.NodeID sender)
        {
            throw new NotImplementedException();
        }
    }
}
