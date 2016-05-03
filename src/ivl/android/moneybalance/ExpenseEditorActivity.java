/*
 * MoneyBalance - Android-based calculator for tracking and balancing expenses
 * Copyright (C) 2012 Ingo van Lil <inguin@gmx.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ivl.android.moneybalance;

import ivl.android.moneybalance.dao.CalculationDataSource;
import ivl.android.moneybalance.dao.DataBaseHelper;
import ivl.android.moneybalance.dao.ExpenseDataSource;
import ivl.android.moneybalance.data.Currency;
import ivl.android.moneybalance.data.Calculation;
import ivl.android.moneybalance.data.Expense;
import ivl.android.moneybalance.data.Person;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class ExpenseEditorActivity extends ActionBarActivity {

	public static final String PARAM_EXPENSE_ID = "expenseId";
	public static final String PARAM_PERSON_ID = "personId";
	public static final String PARAM_CALCULATION_ID = "calculationId";
	public static final String PARAM_DATE = "date";

	private enum  Mode { NEW_EXPENSE, EDIT_EXPENSE }
	private Mode mode;
	private Expense expense;

	private final DataBaseHelper dbHelper = new DataBaseHelper(this);

	private final CalculationDataSource calculationDataSource = new CalculationDataSource(dbHelper);
	private ExpenseDataSource expenseDataSource;

	private Calculation calculation;
	private List<Person> persons;

	private AutoCompleteTextView titleView;
	private EditText amountView;
	private Spinner currencySpinner;
	private TextView payerView;
	private TextView dateView;

	private static class CustomSplitEntry {
		CheckBox enabled;
		EditText weight;
		TextView result;
	}
	private CustomSplitEntry[] customSplitEntries;

	private CheckBox customSplitCheckBox;
	private TableLayout customSplitTable;

	private CurrencyHelper currencyHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR);

		setContentView(R.layout.expense_editor);
		titleView = (AutoCompleteTextView) findViewById(R.id.expense_title);
		amountView = (EditText) findViewById(R.id.expense_amount);
		currencySpinner = (Spinner) findViewById(R.id.expense_currency);
		payerView = (TextView) findViewById(R.id.expense_payer);
		dateView = (TextView) findViewById(R.id.expense_date);

		customSplitCheckBox = (CheckBox) findViewById(R.id.custom_split);
		customSplitTable = (TableLayout) findViewById(R.id.expense_split_table);

		Intent intent = getIntent();

		long calculationId = intent.getLongExtra(PARAM_CALCULATION_ID, -1);
		long expenseId = intent.getLongExtra(PARAM_EXPENSE_ID, -1);

		calculation = calculationDataSource.get(calculationId);
		persons = calculation.getPersons();

		expenseDataSource = new ExpenseDataSource(dbHelper, calculation);

		mode = (expenseId >= 0 ? Mode.EDIT_EXPENSE : Mode.NEW_EXPENSE);
		if (mode == Mode.EDIT_EXPENSE) {
			setTitle(R.string.edit_expense);
			expense = expenseDataSource.get(expenseId);
			titleView.setText(expense.getTitle());
		} else {
			setTitle(R.string.new_expense);
			expense = new Expense(calculation);
			long personId = intent.getLongExtra(PARAM_PERSON_ID, -1);
			expense.setPerson(calculation.getPersonById(personId));
			long millis = intent.getLongExtra(PARAM_DATE, -1);
			if (millis > 0) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(millis);
				expense.setDate(cal);
			}
		}

		List<java.util.Currency> currencies = new ArrayList<>();
		for (Currency currency : calculation.getCurrencies())
			currencies.add(java.util.Currency.getInstance(currency.getCurrencyCode()));
		CurrencySpinnerAdapter adapter = new CurrencySpinnerAdapter(this, currencies);
		adapter.setSymbolOnly(true);

		Currency currency = expense.getCurrency();
		currencyHelper = currency.getCurrencyHelper();
		currencySpinner.setAdapter(adapter);
		currencySpinner.setSelection(adapter.findItem(currency.getCurrencyCode()));
		currencySpinner.setEnabled(currencies.size() > 1);

		currencySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				updateCurrency();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		if (mode == Mode.EDIT_EXPENSE) {
			String formatted = currencyHelper.format(expense.getAmount(), false);
			amountView.setText(formatted);
		}

		Set<String> expenseTitles = new HashSet<>();
		for (Expense expense : calculation.getExpenses())
			expenseTitles.add(expense.getTitle());
		ArrayAdapter<String> expenseTitlesAdapter = new ArrayAdapter<>(this,
				android.R.layout.simple_dropdown_item_1line, expenseTitles.toArray(new String[expenseTitles.size()]));
		titleView.setAdapter(expenseTitlesAdapter);
		titleView.setThreshold(1);

		createCustomSplitRows();
		updateCustomSplit();
		updatePayer();
		updateDate();

		customSplitCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				customSplitTable.setVisibility(isChecked ? View.VISIBLE : View.GONE);
			}
		});

		amountView.addTextChangedListener(updateCustomSplitTextWatcher);

		payerView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pickPayer();
			}
		});

		dateView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pickDate();
			}
		});
	}

	private void createCustomSplitRows() {
		Map<Long, Double> weights = expense.getSplitWeights();
		customSplitEntries = new CustomSplitEntry[persons.size()];
		int dynamicId = 0;

		LayoutInflater inflater = getLayoutInflater();
		for (int i = 0; i < persons.size(); i++) {
			Person person = persons.get(i);
			boolean enabled = true;
			Double weight = 1.0;

			if (weights != null) {
				Double w = weights.get(person.getId());
				if (w != null)
					weight = w;
				else
					enabled = false;
			}

			TableRow row = (TableRow) inflater.inflate(R.layout.split_row, customSplitTable, false);
			customSplitTable.addView(row);

			final CustomSplitEntry customSplitEntry = new CustomSplitEntry();
			customSplitEntries[i] = customSplitEntry;

			customSplitEntry.enabled = (CheckBox) row.findViewById(R.id.split_enabled);
			customSplitEntry.enabled.setId(dynamicId++);
			customSplitEntry.enabled.setText(person.getName() + ":");
			customSplitEntry.enabled.setChecked(enabled);
			customSplitEntry.enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					customSplitEntry.weight.setEnabled(isChecked);
					updateCustomSplit();
				}
			});

			customSplitEntry.weight = (EditText) row.findViewById(R.id.split_weight);
			customSplitEntry.weight.setId(dynamicId++);
			customSplitEntry.weight.setEnabled(enabled);
			customSplitEntry.weight.setText(currencyHelper.format(weight, false));
			customSplitEntry.weight.addTextChangedListener(updateCustomSplitTextWatcher);

			customSplitEntry.result = (TextView) row.findViewById(R.id.split_share);
		}

		customSplitCheckBox.setChecked(weights != null);
		customSplitTable.setVisibility(weights != null ? View.VISIBLE : View.GONE);
	}

	private void updateCustomSplit() {
		try {
			for (CustomSplitEntry customSplitEntry : customSplitEntries)
				customSplitEntry.result.setText("");

			double[] weights = new double[customSplitEntries.length];
			double weightSum = 0;
			for (int i = 0; i < customSplitEntries.length; i++) {
				weights[i] = 0;
				if (customSplitEntries[i].enabled.isChecked())
					weights[i] = getWeight(i);
				weightSum += weights[i];
			}

			double amount = getAmount();
			for (int i = 0; i < customSplitEntries.length; i++) {
				if (customSplitEntries[i].enabled.isChecked() && weightSum > 0) {
					double share = (weights[i] / weightSum * amount);
					String formatted = currencyHelper.format(share);
					customSplitEntries[i].result.setText(formatted);
				}
			}
		} catch (Exception ignored) {}
	}

	private final TextWatcher updateCustomSplitTextWatcher = new TextWatcher() {
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void afterTextChanged(Editable s) {
			updateCustomSplit();
		}
	};

	private void updateCurrency() {
		java.util.Currency c = (java.util.Currency) currencySpinner.getSelectedItem();
		for (Currency currency : calculation.getCurrencies())
			if (currency.getCurrencyCode().equals(c.getCurrencyCode()))
				expense.setCurrency(currency);
		currencyHelper = expense.getCurrency().getCurrencyHelper();
		updateCustomSplit();
	}

	private void updateDate() {
		DateFormat format = DateFormat.getDateInstance();
		dateView.setText(format.format(expense.getDate().getTime()));
	}

	private void updatePayer() {
		payerView.setText(R.string.expense_payer_prompt);
		if (expense.getPerson() != null)
			payerView.setText(expense.getPerson().getName());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.expense_editor_options, menu);
        menu.findItem(R.id.menu_delete).setVisible(mode == Mode.EDIT_EXPENSE);
		return true;
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.menu_save:
    			doSave();
    			return true;
    		case R.id.menu_delete:
    			doDelete();
    			return true;
    		default:
    			return super.onOptionsItemSelected(item);
    	}
    }

	@Override
	protected void onPause() {
		super.onPause();
		dbHelper.close();
	}

	private void pickDate() {
		final DatePickerDialog.OnDateSetListener onDateSet = new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int month, int day) {
				Calendar date = Calendar.getInstance();
				date.clear();
				date.set(year, month, day);
				expense.setDate(date);
				updateDate();
			}
		};

		DialogFragment fragment = new DialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				Calendar date = expense.getDate();
				int year = date.get(Calendar.YEAR);
				int month = date.get(Calendar.MONTH);
				int day = date.get(Calendar.DAY_OF_MONTH);
				return new DatePickerDialog(getActivity(), onDateSet, year, month, day);
			}
		};
		fragment.show(getSupportFragmentManager(), "datePicker");
	}

	private void pickPayer() {
		DialogFragment fragment = new DialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				CharSequence[] personsArray = new CharSequence[persons.size()];
				int selected = -1;
				for (int i = 0; i < persons.size(); i++) {
					Person person = persons.get(i);
					personsArray[i] = person.getName();
					if (person.equals(expense.getPerson()))
						selected = i;
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(ExpenseEditorActivity.this);
				builder.setTitle(R.string.expense_payer_prompt);
				builder.setSingleChoiceItems(personsArray, selected, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int i) {
						Person payer = persons.get(i);
						expense.setPerson(payer);
						updatePayer();
						payerView.setError(null);
						dismiss();
					}
				});
				return builder.create();
			}
		};
		fragment.show(getSupportFragmentManager(), "personSelector");
	}

	private String getExpenseTitle() {
		return titleView.getText().toString().trim();
	}

	private double getAmount() throws ParseException {
		String amountString = amountView.getText().toString();
		return currencyHelper.parse(amountString);
	}

	private double getWeight(int i) throws ParseException {
		String amountString = customSplitEntries[i].weight.getText().toString();
		Number amountNumber = NumberFormat.getNumberInstance().parse(amountString);
		return amountNumber.doubleValue();
	}

	private boolean validate() {
		final Resources res = getResources(); 
		final String errRequired = res.getString(R.string.validate_required);
		final String errNumber = res.getString(R.string.validate_number);
		final String errSplit = res.getString(R.string.validate_select_split_persons);

		boolean valid = true;

		titleView.setError(null);
		amountView.setError(null);
		payerView.setError(null);
		customSplitCheckBox.setError(null);
		for (CustomSplitEntry customSplitEntry : customSplitEntries)
			customSplitEntry.weight.setError(null);

		if (getExpenseTitle().length() == 0) {
			titleView.setError(errRequired);
			valid = false;
		}

		try {
			getAmount();
		} catch (Exception e) {
			amountView.setError(errNumber);
			valid = false;
		}

		if (expense.getPerson() == null) {
			payerView.setError(errRequired);
			valid = false;
		}

		if (customSplitCheckBox.isChecked()) {
			int numEnabled = 0;

			for (int i = 0; i < customSplitEntries.length; i++) {
				if (customSplitEntries[i].enabled.isChecked()) {
					numEnabled++;
					try {
						double weight = getWeight(i);
						if (weight < 0.01)
							customSplitEntries[i].weight.setError(errNumber);
					} catch (Exception e) {
						customSplitEntries[i].weight.setError(errNumber);
						valid = false;
					}
				}
			}

			if (numEnabled == 0) {
				customSplitCheckBox.setError(errSplit);
				valid = false;
			}
		}

		return valid;
	}

	private void save() throws ParseException {
		expense.setTitle(getExpenseTitle());
		expense.setAmount(getAmount());

		Map<Long, Double> weights = null;
		if (customSplitCheckBox.isChecked()) {
			weights = new HashMap<>();
			for (int i = 0; i < customSplitEntries.length; i++)
				if (customSplitEntries[i].enabled.isChecked())
					weights.put(persons.get(i).getId(), getWeight(i));
		}
		expense.setSplitWeights(weights);

		if (mode == Mode.EDIT_EXPENSE)
			expenseDataSource.update(expense);
		else
			expenseDataSource.insert(expense);

		finish();
	}

	private void doSave() {
		try {
			if (validate()) save();
		} catch (Exception e) {
			Toast.makeText(this, "Error saving expense", Toast.LENGTH_LONG).show();
		}
	}

	private void doDelete() {
		expenseDataSource.delete(expense.getId());
		finish();
	}

}
