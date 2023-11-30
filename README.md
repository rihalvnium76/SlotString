# SlotString

FSM-based template string formatting classes implemented using only native Java features, with little to no counter-intuitive implicit conversion features, are used in place of `MessageFormat.format()`.

Current source code version: `0.1.0`.

## Usage

Utility source see `SlotString.java`. 

Example see `SlotStringTest.java`。

If you're not comfortable adding the entire class to your project, you can copy the source code of `SlotString.qformat()` into your class as its member method, and then change `res.setLength(0);` in the method source to `StringBuilder res = new StringBuilder(); StringBuilder key = new StringBuilder(); `

## Performance

Tested on personal computer, for reference only.

```LOG
[I][performanceOfSamePattern] elapsed time of qformat(ms): 1860
[I][performanceOfSamePattern] elapsed time of compile+format(ms): 294
[I][performanceOfDiffPattern] elapsed time of qformat(ms): 2347
[I][performanceOfDiffPattern] elapsed time of compile+format(ms): 3343
```

## License

The MIT License.