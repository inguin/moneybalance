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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ExpenseListActivity extends ActionBarActivity implements OnChildClickListener {

	public static final String PARAM_CALCULATION_ID = "calculationId";

	private long calculationId;

	private final DataBaseHelper dbHelper = new DataBaseHelper(this);
	private final CalculationDataSource calculationDataSource = new CalculationDataSource(dbHelper);
	private ExpenseDataSource expenseDataSource;

	private static final int ITEM_DELETE = 0;

	private ExpenseAdapter adapter;

	private class ExpenseAdapter extends BaseExpandableListAdapter {

		private boolean groupByPerson = true;

		private Calculation calculation;

		private final Map<Person, List<Expense>> expensesByPerson = new HashMap<Person, List<Expense>>();

		private final Set<Calendar> dates = new TreeSet<Calendar>();
		private final Map<Calendar, List<Expense>> expensesByDate = new HashMap<Calendar, List<Expense>>();

		private final LayoutInflater inflater;
		private final String groupSummaryFormat = getResources().getString(R.string.expenses_summary_format);

		public ExpenseAdapter(Context context) {
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public void setCalculation(Calculation calculation) {
			this.calculation = calculation;
			expenseDataSource = new ExpenseDataSource(dbHelper, calculation);

			expensesByPerson.clear();
			dates.clear();
			expensesByDate.clear();

			for (Person person : calculation.getPersons())
				expensesByPerson.put(person, new ArrayList<Expense>());

			for (Expense expense : calculation.getExpenses()) {
				List<Expense> byPersonList = expensesByPerson.get(expense.getPerson());
				byPersonList.add(expense);

				Calendar date = expense.getDate();
				List<Expense> byDateList = expensesByDate.get(date);
				if (byDateList == null) {
					byDateList = new ArrayList<Expense>();
					expensesByDate.put(date, byDateList);
					dates.add(date);
				}
				byDateList.add(expense);
			}

			notifyDataSetChanged();
		}

		public void setGroupByPerson(boolean groupByPerson) {
			this.groupByPerson = groupByPerson;
			notifyDataSetChanged();
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public int getGroupCount() {
			if (calculation == null)
				return 0;
			else if (groupByPerson)
				return calculation.getPersons().size();
			else
				return dates != null ? dates.size() : 0;
		}

		@Override
		public long getGroupId(int groupPosition) {
			if (groupByPerson) {
				Person person = (Person) getGroup(groupPosition);
				return person.getId();
			} else {
				Calendar date = (Calendar) getGroup(groupPosition);
				return date.getTimeInMillis();
			}
		}

		@Override
		public Object getGroup(int groupPosition) {
			if (groupByPerson) {
				return calculation.getPersons().get(groupPosition);
			} else {
				return dates.toArray()[groupPosition];
			}
		}

		private class GroupViewHolder {
			public TextView nameView;
			public TextView summaryView;
			public ImageView addButton;
		}

		@Override
		public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View view = convertView;
			GroupViewHolder holder;

			if (view == null) {
				view = inflater.inflate(R.layout.expense_list_group_row, parent, false);
				holder = new GroupViewHolder();
				holder.nameView = (TextView) view.findViewById(android.R.id.text1);
				holder.summaryView = (TextView) view.findViewById(android.R.id.text2);
				holder.addButton = (ImageView) view.findViewById(R.id.add_button);
				view.setTag(holder);
			} else {
				holder = (GroupViewHolder) view.getTag();
			}

			List<Expense> expenses;

			if (groupByPerson) {
				Person person = (Person) getGroup(groupPosition);
				holder.nameView.setText(person.getName());
				expenses = expensesByPerson.get(person);
			} else {
				Calendar date = (Calendar) getGroup(groupPosition);
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				holder.nameView.setText(format.format(date.getTime()));
				expenses = expensesByDate.get(date);
			}

			int count = 0;
			double total = 0;
			for (Expense expense : expenses) {
				count++;
				total += expense.getExchangedAmount();
			}

			if (count == 0) {
				holder.summaryView.setText(R.string.no_expenses);
			} else {
				String totalStr = calculation.getMainCurrency().getCurrencyHelper().format(total);
				String summary = String.format(groupSummaryFormat, count, totalStr);
				holder.summaryView.setText(summary);
			}

			holder.addButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (groupByPerson) {
						Person person = (Person) getGroup(groupPosition);
						addExpenseForPerson(person.getId());
					} else {
						Calendar date = (Calendar) getGroup(groupPosition);
						addExpenseForDate(date);
					}
				}
			});

			return view;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (groupByPerson) {
				Person person = (Person) getGroup(groupPosition);
				List<Expense> list = expensesByPerson.get(person);
				return list.size();
			} else {
				Calendar date = (Calendar) getGroup(groupPosition);
				List<Expense> list = expensesByDate.get(date);
				return list.size();
			}
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			Expense expense = (Expense) getChild(groupPosition, childPosition);
			return expense.getId();
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			if (groupByPerson) {
				Person person = (Person) getGroup(groupPosition);
				List<Expense> list = expensesByPerson.get(person);
				return list.get(childPosition);
			} else {
				Calendar date = (Calendar) getGroup(groupPosition);
				List<Expense> list = expensesByDate.get(date);
				return list.get(childPosition);
			}
		}

		private class ChildViewHolder {
			public TextView titleView;
			public TextView amountView;
			public TextView exchangedView;
			public TextView details1View;
			public TextView details2View;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View view = convertView;
			ChildViewHolder holder;

			if (view == null) {
				view = inflater.inflate(R.layout.expense_row, parent, false);
				holder = new ChildViewHolder();
				holder.titleView = (TextView) view.findViewById(R.id.expense_title);
				holder.amountView = (TextView) view.findViewById(R.id.expense_amount);
				holder.exchangedView = (TextView) view.findViewById(R.id.expense_exchanged_amount);
				holder.details1View = (TextView) view.findViewById(R.id.expense_details_1);
				holder.details2View = (TextView) view.findViewById(R.id.expense_details_2);
				view.setTag(holder);
			} else {
				holder = (ChildViewHolder) view.getTag();
			}

			Expense expense = (Expense) getChild(groupPosition, childPosition);
			Currency currency = expense.getCurrency();
			CurrencyHelper currencyHelper = currency.getCurrencyHelper();

			holder.titleView.setText(expense.getTitle());
			holder.amountView.setText(currencyHelper.format(expense.getAmount()));
			if (expense.getCurrency().equals(calculation.getMainCurrency())) {
				holder.exchangedView.setVisibility(View.GONE);
			} else {
				CurrencyHelper mainCurrencyHelper = calculation.getMainCurrency().getCurrencyHelper();
				double exchanged = expense.getExchangedAmount();
				holder.exchangedView.setVisibility(View.VISIBLE);
				holder.exchangedView.setText(mainCurrencyHelper.format(exchanged));
			}

			if (groupByPerson) {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				holder.details1View.setText(format.format(expense.getDate().getTime()));
			} else {
				Person person = expense.getPerson();
				holder.details1View.setText(person.getName());
			}

			if (expense.isUnevenSplit()) {
				List<Person> persons = calculation.getPersons();
				List<Double> shares = expense.getShares(persons);
				StringBuilder msg = new StringBuilder();
				for (int i = 0; i < persons.size(); i++) {
					if (shares.get(i) > 0) {
						Person person = persons.get(i);
						if (msg.length() > 0)
							msg.append("; ");
						String shareStr = currencyHelper.format(shares.get(i));
						msg.append(String.format("%s: %s", person.getName(), shareStr));
					}
				}

				holder.details2View.setVisibility(View.VISIBLE);
				holder.details2View.setText(msg);
			} else {
				holder.details2View.setVisibility(View.GONE);
			}
			return view;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR);

		setContentView(R.layout.expense_list);

		Intent intent = getIntent();
		calculationId = intent.getLongExtra(PARAM_CALCULATION_ID, -1);

		ExpandableListView listView = (ExpandableListView) findViewById(R.id.expense_list);
		adapter = new ExpenseAdapter(this);
		listView.setAdapter(adapter);
		listView.setOnChildClickListener(this);
		registerForContextMenu(listView);
		setContentView(listView);

		refresh();
	}

	private void refresh() {
		Calculation calculation = calculationDataSource.get(calculationId);
		setTitle(calculation.getTitle());
		adapter.setCalculation(calculation);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.expense_list_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.calcluation_summary:
				Intent intent = new Intent(this, SummaryActivity.class);
				intent.putExtra(SummaryActivity.PARAM_CALCULATION_ID, calculationId);
				startActivity(intent);
				return true;
			case R.id.export_calculation:
				CsvExporter.export(calculationDataSource.get(calculationId), this);
				return true;
			case R.id.group_by_person:
				adapter.setGroupByPerson(true);
				return true;
			case R.id.group_by_date:
				adapter.setGroupByPerson(false);
				return true;
			case R.id.new_expense:
				addExpense();
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Expense expense = (Expense) adapter.getChild(groupPosition, childPosition);
		Intent intent = new Intent(this, ExpenseEditorActivity.class);
		intent.putExtra(ExpenseEditorActivity.PARAM_CALCULATION_ID, expense.getCalculation().getId());
		intent.putExtra(ExpenseEditorActivity.PARAM_EXPENSE_ID, expense.getId());
		startActivity(intent);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.expense_list) {
			ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
			int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
			if (child != -1) {
				Expense expense = (Expense) adapter.getChild(group, child);
				menu.setHeaderTitle(expense.getTitle());
				menu.add(0, ITEM_DELETE, 0, R.string.menu_delete);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == ITEM_DELETE) {
			ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
			int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
			if (child != -1) {
				Expense expense = (Expense) adapter.getChild(group, child);
				expenseDataSource.delete(expense.getId());
				refresh();
			}
		} else {
			return false;
		}
		return true;
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

	private void addExpenseForPerson(long personId) {
		Intent intent = new Intent(this, ExpenseEditorActivity.class);
		intent.putExtra(ExpenseEditorActivity.PARAM_CALCULATION_ID, calculationId);
		intent.putExtra(ExpenseEditorActivity.PARAM_PERSON_ID, personId);
		startActivity(intent);
	}

	private void addExpenseForDate(Calendar date) {
		Intent intent = new Intent(this, ExpenseEditorActivity.class);
		intent.putExtra(ExpenseEditorActivity.PARAM_CALCULATION_ID, calculationId);
		intent.putExtra(ExpenseEditorActivity.PARAM_DATE, date.getTimeInMillis());
		startActivity(intent);
	}

	private void addExpense() {
		Intent intent = new Intent(this, ExpenseEditorActivity.class);
		intent.putExtra(ExpenseEditorActivity.PARAM_CALCULATION_ID, calculationId);
		startActivity(intent);
	}

}
