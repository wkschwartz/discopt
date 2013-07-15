package gc;

import edu.princeton.cs.algs4.Graph;
import java.util.Comparator;
import java.util.Arrays;


/**
 * A graph with the nodes relabeled so the higher-degree nodes come first.
 * <p>
 * Let <code>g</code> be a <code>Graph</code> instance. Then
 * <code>dsg.reverse</code> such that <code>dsg = DegreeSortedGraph(g)</code> is
 * an isomorphism that maps nodes of <code>dsg</code> to nodes of
 * <code>g</code>. Further, if <var>i</var> < <var>j</var> then the degree of
 * node <var>i</var> of <code>dsg</code> is at least the degree of node
 * <var>j</var> or <code>dsg</code>.
 */
public class DegreeSortedGraph extends Graph {

	private final int[] reverse;

	public DegreeSortedGraph(Graph g) {
		super(g.V());
		int count;
		int[] degree = new int[g.V()];
		Integer[] order = new Integer[g.V()];
		for (int v = 0; v < g.V(); v++) {
			count = 0;
			order[v] = v;
			for (int w : g.adj(v))
				count++;
			degree[v] = count;
		}
		Arrays.sort(order, new DegreeOrder(degree));
		reverse = new int[g.V()];
		for (int v = 0; v < g.V(); v++) {
			for (int w : g.adj(v))
				if (v < w)
					addEdge(order[v], order[w]);
			reverse[order[v]] = v;
		}
	}

	private static class DegreeOrder implements Comparator<Integer> {

		private final int[] degree;

		public DegreeOrder(int[] degree) {
			this.degree = degree;
		}

		public int compare(Integer a, Integer b) {
			if (degree[a] > degree[b])
				return -1;
			else if (degree[a] == degree[b])
				return 0;
			else
				return 1;
		}
	}

	/** Return the original name of ordered vertex <code>v</code>. */
	public int reverse(int v) { return reverse[v]; }
}
