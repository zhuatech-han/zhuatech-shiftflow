// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** 排班、员工知悉、不可排班、岗位资格及独立替班复核。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RosterService {
  final Store db;
  final AccessService access;
  final Clock clock;
  final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  public RosterService(Store db, AccessService access, Clock clock) {
    this.db = db;
    this.access = access;
    this.clock = clock;
  }

  /** 班次输入；计划资料不接收确认时间或系统状态。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record ShiftInput(
      Long version,
      Long postId,
      Long employeeId,
      String title,
      String category,
      String note,
      Instant startsAt,
      Instant endsAt,
      String requestKey) {}

  /** 版本及幂等命令，改派另需员工ID。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record Command(Long version, String requestKey, String note, Long employeeId) {}

  /** 替班请求绑定当前班次及目标员工。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record CoverInput(
      Long shiftId, Long shiftVersion, Long targetId, String note, String requestKey) {}

  /** 岗位资料；历史编号和部门不可覆盖。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record PostInput(
      Long version, String code, String name, Long departmentId, Boolean enabled) {}

  /** 排班资格的人员、岗位和有效日期；业务维护不代表法定认证。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record QualificationInput(
      Long version,
      Long accountId,
      Long postId,
      LocalDate validFrom,
      LocalDate validUntil,
      Boolean enabled) {}

  /** 本人不可排班时段，不采集医疗或请假证明。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record AbsenceInput(Instant startsAt, Instant endsAt, String note, String requestKey) {}

  /** 读取授权目录，只返回员工显示名和部门，不返回凭证或其他部门人员。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object options() {
    access.require("roster.read");
    return Map.of(
        "accounts",
        db.all(Account.class).stream()
            .filter(
                a ->
                    a.enabled
                        && access.visible(a.departmentId)
                        && db.get(AccessRole.class, a.roleId).permissions.contains("roster.read"))
            .map(a -> Map.of("id", a.id, "name", a.displayName, "departmentId", a.departmentId))
            .toList(),
        "departments",
        db.all(Department.class).stream().filter(d -> access.visible(d.id)).toList(),
        "posts",
        db.all(ShiftPost.class).stream().filter(p -> access.visible(p.departmentId)).toList(),
        "dictionaries",
        db.all(DictionaryEntry.class).stream().filter(d -> d.type.equals("shift")).toList(),
        "settings",
        db.all(SystemSetting.class),
        "serverNow",
        clock.instant());
  }

  /** 分页查询计划班次；员工只看本人已发布安排和本人已获批替班历史。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object shifts(
      String search, String status, int page, int size, String sort, boolean mine) {
    access.require("roster.read");
    pagination(search, page, size);
    if (!status.isBlank() && !Set.of("DRAFT", "PUBLISHED", "CANCELLED").contains(status))
      throw new Problem(400, "INVALID_STATE");
    String where = shiftScope() + " and lower(s.title) like :q escape '!'";
    if (!status.isBlank()) where += " and s.status=:s";
    if (mine) where += " and s.employeeId=" + access.current().id;
    String order =
        switch (sort) {
          case "time" -> "s.startsAt asc,s.id asc";
          case "newest" -> "s.createdAt desc,s.id desc";
          case "title" -> "s.title asc,s.id asc";
          default -> throw new Problem(400, "INVALID_INPUT");
        };
    var count =
        db.jpql(Long.class, "select count(s) from Shift s where " + where)
            .setParameter("q", like(search));
    var rows =
        db.jpql(Shift.class, "from Shift s where " + where + " order by " + order)
            .setParameter("q", like(search));
    if (!status.isBlank()) {
      count.setParameter("s", status);
      rows.setParameter("s", status);
    }
    return Map.of(
        "items",
        rows.setFirstResult(page * size).setMaxResults(size).getResultList(),
        "total",
        count.getSingleResult());
  }

  /** 读取授权详情及不能修改的事件快照。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object detail(Long id) {
    var s = readShift(id);
    return Map.of(
        "shift",
        s,
        "employee",
        db.get(Account.class, s.employeeId).displayName,
        "post",
        db.get(ShiftPost.class, s.postId).name,
        "events",
        db.query(RosterEvent.class, "from RosterEvent where shiftId=?1 order by id", id));
  }

  /** 创建排班草稿，服务端决定部门和状态。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Shift create(ShiftInput v) {
    gate("schedule.manage");
    if (v == null) invalid();
    var old = replay(v.requestKey, fingerprint("create", v));
    if (old != null) return readShift(typed(old, "SHIFT"));
    var s = new Shift();
    fill(s, v);
    s.status = "DRAFT";
    s.createdAt = clock.instant();
    s.updatedAt = clock.instant();
    db.save(s);
    db.flush();
    stamp(v.requestKey, fingerprint("create", v), "SHIFT", s.id);
    event(s, null, "CREATE", "", s);
    return s;
  }

  /** 保存未发布草稿；发布班次资料冻结，只能取消或有记录地改派。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Shift save(Long id, ShiftInput v) {
    gate("schedule.manage");
    if (v == null) invalid();
    var s = readShift(id);
    manage(s);
    String fp = fingerprint("save:" + id, v);
    var old = replay(v.requestKey, fp);
    if (old != null) {
      typed(old, "SHIFT");
      return s;
    }
    version(s.version, v.version);
    state(s.status, "DRAFT");
    fill(s, v);
    s.updatedAt = clock.instant();
    db.flush();
    stamp(v.requestKey, fp, "SHIFT", id);
    event(s, null, "EDIT", "", s);
    return s;
  }

  /** 删除从未发布的草稿，已发布计划历史必须保留。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void deleteShift(Long id, Long version) {
    gate("schedule.manage");
    var s = readShift(id);
    manage(s);
    version(s.version, version);
    state(s.status, "DRAFT");
    for (var e : db.query(RosterEvent.class, "from RosterEvent where shiftId=?1", id)) db.delete(e);
    for (var c :
        db.query(CommandStamp.class, "from CommandStamp where kind='SHIFT' and objectId=?1", id))
      db.delete(c);
    access.audit("DELETE_DRAFT", id, s.departmentId);
    db.delete(s);
  }

  /** 发布检查资格、时间、不可排班和间隔；知悉只由本人提交；改派使旧申请失效。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Shift command(Long id, String action, Command v) {
    gate(action.equals("acknowledge") ? "roster.own" : "schedule.manage");
    if (v == null) invalid();
    var s = readShift(id);
    if (action.equals("acknowledge")) {
      if (!s.employeeId.equals(access.current().id)) throw new Problem(403, "NOT_ASSIGNEE");
    } else manage(s);
    String fp = fingerprint("shift:" + id + ":" + action, v);
    var old = replay(v.requestKey, fp);
    if (old != null) {
      typed(old, "SHIFT");
      return s;
    }
    version(s.version, v.version);
    switch (action) {
      case "publish" -> {
        state(s.status, "DRAFT");
        RosterPolicy.times(s.startsAt, s.endsAt, clock.instant(), setting("scheduleHorizonDays"));
        s.restGapHours = setting("restGapHours");
        eligible(s, s.employeeId);
        s.status = "PUBLISHED";
        s.publishedAt = clock.instant();
        s.assignmentRevision++;
      }
      case "acknowledge" -> {
        state(s.status, "PUBLISHED");
        if (!s.endsAt.isAfter(clock.instant())) throw new Problem(409, "TOO_LATE");
        if (s.acknowledgedAt != null) throw new Problem(409, "ALREADY_ACKNOWLEDGED");
        s.acknowledgedAt = clock.instant();
      }
      case "cancel" -> {
        state(s.status, "PUBLISHED");
        future(s);
        AdminService.text(v.note, 1000);
        s.status = "CANCELLED";
        invalidate(s, null);
      }
      case "reassign" -> {
        state(s.status, "PUBLISHED");
        future(s);
        AdminService.text(v.note, 1000);
        if (v.employeeId == null || v.employeeId.equals(s.employeeId)) invalid();
        eligible(s, v.employeeId);
        s.employeeId = v.employeeId;
        s.assignmentRevision++;
        s.acknowledgedAt = null;
        invalidate(s, null);
      }
      default -> throw new Problem(400, "INVALID_ACTION");
    }
    s.updatedAt = clock.instant();
    db.flush();
    stamp(v.requestKey, fp, "SHIFT", id);
    event(s, null, action.toUpperCase(Locale.ROOT), note(v.note), s);
    return s;
  }

  /** 仅为本人当前班次列出同部门、有效且没有时间冲突的替班候选人。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object candidates(Long id) {
    var s = readShift(id);
    if (!s.employeeId.equals(access.current().id) && !has("schedule.manage"))
      throw new Problem(403, "FORBIDDEN");
    state(s.status, "PUBLISHED");
    future(s);
    var result = new ArrayList<Object>();
    for (var a : db.all(Account.class)) {
      if (a.id.equals(s.employeeId) || !a.departmentId.equals(s.departmentId)) continue;
      try {
        if (!db.get(AccessRole.class, a.roleId).permissions.contains("cover.write")) continue;
        eligible(s, a.id);
        result.add(Map.of("id", a.id, "name", a.displayName));
      } catch (Problem e) {
        if (e.status != 409 && e.status != 400) throw e;
      }
    }
    return result;
  }

  /** 申请指定同事替班，保留原指派，最多一条活动申请。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Coverage request(CoverInput v) {
    gate("cover.write");
    if (v == null) invalid();
    String fp = fingerprint("request", v);
    var old = replay(v.requestKey, fp);
    if (old != null) return readCover(typed(old, "COVER"));
    var s = readShift(v.shiftId);
    version(s.version, v.shiftVersion);
    state(s.status, "PUBLISHED");
    future(s);
    if (!s.employeeId.equals(access.current().id)) throw new Problem(403, "NOT_ASSIGNEE");
    if (v.targetId == null || v.targetId.equals(s.employeeId)) invalid();
    if (!db.query(
            Coverage.class,
            "from Coverage where shiftId=?1 and status in ('REQUESTED','ACCEPTED')",
            s.id)
        .isEmpty()) throw new Problem(409, "ACTIVE_REQUEST");
    if (!db.get(AccessRole.class, db.get(Account.class, v.targetId).roleId)
        .permissions
        .contains("cover.write")) throw new Problem(400, "INVALID_EMPLOYEE");
    eligible(s, v.targetId);
    var c = new Coverage();
    c.shiftId = s.id;
    c.requesterId = s.employeeId;
    c.targetId = v.targetId;
    c.assignmentRevision = s.assignmentRevision;
    c.status = "REQUESTED";
    c.note = AdminService.text(v.note, 1000);
    c.createdAt = clock.instant();
    c.updatedAt = clock.instant();
    db.save(c);
    db.flush();
    stamp(v.requestKey, fp, "COVER", c.id);
    event(s, c, "REQUEST_COVER", c.note, c);
    return c;
  }

  /** 分页查询参与或本部门替班请求；批准待办仍要求独立复核权限。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object covers(String status, int page, int size) {
    requireCover();
    pagination("", page, size);
    if (!status.isBlank()
        && !Set.of("REQUESTED", "ACCEPTED", "APPROVED", "REJECTED", "WITHDRAWN", "STALE")
            .contains(status)) throw new Problem(400, "INVALID_STATE");
    String where = coverScope() + (!status.isBlank() ? " and c.status=:s" : "");
    var count = db.jpql(Long.class, "select count(c) from Coverage c where " + where);
    var rows =
        db.jpql(
            Coverage.class,
            "from Coverage c where " + where + " order by c.createdAt desc,c.id desc");
    if (!status.isBlank()) {
      count.setParameter("s", status);
      rows.setParameter("s", status);
    }
    return Map.of(
        "items",
        rows.setFirstResult(page * size).setMaxResults(size).getResultList(),
        "total",
        count.getSingleResult());
  }

  /** 查看本人参与的替班及该申请事件，不暴露其他请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object coverDetail(Long id) {
    var c = readCover(id);
    var s = db.get(Shift.class, c.shiftId);
    return Map.of(
        "coverage",
        c,
        "shift",
        s,
        "requester",
        db.get(Account.class, c.requesterId).displayName,
        "target",
        db.get(Account.class, c.targetId).displayName,
        "post",
        db.get(ShiftPost.class, s.postId).name,
        "events",
        db.query(RosterEvent.class, "from RosterEvent where coverageId=?1 order by id", id));
  }

  /** 对方接受后由两方之外的主管批准，批准事务原子改派并重新验证资格和冲突。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Coverage coverCommand(Long id, String action, Command v) {
    gate(
        action.equals("reject")
            ? "cover.any"
            : action.equals("approve") ? "cover.approve" : "cover.write");
    if (v == null) invalid();
    var c = readCover(id);
    var s = db.get(Shift.class, c.shiftId);
    Long me = access.current().id;
    switch (action) {
      case "accept" -> {
        if (!c.targetId.equals(me)) throw new Problem(403, "NOT_TARGET");
      }
      case "approve" -> {
        access.department(s.departmentId);
        if (me.equals(c.requesterId) || me.equals(c.targetId))
          throw new Problem(403, "INDEPENDENT_REVIEW");
      }
      case "withdraw" -> {
        if (!me.equals(c.requesterId)) throw new Problem(403, "NOT_REQUESTER");
      }
      case "reject" -> {
        if (!me.equals(c.targetId)) {
          access.require("cover.approve");
          access.department(s.departmentId);
          if (me.equals(c.requesterId)) throw new Problem(403, "INDEPENDENT_REVIEW");
        }
      }
      default -> throw new Problem(400, "INVALID_ACTION");
    }
    String fp = fingerprint("cover:" + id + ":" + action, v);
    var old = replay(v.requestKey, fp);
    if (old != null) {
      typed(old, "COVER");
      return c;
    }
    version(c.version, v.version);
    switch (action) {
      case "accept" -> {
        state(c.status, "REQUESTED");
        unchanged(c, s);
        future(s);
        eligible(s, c.targetId);
        c.status = "ACCEPTED";
      }
      case "approve" -> {
        state(c.status, "ACCEPTED");
        unchanged(c, s);
        future(s);
        eligible(s, c.targetId);
        s.employeeId = c.targetId;
        s.assignmentRevision++;
        s.acknowledgedAt = null;
        s.updatedAt = clock.instant();
        c.status = "APPROVED";
        invalidate(s, c.id);
        event(s, null, "COVER_ASSIGNED", AdminService.text(v.note, 1000), s);
      }
      case "withdraw" -> {
        active(c);
        c.status = "WITHDRAWN";
        AdminService.text(v.note, 1000);
      }
      case "reject" -> {
        active(c);
        c.status = "REJECTED";
        AdminService.text(v.note, 1000);
      }
    }
    c.updatedAt = clock.instant();
    db.flush();
    stamp(v.requestKey, fp, "COVER", id);
    event(s, c, "COVER_" + action.toUpperCase(Locale.ROOT), note(v.note), c);
    return c;
  }

  /** 本人登记不可排班时段，不允许覆盖已发布安排。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Unavailability absence(AbsenceInput v) {
    gate("roster.own");
    if (v == null) invalid();
    String fp = fingerprint("absence", v);
    var old = replay(v.requestKey, fp);
    if (old != null) {
      var x = db.get(Unavailability.class, typed(old, "ABSENCE"));
      if (!x.accountId.equals(access.current().id)) throw new Problem(403, "FORBIDDEN");
      return x;
    }
    if (v.startsAt == null
        || v.endsAt == null
        || !v.endsAt.isAfter(v.startsAt)
        || v.startsAt.isBefore(clock.instant())
        || Duration.between(v.startsAt, v.endsAt).compareTo(Duration.ofDays(31)) > 0
        || v.startsAt.isAfter(clock.instant().plusSeconds(180 * 86400L)))
      throw new Problem(400, "INVALID_TIME");
    Long me = access.current().id;
    for (var s : published(me))
      if (RosterPolicy.conflicts(v.startsAt, v.endsAt, s.startsAt, s.endsAt, 0))
        throw new Problem(409, "PUBLISHED_CONFLICT");
    var u = new Unavailability();
    u.accountId = me;
    u.departmentId = access.current().departmentId;
    u.startsAt = v.startsAt;
    u.endsAt = v.endsAt;
    u.note = AdminService.text(v.note, 500);
    u.status = "ACTIVE";
    u.createdAt = clock.instant();
    db.save(u);
    db.flush();
    stamp(v.requestKey, fp, "ABSENCE", u.id);
    access.audit("UNAVAILABILITY_CREATE", u.id, u.departmentId);
    return u;
  }

  /** 查询本人或获准部门的不可排班资料，不返回其他部门资料。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object absences(int page, int size) {
    access.require("roster.read");
    pagination("", page, size);
    String where =
        access.role().scope.equals("ALL")
            ? "1=1"
            : access.role().scope.equals("DEPARTMENT")
                ? "u.departmentId=" + access.current().departmentId
                : "u.accountId=" + access.current().id;
    return Map.of(
        "items",
        db.jpql(
                Unavailability.class,
                "from Unavailability u where " + where + " order by u.startsAt desc,u.id desc")
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList(),
        "total",
        db.jpql(Long.class, "select count(u) from Unavailability u where " + where)
            .getSingleResult());
  }

  /** 仅本人在开始前撤销不可排班，版本和幂等保护，保留记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Unavailability cancelAbsence(Long id, Command v) {
    gate("roster.own");
    if (v == null) invalid();
    var u = db.get(Unavailability.class, id);
    if (!u.accountId.equals(access.current().id)) throw new Problem(403, "NOT_OWNER");
    String fp = fingerprint("absence-cancel:" + id, v);
    var old = replay(v.requestKey, fp);
    if (old != null) {
      typed(old, "ABSENCE");
      return u;
    }
    version(u.version, v.version);
    state(u.status, "ACTIVE");
    if (!u.startsAt.isAfter(clock.instant())) throw new Problem(409, "TOO_LATE");
    AdminService.text(v.note, 1000);
    u.status = "CANCELLED";
    db.flush();
    stamp(v.requestKey, fp, "ABSENCE", id);
    access.audit("UNAVAILABILITY_CANCEL", id, u.departmentId);
    return u;
  }

  /** 岗位清单；只显示可见部门，员工不能编辑资料。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object posts() {
    access.require("roster.read");
    return db.all(ShiftPost.class).stream().filter(p -> access.visible(p.departmentId)).toList();
  }

  /** 建立或更新部门岗位，已引用编号及部门冻结，未来发布班次阻止停用。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public ShiftPost post(Long id, PostInput v) {
    gate("post.manage");
    if (v == null) invalid();
    access.department(v.departmentId);
    db.get(Department.class, v.departmentId);
    var p = id == null ? new ShiftPost() : db.get(ShiftPost.class, id);
    if (id != null) {
      access.department(p.departmentId);
      version(p.version, v.version);
      if (!db.query(Shift.class, "from Shift where postId=?1", id).isEmpty()
          && (!p.code.equals(v.code) || !p.departmentId.equals(v.departmentId)))
        throw new Problem(409, "HISTORY_PROTECTED");
    }
    p.code = AdminService.text(v.code, 60).toUpperCase(Locale.ROOT);
    if (!p.code.matches("[A-Z0-9_-]{1,60}")) invalid();
    p.name = AdminService.text(v.name, 120);
    p.departmentId = v.departmentId;
    p.enabled = Boolean.TRUE.equals(v.enabled);
    if (id == null) db.save(p);
    db.flush();
    verifyPublished();
    access.audit("POST_SAVE", p.id, p.departmentId);
    return p;
  }

  /** 只有没有历史引用的岗位可删除，外键保护资格引用。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void deletePost(Long id, Long version) {
    gate("post.manage");
    var p = db.get(ShiftPost.class, id);
    access.department(p.departmentId);
    version(p.version, version);
    access.audit("POST_DELETE", id, p.departmentId);
    db.delete(p);
  }

  /** 岗位资格按部门或本人范围读取。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object qualifications() {
    access.require("roster.read");
    return db.all(Qualification.class).stream()
        .filter(
            q ->
                access.role().scope.equals("ALL")
                    || access.role().scope.equals("DEPARTMENT")
                        && access.visible(db.get(ShiftPost.class, q.postId).departmentId)
                    || q.accountId.equals(access.current().id))
        .toList();
  }

  /** 管理资格有效日期；不能使未来发布班次变成无资格安排。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Qualification qualification(Long id, QualificationInput v) {
    gate("qualification.manage");
    if (v == null) invalid();
    var p = db.get(ShiftPost.class, v.postId);
    access.department(p.departmentId);
    var a = db.get(Account.class, v.accountId);
    if (!a.departmentId.equals(p.departmentId)) throw new Problem(400, "INVALID_EMPLOYEE");
    if (v.validFrom == null || v.validUntil == null || v.validUntil.isBefore(v.validFrom))
      invalid();
    var q = id == null ? new Qualification() : db.get(Qualification.class, id);
    if (id != null) {
      access.department(db.get(ShiftPost.class, q.postId).departmentId);
      version(q.version, v.version);
      if (!q.accountId.equals(v.accountId) || !q.postId.equals(v.postId))
        throw new Problem(409, "HISTORY_PROTECTED");
    }
    q.accountId = a.id;
    q.postId = p.id;
    q.validFrom = v.validFrom;
    q.validUntil = v.validUntil;
    q.enabled = Boolean.TRUE.equals(v.enabled);
    if (id == null) db.save(q);
    db.flush();
    verifyPublished();
    access.audit("QUALIFICATION_SAVE", q.id, p.departmentId);
    return q;
  }

  /** 删除未用于未来发布班次的资格，不删除班次历史快照。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void deleteQualification(Long id, Long version) {
    gate("qualification.manage");
    var q = db.get(Qualification.class, id);
    var p = db.get(ShiftPost.class, q.postId);
    access.department(p.departmentId);
    version(q.version, version);
    access.audit("QUALIFICATION_DELETE", id, p.departmentId);
    db.delete(q);
    verifyPublished();
  }

  /** 本人未来班次、待知悉和可处理的替班请求，范围与详情一致。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object workbench() {
    access.require("roster.read");
    Long me = access.current().id;
    var mine =
        db.query(
            Shift.class,
            "from Shift where employeeId=?1 and status='PUBLISHED' and endsAt>?2 order by startsAt,id",
            me,
            clock.instant());
    var requests = new ArrayList<Coverage>();
    if (has("cover.write") || has("cover.approve")) {
      for (var c : db.all(Coverage.class)) {
        var s = db.get(Shift.class, c.shiftId);
        if (!s.startsAt.isAfter(clock.instant())) continue;
        if (c.status.equals("REQUESTED") && c.targetId.equals(me)
            || c.status.equals("ACCEPTED")
                && has("cover.approve")
                && access.visible(s.departmentId)
                && !me.equals(c.targetId)
                && !me.equals(c.requesterId)) requests.add(c);
      }
    }
    return Map.of("mine", mine, "reviews", requests);
  }

  /** 授权范围内计划时长与未知悉、替班统计，不当作实际出勤。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object dashboard() {
    access.require("roster.read");
    access.require("dashboard");
    var rows =
        db.jpql(Shift.class, "from Shift s where " + shiftScope())
            .setMaxResults(10001)
            .getResultList();
    if (rows.size() > 10000) throw new Problem(400, "REPORT_LIMIT");
    long minutes = 0, unknown = 0, upcoming = 0;
    Map<String, Long> counts = new TreeMap<>();
    for (var s : rows) {
      counts.merge(s.status, 1L, Long::sum);
      if (s.status.equals("PUBLISHED") && s.endsAt.isAfter(clock.instant())) {
        upcoming++;
        minutes += Duration.between(s.startsAt, s.endsAt).toMinutes();
        if (s.acknowledgedAt == null) unknown++;
      }
    }
    long pending =
        has("cover.write") || has("cover.approve")
            ? db.jpql(
                    Long.class,
                    "select count(c) from Coverage c where "
                        + coverScope()
                        + " and c.status in ('REQUESTED','ACCEPTED') and exists(select s.id from Shift s where s.id=c.shiftId and s.startsAt>:now)")
                .setParameter("now", clock.instant())
                .getSingleResult()
            : 0;
    return Map.of(
        "total",
        rows.size(),
        "upcoming",
        upcoming,
        "unacknowledged",
        unknown,
        "pending",
        pending,
        "minutes",
        minutes,
        "states",
        counts);
  }

  /** 审计按全部、部门、本人三种范围，不包含人员不可排班正文。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object audit() {
    access.require("audit");
    return db.all(AuditEvent.class).stream()
        .filter(
            e ->
                access.role().scope.equals("ALL")
                    || access.role().scope.equals("DEPARTMENT") && access.visible(e.departmentId)
                    || e.actor.equals(access.current().username))
        .sorted(Comparator.comparing((AuditEvent e) -> e.id).reversed())
        .toList();
  }

  /** 导出授权班次及不可修改快照，不添加联系方式或凭证。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public String export(Long id) {
    access.require("export");
    return json.writeValueAsString(detail(id));
  }

  /** 管理变更也必须保留未来发布班次的有效岗位和资格；只验证真实约束，不推断出勤。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void verifyPublished() {
    for (var s :
        db.query(
            Shift.class, "from Shift where status='PUBLISHED' and endsAt>?1", clock.instant())) {
      var p = db.get(ShiftPost.class, s.postId);
      if (!p.enabled || !p.departmentId.equals(s.departmentId))
        throw new Problem(409, "PUBLISHED_CONFLICT");
      var a = db.get(Account.class, s.employeeId);
      if (!a.enabled
          || !a.departmentId.equals(s.departmentId)
          || !db.get(AccessRole.class, a.roleId).permissions.contains("roster.read")
          || !qualified(s, a.id)) throw new Problem(409, "PUBLISHED_CONFLICT");
    }
  }

  private void gate(String permission) {
    db.lock(Department.class, 1L);
    var a = access.current();
    db.refresh(a);
    db.refresh(db.get(AccessRole.class, a.roleId));
    access.require("roster.read");
    if (permission.equals("cover.any")) requireCover();
    else access.require(permission);
  }

  private void fill(Shift s, ShiftInput v) {
    var p = db.get(ShiftPost.class, v.postId);
    access.department(p.departmentId);
    if (!p.enabled) throw new Problem(400, "POST_DISABLED");
    var a = db.get(Account.class, v.employeeId);
    if (!a.enabled
        || !a.departmentId.equals(p.departmentId)
        || !db.get(AccessRole.class, a.roleId).permissions.contains("roster.read"))
      throw new Problem(400, "INVALID_EMPLOYEE");
    RosterPolicy.times(v.startsAt, v.endsAt, clock.instant(), setting("scheduleHorizonDays"));
    if (db.query(
            DictionaryEntry.class,
            "from DictionaryEntry where type='shift' and code=?1",
            v.category)
        .isEmpty()) throw new Problem(400, "INVALID_DICTIONARY");
    s.postId = p.id;
    s.departmentId = p.departmentId;
    s.employeeId = a.id;
    s.title = AdminService.text(v.title, 160);
    s.category = v.category;
    s.note = AdminService.text(v.note, 2000);
    s.startsAt = v.startsAt;
    s.endsAt = v.endsAt;
  }

  private boolean qualified(Shift s, Long employee) {
    return db
        .query(
            Qualification.class,
            "from Qualification where accountId=?1 and postId=?2 and enabled=true",
            employee,
            s.postId)
        .stream()
        .anyMatch(q -> RosterPolicy.covers(q.validFrom, q.validUntil, s.startsAt, s.endsAt));
  }

  private void eligible(Shift s, Long employee) {
    var a = db.get(Account.class, employee);
    var p = db.get(ShiftPost.class, s.postId);
    if (!p.enabled
        || !p.departmentId.equals(s.departmentId)
        || !a.enabled
        || !a.departmentId.equals(s.departmentId)
        || !db.get(AccessRole.class, a.roleId).permissions.contains("roster.read"))
      throw new Problem(409, "INVALID_EMPLOYEE");
    if (!qualified(s, employee)) throw new Problem(409, "NOT_QUALIFIED");
    for (var u :
        db.query(
            Unavailability.class,
            "from Unavailability where accountId=?1 and status='ACTIVE'",
            employee))
      if (RosterPolicy.conflicts(s.startsAt, s.endsAt, u.startsAt, u.endsAt, 0))
        throw new Problem(409, "UNAVAILABLE");
    for (var pshift : published(employee))
      if (!Objects.equals(pshift.id, s.id)
          && RosterPolicy.conflicts(
              s.startsAt,
              s.endsAt,
              pshift.startsAt,
              pshift.endsAt,
              Math.max(s.restGapHours, pshift.restGapHours)))
        throw new Problem(409, "SHIFT_CONFLICT");
  }

  private List<Shift> published(Long employee) {
    return db.query(Shift.class, "from Shift where employeeId=?1 and status='PUBLISHED'", employee);
  }

  private Shift readShift(Long id) {
    access.require("roster.read");
    if (id == null) invalid();
    var s = db.get(Shift.class, id);
    var scope = access.role().scope;
    boolean own =
        s.publishedAt != null
            && (s.employeeId.equals(access.current().id)
                || !db.query(
                        Coverage.class,
                        "from Coverage where shiftId=?1 and requesterId=?2 and status='APPROVED'",
                        id,
                        access.current().id)
                    .isEmpty());
    if (!scope.equals("ALL")
        && !(scope.equals("DEPARTMENT") && access.visible(s.departmentId))
        && !own) throw new Problem(403, "OUT_OF_SCOPE");
    return s;
  }

  private Coverage readCover(Long id) {
    requireCover();
    if (id == null) invalid();
    var c = db.get(Coverage.class, id);
    var s = db.get(Shift.class, c.shiftId);
    if (!access.role().scope.equals("ALL")
        && !(access.role().scope.equals("DEPARTMENT") && access.visible(s.departmentId))
        && !c.requesterId.equals(access.current().id)
        && !c.targetId.equals(access.current().id)) throw new Problem(403, "OUT_OF_SCOPE");
    return c;
  }

  private String shiftScope() {
    String me = access.current().id.toString();
    return switch (access.role().scope) {
      case "ALL" -> "1=1";
      case "DEPARTMENT" -> "s.departmentId=" + access.current().departmentId;
      default ->
          "s.publishedAt is not null and (s.employeeId="
              + me
              + " or exists(select c.id from Coverage c where c.shiftId=s.id and c.requesterId="
              + me
              + " and c.status='APPROVED'))";
    };
  }

  private String coverScope() {
    String me = access.current().id.toString();
    return switch (access.role().scope) {
      case "ALL" -> "1=1";
      case "DEPARTMENT" ->
          "exists(select s.id from Shift s where s.id=c.shiftId and s.departmentId="
              + access.current().departmentId
              + ")";
      default -> "(c.requesterId=" + me + " or c.targetId=" + me + ")";
    };
  }

  private void requireCover() {
    access.require("roster.read");
    if (!has("cover.write") && !has("cover.approve")) throw new Problem(403, "FORBIDDEN");
  }

  private void manage(Shift s) {
    access.require("schedule.manage");
    access.department(s.departmentId);
  }

  private boolean has(String p) {
    return access.role().permissions.contains(p);
  }

  private void unchanged(Coverage c, Shift s) {
    state(s.status, "PUBLISHED");
    if (c.assignmentRevision != s.assignmentRevision || !c.requesterId.equals(s.employeeId))
      throw new Problem(409, "REQUEST_STALE");
  }

  private void invalidate(Shift s, Long except) {
    for (var c :
        db.query(
            Coverage.class,
            "from Coverage where shiftId=?1 and status in ('REQUESTED','ACCEPTED')",
            s.id)) {
      if (Objects.equals(c.id, except)) continue;
      c.status = "STALE";
      c.updatedAt = clock.instant();
      event(s, c, "COVER_STALE", "班次安排已改变", c);
    }
  }

  private void active(Coverage c) {
    if (!Set.of("REQUESTED", "ACCEPTED").contains(c.status))
      throw new Problem(409, "INVALID_STATE");
  }

  private void event(Shift s, Coverage c, String action, String note, Object snapshot) {
    var e = new RosterEvent();
    e.shiftId = s.id;
    e.coverageId = c == null ? null : c.id;
    e.actor = access.current().username;
    e.action = action;
    e.note = note;
    e.snapshot = json.writeValueAsString(snapshot);
    e.createdAt = clock.instant();
    db.save(e);
    access.audit(action, c == null ? s.id : c.id, s.departmentId);
  }

  private CommandStamp replay(String key, String fp) {
    if (key == null || !key.matches("[a-zA-Z0-9_-]{8,80}"))
      throw new Problem(400, "INVALID_REQUEST_KEY");
    var rows =
        db.query(
            CommandStamp.class,
            "from CommandStamp where actor=?1 and requestKey=?2",
            access.current().username,
            key);
    if (rows.isEmpty()) return null;
    var c = rows.getFirst();
    if (!c.fingerprint.equals(fp)) throw new Problem(409, "IDEMPOTENCY_CONFLICT");
    return c;
  }

  private void stamp(String key, String fp, String kind, Long id) {
    var c = new CommandStamp();
    c.actor = access.current().username;
    c.requestKey = key;
    c.fingerprint = fp;
    c.kind = kind;
    c.objectId = id;
    db.save(c);
  }

  private Long typed(CommandStamp s, String kind) {
    if (!s.kind.equals(kind)) throw new Problem(409, "IDEMPOTENCY_CONFLICT");
    return s.objectId;
  }

  private String fingerprint(String action, Object input) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(
                      (action + json.writeValueAsString(input)).getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private int setting(String code) {
    return Integer.parseInt(
        db.query(SystemSetting.class, "from SystemSetting where code=?1", code).getFirst().value);
  }

  private void future(Shift s) {
    if (!s.startsAt.isAfter(clock.instant())) throw new Problem(409, "TOO_LATE");
  }

  private void version(Long actual, Long input) {
    if (input == null || !input.equals(actual)) throw new Problem(409, "STALE_VERSION");
  }

  private void state(String actual, String expected) {
    if (!actual.equals(expected)) throw new Problem(409, "INVALID_STATE");
  }

  private String note(String value) {
    return value == null ? "" : value.trim();
  }

  private String like(String search) {
    return "%"
        + search.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_")
        + "%";
  }

  private void pagination(String search, int page, int size) {
    if (search == null
        || search.length() > 120
        || page < 0
        || page > 100000
        || size < 1
        || size > 100) invalid();
  }

  private void invalid() {
    throw new Problem(400, "INVALID_INPUT");
  }
}
