// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 空库初始化员工、排班员与主管，不创建班次或业务资料。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Component
public class Bootstrap implements ApplicationRunner {
  final Store db;
  final BCryptPasswordEncoder encoder;
  final String password;

  public Bootstrap(
      Store db,
      BCryptPasswordEncoder encoder,
      @Value("${shiftflow.admin-password}") String password) {
    this.db = db;
    this.encoder = encoder;
    this.password = password;
  }

  /** 空库创建角色、导航与管理员；已有密码和业务不被重写。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!db.all(Account.class).isEmpty()) return;
    AdminService.validatePassword(password);
    var d = new Department();
    d.name = "总部";
    db.save(d);
    var names =
        Map.ofEntries(
            Map.entry("roster.read", "查看授权班次"),
            Map.entry("schedule.manage", "编排及发布班次"),
            Map.entry("roster.own", "本人安排及不可排班"),
            Map.entry("cover.write", "申请及接受替班"),
            Map.entry("cover.approve", "独立复核替班"),
            Map.entry("post.manage", "管理排班岗位"),
            Map.entry("qualification.manage", "维护岗位资格"),
            Map.entry("dashboard", "排班统计"),
            Map.entry("export", "导出授权记录"),
            Map.entry("audit", "操作审计"),
            Map.entry("admin", "系统管理"));
    new TreeMap<>(names)
        .forEach(
            (k, v) -> {
              var p = new Permission();
              p.code = k;
              p.name = v;
              db.save(p);
            });
    role("管理员", "ALL", names.keySet());
    role(
        "员工",
        "ASSIGNED",
        Set.of("roster.read", "roster.own", "cover.write", "dashboard", "export"));
    role(
        "排班员",
        "DEPARTMENT",
        Set.of(
            "roster.read",
            "schedule.manage",
            "roster.own",
            "cover.write",
            "post.manage",
            "qualification.manage",
            "dashboard",
            "export",
            "audit"));
    role(
        "主管",
        "DEPARTMENT",
        Set.of(
            "roster.read",
            "schedule.manage",
            "roster.own",
            "cover.write",
            "cover.approve",
            "post.manage",
            "qualification.manage",
            "dashboard",
            "export",
            "audit"));
    var a = new Account();
    a.username = "admin";
    a.displayName = "管理员";
    a.passwordHash = encoder.encode(password);
    a.roleId = db.all(AccessRole.class).getFirst().id;
    a.departmentId = d.id;
    a.enabled = true;
    db.save(a);
    String[][] menus = {
      {"workbench", "我的班次", "My shifts", "roster.read"},
      {"shifts", "班次安排", "Schedule", "roster.read"},
      {"covers", "替班申请", "Coverage requests", "cover.write"},
      {"absences", "不可排班", "Unavailability", "roster.own"},
      {"posts", "排班岗位", "Posts", "post.manage"},
      {"qualifications", "岗位资格", "Qualifications", "qualification.manage"},
      {"dashboard", "排班统计", "Statistics", "dashboard"},
      {"audit", "操作审计", "Audit", "audit"},
      {"users", "账号管理", "Accounts", "admin"},
      {"roles", "角色与权限", "Roles", "admin"},
      {"departments", "部门", "Departments", "admin"},
      {"menus", "导航管理", "Navigation", "admin"},
      {"permissions", "权限目录", "Permissions", "admin"},
      {"dictionaries", "班次类型", "Shift types", "admin"},
      {"settings", "系统参数", "Settings", "admin"}
    };
    for (int i = 0; i < menus.length; i++) {
      var m = new NavMenu();
      m.code = menus[i][0];
      m.name = menus[i][1];
      m.nameEn = menus[i][2];
      m.permissionCode = menus[i][3];
      m.position = i;
      m.enabled = true;
      db.save(m);
    }
    Map.of(
            "timezone",
            "Asia/Shanghai",
            "companyName",
            "知华排班协作",
            "scheduleHorizonDays",
            "90",
            "restGapHours",
            "8")
        .forEach(
            (k, v) -> {
              var s = new SystemSetting();
              s.code = k;
              s.value = v;
              db.save(s);
            });
    String[][] kinds = {
      {"REGULAR", "常规班次", "Regular"}, {"EXTRA", "临时增班", "Additional"}, {"OTHER", "其他安排", "Other"}
    };
    for (var c : kinds) {
      var e = new DictionaryEntry();
      e.type = "shift";
      e.code = c[0];
      e.name = c[1];
      e.nameEn = c[2];
      db.save(e);
    }
  }

  private void role(String name, String scope, Set<String> perms) {
    var a = new AccessRole();
    a.name = name;
    a.scope = scope;
    a.permissions = new HashSet<>(perms);
    db.save(a);
  }
}
