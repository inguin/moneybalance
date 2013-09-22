package ivl.android.moneybalance.filter;

import ivl.android.moneybalance.CurrencyHelper;
import ivl.android.moneybalance.data.Calculation;
import ivl.android.moneybalance.data.Expense;
import ivl.android.moneybalance.data.Person;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.content.res.AssetManager;
import biz.source_code.miniTemplator.MiniTemplator;

public class HtmlOutput {

	private final Context context;
	private final Calculation calculation;
	private final List<Person> persons;

	private final CurrencyHelper helper;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private final Map<Long, Person> personsById = new HashMap<Long, Person>();

	private final Set<Calendar> dates = new TreeSet<Calendar>();
	private final Map<Calendar, List<Expense>> expensesByDate = new HashMap<Calendar, List<Expense>>();

	public HtmlOutput(Context context, Calculation calculation) {
		this.context = context;
		this.calculation = calculation;

		helper = new CurrencyHelper(calculation.getCurrency());

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

	public String toHtml() {
		try {
			AssetManager assets = context.getAssets();
			InputStream stream = assets.open("mail_template.html");
			final char[] buffer = new char[1000];
			final Reader in = new InputStreamReader(stream, "utf-8");
			final StringBuilder out = new StringBuilder();
			while (true) {
				int rsz = in.read(buffer, 0, buffer.length);
				if (rsz < 0) break;
				out.append(buffer, 0, rsz);
			}

			long[] totalExpenses = new long[persons.size()];
			double[] totalConsumption = new double[persons.size()];

			MiniTemplator.TemplateSpecification spec = new MiniTemplator.TemplateSpecification();
			spec.templateText = out.toString();
			MiniTemplator t = new MiniTemplator(spec);
			t.setVariable("calculation_title", calculation.getTitle());
			t.setVariable("num_persons", calculation.getPersons().size());

			int row = 0;
			for (Calendar date : dates) {
				for (Expense expense : expensesByDate.get(date)) {
					Map<Long, Double> weights = expense.getSplitWeights();
					List<Double> shares = expense.getShares(persons);

					for (int i = 0; i < persons.size(); i++) {
						totalConsumption[i] += shares.get(i);
						if (persons.get(i).getId() == expense.getPersonId())
							totalExpenses[i] += expense.getAmount();

						String weight = "";
						if (expense.isUnevenSplit()) {
							Double w = weights.get(persons.get(i).getId());
							if (w != null)
								weight = helper.format(w, false);
						}
						t.setVariable("weight", weight);

						String share = "";
						if (shares.get(i) > 0.0001)
							share = helper.formatCents(Math.round(shares.get(i)));
						t.setVariable("share", share);

						t.addBlock("expense_person");
					}

					t.setVariable("odd_even", (++row % 2 == 0) ? "even" : "odd");
					t.setVariable("payer", personsById.get(expense.getPersonId()).getName());
					t.setVariable("title", expense.getTitle());
					t.setVariable("amount", helper.formatCents(expense.getAmount(), true));
					t.addBlock("expense");
				}
				t.setVariable("date", dateFormat.format(date.getTime()));
				t.addBlock("date");
			}

			for (int i = 0; i < persons.size(); i++) {
				Person person = persons.get(i);
				long result = (long) (totalExpenses[i] - totalConsumption[i]);
				t.setVariable("name", person.getName());
				t.setVariable("total_expenses", helper.formatCents(totalExpenses[i]));
				t.setVariable("total_consumption", helper.formatCents((long) totalConsumption[i]));
				t.setVariable("result", helper.formatCents(result));
				t.addBlock("person");
			}

			return t.generateOutput();
		} catch (Exception e) {
			return e.toString();
		}
	}

}
