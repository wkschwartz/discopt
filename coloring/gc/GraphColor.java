package gc;

import java.util.Scanner;
import java.util.BitSet;
import java.util.LinkedList;
import java.io.IOException;
import edu.princeton.cs.algs4.Graph;

public class GraphColor {

	private final DegreeSortedGraph g;
	private final int V;
	private final int[] colors;

	public GraphColor(Graph g) {
		this.g = new DegreeSortedGraph(g);
		V = g.V();
		colors = solve();
		assert feasible();
	}

	/** Return true if colors obeys g's edge constraints. */
	private boolean feasible() {
		for (int v = 0; v < V; v++)
			for (int w : g.adj(v))
				if (colors[v] == colors[w])
					return false;
		return true;
	}

	/** Return the color of vertex v, or -1 if no solution exists. */
	public int color(int v) { return colors[g.g2dsg(v)]; }

	/** Return the maximum color used */
	public int maxColor() {
		int max = -1;
		for (int v = 0; v < V; v++)
			max = Math.max(max, color(v));
		return max;
	}

	/** Return whether the algorithm proved optimality. */
	public boolean optimal() {
		return color(0) >= 0; // Only reason for non-optimality is infeasibility
	}

	/**
	 * Logical deductions from constraints added via setColor().
	 * <p>
	 * Enforces three constraints: <ul>
	 * <li>all colors are integers in the interval [0, V)
	 * <li>neighboring edges cannot share a color
	 * <li>if the colors are in a V-length slice, then
	 *     <blockquote><code>color[v] <= max(colorv[:v]) + 1</code></blockquote>
	 *</ul>
	 */
	private class SearchNode {

		// A set of possible colors for each node
		private final BitSet[] domain;
		// cumulative maximum: cumm[i] = max(max(domain[0]), ..., max(domain[i]))
		private final int[] cumm;
		// count of solved nodes. -1 when infeasible.
		private int count;

		/** Instantiate a new search node. */
		public SearchNode() {
			domain = new BitSet[V];
			cumm = new int[V];
			for (int i = 0; i < V; i++) {
				domain[i] = new BitSet(V);
				domain[i].set(0, V); // Second arg of set() is exclusive
				cumm[i] = V - 1;
			}
			count = 0;
		}

		/** Copy constructor */
		public SearchNode(SearchNode old) {
			domain = new BitSet[V];
			cumm = new int[V];
			count = old.count;
			System.arraycopy(old.cumm, 0, cumm, 0, V);
			for (int i = 0; i < V; i++)
				domain[i] = (BitSet) old.domain[i].clone();
		}

		/** Return true if setting node v to color obeys the edge constraint. */
		private boolean edgeConstrained(int v, int color) {
			for (int w : g.adj(v))
				if (domain[w].cardinality() == 1 && domain[w].get(color))
					return false;
			return true;
		}

		/**
		 * Return a lower bound the maximum color used based on relaxing
		 * constraints.
		 */
		public int bound() {
			int max = -1;
			for (int v = 0; v < V; v++)
				if (domain[v].cardinality() == 1)
					max = Math.max(domain[v].nextSetBit(0), max);
			return max;
		}

		/**
		 * Return true if the solution is complete and feasible.
		 */
		public boolean solved() { return count == V; }

		/**
		 * Return array of the colors found or -1s if the solution is either
		 * infeasible or not yet found.
		 */
		public int[] solution() {
			int[] a = new int[V];
			if (solved())
				for (int v = 0; v < V; v++)
					a[v] = domain[v].nextSetBit(0);
			else
				for (int v = 0; v < V; v++)
					a[v] = -1;
			return a;
		}

		/**
		 * Return the value of the maximum color used.
		 */
		public int maxColor() { return cumm[V - 1]; }

		/** Return iterable of feasible colors less than max for node v if it
		 * doesn't have a color already.
		 */
		public Iterable<Integer> nextColors(int v, int max) {
			LinkedList<Integer> cs = new LinkedList<Integer>();
			BitSet dv = domain[v];
			if (dv.cardinality() > 1)
				for (int c=dv.nextSetBit(0);c>=0 && c< max;c=dv.nextSetBit(c+1))
					cs.add(c);
			return cs;
		}

		/**
		 * Add the constraint that vertex <code>v</code> is a given color.
		 *
		 * @return <code>false</code> if such a constraint causes
		 * the solution to become infeasible.
		 */
		public boolean setColor(int v, int color) {
			boolean success;

			assert v >= 0 && color >= 0 && v < V && color < V;
			assert domain[v].get(color); // Color is in v's domain
			assert edgeConstrained(v, color); //Color obeys v's edge constraints
			assert v == 0 || color <= cumm[v - 1] + 1; // Symmetry constraint
			assert v == 0 || color <= cumm[v]; // Consequence of the above

			if (color == 0)
				success = ruleOut(v, color + 1, V);
			else if (color == V - 1)
				success = ruleOut(v, 0, V - 1);
			else
				success = ruleOut(v, 0, color) && ruleOut(v, color + 1, V);
			assert !success || domain[v].cardinality() == 1 && domain[v].get(color) :
			"v=" + v + ", color=" + color + " domain=" + domain[v].toString();
			return success;
		}

