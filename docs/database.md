知华科技（上海如静知华信息科技有限公司） · 官网 https://www.zhuatech.cn/ · 商业授权及定制开发微信 zhuatech / zhuatech2。

# 数据库与迁移

数据库 zhuatech_shiftflow，MySQL 8.4。V1__identity.sql 创建 department、access_role、role_permission、account、permission、nav_menu、dictionary_entry、system_setting、audit_event；V2__roster.sql 创建 shift_post、shift_qualification、scheduled_shift、shift_coverage、shift_unavailability、roster_event、roster_command。

外键保护部门、人员、岗位、班次及事件关联；资格 account/post 唯一，岗位 code 唯一，幂等 actor/request_key 唯一。时间、日期和间隔的基本约束在表中，跨对象权限及时间冲突由加锁事务校验。Shift、Coverage、Qualification、Post和Unavailability使用 version 字段。RosterEvent 和审计是追加记录，Account散列不序列化。

Bootstrap 只在没有账号时初始化总部、管理员/员工/排班员/主管四个角色、11项权限、15个菜单、REGULAR/EXTRA/OTHER字典和四个参数。管理员密码来自 ADMIN_PASSWORD，BCrypt强度12；不保存明文密码，不创建业务示例。Flyway保存校验历史。初始业务为空；测试脚本产生的数据仅在独立验收卷。

升级先对项目专属数据卷和配置备份并在副本演练，新增迁移，保持 V1/V2 不变。禁止修改数据库历史或关闭实体校验来掩盖错误。已有账号时环境 ADMIN_PASSWORD 的变化不会改变密码；管理员需通过已授权管理流程处理账号。
