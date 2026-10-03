// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import jakarta.persistence.*;
import java.time.*;

/** 班次和替班不可修改事件及当时JSON快照。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "roster_event")
public class RosterEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public Long id;

  @Column(name = "shift_id", nullable = false)
  public Long shiftId;

  @Column(name = "coverage_id")
  public Long coverageId;

  @Column(name = "actor", nullable = false, length = 60)
  public String actor;

  @Column(name = "action", nullable = false, length = 40)
  public String action;

  @Column(name = "note", nullable = false, length = 1000)
  public String note;

  @Column(name = "snapshot", nullable = false, columnDefinition = "text")
  public String snapshot;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
