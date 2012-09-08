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

import ivl.android.moneybalance.data.DataObject;

import java.util.ArrayList;
import java.util.List;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class AbstractDataSource<T extends DataObject> {

	private final DataBaseHelper dbHelper;
	private final String table;
	private final String[] columns;

	public AbstractDataSource(DataBaseHelper dbHelper, String table, String[] columns) {
		this.dbHelper = dbHelper; 
		this.table = table;
		this.columns = columns;
	}

	protected SQLiteDatabase getDatabase() {
		return dbHelper.getWritableDatabase();
	}

	public long insert(T object) {
		long insertId = getDatabase().insert(
				table,
				null,
				toContentValues(object));
		object.setId(insertId);
		return insertId;
	}

	public void update(T object) {
		long id = object.getId();
		getDatabase().update(
				table,
				toContentValues(object),
				DataBaseHelper.COLUMN_ID + " = ?", new String[] { Long.toString(id) });
	}

	public void delete(long id) {
		getDatabase().delete(
				table,
				DataBaseHelper.COLUMN_ID + " = ?", new String[] { Long.toString(id) });		
	}

	public T get(long id) {
		Cursor cursor = getDatabase().query(
				table, columns,
				DataBaseHelper.COLUMN_ID + " = ?", new String[] { Long.toString(id) },
				null, null, null, null);
	
		cursor.moveToFirst();
		T object = fromCursor(cursor);
		cursor.close();
	
		return object;
	}

	public Cursor listAll() {
		return getDatabase().query(
				table, columns,
				null, null, null, null, null);
	}

	public List<T> getAllFromCursor(Cursor cursor) {
		List<T> objects = new ArrayList<T>();

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			T object = fromCursor(cursor);
			objects.add(object);
			cursor.moveToNext();
		}
		cursor.close();

		return objects;
		
	}

	protected abstract ContentValues toContentValues(T object);

	protected abstract T fromCursor(Cursor cursor);

}
