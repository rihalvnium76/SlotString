# SlotString

FSM-based template string formatting classes implemented using only native Java features, with little to no counter-intuitive implicit conversion features, are used in place of `MessageFormat.format()`.

Current source code version: `0.2.0`.

## Usage

Utility source see `SlotString.java`. 

Example see `SlotStringTest.java`。

~~If you're not comfortable adding the entire class to your project, you can copy the source code of `SlotString.qformat()` into your class as its member method, and then change `res.setLength(0);` in the method source to `StringBuilder res = new StringBuilder(); StringBuilder key = new StringBuilder(); `~~

## Performance

500000 loops are tested on personal computer, for reference only.

See `SlotStringTest.testPerformance()`

```LOG
[I][testPerformance] Starting Performance Test (Loop Times: 500000)
[I][testPerformance:samePattern] Elapsed time of shared buffer qformat (ms): 6643
[I][testPerformance:samePattern] Elapsed time of discrete buffer qformat (ms): 7550
[I][testPerformance:samePattern] Elapsed time of compile+format (ms): 2159
[I][testPerformance:diffPattern] Elapsed time of shared buffer qformat (ms): 8075
[I][testPerformance:diffPattern] Elapsed time of discrete buffer qformat (ms): 9136
[I][testPerformance:diffPattern] Elapsed time of compile+format (ms): 10057
[I][testPerformance:ratio] Elapsed time ratio for Shared-Buffer qformat, Discrete-Buffer qformat and compile+format:
  Same Pattern: 0.8798675496688741, 1.0, 0.28596026490066223
  Diff Pattern: 0.8029233369792185, 0.9084219946306056, 1.0
```

## License

The MIT License.