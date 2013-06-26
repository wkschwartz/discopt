#! /usr/bin/env python3

"""Command line knapsack problem solver.

Usage: python3 knapsack < inputfile.txt
"""


import sys


class Knapsack:

	"""Represent the solution of a one-dimensional knapsack problem.

	Given n-length vectors v of values and w of weights, all integers, as well
	as an integer k capacity, find the vector x of length n whose elements are
	in {0, 1} such that x maximizes
            sum^{n-1}_{i=0}{v_i * x_i}
	subject to
            sum^{n-1}_{i=0}{w_i * x_i} \le k
	and makes the values of x available through index notation.
	"""

	def __init__(self, k, v, w):
		self.v, self.w = tuple(v), tuple(w)
		if len(v) != len(w):
			raise ValueError('ambiguous problem size')
		self.n = len(v)
		if self.n < 1:
			raise ValueError('no items to put in knapsack')
		self.k = k
		self.optimal = False
		type_check = (isinstance(k, int) and
					  all(isinstance(y, int) for y in self.v + self.w))
		if not type_check:
			raise TypeError('k and all elements of v and w must be ints')
		self._fill()
		assert self._feasible()

	def __len__(self):
		return self.n
	def __getitem__(self, i):
		return bool(self.x[i])

	def objective(self):
		"Return the value of the objective function for the solution."
		return sum(map(lambda tup: tup[0] * tup[1], zip(self.v, self.x)))

	def _feasible(self):
		weight = 0
		for i in range(self.n):
			assert isinstance(self.x[i], int)
			weight += self.w[i] * self.x[i]
		return weight <= self.k

	def class_out(self):
		"""
		Return a string containing the answer as the class grading tool expects.

		The first line contains an integer objective-function value followed by
		a `1` if the value is optimal or a `0` otherwise. The next line is a
		space separated list of zeros and ones for each decision variable in
		order.
		"""
		return '\n'.join([' '.join(map(str, [self.objective(), int(self.optimal)])),
						  ' '.join(str(int(xi)) for xi in self.x)]) + '\n'

	def _fill(self):
		"Fill knapsack as optimally as possible."
		t = [None] * self.n
		t[0] = 0
		best = self._branch(t, 0, 0, 0, 0)
		t[0] = 1
		best = self._branch(t, 0, 0, 0, best)
		if not hasattr(self, 'x'):
			self.x = tuple(0 for i in range(self.n))
		self.optimal = self.objective() == best

	def _branch(self, t, i, prev_val, prev_weight, best):
		assert all(isinstance(y, int) for y in (i, prev_val, prev_weight, best))
		assert i < self.n and (t[i] == 0 or t[i] == 1)
		weight = prev_weight + self.w[i] * t[i]
		if weight > self.k:
			return best
		value = prev_val + self.v[i] * t[i]
		new_best = max(value, best)
		if i < self.n - 1:
			t[i + 1] = 0
			new_best = max(self._branch(t, i + 1, value, weight, new_best),
						   new_best)
			t[i + 1] = 1
			new_best = max(self._branch(t, i + 1, value, weight, new_best),
						   new_best)
			t[i + 1] = None # So we return t to caller like we found it
		elif value >= best: # Must include equality in case best from same branch
			self.x = tuple(t)
		return new_best


def main(infile, outfile):
	"""
	Command line interface for the knapsack solver. Uses the class's input and
	output formats on stdin and stdout.

	The input format is as follows. First line, line -1, is two integers: `n
	k`. Line i for i in range(n) is `v[i] w[i]`.
	"""
	n, k = map(int, next(infile).split())
	v, w = [], []
	for vi, wi in map(lambda l: map(int, l.split()), infile):
		v.append(vi)
		w.append(wi)
	if len(v) != n:
		raise ValueError('Expected %d items but found %d' % (n, len(v)))
	knapsack = Knapsack(k, v, w)
	outfile.write(knapsack.class_out())


if __name__ == '__main__':
	if len(sys.argv) > 1:
		print('unexpected arguments; pass input on stdin', file=sys.stderr)
		sys.exit(1)
	main(sys.stdin, sys.stdout)
