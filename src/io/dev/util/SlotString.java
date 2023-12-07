package io.dev.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模板字符串格式化器.<br>
 * <br>
 * 将模板字符串中的占位符（${var}、#{var}、{var}）替换为替换表中键为 var 的值.<br>
 * 替换表中的值默认使用 toString() 转为字符串，BigDecimal 类型的值会使用 toPlainString() 转换，null 值会被转为 "".<br>
 * 支持使用 \ 转义符号为普通文字或键.<br>
 * 使用不支持的语法会导致非预期的解析.
 */
public class SlotString {
  
  /**
   * 文本类型的字符串片段.<br>
   * 写入和比较均用同一常量可直接引用地址比较，避免拆箱.
   */
  private static final Integer TEXT_TYPE = 0;
  /**
   * 键名类型的字符串片段.<br>
   * 写入和比较均用同一常量可直接引用地址比较，避免拆箱.
   */
  private static final Integer KEY_TYPE = 1;

  /**
   * 是否编译中.
   */
  private boolean compiling;
  /**
   * 编译的字符串片段.
   */
  private List<String> parts;
  /**
   * 编译的字符串片段的类型.
   */
  private List<Integer> types;
  /**
   * 多线程支持.
   */
  private boolean multiThread;
  /**
   * 共用的结果缓冲区（仅非多线程可用）.
   */
  private StringBuilder res;
  /**
   * 共用的键名缓冲区（仅非多线程可用）.
   */
  private StringBuilder key;

  /**
   * 创建不支持多线程和编译模板字符串的格式化器.<br>
   * 适用于格式化不同的模板字符串.<br>
   * 仅 {@code qformat} 方法可用.
   */
  public SlotString() {
    this(false);
  }

  /**
   * 创建不支持编译模板字符串的格式化器，可指定是否开启多线程支持.<br>
   * 适用于格式化不同的模板字符串.<br>
   * 仅 {@code qformat} 方法可用.
   * @param multiThread 是否开启多线程支持.
   */
  public SlotString(boolean multiThread) {
    this.multiThread = multiThread;
    if (!multiThread) {
      res = new StringBuilder();
      key = new StringBuilder();
    }
  }
  
  /**
   * 创建支持多线程和编译模板符串的格式化器.<br>
   * 适用于多次格式化同一的模板字符串.<br>
   * 全部方法可用，但编译的结果仅 {@code format} 方法使用，而 {@code qformat} 方法不使用.
   * @param pattern 模板符串.
   */
  public SlotString(String pattern) {
    this(true);
    compile(pattern);
  }

  /**
   * 编译模板符串并存储编译结果.<br>
   * 用于加速 {@code format} 方法格式化.
   * @param pattern 模板符串.
   */
  protected void compile(String pattern) {
    if (pattern == null) {
      return;
    }
    compiling = true;
    StringBuilder res = new StringBuilder();
    StringBuilder key = new StringBuilder();
    parts = new ArrayList<>();
    types = new ArrayList<>();
    parse(pattern, res, key, null);
    if (res.length() != 0) {
      types.add(TEXT_TYPE);
      parts.add(res.toString());
    }
    compiling = false;
  }
  
  /**
   * 格式化编译的模板字符串.<br>
   * 模板字符串中的占位符会根据其名称被替换为替换表中对应的值.<br>
   * 未先编译模板字符串调用该方法会返回 {@code null}.
   * 
   * @param dest 占位符替换表.
   * @return 输出字符串.
   */
  public String format(Map<String, Object> dest) {
    if (parts == null || types == null) {
      return null;
    }
    StringBuilder res = new StringBuilder();
    for (int i = 0; i < types.size(); ++i) {
      Integer type = types.get(i);
      if (type == TEXT_TYPE) {
        res.append(parts.get(i));
      } else if (type == KEY_TYPE) {
        Object val = null;
        if (dest != null) {
          val = dest.get(parts.get(i));
        }
        res.append(asString(val, false));
      }
    }
    return res.toString();
  }
  
  /**
   * 不编译直接格式化传入的模板字符串.<br>
   * 模板字符串中的占位符会根据其名称被替换为替换表中对应的值.
   * 
   * @param pattern 模板字符串.
   * @param dest 占位符替换表.
   * @return 输出字符串.
   */
  public String qformat(String pattern, Map<String, Object> dest) {
    if (pattern == null || pattern.isEmpty()) {
      return pattern;
    }
    StringBuilder res;
    StringBuilder key;
    if (multiThread) {
      res = new StringBuilder();
      key = new StringBuilder();
    } else {
      res = this.res;
      key = this.key;
      res.setLength(0);
      key.setLength(0);
    }
    parse(pattern, res, key, dest);
    return res.toString();
  }
  
  /**
   * 将替换表的值转化为字符串.<br>
   * 重写该方法，并在新实现开头调用 {@code super.asString(val, true)} 可实现在原有转换规则的基础上扩展.<br>
   * 该方法 {@code preventDefault} 为 {@code true} 时，遇到非 {@code null} 和 {@code BigDecimal} 类的 {@code val} 会返回 {@code null}.
   *  
   * @param val 替换表的值
   * @param preventDefault 是否阻止默认的调用 {@code toString()} 行为.
   * @return 转换后的字符串.
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
  
  /**
   * {@code compile} 和 {@code qformat} 方法的公共解析部分.
   * 
   * @param pattern 模板字符串.
   * @param res 使用的结果缓冲区.
   * @param key 使用的键名缓冲区.
   * @param dest 占位符替换表.
   */
  private void parse(String pattern, StringBuilder res, StringBuilder key, Map<String, Object> dest) {
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
          if (compiling) {
            parseCompile(res, key);
          } else {
            parseQformat(res, key, dest);
          }
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
  }
  
  /**
   * {@code qformat} 方法的替换表值处理逻辑.
   * @param res 使用的结果缓冲区.
   * @param key 使用的键名缓冲区.
   * @param dest 占位符替换表.
   */
  private void parseQformat(StringBuilder res, StringBuilder key, Map<String, Object> dest) {
    Object val = null;
    if (dest != null) {
      val = dest.get(key.toString());
    }
    res.append(asString(val, false));
  }
  
  /**
   * {@code compile} 方法的解析结果记录逻辑.
   * @param res 使用的结果缓冲区.
   * @param key 使用的键名缓冲区.
   */
  private void parseCompile(StringBuilder res, StringBuilder key) {
    if (res.length() != 0) {
      types.add(TEXT_TYPE);
      parts.add(res.toString());
      res.setLength(0);
    }
    types.add(KEY_TYPE);
    parts.add(key.toString());
  }
}
