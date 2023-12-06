# SlotString

FSM-based template string formatting classes implemented using only native Java features, with little to no counter-intuitive implicit conversion features, are used in place of `MessageFormat.format()`.

Current source code version: `0.2.2`.

## Usage

Utility source see `SlotString.java`. 

Example see `SlotStringTest.java`。

If it is not appropriate to add the whole class to your project, you can copy the source code of the `qformat` method to your class and manually inline the source code of the `parse` and `parseQformat` methods, and manually inline and adjust the multithreading related fields and judgment code according to the actual multithreading usage. If you are using it in a multi-threaded scenario, you can also move the two buffers to the class as fields and change the type to `StringBuffer`.

## Performance

500000 loops are tested on personal computer, for reference only.

See `SlotStringTest.testPerformance()`

```LOG
[I][testPerformance] Starting Performance Test (Loop Times: 500000)
[I][testPerformance:samePattern] Elapsed time of shared buffer qformat (ms): 5603
[I][testPerformance:samePattern] Elapsed time of discrete buffer qformat (ms): 6318
[I][testPerformance:samePattern] Elapsed time of compile+format (ms): 1838
[I][testPerformance:diffPattern] Elapsed time of shared buffer qformat (ms): 6872
[I][testPerformance:diffPattern] Elapsed time of discrete buffer qformat (ms): 7663
[I][testPerformance:diffPattern] Elapsed time of compile+format (ms): 8496
[I][testPerformance:ratio] Elapsed time ratio for Shared-Buffer qformat, Discrete-Buffer qformat and compile+format:
  Same Pattern: 0.8868312757201646, 1.0, 0.290914846470402
  Diff Pattern: 0.8088512241054614, 0.9019538606403014, 1.0
```

## License

The MIT License.