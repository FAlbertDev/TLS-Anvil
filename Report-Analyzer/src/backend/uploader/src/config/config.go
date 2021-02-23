package config

import (
	"flag"
	"fmt"
	"github.com/sirupsen/logrus"
	"os"
)

var conf *Config

type Config struct {
	DatabaseUrl string
	PreprocessingThreads int
	BasePath string
	LogLevel logrus.Level
}

func GetConfig() *Config {
	if conf == nil {
		conf = &Config{}
		conf.parse()
	}
	return conf
}

func (c *Config) parse() {
	if flag.Parsed() {
		return
	}

	flag.StringVar(&conf.DatabaseUrl, "db", "localhost:27017", "URL where the database is available at")
	flag.StringVar(&conf.BasePath, "path", "", "Scans path recurively for test results")
	flag.IntVar(&conf.PreprocessingThreads, "n", 5, "Preprocessing threads")
	debug := flag.Bool("v", false, "Set log level to debug")
	trace := flag.Bool("vv", false, "Set log level to trace")

	flag.Parse()

	if conf.BasePath == "" {
		fmt.Println("flag -path is required")
		flag.PrintDefaults()
		os.Exit(2)
	}

	if *debug {
		c.LogLevel = logrus.DebugLevel
	} else if *trace {
		c.LogLevel = logrus.TraceLevel
	} else {
		c.LogLevel = logrus.InfoLevel
	}

	return
}
