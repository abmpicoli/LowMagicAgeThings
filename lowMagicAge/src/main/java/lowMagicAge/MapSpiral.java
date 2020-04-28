package lowMagicAge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;

import lowMagicAge.MapSpiral.InsertionInfo;

public class MapSpiral {

	private static Random sr;
	static {
		try {
			sr = SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	private static int chainOptimizationSize = 7;

	private static class ChainCandidate {
		private Chain chain;
		private long date = Long.MAX_VALUE;

		public synchronized Chain best(Chain candidate) throws InterruptedException {
			if (candidate == null) {
				return chain;
			}
			if (chain == null) {
				chain = candidate;
			} else {
				if (chain.totalDistance() > candidate.totalDistance()) {
					date = System.currentTimeMillis();
					chain = candidate;
				}
			}
			return chain;
		}

		public boolean isEvolving() {
			return System.currentTimeMillis() - 300000 < date;
		}
	}

	private static ThreadLocal<Map<Integer, Double>> calculatedDistancesBySiteDistanceKeyThLocal = new ThreadLocal<>();
	public static String[] siteTypes = new String[] { "Village", "City", "Capital", "Fort", "Cave", "Ruin",
			"Abandoned Building", "Tent", "Cabin", "Detached House", "Tower", "Manor", "Stronghold", "Farm", "Farm",
			"Farm", "Farm", "Fishery", "Fishery", "Fishery", "Fishery", "Forest Farm", "Forest Farm", "Forest Farm",
			"Forest Farm", "Quarry", "Common Stone Quarry", "Precious Stone Quarry", "Quarry", "Mine", "Iron Mine",
			"Noble Metal Mine", "Special Metal Mine", "Coal Mine", "Coal Mine", "Coal Mine", "Coal Mine" };

	/*
	 * {n="Village" }, {n="City" }, {n="Capital" }, {n="Fort" }, {n="Cave" },
	 * {n="Ruin" }, {n="Abandoned Building" }, {n="Tent" }, {n="Cabin" },
	 * {n="Detached House" }, {n="Tower" }, {n="Manor" }, {n="Stronghold" },
	 * {n="Farm" }, {n="Farm" }, {n="Farm" }, {n="Farm" }, {n="Fishery" },
	 * {n="Fishery" }, {n="Fishery" }, {n="Fishery" }, {n="Forest Farm" },
	 * {n="Forest Farm" }, {n="Forest Farm" }, {n="Forest Farm" }, {n="Quarry" },
	 * {n="Common Stone Quarry" }, {n="Precious Stone Quarry" }, {n="Quarry" },
	 * {n="Mine" }, {n="Iron Mine" }, {n="Noble Metal Mine" },
	 * {n="Special Metal Mine" }, {n="Coal Mine" }, {n="Coal Mine" }, {n="Coal Mine"
	 * }, {n="Coal Mine" }, {n="All Sites" }, {n="All Towns" }, {n="All Dungeons" },
	 * }
	 * 
	 */
	public static class InsertionInfo {
		public Link previous;
		public final Site insertion;
		public Link next;
		public final Chain chain;
		public final double cost;

		public InsertionInfo(Chain chain, Link previous, Site insertion, Link next) throws InterruptedException {
			this.previous = previous;
			this.insertion = insertion;
			this.next = next;
			this.chain = chain;
			if (previous == null) {
				this.cost = 0;
			} else {
				this.cost = previous.site.distance(insertion, true) + insertion.distance(next.site, true)
						- previous.site.distance(next.site, true);
			}
		}

		public Link execute() {
			Link result = new Link(insertion, previous, next, chain);
			if (previous == null) {
				previous = result;
				next = result;
			} else {
				previous.next = result;
				next.previous = result;
			}
			chain.linksPerSite.put(insertion, result);
			chain.dirty();
			return result;
		}

	}

	static long nextReport = Long.MIN_VALUE;

	private static void report(Supplier<String> msg) throws InterruptedException {
		if (nextReport < System.currentTimeMillis()) {
			nextReport = System.currentTimeMillis() + 1000l;
			System.out.println(Thread.currentThread().getName() + ":" + msg.get());

			Thread.sleep(1);
		}

	}

	static class Link {
		private Site site;
		private Link previous;
		private Link next;
		private Chain chain;

		public Link(Site site, Link previous, Link next, Chain chain) {
			this.site = site;
			this.previous = previous;
			this.next = next;
			this.chain = chain;
		}

		public InsertionInfo getInsertionCost(Site s) throws InterruptedException {
			InsertionInfo info1 = new InsertionInfo(chain, previous, s, this);
			InsertionInfo info2 = new InsertionInfo(chain, this, s, next);
			InsertionInfo result = info1.cost < info2.cost ? info1 : info2;
			if (result.cost > 100000) {
				return null;
			}
			return result;
		}

		@Override
		public String toString() {
			return ((previous != null ? previous.site.id : "") + "::" + site.toString() + "::"
					+ (next != null ? next.site.id : ""));
		}

		public double getLinearDistance(Link l2) {
			double delta1 = l2.site.x - this.site.x;
			double result = delta1 * delta1;
			delta1 = l2.site.y - this.site.y;
			result += delta1 * delta1;
			return Math.sqrt(result);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Link)) {
				return false;
			} else {
				Link o = (Link) obj;
				return o.site == this.site && o.next.site == this.next.site && o.previous.site == this.previous.site;
			}
		}

		@Override
		public int hashCode() {
			return this.site.id;
		}
	}

