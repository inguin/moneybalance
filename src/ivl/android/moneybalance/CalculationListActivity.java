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
import ivl.android.moneybalance.data.Expense;
import ivl.android.moneybalance.data.Person;

import java.text.SimpleDateFormat;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class CalculationListActivity extends ActionBarActivity implements OnItemClickListener {

	private final DataBaseHelper dbHelper = new DataBaseHelper(this);
	private final CalculationDataSource dataSource = new CalculationDataSource(dbHelper);
	private Cursor cursor;

	private ListView listView;
	private CalculationAdapter adapter;

	private static final int ITEM_DELETE = 0;
	private static final int ITEM_SUMMARY = 1;
	
	private class CalculationAdapter extends CursorAdapter {

		private final String summaryFormat = getResources().getString(R.string.expenses_summary_format);
		private final String dateRangeFormat = getResources().getString(R.string.date_range_format);

		CalculationAdapter(Context context) {
			super(context, null, 0);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			// TODO: Use view holder
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			return inflater.inflate(R.layout.calculation_list_row, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Calculation calculation = dataSource.fromCursor(cursor);

			StringBuilder personNames = new StringBuilder();
			for (Person person : calculation.getPersons()) {
				if (personNames.length() > 0) {
					personNames.append(", ");
				}
				personNames.append(person.getName());
			}

			TextView titleView = (TextView) view.findViewById(R.id.calculation_title);
			titleView.setText(calculation.getTitle());
			TextView personsView = (TextView) view.findViewById(R.id.calculation_persons);
			personsView.setText(personNames);

			TextView datesView = (TextView) view.findViewById(R.id.calculation_dates);
			TextView summaryView = (TextView) view.findViewById(R.id.calculation_summary);

			List<Expense> expenses = calculation.getExpenses();
			if (expenses.size() == 0) {
				datesView.setVisibility(View.GONE);
				summaryView.setText(R.string.no_expenses);
			} else {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				String firstDate = format.format(calculation.getFirstDate().getTime());
				String lastDate = format.format(calculation.getLastDate().getTime());
				datesView.setText(String.format(dateRangeFormat, firstDate, lastDate));
				datesView.setVisibility(View.VISIBLE);

				int count = calculation.getExpenses().size();
				CurrencyHelper helper = new CurrencyHelper(calculation.getCurrency());
				String total = helper.formatCents(calculation.getExpenseTotal());
				summaryView.setText(String.format(summaryFormat, count, total));
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR);

		setContentView(R.layout.calculation_list);

		listView = (ListView) findViewById(R.id.calculation_list);
		adapter = new CalculationAdapter(this);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		registerForContextMenu(listView);
		setContentView(listView);

		refresh();
	}

	private void refresh() {
		cursor = dataSource.listAll();
		adapter.changeCursor(cursor);
		listView.setAdapter(adapter);
	}

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		cursor.moveToPosition(position);
		Calculation calculation = dataSource.fromCursor(cursor);
		Intent intent = new Intent(this, ExpenseListActivity.class);
		intent.putExtra(ExpenseListActivity.PARAM_CALCULATION_ID, calculation.getId());
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.calculation_list_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.new_calculation:
				startActivity(new Intent(this, CalculationEditorActivity.class));
				return true;
			case R.id.about:
				showAboutDialog();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.calculation_list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			cursor.moveToPosition(info.position);
			Calculation calculation = dataSource.fromCursor(cursor);
			menu.setHeaderTitle(calculation.getTitle());
			menu.add(0, ITEM_DELETE, 0, R.string.menu_delete);
			menu.add(0, ITEM_SUMMARY, 0, R.string.calculation_summary);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		cursor.moveToPosition(info.position);
		long calculationId = cursor.getLong(0);

		if (item.getItemId() == ITEM_DELETE) {
			confirmAndDelete(dataSource.get(calculationId));
			return true;
		} else if (item.getItemId() == ITEM_SUMMARY) {
			Intent intent = new Intent(this, SummaryActivity.class);
			intent.putExtra(ExpenseListActivity.PARAM_CALCULATION_ID, calculationId);
			startActivity(intent);
		} else {
			return false;
		}
		return true;
	}

	private void confirmAndDelete(final Calculation calculation) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(android.R.drawable.ic_delete);
		dialog.setTitle(calculation.getTitle());
		dialog.setMessage(R.string.confirm_delete_calculation);
		dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dataSource.delete(calculation.getId());
				refresh();
			}
		});
		dialog.setNegativeButton(android.R.string.no, null);
		dialog.show();
	}

	private void showAboutDialog() {
		AboutDialog about = new AboutDialog(this);
		about.show();
	}

	@Override
	protected void onPause() {
		super.onPause();
		dbHelper.close();
	}

	@Override
	protected void onResume() {
		refresh();
		super.onResume();
	}

}
