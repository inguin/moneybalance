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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Calculation extends DataObject {

	private String title;
	private String mainCurrencyCode;
	private List<Currency> currencies = new ArrayList<Currency>();
	private List<Person> persons = new ArrayList<Person>();
	private List<Expense> expenses = new ArrayList<Expense>();

	private static final long MILLIS_PER_DAY = 24 * 3600 * 1000;

	public Calculation(String title, String mainCurrencyCode) {
		this.title = title;
		setMainCurrencyCode(mainCurrencyCode);
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public String getMainCurrencyCode() {
		return mainCurrencyCode;
	}
	public void setMainCurrencyCode(String mainCurrencyCode) {
		Currency currency = null;
		for (Currency c : currencies)
			if (c.getCurrencyCode().equals(mainCurrencyCode))
				currency = c;
		if (currency != null) {
			currency.setExchangeRate(1, 1);
		} else {
			currency = new Currency(getId());
			currency.setCurrencyCode(mainCurrencyCode);
			currencies.add(currency);
		}
		this.mainCurrencyCode = mainCurrencyCode;
	}
	public Currency getMainCurrency() {
		Currency currency = currencies.get(0);
		for (Currency c : currencies)
			if (c.getCurrencyCode().equals(mainCurrencyCode))
				currency = c;
		return currency;
	}

	public List<Currency> getCurrencies() {
		return currencies;
	}
	public void setCurrencies(List<Currency> currencies) {
		this.currencies = currencies;
	}
	public Currency getCurrencyById(long currencyId) {
		for (Currency currency : currencies)
			if (currency.getId() == currencyId)
				return currency;
		return null;
	}

	public List<Person> getPersons() {
		return persons;
	}
	public void setPersons(List<Person> persons) {
		this.persons = persons;
	}
	public Person getPersonById(long personId) {
		for (Person person : persons)
			if (person.getId() == personId)
				return person;
		return null;
	}

	public List<Expense> getExpenses() {
		return expenses;
	}
	public void setExpenses(List<Expense> expenses) {
		this.expenses = expenses;
	}

	public double getExpenseTotal() {
		double total = 0;
		for (Expense expense : expenses) {
			Currency currency = expense.getCurrency();
			total += currency.exchangeAmount(expense.getAmount());
		}
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
