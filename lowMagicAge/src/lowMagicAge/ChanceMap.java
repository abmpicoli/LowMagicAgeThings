package lowMagicAge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;


class ChanceMap<T> {
	private Map<T, Double> chance = new HashMap<T, Double>();
	private double max = -1;
	Collection<T> resetList;
	double resetValue;

	/**
	 * Create a populated chancemap with a reset list.
	 * 
	 * @param asList
	 * @param value
	 */
	public ChanceMap(Collection<T> list, double value) {
		this.resetList = list;
		this.resetValue = value;
		for (T x : list) {
			internalAdd(x, value);
		}
	}

	public ChanceMap() {

	}

	public void addAll(Map<T, Double> extraEntries, double factor) {
		for (Entry<T, Double> x : extraEntries.entrySet()) {
			T key = x.getKey();
			double value = x.getValue() * factor;
			internalAdd(key, value);
		}
	}

	public void add(T entry, double value) {
		if(resetList != null && getMax()==0) {
			addAll(resetList,resetValue);
		}
		internalAdd(entry, value);
		if(resetList != null && getMax()==0) {
			addAll(resetList,resetValue);
		}
		//System.out.println(this);
	}

	private void internalAdd(T entry, double value) {
		double endValue = value;
		Double existing = chance.get(entry);
		if (existing != null) {
			endValue = endValue +existing;
		}
		if (endValue <=0 ) {
			endValue = 0;
			chance.remove(entry);
		} else {
			chance.put(entry, endValue);
		}
		max = -1;
	}

	public T random(Random random) {
		double chosen = random.nextDouble() * getMax();
		double min = 0;
		for (Entry<T, Double> x : chance.entrySet()) {
			double max = min + x.getValue();
			if (chosen >= min && chosen < max && max != min) {
				return x.getKey();
			}
			min = max;
		}
		return null;
	}
	
	/**
	 * Returns the chance that this item could be chosen (from 0 to 1).
	 * @param object
	 * @return
	 */
	public double chance(T object) {
		Double value = this.chance.get(object);
		if(value ==null) {
			return 0;
		} else {
			if(getMax() > 0) {
				return value / getMax();
			} else {
				return 0;
			}
		}
	}
	
	public T randomSubtraction(Random random, double subtraction) {
		T chosen = random(random);
		if(chosen != null) {
			add(chosen, -subtraction);
		}
		return chosen;
	}

	public double getMax() {
		if (max == -1) {
			max = 0;
			for (Double x : chance.values()) {
				max = max + x;
			}
		}
		return max;
	}

	public void clear() {
		chance.clear();
	}

	public void put(T x, int i) {
		add(x, i);
	}

	public void put(T x, double d) {
		add(x, d);

	}

	public void addAll(ChanceMap<T> featChances, double value) {
		for (Entry<T, Double> x : featChances.chance.entrySet()) {
			add(x.getKey(), x.getValue() * value);
		}

	}

	/**
	 * Add all entries in the list with equal weight.
	 * @param entries
	 * @param extraAmount
	 */
	public void addAll(Collection<T> entries, double extraAmount) {
		for (T x : entries) {
			add(x, extraAmount);
		}

	}

	public Set<Entry<T, Double>> entrySet() {
		return Collections.unmodifiableSet(chance.entrySet());
	}

	public void addOrReset(T entry, double extraAmount, Collection<T> resetList, double resetValue) {
		if(getMax()==0) {
			addAll(resetList,resetValue);
		}
		add(entry,extraAmount);
		if(getMax()==0) {
			addAll(resetList,resetValue);
		}
		
		
	}
	@Override
	public String toString() {
		return chance.toString();
	}

}