// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
export const states = {
  DRAFT: ["草稿", "Draft"],
  PUBLISHED: ["已发布", "Published"],
  CANCELLED: ["已取消", "Cancelled"],
  REQUESTED: ["待对方接受", "Awaiting acceptance"],
  ACCEPTED: ["待主管批准", "Awaiting approval"],
  APPROVED: ["已批准", "Approved"],
  REJECTED: ["已拒绝", "Rejected"],
  WITHDRAWN: ["已撤回", "Withdrawn"],
  STALE: ["已失效", "Stale"],
  ACTIVE: ["有效", "Active"],
};
export const commands = {
  publish: ["发布班次", "Publish shift"],
  acknowledge: ["确认知悉", "Acknowledge"],
  cancel: ["取消班次", "Cancel shift"],
  reassign: ["改派员工", "Reassign"],
  request: ["申请替班", "Request coverage"],
  accept: ["接受替班", "Accept coverage"],
  approve: ["批准替班", "Approve coverage"],
  reject: ["拒绝申请", "Reject request"],
  withdraw: ["撤回申请", "Withdraw request"],
};
export const labels = {
  title: ["班次名称", "Shift title"],
  postId: ["岗位", "Post"],
  employeeId: ["排班员工", "Assigned employee"],
  accountId: ["员工", "Employee"],
  targetId: ["替班同事", "Covering colleague"],
  category: ["班次类型", "Shift type"],
  startsAt: ["开始（上海时区）", "Start (Shanghai time)"],
  endsAt: ["结束（上海时区）", "End (Shanghai time)"],
  validFrom: ["资格生效日期", "Valid from"],
  validUntil: ["资格截止日期", "Valid until"],
  note: ["说明", "Note"],
  code: ["编号", "Code"],
  name: ["名称", "Name"],
  nameEn: ["英文名称", "English name"],
  departmentId: ["所属部门", "Department"],
  enabled: ["启用", "Enabled"],
  username: ["登录账号", "Username"],
  displayName: ["显示名称", "Display name"],
  password: ["初始或重置密码", "Initial / reset password"],
  roleId: ["角色", "Role"],
  scope: ["数据范围", "Scope"],
  permissions: ["权限", "Permissions"],
  permissionCode: ["菜单所需权限", "Required permission"],
  position: ["顺序", "Order"],
  type: ["字典类型", "Dictionary type"],
  value: ["参数值", "Value"],
  oldPassword: ["当前密码", "Current password"],
  newPassword: ["新密码", "New password"],
};
export const errors = {
  UNAUTHENTICATED: ["会话失效，请重新登录", "Session expired. Sign in."],
  LOGIN_FAILED: [
    "账号、密码错误或账号停用",
    "Invalid credentials or disabled account.",
  ],
  LOGIN_THROTTLED: ["请五分钟后重试", "Retry in five minutes."],
  FORBIDDEN: ["没有操作权限", "Permission denied."],
  OUT_OF_SCOPE: ["没有此记录的数据权限", "Outside your data scope."],
  NOT_OWNER: ["仅本人可操作", "Owner only."],
  NOT_ASSIGNEE: ["仅当前排班员工可操作", "Current assignee only."],
  NOT_TARGET: ["仅指定同事可接受", "Designated colleague only."],
  NOT_REQUESTER: ["仅申请人可撤回", "Requester only."],
  INDEPENDENT_REVIEW: [
    "须由申请人与替班人之外的主管复核",
    "Independent approver required.",
  ],
  STALE_VERSION: ["记录已更新，请刷新后操作", "Record changed. Refresh first."],
  INVALID_STATE: [
    "状态不支持此操作，请刷新",
    "State does not allow this action.",
  ],
  INVALID_ACTION: ["不支持的操作", "Unknown action."],
  INVALID_TIME: [
    "结束须晚于开始；班次最长12小时，不可排班最长31天",
    "End must follow start; shifts max 12h, unavailability max 31 days.",
  ],
  SCHEDULE_WINDOW: [
    "班次须在未来且不超过登记窗口",
    "Shift must start within the future horizon.",
  ],
  TOO_LATE: ["已超过操作时间", "Action window ended."],
  ALREADY_ACKNOWLEDGED: ["已经确认知悉", "Already acknowledged."],
  INVALID_EMPLOYEE: [
    "员工须启用、具备所需权限并属于岗位部门",
    "Employee must be active, authorized and in the post department.",
  ],
  POST_DISABLED: ["岗位已停用", "Post is disabled."],
  NOT_QUALIFIED: [
    "员工的有效资格未覆盖整个班次",
    "Qualification does not cover the whole shift.",
  ],
  UNAVAILABLE: ["班次与不可排班时段冲突", "Employee is unavailable."],
  SHIFT_CONFLICT: [
    "员工班次重叠或相邻间隔不足",
    "Overlapping shifts or insufficient gap.",
  ],
  PUBLISHED_CONFLICT: [
    "须先处理已发布班次，才能修改此资料",
    "Resolve published assignments before this change.",
  ],
  ACTIVE_REQUEST: [
    "已有待处理的替班申请",
    "An active coverage request exists.",
  ],
  REQUEST_STALE: [
    "原班次已变更，请刷新",
    "Original assignment changed. Refresh.",
  ],
  HISTORY_PROTECTED: ["已被业务引用，须保留历史", "History is protected."],
  IDEMPOTENCY_CONFLICT: [
    "重试内容不同，请重新打开操作",
    "Retry content changed. Reopen action.",
  ],
  INVALID_REQUEST_KEY: ["请重新打开操作", "Reopen action."],
  INVALID_DICTIONARY: ["班次类型不存在", "Unknown shift type."],
  INVALID_INPUT: [
    "请检查必填项、长度和时间",
    "Check required fields, lengths and times.",
  ],
  CONFLICT: ["编号重复或记录仍被使用", "Duplicate code or referenced record."],
  LAST_ADMIN: [
    "须保留一个启用的全范围管理员",
    "Keep an active all-data administrator.",
  ],
  WEAK_PASSWORD: [
    "密码至少12位，含大写、小写及数字，UTF-8不超过72字节",
    "Use 12+ characters, upper/lower case, digits and at most 72 UTF-8 bytes.",
  ],
  OLD_PASSWORD_INVALID: ["当前密码错误", "Incorrect current password."],
  BUILTIN_RESOURCE: [
    "内建目录不可删除或修改代码",
    "Built-in catalog is protected.",
  ],
  NOT_FOUND: ["记录不存在", "Record not found."],
  REPORT_LIMIT: [
    "超过一万条记录，请缩小范围",
    "More than 10,000 records. Narrow scope.",
  ],
  INVALID_SETTING: [
    "时区固定；窗口1–180天，相邻间隔0–24小时",
    "Fixed timezone; horizon 1–180 days, rest gap 0–24 hours.",
  ],
};
/** 以固定上海时区显示日期时间，不依赖浏览器所在时区。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function date(value) {
  return value
    ? new Intl.DateTimeFormat("zh-CN", {
        timeZone: "Asia/Shanghai",
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";
}
/** 将服务器 UTC 时间转为上海本地表单字符串。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function localInput(value) {
  if (!value) return "";
  return new Date(new Date(value).getTime() + 8 * 3600000)
    .toISOString()
    .slice(0, 16);
}
/** 上海表单时间转为带时区 ISO 时间，拒绝非法或缺失输入。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function instant(value) {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value))
    throw new Error("INVALID_TIME");
  const d = new Date(value + ":00+08:00");
  if (!Number.isFinite(d.getTime()) || localInput(d.toISOString()) !== value)
    throw new Error("INVALID_TIME");
  return d.toISOString();
}

/** 班次操作入口；权限最终由接口验证。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function actions(s, me, now) {
  if (!s || !me) return [];
  const p = me.permissions;
  const scope =
    me.scope === "ALL" ||
    (me.scope === "DEPARTMENT" && me.departmentId === s.departmentId);
  const future = new Date(s.startsAt) > new Date(now);
  let out = [];
  if (p.includes("schedule.manage") && scope) {
    if (s.status === "DRAFT" && future) out.push("publish");
    if (s.status === "PUBLISHED" && future) out.push("reassign", "cancel");
  }
  if (s.status === "PUBLISHED" && s.employeeId === me.id) {
    if (
      !s.acknowledgedAt &&
      new Date(s.endsAt) > new Date(now) &&
      p.includes("roster.own")
    )
      out.push("acknowledge");
    if (future && p.includes("cover.write")) out.push("request");
  }
  return out;
}
/** 替班入口排除本人复核，过期仍可拒绝或撤回。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function coverActions(c, s, me, now) {
  if (!c || !me || !["REQUESTED", "ACCEPTED"].includes(c.status)) return [];
  const p = me.permissions;
  let out = [];
  const future = new Date(s.startsAt) > new Date(now);
  if (c.targetId === me.id && p.includes("cover.write")) {
    if (c.status === "REQUESTED" && future) out.push("accept");
    out.push("reject");
  }
  if (c.requesterId === me.id && p.includes("cover.write"))
    out.push("withdraw");
  if (
    p.includes("cover.approve") &&
    c.requesterId !== me.id &&
    c.targetId !== me.id &&
    (me.scope === "ALL" ||
      (me.scope === "DEPARTMENT" && me.departmentId === s.departmentId))
  ) {
    if (c.status === "ACCEPTED" && future) out.push("approve");
    out.push("reject");
  }
  return out;
}
