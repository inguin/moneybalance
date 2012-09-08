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

import ivl.android.moneybalance.data.Calculation;
import ivl.android.moneybalance.data.Expense;
import ivl.android.moneybalance.data.Person;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;


import android.content.ContentValues;
import android.database.Cursor;

public class CalculationDataSource extends AbstractDataSource<Calculation> {

	private final PersonDataSource personDataSource;
	private final ExpenseDataSource expenseDataSource;

	private static final String[] COLUMNS = {
		DataBaseHelper.COLUMN_ID,
		DataBaseHelper.COLUMN_TITLE,
		DataBaseHelper.COLUMN_CURRENCY
	};

	public CalculationDataSource(DataBaseHelper dbHelper) {
		super(dbHelper, DataBaseHelper.TABLE_CALCULATIONS, COLUMNS);
		personDataSource = new PersonDataSource(dbHelper);
		expenseDataSource = new ExpenseDataSource(dbHelper);
	}

	@Override
	protected ContentValues toContentValues(Calculation calculation) {
		ContentValues values = new ContentValues();
		values.put(DataBaseHelper.COLUMN_TITLE, calculation.getTitle());
		values.put(DataBaseHelper.COLUMN_CURRENCY, calculation.getCurrency().getCurrencyCode());
		return values;
	}

	@Override
	public Calculation fromCursor(Cursor cursor) {
		long calculationId = cursor.getLong(0);
		String title = cursor.getString(1);
		String currency = cursor.getString(2);
		Cursor personsCursor = personDataSource.listByCalculation(calculationId);
		List<Person> persons = personDataSource.getAllFromCursor(personsCursor);
		List<Expense> expenses = new ArrayList<Expense>();

		for (Person person : persons) {
			Cursor expensesCursor = expenseDataSource.listByPerson(person.getId());
			List<Expense> personExpenses = expenseDataSource.getAllFromCursor(expensesCursor); 
			expenses.addAll(personExpenses);
		}

		Calculation calculation = new Calculation();
		calculation.setId(calculationId);
		calculation.setTitle(title);
		calculation.setCurrency(Currency.getInstance(currency));
		calculation.setPersons(persons);
		calculation.setExpenses(expenses);
		return calculation;		
	}

	@Override
	public void delete(long id) {
		Calculation calculation = get(id);
		
		for (Expense expense : calculation.getExpenses())
			expenseDataSource.delete(expense.getId());

		for (Person person : calculation.getPersons())
			personDataSource.delete(person.getId());

		super.delete(id);
	}

	public Calculation createCalculation(String title, Currency currency, List<String> personNames) {
		Calculation calculation = new Calculation();
		calculation.setTitle(title);
		calculation.setCurrency(currency);
		insert(calculation);

		List<Person> persons = new ArrayList<Person>();
		for (String personName : personNames) {
			Person person = new Person();
			person.setCalculationId(calculation.getId());
			person.setName(personName);
			personDataSource.insert(person);
			persons.add(person);
		}

		return get(calculation.getId());
	}

}
