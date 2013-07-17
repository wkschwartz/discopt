#! /usr/bin/env python3
"""Provide solveIt() hook for the class's submit.pyc script. Feed data on stdin."""


from __future__ import print_function, unicode_literals


import sys
sys.path.insert(0, '..')
import base_solver


class Solution(base_solver.JavaSolution):

	SOLVER = 'gc.GraphColor'
	SRCGLOBS = ['gc/*.java']

solveIt = Solution.solveIt

if __name__ == '__main__':
	solver.main(Solution)
