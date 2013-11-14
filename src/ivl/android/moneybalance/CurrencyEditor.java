package ivl.android.moneybalance;

import ivl.android.moneybalance.data.Currency;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class CurrencyEditor {

	private final View view;
	private final CurrencySpinnerAdapter spinnerAdapter;
	private final Spinner currencyField;
	private final TextView thisCurrencyLabel;
	private final TextView mainCurrencyLabel;
	private final EditText thisCurrencyRate;
	private final EditText mainCurrencyRate;

	private Currency mainCurrency;

	public CurrencyEditor(Context context, Currency mainCurrency, List<Currency> hiddenCurrencies) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.currency_editor, null, false);

		this.mainCurrency = mainCurrency;
		spinnerAdapter = new CurrencySpinnerAdapter(context);
		for (Currency c : hiddenCurrencies)
			spinnerAdapter.hideItem(c.getCurrencyCode());

		currencyField = (Spinner) view.findViewById(R.id.additional_currency_spinner);
		currencyField.setAdapter(spinnerAdapter);
		currencyField.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				updateSelectedCurrency();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		thisCurrencyLabel = (TextView) view.findViewById(R.id.currency_symbol_this);
		mainCurrencyLabel = (TextView) view.findViewById(R.id.currency_symbol_main);
		mainCurrencyLabel.setText(mainCurrency.getSymbol());

		thisCurrencyRate = (EditText) view.findViewById(R.id.exchange_rate_this);
		mainCurrencyRate = (EditText) view.findViewById(R.id.exchange_rate_main);

		updateSelectedCurrency();
	}

	public View getView() {
		return view;
	}

	public void setValue(Currency value) {
		int position = spinnerAdapter.findItem(value.getCurrencyCode());
		currencyField.setSelection(position);
		currencyField.setEnabled(false);
		thisCurrencyLabel.setText(value.getSymbol());

		setExchangeRateThis(value.getExchangeRateThis());
		setExchangeRateMain(value.getExchangeRateMain());
	}

	public boolean validate() {
		// TODO
		boolean valid = true;
		try {
			getExchangeRateThis();
			getExchangeRateMain();
		} catch (ParseException e) {
			valid = false;
		}
		return valid;
	}

	public Currency getValue(long calculationId) {
		try {
			Currency value = new Currency(calculationId);
			value.setCurrencyCode(getSelectedCurrencyCode());
			value.setExchangeRate(getExchangeRateThis(), getExchangeRateMain());
			return value;
		} catch (ParseException e) {
			return null;
		}
	}

	private void updateSelectedCurrency() {
		java.util.Currency c = (java.util.Currency) currencyField.getSelectedItem();
		thisCurrencyLabel.setText(c.getSymbol());
	}

	private String getSelectedCurrencyCode() {
		java.util.Currency c = (java.util.Currency) currencyField.getSelectedItem();
		return c.getCurrencyCode();
	}

	private void setExchangeRateThis(double rate) {
		java.util.Currency c = (java.util.Currency) currencyField.getSelectedItem();
		CurrencyHelper helper = new CurrencyHelper(c);
		thisCurrencyRate.setText(helper.format(rate, false));
	}

	private double getExchangeRateThis() throws ParseException {
		String str = thisCurrencyRate.getText().toString();
		return NumberFormat.getNumberInstance().parse(str).doubleValue();
	}

	private void setExchangeRateMain(double rate) {
		CurrencyHelper helper = mainCurrency.getCurrencyHelper();
		mainCurrencyRate.setText(helper.format(rate, false));
	}

	private double getExchangeRateMain() throws ParseException {
		String str = mainCurrencyRate.getText().toString();
		return NumberFormat.getNumberInstance().parse(str).doubleValue();
	}

}
