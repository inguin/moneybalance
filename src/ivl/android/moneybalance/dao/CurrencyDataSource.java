package ivl.android.moneybalance.dao;

import ivl.android.moneybalance.data.Currency;
import android.content.ContentValues;
import android.database.Cursor;

public class CurrencyDataSource extends AbstractDataSource<Currency> {

	private static final String[] COLUMNS = {
		DataBaseHelper.COLUMN_ID,
		DataBaseHelper.COLUMN_CALCULATION_ID,
		DataBaseHelper.COLUMN_CURRENCY_CODE,
		DataBaseHelper.COLUMN_RATE_THIS,
		DataBaseHelper.COLUMN_RATE_MAIN
	};

	public CurrencyDataSource(DataBaseHelper dbHelper) {
		super(dbHelper, DataBaseHelper.TABLE_CURRENCIES, COLUMNS);
	}

	@Override
	protected ContentValues toContentValues(Currency currency) {
		ContentValues values = new ContentValues();
		values.put(DataBaseHelper.COLUMN_CALCULATION_ID, currency.getCalculationId());
		values.put(DataBaseHelper.COLUMN_CURRENCY_CODE, currency.getCurrencyCode());
		values.put(DataBaseHelper.COLUMN_RATE_THIS, currency.getExchangeRateThis());
		values.put(DataBaseHelper.COLUMN_RATE_MAIN, currency.getExchangeRateMain());
		return values;
	}

	@Override
	public Currency fromCursor(Cursor cursor) {
		long calculationId = cursor.getLong(1);
		Currency currency = new Currency(calculationId);
		currency.setId(cursor.getLong(0));
		currency.setCurrencyCode(cursor.getString(2));
		currency.setExchangeRate(cursor.getDouble(3), cursor.getDouble(4));
		return currency;
	}

	public Cursor listByCalculation(long calculationId) {
		return getDatabase().query(
				DataBaseHelper.TABLE_CURRENCIES, COLUMNS,
				DataBaseHelper.COLUMN_CALCULATION_ID + " = ?", new String[] { Long.toString(calculationId) },
				null, null, null);
	}

}
