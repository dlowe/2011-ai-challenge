ZIP   := 2011-ai-challenge.zip
FILES := MyBot.clj ants.clj

$(ZIP): $(FILES)
	zip $@ $(FILES)

.PHONY: clean
clean:
	rm -f $(ZIP)
