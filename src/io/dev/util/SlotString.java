package io.dev.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

// @formatter:off
/**
 * 将模板字符串中的占位符（${var}、#{var}、{var}）替换为替换表中键为 var 的值.<br>
 * 替换表中的值默认使用 toString() 转为字符串，BigDecimal 类型的值会使用 toPlainString() 转换，null 值会被转为 "".<br>
 * 支持使用 \ 转义符号为普通文字.<br>
 * 使用不支持的语法会导致非预期的解析.
 */
//@formatter:on
public class SlotString {
  /**
   * 结果缓冲区.
   */
  private StringBuilder res;
  /**
   * 变量名缓冲区.
   */
  private StringBuilder key;
  /**
   * 编译的字符串.
   */
  private String[] parts;
  /**
   * 编译的字符串的类型。0: 普通文本; 1: 变量名.
   */
  private int[] types;

  /**
   * 初始化共用的缓冲区.
   */
  public SlotString() {
    res = new StringBuilder();
    key = new StringBuilder();
  }

  /**
   * 初始化共用的缓冲区并编译模板字符串.
   * 
   * @param pattern 模板字符串.
   */
  public SlotString(String pattern) {
    this();
    compile(pattern);
  }

  /**
   * 将模板字符串中的占位符按替换表替换为值.<br>
   * 此方法不进行预编译模板字符串，而直接解析.<br>
   * 适用于动态的模板字符串.
   * 
   * @param pattern 模板字符串
   * @param dest    替换表
   * @return 输出字符串.
   */
  public String qformat(String pattern, Map<String, Object> dest) {
    if (pattern == null || pattern.isEmpty()) {
      return pattern;
    }
    // initialize buffers
    res.setLength(0);
    int state = 0;
    char prev = 0;
    for (int i = 0; i < pattern.length(); ++i) {
      char c = pattern.charAt(i);
      if (state == 0) {
        if (c == '#' || c == '$') {
          state = 1;
          prev = c;
        } else if (c == '{') {
          state = 2;
          key.setLength(0);
        } else if (c == '\\') {
          state = 3;
        } else {
          res.append(c);
        }
      } else if (state == 1) {
        if (c == '{') {
          state = 2;
          key.setLength(0);
        } else {
          state = 0;
          res.append(prev);
          --i; // re-parse
        }
      } else if (state == 2) {
        if (c == '}') {
          state = 0;
          Object val = null;
          if (dest != null) {
            val = dest.get(key.toString());
          }
          res.append(asString(val, false));
        } else if (c == '\\') {
          state = 4;
        } else {
          key.append(c);
        }
      } else if (state == 3) {
        state = 0;
        res.append(c);
      } else if (state == 4) {
        state = 2;
        key.append(c);
      }
    }
    return res.toString();
  }

  /**
   * 预编译模板字符串.<br>
   * 适用于固定的模板字符串.
   * 
   * @param pattern 模板字符串.
   */
  public SlotString compile(String pattern) {
    if (pattern == null || pattern.isEmpty()) {
      parts = new String[] { "" };
      types = new int[] { 0 };
      return this;
    }
    ArrayList<String> parts0 = new ArrayList<>();
    ArrayList<Integer> types0 = new ArrayList<>();
    res.setLength(0);
    int state = 0;
    char prev = 0;
    for (int i = 0; i < pattern.length(); ++i) {
      char c = pattern.charAt(i);
      if (state == 0) {
        if (c == '#' || c == '$') {
          state = 1;
          prev = c;
        } else if (c == '{') {
          state = 2;
          key.setLength(0);
          if (res.length() != 0) {
            parts0.add(res.toString());
            types0.add(0);
            res.setLength(0);
          }
        } else if (c == '\\') {
          state = 3;
        } else {
          res.append(c);
        }
      } else if (state == 1) {
        if (c == '{') {
          state = 2;
          key.setLength(0);
          if (res.length() != 0) {
            parts0.add(res.toString());
            types0.add(0);
            res.setLength(0);
          }
        } else {
          state = 0;
          res.append(prev);
          --i; // re-parse
        }
      } else if (state == 2) {
        if (c == '}') {
          state = 0;
          parts0.add(key.toString());
          types0.add(1);
        } else if (c == '\\') {
          state = 4;
        } else {
          key.append(c);
        }
      } else if (state == 3) {
        state = 0;
        res.append(c);
      } else if (state == 4) {
        state = 2;
        key.append(c);
      }
    }
    if (res.length() != 0) {
      parts0.add(res.toString());
      types0.add(0);
    }
    parts = parts0.toArray(new String[parts0.size()]);
    types = types0.stream().mapToInt(Integer::valueOf).toArray();
    return this;
  }

  /**
   * 将模板字符串中的占位符按替换表替换为值.<br>
   * 此方法使用预编译的模板字符串进行处理.<br>
   * 如果没有进行过预编译，则本方法返回 null.
   * 
   * @param dest 替换表
   * @return 输出字符串.
   */
  public String format(Map<String, Object> dest) {
    if (parts == null || types == null) {
      return null;
    }
    res.setLength(0);
    for (int i = 0; i < types.length; ++i) {
      int type = types[i];
      if (type == 0) {
        res.append(parts[i]);
      } else if (type == 1) {
        Object val = null;
        if (dest != null) {
          val = dest.get(parts[i]);
        }
        res.append(asString(val, false));
      }
    }
    return res.toString();
  }

  /**
   * 将替换表中的值转换为字符串.<br>
   * 如果需要扩展此方法支持的转换类型，可以子类重写此方法，并调用 {@code super.asString(val, true)}，<br>
   * 之后该方法会在原调用 toString() 的地方返回 null.<br>
   * 
   * @param val 值
   * @param preventDefault 是否阻止默认的 toString() 转换.
   * @return 值的字符串形式.
   */
  protected String asString(Object val, boolean preventDefault) {
    if (val == null) {
      return "";
    } else if (val instanceof BigDecimal) {
      return ((BigDecimal) val).toPlainString();
    }
    if (preventDefault) {
      return null;
    } else {
      return val.toString();
    }
  }
}
