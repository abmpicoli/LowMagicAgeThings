package lowMagicAge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Queue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MapSpiral {
	private static Logger LOG = LogManager.getFormatterLogger();
	private static Logger OUT = LogManager.getFormatterLogger("OUTPUT");
	/**
	 * How extra costly can be a route between two places to be considered an
	 * unviable route. In fraction of the linear distance;
	 */
	private static final double linearDistanceFactor = 4;
	private static final String Queue = null;

	private static double moveCost1Square(int x0, int y0, int x1, int y1) {
		if (x0 < 0 || x0 > 127 || x1 < 0 || x1 > 127 || y0 < 0 || y0 > 63 || y1 < 0 || y1 > 63) {
			return Double.MAX_VALUE;
		}
		int ix = x1 - x0;
		int iy = y1 - y0;
		if (ix < -1 || ix > 1) {
			throw new IllegalArgumentException("Error calling moveCost1Square Delta must be 1 square at most: " + x0
					+ "," + y0 + "," + x1 + "," + y1);

		}
		if (iy < -1 || iy > 1) {
			throw new IllegalArgumentException("Error calling moveCost1Square Delta must be 1 square at most: " + x0
					+ "," + y0 + "," + x1 + "," + y1);
		}
		if (ix == 0 && iy == 0) {
			return 0;
		}
		double multiplier;
		if (ix != 0 && iy != 0) {
			multiplier = 1.41;
		} else {
			multiplier = 1.0;
		}
		double moveCost;
		moveCost = (cost.get(map[x0][y0]) + cost.get(map[x1][y1])) / 2.0;
		moveCost = moveCost * multiplier;
		return moveCost;

	}

	private static Map<Thread, Stack<String>> context = new HashMap<>();

	private static void pushContext(String s) {
		Thread th = Thread.currentThread();
		Stack<String> stack = context.get(th);

		if (stack == null) {
			stack = new Stack<>();
			synchronized (context) {
				context.put(th, stack);
			}
		}
		synchronized (stack) {
			stack.push(s);
		}
	}

	private static class Distance implements Comparable<Distance> {

		private Site start;
		private Site end;
		private double linearDistance;

		public Distance(Site s1, Site s2) {
			int i1 = mapindex(s1.x, s1.y);
			int i2 = mapindex(s2.x, s2.y);
			if (i1 < i2) {
				this.start = s1;
				this.end = s2;
			} else {
				this.start = s2;
				this.end = s1;
			}
			this.linearDistance = s1.linearDistance(s2);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Distance)) {
				return false;
			}
			Distance d = (Distance) o;
			return d.start.equals(this.start) && d.end.equals(this.end);
		}

		@Override
		public int hashCode() {
			return start.hashCode() + end.hashCode();
		}

		@Override
		public int compareTo(Distance o) {
			int result = (int) Math.signum(this.linearDistance - o.linearDistance);
			if (result == 0) {
				result = this.start.id - o.start.id;
			}
			if (result == 0) {
				result = this.end.id - o.end.id;
			}
			return result;
		}

		public double linearDistance() {
			return linearDistance;
		}

	}

	private static String popContext() {
		Thread th = Thread.currentThread();
		Stack<String> stack;
		synchronized (context) {
			stack = context.get(th);
		}
		if (stack != null && !stack.isEmpty()) {
			synchronized (stack) {
				return stack.pop();
			}
		} else {
			return null;
		}
	}

	private static Random sr;
	static {
		try {
			sr = SecureRandom.getInstanceStrong();
			sr = new Random(sr.nextLong());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static class ChainCandidate {
		private Chain currentBestChain;
		private long dateAChainWasFound = Long.MAX_VALUE;
		private List<Site> sites;
		private LinkedList<Site> shuffledSites;
		int mainIterationIndex = 0;
		private long end;
		private long stableDuration;
		private int attempts;
		private int minimumAttempts;

		public synchronized Chain best(Chain candidate) {
			++attempts;
			if (candidate == null) {
				return currentBestChain;
			}
			if (currentBestChain == null) {
				currentBestChain = candidate;
			} else {
				if (currentBestChain.totalDistance() > candidate.totalDistance()) {
					dateAChainWasFound = System.currentTimeMillis();
					currentBestChain = candidate;
				}
			}
			return currentBestChain;
		}

		public ChainCandidate(List<Site> theSites, long testDuration, long stableDuration, int minimumAttempts) {
			this.sites = theSites;
			this.end = System.currentTimeMillis() + testDuration;
			this.stableDuration = stableDuration;
			this.shuffledSites = new LinkedList<Site>();
			this.minimumAttempts = minimumAttempts;
		}

		public synchronized boolean isEvolving() {
			return System.currentTimeMillis() - stableDuration < dateAChainWasFound;
		}

		public synchronized boolean isFinished() {
			return isEvolving() && System.currentTimeMillis() < end && attempts >= minimumAttempts;
		}

		public synchronized Collection<Site> getSeed() {
			Set<Site> result = new LinkedHashSet<>();
			while (result.size() < 3) {
				if (shuffledSites.isEmpty()) {
					shuffledSites.addAll(sites);
					Collections.shuffle(shuffledSites);
				}
				Site site = shuffledSites.removeFirst();
				if (!result.add(site)) {
					shuffledSites.add(site);
				}
			}
			return result;
		}

		public synchronized int getIteration() {
			return mainIterationIndex++;
		}

		public synchronized Chain getCurrent() {
			return currentBestChain;
		}
	}

	private static ThreadLocal<Map<Integer, Double>> calculatedDistancesBySiteDistanceKeyThLocal = new ThreadLocal<>();
	public static String[] siteTypes = new String[] { "null", "Village", "City", "Capital", "Fort", "Cave", "Ruin",
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

		public InsertionInfo(Chain chain, Link previous, Site insertion, Link next) {
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

		@Override
		public String toString() {
			return this.previous.site + "-> " + this.insertion + "->" + this.next.site + "(" + this.cost + ")";
		}

	}

	static volatile long nextReport = Long.MIN_VALUE;

	private static void report(Supplier<String> msg, boolean force) {
		if (nextReport < System.currentTimeMillis() || force) {
			nextReport = System.currentTimeMillis() + 1000l;
			Stack<String> stack = context.get(Thread.currentThread());
			LOG.info("[%s] %s", resumeStack(stack), msg.get());
			synchronized (context) {
				for (Entry<Thread, Stack<String>> x : context.entrySet()) {
					if (x.getKey() != Thread.currentThread()) {
						LOG.info("   %s [ %s ]", x.getKey().getName(), resumeStack(x.getValue()));
					}
				}
			}
		}

	}

	private static String resumeStack(Stack<String> stack) {
		if (stack == null) {
			return "";
		}
		synchronized (stack) {
			StringBuilder builder = new StringBuilder();
			if (stack.size() > 6) {

				printStack(builder, stack, 0, 3);
				builder.append("... +" + (stack.size() - 6) + "...");
				printStack(builder, stack, stack.size() - 3, 3);
			} else {
				printStack(builder, stack, 0, stack.size());
			}
			return builder.toString();
		}

	}

	private static void printStack(StringBuilder builder, Stack<String> stack, int start, int length) {
		String separator = "";
		for (int i = start; i < start + length; ++i) {
			builder.append(separator);
			builder.append(stack.get(i));
			separator = " >> ";
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

		public InsertionInfo getInsertionCost(Site s) {
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
		private int chainOptimizationSize = 8;

		/**
		 * Creates a "incomplete" chain, based on the parent chain, where there are
		 * "missing" items.
		 * 
		 * @param chain
		 * @param chance
		 * @param amount
		 * @param random @
		 */
		public Chain(Chain chain, double fraction, Random random) {
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
		 * @param site @
		 */
		public Link append(Site site) {

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
		public boolean add(Site site) {
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
				// optimize(site);
			}

		}

		/**
		 * make small local optimizations around a site.
		 * 
		 * @
		 */
		private void optimize(Site site) {
			if (linksPerSite.size() < chainOptimizationSize + 1) {
				return;
			}
			Link centralLink = linksPerSite.get(site);
			Link startLink = centralLink;
			Link previousInsertion = null;
			LinkedList<Site> originalSnippet = new LinkedList<Site>();
			Link nextInsertion = null;
			pushContext("optimize " + site.id);
			try {
				for (int i = 0; i < chainOptimizationSize / 2; ++i) {
					startLink = startLink.previous;
				}
				Link currentLink = startLink;
				previousInsertion = startLink.previous;
				while (originalSnippet.size() < chainOptimizationSize) {
					originalSnippet.add(currentLink.site);
					currentLink = currentLink.next;
				}
				nextInsertion = currentLink;

				boolean changed = false;
				LinkedList<Site> best = new LinkedList<Site>();
				LinkedList<Site> candidate = new LinkedList<Site>();
				LinkedList<Site> remaining = new LinkedList<Site>();
				best.addAll(originalSnippet);
				double bestDistance = snippetDistance(previousInsertion, originalSnippet, nextInsertion);
				int[] counters = new int[chainOptimizationSize - 1];
				counters[0] = -1;
				EVALUATE_ALL_POSSIBILITIES: while (true) {
					double candidateDistance = 0;
					candidate.clear();
					remaining.clear();
					remaining.addAll(originalSnippet);
					int i = 0;
					int max = chainOptimizationSize - 1;
					do {
						counters[i] = counters[i] + 1;
						if (counters[i] > max) {
							counters[i] = 0;
							i = i + 1;
							max = max - 1;
						} else {
							break;
						}
					} while (i < counters.length);
					if (i == counters.length) {
						break;
					}
					Site previous = previousInsertion.site;
					for (i = 0; i < counters.length; ++i) {
						Site current = remaining.remove(counters[i]);
						candidate.add(current);
						candidateDistance += previous.distance(current, true);
						if (candidateDistance > bestDistance) {
							continue EVALUATE_ALL_POSSIBILITIES;
						}
					}
					candidate.add(remaining.getFirst());
					candidateDistance += candidate.getLast().distance(nextInsertion.site, true);
					if (candidateDistance < bestDistance) {
						double repCandidateDistance = candidateDistance;
						double repBestDistance = bestDistance;
						report(() -> "found better order: from\n " + best + " to \n" + candidate
								+ "\n we have a distance economy from " + repBestDistance + " to "
								+ repCandidateDistance);
						best.clear();
						best.addAll(candidate);
						bestDistance = candidateDistance;
						changed = true;
					}
				}
				if (changed) {
					Link current = previousInsertion;
					for (Site s : best) {
						Link prev = current;
						current = linksPerSite.get(s);
						prev.next = current;
						current.previous = prev;
					}
					current.next = nextInsertion;
					nextInsertion.previous = current;
					dirty();
				}
			} finally {
				popContext();
			}

		}

		private double snippetDistance(Link previousInsertion, LinkedList<Site> snippet, Link nextInsertion) {
			Site previous = previousInsertion.site;
			double total = 0;
			for (Site x : snippet) {
				total += previous.distance(x, true);
				previous = x;
			}
			total = total + previous.distance(nextInsertion.site, true);
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
					LOG.debug("??? too big: %s", builder);
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

		public double getAbsorptionCost(Chain chain) {
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

		public void absorb(Chain chain) {
			LOG.info("ABSORBING CHAIN %s\n INTO \n%s", chain, this);
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
			LOG.info("Found the following connections to make : %s to %s and %s to %s", bestSource1, bestDestination1,
					bestSource2, bestDestination2);
			if (reverseDestination) {
				List<Link> theLinks = new ArrayList<>();
				for (Link l : chain) {
					theLinks.add(l);
				}
				Collections.reverse(theLinks);
				LOG.info("Reversing order: from %s", chain);
				for (int i = 0; i < theLinks.size(); ++i) {
					int next = (i + 1) % theLinks.size();
					int previous = (i + theLinks.size() - 1) % theLinks.size();
					theLinks.get(i).previous = theLinks.get(previous);
					theLinks.get(i).next = theLinks.get(next);
				}
				LOG.info("Reversing order: to: %s", chain);
			}
			if (bestSource1 != null) {
				dirty();
				bestSource1.next = bestDestination1;
				bestDestination1.previous = bestSource1;
				bestSource2.next = bestDestination2;
				bestDestination2.previous = bestSource2;
				this.linksPerSite.putAll(chain.linksPerSite);
				LOG.info("NEW CHAIN: %s", this);
			} else {
				LOG.info("NO SUITABLE PLACE???");
			}

		}

		public InsertionInfo getInsertionCost(Site s) {
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

		public synchronized double totalDistance() {
			if (totalDistance < 0) {
				if (!isEmpty()) {
					try {
						pushContext("totalDistance");
						double distanceRecalc = 0.0;
						for (Link x : this) {
							report(() -> "Evaluating distance for " + x.site);
							distanceRecalc += x.site.distance(x.next.site, true);
						}
						this.totalDistance = distanceRecalc;
					} finally {
						popContext();
					}
				} else {
					totalDistance = 0;
				}
			}
			return totalDistance;
		}

		public void optimize() {
			int limit = chainOptimizationSize / 3;
			int index = sr.nextInt();
			try {
				pushContext("optimize");

				for (Site s : linksPerSite.keySet()) {
					index = (index + 1) % limit;
					if (index == 0) {
						optimize(s);
					}
				}
			} finally {
				popContext();
			}
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
			cost.put(' ', 1.0);// land
			cost.put('T', 2.0);// forest
			cost.put('^', 2.0);// hills
			cost.put('~', 10000.0);// sea
			cost.put('M', 10000.0);// mountains.
			cost.put('-', 10000.0);// lakes
			cost.put('S', 1.0);// special notation to make sites consistently reachable, even if over
								// mountains. Not present in the original maps.
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	static PersistentMap calculatedDistancesBySiteDistanceKey = new PersistentMap(
			new File("D:\\SteamLibrary\\steamapps\\common\\LowMagicAge\\wlds\\wld_1_map.distances"));

	static Map<Integer, Site> siteByMapPosition = new HashMap<>();

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
			map[x][y] = 'S';
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

		public Double distance(Site other, boolean lazy) {
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
						Double theDistance;
						theDistance = calculatedDistancesBySiteDistanceKey.get(key);
						if (lazy) {
							return calculatedDistancesBySiteDistanceKey.get(key);
						} else {
							if (theDistance == null || theDistance > 10000.0) {
								try {
									pushContext("dist " + this.id + " to " + other.id);
									calculatedDistancesBySiteDistanceKey.put(key, PersistentMap.RESERVED);
									theDistance = dist(this.x, this.y, other.x, other.y, 0.0, 0.0, null);
									calculatedDistancesBySiteDistanceKey.put(key, theDistance);
									LOG.info("ADDED NEW DISTANCE TO CHART: Dist %s to %s = %6.2f", this, other,
											theDistance);
								} finally {
									popContext();
								}
								return calculatedDistancesBySiteDistanceKey.get(key);
							} else if (theDistance == PersistentMap.RESERVED) {
								return (Double.MAX_VALUE);
							} else {
								return (theDistance);
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

	static double dist(int x, int y, int x2, int y2, double soFar, double acceptableMax, double[][] distanceTable) {
		boolean pushContext = false;
		double linearDistance = linearDistance(x, y, x2, y2);

		if (acceptableMax == 0) {
			acceptableMax = (128.0 + 64.0) * 2;
			pushContext = true;
		}
		try {
			if (pushContext) {
				pushContext("dist " + x + "," + y + "," + x2 + "," + y2);

			}
			double[][] distanceTable2 = distanceTable;
			double soFar2= soFar;
			double acceptableMax2 = acceptableMax;
			report(() -> {
				StringBuilder builder = new StringBuilder();
				builder.append(String.format("DIST %d,%d %d,%d %5.2f %5.2f",x,y,x2,y2,soFar2,acceptableMax2));
				int mapStartX = Integer.min(x, x2);
				mapStartX = Integer.max(0, mapStartX - 5);
				int mapEndX = Integer.max(x, x2);
				mapEndX = Integer.min(127, mapEndX + 5);
				int mapStartY = Integer.min(y, y2);
				mapStartY = Integer.max(0, mapStartY - 5);
				int mapEndY = Integer.max(y, y2);
				mapEndY = Integer.min(63, mapEndY + 5);
				builder.append("\n====\n");
				for (int i = mapStartY; i <= mapEndY; ++i) {
					for (int j = mapStartX; j < mapEndX; ++j) {
						if (j == x && i == y) {
							builder.append("X");
						} else if (j == x2 && i == y2) {
							builder.append("Y");
						} else {
							builder.append(map[j][i]);
						}
					}
					builder.append("\n");
				}
				if(distanceTable2 != null) {
					builder.append("====\n");
					int max = Integer.parseInt("zz",Character.MAX_RADIX);
					for (int i = mapStartY; i <= mapEndY; ++i) {
						for (int j = mapStartX; j < mapEndX; ++j) {
							double thisDistance= distanceTable2[j][i];
							String thisDistanceValue;
							if(thisDistance > max) {
								thisDistanceValue="zz";
							} else if(thisDistance==0) {
								thisDistanceValue="..";
							} else {
								thisDistanceValue = "00"+Integer.toString((int)(thisDistance + 0.5), Character.MAX_RADIX);
								thisDistanceValue = thisDistanceValue.substring(thisDistanceValue.length()-2);
							}
							builder.append(thisDistanceValue + " ");
						}
						builder.append("\n");
					}
					
				}
				return builder.toString();

			});
			if (distanceTable == null) {
				distanceTable = new double[128][64];
			}
			if (distanceTable[x][y] == 0) { // 0 means not initialized.
				soFar = soFar + 0.00000001;
				distanceTable[x][y] = soFar;
			}
			if (distanceTable[x2][y2] == 0) {
				distanceTable[x2][y2] = Double.MAX_VALUE;
			}
			if (soFar + linearDistance > acceptableMax) {
				return distanceTable[x2][y2];
			}

			if (distanceTable[x][y] != 0 && distanceTable[x][y] < soFar) {
				return distanceTable[x2][y2];
			}

			if (distanceTable[x][y] < soFar) {
				return distanceTable[x2][y2];
			} else {
				distanceTable[x][y] = soFar;
			}
			if (x == x2 && y == y2) {
				return distanceTable[x2][y2];
			}
			if (distanceTable[x2][y2] <= soFar + linearDistance(x, y, x2, y2)) {
				return distanceTable[x2][y2];
			}

			Site s1;
			Site s2;
			s1 = siteByMapPosition.get(mapindex(x, y));
			s2 = siteByMapPosition.get(mapindex(x2, y2));

			if (s1 != null && s2 != null) {
				Double lazyDistance = s1.distance(s2, true);
				if (lazyDistance != null && lazyDistance != PersistentMap.RESERVED && lazyDistance <= acceptableMax) {
					double result = soFar + lazyDistance;
					result = Double.min(distanceTable[x2][y2], result);
					distanceTable[x2][y2] = result;
					return result;
				}
			}
			/*
			 * In this case, we will try a direct route. If it is found, consider the issue
			 * solved.
			 */
			double directRoute = tryDirectRoute(x, y, x2, y2);
			if (directRoute < Double.MAX_VALUE) {
				double result = soFar + directRoute;
				result = Math.min(result, distanceTable[x2][y2]);
				distanceTable[x2][y2] = result;
				return result;
			}
			TreeSet<int[]> deltas = new TreeSet<>((d1, d2) -> {
				Function<int[], Double> estimateDistance = (i) -> {
					int x1 = i[0] + x;
					int y1 = i[1] + y;

					return moveCost1Square(x, y, x1, y1) + linearDistance(x1, y1, x2, y2);
				};
				int result = (int) Math.signum(estimateDistance.apply(d1) - estimateDistance.apply(d2));
				if (result == 0) {
					result = d1[0] - d2[0];
				}
				if (result == 0) {
					result = d1[1] - d2[1];
				}
				return result;
			}

			);

			for (int i = -1; i <= 1; ++i) {
				for (int j = -1; j <= 1; ++j) {
					deltas.add(new int[] { i, j });
				}
			}

			double bestDistance = distanceTable[x2][y2];
			for (int[] d : deltas) {
				int ix = d[0];
				int iy = d[1];
				if (ix == 0 && iy == 0) {
					continue;
				}
				int nextY = y + iy;
				if (nextY < 0 || nextY > 63) {
					continue;
				}
				int nextX = x + ix;
				if (nextX < 0 || nextX > 127) {
					continue;
				}
				bestDistance = Math.min(distanceTable[x2][y2], bestDistance);
				acceptableMax = Math.min(bestDistance, acceptableMax);
				double moveCost = moveCost1Square(x, y, nextX, nextY);
				double totalCost = soFar + moveCost;
				double existingCost = distanceTable[nextX][nextY];
				if (existingCost == 0) {
					existingCost = Double.MAX_VALUE;
					distanceTable[nextX][nextY] = totalCost;
				}
				if (totalCost >= existingCost) {
					continue;
				}
				if (totalCost >= acceptableMax) {
					continue;
				}
				distanceTable[nextX][nextY] = totalCost;
				if (bestDistance <= totalCost + linearDistance(nextX, nextY, x2, y2)) {
					continue;
				}
				try {
					pushContext("Try " + nextX + "," + nextY + "," + totalCost + "," + acceptableMax);
					bestDistance = Math.min(bestDistance,
							dist(nextX, nextY, x2, y2, totalCost, acceptableMax, distanceTable));
				} finally {
					popContext();
				}
				distanceTable[x2][y2] = Math.min(distanceTable[x2][y2], bestDistance);

			}
			return distanceTable[x2][y2];
		} finally {
			if (pushContext) {
				popContext();
			}
		}
	}

	/**
	 * Returns the direct route cost if it is recognizably cheaper using the map
	 * information.
	 * 
	 * @param x
	 * @param y
	 * @param x2
	 * @param y2
	 * @return
	 */
	private static double tryDirectRoute(int x, int y, int x2, int y2) {
		double acceptableMin = linearDistance(x, y, x2, y2);

		int ixdiag = (int) Math.signum(x2 - x);
		int iydiag = (int) Math.signum(y2 - y);

		int dx = (x2 - x);
		int dy = (y2 - y);
		int absdx = Math.abs(dx);
		int absdy = Math.abs(dy);
		int max = Math.max(absdx, absdy);
		int min = Math.min(absdx, absdy);
		int diagonals = min;
		int unidirection = max - min;
		int uniddx, uniddy;
		if (absdx > absdy) {
			uniddx = ixdiag;
			uniddy = 0;
		} else {
			uniddx = 0;
			uniddy = iydiag;
		}
		int maxDiagonals = diagonals;
		boolean found;
		NEXT_INITIALDIAGONAL_CHECK: for (int initialDiagonals = maxDiagonals; initialDiagonals >= 0; --initialDiagonals) {
			int x0 = x;
			int y0 = y;
			int initialDiagonalRemaining = initialDiagonals;
			int totalDiagonalsRemaining = diagonals;
			int unidirectionLeft = unidirection;
			DIAGONAL_CHECK: while (initialDiagonalRemaining > 0) {
				int x1 = x0 + ixdiag;
				int y1 = y0 + iydiag;
				double cost = moveCost1Square(x0, y0, x1, y1);
				if (cost > 1.5) {
					initialDiagonals = maxDiagonals - initialDiagonalRemaining;
					break DIAGONAL_CHECK;
				} else {
					x0 = x1;
					y0 = y1;
					initialDiagonalRemaining--;
					totalDiagonalsRemaining--;
				}
			}
			while (x0 != x2 && y0 != y2) {
				int x1, y1;
				if (unidirectionLeft > 0) {
					x1 = x0 + uniddx;
					y1 = y0 + uniddy;
					if (moveCost1Square(x0, y0, x1, y1) < 1.5) {
						--unidirectionLeft;
						x0 = x1;
						y0 = y1;
					} else {
						if (totalDiagonalsRemaining > 0) {
							x1 = x0 + ixdiag;
							y1 = y0 + iydiag;
							if (moveCost1Square(x0, y0, x1, y1) < 1.5) {
								--totalDiagonalsRemaining;
								x0 = x1;
								y0 = y1;
							} else {
								continue NEXT_INITIALDIAGONAL_CHECK;
							}
						} else {
							continue NEXT_INITIALDIAGONAL_CHECK;
						}
					}
				} else {
					if (totalDiagonalsRemaining > 0) {
						x1 = x0 + ixdiag;
						y1 = y0 + iydiag;
						if (moveCost1Square(x0, y0, x1, y1) < 1.5) {
							--totalDiagonalsRemaining;
							x0 = x1;
							y0 = y1;
						} else {
							continue NEXT_INITIALDIAGONAL_CHECK;
						}
					} else {
						continue NEXT_INITIALDIAGONAL_CHECK;
					}
				}
			}
			if (x0 == x2 && y0 == y2) {
				return acceptableMin;
			} else {
				return Double.MAX_VALUE;
			}
		}
		return Double.MAX_VALUE;

	}

	/**
	 * THis is the best distance that is possible, considering that only movement in
	 * the horizontal or diagonal are allowed, considering all costs = 1.
	 * 
	 * @param x
	 * @param y
	 * @param x2
	 * @param y2
	 * @return
	 */
	private static double linearDistance(int x, int y, int x2, int y2) {
		int dx = Math.abs(x2 - x);
		int dy = Math.abs(y2 - y);
		int max = Math.max(dx, dy);
		int min = Math.min(dx, dy);
		return ((max - min) * 1.0 + (min) * 1.41) * (1 + 1 / 128.0 / 2); // adding a little noise so any rounding errors
																			// can be considered.

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
			Collections.shuffle(sites, sr);
			/* This is a sampling routine : remove it if you want the full map */
			// sample(sites,40);
			do {
				int distance2 = 31;
				int x1 = sr.nextInt(128 - distance2) + distance2 / 2;
				int y1 = sr.nextInt(64 - distance2) + distance2 / 2;
				int x2 = x1 + sr.nextInt(distance2) - distance2 / 2;
				int y2 = y1 + sr.nextInt(distance2) - distance2 / 2;
				if (cost.get(map[x1][y1]) < 10 && cost.get(map[x2][y2]) < 10) {
					LOG.info("TEST: Distance from (%d,%d) to (%d,%d) = %6.2f", x1, y1, x2, y2,
							dist(x1, y1, x2, y2, 0.0, 0.0, null));
					break;
				}
			} while (true);
			LOG.warn("Calculating ALL distances");
			TreeSet<Distance> allDistances = new TreeSet<>();
			for (int i = 0; i < sites.size() - 1; ++i) {
				for (int j = i + 1; j < sites.size(); ++j) {
					allDistances.add(new Distance(sites.get(i), sites.get(j)));
				}
			}
			Queue<Distance> queue = new LinkedList<>();
			queue.addAll(allDistances);
			int initialSize = queue.size();
			long start = System.currentTimeMillis();
			Runnable calculateDistance = () -> {
				int officialCount;
				long localStart = System.currentTimeMillis();

				while (true) {
					Distance x;
					synchronized (queue) {
						if (queue.size() > 0) {
							x = queue.poll();
							officialCount = initialSize - queue.size();
						} else {
							break;
						}
					}

					long thisOpStart = System.currentTimeMillis();
					double distance = x.start.distance(x.end, false);
					LOG.info("ALLDIST: took %d ms to calculate the Distance %s to %s = %6.2f",
							System.currentTimeMillis() - thisOpStart, x.start, x.end, distance);
					if (officialCount % 20 == 0) {
						long elapsed = System.currentTimeMillis() - start;

						double processingTime = elapsed * 1.0 / officialCount;
						long extraTime = (long) (processingTime * queue.size());
						Date date = new Date(System.currentTimeMillis() + extraTime);
						LOG.info("ALLDIST: AT THE CURRENT RATE: finish will be expected by %s",
								() -> new SimpleDateFormat("MM/dd HH:mm:ss").format(date));
						try {
							calculatedDistancesBySiteDistanceKey.persist();
						} catch (Exception ex) {
							LOG.error(ex);
							return;
						}
					}
				}
			};
			Thread[] distanceThreads = new Thread[Runtime.getRuntime().availableProcessors()];
			for (int i = 0; i < distanceThreads.length; ++i) {
				distanceThreads[i] = new Thread(calculateDistance);
				distanceThreads[i].setName("dt" + i);
				distanceThreads[i].start();
			}
			for (int i = 0; i < distanceThreads.length; ++i) {
				distanceThreads[i].join();
			}

			calculatedDistancesBySiteDistanceKey.persist();
			ChainCandidate bestChain = new ChainCandidate(sites, 1800000, 300000, sites.size() / 30);

			Runnable runnable = () -> findSiteInParallel(bestChain);
			Thread[] th = new Thread[Runtime.getRuntime().availableProcessors()];
			for (int i = 0; i < th.length; ++i) {
				th[i] = new Thread(runnable);
				th[i].setName("fsp" + i);
				th[i].start();
			}
			for (int i = 0; i < th.length; ++i) {
				th[i].join();
			}
			OUT.info("The best chain is:\n");
			for (Link x : bestChain.currentBestChain) {
				OUT.info(x.site.name + " (" + x.site.x + "," + x.site.y + ")");
			}

			System.out.println("The best chain is:\n=====\n" + bestChain.currentBestChain + "\n-----\nTotal distance "
					+ bestChain.currentBestChain.totalDistance());

		} finally {
			report(() -> "Final stack status", true);
		}

	}

	private static void report(Supplier<String> supplier) {
		report(supplier, false);

	}

	/**
	 * Gets the sites that are closer to a random site.
	 * 
	 * @param sites  a site list. it will be tampered with.
	 * @param sample
	 */
	private static void sample(List<Site> sites, int sample) {
		Collections.shuffle(sites, sr);
		Site center = sites.get(0);
		TreeSet<Site> chosen = new TreeSet<>((s1, s2) -> {
			int result = (int) Math.signum(s1.distance(center, false) - s2.distance(center, false));
			if (result == 0) {
				result = s1.compareTo(s2);
			}
			return result;
		});
		for (Site s : sites) {
			chosen.add(s);
			if (chosen.size() > sample) {
				chosen.remove(chosen.last());
			}
		}
		sites.clear();
		sites.addAll(chosen);
		System.out.println("Sample:" + sites);
	}

	private static void findSiteInParallel(ChainCandidate bestChain) {
		try {
			Random random = new Random(sr.nextLong());
			pushContext("findsite");
			Map<Integer, Double> theThreadLocalMap = new HashMap<>();
			for (Entry<Integer, Double> x : calculatedDistancesBySiteDistanceKey.entrySet()) {
				theThreadLocalMap.put(x.getKey(), x.getValue());
			}
			double bestDistance = Double.MAX_VALUE;
			calculatedDistancesBySiteDistanceKeyThLocal.set(theThreadLocalMap);
			List<Site> thisIterationShuffle = new ArrayList<>();
			Set<Site> thisIterationSites = new LinkedHashSet<Site>();
			Chain chain = null;
			while (!bestChain.isFinished()) {
				int thisIterationNumber = bestChain.getIteration();
				pushContext("longwhile" + thisIterationNumber);
				report(() -> "");
				try {
					System.out.println("at iteration " + thisIterationNumber);
					thisIterationShuffle.addAll(bestChain.sites);
					Collections.shuffle(thisIterationShuffle, random);
					thisIterationSites.addAll(thisIterationShuffle);
					chain = new Chain();
					for (Site s : bestChain.getSeed()) {
						chain.append(s);
						thisIterationSites.remove(s);
					}
					System.out.println(
							"Starting iterationNumber " + thisIterationNumber + " by new random chain " + chain);
					double currentDistance = chain.totalDistance();
					if (currentDistance > bestDistance && currentDistance < Double.MAX_VALUE) {
						chain.optimize();
						currentDistance = chain.totalDistance();
						if (currentDistance > bestDistance) {
							System.out.println(thisIterationNumber + " iteration skipped: Current distance "
									+ chain.totalDistance() + " > best distance " + bestDistance);
							chain = null;
						}
					}

					boolean isRepeating = false;
					while (!thisIterationSites.isEmpty() && !isRepeating && chain != null) {
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

							report(() -> " the chain so far: " + chain2.linksPerSite.size() + " entries best is "
									+ best[0] + " remains " + thisIterationSites.size());
						}
						report(() -> "FINAL OUTCOME: the chain so far: " + chain2.linksPerSite.size()
								+ "entries best is " + best[0] + " remains " + thisIterationSites.size());

						if (best[0] != null) {
							Link result = best[0].execute();
							if (chain.totalDistance() > bestDistance && chain.totalDistance() < Double.MAX_VALUE) {
								chain.optimize();
								if (chain.totalDistance() > bestDistance) {
									System.out
											.println("Bypassing this evaluation: totalDistance " + chain.totalDistance()
													+ " is already bigger than the best distance " + bestDistance);
									chain = null;
									break;
								}
							}

							if (result != null) {
								thisIterationSites.remove(result.site);
							} else {
								LOG.info("Couldn't execute %s", best[0]);
							}
						} else {
							synchronized (MapSpiral.class) {
								LOG.info("Site left: %s", thisIterationSites);
								isRepeating = true;

								for (Site s : thisIterationSites) {
									for (Site s2 : bestChain.sites) {
										LOG.info("Site: %s Distance %f LinearDistance %f", s2, s.distance(s2, true),
												s.linearDistance(s2));
									}
								}
								return;
							}

						}
					}
					if (chain != null) {
						chain.optimize();
					}
					if (chain != null) {
						Chain oldChain = bestChain.getCurrent();
						Chain bestChainFound = bestChain.best(chain);
						bestDistance = bestChainFound.totalDistance();
						if (bestChainFound != oldChain && bestChainFound != null) {
							LOG.info("FOUND A BETTER CHAIN!\n====\n%s\n----", bestChain.currentBestChain);
						}
						LOG.info("iteration #: %d", thisIterationNumber);
						LOG.info("Old chain length: %6.2f", (oldChain != null ? oldChain.totalDistance() : Double.NaN));
						LOG.info("Best chain length: %6.2f", bestChain.currentBestChain.totalDistance());
						LOG.info("This chain length: %6.2f", chain.totalDistance());
						LOG.info("This chain items: %d", chain.linksPerSite.size());
					}

					thisIterationSites.clear();
					thisIterationShuffle.clear();
					chain = null;

				} finally {
					popContext();
				}
			}
		} finally {
			popContext();
		}
	}
}
