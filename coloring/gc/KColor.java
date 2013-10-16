package gc;

import java.util.Scanner;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.io.IOException;
import edu.princeton.cs.algs4.Graph;

public class KColor {

	private static final int UNSOLVED = -1, INFEASIBLE = -2;
	// The graph to be colored.
	private final DegreeSortedGraph g;
	// Number of nodes, for convenience.
	private final int V;
	// k is the maximum number of colors to use.
	private final int k;
	// The solution, once we have it.
	private final int[] colors;

	public KColor(Graph g, int k) {
		if (k < 1 || k > g.V()) {
			String m = String.format("Cannot %d-color a G_%d graph", k, g.V());
			throw new IllegalArgumentException(m);
		}
		System.err.printf("Finding %d-coloring\n", k);
		this.g = new DegreeSortedGraph(g);
		this.k = k;
		V = g.V();
		colors = solve();
	}

	/** Return the color of vertex v, or -1 if no solution exists. */
	public int color(int v) { return colors[g.g2dsg(v)]; }

	/** Return the maximum color used */
	public int maxColor() {
		int max = INFEASIBLE;
		for (int v = 0; v < V; v++)
			max = Math.max(max, color(v));
		return max;
	}

	/** Return whether the algorithm proved optimality. */
	public boolean optimal() {
		return false;
	}

	/** Return whether the solution is feasible. */
	public boolean isFeasible() { return feasible() == ""; }

	/**
	 * Return a string describing the error if the coloring is infeasible.
	 */
	private String feasible() {
		if (colors.length != V)
			return String.format("Wrong number of nodes colored: %d",
								 colors.length);
		for (int v = 0; v < V; v++) {
			if (colors[v] < 0 || colors[v] >= k)
				return String.format("Node %d has bad %d-color %d",
									 v, k, colors[v]);
			for (int neighbor : g.adj(v))
				if (colors[v] == colors[neighbor])
					return String.format("Nodes %d and %d cannot both be %d",
										 v, neighbor, colors[v]);
		}
		return "";
	}

	private int[] solve() {
		SearchNode solution = attempt(new SearchNode(), 0, 0);
		if (solution != null)
			return solution.solution();
		int[] fail = new int[V];
		for (int v = 0; v < V; v++)
			fail[v] = INFEASIBLE;
		return fail;
	}

	private SearchNode attempt(SearchNode s, int v, int color) {
		SearchNode nextAttempt;
//		System.out.printf("Attempting node %d = color %d\n", v, color);
		if (!s.setColor(v, color)) {
//			System.out.println("Terminating bad branch");
			return null;
		}
		if (s.solved()) {
//			System.out.println("Solved1");
			return s;
		}
		for (int node : s.unsolvedNodes()) {
			for (int nextColor : s.nextColors(node)) {
				nextAttempt = attempt(new SearchNode(s), node, nextColor);
				if (nextAttempt != null && nextAttempt.solved()) {
//					System.out.println("Solved2");
					return nextAttempt;
				}
			}
		}
//		System.out.println("Terminating bad branch");
		return null; // None of the nodes below on the subtree were feasible.
	}

	private class SearchNode {

		// A set of possible colors for each node
		private final BitSet[] domains;
		// The partial coloring being built up. UNSOLVED when not solved yet.
		private final int[] partial;
		// Maximum color used so far
		private int maxColor;
		// The count of solved nodes. INFEASIBLE when infeasible. V when solved.
		private int solved;
		// Search tree depth
		private int depth;
		// For improving first-fail search order
		private Comparator<Integer> order;

		public SearchNode() {
			domains = new BitSet[V];
			partial = new int[V];
			for (int v = 0; v < V; v++) {
				domains[v] = new BitSet();
				domains[v].set(0, k);
				partial[v] = UNSOLVED;
			}
			solved = 0;
			depth = 0;
			maxColor = 0;
			order = new DomainSizeOrder();
		}

		// copy constructor
		public SearchNode(SearchNode old) {
			domains = new BitSet[V];
			partial = new int[V];
			System.arraycopy(old.partial, 0, partial, 0, V);
			for (int i = 0; i < V; i++)
				domains[i] = (BitSet) old.domains[i].clone();
			solved = old.solved;
			depth = old.depth + 1;
			maxColor = old.maxColor;
			order = new DomainSizeOrder();
		}

		/**
		 * For sorting nodes by domain size for fail-first search.
		 */
		private class DomainSizeOrder implements Comparator<Integer> {
			public int compare(Integer v, Integer w) {
				return domains[v].cardinality() - domains[w].cardinality();
			}
		}

		public int depth() { return depth; }

		/**
		 * Return true if the solution is complete and feasible.
		 */
		public boolean solved() { return solved == V; }

		/**
		 * Return array of the colors found or -1s if the solution is either
		 * infeasible or not yet found.
		 */
		public int[] solution() {
			int[] solution = new int[V];
			System.arraycopy(partial, 0, solution, 0, V);
			return solution;
		}

