using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using log4net;
using System.Reflection;

namespace BankingNode
{
    class TransferID
    {
        private readonly ILog logerr = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private SRBanking.ThriftInterface.TransferID internalObject = null;
        public NodeID Sender
        {
            set
            {
                if (value == null)
                {
                    internalObject.Sender = null;
                }else
                    internalObject.Sender = value.ToBase();
            }
            get
            {
                return new NodeID(internalObject.Sender);
            }
        }
        public long Counter
        {
            set
            {
                internalObject.Counter = value;
            }
            get
            {
                return internalObject.Counter;
            }
        }
        public static bool operator ==(TransferID c1, TransferID c2)
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
        public static bool operator !=(TransferID c1, TransferID c2)
        {
            
            return !(c1 == c2);
        }
        public TransferID(SRBanking.ThriftInterface.TransferID tid = null)
        {
            if (tid == null)
            {
                internalObject = new SRBanking.ThriftInterface.TransferID();
            }
            else
            {
                internalObject = tid;
            }
        }
        public override bool Equals(object obj)
        {
            if ((obj) == null)
            {
                return false;
            }
            return (((TransferID)obj).Sender == Sender && ((TransferID)obj).Counter == Counter); 
        }
        public int CompareTo(object obj)
        {
            return Equals(this, obj)?1:0;
        }
        public override int GetHashCode()
        {
            return this.Sender.GetHashCode()+this.Counter.GetHashCode();
        }
        public SRBanking.ThriftInterface.TransferID ToBase()
        {
            return internalObject;
        }
    }
}
