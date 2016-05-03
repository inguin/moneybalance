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
package ivl.android.moneybalance.dao;

import ivl.android.moneybalance.data.Currency;
import ivl.android.moneybalance.data.Calculation;
import ivl.android.moneybalance.data.Expense;
import ivl.android.moneybalance.data.Person;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

public class CalculationDataSource extends AbstractDataSource<Calculation> {

	private final DataBaseHelper dbHelper;

	private static final String[] COLUMNS = {
		DataBaseHelper.COLUMN_ID,
		DataBaseHelper.COLUMN_TITLE,
		DataBaseHelper.COLUMN_CURRENCY
	};

	public CalculationDataSource(DataBaseHelper dbHelper) {
		super(dbHelper, DataBaseHelper.TABLE_CALCULATIONS, COLUMNS);
		this.dbHelper = dbHelper;
	}

	@Override
	protected ContentValues toContentValues(Calculation calculation) {
		ContentValues values = new ContentValues();
		values.put(DataBaseHelper.COLUMN_TITLE, calculation.getTitle());
		values.put(DataBaseHelper.COLUMN_CURRENCY, calculation.getMainCurrencyCode());
		return values;
	}

	@Override
	public Calculation fromCursor(Cursor cursor) {
		long calculationId = cursor.getLong(0);
		String title = cursor.getString(1);
		String mainCurrencyCode = cursor.getString(2);

		Calculation calculation = new Calculation(title, mainCurrencyCode);
		calculation.setId(calculationId);

		CurrencyDataSource currencyDataSource = new CurrencyDataSource(dbHelper);
		Cursor currenciesCursor = currencyDataSource.listByCalculation(calculationId);
		List<Currency> currencies = currencyDataSource.getAllFromCursor(currenciesCursor);
		calculation.setCurrencies(currencies);

		PersonDataSource personDataSource = new PersonDataSource(dbHelper, calculation);
		Cursor personsCursor = personDataSource.listByCalculation();
		List<Person> persons = personDataSource.getAllFromCursor(personsCursor);
		calculation.setPersons(persons);

		ExpenseDataSource expenseDataSource = new ExpenseDataSource(dbHelper, calculation);
		List<Expense> expenses = new ArrayList<>();
		calculation.setExpenses(expenses);

		for (Person person : persons) {
			Cursor expensesCursor = expenseDataSource.listByPerson(person.getId());
			List<Expense> personExpenses = expenseDataSource.getAllFromCursor(expensesCursor); 
			expenses.addAll(personExpenses);
		}

		return calculation;
	}

	@Override
	public void delete(long id) {
		Calculation calculation = get(id);

		ExpenseDataSource expenseDataSource = new ExpenseDataSource(dbHelper, calculation);
		for (Expense expense : calculation.getExpenses())
			expenseDataSource.delete(expense.getId());

		PersonDataSource personDataSource = new PersonDataSource(dbHelper, calculation);
		for (Person person : calculation.getPersons())
			personDataSource.delete(person.getId());

		CurrencyDataSource currencyDataSource = new CurrencyDataSource(dbHelper);
		for (Currency currency : calculation.getCurrencies())
			currencyDataSource.delete(currency.getId());

		super.delete(id);
	}

	@Override
	public void update(Calculation calculation) {
		super.update(calculation);

		CurrencyDataSource currencyDataSource = new CurrencyDataSource(dbHelper);
		Cursor currenciesCursor = currencyDataSource.listByCalculation(calculation.getId());
		List<Currency> oldCurrencies = currencyDataSource.getAllFromCursor(currenciesCursor);

		for (Currency oldCurrency : oldCurrencies) {
			Currency newCurrency = null;
			for (Currency c : calculation.getCurrencies())
				if (oldCurrency.getCurrencyCode().equals(c.getCurrencyCode()))
					newCurrency = c;
			if (newCurrency != null)
				currencyDataSource.update(newCurrency);
			else
				currencyDataSource.delete(oldCurrency.getId());
		}

		for (Currency newCurrency : calculation.getCurrencies()) {
			boolean found = false;
			for (Currency oldCurrency : oldCurrencies)
				if (oldCurrency.getCurrencyCode().equals(newCurrency.getCurrencyCode()))
					found = true;
			if (!found)
				currencyDataSource.insert(newCurrency);
		}
	}

	public Calculation createCalculation(String title, String mainCurrencyCode, List<String> personNames) {
		Calculation calculation = new Calculation(title, mainCurrencyCode);
		insert(calculation);

		CurrencyDataSource currencyDataSource = new CurrencyDataSource(dbHelper);
		Currency mainCurrency = new Currency(calculation.getId());
		mainCurrency.setCurrencyCode(mainCurrencyCode);
		currencyDataSource.insert(mainCurrency);

		PersonDataSource personDataSource = new PersonDataSource(dbHelper, calculation);
		List<Person> persons = new ArrayList<>();
		for (String personName : personNames) {
			Person person = new Person(calculation);
			person.setName(personName);
			personDataSource.insert(person);
			persons.add(person);
		}

		return get(calculation.getId());
	}

}