		/**
		 * Rule out that a vertex <code>v</code> has any color from
		 * <code>fromColor</code> (inclusive) to <code>toColor</code>
		 * (exclusive).
		 *
		 * @return <code>false</code> if such a constraint causes
		 * the solution to become infeasible.
		 */
		private boolean ruleOut(int v, int fromColor, int toColor) {
			int oldCard = domain[v].cardinality(), newMax;
			boolean vFell, v_1Fell;
			assert v >= 0 && fromColor >= 0 && toColor > fromColor &&
				v < V && toColor <= V;
			assert oldCard >= 1;

			domain[v].clear(fromColor, toColor);
			newMax = domain[v].length() - 1;
			assert v == 0 || newMax <= cumm[v - 1] + 1;

			if (newMax < 0) { // domain[v] is empty: constraint is infeasible.
				count = -1;
				return false;
			}
			// Reestablish the invariant defining cumm[] before any more
			// recursive calls, and propogate the symmetry breaking constraint
			// cumm[] is for. The conditional below allows propogation when the
			// binding constraint on cumm[v+1] falls: either if v's max fell or
			// if cumm[v-1] fell.
			vFell = (v == 0 || cumm[v] > cumm[v - 1]) && cumm[v] > newMax;
			v_1Fell = v > 0 && v < V - 1 && cumm[v] == cumm[v - 1] &&
				cumm[v + 1] > cumm[v - 1] + 1;
			if (vFell || v_1Fell) {
				cumm[v] = v == 0 ? newMax : Math.max(cumm[v - 1], newMax);
				if (v < V - 1) {
					if (cumm[v] + 2 < V && !ruleOut(v + 1, cumm[v] + 2, V))
						return false;
				}
			}
			assert v == 0 || cumm[v] == Math.max(cumm[v - 1], domain[v].length() - 1);
			// Propogate the edge constraint now that v has become the color
			// newMax
			if (oldCard > 1 && domain[v].cardinality() == 1) {
				assert newMax >= 0;
				count++; // newMax is v's color now.
				for (int w: g.adj(v))
					if (!ruleOut(w, newMax, newMax + 1))
						return false;
			}
			return true;
		}
	}

	private int[] solve() {
		SearchNode s;
		// Symmetry constraint: color[start] = 0
		s = branch(new SearchNode(), 0, 0, null);
		if (s != null)
			return s.solution();
		else {
			int[] cs = new int[V];
			for (int v = 0; v < V; v++)
				cs[v] = -1;
			return cs;
		}
	}

	/** Branch and bound. */
	private SearchNode branch(SearchNode s, int v, int color, SearchNode best) {
		SearchNode newBest, nextTry;
		assert s != null && v >= 0 && color >= 0 && v < V && color < V;
		assert best != null || v == 0 && color == 0; // best only null if first call.
		if (!s.setColor(v, color))
			return null;
		if (best != null && s.bound() > best.maxColor())
			return best;
		if (best == null || s.maxColor() < best.maxColor())
			newBest = s;
		else
			newBest = best;
		if (!newBest.solved()) {
			for (int w = v; w < V; w++) {
				for(int nextColor : s.nextColors(w, newBest.maxColor())) {
					nextTry = branch(new SearchNode(s), w, nextColor, newBest);
					if (nextTry != null && nextTry.maxColor() < newBest.maxColor())
						newBest = nextTry;
				}
			}
		}
		return newBest;
	}

	/**
	 * Return a string represenation of the solution.
	 * <p>
	 * The first line is the optimal objective value, a space, and 1 if the
	 * soluiton is optimal and 0 if it isn't. The next line is a space separated
	 * list of integers representing the colors of the input nodes.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(maxColor());
		sb.append(' ');
		sb.append(optimal() ? 1 : 0);
		sb.append(System.getProperty("line.separator"));
		for (int v = 0; v < V; v++) {
			sb.append(color(v));
			if (v < V - 1)
				sb.append(' ');
		}
		return sb.toString();
	}

	/**
	 * Read a graph from stdin and write the solution to stdout.
	 * <p>
	 * The graph should be written with the first line giving the number of
	 * vertices, a space, and the number of edges. Each subsequent line should
	 * describe an edge by giving the adjoined vertices' indices separated by a
	 * space.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			System.err.println("unkown arguments found; pass data in on stdin");
			System.exit(1);
		}
		Scanner s = new Scanner(System.in, "UTF-8");
		Graph g = new Graph(s.nextInt());
		int E = s.nextInt();
		while (s.hasNextInt())
			g.addEdge(s.nextInt(), s.nextInt());
		if (E != g.E())
			throw new IOException("Found "+g.E()+" of "+E+" expected edges.");
		System.out.println(new GraphColor(g).toString());
	}
}
