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

import ivl.android.moneybalance.data.Person;

import android.content.ContentValues;
import android.database.Cursor;

public class PersonDataSource extends AbstractDataSource<Person> {

	private static final String[] COLUMNS = {
		DataBaseHelper.COLUMN_ID,
		DataBaseHelper.COLUMN_CALCULATION_ID,
		DataBaseHelper.COLUMN_NAME
	};

	public PersonDataSource(DataBaseHelper dbHelper) {
		super(dbHelper, DataBaseHelper.TABLE_PERSONS, COLUMNS);
	}

	@Override
	protected ContentValues toContentValues(Person person) {
		ContentValues values = new ContentValues();
		values.put(DataBaseHelper.COLUMN_CALCULATION_ID, person.getCalculationId());
		values.put(DataBaseHelper.COLUMN_NAME, person.getName());
		return values;
	}

	@Override
	public Person fromCursor(Cursor cursor) {
		Person person = new Person();
		person.setId(cursor.getLong(0));
		person.setCalculationId(cursor.getLong(1));
		person.setName(cursor.getString(2));
		return person;
	}

	public Cursor listByCalculation(long calculationId) {
		return getDatabase().query(
				DataBaseHelper.TABLE_PERSONS, COLUMNS,
				DataBaseHelper.COLUMN_CALCULATION_ID + " = ?", new String[] { Long.toString(calculationId) },
				null, null, null);
	}

}
