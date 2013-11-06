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

		Calculation calculation = new Calculation();
		calculation.setId(calculationId);
		calculation.setTitle(cursor.getString(1));
		calculation.setMainCurrencyCode(cursor.getString(2));

		CurrencyDataSource currencyDataSource = new CurrencyDataSource(dbHelper);
		Cursor currenciesCursor = currencyDataSource.listByCalculation(calculationId);
		List<Currency> currencies = currencyDataSource.getAllFromCursor(currenciesCursor);
		calculation.setCurrencies(currencies);

		PersonDataSource personDataSource = new PersonDataSource(dbHelper, calculation);
		Cursor personsCursor = personDataSource.listByCalculation();
		List<Person> persons = personDataSource.getAllFromCursor(personsCursor);
		calculation.setPersons(persons);

		ExpenseDataSource expenseDataSource = new ExpenseDataSource(dbHelper, calculation);
		List<Expense> expenses = new ArrayList<Expense>();
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

	public Calculation createCalculation(String title, String mainCurrency, List<String> personNames) {
		Calculation calculation = new Calculation();
		calculation.setTitle(title);
		calculation.setMainCurrencyCode(mainCurrency);
		insert(calculation);

		CurrencyDataSource currencyDataSource = new CurrencyDataSource(dbHelper);
		Currency ac = new Currency(calculation.getId(), mainCurrency);
		currencyDataSource.insert(ac);

		PersonDataSource personDataSource = new PersonDataSource(dbHelper, calculation);
		List<Person> persons = new ArrayList<Person>();
		for (String personName : personNames) {
			Person person = new Person(calculation);
			person.setName(personName);
			personDataSource.insert(person);
			persons.add(person);
		}

		return get(calculation.getId());
	}

}
