// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import jakarta.persistence.*;
import java.time.*;

/** 员工本人登记不可排班时段；不登记医疗资料或请假审批。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "shift_unavailability")
public class Unavailability {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public Long id;

  @Version public Long version;

  @Column(name = "account_id", nullable = false)
  public Long accountId;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "starts_at", nullable = false)
  public Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  public Instant endsAt;

  @Column(name = "note", nullable = false, length = 500)
  public String note;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
