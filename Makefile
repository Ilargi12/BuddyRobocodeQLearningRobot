OUTPUT_DIR = BuddyRobocode
ROBOCODE_ROBOTS_PATH = ${HOME}/robocode/robots

all: build package

build:
	javac -classpath "lib/*" *.java

package:
	mkdir $(OUTPUT_DIR)
	mv *.class $(OUTPUT_DIR)/

install:
	cp -p -R $(OUTPUT_DIR) $(ROBOCODE_ROBOTS_PATH)

clean:
	rm -f *.class
	rm -rf $(OUTPUT_DIR)

