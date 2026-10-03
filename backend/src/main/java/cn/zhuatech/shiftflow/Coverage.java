// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import jakarta.persistence.*;
import java.time.*;

/** 员工申请由指定同事替班，原安排保留至独立主管批准。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "shift_coverage")
public class Coverage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public Long id;

  @Version public Long version;

  @Column(name = "shift_id", nullable = false)
  public Long shiftId;

  @Column(name = "requester_id", nullable = false)
  public Long requesterId;

  @Column(name = "target_id", nullable = false)
  public Long targetId;

  @Column(name = "assignment_revision", nullable = false)
  public int assignmentRevision;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "note", nullable = false, length = 1000)
  public String note;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;
}
