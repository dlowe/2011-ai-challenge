#!/usr/bin/env python

import unittest
from . import TestTactic

class TestAnnihilation(TestTactic, unittest.TestCase):
    """Ants shouldn't crash into each other!"""

    map = """
          %%%%%%%%%%%%
          %0.0%%%%%%1%
          %%%%%%%%%%%%
          """

    def assertions(self, game):
        self.assertEquals(game['status'][0], 'survived')

if __name__ == '__main__':
    unittest.main()
