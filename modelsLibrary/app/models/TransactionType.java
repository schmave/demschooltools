package models;

public enum TransactionType
{
	CashDeposit,
    CashWithdrawal,
    DigitalTransaction;

    @Override
   	public String toString() {
    	switch (this) {
    		case CashDeposit: return "Cash Deposit";
    		case CashWithdrawal: return "Cash Withdrawal";
    		case DigitalTransaction: return "Digital Transaction";
    		default: throw new IllegalArgumentException();
    	}
   	}
}