package ivl.android.moneybalance;

import java.util.ArrayList;
import java.util.List;

import ivl.android.moneybalance.dao.CalculationDataSource;
import ivl.android.moneybalance.dao.DataBaseHelper;
import ivl.android.moneybalance.data.Calculation;
import ivl.android.moneybalance.data.Currency;
import ivl.android.moneybalance.data.Expense;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class ManageCurrenciesActivity extends ActionBarActivity {

	public static final String PARAM_CALCULATION_ID = "calculationId";

	private final DataBaseHelper dbHelper = new DataBaseHelper(this);
	private final CalculationDataSource calculationDataSource = new CalculationDataSource(dbHelper);
	private Calculation calculation;

	private class CurrencyEntry {
		public Currency currency;
		public boolean used;
	}

	private Spinner mainCurrencyField;
	private final List<CurrencyEntry> additionalCurrencies = new ArrayList<>();
	private AdditionalCurrencyAdapter additionalCurrencyAdapter;
	private ListView additionalCurrencyList;

	private class AdditionalCurrencyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// plus one for "Add Currency"
			return additionalCurrencies.size() + 1;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.currency_list_entry, parent, false);

			TextView textView = (TextView) view.findViewById(R.id.currency_details);
			ImageView deleteButton = (ImageView) view.findViewById(R.id.delete_button);
			textView.setId(position);

			boolean showDeleteButton = false;

			if (position < additionalCurrencies.size()) {
				final Currency thisCurrency = additionalCurrencies.get(position).currency;
				java.util.Currency newMainCurrency = (java.util.Currency) mainCurrencyField.getSelectedItem();
				CurrencyHelper mainCurrencyHelper = new CurrencyHelper(newMainCurrency);

				String text = String.format("%s (%s = %s)",	thisCurrency.getCurrencyCode(),
								thisCurrency.getCurrencyHelper().format(thisCurrency.getExchangeRateThis()),
								mainCurrencyHelper.format(thisCurrency.getExchangeRateMain()));

				textView.setText(text);
				textView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						showEditCurrencyDialog(thisCurrency);
					}
				});

				showDeleteButton = !additionalCurrencies.get(position).used;

				deleteButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						deleteCurrency(thisCurrency);
					}
				});
			} else {
				textView.setText(R.string.add_currency);
				textView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						showEditCurrencyDialog(null);
					}
				});
			}

			deleteButton.setVisibility(showDeleteButton ? View.VISIBLE : View.INVISIBLE);
			return view;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR);

		setContentView(R.layout.manage_currencies);
		setTitle(R.string.manage_currencies);

		Intent intent = getIntent();
		long calculationId = intent.getLongExtra(PARAM_CALCULATION_ID, -1);
		calculation = calculationDataSource.get(calculationId);

		mainCurrencyField = (Spinner) findViewById(R.id.main_currency);
		CurrencySpinnerAdapter mainCurrencyAdapter = new CurrencySpinnerAdapter(this);
		mainCurrencyField.setAdapter(mainCurrencyAdapter);
		int selected = mainCurrencyAdapter.findItem(calculation.getMainCurrencyCode());
		mainCurrencyField.setSelection(selected);
		mainCurrencyField.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				update();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		additionalCurrencyAdapter = new AdditionalCurrencyAdapter();
		additionalCurrencyList = (ListView) findViewById(R.id.additional_currency_list);

		update();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.manage_currencies_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_save:
				doSave();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void update() {
		additionalCurrencies.clear();
		java.util.Currency newMainCurrency = (java.util.Currency) mainCurrencyField.getSelectedItem();

		for (Currency currency : calculation.getCurrencies()) {
			if (!currency.getCurrencyCode().equals(newMainCurrency.getCurrencyCode())) {
				CurrencyEntry entry = new CurrencyEntry();
				entry.currency = currency;
				entry.used = false;
				additionalCurrencies.add(entry);
			}
		}

		for (Expense expense : calculation.getExpenses())
			for (CurrencyEntry entry : additionalCurrencies)
				if (entry.currency.equals(expense.getCurrency()))
					entry.used = true;

		additionalCurrencyList.setAdapter(additionalCurrencyAdapter);
	}

	private void deleteCurrency(Currency currency) {
		if (!currency.equals(calculation.getMainCurrency()))
			calculation.getCurrencies().remove(currency);
		update();
	}

	private void showEditCurrencyDialog(final Currency currency) {
		DialogFragment fragment = new DialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				List<Currency> hiddenCurrencies = new ArrayList<>();
				for (Currency c : calculation.getCurrencies())
					if (!c.equals(currency))
						hiddenCurrencies.add(c);

				final CurrencyEditor editor = new CurrencyEditor(getActivity(), calculation.getMainCurrency(), hiddenCurrencies);
				if (currency != null)
					editor.setValue(currency);

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setView(editor.getView());
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setNegativeButton(android.R.string.cancel, null);
				final AlertDialog dialog = builder.create();

				// register custom listener with validation for positive button
				dialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface di) {
						Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
						okButton.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (editor.validate()) {
									Currency newCurrency = editor.getValue(calculation.getId());
									if (currency != null) {
										currency.setExchangeRate(newCurrency.getExchangeRateThis(), newCurrency.getExchangeRateMain());
									} else {
										calculation.getCurrencies().add(newCurrency);
									}
									dismiss();
									update();
								}
							}
						});
					}
				});

				return dialog;
			}
		};

		fragment.show(getSupportFragmentManager(), "editCurrency");
	}

	private void doSave() {
		java.util.Currency newMainCurrency = (java.util.Currency) mainCurrencyField.getSelectedItem();
		calculation.setMainCurrencyCode(newMainCurrency.getCurrencyCode());
		calculationDataSource.update(calculation);
		finish();
	}

}
