using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using log4net;
using System.Reflection;

namespace BankingNode
{
    class Swarm
    {
        private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private SRBanking.ThriftInterface.Swarm internalObject = null;
       /* private List<NodeID> internallist;
        private NodeID internalLeader;
        private TransferID internalTransferID;*/
        public TransferID Transfer
        {
            set
            {
                if (value == null)
                {
                    internalObject.Transfer = null;
                }else
                    internalObject.Transfer = value.ToBase();
            }
            get
            {
                return new TransferID(internalObject.Transfer);
            }
        }
        public NodeID Leader
        {
            set
            {
                if (value != null)
                    internalObject.Leader = value.ToBase();
                else
                    internalObject.Leader = null;
            }
            get
            {
                return new NodeID(internalObject.Leader);
            }
        }
        public List<NodeID> Members
        {
            set
            {
                if (value != null)
                {
                    internalObject.Members = new List<SRBanking.ThriftInterface.NodeID>();
                    foreach (NodeID x in value)
                    {
                        internalObject.Members.Add(x.ToBase());
                    }
                }
            }
            get
            {
                List<NodeID> tmp = new List<NodeID>();
                foreach (SRBanking.ThriftInterface.NodeID x in internalObject.Members)
                {
                    tmp.Add(new NodeID(x));
                }
                return tmp;
            }
        }
        public void AddToSwarm(NodeID node)
        {
            internalObject.Members.Add(node.ToBase());
        }
        public static bool operator ==(Swarm c1, Swarm c2)
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
        public static bool operator !=(Swarm c1, Swarm c2)
        {
            
            return !(c1 == c2);
        }
        public Swarm(SRBanking.ThriftInterface.Swarm node = null)
        {
            if (node == null)
            {
                internalObject = new SRBanking.ThriftInterface.Swarm();
                internalObject.Members = new List<SRBanking.ThriftInterface.NodeID>();
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
            if (!(((Swarm)obj).Leader == Leader && ((Swarm)obj).Transfer == Transfer))
                return false;
            if (((Swarm)obj).Members.Count != Members.Count)
                return false;
            foreach (NodeID x in ((Swarm)obj).Members)
            {

                bool flag = true;
                foreach (NodeID y in Members)
                {
                    if (x == y)
                    {
                        flag = false;
                        break;
                    }
                }
                if (flag) return false;
            }
            return true;
        }
        public int CompareTo(object obj)
        {
            return Equals(this, obj)?1:0;
        }
        public override int GetHashCode()
        {
            return this.Leader.GetHashCode()+this.Transfer.GetHashCode();
        }
        public SRBanking.ThriftInterface.Swarm ToBase()
        {
            return internalObject;
        }
        public override string ToString()
        {
            string text = "swarm:";
            text += Transfer.ToString();
            if(Leader == null)
                text += "<null" +   "> --->";
            else
                text+= "<"+Leader.ToString()+"> --->";
            foreach(NodeID x in Members)
            {
                if (x == null)
                {
                    text += "null,";
                }else
                    text += x.ToString() + ",";
            }
            return text;
        }

        public void DeleteMember(NodeID x)
        {
            internalObject.Members.Remove(x.ToBase());
        }
    }
}
