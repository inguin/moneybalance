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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "moneybalance.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_CALCULATIONS = "calculations";
	public static final String TABLE_PERSONS = "persons";
	public static final String TABLE_EXPENSES = "expenses";
	public static final String TABLE_SPLIT_WEIGHTS = "split_weights";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_CALCULATION_ID = "calculation_id";
	public static final String COLUMN_PERSON_ID = "person_id";
	public static final String COLUMN_EXPENSE_ID = "expense_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_CURRENCY = "currency";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_AMOUNT = "amount";
	public static final String COLUMN_DATE = "date";
	public static final String COLUMN_WEIGHT = "weight";

	public DataBaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql;

		sql = "CREATE TABLE " + TABLE_CALCULATIONS + "(" +
				COLUMN_ID + " integer primary key autoincrement, " +
				COLUMN_TITLE + " text not null," +
				COLUMN_CURRENCY + " text not null)"; 
		db.execSQL(sql);

		sql = "CREATE TABLE " + TABLE_PERSONS + "(" +
				COLUMN_ID + " integer primary key autoincrement, " +
				COLUMN_CALCULATION_ID + " integer not null, " + 
				COLUMN_NAME + " text not null)";
		db.execSQL(sql);

		sql = "CREATE TABLE " + TABLE_EXPENSES + "(" +
				COLUMN_ID + " integer primary key autoincrement, " +
				COLUMN_PERSON_ID + " integer not null, " +
				COLUMN_TITLE + " text not null, " + 
				COLUMN_AMOUNT + " integer not null, " +
				COLUMN_DATE + " integer not null)";
		db.execSQL(sql);

		sql = "CREATE TABLE " + TABLE_SPLIT_WEIGHTS + "(" +
				COLUMN_EXPENSE_ID + " integer not null, " +
				COLUMN_PERSON_ID + " integer not null, " +
				COLUMN_WEIGHT + " real not null)";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DataBaseHelper.class.getName(), String.format("Upgrading from %d to %d", oldVersion, newVersion));
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SPLIT_WEIGHTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERSONS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALCULATIONS);
		onCreate(db);
	}

}
