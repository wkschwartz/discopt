package gc;

import java.util.Iterator;

import edu.princeton.cs.introcs.In;
import edu.princeton.cs.algs4.Graph;

import org.junit.runners.JUnit4;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for {@link DegreeSortedGraph}.
 *
 * @author William Schwartz
 */
@RunWith(JUnit4.class)
public class TestDegreeSortedGraph {

	private static final String graphPath = "instructor/data/gc_1000_9";
	private Graph g;
	private DegreeSortedGraph dsg;

	private Graph graph(String path) {
		In graphFile = new In(graphPath);
		int V = graphFile.readInt(), E = graphFile.readInt();
		Graph g = new Graph(V);
		int count;
		for (count = 0; !graphFile.isEmpty(); count++)
			g.addEdge(graphFile.readInt(), graphFile.readInt());
		graphFile.close();
		org.junit.Assert.assertEquals(E, count);
		return g;
	}

	@Before
	public void setUp() {
		In graphFile = new In(graphPath);
		g = new Graph(graphFile);
		graphFile.close();
		dsg = new DegreeSortedGraph(g);
	}

	@Test
	public void g2dsgMapsGtoDSG() {
		org.junit.Assert.assertEquals(g.V(), dsg.V());
		org.junit.Assert.assertEquals(g.E(), dsg.E());
		for (int gNode = 0; gNode < dsg.V(); gNode++) {
			Iterator<Integer> dsgAdj = dsg.adj(dsg.g2dsg(gNode)).iterator();
			Iterator<Integer> gAdj = g.adj(gNode).iterator();
			while (gAdj.hasNext())
				org.junit.Assert.assertEquals((int) dsg.g2dsg(gAdj.next()),
											  (int) dsgAdj.next());
		}
	}

	public static void main(String[] args) {
		System.out.println("before");
		org.junit.runner.JUnitCore.runClasses(TestDegreeSortedGraph.class);
		System.out.println("after");
	}
}
