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

	private class SearchNode {

		// A set of possible colors for each node
		private final BitSet[] domain;
		// cumm[i] = max(max(domain[0]), ..., max(domain[i]))
		private final int[] cumm;

		/**
		 * Instantiate a new search node, breaking symmetry with lexical
		 * ordering and setting color[0] = 0;
		 */
		public SearchNode() {
			domain = new BitSet[V];
			cumm = new int[V];
			for (int i = 0; i < V; i++) {
				domain[i] = new BitSet(V);
				domain[i].set(0, i + 1); // Second arg of set() is exclusive
				cumm[i] = -1;
			}
			setColor(0, 0, true);
		}

		/** Copy constructor */
		public SearchNode(SearchNode old) {
			domain = new BitSet[V];
			for (int i = 0; i < V; i++)
				domain[i] = old.domain[i].clone();
		}

		/**
		 * Set vertex v to color. If set is false, skip setting the
		 * domain. Return false when this operation caused an infeasible
		 * solution.
		 */
		public boolean setColor(int v, int color, boolean set) {
			assert v >= 0 && color >= 0 && v < V && color < V;
			assert domain[v].get(color); // Color is in v's domain
			assert feasible(v, color); // Color obeys v's edge constraints
			assert v == 0 || color <= cumm[v - 1] + 1; // Symmetry constraint
			assert v == 0 || color <= cumm[v]; // Consequence of the above

			BitSet vset = domain[v];
			if (set) {
				vset.clear();
				vset.set(color);
				if (v == 0)
					cumm[v] = color;
				else if (color < cumm[v])
					cumm[v] = Math.max(color, cumm[v - 1]);
				for (int w = v + 1; w < V; w++)
					cumm[w] = Math.max(cumm[w - 1], domain[w].length() - 1);
			}

			assert domain[v].cardinality() == 1 && domain[v].get(color);
			// Propogate the change in the symmetry breaking constraint
//			for (int w = v + 1; w < V; w++)
//	            if (!ruleOut(w, getMax(w)));
			// Edge constraint
			for (int w : g.adj(v))
				ruleOut(w, color)
		}

		/** Return true if setting node v to color obeys the edge constraint. */
		private boolean feasible(int v, int color) {
			for (int w : g.adj(v))
				if (domain[w].cardinality() == 1 && domain[w].get(color))
					return false;
			return true;
		}

		/** Rule out that a vertex has a color. Return the color of v or -1. */
		private boolean ruleOut(int v, int color) {
			assert v >= 0 && color >= 0 && v < V && color < V;

			BitSet vset = domain[v];
			int newMax;

			vset.clear(color);
			newMax = vset.length() - 1;

			if (vset.cardinality() == 1)
				return setColor(v, vset.nextSetBit(0), false);
			else if (newMax < 0) // We found an infeasible arrangement
				return false;
			else if (newMax < cumm[v]) {
				// Propogate the change in the symmetry breaking constraint
				assert newMax >= 0 && newMax <= cumm[v - 1] + 1;
				if (v == 0)
					cumm[v] = newMax;
				else
					for (int w = v; w < V; w++)
						cumm[w] = Math.max(cumm[w - 1], domain[w].length() - 1);
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
