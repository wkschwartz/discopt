/**
 * Solve the graph coloring problem using the Choco constraint-programming
 * library.
 * <p>
 * Dependencies: Graph.java (from algs4.jar by Sedgwick and Wayne). Choco 2
 * (http://www.emn.fr/z-info/choco-solver/)
 */

import choco.Choco;
import choco.cp.solver.CPSolver;
import choco.cp.model.CPModel;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;

public class GraphColoring {
	private static final int TIME_LIMIT = 10 * 1000; // 10 seconds

	private final Graph g;
	private final CPSolver s;
	private final IntegerVariable[] nodes;

	public GraphColoring(Graph g) {
		g = new Graph(g); // defensive copy
		CPModel m = new CPModel();

		// Create the decision variables
		nodes = new IntegerVariable[g.V()];
		for (int i = 0; i < g.V(); i++) {
			nodes[i] = Choco.makeIntVar("node-" + i, 0, g.V() - 1);
			m.addVariable(nodes[i]); // Add in case a node has no neighbors
		}

		// Add the constraints
		for (int v = 0; v < g.V(); v++)
			for (int w : g.adj(v))
				if (v < w) // So we don't count both v->w and w->v
					m.addConstraint(Choco.neq(nodes[v], nodes[w]));

		// Add the objective function
		IntegerVariable obj = Choco.makeIntVar("obj", 0, g.V() - 1, Options.V_OBJECTIVE);
		m.addVariable(obj);
		m.addConstraint(Choco.eq(obj, Choco.max(nodes)));

		// Create the solver
		s = new CPSolver();
		s.read(m);
		s.minimize(true);
		s.setTimeLimit(TIME_LIMIT);
		if (!s.solve())
			throw new Exception("Failed to solve");
	}

	/** Return the value of the objective function: the max color */
	public int objective() {
		int t, max = 0;
		for (int v = 0; v < g.V(); v++) {
			t = color(v);
			if (t > max)
				max = t;
		}
		return max;
	}

	/** Return the color of node v. */
	public int color(int v) {
		return s.getVar(nodes[v]).getVal();
	}

	/** Return whether the solver proved optimality. */
	public boolean optimal() {
		return false; // For now, we're not even optimizing
	}

}
