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
package ivl.android.moneybalance.data;

import java.util.Calendar;
import java.util.Currency;
import java.util.List;


public class Calculation extends DataObject {

	private String title;
	private Currency currency;
	private List<Person> persons;
	private List<Expense> expenses;

	private static final long MILLIS_PER_DAY = 24 * 3600 * 1000;

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public Currency getCurrency() {
		return currency;
	}
	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public List<Person> getPersons() {
		return persons;
	}
	public void setPersons(List<Person> persons) {
		this.persons = persons;
	}

	public List<Expense> getExpenses() {
		return expenses;
	}
	public void setExpenses(List<Expense> expenses) {
		this.expenses = expenses;
	}

	public long getExpenseTotal() {
		long total = 0;
		for (Expense expense : expenses)
			total += expense.getAmount();
		return total;
	}

	public Calendar getFirstDate() {
		Calendar date = null;
		for (Expense expense : expenses)
			if (date == null || expense.getDate().compareTo(date) < 0)
				date = expense.getDate();
		return date;
	}

	public Calendar getLastDate() {
		Calendar date = null;
		for (Expense expense : expenses)
			if (date == null || expense.getDate().compareTo(date) > 0)
				date = expense.getDate();
		return date;
	}

	public long getDuration() {
		Calendar first = getFirstDate();
		Calendar last = getLastDate();
		if (first == null || last == null)
			return 0;
		return (last.getTimeInMillis() - first.getTimeInMillis()) / MILLIS_PER_DAY + 1;
	}

	@Override
	public String toString() {
		return getTitle();
	}

}
