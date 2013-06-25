/**
 * Command line knapsack problem solver
 * <p>
 * Usage: <code>java Knapsack &lt; inputfile.txt</code>
 * <p>
 * Dependencies: StdIn.java (by Sedgwick and Wayne)
 * <p>
 * This program (or library -- there is a Java-native constructor available) takes as
 * input <var>n</var>-length vectors <var>v</var> of values and <var>w</var> of weights,
 * all integers, as well as an integer <var>k</var> capacity. Then it finds the vector
 * <var>x</var> of length <var>n</var> whose elements are in {0, 1} such that <var>x</var>
 * maximizes <br />
 *            $$ sum^{n-1}_{i=0}{v_i * x_i} $$
 * subject to <br />
 *            $$ sum^{n-1}_{i=0}{w_i * x_i} \le k $$
 * and makes the values of <var>x</var> available through the <code>item</code> method.
 *
 * @author William Schwartz
 */

public class Knapsack {
	private final boolean optimal; // whether solution proved optimal
	private final int n, k; // n: number of items; k: weight capacity
	private final int[] v, w, x; // v: values; w: weights; x: decisions;

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
		x = new int[n];
		optimal = false;
		// Defensive copy
		this.v = new int[n];
		this.w = new int[n];
		for (int i = 0; i < n; i++) {
			this.v[i] = v[i];
			this.w[i] = w[i];
		}
		fill();
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

	/**
	 * Return true if the solution is feasible.
	 */
	public boolean feasible() {
		int weight = 0;
		for (int i = 0; i < n; i++)
			weight += w[i] * x[i];
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
		if (optimal())
			s.append(1);
		else
			s.append(0);
		s.append('\n');
		for (int i = 0; i < n; i++) {
			if (item(i))
				s.append(1);
			else
				s.append(0);
			if (i < n - 1)
				s.append(' ');
		}
		return s.toString();
	}

	/**
	 * Fill the knapsack as optimally as possible. Results stored in instance
	 * variables <code>x</code> and <code>optimal</code>.
	 */
	private void fill() {
		throw new UnsupportedOperationException("not implemented yet");
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
