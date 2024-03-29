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

class TestDoubleGreed(TestTactic, unittest.TestCase):
    """In the absence of other inputs, ants should gather food efficiently, even if they have
       to move in opposite directions!"""

    map = """
          %%%%%%%%%%%%%%%%%%%%
          %*..A.a.....*%%%%%B%
          %%%%%%%%%%%%%%%%%%%%
          """

    def assertions(self, game):
        ## no dying!
        self.assertSurvived()
        self.assertEquals(game['replaydata']['cutoff'], 'turn limit reached')

        ## food are reachable in 2 and 3 moves, respectively
        self.assertFoodEaten(1, 1, turn=2)
        self.assertFoodEaten(1, 12, turn=5)

class TestShitloadsOfFoodAndAnts(TestTactic, unittest.TestCase):
    """Ants should be able to gather food efficiently even when there's lots of it and lots of them."""

    repeat = 60
    map = ('%%%%' * repeat + '%%%%%%\n'
         + '%*.A' * repeat + '%%%B%%\n'
         + '%%%%' * repeat + '%%%%%%\n')

    def assertions(self, game):
        ## no dying!
        self.assertSurvived()
        self.assertEquals(game['replaydata']['cutoff'], 'turn limit reached')

        for x in range(self.repeat):
            self.assertFoodEaten(1, x * 4 + 1, turn=1)

class TestBlockedGreed(TestTactic, unittest.TestCase):
    """Should be able to get around obstacles."""

    map = """
          %%%%%%%%%%%%
          %...*%%%%%B%
          %.%%%%%%%%%%
          %...A%%%%%%%
          %%%%%%%%%%%%
          """

    def assertions(self, game):
        ## no dying!
        self.assertSurvived()
        self.assertEquals(game['replaydata']['cutoff'], 'turn limit reached')

        ## food is reachable in 7 moves
        self.assertFoodEaten(1, 4, turn=7)

class TestNotOverlyPolite(TestTactic, unittest.TestCase):
    """Shouldn't wait an extra turn for same-team ants to move out of the way."""

    map = """
          %%%%%%%%%
          %%%*%%%%%
          %%%.%%%%%
          %Aa.%%%%%
          %%%.%%%%%
          %%%.%%%B%
          %%%*%%%%%
          %%%%%%%%%
          """

    def assertions(self, game):
        ## no dying!
        self.assertSurvived()
        self.assertEquals(game['replaydata']['cutoff'], 'turn limit reached')

        ## food are reachable in 
        self.assertFoodEaten(1, 3, turn=2)
        self.assertFoodEaten(6, 3, turn=4)

class TestRazing(TestTactic, unittest.TestCase):
    """In the absence of other inputs, aggressively raze enemy hills!"""

    map = """
          %%%%%%%%%%%
          %A....%%%b%
          %.....%%%%%
          %....1%%%%%
          %%%%%%%%%%%
          """

    def assertions(self, game):
        ## no dying!
        self.assertSurvived()

        ## the enemy hill is reachable in 6 moves
        self.assertEquals(game['replaydata']['cutoff'], 'rank stabilized')
        self.assertEquals(game['game_length'], 6)

class TestHoldGrudge(TestTactic, unittest.TestCase):
    """Don't forget where hills are when they are out of sight."""

    map = """
          %%%%%%%%%%%%%%%%%%%%%%%%%%%%%
          %%%%%.A.............a..1%%%%%
          %%%%%%%%%%%%%%%%%%%%%%%%%%%%%
          %%%%%%%%%%%%%%%%%%%%%b%%%%b%%
          """

    def assertions(self, game):
        ## no dying!
        self.assertSurvived()

        ## the closer ant is killed trying to raze the hill...
        ## but though it's out of sight, the second ant still remembers where it is and goes
        ## to raze it efficiently.
        self.assertEquals(game['replaydata']['cutoff'], 'rank stabilized')
        self.assert_(game['game_length'] <= 20)

if __name__ == '__main__':
    unittest.main(argv=sys.argv)
