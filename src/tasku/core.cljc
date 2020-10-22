(ns tasku.core
  #?(:clj (:import [clojure.lang IDeref])))

(defprotocol ReactiveQuery
  (-created [self id v])
  (-deleted [self id])
  (-updated [self id v*]))

(defprotocol QueryWatchable
  (add-query [self k query])
  (remove-query [self k]))

(defprotocol DB
  (create-entity! [db kvs])
  (delete-entity! [db id])
  (update-entity! [db id f]))

(deftype TaskuDB [id-counter entities watchers]
  QueryWatchable
  (add-query [_ k query] (swap! watchers assoc k query))
  (remove-query [_ k] (swap! watchers dissoc k))

  DB
  (create-entity! [_ kvs]
    (assert (not (contains? kvs ::id)))
    (let [id (swap! id-counter inc)
          entity (assoc kvs ::id id)]
      (swap! entities assoc id entity)
      (doseq [watcher @watchers]
        (-created watcher id entity))))

  (delete-entity! [_ id]
    (assert (contains? @entities id))
    (swap! entities dissoc id)
    (doseq [watcher @watchers]
      (-deleted watcher id)))

  (update-entity! [_ id f]
    (assert (contains? @entities id))
    (let [entities* (swap! entities update id (fn [entity]
                                                (let [entity* (f entity)]
                                                  (assert (= (::id entity*) (::id entity)))
                                                  entity*)))
          entity* (get entities* id)]
      (doseq [watcher @watchers]
        (-updated watcher id entity*))))

  #?@(:clj
      [IDeref
       (deref [_] @entities)]

      :cljs
      [IDeref
       (-deref [_] @entities)]))

(defn create-db [] (TaskuDB. (atom 0) (atom {}) (atom {})))

(deftype Filter [pred src index watchers]
  QueryWatchable
  (add-query [self k query]
    (when (empty? @watchers)
      (reset! index (into {} (filter (comp pred val)) @src))
      (add-watch src self self))
    (swap! watchers assoc k query))

  (remove-query [self k]
    (let [watchers (swap! watchers dissoc k)]
      (when (empty? watchers)
        (remove-query src self))))

  ReactiveQuery
  (-created [self id v]
    (when (pred v)
      (swap! index assoc id v)
      (doseq [watcher @watchers]
        (-created watcher id v))))

  (-deleted [self id]
    (when (contains? @index id)
      (swap! index dissoc id)
      (doseq [watcher @watchers]
        (-deleted watcher id))))

  (-updated [self id v*]
    (if (pred v*)
      (do (swap! index assoc id v*)
          (doseq [watcher @watchers]
            (-updated watcher id v*)))
      (when (contains? @index id)
        (swap! index dissoc id v*)
        (doseq [watcher @watchers]
          (-updated watcher id v*)))))

  #?@(:clj
      [IDeref
       (deref [_]
         (if (seq @watchers)
           @index
           (reset! index (into {} (filter (comp pred val)) @src))))]

      :cljs
      [IDeref
       (-deref [_]
               (if (seq @watchers)
                 @index
                 (reset! index (into {} (filter (comp pred val)) @src))))]))

(defn filterq [pred src]
  (Filter. pred src (atom nil) (atom {})))
