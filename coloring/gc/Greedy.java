package gc;

import java.util.HashSet;
import edu.princeton.cs.algs4.Graph;

public class Greedy {

	private final DegreeSortedGraph g;
	private final int V;
	private final int[] colors;

	public Greedy(Graph g) {
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
		return false; // Yeah... no.
	}

	public int[] solution() {
		int[] solution = new int[V];
		System.arraycopy(colors, 0, solution, 0, V);
		return solution;
	}


	private int[] greedy(int[] order) {
		int[] colors = new int[V];
		int count = 0;
		HashSet<Integer> neighborColors = new HashSet<Integer>();
		for (int node : order) {
			neighborColors.clear();
			for (int neighbor : g.adj(node))
				if (neighbor < count)
					neighborColors.add(colors[neighbor]);
			for (int minColor = 0; minColor < V; minColor++) {
				if (!neighborColors.contains(minColor)) {
					colors[node] = minColor;
					count++;
					break;
				}
			}
		}
		if (count != V)
			throw new IllegalArgumentException("bad order");
		return colors;
	}

	private int[] welsh_powell() {
		int[] order = new int[V];
		// For Welsh-Powell, just use the node order since the graph here is
		// degree-sorted to begin with.
		for (int node = 0; node < V; node++)
			order[node] = node;
		return greedy(order);
	}

	private int[] solve() {
		return welsh_powell();
	}
}
