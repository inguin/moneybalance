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
import ivl.android.moneybalance.data.Expense;
import ivl.android.moneybalance.data.Person;

public class CsvOutput {

	private final Calculation calculation;
	private final List<Person> persons;

	private final CurrencyHelper helper;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
	private final Map<Long, Person> personsById = new HashMap<Long, Person>();

	private final Set<Calendar> dates = new TreeSet<Calendar>();
	private final Map<Calendar, List<Expense>> expensesByDate = new HashMap<Calendar, List<Expense>>();

	private StringBuffer buffer;
	private int row;

	public CsvOutput(Calculation calculation) {
		this.calculation = calculation;

		helper = new CurrencyHelper(calculation.getCurrency(), Locale.ENGLISH);
		helper.setGroupingUsed(false);

		persons = calculation.getPersons();
		for (Person person : persons)
			personsById.put(person.getId(), person);

		for (Expense expense : calculation.getExpenses()) {
			Calendar date = expense.getDate();
			List<Expense> byDateList = expensesByDate.get(date);
			if (byDateList == null) {
				byDateList = new ArrayList<Expense>();
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
		buffer.append(quote(calculation.getTitle()) + "\n");
	}

	private void appendHeadings() {
		row++;
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
		buffer.append(quote(personsById.get(expense.getPersonId()).getName()));
		buffer.append(',');
		buffer.append(quote(expense.getTitle()));
		buffer.append(',');
		buffer.append(helper.formatCents(expense.getAmount(), false));

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
			String amountCell = cell(row, 3);
			String firstWeightCell = cell(row, 4);
			String weightCell = cell(row, 4 + i);
			String lastWeightCell = cell(row, 4 + persons.size() - 1);
			String formula = String.format("=%s*%s/SUM(%s:%s)", amountCell, weightCell, firstWeightCell, lastWeightCell);
			buffer.append(',');
			buffer.append(formula);
		}

		buffer.append('\n');
	}

	private void appendTotalExpenses() {
		row++;
		buffer.append(",,,");

		for (int i = 0; i < persons.size(); i++)
			buffer.append(',');

		for (int i = 0; i < persons.size(); i++) {
			int column = 4 + persons.size() + i;
			String firstNameCell = cell(3, 1);
			String lastNameCell = cell(3 + calculation.getExpenses().size() - 1, 1);
			String nameCell = cell(2, column);
			String firstAmountCell = cell(3, 3);
			String lastAmountCell = cell(3 + calculation.getExpenses().size() - 1 , 3);
			String formula = String.format("\"=SUMIF(%s:%s, %s, %s:%s)\"", firstNameCell, lastNameCell, nameCell, firstAmountCell, lastAmountCell);
			buffer.append(',');
			buffer.append(formula);
		}

		buffer.append('\n');
	}

	private void appendTotalConsumptions() {
		row++;
		buffer.append(",,,");

		for (int i = 0; i < persons.size(); i++)
			buffer.append(',');

		for (int i = 0; i < persons.size(); i++) {
			int column = 4 + persons.size() + i;
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
		buffer.append(",,,");

		for (int i = 0; i < persons.size(); i++)
			buffer.append(',');

		for (int i = 0; i < persons.size(); i++) {
			int column = 4 + persons.size() + i;
			String expenseCell = cell(row - 2, column);
			String consumptionCell = cell(row - 1, column);
			String formula = String.format("\"=%s-%s\"", expenseCell, consumptionCell);
			buffer.append(',');
			buffer.append(formula);
		}

		buffer.append('\n');
	}

	private String quote(String text) {
		StringBuilder buf = new StringBuilder();
		buf.append('"');
		buf.append(text.replace("\"", "\"\""));
		buf.append('"');
		return buf.toString();
	}

	private String cell(int row, int column) {
		return String.format("%c%d", 'A' + column, row);
	}

}
