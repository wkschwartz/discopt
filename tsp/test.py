#! /usr/bin/env python3


import sys
sys.path.insert(0, '..')
import test_answers as ta
import solver


class TestTSP(ta.TestAnswers):

	PREFIX = 'tsp'
	PROBLEMS = ((50, 3), (70, 7), (100, 5), (250, 9), (500, 1), (1000, 5))
	EXPECTED_ANSWERS = (428, 20750, 29440, 36934, 316527, 67217770) # The last is wrong

	@staticmethod
	def solve(data):
		return solver.solveIt(data, time=True, assertions=True)


load_tests = ta.load_tests_factory(TestTSP)


if __name__ == '__main__':
	ta.main()
