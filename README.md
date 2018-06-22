# `List` implementation based on red-black tree
`TreeList<T>` is a random-access `List<T>` implementation having the operation time complexities 
as described below. It provides a trade-off between costly seek operations of `LinkedList`
and linear-time insertions and removals in non-final positions of `ArrayList`.

Given _n_ is the size of a list, all the operations concerning a single list element execute 
in _O(log(n))_ time: `get(int)`, `set(int, T)`, `add(T)`, `add(int, T)`, and `remove(int)`.

Given _m_ is the size of another collection, the `addAll(int, Collection)` operation executes 
in _O(m log(m + n))_ time unless the first argument is _0_ or _n_ (insertion as the head 
or the tail of the list), in which case the operation executes in _O(m + log(m + n))_ time.
This last time complexity naturally applies to the `addAll(Collection)` invocations.

A `TreeList` may also be populated from another collection in _O(m)_ time upon instance construction.

Given _n<sub>1</sub>_ and _n<sub>2</sub>_ are the sizes of two different `TreeList` 
instances, the lists may be concatenated using the static 
`TreeList::concat(TreeList, TreeList)` method in only 
_O(log(n<sub>1</sub> + n<sub>2</sub>))_ time, though the operation will clear 
both original lists. 
