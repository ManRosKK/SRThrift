﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using log4net;
using System.Reflection;
using System.Net;

namespace BankingNode
{
    class NodeID : IComparable
    {
        private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private SRBanking.ThriftInterface.NodeID internalObject = null;
        public string IP
        {
          get
          {
              return internalObject.IP;
          }
          set
          {
              internalObject.IP = value;
          }
        }

        public int Port
        {
            set
            {
                internalObject.Port = value;
            }
            get
            {
                return internalObject.Port;
            }
        }
        public static bool operator >(NodeID c1, NodeID c2)
        {
            int addr1 = BitConverter.ToInt32(IPAddress.Parse(c1.IP).GetAddressBytes(), 0);
            int addr2 = BitConverter.ToInt32(IPAddress.Parse(c2.IP).GetAddressBytes(), 0);
            long n1 = addr1;
            long n2 = addr2;
            n1 += ((long)c1.Port) << 32;
            n2 += ((long)c2.Port) << 32;
            return n1 > n2;
        }
        public static bool operator <(NodeID c1, NodeID c2)
        {
            int addr1 = BitConverter.ToInt32(IPAddress.Parse(c1.IP).GetAddressBytes(), 0);
            int addr2 = BitConverter.ToInt32(IPAddress.Parse(c2.IP).GetAddressBytes(), 0);
            long n1 = addr1;
            long n2 = addr2;
            n1 += ((long)c1.Port) << 32;
            n2 += ((long)c2.Port) << 32;
            return n1 < n2;
        }
        public static bool operator ==(NodeID c1, NodeID c2)
        {
            if (((object)c1) == ((object)c2))
            {
                return true;
            }
            if (((object)c1) == null)
            {
                return false;
            }
            return c1.Equals(c2);
        }
        public static bool operator!= (NodeID c1, NodeID c2)
        {
            
            return !(c1 == c2);
        }
        public NodeID(SRBanking.ThriftInterface.NodeID node = null)
        {
            if (node == null)
            {
                internalObject = new SRBanking.ThriftInterface.NodeID();
            }
            else
            {
                internalObject = node;
            }
        }
        public override bool Equals(object obj)
        {
            if ((obj) == null)
            {
                return false;
            }
            return (((NodeID)obj).IP == IP && ((NodeID)obj).Port == Port); 
        }
        public int CompareTo(object obj)
        {
            return Equals(this, obj)?1:0;
        }
        public override int GetHashCode()
        {
            return this.IP.GetHashCode()+this.Port.GetHashCode();
        }
        public SRBanking.ThriftInterface.NodeID ToBase()
        {
            return internalObject;
        }
        public override string ToString()
        {
            return "[" + IP.ToString() + "|" + Port.ToString() + "]";
        }

    }
}
