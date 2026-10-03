// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import jakarta.persistence.*;
import java.time.*;

/** 发布的计划班次与当前指派，确认仅代表员工知悉，不代表出勤。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "scheduled_shift")
public class Shift {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public Long id;

  @Version public Long version;

  @Column(name = "post_id", nullable = false)
  public Long postId;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "employee_id", nullable = false)
  public Long employeeId;

  @Column(name = "title", nullable = false, length = 160)
  public String title;

  @Column(name = "category", nullable = false, length = 60)
  public String category;

  @Column(name = "note", nullable = false, length = 2000)
  public String note;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "starts_at", nullable = false)
  public Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  public Instant endsAt;

  @Column(name = "published_at")
  public Instant publishedAt;

  @Column(name = "acknowledged_at")
  public Instant acknowledgedAt;

  @Column(name = "assignment_revision", nullable = false)
  public int assignmentRevision;

  @Column(name = "rest_gap_hours", nullable = false)
  public int restGapHours;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;
}
