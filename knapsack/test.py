#! /usr/bin/env python3


import unittest
import subprocess
import os
import glob


def load_tests(loader, tests, pattern):
	"Load all tests available in TestKnapsack."
	TK = type('TestKnapsack', (TestKnapsack, unittest.TestCase), {})
	for i in TestKnapsack.TESTS_AVAILABLE:
		tests.addTest(TK(test=i, methodName='test_answer'))
	return tests


class TestKnapsack:

	JAVA = ['java', '-ea']
	JAVAC = ['javac', '-g']
	SOLVER = 'Knapsack'
	DIR = os.path.join('instructor', 'data')

	# Problem sizes and correct, optimal objective values for knapsack tests 1
	# to 6 (indexed 0 to 5).
	PROBLEM_SIZES = (30, 50, 200, 400, 1000, 10000)
	EXPECTED_ANSWERS = (99798, 142156, 100236, 3967180, 109899, 1099893)
	TESTS_AVAILABLE = tuple(range(len(PROBLEM_SIZES)))

	@classmethod
	def setUpClass(cls):
		subprocess.check_call(cls.JAVAC + [cls.SOLVER + '.java'])

	@classmethod
	def tearDownClass(cls):
		for filename in glob.iglob(cls.SOLVER + '*.class'):
			os.unlink(filename)

	def __init__(self, *args, test=None, **kwargs):
		super().__init__(*args, **kwargs)
		if test not in self.TESTS_AVAILABLE:
			raise ValueError('unknown test %r' % test)
		self.test = test

	def __len__(self):
		return self.PROBLEM_SIZES[self.test]

	@property
	def filename(self):
		return os.path.join(self.DIR, 'ks_%d_0' % len(self))

	def shortDescription(self):
		return os.path.basename(self.filename)

	def setUp(self):
		self.data_file = open(self.filename)

	def tearDown(self):
		self.data_file.close()

	@property
	def expected_answer(self):
		return self.EXPECTED_ANSWERS[self.test]

	def test_answer(self):
		result = subprocess.check_output(self.JAVA + [self.SOLVER],
										 stdin=self.data_file)
		answer = int(result.decode('ascii').split()[0])
		self.assertEqual(answer, self.expected_answer)


if __name__ == '__main__':
	unittest.main()
