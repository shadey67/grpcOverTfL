# TfL gRPC Service

A Java gRPC service that wraps some of [TfL's public APIs](https://api-portal.tfl.gov.uk/).

## Features

- **Line status**: get the current status of any TfL line (unary RPC).
- **Live arrivals**: subscribe to a station and get latest arrivals board, updated every 30 seconds.
- **Shared polling**: each station has a single poller. TfL is called once per update no matter the number of clients, and the result is fanned out to every subscriber. New subscribers get the latest board immediately. Polling stops when the last one disconnects.

## Usage

### Get the status of a line

```bash
grpcurl -plaintext -d '{"line_id": "jubilee"}' \
  localhost:8081 transit.v1.TransitService/GetLineStatus
```

Line IDs are TfL's lowercase IDs, e.g. `jubilee`, `northern`, `victoria`, `central`, `hammersmith-city`, `elizabeth`.

### Subscribe to live arrivals

```bash
grpcurl -plaintext -d '{"stop_id": "940GZZLUWHP"}' \
  localhost:8081 transit.v1.TransitService/WatchArrivals
```

You'll get the current arrivals straight away, then a fresh update every 30 seconds. Arrivals are sorted soonest first.

To see shared polling, run the same command in two or three terminals. The server log shows a single TfL poll per interval rather than one poll per client.

### Example stop IDs

| Station | Stop ID |
|---|---|
| West Hampstead | `940GZZLUWHP` |
| Oxford Circus | `940GZZLUOXC` |
| King's Cross St. Pancras | `940GZZLUKSX` |
| Waterloo | `940GZZLUWLO` |
| Bank | `940GZZLUBNK` |
| Liverpool Street | `940GZZLULVT` |
| Canary Wharf | `940GZZLUCYF` |
| Victoria | `940GZZLUVIC` |

## How shared polling works

```
                        ┌──► Client A
TfL ◄── one poll ── StopFeed(940GZZLUWHP) ──► Client B
                        └──► Client C
```

- **`ArrivalsHub`** keeps one `StopFeed` per watched station. Feeds are created on the first subscribe and removed on the last unsubscribe using `ConcurrentHashMap.compute` so concurrent subscribes and unsubscribes can't create duplicate feeds or lose subscribers.
- **`StopFeed`** polls TfL every 30 seconds, caches the latest result, and sends it to each subscriber.
- **`Subscriber`** wraps one client's stream.
