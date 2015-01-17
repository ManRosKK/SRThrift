package model;

import SRBanking.ThriftInterface.TransferData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by MarJan
 */
public class Account implements Serializable{

    private long balance;
    private List<TransferData> transferHistory;

    public Account(long balance)
    {
        this.balance = balance;
        transferHistory = new ArrayList<TransferData>();
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public boolean isTransferPossible(long transferMoney)
    {
        return balance > transferMoney;
    }

    public List<TransferData> getTransferHistory() {
        return transferHistory;
    }

    public void addTransferToHistory(TransferData transferData)
    {
        transferHistory.add(transferData);
    }
}