	static class Chain implements Iterable<Link> {
		// being a circular chain, having any link of the chain will return all the
		// chain.
		Link theFirstLink;
		Map<Site, Link> linksPerSite = new TreeMap<>();
		private int[] centerCoordinates;
		private double totalDistance;

		/**
		 * Creates a "incomplete" chain, based on the parent chain, where there are
		 * "missing" items.
		 * 
		 * @param chain
		 * @param chance
		 * @param amount
		 * @param random
		 * @throws InterruptedException 
		 */
		public Chain(Chain chain, double fraction, Random random) throws InterruptedException {
			int toRemove = (int) (chain.linksPerSite.size() * fraction + 1);
			int holes = random.nextInt(5) + 1;
			int amountPerHole = (toRemove / holes * 2 + 1);
			double chanceOfHole = ((double) holes) / chain.linksPerSite.size();
			int thisAmount = 0;
			for (Link x : chain) {
				if (thisAmount > 0) {
					--thisAmount;
					--toRemove;
					if (toRemove > 0) {
						continue;
					} else {
						break;
					}
				}
				if (random.nextDouble() < chanceOfHole) {
					thisAmount = 1 + random.nextInt(amountPerHole);
					continue;
				}
				append(x.site);
			}
		}

		/** Creates an "empty" chain. */
		public Chain() {

		}

		/**
		 * Forcefully append the site in the last point of the chain.
		 * 
		 * @param site
		 * @throws InterruptedException 
		 */
		public Link append(Site site) throws InterruptedException {

			if (linksPerSite.containsKey(site)) {
				return linksPerSite.get(site);
			}
			dirty();
			Link resultLink;
			if (theFirstLink == null) {
				theFirstLink = new Link(site, null, null, this);
				resultLink = theFirstLink;
			} else {
				if (theFirstLink.next == null) {
					resultLink = new Link(site, theFirstLink, theFirstLink, this);
					theFirstLink.next = resultLink;
					theFirstLink.previous = resultLink;
				} else {
					Link oldPrevious = theFirstLink.previous;
					resultLink = new Link(site, oldPrevious, theFirstLink, this);
					theFirstLink.previous = resultLink;
					oldPrevious.next = resultLink;
				}
			}
			linksPerSite.put(site, resultLink);
			optimize(site);
			return resultLink;
		}

		/**
		 * Adds a site to a chain.
		 * 
		 * @param site
		 * @return true if the site could be added to this chain. False if the site is
		 *         already in some chain or there wasn't a valid chain.
		 */
		public boolean add(Site site) throws InterruptedException {
			dirty();
			try {
			if (linksPerSite.containsKey(site)) {
				return false;
			}
			if (theFirstLink == null) {
				theFirstLink = new Link(site, null, null, this);
				theFirstLink.previous = theFirstLink;
				theFirstLink.next = theFirstLink;
				return true;
			} else {
				InsertionInfo info = getInsertionCost(site);
				if (info != null) {
					Link l = info.execute();
					linksPerSite.put(site, l);
					return true;
				} else {
					return false;
				}
			}
			} finally {
				optimize(site);
			}

		}

