// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import jakarta.persistence.*;
import java.time.*;

/** 人员对岗位的排班资格及有效日期，不代表法定资质证明。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "shift_qualification")
public class Qualification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public Long id;

  @Version public Long version;

  @Column(name = "account_id", nullable = false)
  public Long accountId;

  @Column(name = "post_id", nullable = false)
  public Long postId;

  @Column(name = "valid_from", nullable = false)
  public LocalDate validFrom;

  @Column(name = "valid_until", nullable = false)
  public LocalDate validUntil;

  @Column(name = "enabled", nullable = false)
  public boolean enabled;
}
