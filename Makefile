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