		/**
		 * make small local optimizations around a site.  
		 * @throws InterruptedException 
		 */
		private void optimize(Site site) throws InterruptedException {
			 
			if(linksPerSite.size() > chainOptimizationSize) {
				Link centralLink = linksPerSite.get(site);
				
				Link startLink = centralLink;
				for(int i=0; i < chainOptimizationSize/2;++i) {
					startLink = startLink.previous;
				}
				Link previousInsertion = startLink.previous;
				LinkedList<Site> originalSnippet = new LinkedList<Site>();
				Link currentLink = startLink;
				while(originalSnippet.size() < chainOptimizationSize) {
					originalSnippet.add(currentLink.site);
					currentLink = currentLink.next;
				}
				Link nextInsertion = currentLink;

				boolean changed = false;
				do {
					originalSnippet.add(currentLink.site);
					currentLink = currentLink.next;
				} while(currentLink != nextInsertion);
				LinkedList<Site> best = new LinkedList<Site>();
				LinkedList<Site> candidate = new LinkedList<Site>();
				LinkedList<Site> remaining = new LinkedList<Site>();
				best.addAll(originalSnippet);
				double bestDistance = snippetDistance(previousInsertion,originalSnippet,nextInsertion);
				int[] counters = new int[chainOptimizationSize-1];
				counters[0]=-1;
				EVALUATE_ALL_POSSIBILITIES: while(true) {
					double candidateDistance = 0;
					candidate.clear();
					remaining.clear();
					remaining.addAll(originalSnippet);
					int i=0;
					int max = chainOptimizationSize-1;
					do {
						counters[i] = counters[i]+1;
						if(counters[i] > max) {
							counters[i]=0;
							i=i+1;
							max=max-1;
						} else {
							break;
						}
					} while( i < counters.length);
					if(i == counters.length) {
						break;
					}
					Site previous = previousInsertion.site;
					for(i=0; i < counters.length;++i) {
						Site current = remaining.remove(counters[i]);
						candidate.add(current);
						candidateDistance += previous.distance(current, true);
						if(candidateDistance > bestDistance ) {
							continue EVALUATE_ALL_POSSIBILITIES;
						}
					}
					candidateDistance += candidate.getLast().distance(nextInsertion.site,true);
					if(candidateDistance < bestDistance) {
						double repCandidateDistance = candidateDistance;
						double repBestDistance=  bestDistance;
						report(()-> "found better order: from\n " + best + " to \n" + candidate + "\n we have a distance economy from " + repBestDistance + " to " + repCandidateDistance);
						best.clear();
						best.addAll(candidate);
						bestDistance = candidateDistance;
						changed=true;
					}
				}
				if(changed) {
					Link current= previousInsertion;
					for(Site s: best) {
						Link prev = current;
						current = linksPerSite.get(s);
						prev.next= current;
						current.previous = prev;
					}
					current.next = nextInsertion;
					nextInsertion.previous = current;
					dirty();
				}
			}
		}

		private double snippetDistance(Link previousInsertion, LinkedList<Site> snippet, Link nextInsertion) throws InterruptedException {
			Site previous = previousInsertion.site;
			double total = 0;
			for(Site x: snippet) {
				total += previous.distance(x, true);
			}
			total = total + snippet.getLast().distance(nextInsertion.site,true);
			return total;
		}

		private void dirty() {
			centerCoordinates = null;
			totalDistance = -1.0;
		}

		@Override
		public String toString() {
			Link current = theFirstLink;
			StringBuilder builder = new StringBuilder();

			int index = 0;
			do {
				if (!(builder.length() == 0)) {
					builder.append("  ==>  ");
				} else {
					builder.append(">>>>>\n");
				}
				if (index++ % 3 == 0) {
					builder.append("\n");
				}
				builder.append(current);
				current = current.next;
				if (builder.length() > 100000) {
					System.out.println("??? too big: " + builder);
					throw new RuntimeException();
				}
			} while (current != theFirstLink);
			builder.append("\n========");
			return builder.toString();
		}

		@Override
		public Iterator<Link> iterator() {
			return new Iterator<Link>() {
				Link current = null;

				@Override
				public boolean hasNext() {
					if (theFirstLink == null) {
						return false;
					}
					return current == null || current != theFirstLink;
				}

				@Override
				public Link next() {
					if (hasNext()) {
						Link result;
						if (current == null) {
							result = theFirstLink;
						} else {
							result = current;
						}
						current = result.next;
						return result;
					}
					throw new NoSuchElementException();
				}

			};
		}

		public double getAbsorptionCost(Chain chain) throws InterruptedException {
			double insertionCost = estimatedDistance(chain) * 2;
			for (Link l : chain) {
				for (Link l2 : this) {
					if (l.getLinearDistance(l2) * 3 < insertionCost) {
						double thisDistance = l.site.distance(l2.site, true) - l2.site.distance(l2.next.site, true)
								+ l2.next.site.distance(l.next.site, true) - l.site.distance(l.next.site, true);
						insertionCost = Double.min(insertionCost, thisDistance);
						thisDistance = l.site.distance(l2.next.site, true) - l2.site.distance(l2.next.site, true)
								+ l2.next.site.distance(l.site, true) - l.site.distance(l.next.site, true);
						insertionCost = Double.min(insertionCost, thisDistance);
					}
				}
			}
			return insertionCost;

		}

