package ivl.android.moneybalance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class CurrencySpinnerAdapter extends BaseAdapter {

	private final List<Currency> currencies;
	private final Context context;
	private boolean symbolOnly = false;

	public CurrencySpinnerAdapter(Context context) {
		this(context, getAllCurrencies());
	}

	public CurrencySpinnerAdapter(Context context, List<Currency> currencies) {
		this.currencies = currencies;
		this.context = context;
	}

	public void setSymbolOnly(boolean symbolOnly) {
		this.symbolOnly = symbolOnly;
	}

	@Override
	public int getCount() {
		return currencies.size();
	}

	@Override
	public Object getItem(int position) {
		return currencies.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public int findItem(Currency currency) {
		for (int i = 0; i < currencies.size(); i++)
			if (currencies.get(i).equals(currency))
				return i;
		return -1;
	}

	public int findItem(String currencyCode) {
		for (int i = 0; i < currencies.size(); i++)
			if (currencies.get(i).getCurrencyCode().equals(currencyCode))
				return i;
		return -1;
	}

	public void hideItem(String currencyCode) {
		Currency currency = Currency.getInstance(currencyCode);
		currencies.remove(currency);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view = (TextView) convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
		}

		Currency currency = currencies.get(position);
		view.setText(symbolOnly ? currency.getSymbol() : getDisplayName(currency));
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		TextView view = (TextView) convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = (TextView) inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
		}

		Currency currency = currencies.get(position);
		view.setText(getDisplayName(currency));
		return view;
	}

	@TargetApi(19)
	private static List<Currency> getAllCurrencies() {
		List<Currency> currencies = new ArrayList<>();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			for (Currency currency : Currency.getAvailableCurrencies())
				currencies.add(currency);
		} else {
			// API <= 18 does not implement getAvailableLocales(), and Set<Currency> does
			// not seem to work.
			Set<String> codes = new TreeSet<>();
			for (Locale locale : Locale.getAvailableLocales()) {
				try {
					codes.add(Currency.getInstance(locale).getCurrencyCode());
				} catch (Exception ignored) {}
			}
			for (String code : codes)
				currencies.add(Currency.getInstance(code));
		}

		Comparator<Currency> comparator = new Comparator<Currency>() {
			@Override
			public int compare(Currency lhs, Currency rhs) {
				return getDisplayName(lhs).compareTo(getDisplayName(rhs));
			}
		};
		List<Currency> result = new ArrayList<>(currencies);
		Collections.sort(result, comparator);
		return result;
	}

	@TargetApi(19)
	private static String getDisplayName(Currency currency) {
		String name;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			name = currency.getDisplayName();
		} else {
			name = currency.getCurrencyCode();
		}
		return String.format("%s (%s)", name, currency.getSymbol());
	}

}
