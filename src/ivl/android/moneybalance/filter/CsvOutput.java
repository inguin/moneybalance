package ivl.android.moneybalance.filter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ivl.android.moneybalance.CurrencyHelper;
import ivl.android.moneybalance.data.Calculation;
import ivl.android.moneybalance.data.Currency;
import ivl.android.moneybalance.data.Expense;
import ivl.android.moneybalance.data.Person;

public class CsvOutput {

	private final Calculation calculation;
	private final List<Person> persons;
	private final boolean multiCurrency;

	private final CurrencyHelper helper;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

	private final Set<Calendar> dates = new TreeSet<>();
	private final Map<Calendar, List<Expense>> expensesByDate = new HashMap<>();

	private StringBuffer buffer;
	private int row;

	public CsvOutput(Calculation calculation) {
		this.calculation = calculation;
		persons = calculation.getPersons();
		multiCurrency = (calculation.getCurrencies().size() > 1);

		helper = calculation.getMainCurrency().getCurrencyHelper(Locale.ENGLISH);
		helper.setGroupingUsed(false);

		for (Expense expense : calculation.getExpenses()) {
			Calendar date = expense.getDate();
			List<Expense> byDateList = expensesByDate.get(date);
			if (byDateList == null) {
				byDateList = new ArrayList<>();
				expensesByDate.put(date, byDateList);
				dates.add(date);
			}
			byDateList.add(expense);
		}
	}

	public String toCsv() {
		buffer = new StringBuffer();
		row = 0;

		appendTitleRow();
		appendHeadings();

		for (Calendar date : dates) {
			for (Expense expense : expensesByDate.get(date)) {
				appendExpense(date, expense);
			}
		}

		appendTotalExpenses();
		appendTotalConsumptions();
		appendResults();

		return buffer.toString();
	}

	private void appendTitleRow() {
		row++;
		buffer.append(quote(calculation.getTitle()));
		buffer.append("\n");
	}

	private void appendHeadings() {
		row++;
		buffer.append(",,,,");
		if (multiCurrency)
			buffer.append(",,,");
		for (Person person : persons) {
			buffer.append(',');
			buffer.append(quote(person.getName()));
		}
		for (Person person : persons) {
			buffer.append(',');
			buffer.append(quote(person.getName()));
		}
		buffer.append('\n');
	}

	private void appendExpense(Calendar date, Expense expense) {
		row++;

		buffer.append(dateFormat.format(date.getTime()));
		buffer.append(',');
		buffer.append(quote(expense.getPerson().getName()));
		buffer.append(',');
		buffer.append(quote(expense.getTitle()));
		buffer.append(',');

		Currency currency = expense.getCurrency();
		if (multiCurrency) {
			Currency mainCurrency = calculation.getMainCurrency();
			if (currency.equals(mainCurrency)) {
				buffer.append(",,,");
				buffer.append(quote(currency.getCurrencyCode()));
				buffer.append(',');
				buffer.append(helper.format(expense.getAmount(), false));
			} else {
				buffer.append(quote(currency.getCurrencyCode()));
				buffer.append(',');
				buffer.append(helper.format(expense.getAmount(), false));
				buffer.append(',');
				buffer.append(currency.getExchangeRateMain() / currency.getExchangeRateThis());
				buffer.append(',');
				buffer.append(quote(mainCurrency.getCurrencyCode()));
				buffer.append(',');
				buffer.append(String.format("=%s*%s", cell(row, localAmountColumn()), cell(row, exchangeRateColumn())));
			}
		} else {
			buffer.append(quote(currency.getCurrencyCode()));
			buffer.append(',');
			buffer.append(helper.format(expense.getAmount(), false));
		}

		Map<Long, Double> weights = expense.getSplitWeights();

		for (int i = 0; i < persons.size(); i++) {
			String weight = "";
			if (expense.isUnevenSplit()) {
				Double w = weights.get(persons.get(i).getId());
				if (w != null)
					weight = helper.format(w, false);
			} else {
				weight = helper.format(1, false);
			}
			buffer.append(',');
			buffer.append(weight);
		}

		for (int i = 0; i < persons.size(); i++) {
			String amountCell = cell(row, exchangedAmountColumn());
			String firstWeightCell = cell(row, firstWeightColumn());
			String weightCell = cell(row, firstWeightColumn() + i);
			String lastWeightCell = cell(row, firstWeightColumn() + persons.size() - 1);
			String formula = String.format("=%s*%s/SUM(%s:%s)", amountCell, weightCell, firstWeightCell, lastWeightCell);
			buffer.append(',');
			buffer.append(formula);
		}

		buffer.append('\n');
	}

