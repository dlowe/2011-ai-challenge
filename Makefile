ZIP   := 2011-ai-challenge.zip
FILES := MyBot.clj ants.clj

$(ZIP): test $(FILES)
	zip $@ $(FILES)

.PHONY: clean
clean:
	rm -f $(ZIP)

.PHONY: test
test:
	CLASSPATH=$(CLASSPATH):`pwd`
	(cd ./tools; ./test_bot.sh "java clojure.main ../MyBot.clj")
	python -m test.tactics

.PHONY: repl
repl:
	CLASSPATH=$(CLASSPATH):`pwd`
	java clojure.main -i ants.clj -e "(in-ns 'ants)" -r

.PHONY: smoke
smoke:
	CLASSPATH=$(CLASSPATH):`pwd`
	(cd ./tools; ./playgame.py --player_seed 42 --end_wait=0.25 --verbose --log_dir game_logs --turns 1000 --map_file maps/maze/maze_04p_01.map "java clojure.main ../MyBot.clj" "python sample_bots/python/LeftyBot.py" "python sample_bots/python/HunterBot.py" "python sample_bots/python/GreedyBot.py")
