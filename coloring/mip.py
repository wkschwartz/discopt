from __future__ import print_function

from time import clock, time
import sys
import os
from random import randrange

try:
	import pulp
except ImportError:
	if sys.version_info.major < 3:
		raise

sys.path.insert(0, '../../tvrepack')
from tvrepack.relations import Digraph


def printerr(*args):
	print(*args, file=sys.stderr)

def greedy(graph, order=None):
	"""Greedy algorithm for finding an upper bound to graph coloring.

	graph is the undirected graph to be colored. order is a sequence of nodes
	that determines the order in which colors are assigned, defaulting to label
	order. Return a new feasible list of each node's color.

	My best guess is that the running time for this algorithm is about linear in
	the number of edges, though I'm guessing edge density is a factor.
	"""
	if order is None:
		order = graph
	new = [None for node in graph]
	colors = range(len(graph))
	generator = hasattr(order, 'send') and hasattr(order, '__iter__') and (
		hasattr(order, 'next') or hasattr(order, '__next__'))
	if not generator:
		order = iter(order)
	while True:
		try:
			node = next(order)
		except StopIteration:
			break
		# We can do the same thing using a minheap, but in Python it takes about
		# 2.4 times longer (tested on a random graph with a 1000 nodes and 90%
		# edge density), I'm guessing partially because of the linearithmic
		# nature of sorting and partially because of Python's function call
		# overahead.
		neighbors_colors = set(new[neighbor] for neighbor in graph[node] if
							   neighbor < len(new))
		for min in colors:
			if min not in neighbors_colors:
				new[node] = min
				break
		if generator:
			order.send(new)
	if None in new:
		raise ValueError("Node order does not include all nodes range(0, %d)"
						 % len(graph))
	return new

def welsh_powell(graph):
	"Greedy graph coloring with nodes ordered descending by degree."
	# See "An upper bound for the chromatic number of a graph and its
	# application to timetabling problems", Welsh and Powell, The Computer
	# Journal (1967) 10 (1): 85-86. doi: 10.1093/comjnl/10.1.85
	order = sorted(graph, key=lambda node: -sum(1 for neighbor in graph[node]))
	return greedy(graph, order)

# def dsatur(graph):
# 	"Greedy grpah coloring using DSATUR algorithm of Brelaz (1979)."
# 	def order(graph):
# 		order = sorted(
# 			graph, key=lambda node: sum(1 for neighbor in graph[node]))
# 		colors = (yield order.pop())
# 		while order:
# 			# Find node with maximal saturation degree
# 			node = None
# 			max = 0
# 			for node in graph:
# 				dsatur = len(set(colors[neighbor] for

def iterated_greedy(graph):
	"Iterated greedy coloring of Culberson (1992)."
	colors = welsh_powell(graph)
	k = max(colors)
	iterations, stagnations = 1, 0
	while True:
		preorder = sorted(graph, key=lambda node: -colors[node])
		if iterations % 3 == 0:
			# Reverse order heuristic
			order = preorder
		elif iterations % 3 == 1:
			# Largest-first heuristic
			sizes = {color: 0 for color in range(k + 1)}
			for color in colors:
				sizes[color] += 1
			order = sorted(preorder, key=lambda node: -sizes[colors[node]])
		else:
			# Random shuffle
			random = [randrange(k + 1) for color in range(k + 1)]
			order = sorted(preorder, key=lambda node: random[colors[node]])
		next_colors = greedy(graph, order)
		next_k = max(next_colors)
		iterations += 1
		assert next_k <= k, "Culberson lemma 4.1 violated"
		if next_k < k:
			colors = next_colors
			k = next_k
			stagnations = 0
		elif stagnations == 1 or iterations == 32:
			break
		else:
			stagnations += 1
	return colors



def test_color(f=greedy):
	print("testing", f.__name__)
	for filename in os.listdir('instructor/data/'):
		with open('instructor/data/' + filename) as file:
			graph = parse_graph(file.read())
		print(filename, end=' ')
		colors = f(graph)
		obj = max(colors)
		if obj >= len(graph):
			print()
			raise AssertionError("non-dense color assignment")
		result = is_feasible(graph, colors, obj)
		if result is not None:
			print()
			raise AssertionError(result)
		print(obj)
	print("OK")


