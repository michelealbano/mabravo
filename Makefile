VER=1.1.0

.PHONY: all clean

all: clean
	javac -classpath code `find code -name "*.java"`
	cd code && jar cf mabravo-$(VER).jar `find -name "*.class"`
	mv code/mabravo-$(VER).jar .

clean:
	rm -f `find -name "*.class"`
	rm -f mabravo-$(VER).jar
