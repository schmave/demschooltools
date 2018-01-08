package models;

public enum TransactionType
{
	CashDeposit,
    CashWithdrawal,
    CashTransfer,
    DigitalCredit,
    DigitalPurchase,
    DigitalTransfer;

    @Override
   	public String toString() {
    	switch (this) {
    		case CashDeposit: return "Cash Deposit";
    		case CashWithdrawal: return "Cash Withdrawal";
    		case CashTransfer: return "Cash Transfer";
    		case DigitalCredit: return "Digital Credit";
    		case DigitalPurchase: return "Digital Purchase";
    		case DigitalTransfer: return "Digital Transfer";
    		default: throw new IllegalArgumentException();
    	}
   	}
}