def parse_graph(data):
	"""
	Construct a graph from a string whose first line contains the number of
	nodes and edges and subsequent lines contain undirected edges.
	"""
	table = [tuple(map(int, line.split())) for line in data.split('\n') if line]
	nodes, edges = table[0]
	graph = Digraph(nodes)
	for from_, to in table[1:]:
		graph.add(from_, to)
		graph.add(to, from_)
	found = graph.num_edges()
	assert found == edges * 2, "Expected %d edges, found %d" % (edges, found)
	return graph


def build_lp(graph, name=None):
	"""
	Construct a graph coloring mixted integer program with PuLP. Return the
	problem, the decision variables, and the objective function variable.
	"""
	if name:
		name = "Graph Coloring: %s" % name
	else:
		name = "Graph Coloring: %s nodes, %e edges" % (
			len(graph), graph.num_edges())
	problem = pulp.LpProblem(name, pulp.LpMinimize)

	# Determine the number of colors we'll need. The naive upper bound is the
	# number of nodes -- and this will certainly be the case when the graph has
	# 100% node density. However, we can use the greedy algorithm to come up with
	# a tighter bound.
	greedy_assignment = iterated_greedy(graph)
	greedy_bound = max(greedy_assignment)
	colors = list(range(greedy_bound + 1))
	printerr("chromatic upper bound: %d" % greedy_bound)

	dec = [] # list of binary color decisions per node
	for node in graph:
		node_dec = []
		for color in colors:
			choice = pulp.LpVariable(
				"node_%s_color_%s" % (node, color), 0, 1, pulp.LpBinary)
			node_dec.append(choice)
		dec.append(node_dec)

	obj = pulp.LpVariable("objective", 0, greedy_bound, pulp.LpInteger)

	problem += obj, "objective: maximum color used"
	problem += dec[0][0] == 1, "anchor"
	for color in colors:
		if color != 0:
			problem += dec[0][color] == 0, "anchor != %d" % color
	for node in graph:
		for color in colors:
			for neighbor in graph[node]:
				if node > neighbor: # undirected graph
					msg = "edge %d %d color %d" % (node, neighbor, color)
					problem += dec[node][color] + dec[neighbor][color] <= 1, msg
			msg = "max constraint: node %s color %s" % (node, color)
			problem += obj >= color * dec[node][color], msg
		problem += pulp.lpSum(dec[node]) == 1, "One color for node %s" % node

	return problem, dec, obj

def is_feasible(graph, colors, obj):
	"Return whether colors assigned in list colors are feasible for graph."
	if len(graph) != len(colors):
		return "wrong number of colors assigned: expected %s, found %s" % (
			len(graph), len(colors))
	if max(colors) != obj:
		return "Expected objective value %s but got %s" % (obj, max(colors))
	for node in graph:
		color = colors[node]
		if not isinstance(color, int) or color < 0:
			return "Node %s has an incomprehensible color assignment %r" % (
				node, color)
		for neighbor in graph[node]:
			if color == colors[neighbor]:
				return "Nodes %s and %s cannot both be color %s" % (
					node, neighbor, color)
	return None

def solve(data):
	"Given graph data, return a list indexed by node whose values are colors."
	printerr("building graph...")
	graph = parse_graph(data)
	printerr("building integer program...")
	start = clock()
	prob, choices, obj = build_lp(graph)
	end = clock()
	printerr("Done in %s s" % (end - start))
	printerr("solving...")
	start = clock()
	prob.solve()
	end = clock()
	printerr("Done in %s s" % (end - start))
	colors = []
	for node in graph:
		count = 0
		for color in range(len(choices[node])):
			if choices[node][color].value():
				if count == 0:
					colors.append(color)
				else:
					raise RuntimeError("Found multiple colors for %s" % node)
				count += 1
	reason = is_feasible(graph, colors, obj.value())
	if reason is not None:
		raise RuntimeError(reason)
	return colors, int(obj.value()), pulp.LpStatus[prob.status] == "Optimal"

def solveIt(data, time=True, assertions=True):
	solution, obj, optimal = solve(data)
	ret = str(obj) + " " + ("1" if optimal else "0") + '\n'
	ret += ' '.join(map(str, solution))
	return ret

def main():
	if len(sys.argv) > 1:
		printerr('unexpected arguments; feed data on stdin')
		sys.exit(1)
	print(solveIt(sys.stdin.read()))

if __name__ == '__main__':
	main()
