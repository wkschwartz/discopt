import java.util.BitSet;

public class GraphColor {

	private final Graph g;
	private final int V;
	private final int[] color;

	public GraphColor(Graph g) {
		this.g = Graph(g); // Defensive copy
		V = g.V();
		color = solve();
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
		// cumm[i] = max(max(domain[0]), ..., max(domain[i]))
		private final int[] cumm;

		/** Instantiate a new search node. */
		public SearchNode() {
			domain = new BitSet[V];
			cumm = new int[V];
			for (int i = 0; i < V; i++) {
				domain[i] = new BitSet(V);
				domain[i].set(0, V); // Second arg of set() is exclusive
				cumm[i] = -1;
			}
		}

		/** Copy constructor */
		public SearchNode(SearchNode old) {
			domain = new BitSet[V];
			for (int i = 0; i < V; i++)
				domain[i] = old.domain[i].clone();
		}

		/** Return true if setting node v to color obeys the edge constraint. */
		private boolean feasible(int v, int color) {
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
		 * Test whether we've found a color for every node. 1 indicates
		 * success. 0 indicates that the search is not over. -1 indicates
		 * infeasibility.
		 */
		public int solved() {
			int card;
			for (int v = 0; v < V; v++) {
				card = domain[v].cardinality();
				if (card == 0)
					return -1;
				else if (card > 1)
					return 0;
			}
			return 1;
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
			assert feasible(v, color); // Color obeys v's edge constraints
			assert v == 0 || color <= cumm[v - 1] + 1; // Symmetry constraint
			assert v == 0 || color <= cumm[v]; // Consequence of the above

			if (color == 0)
				success = ruleOut(v, color + 1, V);
			else if (color == V - 1)
				success = ruleOut(v, 0, V - 1);
			else
				success = ruleOut(v, 0, color) && ruleOut(v, color + 1, V);
			assert domain[v].cardinality() == 1 && domain[v].get(color);
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
			int newMax;
			assert v >= 0 && fromColor >= 0 && toColor > fromColor &&
				v < V && toColor <= V;

			domain[v].clear(fromColor, toColor);
			newMax = domain[v].length() - 1;
			assert v == 0 || newMax <= cumm[v - 1] + 1;

			if (newMax < 0) // domain[v] is empty: constraint is infeasible.
				return false;
			else if (domain[v].cardinality() == 1) // newMax is v's color now.
				for (int w: g.adj(v)) // Enforce edge constraint.
					if (!ruleOut(w, newMax, newMax + 1))
						return false;
			// The condition below ensures that cumm[v] will go down, meaning
			// that a new constraint may be propogated. Note that when
			// cumm[v] > cumm[v - 1], cumm[v] equals v's old max before the call
			// to clear above. Thus v's old max was the binding constraint
			// preventing cumm[v + 1] from being lower than it is.
			if ((v == 0 || cumm[v] > cumm[v - 1]) && cumm[v] > newMax) {
				if (v == 0)
					cumm[v] = color;
				else
					cumm[v] = cumm[v - 1] > newMax ? cumm[v - 1] : newMax;
				if (v < V - 1)
					return ruleOut(v + 1, cumm[v] + 2, V);
			}
			return true;
		}
	}

	private int[] solve() {
		SeachNode s = new SearchNode(V);
		int start = 0; // TODO This should probably be the highest-degree node.
		// Symmetry constraint: color[start] = 0;
		s.setColor(start, 0, true);

	}
}
