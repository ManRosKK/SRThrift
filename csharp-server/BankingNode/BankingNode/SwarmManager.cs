using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Collections.Concurrent;
using System.Timers;
using log4net;
using System.Reflection;
namespace BankingNode
{
    class SwarmManager
    {
        private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private struct SwarmDescription
        {
            public Timer timer ;
            public TransferData data;
        }
        public delegate void SwarmTimeoutDelegate(TransferID id,SwarmManager swarmManager);
        public delegate void SwarmLeaderTimeToPingDelegate(TransferID id, SwarmManager swarmManager);
        public SwarmTimeoutDelegate SwarmTimeout
        {
            set;
            get;
        }
        public SwarmLeaderTimeToPingDelegate SwarmLeaderTimeToPing
        {
            set;
            get;
        }
        private Dictionary<TransferID, Swarm> swarms = new Dictionary<TransferID, Swarm>();
        private Dictionary<TransferID, SwarmDescription> swarmsDescription = new Dictionary<TransferID, SwarmDescription>();

        public void UpdateSwarm(Swarm s)
        {
            if(swarms.ContainsKey(s.Transfer))
            {
                swarms[s.Transfer] = s;
            }else
                throw new SRBanking.ThriftInterface.NotSwarmMemeber();
        }
        public void CreateSwarm(Swarm s,TransferData data)
        {
            if (swarms.ContainsKey(s.Transfer))
            {
                throw new SRBanking.ThriftInterface.AlreadySwarmMemeber();
            }
            else
            {
                swarms.Add(s.Transfer, s);
                if (s.Leader == ConfigLoader.Instance.ConfigGetSelfId())
                {
                    //ja jestem liderem
                    SwarmDescription sd = new SwarmDescription();
                    sd.timer = new Timer();
                    sd.timer.Elapsed    += new ElapsedEventHandler( (object source, ElapsedEventArgs e)=>{SwarmLeaderTimeToPing(s.Transfer, this);});
                    sd.timer.Interval   = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.TimePingSwarm);
                    sd.timer.AutoReset  = true;
                    sd.data = data;
                    swarmsDescription.Add(s.Transfer, sd);
                    sd.timer.Enabled    = true;
                    sd.timer.Start();
                }
                else
                {
                    //ja jestem czlonkiem
                    SwarmDescription sd = new SwarmDescription();
                    sd.timer = new Timer();
                    sd.timer.Elapsed += new ElapsedEventHandler((object source, ElapsedEventArgs e) => { SwarmTimeout(s.Transfer, this); });
                    sd.timer.Interval = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.TimePingSwarm);
                    sd.timer.AutoReset = false;
                    sd.data = data;
                    swarmsDescription.Add(s.Transfer, sd);
                    sd.timer.Enabled = true;
                    sd.timer.Start();
                }
            }
        }
        public void DeleteSwarm(TransferID id)
        {
            if (swarms.ContainsKey(id))
            {
                swarms.Remove(id);
                swarmsDescription[id].timer.Dispose();
                swarmsDescription.Remove(id);
            }
            else
                throw new SRBanking.ThriftInterface.NotSwarmMemeber();     
        }
        public void PingSwarm(NodeID leader, TransferID id)
        {
            if (swarmsDescription.ContainsKey(id))
            {
                if (leader == swarms[id].Leader)
                {
                    swarmsDescription[id].timer.Dispose();
                    SwarmDescription sd = swarmsDescription[id];
                    sd.timer = new Timer();
                    sd.timer.Elapsed += new ElapsedEventHandler((object source, ElapsedEventArgs e) => { SwarmTimeout(id, this); });
                    sd.timer.Interval = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.TimePingSwarm);
                    sd.timer.AutoReset = false;

                    sd.timer.Enabled = true;
                    sd.timer.Start();
                }
                else
                {
                    throw new SRBanking.ThriftInterface.WrongSwarmLeader();
                }
                swarms.Remove(id);
                swarmsDescription[id].timer.Dispose();
                swarmsDescription.Remove(id);
            }else
                throw new SRBanking.ThriftInterface.NotSwarmMemeber(); 
        }
        public TransferData GetTransferData(TransferID id)
        {
            return swarmsDescription[id].data;
        }
        public List<NodeID> GetSwarmMembers(TransferID id)
        {
            return swarms[id].Members;
        }
        public Swarm GetSwarm(TransferID id)
        {
            return swarms[id];
        }
        public List<SRBanking.ThriftInterface.Swarm> GetSwarmList()
        {
            List<SRBanking.ThriftInterface.Swarm> ll = new List<SRBanking.ThriftInterface.Swarm>();
            foreach (Swarm x in swarms.Values)
            {
                ll.Add(x.ToBase());
            }
            return ll;
        }
    }
}
