#!/usr/bin/env python

import tempfile
import shutil
import json
import subprocess

class TestTactic(object):
    """Takes care of the plumbing in running tactical simulations through playgame.py.
       Create a tactical test scenario by adding a class in this file which:
        1. subclasses TestTactic
        2. subclasses unittest.TestCase
        3. has a 'map' attribute
           (see http://aichallenge.org/specification.php#Map-Format)
        4. has an 'assertions' method taking a hydrated game replay as argument
           (see http://aichallenge.org/specification.php#Replay-Format)
        The assertions() method should
    """

    def test(self):
        mapdata = [ x.strip() for x in self.map.strip().split("\n") if x ]
        rows = len(mapdata)
        columns = len(mapdata[0])
        for row in mapdata:
            assert len(row) == columns

        map_file = tempfile.NamedTemporaryFile()
        with map_file:
            map_file.write("rows %d\n" % rows)
            map_file.write("cols %d\n" % columns)
            map_file.write("players 2\n") ## XXX: yeah, yeah
            for row in mapdata:
                map_file.write("m %s\n" % row)
            map_file.flush()

            log_dir = tempfile.mkdtemp()

            try:
                subprocess.call('(cd ./tools; python ./playgame.py --scenario --food none --log_dir %s --turns 30 --map_file %s "java clojure.main ../MyBot.clj" "python submission_test/TestBot.py" --nolaunch -e --strict --capture_errors)' % (log_dir, map_file.name), shell=True)
                self.game = json.loads(open("%s/0.replay" % log_dir).read())
                # print json.dumps(self.game, sort_keys=True, indent=4)
            finally:
                shutil.rmtree(log_dir)

        self.assertions(self.game)

    def assertFoodEaten(self, food_row, food_col, turn=0, player=0):
        for f in (self.game['replaydata']['food']):
            if f[0] == food_row and f[1] == food_col:
                self.assertEquals(f[3], turn, "food (%d, %d) eaten on turn %d" % (food_row, food_col, turn))
                self.assertEquals(f[4], player, "food (%d, %d) eaten by player %d" % (food_row, food_col, player))
                return
        self.assert_(False, "no food at (%d, %d)" % (food_row, food_col))

    def assertSurvived(self, player=0):
        self.assertEquals(self.game['status'][player], 'survived')
