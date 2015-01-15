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
namespace BankingNode
{
    class Program
    {
        static void Main(string[] args)
        {
            ConfigLoader.Instance.Args = args;
            long[][] xxx = ConfigLoader.Instance.ConfigGetRanges(ConfigLoader.ConfigLoaderKeys.PortList);
            Console.WriteLine("xxxx {0}",args[0]);
            Console.WriteLine("xxxx");
        }
    }
}