		private double estimatedDistance(Chain chain) {
			double result = 0;
			for (int i = 0; i < 2; ++i) {
				double delta1 = chain.getCenterCoordinates()[i] - this.getCenterCoordinates()[i];
				delta1 = delta1 * delta1;
				result += delta1;
			}
			return Math.sqrt(result);
		}

		/**
		 * Gets the "center" of the chain.
		 * 
		 * @return
		 */
		public int[] getCenterCoordinates() {
			if (centerCoordinates == null) {
				double[] coordinates = new double[2];
				for (Site x : linksPerSite.keySet()) {
					coordinates[0] += x.x;
					coordinates[1] += x.y;
				}
				int[] result = new int[2];
				for (int i = 0; i < coordinates.length; ++i) {
					coordinates[i] = coordinates[i] / linksPerSite.size() + 0.5;
					result[i] = (int) coordinates[i];
				}
				centerCoordinates = result;
			}
			return centerCoordinates;

		}

		public double getEstimatedAbsorptionCost(Chain chain2) {
			int[] c1 = chain2.getCenterCoordinates();
			int[] c2 = this.getCenterCoordinates();
			double result = 0;
			for (int i = 0; i < 2; ++i) {
				double squared = c1[i] - c2[i];
				squared = squared * squared;
				result += squared;
			}
			result = Math.sqrt(result);
			return result;
		}

		public void absorb(Chain chain) throws InterruptedException {
			System.out.println("ABSORBING CHAIN " + chain + " \n INTO \n " + this);
			double insertionCost = Double.MAX_VALUE;
			Link bestSource1 = null;
			Link bestDestination1 = null;
			Link bestSource2 = null;
			Link bestDestination2 = null;
			boolean reverseDestination = false;
			for (Link l : this) {
				for (Link l2 : chain) {
					if (l.getLinearDistance(l2) * 3 < insertionCost) {
						double thisDistance = l.site.distance(l2.next.site, true) - l2.site.distance(l2.next.site, true)
								+ l2.site.distance(l.next.site, true) - l.site.distance(l.next.site, true);
						insertionCost = Double.min(insertionCost, thisDistance);
						if (insertionCost == thisDistance) {
							bestSource1 = l;
							bestDestination1 = l2.next;
							bestSource2 = l2;
							bestDestination2 = l.next;
							reverseDestination = false;
						}
						thisDistance = l.site.distance(l2.site, true) - l2.site.distance(l2.next.site, true)
								+ l2.next.site.distance(l.next.site, true) - l.site.distance(l.next.site, true);
						insertionCost = Double.min(insertionCost, thisDistance);
						if (insertionCost == thisDistance) {
							bestSource1 = l;
							bestDestination1 = l2;
							bestSource2 = l2.next;
							bestDestination2 = l.next;
							reverseDestination = true;
						}

					}
					double result = insertionCost;
					report(() -> {
						return "Current absorption cost: " + result;
					});
				}
			}
			System.out.println("Found the following connections to make : " + bestSource1 + " to " + bestDestination1
					+ " and " + bestSource2 + " to " + bestDestination2);
			if (reverseDestination) {
				List<Link> theLinks = new ArrayList<>();
				for (Link l : chain) {
					theLinks.add(l);
				}
				Collections.reverse(theLinks);
				System.out.println("Reversing order: from " + chain);
				for (int i = 0; i < theLinks.size(); ++i) {
					int next = (i + 1) % theLinks.size();
					int previous = (i + theLinks.size() - 1) % theLinks.size();
					theLinks.get(i).previous = theLinks.get(previous);
					theLinks.get(i).next = theLinks.get(next);
				}
				System.out.println("To: " + chain);
			}
			if (bestSource1 != null) {
				dirty();
				bestSource1.next = bestDestination1;
				bestDestination1.previous = bestSource1;
				bestSource2.next = bestDestination2;
				bestDestination2.previous = bestSource2;
				this.linksPerSite.putAll(chain.linksPerSite);
				System.out.println("NEW CHAIN:" + this);
			} else {
				System.out.println("NO SUITABLE PLACE???");
			}

		}

		public InsertionInfo getInsertionCost(Site s) throws InterruptedException {
			if (linksPerSite.containsKey(s)) {
				return null;
			}
			InsertionInfo best = null;
			if (theFirstLink == null) {
				return new InsertionInfo(this, null, s, null);
			}
			for (Link l : this) {
				InsertionInfo candidate = l.getInsertionCost(s);
				if (best == null) {
					best = candidate;
				} else {
					if (candidate != null) {
						best = best.cost < candidate.cost ? best : candidate;
					}
				}
			}
			return best;
		}

