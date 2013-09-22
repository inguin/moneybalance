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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

public class CurrencyHelper {

	private final NumberFormat currencyFormat;
	private final NumberFormat plainFormat;
	private long divider;

	public CurrencyHelper(Currency currency, Locale locale) {
		currencyFormat = NumberFormat.getCurrencyInstance(locale);
		plainFormat = NumberFormat.getNumberInstance(locale);

		currencyFormat.setCurrency(currency);
		currencyFormat.setMinimumFractionDigits(currency.getDefaultFractionDigits());
		currencyFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());

		plainFormat.setMinimumFractionDigits(currency.getDefaultFractionDigits());
		plainFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());

		divider = Math.round(Math.pow(10, currency.getDefaultFractionDigits()));
	}

	public CurrencyHelper(Currency currency) {
		this(currency, Locale.getDefault());
	}

	public void setGroupingUsed(boolean value) {
		currencyFormat.setGroupingUsed(value);
		plainFormat.setGroupingUsed(value);
	}

	public String format(double value, boolean withSymbol) {
		NumberFormat format = withSymbol ? currencyFormat : plainFormat;
		return format.format(value);	
	}

	public String format(double value) {
		return format(value, true);
	}

	public String formatCents(long value, boolean withSymbol) {
		return format((double)value / divider, withSymbol);		
	}

	public String formatCents(long value) {
		return formatCents(value, true);		
	}

	public long parseAsCents(String amountString) throws ParseException {
		Number amount = plainFormat.parse(amountString);
		return Math.round(amount.floatValue() * divider);
	}

}
