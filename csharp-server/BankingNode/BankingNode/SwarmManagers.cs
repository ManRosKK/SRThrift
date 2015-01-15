using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Collections.Concurrent;

namespace BankingNode
{
    class SwarmManagers
    {
        private Dictionary<SRBanking.ThriftInterface.TransferID, SRBanking.ThriftInterface.Swarm> swarms = new Dictionary<SRBanking.ThriftInterface.TransferID, SRBanking.ThriftInterface.Swarm>();
        private Dictionary<SRBanking.ThriftInterface.TransferID, SRBanking.ThriftInterface.Swarm> swarmsDescription = new Dictionary<SRBanking.ThriftInterface.TransferID, SRBanking.ThriftInterface.Swarm>();
        private BlockingCollection<Action> taskQueue = new BlockingCollection<Action>();

        public void UpdateSwarm(SRBanking.ThriftInterface.Swarm s)
        {
            if(swarms.ContainsKey(s.Transfer))
            {
                swarms[s.Transfer] = s;
            }else
                throw new SRBanking.ThriftInterface.NotSwarmMemeber();
        }
        public void CreateSwarm(SRBanking.ThriftInterface.Swarm s)
        {
            if (swarms.ContainsKey(s.Transfer))
            {
                throw new SRBanking.ThriftInterface.AlreadySwarmMemeber();
            }
            else
                swarms.Add(s.Transfer, s);
        }
        public void DeleteSwarm(SRBanking.ThriftInterface.TransferID id)
        {
            if (swarms.ContainsKey(id))
            {
                swarms.Remove(id);   
            }
            else
                throw new SRBanking.ThriftInterface.NotSwarmMemeber();     
        }
    }
}
