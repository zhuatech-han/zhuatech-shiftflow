// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import { test } from "node:test";
import assert from "node:assert/strict";
import { actions, coverActions, localInput, instant } from "./schema.js";
const now = "2026-10-05T01:00:00Z",
  s = {
    status: "PUBLISHED",
    startsAt: "2026-10-05T02:00:00Z",
    endsAt: "2026-10-05T03:00:00Z",
    employeeId: 1,
    departmentId: 2,
  },
  me = {
    id: 1,
    departmentId: 2,
    scope: "ASSIGNED",
    permissions: ["roster.own", "cover.write"],
  },
  c = { status: "REQUESTED", requesterId: 1, targetId: 3 };
test("Shanghai conversion round trip", () =>
  assert.equal(instant(localInput(now)), now.replace("Z", ".000Z")));
test("invalid date rejected", () =>
  assert.throws(() => instant("2026-02-30T10:00")));
test("missing offset form rejected", () => assert.throws(() => instant(now)));
test("employee own actions", () =>
  assert.deepEqual(actions(s, me, now), ["acknowledge", "request"]));
test("other employee no actions", () =>
  assert.deepEqual(actions(s, { ...me, id: 3 }, now), []));
test("known schedule has no duplicate acknowledge", () =>
  assert.deepEqual(actions({ ...s, acknowledgedAt: now }, me, now), [
    "request",
  ]));
test("ended shift no actions", () =>
  assert.deepEqual(actions(s, me, s.endsAt), []));
test("manager out of department no action", () =>
  assert.deepEqual(
    actions(
      s,
      {
        ...me,
        id: 4,
        departmentId: 9,
        scope: "DEPARTMENT",
        permissions: ["schedule.manage"],
      },
      now,
    ),
    [],
  ));
test("target accepts then independent approval", () => {
  assert.deepEqual(coverActions(c, s, { ...me, id: 3 }, now), [
    "accept",
    "reject",
  ]);
  assert.deepEqual(
    coverActions(
      { ...c, status: "ACCEPTED" },
      s,
      { ...me, id: 4, scope: "DEPARTMENT", permissions: ["cover.approve"] },
      now,
    ),
    ["approve", "reject"],
  );
});
test("target cannot approve self", () =>
  assert.deepEqual(
    coverActions(
      { ...c, status: "ACCEPTED" },
      s,
      { ...me, id: 3, scope: "ALL", permissions: ["cover.approve"] },
      now,
    ),
    [],
  ));
test("late target can decline but not accept", () =>
  assert.deepEqual(coverActions(c, s, { ...me, id: 3 }, s.startsAt), [
    "reject",
  ]));
test("closed requests no actions", () =>
  assert.deepEqual(coverActions({ ...c, status: "APPROVED" }, s, me, now), []));
