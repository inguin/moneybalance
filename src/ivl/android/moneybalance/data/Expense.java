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
import java.util.Map;

public class Expense extends DataObject {

	private long personId;
	private String title = new String();
	private long amount;
	private Calendar date = Calendar.getInstance();
	private Map<Long, Double> splitWeights = null;

	public Expense() {
		setDate(Calendar.getInstance());
	}

	public long getPersonId() {
		return personId;
	}
	public void setPersonId(long personId) {
		this.personId = personId;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public long getAmount() {
		return amount;
	}
	public void setAmount(long amount) {
		this.amount = amount;
	}

	public Calendar getDate() {
		return date;
	}
	public void setDate(Calendar date) {
		this.date.clear();
		int year = date.get(Calendar.YEAR);
		int month = date.get(Calendar.MONTH);
		int day = date.get(Calendar.DAY_OF_MONTH);
		this.date.set(year, month, day);
	}

	public Map<Long, Double> getSplitWeights() {
		return splitWeights;
	}
	public void setSplitWeights(Map<Long, Double> splitWeights) {
		this.splitWeights = splitWeights;
	}

	public boolean isUnevenSplit() {
		return (splitWeights != null && splitWeights.size() > 0);
	}

	public List<Double> getShares(List<Person> persons) {
		List<Double> result = new ArrayList<Double>();
		if (!isUnevenSplit()) {
			double share = (double) getAmount() / persons.size();
			for (int i = 0; i < persons.size(); i++)
				result.add(share);
		} else {
			double totalWeight = 0;
			for (double weight : splitWeights.values())
				totalWeight += weight;
			for (int i = 0; i < persons.size(); i++) {
				Person person = persons.get(i);
				Double weight = splitWeights.get(person.getId());
				double share = 0;
				if (weight != null)
					share = amount * weight / totalWeight;
				result.add(share);
			}
		}
		return result;
	}

}