		/** Return iterable of feasible colors less than max for node v if it
		 * doesn't have a color already.
		 */
		public Iterable<Integer> nextColors(int v) {
			LinkedList<Integer> cs = new LinkedList<Integer>();
			BitSet d = domains[v];
			if (d.cardinality() > 1)
				for (int c = d.nextSetBit(0); c >= 0 && c <= maxColor; c = d.nextSetBit(c + 1))
					cs.add(c);
			int unused = d.nextSetBit(maxColor + 1);
			if (unused >= 0)
				cs.add(unused);
			return cs;
		}

		/**
		 * Return iterable of nodes not yet solved sorted first in ascending
		 * order of domain size and second in descending order of degree. This
		 * ordering should help find bad search paths faster, hopefully trimming
		 * the search tree.
		 */
		public Iterable<Integer> unsolvedNodes() {
			int count = 0;
			ArrayList<Integer> nodes = new ArrayList<Integer>(V - solved);
			for (int v = 0; count < (V - solved) && v < V; v++) {
				if (partial[v] == UNSOLVED) {
					count++;
					nodes.add(v);
				}
			}
			Collections.sort(nodes, order);
			return nodes;
		}

		/**
		 * Rule out that a vertex <code>v</code> has any color from
		 * <code>fromColor</code> (inclusive) to <code>toColor</code>
		 * (exclusive).
		 *
		 * @return <code>false</code> if such a constraint causes
		 * the solution to become infeasible.
		 */
		private boolean ruleOut(int v, int lo, int hi) {
			if (lo < 0 || v < 0 || hi < lo || v >= V || hi > k) {
				String m = String.format("v=%d, lo=%d, hi=%d", v, lo, hi);
				throw new IllegalArgumentException(m);
			}

			int oldMax, newMax;
			BitSet domain = domains[v];
			oldMax = domain.length() - 1;
			domain.clear(lo, hi);
			newMax = domain.length() - 1;

			// Base case for infeasibility: no more options for the node.
			if (newMax < 0) {
				assert domain.isEmpty();
				solved = INFEASIBLE;
				return false;
			}
			if (partial[v] == UNSOLVED) {
				int nextColor;
				assert domain.cardinality() > 0;
				if (domain.cardinality() == 1) {
					// If solved, mark as solved and propogate edge constraint.
//					System.out.printf("Discovered node %d = color %d\n", v, newMax);
					assert domain.get(newMax);
					solved++;
					partial[v] = newMax;
					if (maxColor < newMax) {
						// Difference of 1 because of the lexicographical
						// symmetry breaking below
						assert newMax - maxColor == 1;
						maxColor = newMax;
					}
					for (int w: g.adj(v))
						if (!ruleOut(w, newMax, newMax + 1))
							return false;
				}
				else if ((nextColor = domain.nextSetBit(0)) > maxColor) {
					// Lexicographic symmetry breaking: Since no used color is
					// left, just take the next unused one.
					assert domain.cardinality() > 1;
					// Really we want to call setColor but that doubles the height
					// of the call stack in the easy cases when this symmetry
					// breaking constraint solves big chunks of the puzzle.
					ruleOut(v, nextColor + 1, k);
				}
			}
			return true;
		}

		/**
		 * Add the constraint that vertex <code>v</code> is a given color.
		 *
		 * @return <code>false</code> if such a constraint causes
		 * the solution to become infeasible.
		 */
		public boolean setColor(int v, int color) {
			if (color < 0 || color >= k || v < 0 ||  v >= V ||
				!domains[v].get(color)) {
				String m = String.format("v=%d, color=%d", v, color);
				throw new IllegalArgumentException(m);
			}
//			assert v >= depth: "depth: " + depth + " v: " + v;

			boolean success;

			if (color == 0)
				success = ruleOut(v, 1, k);
			else if (color == k - 1)
				success = ruleOut(v, 0, k - 1);
			else
				success = ruleOut(v, 0, color) && ruleOut(v, color + 1, k);
			assert (!success || domains[v].cardinality() == 1 &&
					domains[v].get(color) && partial[v] == color) :
			"v=" + v + ", color=" + color + " domain=" + domains[v].toString();
			return success;
		}
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
		if (args.length > 1) {
			System.err.println("unkown arguments found; pass data in on stdin");
			System.exit(1);
		}
		if (args.length == 0) {
			System.err.println("must pass argument k for at-most-k-coloring");
			System.exit(1);
		}
		int k = Integer.parseInt(args[0]);
		Scanner s = new Scanner(System.in, "UTF-8");
		Graph g = new Graph(s.nextInt());
		int E = s.nextInt();
		while (s.hasNextInt())
			g.addEdge(s.nextInt(), s.nextInt());
		if (E != g.E())
			throw new IOException("Found "+g.E()+" of "+E+" expected edges.");
		System.out.println(new KColor(g, k).toString());
	}
}
