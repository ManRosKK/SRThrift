using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using log4net;
using System.Reflection;
namespace BankingNode
{
    
    class ConfigLoader
    {
        public enum ConfigLoaderKeys
        {
            SelfIp,
            SelfPort,
            Balance,
            SwarmSize,
            IpList,
            PortList,
            TimeBetweenRetransfers,
            TimePingSwarm
        };

        private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private string path = "";
        private string[] args;
        Ini.IniFile iniDecoder;
        static private ConfigLoader self = null;
        private ConfigLoader()
        {
        }
        public string[] Args
        {
            set
            {
                args = value;
                if (args.Length < 4)
                {
                    throw new ArgumentException("Init parameter");
                }
                path = args[3];
                iniDecoder = new Ini.IniFile(path);
                logerr.Info(this);
            }
        }
        static public ConfigLoader Instance
        {
            get
            {
                if (self == null )
                    self = new ConfigLoader();
                return self;
            }
        }
        public Int64 ConfigGetInt(ConfigLoaderKeys key)
        {
            logerr.Info(key);
            if (key == ConfigLoaderKeys.SelfPort)
            {
                logerr.Info(args[1]);
                return Int64.Parse(args[1]);
            }
            if (key == ConfigLoaderKeys.Balance)
            {
                logerr.Info(args[2]);
                return Int64.Parse(args[2]);
            }
            if (key == ConfigLoaderKeys.SwarmSize)
            {
                logerr.Info(iniDecoder.IniReadValue("config", "swarm_size"));
                return Int64.Parse(iniDecoder.IniReadValue("config", "swarm_size"));
            }
            if (key == ConfigLoaderKeys.TimeBetweenRetransfers)
            {
                logerr.Info(iniDecoder.IniReadValue("config", "try_deliver_transfer_every"));
                return Int64.Parse(iniDecoder.IniReadValue("config", "try_deliver_transfer_every"));
            }
            if (key == ConfigLoaderKeys.TimePingSwarm)
            {
                logerr.Info(iniDecoder.IniReadValue("config", "ping_swarm_every"));
                return Int64.Parse(iniDecoder.IniReadValue("config", "ping_swarm_every"));
            }
            throw new ConfigWrongKeyException();
        }
        public Int64[][] ConfigGetRanges(ConfigLoaderKeys key)
        {
            logerr.Info(key);
            if (key == ConfigLoaderKeys.PortList)
            {
                return iniDecoder.IniReadInts("config", "port_list");
            }

            throw new ConfigWrongKeyException();
        }
        public string ConfigGetString(ConfigLoaderKeys key)
        {
            logerr.Info(key);
            if (key == ConfigLoaderKeys.SelfIp)
            {
                logerr.Info(args[0]);
                return args[0];
            }

            throw new ConfigWrongKeyException();        
        }
        public string[] ConfigGetStrings(ConfigLoaderKeys key)
        {
            logerr.Info(key);
            if (key == ConfigLoaderKeys.IpList)
                return iniDecoder.IniReadStrings("config", "ip_list");

            throw new ConfigWrongKeyException();
        }
        public NodeID ConfigGetSelfId()
        {
            
            SRBanking.ThriftInterface.NodeID self = new SRBanking.ThriftInterface.NodeID();
            self.IP = ConfigGetString(ConfigLoaderKeys.SelfIp);
            self.Port = (int)ConfigGetInt(ConfigLoaderKeys.SelfPort);
            logerr.Info(self.IP.ToString()+"|"+ self.Port.ToString());
            return new NodeID(self);
        }
        public override string  ToString()
        {
            string text = "Args: ";
            for (int i = 0; i < args.Length; i++)
            {
                text += args[i] + "|";
            }
            return text;
        }
    }
}