	private void appendTotalExpenses() {
		row++;
		buffer.append(",,,,");
		if (multiCurrency)
			buffer.append(",,,");

		for (int i = 0; i < persons.size(); i++)
			buffer.append(',');

		for (int i = 0; i < persons.size(); i++) {
			int column = firstShareColumn() + i;
			String firstNameCell = cell(3, nameColumn());
			String lastNameCell = cell(3 + calculation.getExpenses().size() - 1, nameColumn());
			String nameCell = cell(2, column);
			String firstAmountCell = cell(3, exchangedAmountColumn());
			String lastAmountCell = cell(3 + calculation.getExpenses().size() - 1 , exchangedAmountColumn());
			String formula = String.format("=SUMIF(%s:%s; %s; %s:%s)", firstNameCell, lastNameCell, nameCell, firstAmountCell, lastAmountCell);
			buffer.append(',');
			buffer.append(formula);
		}

		buffer.append('\n');
	}

	private void appendTotalConsumptions() {
		row++;
		buffer.append(",,,,");
		if (multiCurrency)
			buffer.append(",,,");

		for (int i = 0; i < persons.size(); i++)
			buffer.append(',');

		for (int i = 0; i < persons.size(); i++) {
			int column = firstShareColumn() + i;
			String firstCell = cell(3, column);
			String lastCell = cell(3 + calculation.getExpenses().size() - 1, column);
			String formula = String.format("=SUM(%s:%s)", firstCell, lastCell);
			buffer.append(',');
			buffer.append(formula);
		}

		buffer.append('\n');
	}

	private void appendResults() {
		row++;
		buffer.append(",,,,");
		if (multiCurrency)
			buffer.append(",,,");

		for (int i = 0; i < persons.size(); i++)
			buffer.append(',');

		for (int i = 0; i < persons.size(); i++) {
			int column = firstShareColumn() + i;
			String expenseCell = cell(row - 2, column);
			String consumptionCell = cell(row - 1, column);
			String formula = String.format("=%s-%s", expenseCell, consumptionCell);
			buffer.append(',');
			buffer.append(formula);
		}

		buffer.append('\n');
	}

	private String quote(String text) {
		return '"' + text.replace("\"", "\"\"") + '"';
	}

	private String cell(int row, int column) {
		String columns = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String result = "";
		if (column > 26) result += columns.charAt((column - 1) / 26 - 1);
		result += columns.charAt((column - 1) % 26);
		result += Integer.toString(row);
		return result;
	}

	// Column numbers (n = Number of persons):
	// Single | Multi | Description
	//     1  |    1  |  Date
	//     2  |    2  |  Payer
	//     3  |    3  |  Title
	//     -  |    4  |  Local Currency
	//     -  |    5  |  Amount (local currency)
	//     -  |    6  |  Exchange Rate
	//     4  |    7  |  Main Currency
	//     5  |    8  |  Amount (exchanged)
	//     6  |    9  |  First split weight
	//   5+n  |  8+n  |  Last split weight
	//   6+n  |  9+n  |  First share
	//  5+2n  | 8+2n  |  Last share
	private int nameColumn() {
		return 2;
	}
	private int localAmountColumn() {
		return 5;
	}
	private int exchangeRateColumn() {
		return 6;
	}
	private int exchangedAmountColumn() {
		return multiCurrency ? 8 : 5;
	}
	private int firstWeightColumn() {
		return multiCurrency ? 9 : 6;
	}
	private int firstShareColumn() {
		return firstWeightColumn() + persons.size();
	}

}
