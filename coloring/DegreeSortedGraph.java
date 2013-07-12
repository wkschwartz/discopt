import java.util.Comparator;
import java.util.Arrays;

public class DegreeSortedGraph extends Graph {

	private final Integer[] order;

	public DegreeSortedGraph(Graph g) {
		super(g.V());
		int count;
		int[] degree = new int[g.V()];
		order = new Integer[g.V()];
		for (int v = 0; v < g.V(); v++) {
			count = 0;
			order[v] = v;
			for (int w : g.adj(v))
				count++;
			degree[v] = count;
		}
		Arrays.sort(order, new DegreeOrder(degree));
		for (int i = 0; i < g.V(); i++)
			for (int w : g.adj(order[i]))
				addEdge(order[i], order[w]);
	}

	private class DegreeOrder implements Comparator<Integer> {

		private final int[] degree;

		public DegreeOrder(int[] degree) {
			this.degree = degree;
		}

		public int compare(Integer a, Integer b) {
			if (degree[a] > degree[b])
				return 1;
			else if (degree[a] == degree[b])
				return 0;
			else
				return -1;
		}
	}

	/** Return the original name of ordered vertex <code>v</code>. */
	public int reverse(int v) {
		for (int i = 0; i < order.length; i++)
			if (order[i] == v)
				return i;
		throw new ArrayIndexOutOfBoundsException();
	}
}
