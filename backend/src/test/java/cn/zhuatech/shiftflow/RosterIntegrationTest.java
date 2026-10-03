// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 排班、权限与替班接口验收；可控时钟验证操作窗口，不向运行服务提供调时接口。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RosterIntegrationTest.TimeConfig.class)
class RosterIntegrationTest {
  static final String password = "Aa9" + UUID.randomUUID();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("shiftflow.admin-password", () -> password);
  }

  /** 测试专用可控时钟，生产使用系统 UTC 时钟。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  static class MutableClock extends Clock {
    Instant value = Instant.parse("2026-10-05T01:45:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return this;
    }

    public Instant instant() {
      return value;
    }
  }

  /** 测试环境时钟绑定。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock();
    }
  }

  @Autowired MockMvc mvc;
  @Autowired MutableClock clock;
  final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
  MockHttpSession admin, employee, target, manager, outsider;
  long dept, ownerId, targetId, managerId, role, post, id, qual;
  String suffix, ownerName;
  JsonNode v;
  final Instant start = Instant.parse("2026-10-05T02:00:00Z"), end = start.plusSeconds(3600);

  @BeforeEach
  void setup() throws Exception {
    clock.value = start.minusSeconds(900);
    admin = login("admin", password);
    suffix = UUID.randomUUID().toString().substring(0, 8);
    dept =
        ok(admin, "POST", "/admin/departments", Map.of("name", "排班验收-" + suffix))
            .path("id")
            .asLong();
    long managerRole = 0;
    for (var x : ok(admin, "GET", "/admin/roles", null)) {
      if (x.path("name").asString().equals("员工")) role = x.path("id").asLong();
      if (x.path("name").asString().equals("主管")) managerRole = x.path("id").asLong();
    }
    ownerName = "owner-" + suffix;
    ownerId = user(ownerName, role, dept);
    targetId = user("target-" + suffix, role, dept);
    managerId = user("manager-" + suffix, managerRole, dept);
    user("outside-" + suffix, role, 1);
    employee = login(ownerName, password);
    target = login("target-" + suffix, password);
    manager = login("manager-" + suffix, password);
    outsider = login("outside-" + suffix, password);
    post =
        ok(
                manager,
                "POST",
                "/posts",
                Map.of(
                    "code",
                    "POST-" + suffix,
                    "name",
                    "岗位验收",
                    "departmentId",
                    dept,
                    "enabled",
                    true))
            .path("id")
            .asLong();
    qual = qualification(ownerId);
    qualification(targetId);
    v = ok(manager, "POST", "/shifts", draft(start, end, ownerId));
    id = v.path("id").asLong();
  }

  long user(String name, long role, long dep) throws Exception {
    return ok(
            admin,
            "POST",
            "/admin/users",
            Map.of(
                "username",
                name,
                "displayName",
                "验收测试 " + name,
                "roleId",
                role,
                "departmentId",
                dep,
                "enabled",
                true,
                "password",
                password))
        .path("id")
        .asLong();
  }

  long qualification(long who) throws Exception {
    return ok(
            manager,
            "POST",
            "/qualifications",
            Map.of(
                "accountId",
                who,
                "postId",
                post,
                "validFrom",
                "2026-10-01",
                "validUntil",
                "2026-12-31",
                "enabled",
                true))
        .path("id")
        .asLong();
  }

  Map<String, Object> draft(Instant a, Instant b, long who) {
    return Map.of(
        "title",
        "班次验收",
        "postId",
        post,
        "employeeId",
        who,
        "startsAt",
        a,
        "endsAt",
        b,
        "category",
        "REGULAR",
        "note",
        "隔离验收测试",
        "requestKey",
        UUID.randomUUID().toString());
  }

  JsonNode current() throws Exception {
    return ok(manager, "GET", "/shifts/" + id, null).path("shift");
  }

  Map<String, Object> command(JsonNode n) {
    return Map.of(
        "version",
        n.path("version").asLong(),
        "requestKey",
        UUID.randomUUID().toString(),
        "note",
        "验收测试处理",
        "employeeId",
        targetId);
  }

  JsonNode act(MockHttpSession c, String a) throws Exception {
    return ok(c, "POST", "/shifts/" + id + "/commands/" + a, command(current()));
  }

  JsonNode request() throws Exception {
    var n = current();
    return ok(
        employee,
        "POST",
        "/covers",
        Map.of(
            "shiftId",
            id,
            "shiftVersion",
            n.path("version").asLong(),
            "targetId",
            targetId,
            "note",
            "验收测试替班",
            "requestKey",
            UUID.randomUUID().toString()));
  }

  JsonNode cover(MockHttpSession c, JsonNode n, String action) throws Exception {
    return ok(c, "POST", "/covers/" + n.path("id").asLong() + "/commands/" + action, command(n));
  }

  MockHttpSession login(String name, String pw) throws Exception {
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(json.writeValueAsString(Map.of("username", name, "password", pw))))
            .andReturn();
    assertEquals(200, result.getResponse().getStatus());
    return (MockHttpSession) result.getRequest().getSession();
  }

  MvcResult call(MockHttpSession who, String method, String path, Object body) throws Exception {
    var req =
        switch (method) {
          case "POST" -> post("/api" + path);
          case "PUT" -> put("/api" + path);
          case "DELETE" -> delete("/api" + path);
          default -> get("/api" + path);
        };
    req.session(who).with(csrf());
    if (body != null) req.contentType("application/json").content(json.writeValueAsString(body));
    return mvc.perform(req).andReturn();
  }

  JsonNode ok(MockHttpSession who, String method, String path, Object body) throws Exception {
    var res = call(who, method, path, body);
    assertEquals(200, res.getResponse().getStatus(), res.getResponse().getContentAsString());
    return json.readTree(res.getResponse().getContentAsString());
  }

  void expect(MockHttpSession who, String method, String path, Object body, int status)
      throws Exception {
    var res = call(who, method, path, body);
    assertEquals(status, res.getResponse().getStatus(), res.getResponse().getContentAsString());
  }

  @Test
  void completeCoverage() throws Exception {
    act(manager, "publish");
    act(employee, "acknowledge");
    var c = request();
    assertEquals(ownerId, current().path("employeeId").asLong());
    c = cover(target, c, "accept");
    assertEquals(ownerId, current().path("employeeId").asLong());
    cover(manager, c, "approve");
    assertEquals(targetId, current().path("employeeId").asLong());
    assertTrue(current().path("acknowledgedAt").isNull());
    act(target, "acknowledge");
    assertEquals(8, ok(manager, "GET", "/shifts/" + id, null).path("events").size());
  }

  @Test
  void employeeCannotSeeDraft() throws Exception {
    expect(employee, "GET", "/shifts/" + id, null, 403);
    assertEquals(0, ok(employee, "GET", "/shifts", null).path("total").asLong());
  }

  @Test
  void otherDepartmentDenied() throws Exception {
    act(manager, "publish");
    expect(outsider, "GET", "/shifts/" + id, null, 403);
    expect(outsider, "GET", "/shifts/" + id + "/report.json", null, 403);
    assertEquals(0, ok(outsider, "GET", "/shifts", null).path("total").asLong());
  }

  @Test
  void onlyCurrentEmployeeAcknowledges() throws Exception {
    act(manager, "publish");
    expect(manager, "POST", "/shifts/" + id + "/commands/acknowledge", command(current()), 403);
    act(employee, "acknowledge");
  }

  @Test
  void employeeCannotPublish() throws Exception {
    expect(employee, "POST", "/shifts/" + id + "/commands/publish", command(current()), 403);
  }

  @Test
  void optimisticVersion() throws Exception {
    var c = new HashMap<>(command(current()));
    c.put("version", -1);
    expect(manager, "POST", "/shifts/" + id + "/commands/publish", c, 409);
  }

  @Test
  void exactPublishReplay() throws Exception {
    var c = command(current());
    var a = ok(manager, "POST", "/shifts/" + id + "/commands/publish", c);
    var b = ok(manager, "POST", "/shifts/" + id + "/commands/publish", c);
    assertEquals(a.path("version").asLong(), b.path("version").asLong());
    assertEquals(2, ok(manager, "GET", "/shifts/" + id, null).path("events").size());
  }

  @Test
  void changedReplayRejected() throws Exception {
    var c = new HashMap<>(command(current()));
    ok(manager, "POST", "/shifts/" + id + "/commands/publish", c);
    c.put("note", "different");
    expect(manager, "POST", "/shifts/" + id + "/commands/publish", c, 409);
  }

  @Test
  void draftDelete() throws Exception {
    expect(manager, "DELETE", "/shifts/" + id + "?version=-1", null, 409);
    ok(manager, "DELETE", "/shifts/" + id + "?version=" + current().path("version").asLong(), null);
    expect(manager, "GET", "/shifts/" + id, null, 404);
  }

  @Test
  void publishedHistoryCannotDeleteOrEdit() throws Exception {
    act(manager, "publish");
    expect(
        manager,
        "DELETE",
        "/shifts/" + id + "?version=" + current().path("version").asLong(),
        null,
        409);
    var d = new HashMap<>(draft(start, end, ownerId));
    d.put("version", current().path("version").asLong());
    expect(manager, "PUT", "/shifts/" + id, d, 409);
  }

  @Test
  void unqualifiedPublishRejected() throws Exception {
    ok(manager, "DELETE", "/qualifications/" + qual + "?version=0", null);
    expect(manager, "POST", "/shifts/" + id + "/commands/publish", command(current()), 409);
    assertEquals("DRAFT", current().path("status").asString());
  }

  @Test
  void publishedQualificationGuard() throws Exception {
    act(manager, "publish");
    expect(manager, "DELETE", "/qualifications/" + qual + "?version=0", null, 409);
    expect(
        manager,
        "PUT",
        "/qualifications/" + qual,
        Map.of(
            "version",
            0,
            "accountId",
            ownerId,
            "postId",
            post,
            "validFrom",
            "2026-10-01",
            "validUntil",
            "2026-12-31",
            "enabled",
            false),
        409);
  }

  @Test
  void publishedEmployeeGuard() throws Exception {
    act(manager, "publish");
    expect(
        admin,
        "PUT",
        "/admin/users/" + ownerId,
        Map.of(
            "username",
            ownerName,
            "displayName",
            "验收",
            "roleId",
            role,
            "departmentId",
            dept,
            "enabled",
            false),
        409);
    ok(employee, "GET", "/auth/me", null);
  }

  @Test
  void publishedRoleGuard() throws Exception {
    act(manager, "publish");
    expect(
        admin,
        "PUT",
        "/admin/roles/" + role,
        Map.of("name", "员工", "scope", "ASSIGNED", "permissions", Set.of("roster.own")),
        409);
  }

  @Test
  void publishedPostGuard() throws Exception {
    act(manager, "publish");
    expect(
        manager,
        "PUT",
        "/posts/" + post,
        Map.of(
            "version",
            0,
            "code",
            "POST-" + suffix,
            "name",
            "岗位验收",
            "departmentId",
            dept,
            "enabled",
            false),
        409);
  }

  @Test
  void activeRequestUnique() throws Exception {
    act(manager, "publish");
    request();
    var n = current();
    expect(
        employee,
        "POST",
        "/covers",
        Map.of(
            "shiftId",
            id,
            "shiftVersion",
            n.path("version").asLong(),
            "targetId",
            targetId,
            "note",
            "重复申请",
            "requestKey",
            UUID.randomUUID().toString()),
        409);
  }

  @Test
  void acceptOnlyTarget() throws Exception {
    act(manager, "publish");
    var c = request();
    expect(
        employee, "POST", "/covers/" + c.path("id").asLong() + "/commands/accept", command(c), 403);
  }

  @Test
  void approvalRequiresAcceptance() throws Exception {
    act(manager, "publish");
    var c = request();
    expect(
        manager, "POST", "/covers/" + c.path("id").asLong() + "/commands/approve", command(c), 409);
  }

  @Test
  void independentApproval() throws Exception {
    act(manager, "publish");
    var c = cover(target, request(), "accept");
    long mr = 0;
    for (var x : ok(admin, "GET", "/admin/roles", null))
      if (x.path("name").asString().equals("主管")) mr = x.path("id").asLong();
    ok(
        admin,
        "PUT",
        "/admin/users/" + targetId,
        Map.of(
            "username",
            "target-" + suffix,
            "displayName",
            "验收主管",
            "roleId",
            mr,
            "departmentId",
            dept,
            "enabled",
            true));
    expect(
        target, "POST", "/covers/" + c.path("id").asLong() + "/commands/approve", command(c), 403);
  }

  @Test
  void withdrawPreservesAssignment() throws Exception {
    act(manager, "publish");
    var c = cover(employee, request(), "withdraw");
    assertEquals("WITHDRAWN", c.path("status").asString());
    assertEquals(ownerId, current().path("employeeId").asLong());
  }

  @Test
  void rejectPreservesAssignment() throws Exception {
    act(manager, "publish");
    var c = cover(target, request(), "reject");
    assertEquals("REJECTED", c.path("status").asString());
    assertEquals(ownerId, current().path("employeeId").asLong());
  }

  @Test
  void cancelInvalidatesRequest() throws Exception {
    act(manager, "publish");
    var c = request();
    act(manager, "cancel");
    assertEquals(
        "STALE",
        ok(employee, "GET", "/covers/" + c.path("id").asLong(), null)
            .path("coverage")
            .path("status")
            .asString());
  }

  @Test
  void reassignInvalidatesRequest() throws Exception {
    act(manager, "publish");
    act(employee, "acknowledge");
    var c = request();
    act(manager, "reassign");
    assertEquals(targetId, current().path("employeeId").asLong());
    assertTrue(current().path("acknowledgedAt").isNull());
    assertEquals(
        "STALE",
        ok(employee, "GET", "/covers/" + c.path("id").asLong(), null)
            .path("coverage")
            .path("status")
            .asString());
  }

  @Test
  void publishConflict() throws Exception {
    act(manager, "publish");
    var second =
        ok(
            manager,
            "POST",
            "/shifts",
            draft(start.plusSeconds(1800), end.plusSeconds(1800), ownerId));
    expect(
        manager,
        "POST",
        "/shifts/" + second.path("id").asLong() + "/commands/publish",
        command(second),
        409);
  }

  @Test
  void gapConflict() throws Exception {
    act(manager, "publish");
    var second =
        ok(
            manager,
            "POST",
            "/shifts",
            draft(end.plusSeconds(3600), end.plusSeconds(7200), ownerId));
    expect(
        manager,
        "POST",
        "/shifts/" + second.path("id").asLong() + "/commands/publish",
        command(second),
        409);
  }

  @Test
  void unavailabilityBlocksPublish() throws Exception {
    ok(
        employee,
        "POST",
        "/absences",
        Map.of(
            "startsAt",
            start,
            "endsAt",
            end,
            "note",
            "验收不可排班",
            "requestKey",
            UUID.randomUUID().toString()));
    expect(manager, "POST", "/shifts/" + id + "/commands/publish", command(current()), 409);
  }

  @Test
  void publishedBlocksUnavailability() throws Exception {
    act(manager, "publish");
    expect(
        employee,
        "POST",
        "/absences",
        Map.of(
            "startsAt",
            start,
            "endsAt",
            end,
            "note",
            "验收不可排班",
            "requestKey",
            UUID.randomUUID().toString()),
        409);
  }

  @Test
  void unavailabilityPrivacyAndCancel() throws Exception {
    var u =
        ok(
            employee,
            "POST",
            "/absences",
            Map.of(
                "startsAt",
                start,
                "endsAt",
                end,
                "note",
                "验收不可排班",
                "requestKey",
                UUID.randomUUID().toString()));
    assertEquals(0, ok(target, "GET", "/absences", null).path("total").asLong());
    expect(target, "POST", "/absences/" + u.path("id").asLong() + "/cancel", command(u), 403);
    ok(employee, "POST", "/absences/" + u.path("id").asLong() + "/cancel", command(u));
    act(manager, "publish");
  }

  @Test
  void lateAcceptanceAndCancellation() throws Exception {
    act(manager, "publish");
    var c = request();
    clock.value = start;
    expect(
        target, "POST", "/covers/" + c.path("id").asLong() + "/commands/accept", command(c), 409);
    expect(manager, "POST", "/shifts/" + id + "/commands/cancel", command(current()), 409);
    cover(employee, c, "withdraw");
  }

  @Test
  void acknowledgementDeadline() throws Exception {
    act(manager, "publish");
    clock.value = end;
    expect(employee, "POST", "/shifts/" + id + "/commands/acknowledge", command(current()), 409);
  }

  @Test
  void exportNoCredentialsOrAdvertisement() throws Exception {
    act(manager, "publish");
    String raw =
        ok(employee, "GET", "/shifts/" + id + "/report.json", null).toString().toLowerCase();
    assertFalse(raw.contains("password"));
    assertFalse(raw.contains("zhuatech"));
  }

  @Test
  void queryBoundsAndLiteralSearch() throws Exception {
    expect(manager, "GET", "/shifts?size=101", null, 400);
    expect(manager, "GET", "/covers?status=UNKNOWN", null, 400);
    assertEquals(0, ok(manager, "GET", "/shifts?search=%25", null).path("total").asLong());
  }

  @Test
  void csrfRequired() throws Exception {
    var res =
        mvc.perform(
                post("/api/shifts")
                    .session(manager)
                    .contentType("application/json")
                    .content(json.writeValueAsString(draft(start, end, ownerId))))
            .andReturn();
    assertEquals(403, res.getResponse().getStatus());
  }

  @Test
  void concurrentPublicationSerialized() throws Exception {
    var n = ok(manager, "POST", "/shifts", draft(start, end, ownerId));
    var c1 = command(current());
    var c2 = command(n);
    var gate = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var a =
          pool.submit(
              () -> {
                gate.await();
                return call(manager, "POST", "/shifts/" + id + "/commands/publish", c1)
                    .getResponse()
                    .getStatus();
              });
      var b =
          pool.submit(
              () -> {
                gate.await();
                return call(
                        admin, "POST", "/shifts/" + n.path("id").asLong() + "/commands/publish", c2)
                    .getResponse()
                    .getStatus();
              });
      gate.countDown();
      var results = new ArrayList<>(List.of(a.get(), b.get()));
      Collections.sort(results);
      assertEquals(List.of(200, 409), results);
    }
  }

  @Test
  void disabledSessionFails() throws Exception {
    ok(
        admin,
        "PUT",
        "/admin/users/" + ownerId,
        Map.of(
            "username",
            ownerName,
            "displayName",
            "验收",
            "roleId",
            role,
            "departmentId",
            dept,
            "enabled",
            false));
    expect(employee, "GET", "/auth/me", null, 401);
  }

  @Test
  void approvalRechecksCandidateConflict() throws Exception {
    act(manager, "publish");
    var c = cover(target, request(), "accept");
    var n = ok(manager, "POST", "/shifts", draft(start, end, targetId));
    ok(manager, "POST", "/shifts/" + n.path("id").asLong() + "/commands/publish", command(n));
    expect(
        manager, "POST", "/covers/" + c.path("id").asLong() + "/commands/approve", command(c), 409);
    assertEquals(ownerId, current().path("employeeId").asLong());
  }

  @Test
  void approvalOnlyRoleCanReject() throws Exception {
    act(manager, "publish");
    var c = request();
    long r =
        ok(
                admin,
                "POST",
                "/admin/roles",
                Map.of(
                    "name",
                    "独立复核-" + suffix,
                    "scope",
                    "DEPARTMENT",
                    "permissions",
                    Set.of("roster.read", "cover.approve")))
            .path("id")
            .asLong();
    user("review-" + suffix, r, dept);
    cover(login("review-" + suffix, password), c, "reject");
  }
}
