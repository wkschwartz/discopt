package gc;

import edu.princeton.cs.algs4.Graph;
import java.util.Comparator;
import java.util.Arrays;
import java.util.Scanner;


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

	private final int[] g2dsg;

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
		g2dsg = new int[g.V()];
		for (int v = 0; v < g.V(); v++)
			g2dsg[order[v]] = v;
		for (int v = 0; v < g.V(); v++)
			for (int w : g.adj(v))
				if (v < w)
					addEdge(g2dsg(v), g2dsg(w));
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

	/** Return the new name of the input graph's vertex <code>v</code>. */
	public int g2dsg(int v) { return g2dsg[v]; }

	/** Test client */
	public static void main(String[] args) {
		if (args.length > 0) {
			System.err.println("unkown arguments found; pass data in on stdin");
			System.exit(1);
		}
		Scanner s = new Scanner(System.in, "UTF-8");
		Graph g = new Graph(s.nextInt());
		int E = s.nextInt();
		while (s.hasNextInt())
			g.addEdge(s.nextInt(), s.nextInt());
		System.out.println("From original graph:");
		System.out.println(g.toString());
		DegreeSortedGraph dg = new DegreeSortedGraph(g);
		System.out.println("To sorted graph:");
		System.out.println(dg.toString());
		System.out.println("from | to");
		for (int i = 0; i < g.V(); i++)
			System.out.println(i + "    |  " + dg.g2dsg(i));
		System.out.println("\nto | from | degree");
	}
}
