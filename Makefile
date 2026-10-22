JAVAC ?= javac
JAVA ?= java
SRC := src/main/java
TEST := src/test/java
OUT := build

.PHONY: all test clean

all:
	mkdir -p $(OUT)
	$(JAVAC) -d $(OUT) $(shell find $(SRC) -name '*.java')

test:
	mkdir -p $(OUT)
	$(JAVAC) -d $(OUT) $(shell find $(SRC) $(TEST) -name '*.java')
	$(JAVA) -cp $(OUT) cronexpr.CronExpressionTest

clean:
	rm -rf $(OUT)
