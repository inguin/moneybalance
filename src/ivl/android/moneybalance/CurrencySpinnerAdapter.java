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

public class CurrencySpinnerAdapter extends BaseAdapter {

	private final List<Currency> allCurrencies;
	private final Context context;

	public CurrencySpinnerAdapter(Context context) {
		allCurrencies = getAllCurrencies();
		this.context = context;
	}

	@Override
	public int getCount() {
		return allCurrencies.size();
	}

	@Override
	public Object getItem(int position) {
		return allCurrencies.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public int findItem(Currency currency) {
		for (int i = 0; i < allCurrencies.size(); i++)
			if (allCurrencies.get(i).equals(currency))
				return i;
		return -1;
	}

	public int findItem(String currencyCode) {
		for (int i = 0; i < allCurrencies.size(); i++)
			if (allCurrencies.get(i).getCurrencyCode().equals(currencyCode))
				return i;
		return -1;
	}

	public void hideItem(String currencyCode) {
		Currency currency = Currency.getInstance(currencyCode);
		allCurrencies.remove(currency);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view = (TextView) convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
		}

		Currency currency = allCurrencies.get(position);
		view.setText(getDisplayName(currency));
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		TextView view = (TextView) convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = (TextView) inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
		}

		Currency currency = allCurrencies.get(position);
		view.setText(getDisplayName(currency));
		return view;
	}

	@TargetApi(19)
	private static List<Currency> getAllCurrencies() {
		List<Currency> currencies = new ArrayList<Currency>();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			for (Currency currency : Currency.getAvailableCurrencies())
				currencies.add(currency);
		} else {
			// API <= 18 does not implement getAvailableLocales(), and Set<Currency> does
			// not seem to work.
			Set<String> codes = new TreeSet<String>();
			for (Locale locale : Locale.getAvailableLocales()) {
				try {
					codes.add(Currency.getInstance(locale).getCurrencyCode());
				} catch (Exception e) {}
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
		List<Currency> result = new ArrayList<Currency>(currencies);
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
