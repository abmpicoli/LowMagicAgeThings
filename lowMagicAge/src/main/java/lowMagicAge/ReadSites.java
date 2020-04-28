package lowMagicAge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadSites {
	public static interface Operator {
		/**
		 * Specify an operation on the site class, based on the read line.
		 * 
		 * @param site
		 * @param line
		 * @return the next operator to use of the {@link ReadSites#operators} array.
		 */
		public int operate(Site site, String line);
	}

	public static class Price implements Comparable<Price> {

		int good;
		int price;
		String siteId;

		@Override
		public int compareTo(Price arg0) {
			return this.price - arg0.price;
		}

		@Override
		public String toString() {
			return "@" + siteId + "," + good + "=" + price;
		}

	}

	public static class Site {
		private double gold;
		public String id;
		private Map<String, String> itemQuantities = new HashMap<String, String>();
		private List<Integer> priceList = new ArrayList<>();

		public void addItem(String item, String quantity) {
			itemQuantities.put(item, quantity);

		}

		public void addPrice(int value) {
			priceList.add(value);
		}

		public void setGold(double value) {
			gold = value;

		}

		@Override
		public String toString() {
			return id + ":" + itemQuantities;
		}

	}

	private static Pattern _shopItemsPattern = Pattern.compile("\\d+=(.*)");
	private static Matcher _shopItemsPmatcher = _shopItemsPattern.matcher("");
	public static Operator[] operators = new Operator[] { (Site site, String line) -> {// id
		site.id = line;
		return 1;
	}, (Site site, String line) -> { // last visit
		return 2;
	}, (Site site, String line) -> { // town size
		if (line.length() > 1) {
			String[] parts = line.split("\\s+");
			site.setGold(Double.parseDouble(parts[4]));
		}
		return 3;
	}, (Site site, String line) -> { // productions
		return 4;
	}, (Site site, String line) -> { // price factors
		String[] values = line.split(",");
		if (values.length > 0) {
			for (String s : values) {
				site.addPrice(Integer.parseInt(s.trim()));
			}
		}
		return 5;
	}, (Site site, String line) -> { // specialties
		return 6;
	}, (Site site, String line) -> { // jobs
		return 7;
	}, (Site site, String line) -> { // pcs
		return 8;
	}, (Site site, String line) -> { // shop num
		return 9;
	}, (Site site, String line) -> { // shop items
		_shopItemsPmatcher.reset(line);
		if (_shopItemsPmatcher.matches()) {
			String[] items = _shopItemsPmatcher.group(1).split(";");
			for (String item : items) {
				String[] fields = item.split(",");
				site.addItem(fields[0], fields[1]);
			}
			return 9;
		} else {
			return -1;
		}
	}

	};

	private static Map<Integer, SortedSet<Price>> ranks = new HashMap<>();

	public static void main(String[] args) throws Exception {
		File f = new File(
				"D:\\SteamLibrary\\steamapps\\common\\LowMagicAge\\saves\\profile_1\\adventure_2\\auto_save\\wld_1_sites_d.txt");
		try (FileReader fr = new FileReader(f); BufferedReader br = new BufferedReader(fr);) {
			String line;
			Operator operator = null;
			Site site = null;
			do {

				line = br.readLine();
				if ("".equals(line)) {
					process(site);
					operator = operators[0];
					site = new Site();
				} else {
					if (operator != null) {
						int nextOperator = operator.operate(site, line);
						if (nextOperator >= 0) {
							operator = operators[nextOperator];
						} else {
							process(site);
						}
					}
				}

			} while (line != null);
		}
		SortedMap<Integer,List<Integer>> profitXGood = new TreeMap<>(); 
		for (int i = 0; i < ranks.size(); ++i) {
			SortedSet<Price> rank = ranks.get(i);
			Price[] price = rank.toArray(new Price[rank.size()]);
			for (int j = 0; j < 5; ++j) {
				System.out.print(price[j]);
				System.out.print(" ");
			}
			for (int j = price.length - 1; j > price.length - 6; --j) {
				System.out.print(price[j]);
				System.out.print(" ");
			}
			int profit = rank.last().price - rank.first().price;
			List<Integer> value = profitXGood.get(profit);
			if(value==null) {
				value = new ArrayList<Integer>();
				profitXGood.put(profit, value);
			}
			value.add(i);
			System.out.println("Profit = " + profit);
			
		}
		for(Entry<Integer, List<Integer>> x:  profitXGood.entrySet()) {
			System.out.println("Profit=" + x.getKey() + " for goods " + x.getValue());
		}

	}

	private static void process(Site site) {
		if (site != null && site.priceList.size() > 0) {
			for (int i = 0; i < site.priceList.size(); ++i) {
				Price price = new Price();
				price.siteId = site.id;
				price.good = i;
				price.price = site.priceList.get(i);
				SortedSet<Price> rank = ranks.get(i);
				if (rank == null) {
					rank = new TreeSet<Price>();
					ranks.put(i, rank);
				}
				rank.add(price);
			}
		}
	}
}
