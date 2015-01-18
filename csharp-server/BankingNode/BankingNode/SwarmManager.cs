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
        public enum SwarmState
        {
            Idle,
            Election,
            Dirty
        }
        private class SwarmDescription
        {
            public Timer timer ;
            public TransferData data;
            public SwarmState state;
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

            logerr.Info("CREATE SWARM : " + swarms.Count);
            if (swarms.ContainsKey(s.Transfer))
            {
                throw new SRBanking.ThriftInterface.AlreadySwarmMemeber();
            }
            else
            {
                swarms.Add(s.Transfer, s);
                logerr.Info("SWARM : " + swarms.Count);
                if (s.Leader == ConfigLoader.Instance.ConfigGetSelfId())
                {
                    //ja jestem liderem

                    logerr.Info("---SETTING ping time): " +s);
                    SwarmDescription sd = new SwarmDescription();
                    sd.timer = new Timer();
                    sd.timer.Elapsed    += new ElapsedEventHandler( (object source, ElapsedEventArgs e)=>{SwarmLeaderTimeToPing(s.Transfer, this);});
                    sd.timer.Interval   = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.TimePingSwarm);
                    sd.timer.AutoReset  = true;
                    sd.data = data;
                    sd.state = SwarmState.Dirty;
                    swarmsDescription.Add(s.Transfer, sd);
                    sd.timer.Enabled    = true;
                    sd.timer.Start();
                }
                else
                {
                    logerr.Info("---SETTING TIMEOUT): " + s);
                    //ja jestem czlonkiem
                    SwarmDescription sd = new SwarmDescription();
                    sd.timer = new Timer();
                    sd.timer.Elapsed += new ElapsedEventHandler((object source, ElapsedEventArgs e) => { SwarmTimeout(s.Transfer, this); });
                    sd.timer.Interval = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.TimePingSwarm)*4+5000;
                    sd.timer.AutoReset = false;
                    sd.data = data;
                    sd.state = SwarmState.Idle;
                    swarmsDescription.Add(s.Transfer, sd);
                    sd.timer.Enabled = true;
                    sd.timer.Start();
                }
            }
            logerr.Info("SWARM : " + swarms.Count);
            GetSwarmList();
        }
        public void DirtySwarm(TransferID id)
        {
            logerr.Info("w dirtyswarm" + id);
            if (swarmsDescription.ContainsKey(id))
            {
                logerr.Info("w dirtyswarm 2222 " + id);
                logerr.Info("w dirtyswarm xxxx" + swarmsDescription[id].state);
                if (swarmsDescription[id].state != SwarmState.Election)
                {
                    logerr.Info("w dirtyswarm 3333 " + id);
                    swarmsDescription[id].state = SwarmState.Dirty;
                    logerr.Info("w dirtyswarm yyyyy" + swarmsDescription[id].state);
                }
                return;
            }
            throw new SRBanking.ThriftInterface.NotSwarmMemeber();
        }
        public void CleanSwarm(TransferID id)
        {
            if (swarmsDescription.ContainsKey(id))
            {
                if (swarmsDescription[id].state != SwarmState.Election)
                    swarmsDescription[id].state = SwarmState.Idle;
                return;
            }
            throw new SRBanking.ThriftInterface.NotSwarmMemeber();
        }
        public bool IsDirtySwarm(TransferID id)
        {
            if (swarmsDescription.ContainsKey(id))
            {
                return (swarmsDescription[id].state == SwarmState.Dirty);
            }
            throw new SRBanking.ThriftInterface.NotSwarmMemeber();
        }
        public void BeginElection(TransferID id)
        {
            if (swarms[id].Leader != ConfigLoader.Instance.ConfigGetSelfId())
            {
                swarms[id].Leader = null;
                swarmsDescription[id].state = SwarmState.Election;
            }
        }
        public void EndElection(TransferID id)
        {
            if (swarms[id].Leader == ConfigLoader.Instance.ConfigGetSelfId())
            {
                //ja jestem liderem
                SwarmDescription sd = swarmsDescription[id];
                sd.timer.Dispose();
                sd.timer = new Timer();
                sd.timer.Elapsed += new ElapsedEventHandler((object source, ElapsedEventArgs e) => { SwarmLeaderTimeToPing(id, this); });
                sd.timer.Interval = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.TimePingSwarm);
                sd.timer.AutoReset = true;
                sd.state = SwarmState.Dirty;
                sd.timer.Enabled = true;
                sd.timer.Start();
            }
            else
            {
                //ja jestem czlonkiem
                SwarmDescription sd =  swarmsDescription[id];
                sd.timer = new Timer();
                sd.timer.Elapsed += new ElapsedEventHandler((object source, ElapsedEventArgs e) => { SwarmTimeout(id, this); });
                sd.timer.Interval = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.TimePingSwarm) * 4 + 5000; ;
                sd.timer.AutoReset = false;
                sd.state = SwarmState.Idle;
                sd.timer.Enabled = true;
                sd.timer.Start();
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
            logerr.Info("SWARM PINGED BY: " + leader + " | " + id);
            if (swarmsDescription.ContainsKey(id))
            {
                if (leader == swarms[id].Leader )
                {
                    if (leader == ConfigLoader.Instance.ConfigGetSelfId())
                        return;
                    swarmsDescription[id].timer.Dispose();
                    SwarmDescription sd = swarmsDescription[id];
                    sd.timer.Stop();
                    sd.timer.Dispose();

                    logerr.Info("SWARM PINGED BY (SETTING TIMEOUT): " + leader + " | " + id);
                    sd.timer = new Timer();
                    sd.timer.Elapsed += new ElapsedEventHandler((object source, ElapsedEventArgs e) => { SwarmTimeout(id, this); });
                    sd.timer.Interval = ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.TimePingSwarm)*4+5000;
                    sd.timer.AutoReset = false;

                    sd.timer.Enabled = true;
                    sd.timer.Start();
                }
                else
                {

                    throw new SRBanking.ThriftInterface.WrongSwarmLeader();
                }
                //swarms.Remove(id);
                //swarmsDescription[id].timer.Dispose();
                //swarmsDescription.Remove(id);
            }
            else
            {

                //logerr.Info("NOT SWARM MEMBER " + leader + " | " + id+"--->"+swarms[id]);
                throw new SRBanking.ThriftInterface.NotSwarmMemeber();
            }
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
            if(swarms.ContainsKey(id))
                return swarms[id];
            throw new SRBanking.ThriftInterface.NotSwarmMemeber();
        }
        public List<SRBanking.ThriftInterface.Swarm> GetSwarmList()
        {
            List<SRBanking.ThriftInterface.Swarm> ll = new List<SRBanking.ThriftInterface.Swarm>();
            logerr.Info("GET SWARM LIST: " + swarms.Count+"|" +swarms.Values.Count.ToString());
            foreach (Swarm x in swarms.Values)
            {
                ll.Add(x.ToBase());
                logerr.Info("SWARM name: " + x);
            }
            return ll;
        }

    }
}
