package ivl.android.moneybalance.data;

import ivl.android.moneybalance.CurrencyHelper;

import java.util.Locale;

public class Currency extends DataObject {

	private final long calculationId;
	private String currencyCode;
	private long decimalFactor;
	private double rateThis;
	private double rateMain;

	public Currency(long calculationId) {
		this.calculationId = calculationId;
		setExchangeRate(1.0, 1.0);
		decimalFactor = 1;
	}

	public long getCalculationId() {
		return calculationId;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
		java.util.Currency jc = java.util.Currency.getInstance(currencyCode);
		decimalFactor = Math.round(Math.pow(10, jc.getDefaultFractionDigits()));
	}

	public long getDecimalFactor() {
		return decimalFactor;
	}

	public double getExchangeRateThis() {
		return rateThis;
	}
	public double getExchangeRateMain() {
		return rateMain;
	}
	public void setExchangeRate(double rateThis, double rateMain) {
		this.rateThis = rateThis;
		this.rateMain = rateMain;
	}

	public String getSymbol() {
		java.util.Currency currency = java.util.Currency.getInstance(currencyCode);
		return currency.getSymbol();
	}

	public CurrencyHelper getCurrencyHelper() {
		java.util.Currency currency = java.util.Currency.getInstance(currencyCode);
		return new CurrencyHelper(currency);
	}
	public CurrencyHelper getCurrencyHelper(Locale locale) {
		java.util.Currency currency = java.util.Currency.getInstance(currencyCode);
		return new CurrencyHelper(currency, locale);
	}

	public double exchangeAmount(double thisCurrencyAmount) {
		return thisCurrencyAmount * rateMain / rateThis;
	}

}