		public boolean isEmpty() {
			return theFirstLink == null;
		}

		public synchronized double totalDistance() throws InterruptedException {
			if (totalDistance < 0) {
				if (!isEmpty()) {
					double distanceRecalc = 0.0;
					for (Link x : this) {
						distanceRecalc += x.site.distance(x.next.site, true);
					}
					this.totalDistance = distanceRecalc;
				} else {
					totalDistance = 0;
				}
			}
			return totalDistance;
		}

	}

	static char[][] map;

	static Map<Character, Double> cost = new HashMap<>();

	static {

		try (BufferedReader reader = new BufferedReader(
				new FileReader(new File("D:\\SteamLibrary\\steamapps\\common\\LowMagicAge\\wlds\\wld_1_map.txt")))) {
			map = new char[128][64];
			reader.readLine();
			for (int i = 0; i <= 63; ++i) {
				String line = reader.readLine();
				for (int x = 0; x < 127; ++x) {
					map[x][i] = line.charAt(x);
				}
			}
			cost.put(' ', 1.0);
			cost.put('T', 2.0);
			cost.put('^', 2.0);
			cost.put('~', 10000.0);
			cost.put('M', 10000.0);
			cost.put('-', 10000.0);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	static Map<Integer, AtomicReference<Double>> calculatedDistancesBySiteDistanceKey = new HashMap<>();

	static Map<Integer, Site> siteByMapPosition = new HashMap<>();

	private static final double BEING_CALCULATED = -1;

	static class Site implements Comparable<Site> {
		static final Pattern pattern = Pattern
				.compile("\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)(\\s+(.*),(.*))?");

		String name;
		int x;
		int y;
		int id;
		int type;

		@Override
		public boolean equals(Object obj) {
			return ((Site) obj).id == this.id;
		}

		@Override
		public int hashCode() {
			return id;
		}

		public Site(Matcher matcher) {
			x = Integer.parseInt(matcher.group(5));
			y = Integer.parseInt(matcher.group(6));
			siteByMapPosition.put(mapindex(x, y), this);
			name = matcher.group(8);
			id = Integer.parseInt(matcher.group(1));
			type = Integer.parseInt(matcher.group(2));
			if (name == null) {
				name = siteTypes[type] + "(" + type + ")";
			}
			double theCost = Double.MAX_VALUE;
			for (int i = -1; i <= 1; ++i) {
				for (int j = -1; j <= 1; ++j) {
					theCost = Math.min(cost.get(map[x + i][y + j]), theCost);
				}
			}
			if (theCost > 2) {
				System.out.println("Impassable site?? " + this);
			}
		}

		public Site(int x, int y, int id, String name) {
			this.x = x;
			this.y = y;
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString() {
			return id + ":" + name + " (" + x + "," + y + ")";
		}

		public Double distance(Site other, boolean lazy) throws InterruptedException {
			Double result;
			if (!lazy) {
				report(() -> "calling distance " + this + " to " + other + " , " + lazy);
			}
			if (other.id == this.id) {
				result = 0.0;
			} else {
				int smallId, largeId;
				if (other.id < this.id) {
					smallId = other.id;
					largeId = this.id;
				} else {
					smallId = this.id;
					largeId = other.id;
				}
				int key = smallId * 1000 + largeId;
				Function<Integer, Double> theGetValue;
				Map<Integer, Double> thLocal = calculatedDistancesBySiteDistanceKeyThLocal.get();
				if (thLocal != null) {
					theGetValue = (i) -> thLocal.get(i);
				} else {
					theGetValue = (i) -> {
						AtomicReference<Double> theDistance;
						synchronized (calculatedDistancesBySiteDistanceKey) {
							theDistance = calculatedDistancesBySiteDistanceKey.get(key);
							if (theDistance == null) {
								theDistance = new AtomicReference<Double>();
								calculatedDistancesBySiteDistanceKey.put(key, theDistance);
							}
						}
						if (lazy) {
							return theDistance.get();
						} else {
							if (theDistance.get() == null) {
								theDistance.set(BEING_CALCULATED);
								try {
									theDistance.set(
											dist(this.x, this.y, other.x, other.y, 0.0, new double[] { 0.0 }, null));
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								return (theDistance.get());
							} else if (theDistance.get() == BEING_CALCULATED) {
								return (Double.MAX_VALUE);
							} else {
								return (theDistance.get());
							}
						}
					};

				}
				return theGetValue.apply(key);

			}

			report(() -> "FOUND IT! " + this + " to " + other + "," + lazy + " = " + result);

			return result;
		}

		@Override
		public int compareTo(Site arg0) {
			return this.id - arg0.id;
		}

		public double linearDistance(Site s2) {
			return MapSpiral.linearDistance(this.x, this.y, s2.x, s2.y);
		}
	}

	static int mapindex(int x, int y) {
		return y * 128 + x;
	}

	static double dist(int x, int y, int x2, int y2, double soFar, double[] maxDistance, Set<Integer> routeProvided)
			throws InterruptedException {
		Set<Integer>[] routeForPlay = new Set[] { routeProvided };
		report(() -> "XXX : dist(" + x + "," + y + "," + x2 + "," + y2 + "," + soFar + "," + maxDistance[0] + ","
				+ routeForPlay[0]);
		double linearDistance = linearDistance(x, y, x2, y2);
		;
		if (maxDistance[0] == 0.0) {
			maxDistance[0] = linearDistance;
			maxDistance[0] = maxDistance[0] * 2.3;
		}

		if (maxDistance[0] >  143 * 2.3) {
			return Double.MAX_VALUE;
		}
		if (soFar + linearDistance > maxDistance[0]) {
			return Double.MAX_VALUE;
		}
		if (routeForPlay[0] == null) {
			routeForPlay[0] = new HashSet<>();
		}
		if (routeForPlay[0].contains(mapindex(x, y)) || routeForPlay[0].contains(mapindex(x2, y2))) {
			return Double.MAX_VALUE;
		}
		routeForPlay[0].add(mapindex(x, y));
		if (x == x2 && y == y2) {
			return soFar;
		}
		if (soFar > maxDistance[0]) {
			return Double.MAX_VALUE;
		}
		Site s1;
		Site s2;
		s1 = siteByMapPosition.get(mapindex(x, y));
		s2 = siteByMapPosition.get(mapindex(x2, y2));
		if (s1 != null && s2 != null) {
			Double lazyDistance = s1.distance(s2, true);
			if (lazyDistance == null) {
				return soFar + s1.distance(s2, false);
			} else if (lazyDistance != BEING_CALCULATED) {
				return soFar + lazyDistance;
			}
		}

		double bestDistance = Double.MAX_VALUE;

		int dx = (int) Math.signum(x2 - x);
		int dy = (int) Math.signum(y2 - y);
		List<int[]> deltas = new ArrayList<>();
		/*
		 * if (dx != 0) { if (dy != 0) { deltas.add(new int[] { dx, dy });
		 * deltas.add(new int[] { dx, 0 }); deltas.add(new int[] { 0, dy }); } else {
		 * deltas.add(new int[] { dx, 0 }); deltas.add(new int[] { dx, -1 });
		 * deltas.add(new int[] { dx, 1 }); } } else { deltas.add(new int[] { 0, dy });
		 * deltas.add(new int[] { -1, dy }); deltas.add(new int[] { 1, dy }); }
		 */
		for (int i = -1; i <= 1; ++i) {
			for (int j = -1; j <= 1; ++j) {
				deltas.add(new int[] { i, j });
			}
		}

		for (int[] d : deltas) {
			int ix = d[0];
			int iy = d[1];
			if (ix == 0 && iy == 0) {
				continue;
			}
			double multiplier;
			if (ix != 0 && iy != 0) {
				multiplier = 1.41;
			} else {
				multiplier = 1.0;
			}
			int nextX = x + ix;
			if (nextX < 0 || nextX > 127) {
				continue;
			}
			int nextY = y + iy;
			if (nextY < 0 || nextY > 63) {
				continue;
			}
			double moveCost;
			if (nextX == x2 && nextY == y2) {
				moveCost = 1;
			} else {
				moveCost = (cost.get(map[nextX][nextY]));
				moveCost = moveCost * multiplier;
			}
			if (soFar + moveCost > maxDistance[0]) {
				continue;
			}
			bestDistance = Math.min(bestDistance,
					dist(nextX, nextY, x2, y2, soFar + moveCost, maxDistance, routeForPlay[0]));
			maxDistance[0] = Math.min(maxDistance[0], bestDistance);

		}
		return bestDistance;

	}

	private static double linearDistance(int x, int y, int x2, int y2) {
		double linearDistance = Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y));
		return linearDistance;
	}

	public static void main(String[] args) throws Exception {
		
		File f = new File("D:\\SteamLibrary\\steamapps\\common\\LowMagicAge\\wlds\\wld_1_sites.txt");
		// wsites v1 329 // id type img_var img_flip x y[ en_name,cs_name]

		Matcher matcher = Site.pattern.matcher("");
		List<Site> sites = new ArrayList<>();
		try (FileReader fr = new FileReader(f); BufferedReader reader = new BufferedReader(fr);) {
			String line;
			do {
				line = reader.readLine();
				if (line != null) {
					matcher.reset(line);
					if (matcher.matches()) {
						Site s = new Site(matcher);
						sites.add(s);
						System.out.println(s);
					}
				}
			} while (line != null);
			
			
			long end = System.currentTimeMillis() + 1800000l;

			ChainCandidate bestChain = new ChainCandidate();
			AtomicInteger iteration = new AtomicInteger();
			System.out.println("Calculating ALL distances");
			for (int i = 0; i < sites.size() - 1; ++i) {
				for (int j = i + 1; j < sites.size(); ++j) {
					sites.get(i).distance(sites.get(j), false);
				}
				System.out.println("==== at i=" + i);
			}

			Runnable runnable = () -> findSiteInParallel(sites, end, bestChain, iteration);
			Thread[] th = new Thread[Runtime.getRuntime().availableProcessors()];
			for (int i = 0; i < th.length; ++i) {
				th[i] = new Thread(runnable);
				th[i].start();
			}
			for (int i = 0; i < th.length; ++i) {
				th[i].join();
			}
			System.out.println("The best chain is:\n");
			for (Link x : bestChain.chain) {
				System.out.println(x.site.name + " (" + x.site.x + "," + x.site.y + ")");
			}

			System.out.println("The best chain is:\n=====\n" + bestChain.chain + "\n-----\nTotal distance "
					+ bestChain.chain.totalDistance());

		}

	}

	private static void findSiteInParallel(List<Site> sites, long end, ChainCandidate bestChain,
			AtomicInteger iteration) {
		try {
			Random random = new Random(sr.nextLong());
			Map<Integer, Double> theThreadLocalMap = new HashMap<>();
			for (Entry<Integer, AtomicReference<Double>> x : calculatedDistancesBySiteDistanceKey.entrySet()) {
				theThreadLocalMap.put(x.getKey(), x.getValue().get());
			}
			List<Site> permanentShuffleSeed = new ArrayList<>();
			permanentShuffleSeed.addAll(sites);
			Collections.shuffle(permanentShuffleSeed);
			int permanentShuffleSeedIndex = 0;
			double bestDistance = Double.MAX_VALUE;
			calculatedDistancesBySiteDistanceKeyThLocal.set(theThreadLocalMap);
			List<Site> thisIterationShuffle = new ArrayList<>();
			Set<Site> thisIterationSites = new LinkedHashSet<Site>();
			Chain chain = null;
			List<Integer> chainCutSequence = new ArrayList<Integer>();
			for (int i = 0; i < sites.size(); ++i) {
				chainCutSequence.add(i);
			}
			Collections.shuffle(chainCutSequence, random);
			int optimizationSequence = 0;
			Chain currentlyKnownBestChain = bestChain.chain;
			int useOptimization = 0;
			while (System.currentTimeMillis() < end && bestChain.isEvolving()) {
				int thisIterationNumber = iteration.incrementAndGet();
				String mode = null;
				if (chain == null) {
					thisIterationShuffle.addAll(sites);
					Collections.shuffle(thisIterationShuffle, random);
					chain = new Chain();
					thisIterationSites.addAll(thisIterationShuffle);
					for (int i = 0; i < 3; ++i) {
						Site s = thisIterationSites.iterator().next();
						chain.append(s);
						thisIterationSites.remove(s);
					}
					System.out.println("Starting iterationNumber " + thisIterationNumber + " by new random chain");
					mode = "random";
				}
				double currentDistance = chain.totalDistance();
				if (currentDistance > bestDistance) {
					System.out.println(thisIterationNumber + " iteration skipped: Current distance "
							+ chain.totalDistance() + " > best distance " + bestDistance);
					chain = null;
				}

				boolean isRepeating = false;
				while (!thisIterationSites.isEmpty() && !isRepeating && chain != null) {
					if (chain.isEmpty()) {
						Site site = thisIterationSites.iterator().next();
						chain.add(site);
						thisIterationSites.remove(site);
					}
					Chain chain2 = chain;
					InsertionInfo[] best = new InsertionInfo[1];
					for (Site s : thisIterationSites) {
						InsertionInfo info = chain.getInsertionCost(s);
						if (info != null) {
							if (best[0] == null) {
								best[0] = info;
							} else {
								double bestCost = best[0].cost;
								double infoCost = info.cost;
								double total = bestCost + infoCost;
								bestCost = bestCost / total;
								bestCost = bestCost * bestCost;
								infoCost = infoCost / total;
								infoCost = infoCost * infoCost;
								total = bestCost + infoCost;
								bestCost = bestCost / total;
								if (random.nextDouble() < bestCost) {
									best[0] = info;
								}
							}
						}

						report(() -> thisIterationNumber + " the chain so far: " + chain2.linksPerSite.size()
								+ "entries best is " + best[0] + " remains " + thisIterationSites.size());
					}
					report(() -> thisIterationNumber + "FINAL OUTCOME: the chain so far: " + chain2.linksPerSite.size()
							+ "entries best is " + best[0] + " remains " + thisIterationSites.size());

					if (best[0] != null) {
						currentDistance = currentDistance + best[0].cost;
						if (currentDistance > bestDistance && chain.totalDistance() > bestDistance) {
							System.out.println(thisIterationNumber + " iteration skipped: Current distance "
									+ chain.totalDistance() + " > best distance " + bestDistance);
							chain = null;
							break;
						}
						Link result = best[0].execute();

						if (result != null) {
							thisIterationSites.remove(result.site);
						} else {
							System.out.println("Couldn't execute " + best[0]);
						}
					} else {
						synchronized (MapSpiral.class) {
							System.out.println("Site left: " + thisIterationSites);
							isRepeating = true;

							for (Site s : thisIterationSites) {
								for (Site s2 : sites) {
									System.out.printf("Site: %s Distance %f LinearDistance %f \n ", s2,
											s.distance(s2, true), s.linearDistance(s2));
								}
							}
							return;
						}

					}
				}

				synchronized (bestChain) {
					if (bestChain.chain != null) {
						bestDistance = bestChain.chain.totalDistance();
						if (bestChain.chain != currentlyKnownBestChain) {
							optimizationSequence = 0;
						}
						currentlyKnownBestChain = bestChain.chain;
					}

					if (chain != null) {
						Chain oldChain = bestChain.chain;
						bestChain.best(chain);
						bestDistance = bestChain.chain.totalDistance();
						if (bestChain.chain != oldChain && bestChain.chain != null) {
							System.out.println("FOUND A BETTER CHAIN!\n====" + bestChain.chain);
							optimizationSequence = 0;
						}
						System.out.println("iteration #: " + thisIterationNumber + " by " + mode);
						System.out.println("Old chain length: "
								+ (oldChain != null ? oldChain.totalDistance() : " not available"));
						System.out.println("Best chain length: " + bestChain.chain.totalDistance());
						System.out.println("This chain length: " + chain.totalDistance());
					}
					mode = "unknown";
					thisIterationSites.clear();
					thisIterationShuffle.clear();
					chain = null;
					useOptimization = (useOptimization + 1) % 3;
					if (useOptimization != 0 ) {
						for (; optimizationSequence < sites.size(); ++optimizationSequence) {
							chain = new Chain(bestChain.chain, 0.03, random);
							if (chain.totalDistance() < bestDistance && bestChain.isEvolving()) {
								break;

							}
							if (chain.totalDistance() >= bestDistance) {
								chain = null;
							} else {
								mode = "inheritance";
							}
						}
					}
					if (chain == null) {
						FIND_GOOD_SEED: do {
							chain = new Chain();
							chain.append(permanentShuffleSeed.get(permanentShuffleSeedIndex));
							chain.append(permanentShuffleSeed.get(permanentShuffleSeedIndex + 1));
							chain.append(permanentShuffleSeed.get(permanentShuffleSeedIndex + 2));
							if (chain.totalDistance() < bestDistance) {
								mode = "random";
								break FIND_GOOD_SEED;
							}
							++permanentShuffleSeedIndex;
							if(permanentShuffleSeedIndex > sites.size()-2) {
								permanentShuffleSeedIndex = 0;
								permanentShuffleSeed.clear();
								permanentShuffleSeed.addAll(sites);
								Collections.shuffle(permanentShuffleSeed,random);
							}
						} while (bestChain.isEvolving() && System.currentTimeMillis() < end);
					}
					if (chain != null) {
						thisIterationSites.addAll(sites);
						thisIterationSites.removeAll(chain.linksPerSite.keySet());
						thisIterationShuffle.clear();
						thisIterationShuffle.addAll(thisIterationSites);
						Collections.shuffle(thisIterationShuffle, random);
						System.out.println("Starting iterationNumber " + thisIterationNumber + " by " + mode);
					} else {
						System.out.println("Breaking iteration: couldn't find a good candidate");
						return;
					}
				}

			}
		} catch (InterruptedException ignoreIt) {
			System.out.println("Interrupted");
			return;
		}
	}
}
