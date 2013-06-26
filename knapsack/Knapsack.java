/**
 * Command line knapsack problem solver
 * <p>
 * Usage: <code>java Knapsack &lt; inputfile.txt</code>
 * <p>
 * Dependencies: StdIn.java (by Sedgwick and Wayne)
 * <p>
 * This program (or library -- there is a Java-native constructor available)
 * takes as input <var>n</var>-length vectors <var>v</var> of values and
 * <var>w</var> of weights, all integers, as well as an integer <var>k</var>
 * capacity. Then it finds the vector <var>x</var> of length <var>n</var> whose
 * elements are in {0, 1} such that <var>x</var> maximizes <br>
 *            $$ sum^{n-1}_{i=0}{v_i * x_i} $$ <br>
 * subject to <br>
 *            $$ sum^{n-1}_{i=0}{w_i * x_i} \le k $$ <br>
 * and makes the values of <var>x</var> available through the <code>item</code>
 * method.
 *
 * @author William Schwartz
 */

import java.util.Arrays;

public class Knapsack {
	private final int n, k; // n: number of items; k: weight capacity
	private final int[] v, w, x; // v: values; w: weights; x: decisions
	private boolean optimal; // whether solution proved optimal

	/**
	 * Initialize a new knapsack and fill it optimally. Note that parameters
	 * <code>v</code> and <code>w</code> must be the same length.
	 *
	 * @param k integer capacity
	 * @param v integer array of values for each item, length at least 1
	 * @param w integer array of weights for each item, length at least 1
	 */
	public Knapsack(int k, int[] v, int[] w) {
		if (v.length != w.length)
			throw new IllegalArgumentException("ambiguous problem size");
		n = v.length;
		if (n < 1)
			throw new IllegalArgumentException("no items to put in knapsack");
		this.k = k;
		this.v = new int[n];
		this.w = new int[n];
		System.arraycopy(v, 0, this.v, 0, n);
		System.arraycopy(w, 0, this.w, 0, n);
		x = new int[n];
		optimal = false;
		fill(); // sets x and possibly flips optimal
		assert feasible();
	}

	/**
	 * Return true if item <code>i</code> is in the solution.
	 *
	 * @param i the index of the decision variable being queried
	 */
	public boolean item(int i) {
		return x[i] == 1;
	}

	/**
	 * Return problem size
	 */
	public int size() { return n; }

	/**
	 * Return the value of the objective function given the solution.
	 */
	public int objective() {
		int value = 0;
		for (int i = 0; i < n; i++)
			value += v[i] * x[i];
		return value;
	}

	/**
	 * Return true if the algorithm proved that the solution was optimal.
	 */
	public boolean optimal() {
		return optimal;
	}

	// Assertions that the soluiton is feasible. Only called by assert. Return
	// value is useless.
	private boolean feasible() {
		int weight = 0;
		for (int i = 0; i < n; i++) {
			assert x[i] == 0 || x[i] == 1;
			weight += w[i] * x[i];
		}
		return weight <= k;
	}

	/**
	 * Return a string containing the answer as the class grading tool expects
	 * it.
	 * <p>
	 * The first line contains an integer objective-function value followed by a
	 * <code>1</code> if the value is optimal or a <code>0</code> otherwise. The
	 * next line is a space separated list of zeros and ones for each decision
	 * variable in order.
	 */
	public String classOut() {
		StringBuilder s = new StringBuilder();
		s.append(objective());
		s.append(' ');
		s.append(optimal() ? 1 : 0);
		s.append('\n');
		for (int i = 0; i < n; i++) {
			s.append(item(i) ? 1 : 0);
			s.append(i < n - 1 ? ' ' : '\n');
		}
		return s.toString();
	}

	private class Item implements Comparable {
		private double ratio;
		private int i;
		public Item(int i) {
			this.i = i;
			ratio = (double) v[i] / w[i];
		}
		public int i() { return i; }
		public int compareTo(Object o) {
			if (o == null)
				throw new NullPointerException("cannot compare to null");
			Item other = (Item) o;
			if      (ratio <  other.ratio) return -1;
			else if (ratio == other.ratio) return  0;
			else                           return  1;
		}
	}

	/**
	 * Fill the knapsack as optimally as possible. Results stored in instance
	 * variables <code>x</code> and <code>optimal</code>.
	 */
	private void fill() {
		Item[] items = new Item[n];
		int[] t = new int[n];
		for (int i = 0; i < n; i++) {
			t[i] = -1;
			items[i] = new Item(i);
		}
		Arrays.sort(items);
		t[0] = 0;
		int best = branch(t, 0, 0, 0, 0, items);
		t[0] = 1;
		best = branch(t, 0, 0, 0, best, items);
		optimal = objective() == best;
	}

	// Return an upper bound for the value at the best leaf of this search node.
	// t is the decision vector being tested, i is the row in the decision tree
	// (which item is being tested for in vs. out status), and items is a sorted
	// array of Item objects.
	private double bound(int[] t, int i, double weight, double value, Item[] items) {
		int item, wi;
		double fraction;
		for (int j = n - 1; j >= 0; j--) {
			item = items[j].i();
			if (item <= i)
				continue;
			wi = w[item];
			fraction = Math.min(1.0, (double) (k - weight) / wi);
			weight += fraction * wi;
			if (weight < k)
				value += fraction * v[item];
			else break;
		}
		return value;
	}

	private int branch(int[] t, int i, int prevValue, int prevWeight, int best,
					   Item[] items)
	{
		assert i < n && (t[i] == 0 || t[i] == 1);
		int weight = prevWeight + w[i] * t[i];
		if (weight > k)
			return best;
		int value = prevValue + v[i] * t[i];
		if (bound(t, i, weight, value, items) < best)
			return best;
		int newBest = Math.max(value, best);
		if (i < n - 1) {
			t[i + 1] = 0;
			newBest = Math.max(branch(t, i + 1, value, weight, newBest, items),
							   newBest);
			t[i + 1] = 1;
			newBest = Math.max(branch(t, i + 1, value, weight, newBest, items),
							   newBest);
			t[i + 1] = -1; // So we return t to caller like we found it
		}
		// Must include equality in case best came from higher up the branch
		else if (value >= best)
			System.arraycopy(t, 0, x, 0, n);
		return newBest;
	}

	/**
	 * Command line interface for the knapsack solver. Uses the class's input
	 * and output formats on stdin and stdout.
	 * <p>
	 * The input format is as follows. First line, line -1, is two integers:
	 * <code><var>n</var> <var>k</var></code>. Line <var>i</var> for
	 * <var>i</var> 0 through <var>n</var> - 1 is <code><var>v</var><sub>i</sub>
	 * <var>w</var><sub>i</sub></code>.
	 */
	public static void main(String[] args) {
		if (args.length != 0) {
			String msg = "unexpected arguments; pass input on stdin";
			throw new IllegalArgumentException(msg);
		}
		int n = StdIn.readInt();
		int k = StdIn.readInt();
		int[] v = new int[n];
		int[] w = new int[n];
		for (int i = 0; i < n; i++) {
			v[i] = StdIn.readInt();
			w[i] = StdIn.readInt();
		}
		Knapsack knapsack = new Knapsack(k, v, w);
		System.out.print(knapsack.classOut());
	}
}
