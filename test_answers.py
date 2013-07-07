'''Base class for testing answers for this class.

To use this module, subclass `TestAnswers` and follow the instructions for using
`load_tests_factory`. This module also exports a `main` function for use from
the command line.
'''

import unittest as _unittest
import os as _os


main = _unittest.main


def load_tests_factory(test_class, test_methods=('test_answer',)):
	"""Return a load_tests-protocol function for a test module.

	`test_class` should be the test class that inherits from
	`TestAnswers`. `test_methods` should be an iterable of strings giving the
	names of the test methods to call; defaults to a tuple containing just
	"test_answer".

	Bind the return value of this function to a module-global variable named
	`load_tests` so the default `unittest` test runner picks it up.
	"""
	def load_tests(loader, tests, pattern):
		T = type(test_class.__name__, (test_class, _unittest.TestCase), {})
		for i in T.tests_available():
			tests.addTest(T(test=i, methodName='test_answer'))
		return tests
	return load_tests


class TestAnswers:

	"""
	Base class for testing the objective function values produced by the
	Coursera class's `solver.solveIt` API.

	Base classes should override `PREFIX`, `PROBLEMS` and
	`EXPECTED_ANSWERS`. See comments in the source for instructions.
	"""

	PREFIX = None # Should be a str prefix for the data files of the form
                  # prefix_size_version
	PROBLEMS = None # Should be a tuple of two tuples with problem sizes and
	                # version numbers
	EXPECTED_ANSWERS = None # Should be a tuple of answers for each problem
	DIR = _os.path.join('instructor', 'data') # Location of data files.

	@classmethod
	def tests_available(cls):
		return range(len(cls.PROBLEMS))

	@classmethod
	def setUpClass(cls):
		assert isinstance(cls.PREFIX, str)
		assert len(cls.PROBLEMS) == len(cls.EXPECTED_ANSWERS)
		assert all(isinstance(i, int) for i in cls.EXPECTED_ANSWERS)
		assert all(isinstance(t, tuple) and len(t) == 2 for t in cls.PROBLEMS)
		from solver import solveIt
		cls.solve = staticmethod(solveIt)

	def __init__(self, *args, test=None, **kwargs):
		super().__init__(*args, **kwargs)
		if test not in self.tests_available():
			raise ValueError('unknown test %r' % test)
		self.test = test

	def __len__(self):
		return self.PROBLEMS[self.test][0]

	def problem_version(self):
		return self.PROBLEMS[self.test][1]

	def shortDescription(self):
		return '%s_%d_%d' % (self.PREFIX, len(self), self.problem_version())

	def filename(self):
		return _os.path.join(self.DIR, self.shortDescription())

	def expected_answer(self):
		return self.EXPECTED_ANSWERS[self.test]

	def setUp(self):
		with open(self.filename()) as file:
			self.data = file.read()

	def test_answer(self):
		result = self.solve(self.data)
		answer = int(result.split()[0])
		self.assertEqual(answer, self.expected_answer())
