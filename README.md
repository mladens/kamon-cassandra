Cassandra Integration   ![Build Status](https://travis-ci.org/kamon-io/kamon-cassandra.svg?branch=master)
==========================


### Metrics ###

Connection metrics
---------------------------------------
Histogram `cassandra.client.pool-borrow-time`
    Time spent waiting for a connection for an execution
    Tags
        `target` <- this one doesnt make sense

Histogram `cassandra.connection.pool.size`
    Number of active connections per node
    Tags 
        `target` <- node

Hiostogram `cassandra.trashed-connections`
    Number of thrashed connections per host
    Tags   
        `target`

Histogram `cassandra.client.inflight-per-connection`
    Distribution of in-flight requests over connections, recorded when connections is borrowed from pool

Histogram `cassandra.client.inflight-per-target`
    Distribution of in-flight requests over cluster nodes




Query metrics
---------------------------------------

Histogram `cassandra.client.query.duration`
    User observed query duration, as measured from the moment query is `executeAsync` is invoked until first page of result set is ready
    Tags
        `statement.kind` -> present only for DML statements `select` | `insert` | `update` | `delete`

Counter `cassandra.client.query.count`  //same data can be extracted from former histogram
    Counts total number of executed client queries (not including retries, speculations, fetches..)
    Tags   
        `statement.kind`

RangeSampler `cassandra.client.inflight`
    Current number of of active queries
    Tags
        `target` -> Not used? shoul dbe host, but dont have that info on session, maybe doesnt make sense
        for query (does for execution?)

Counter `cassandra.query.errors` 
    Count total number of failed executions (not neccessarily failed entire query)
    Tags    
        `target` -> target node for execution
 
Counter `cassandra.query.timeouts` 
    Count total number of timed-out executions
    Tags    
        `target` -> target node for execution
 
Counter `cassandra.query.retries`
    Count cluster-wide total number of retried exectutions
    
Counter `cassandra.query.speculative`
    Count cluster-wide total number of speculative executions (only issued queries, not measuring whether speculative won or got canceled by original response arriving)

Counter `cassandra.query.cancelled`
    Count cluster-wide totaln number of cancelled executions (including user hanging up or speculative execution getting cancelked)
    



Executor metrics:
----------------------------------------

Gauge `cassandra.queue.executor` 
    Number of queued up tasks in the main internal executor.
Gauge `cassandra.queue.blocking`
    Number of queued up tasks in the blocking executor.
Gauge `cassandra.queue.reconnection`
    Number of queued up tasks in the reconnection executor
Gauge `cassandra.scheduled-tasks`
    Number of queued up tasks in the scheduled tasks executor.




Tracing
----------------------------------------------
TODO missing operation name
Client Span is created for every `executeAsync` invocation tagged with 
- `span.kind` - `client`
- `cassandra.query` containing cql
- `cassandra.keyspace` 
- `cassandra.query.kind` for DML statements
- `cassandra.client.rs.session-id` for correlation if server tracing is enabled
- `cassandra.client.rs.cl` - achieved consistency level
- `cassandra.client.rs.fetch-size` 
- `cassandra.client.rs.fetched` - rows retrieved in current rows
- `cassandra.client.rs.has-more` - if more result set is not fully exhausted yet


As a child of each client span, one span is created per execution. These execution span can represent
initial or subsequent fetches (triggered by iterating through result set past first page). Internal retries
based on retry policy or speculative executions.
Operation name indicates the type of execution
    `cassandra.client.query.prepare|execution|speculative`

Execution spans are tagged with actual target information:
    `dc`
    `rack`
    `target`

Retried executions are additionally tagged with `retry` -> true.


Execution spon start time indicates start of execution which also includes connection pooling time,
mark `writing` is used to indicate moment connection is aquired an request gets on the wire.


