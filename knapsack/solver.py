#! /usr/bin/env python

"""Provide solveIt() hook for the class's submit.pyc script. Feed data on stdin."""


from __future__ import print_function, unicode_literals

import subprocess
import tempfile
import sys
import glob
import os

JAVA = ['java']
JAVAC = ['javac']
SOLVER = 'Knapsack'


def printerr(*args, **kwargs):
	"Print to stderr."
	print(*args, file=sys.stderr, **kwargs)


def screenwidth():
	"Return screen width. In a function in case you get fancy."
	return 80


def solveIt(data, time=False):
	"""Hook for class submission script.

	Presumably `data` is a string containing the contents of an input file as
	described in the instructions. This function should return a string with the
	output format described there too.

	However, really all we're doing is passing `data` to Java and returning what
	we get back.
	"""
	printerr('Compiling...')
	subprocess.check_call(JAVAC + [SOLVER + '.java'])
	data_file = tempfile.TemporaryFile()
	data_file.write(data.encode('ascii'))
	data_file.seek(0)
	cmd = JAVA + [SOLVER] #, '<', data_file.name]
	if time:
		cmd = ['time'] + cmd
	printerr('Solving...')
	result = subprocess.check_output(cmd, stdin=data_file)
	printerr('Cleaning up...')
	for filename in glob.iglob(SOLVER + '*.class'):
		os.unlink(filename)
	printerr('=' * screenwidth())
	return result.decode('ascii')


if __name__ == '__main__':
	if len(sys.argv) > 1:
		printerr('unexpected arguments; feed data on stdin')
		sys.exit(1)
	print(solveIt(sys.stdin.read(), time=True))

