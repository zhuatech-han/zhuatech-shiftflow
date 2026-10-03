// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import java.time.*;

/** 排班时长、有效日期及相邻时段规则；内部计划限制不代表劳动合规。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
public final class RosterPolicy {
  private RosterPolicy() {}

  /** 未来班次最长十二小时，精确到秒，开始不得超出登记窗口。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static void times(Instant start, Instant end, Instant now, int horizon) {
    if (start == null
        || end == null
        || !end.isAfter(start)
        || Duration.between(start, end).compareTo(Duration.ofHours(12)) > 0)
      throw new Problem(400, "INVALID_TIME");
    if (start.isBefore(now) || start.isAfter(now.plusSeconds(horizon * 86400L)))
      throw new Problem(400, "SCHEDULE_WINDOW");
  }

  /** 半开区间相邻不重叠，间隔小于配置值时也冲突。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static boolean conflicts(Instant a, Instant b, Instant c, Instant d, int gap) {
    return a.isBefore(d.plusSeconds(gap * 3600L)) && c.isBefore(b.plusSeconds(gap * 3600L));
  }

  /** 资格须覆盖班次涉及的全部上海日期，结束瞬间不占下一个日期。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static boolean covers(LocalDate from, LocalDate until, Instant start, Instant end) {
    return !start.atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().isBefore(from)
        && !end.minusNanos(1).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().isAfter(until);
  }
}
