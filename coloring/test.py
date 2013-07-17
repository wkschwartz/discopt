#! /usr/bin/env python3


import sys
sys.path.insert(0, '..')
import test_answers as ta
import solver


class TestColoring(ta.TestAnswers):

	PREFIX = 'gc'
	PROBLEMS = ((50, 3), (70, 7), (100, 5), (250, 9), (500, 1), (1000, 5))
	EXPECTED_ANSWERS = (6, 17, 15, 73, 13, 85) # The latter two are wrong

	@staticmethod
	def solve(data):
		return solver.solveIt(data, time=True, assertions=True)


load_tests = ta.load_tests_factory(TestColoring)


if __name__ == '__main__':
	ta.main()
