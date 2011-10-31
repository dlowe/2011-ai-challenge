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
                subprocess.call('(cd ./tools; python ./playgame.py --food none --log_dir %s --turns 30 --map_file %s "java clojure.main ../MyBot.clj" "python submission_test/TestBot.py" --nolaunch -e --strict --capture_errors)' % (log_dir, map_file.name), shell=True)
                game = json.loads(open("%s/0.replay" % log_dir).read())
            finally:
                shutil.rmtree(log_dir)

        self.assertions(game)
