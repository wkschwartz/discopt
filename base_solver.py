#! /usr/bin/env python

"""Provide solveIt() hook for the class's submit.pyc script. Feed data on stdin."""


from __future__ import print_function, unicode_literals

import subprocess
import tempfile
import sys
import glob
import os



def printerr(*args, **kwargs):
	"Print to stderr."
	print(*args, file=sys.stderr, **kwargs)


def screenwidth():
	"Return screen width. In a function in case you get fancy."
	return 80


class JavaSolution(object):

	JAVA = ['java']
	JAVAC = ['javac']
	SOLVER = None
	SRCGLOBS = ["*.java"]

	def __init__(self, data, time=False, assertions=False):
		self.data = str(data)
		self.time = bool(time)
		self.assertions = bool(assertions)
		try:
			self.compile()
			self.data_file = self.write_data()
			self.result = self.solve()
		finally:
			self.cleanup()

	def compile(self):
		printerr('Compiling...')
		cmd = list(self.JAVAC)
		if self.assertions:
			cmd.append('-g')
		for g in self.SRCGLOBS:
			for fn in glob.iglob(g):
				cmd.append(fn)
		subprocess.check_call(cmd)

	def write_data(self):
		"Write input string to a temporary file and leave it open."
		data_file = tempfile.TemporaryFile()
		data_file.write(self.data.encode('ascii'))
		data_file.seek(0)
		return data_file

	def get_cmd(self):
		cmd = list(self.JAVA)
		if self.assertions:
			cmd.append('-ea')
		cmd.append(self.SOLVER)
		if self.time:
			cmd = ['time'] + cmd
		return cmd

	def solve(self):
		printerr('Solving...')
		return subprocess.check_output(self.get_cmd(), stdin=self.data_file)

	def cleanup(self):
		printerr('Cleaning up...')
		if hasattr(self, 'data_file'):
			self.data_file.close()
		for g in self.SRCGLOBS:
			for filename in glob.iglob(g.replace('java', 'class')):
				os.unlink(filename)

	@classmethod
	def solveIt(cls, data, time=False, assertions=False):
		"""Hook for class submission script.

		Presumably `data` is a string containing the contents of an input file
		as described in the instructions. This function should return a string
		with the output format described there too.

		However, really all we're doing is passing `data` to Java and returning
		what we get back.
		"""
		return cls(data, time, assertions).result


def main(solver=JavaSolution):
	if len(sys.argv) > 1:
		printerr('unexpected arguments; feed data on stdin')
		sys.exit(1)
	output = solver.solveIt(sys.stdin.read(), assertions=True, time=True)
	print(output.decode('ascii'))
	printerr('=' * screenwidth())


if __name__ == '__main__':
	main()
