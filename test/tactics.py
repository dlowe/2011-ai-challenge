#!/usr/bin/env python

import sys
import unittest
from . import TestTactic

class TestAnnihilation(TestTactic, unittest.TestCase):
    """Ants shouldn't crash into each other!"""

    map = """
          %%%%%%%%%%%%
          %a.A%%%%%%B%
          %%%%%%%%%%%%
          """

    def assertions(self, game):
        self.assertEquals(game['replaydata']['cutoff'], 'turn limit reached')
        self.assertEquals(game['status'][0], 'survived')

class TestGreedyAnnihilation(TestTactic, unittest.TestCase):
    """Ants shouldn't crash into each other in the hunt for food!"""

    map = """
          %%*%%%%%%%%
          %a.A%%%%%B%
          %%%%%%%%%%%
          """

    def assertions(self, game):
        self.assertEquals(game['replaydata']['cutoff'], 'turn limit reached')
        self.assertEquals(game['status'][0], 'survived')

if __name__ == '__main__':
    unittest.main(argv=sys.argv)
