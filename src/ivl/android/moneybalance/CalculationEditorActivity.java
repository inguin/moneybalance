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
import ivl.android.moneybalance.data.Calculation;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public class CalculationEditorActivity extends ActionBarActivity {

	private static final int MIN_PERSONS = 2;
	private static final int MAX_PERSONS = 100;

	private EditText titleField;
	private Spinner currencyField;
	private LinearLayout personList;

	private class PersonView {
		public View view;
		public EditText nameField;
		public ImageView deleteButton;
	};
	private final List<PersonView> personViews = new ArrayList<PersonView>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR);

		setContentView(R.layout.calculation_editor);
		setTitle(R.string.new_calculation);

		titleField = (EditText) findViewById(R.id.calculation_title);
		currencyField = (Spinner) findViewById(R.id.calculation_currency);
		personList = (LinearLayout) findViewById(R.id.person_list);

		CurrencySpinnerAdapter adapter = new CurrencySpinnerAdapter(this);
		int selected = adapter.findItem(getDefaultCurrency());
		currencyField.setAdapter(adapter);
		currencyField.setSelection(selected);

		if (savedInstanceState != null) {
			for (String personName : savedInstanceState.getStringArrayList("personNames")) {
				PersonView view = addPersonRow();
				view.nameField.setText(personName);
			}
		}
		createOrDeletePersonRows();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList("personNames", getPersonNames());
	}

	private static Currency getDefaultCurrency() {
		Locale locale = Locale.getDefault();
		return Currency.getInstance(locale);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.calculation_editor_options, menu);
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

	private PersonView addPersonRow() {
		final View view = getLayoutInflater().inflate(R.layout.person_list_entry, personList, false);
		personList.addView(view);

		final PersonView personView = new PersonView();
		personView.view = view;
		personView.nameField = (EditText) view.findViewById(R.id.person_name);
		personView.deleteButton = (ImageView) view.findViewById(R.id.delete_button);
		personViews.add(personView);

		personView.nameField.setId(-1); // do not restore in onRestoreInstanceState()
		personView.nameField.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				createOrDeletePersonRows();
			}
		});

		personView.nameField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus)
					createOrDeletePersonRows();
			}
		});

		personView.deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deletePersonRow(personView);
				createOrDeletePersonRows();
			}
		});
		return personView;
	}

	private void deletePersonRow(PersonView row) {
		personList.removeView(row.view);
		personViews.remove(row);
	}

	private void createOrDeletePersonRows() {
		int numEmpty = 0;
		int i = 0;
		while (i < personViews.size() && personViews.size() >= 2) {
			PersonView personView = personViews.get(i++); 
			String name = personView.nameField.getText().toString().trim();
			if (name.length() == 0) {
				if (!personView.nameField.hasFocus()) {
					deletePersonRow(personView);
					i--;
				} else {
					numEmpty++;
				}
			}
		}

		while ((personViews.size() < MIN_PERSONS) || 
			   (numEmpty < 1 && personViews.size() < MAX_PERSONS))
		{
			addPersonRow();
			numEmpty++;
		}

		for (i = 0; i < personViews.size(); i++) {
			PersonView personView = personViews.get(i);
			boolean last = (i + 1 == personViews.size());
			personView.deleteButton.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
		}
	}

	private String getCalculationTitle() {
		return titleField.getText().toString().trim();
	}

	private String getPersonName(int i) {
		PersonView personView = personViews.get(i);
		return personView.nameField.getText().toString().trim();
	}

	private ArrayList<String> getPersonNames() {
		ArrayList<String> personNames = new ArrayList<String>();
		for (int i = 0; i < personViews.size(); i++) {
			String name = getPersonName(i);
			if (name.length() > 0)
				personNames.add(name);
		}
		return personNames;
	}

	private boolean validate() {
		final Resources res = getResources(); 

		boolean valid = true;
		titleField.setError(null);
		for (PersonView personView : personViews)
			personView.nameField.setError(null);

		if (getCalculationTitle().length() == 0) {
			final String errRequired = res.getString(R.string.validate_required);
			titleField.setError(errRequired);
			valid = false;
		}

		Set<String> personNames = new HashSet<String>();
		for (int i = 0; i < personViews.size(); i++) {
			String name = getPersonName(i);
			if (name.length() > 0) {
				if (personNames.contains(name)) {
					PersonView personView = personViews.get(i);
					personView.nameField.setError(res.getString(R.string.validate_duplicate_name));
					valid = false;
				} else {
					personNames.add(name);
				}
			}
		}

		if (valid && personNames.size() < MIN_PERSONS) {
			PersonView personView = personViews.get(personViews.size() - 1);
			String format = res.getString(R.string.validate_min_names);
			personView.nameField.setError(String.format(format, MIN_PERSONS));
			valid = false;
		}

		return valid;
	}

	private void save() {
		String title = getCalculationTitle();
		Currency currency = (Currency) currencyField.getSelectedItem();
		List<String> personNames = getPersonNames();

		DataBaseHelper dbHelper = new DataBaseHelper(this);
		CalculationDataSource dataSource = new CalculationDataSource(dbHelper);
		Calculation calculation = dataSource.createCalculation(title, currency.getCurrencyCode(), personNames);
		dbHelper.close();

		Intent intent = new Intent(this, ExpenseListActivity.class);
		intent.putExtra(ExpenseListActivity.PARAM_CALCULATION_ID, calculation.getId());
		startActivity(intent);
		finish();
	}

	private void doSave() {
		try {
			if (validate()) save();
		} catch (Exception e) {
			Toast.makeText(this, "Error saving calculation", Toast.LENGTH_LONG).show();
			Log.e(CalculationEditorActivity.class.toString(), "Error saving calculation", e);
		}
	}

}
