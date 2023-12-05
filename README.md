# SlotString

FSM-based template string formatting classes implemented using only native Java features, with little to no counter-intuitive implicit conversion features, are used in place of `MessageFormat.format()`.

Current source code version: `0.2.1`.

## Usage

Utility source see `SlotString.java`. 

Example see `SlotStringTest.java`。

If it is not appropriate to add the whole class to your project, you can copy the source code of the `qformat` method to your class and manually inline the source code of the `parse` and `parseQformat` methods, and manually inline and adjust the multithreading related fields and judgment code according to the actual multithreading usage. If you are using it in a multi-threaded scenario, you can also move the two buffers to the class as fields and change the type to `StringBuffer`.

## Performance

500000 loops are tested on personal computer, for reference only.

See `SlotStringTest.testPerformance()`

```LOG
[I][testPerformance] Starting Performance Test (Loop Times: 500000)
[I][testPerformance:samePattern] Elapsed time of shared buffer qformat (ms): 5697
[I][testPerformance:samePattern] Elapsed time of discrete buffer qformat (ms): 6406
[I][testPerformance:samePattern] Elapsed time of compile+format (ms): 1914
[I][testPerformance:diffPattern] Elapsed time of shared buffer qformat (ms): 6867
[I][testPerformance:diffPattern] Elapsed time of discrete buffer qformat (ms): 7703
[I][testPerformance:diffPattern] Elapsed time of compile+format (ms): 8387
[I][testPerformance:ratio] Elapsed time ratio for Shared-Buffer qformat, Discrete-Buffer qformat and compile+format:
  Same Pattern: 0.8893225101467375, 1.0, 0.29878239150796126
  Diff Pattern: 0.8187671396208418, 0.9184452128293789, 1.0
```

## License

The MIT License.