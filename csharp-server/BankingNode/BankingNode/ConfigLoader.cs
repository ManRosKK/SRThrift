using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
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
            if (key == ConfigLoaderKeys.SelfPort)
                return Int64.Parse(args[1]);
            if (key == ConfigLoaderKeys.Balance)
                return Int64.Parse(args[2]);
            if (key == ConfigLoaderKeys.SwarmSize)
                return Int64.Parse(iniDecoder.IniReadValue("config", "swarm_size"));
            if (key == ConfigLoaderKeys.TimeBetweenRetransfers)
                return Int64.Parse(iniDecoder.IniReadValue("config", "try_deliver_transfer_every"));
            if (key == ConfigLoaderKeys.TimePingSwarm)
                return Int64.Parse(iniDecoder.IniReadValue("config", "ping_swarm_every"));
            throw new ConfigWrongKeyException();
        }
        public Int64[][] ConfigGetRanges(ConfigLoaderKeys key)
        {
            if (key == ConfigLoaderKeys.PortList)
                return iniDecoder.IniReadInts("config", "port_list");

            throw new ConfigWrongKeyException();
        }
        public string ConfigGetString(ConfigLoaderKeys key)
        {
            if (key == ConfigLoaderKeys.SelfIp)
                return args[0];

            throw new ConfigWrongKeyException();        
        }
        public string[] ConfigGetStrings(ConfigLoaderKeys key)
        {
            if (key == ConfigLoaderKeys.IpList)
                return iniDecoder.IniReadStrings("config", "ip_list");

            throw new ConfigWrongKeyException();
        }
    }
}
