# Data Model

## Records

* A record consists of key-value pairs
* Two kinds of keys
    - Regular keys
    - System keys
        * __id -- synthetic primary key
        * __type -- Type of the record
* Key is really a reference to an attribute

## Attributes

* __id
* __type -- reference to the Attribute type
* __name -- human-readable name
* __valType -- type of values

## Types

* Everything can be dumped to JSON
* Enforced schema, but records can have extra attributes
* Attributes as in Datomic
* Allow foreign keys, normalization

* I32
* F64
* Bool
* String
* Ref T -- foreign key
* {attr-id} -- embedded record, a set of attributes, subtyping semantics
* [T] -- embedded array
* Type -- type of types
* Alias id -- reference to type, so types can be shared

# Durability

* Append-only log in IndexedDB
* Full history is retained on disk
* Current state only in memory
    - Could save snapshots to disk to avoid recomputation lag on load

# Reactivity

* Queries are stream-like; recomputed on writes
* Integrate with nilern/mistletoe
* At least by-id and filter can only update when write is relevant
* Queries will load through refences automatically, either by
    - ML-like explicit refs, reproducing cycles
    - Covering DAG with no explicit refs except cycle markers
    - Covering tree with no explicit refs except for repetitions

# Distribution

* Value consistency over availability
* Use Raft protocol over WebRTC

