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

	/** Return the color of vertex v, or -1 if no solution exists. */
	public int color(int v) { return color[v]; }

	/** Return the maximum color used */
	public int maxColor() {
		int max = -1;
		for (int v = 0; v < V; v++)
			max = Math.max(max, color(v));
		return max;
	}

	/** Return whether the algorithm proved optimality. */
	return boolean optimal() {
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
		// cumm[i] = max(max(domain[0]), ..., max(domain[i]))
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
				cumm[i] = -1;
			}
			count = 0;
		}

		/** Copy constructor */
		public SearchNode(SearchNode old) {
			domain = new BitSet[V];
			for (int i = 0; i < V; i++)
				domain[i] = old.domain[i].clone();
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
		 * Return an array of the colors found or null if the solution is either
		 * infeasible or not yet found.
		 */
		public int[] solution() {
			int[] color;
			if (!solved())
				return null;
			color = new int[V];
			for (int v = 0; v < V; v++)
				color[v] = domain[v].nextSetBit(0);
			return color;
		}

		/**
		 * Return the value of the maximum color used.
		 */
		public int maxColor() { return cumm[V - 1]; }

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
			int oldCard, newMax;
			assert v >= 0 && fromColor >= 0 && toColor > fromColor &&
				v < V && toColor <= V;
			assert v == 0 ||
				cumm[v] == Math.max(cumm[v - 1], domain[v].length() - 1);

			oldCard = domain[v].cardinality();
			assert oldCard >= 1;
			domain[v].clear(fromColor, toColor);
			newMax = domain[v].length() - 1;
			assert v == 0 || newMax <= cumm[v - 1] + 1;

			if (newMax < 0) { // domain[v] is empty: constraint is infeasible.
				count = -1;
				return false;
			}
			else if (oldCard > 1 && domain[v].cardinality() == 1) {
				count++; // newMax is v's color now.
				for (int w: g.adj(v)) // Enforce edge constraint.
					if (!ruleOut(w, newMax, newMax + 1))
						return false;
			}
			// The condition below ensures that cumm[v] will go down, meaning
			// that a new constraint may be propogated. Note that when
			// cumm[v] > cumm[v - 1], cumm[v] equals v's old max before the call
			// to clear above. Thus v's old max was the binding constraint
			// preventing cumm[v + 1] from being lower than it is. Further,
			// since the conditional requires that domain[v].length() changed,
			// it won't be true when oldCard == 1 because the only possible
			// change in that circumstance would cause newMax < 0 and thus the
			// funciton would have returned false by now.
			if ((v == 0 || cumm[v] > cumm[v - 1]) && cumm[v] > newMax) {
				assert oldCard != 1;
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
		// Symmetry constraint: color[start] = 0; further, since we will make
		// guesses going from low numbered nodes up and from low color numbers
		// up.
		return branch(s, 0, 0, V - 1);
	}

	/** Branch and bound. */
	private int[] branch(SearchNode s, int v, int color, int best) {
		int[] color;
		if (!s.setColor(v, color) || s.bound() > best) // short circuit
			return null;
		if ((color = s.solution()) != null)
			return color;
		// Now must make a guess for each node w in range(v + 1, V) and each
		// color c in domain of w. Will likely have to add methods to SearchNode
		// for this.
	}
}
