Elasticsearch Fess Suggest Plugin
=======================

## Overview

Elasticsearch Fess Suggest Plugin provides flexible suggest feature.


## Version

| Taste     | Tested on Elasticsearch |
|:---------:|:-----------------------:|
| master    | 1.7.X                   |
| 1.7.2     | 1.7.1                   |

Note that this plugin supports Java 8 or the above.

### Issues/Questions

Please file an [issue](https://github.com/codelibs/elasticsearch-fess-suggest/issues "issue").
(Japanese forum is [here](https://github.com/codelibs/codelibs-ja-forum "here").)

## Installation

### Install Fess Suggest Plugin

    $ $ES_HOME/bin/plugin --install org.codelibs/elasticsearch-fess-suggest/1.7.2

Fess Suggest plugin depends on [Elasticsearch Analysis Kuromoji Neologd](https://github.com/codelibs/elasticsearch-analysis-kuromoji-neologd "Elasticsearch Analysis Kuromoji Neologd").
Please install elasticsearch-analysis-kuromoji-neologd.

## Getting Started

### Create Configurations

Fess Suggest plugin has some index to manage suggest data.
The following command creates configuration indices for "doc" suggest.

    $ curl -XPOST localhost:9200/doc/_fsuggest/create

"doc-suggest" index is created. Suggest data is stored to this index.

## Register Suggest Data

To register suggest data, send localhost:9200/{name}/_fsuggest/update/searchword by POST.
For examples, the following request stores "Foo{num} Bar" as suggest data.

    $ count=1;while [ $count -lt 10 ] ; do curl -XPOST localhost:9200/doc/_fsuggest/update/searchword -d "{\"keyword\":\"Foo$count Bar\"}";count=`expr $count + 1`;done

To register suggest data from document, send localhost:9200/{name}/fsuggest/update/document.
UpdateDocument Api register the suggest data by analyzing document.

    $ curl -XPOST localhost:9200/doc/_fsuggest/update/document -d '{"document" : "hello world.", "fields" : ["content"]}'

## Get Suggest Data

To get suggest data, send localhost:9200/{name}/_fsuggest wtih "q" parameter.

    $ curl -XGET "localhost:9200/doc/_fsuggest?q=f&pretty"
