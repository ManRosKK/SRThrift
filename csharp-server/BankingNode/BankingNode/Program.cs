using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Collections.Concurrent;
using System.Threading;
using System.Threading.Tasks;
using Thrift;
using SRBanking;
using Ini;
using log4net;
using log4net.Config;
using System.Reflection;
using Thrift.Transport;
using Thrift.Server;
using System.Xml;
using System.Xml.XPath;
namespace BankingNode
{
    class Program
    {
        static private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        
        static void Main(string[] args)
        {
            try
            {
                log4net.GlobalContext.Properties["LogName"] = args[1] + ".log";
                XmlConfigurator.Configure();
                logerr.Info("Start application");
                ConfigLoader.Instance.Args = args;
                logerr.Info("Starting server");
              /*  SRBanking.ThriftInterface.NodeID x1 = new SRBanking.ThriftInterface.NodeID();
                x1.Port = 99;
                x1.IP ="kazik";
                SRBanking.ThriftInterface.NodeID x2 = new SRBanking.ThriftInterface.NodeID();
                x2.Port = 99;
                x2.IP ="kazik";
                NodeID xx = null;
                Dictionary<NodeID, int> ss = new Dictionary<NodeID, int>();
                ss.Add(new NodeID(x1), 9);

                logerr.Info(ss.ContainsKey(new NodeID(x2) ));
                logerr.Info(x1 == x2);
                logerr.Info((new NodeID(x1)) == (new NodeID(x2)));
                logerr.Info((new NodeID(x1)) == null);
                logerr.Info(xx== null);
                logerr.Info(null == xx);
                logerr.Info(xx == (new NodeID(x1)));
                logerr.Info((new NodeID(x1) ).Equals(new NodeID(x2)));*/
                var handler = new NodeServicesHandler();
                var processor = new SRBanking.ThriftInterface.NodeService.Processor(handler);

                TServerTransport transport = new TServerSocket((int)ConfigLoader.Instance.ConfigGetInt(ConfigLoader.ConfigLoaderKeys.SelfPort));
                TServer server = new TThreadPoolServer(processor, transport);
                handler.server = server;
                logerr.Info("Server started");
                server.Serve();
                
                logerr.Info("Server closed");
                //Console.WriteLine("xxxx {0}",args[0]);
            }
            catch (Exception ex)
            {
                logerr.Error("Error in Main", ex);
            }
        }
    }
}
