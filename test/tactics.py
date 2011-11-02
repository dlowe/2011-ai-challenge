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
        self.assertSurvived()
        self.assertEquals(game['replaydata']['cutoff'], 'turn limit reached')

class TestGreedyAnnihilation(TestTactic, unittest.TestCase):
    """Ants shouldn't crash into each other in the hunt for food!"""

    map = """
          %%*%%%%%%%%
          %a.A%%%%%B%
          %%%%%%%%%%%
          """

    def assertions(self, game):
        ## no dying!
        self.assertSurvived()
        self.assertEquals(game['replaydata']['cutoff'], 'turn limit reached')

        ## still should eat the food on the first turn, though
        self.assertFoodEaten(0, 2, turn=1)

class TestGreed(TestTactic, unittest.TestCase):
    """In the absence of other inputs, ants should gather food efficiently."""

    map = """
          %%%%%%%%%%%
          %*..%%%%%B%
          %...%%%%%%%
          %..A%%%%%%%
          %%%%%%%%%%%
          """

    def assertions(self, game):
        ## no dying!
        self.assertSurvived()
        self.assertEquals(game['replaydata']['cutoff'], 'turn limit reached')

        ## food is reachable in 3 moves
        self.assertFoodEaten(1, 1, turn=3)

if __name__ == '__main__':
    unittest.main(argv=sys.argv)
