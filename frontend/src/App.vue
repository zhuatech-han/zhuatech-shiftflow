<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, onMounted } from "vue";
import {
  ClipboardList,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Plus,
  RefreshCw,
  X,
  ArrowUpRight,
} from "@lucide/vue";
import { api, resetCsrf } from "./api.js";
import {
  states,
  commands,
  labels,
  errors,
  date,
  localInput,
  instant,
  actions,
  coverActions,
} from "./schema.js";
const me = ref(null),
  lang = ref(localStorage.getItem("shiftflow-language") || "zh"),
  login = ref({ username: "", password: "" }),
  page = ref("workbench"),
  loading = ref(false),
  saving = ref(false),
  error = ref(""),
  success = ref(""),
  options = ref({
    accounts: [],
    departments: [],
    posts: [],
    dictionaries: [],
    settings: [],
  }),
  rows = ref([]),
  total = ref(0),
  offset = ref(0),
  search = ref(""),
  status = ref(""),
  sort = ref("time"),
  mine = ref(false),
  detail = ref(null),
  detailKind = ref("shifts"),
  dialog = ref(null),
  stats = ref({}),
  work = ref({ mine: [], reviews: [] });
const tr = (zh, en) => (lang.value === "zh" ? zh : en);
const pair = (v) => v?.[lang.value === "zh" ? 0 : 1] || "";
const can = (p) => me.value?.permissions.includes(p);
const adminPages = [
  "users",
  "roles",
  "departments",
  "menus",
  "permissions",
  "dictionaries",
  "settings",
];
const pageLabel = computed(() => {
  const m = me.value?.menus.find((x) => x.code === page.value);
  return m ? (lang.value === "zh" ? m.name : m.nameEn) : "";
});
const selected = computed(() => detail.value?.shift);
const adminRows = computed(() =>
  rows.value.filter((r) =>
    JSON.stringify(r).toLowerCase().includes(search.value.toLowerCase()),
  ),
);
const adminVisible = computed(() =>
  adminRows.value.slice(offset.value * 20, offset.value * 20 + 20),
);
const departmentName = (id) =>
  options.value.departments.find((r) => r.id === id)?.name || "#" + id;
const accountName = (id) =>
  options.value.accounts.find((r) => r.id === id)?.name ||
  (id === me.value?.id ? me.value.displayName : "#" + id);
const postName = (id) =>
  options.value.posts.find((r) => r.id === id)?.name || "#" + id;
const field = (key, type = "text", extra = {}) => ({ key, type, ...extra });
const departmentChoices = () =>
  options.value.departments
    .filter((d) => me.value.scope === "ALL" || d.id === me.value.departmentId)
    .map((d) => ({ value: d.id, label: d.name }));
const accountChoices = () =>
  options.value.accounts.map((a) => ({
    value: a.id,
    label: a.name + " · " + departmentName(a.departmentId),
  }));
const postChoices = () =>
  options.value.posts
    .filter((p) => p.enabled)
    .map((p) => ({
      value: p.id,
      label: p.name + " · " + departmentName(p.departmentId),
    }));
const roleChoices = ref([]),
  permissionChoices = ref([]);
const adminFields = {
  users: () => [
    field("username"),
    field("displayName"),
    field("password", "password", { required: !dialog.value?.id }),
    field("roleId", "select", { options: roleChoices.value }),
    field("departmentId", "select", { options: departmentChoices() }),
    field("enabled", "checkbox"),
  ],
  roles: () => [
    field("name"),
    field("scope", "select", {
      options: [
        { value: "ALL", label: tr("全部", "All") },
        {
          value: "DEPARTMENT",
          label: tr("本部门", "Department"),
        },
        {
          value: "ASSIGNED",
          label: tr("本人安排及替班", "Own assignments and coverage"),
        },
      ],
    }),
    field("permissions", "permissions", { options: permissionChoices.value }),
  ],
  departments: () => [field("name")],
  menus: () => [
    field("code", "readonly"),
    field("name"),
    field("nameEn"),
    field("permissionCode", "select", { options: permissionChoices.value }),
    field("position", "number"),
    field("enabled", "checkbox"),
  ],
  permissions: () => [field("code", "readonly"), field("name")],
  dictionaries: () => [
    field("type"),
    field("code"),
    field("name"),
    field("nameEn"),
  ],
  settings: () => [field("code", "readonly"), field("value")],
};
/** 同源错误映射为业务反馈，失效会话返回登录页。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function fail(e) {
  error.value =
    pair(errors[e.message]) ||
    tr(
      "操作未完成，请检查输入后重试",
      "Operation failed. Check inputs and retry.",
    );
  if (e.message === "UNAUTHENTICATED") {
    me.value = null;
    detail.value = null;
    dialog.value = null;
  }
}
async function signIn() {
  saving.value = true;
  error.value = "";
  try {
    resetCsrf();
    me.value = await api("/auth/login", "POST", login.value);
    login.value.password = "";
    await load();
  } catch (e) {
    fail(e);
  } finally {
    saving.value = false;
  }
}
async function signOut() {
  try {
    await api("/auth/logout", "POST");
  } catch (e) {
    fail(e);
  } finally {
    resetCsrf();
    me.value = null;
    detail.value = null;
    dialog.value = null;
  }
}
function language() {
  lang.value = lang.value === "zh" ? "en" : "zh";
  localStorage.setItem("shiftflow-language", lang.value);
}

/** 按实时账号权限读取排班数据。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function load() {
  if (!me.value) return;
  loading.value = true;
  error.value = "";
  try {
    me.value = await api("/auth/me");
    options.value = await api("/options");
    if (can("admin")) {
      roleChoices.value = (await api("/admin/roles")).map((r) => ({
        value: r.id,
        label: r.name,
      }));
      permissionChoices.value = (await api("/admin/permissions")).map((p) => ({
        value: p.code,
        label: p.name,
      }));
    }
    if (!me.value.menus.some((m) => m.code === page.value))
      page.value = me.value.menus[0]?.code || "workbench";
    if (detail.value) {
      detail.value = await api(
        "/" +
          detailKind.value +
          "/" +
          (detailKind.value === "covers"
            ? detail.value.coverage.id
            : selected.value.id),
      );
      return;
    }
    if (["shifts", "covers", "absences"].includes(page.value)) {
      const p = new URLSearchParams({
        page: offset.value,
        size: 20,
        status: status.value,
        search: search.value,
        sort: sort.value,
        mine: mine.value,
      });
      const v = await api("/" + page.value + "?" + p);
      rows.value = v.items;
      total.value = v.total;
    } else if (page.value === "workbench") work.value = await api("/workbench");
    else if (page.value === "dashboard") stats.value = await api("/dashboard");
    else if (page.value === "audit") rows.value = await api("/audit");
    else if (adminPages.includes(page.value))
      rows.value = await api("/admin/" + page.value);
    else rows.value = await api("/" + page.value);
  } catch (e) {
    fail(e);
  } finally {
    loading.value = false;
  }
}
async function navigate(p) {
  page.value = p;
  detail.value = null;
  search.value = "";
  status.value = "";
  offset.value = 0;
  success.value = "";
  await load();
}
async function openDetail(id, kind = "shifts") {
  try {
    detailKind.value = kind;
    detail.value = await api("/" + kind + "/" + id);
    error.value = "";
    success.value = "";
  } catch (e) {
    fail(e);
  }
}
function openShift(s = null) {
  dialog.value = {
    kind: "shifts",
    id: s?.id,
    title: tr(s ? "编辑班次草稿" : "建立班次", s ? "Edit draft" : "New shift"),
    form: s
      ? {
          ...s,
          startsAt: localInput(s.startsAt),
          endsAt: localInput(s.endsAt),
          requestKey: crypto.randomUUID(),
        }
      : {
          postId: postChoices()[0]?.value,
          employeeId: accountChoices()[0]?.value,
          title: "",
          category: options.value.dictionaries[0]?.code,
          note: "",
          startsAt: localInput(
            new Date(
              new Date(options.value.serverNow).getTime() + 86400000,
            ).toISOString(),
          ),
          endsAt: localInput(
            new Date(
              new Date(options.value.serverNow).getTime() + 90000000,
            ).toISOString(),
          ),
          requestKey: crypto.randomUUID(),
        },
    fields: [
      field("title"),
      field("postId", "select", { options: postChoices() }),
      field("employeeId", "select", { options: accountChoices() }),
      field("category", "select", {
        options: options.value.dictionaries.map((d) => ({
          value: d.code,
          label: lang.value === "zh" ? d.name : d.nameEn,
        })),
      }),
      field("startsAt", "datetime-local"),
      field("endsAt", "datetime-local"),
      field("note", "textarea"),
    ],
  };
  error.value = "";
}
async function openCommand(action) {
  try {
    const s = selected.value,
      c = detail.value.coverage;
    const form = {
      version: c ? c.version : s.version,
      requestKey: crypto.randomUUID(),
      note: "",
    };
    let fields = [
      field("note", "textarea", {
        required: !["publish", "acknowledge", "accept"].includes(action),
      }),
    ];
    if (action === "reassign")
      fields.unshift(
        field("employeeId", "select", {
          options: accountChoices().filter((a) => a.value !== s.employeeId),
        }),
      );
    if (action === "request") {
      const choices = await api("/shifts/" + s.id + "/candidates");
      if (!choices.length) {
        error.value = tr(
          "没有符合资格且时间可用的替班同事",
          "No eligible, available covering colleague.",
        );
        return;
      }
      form.shiftId = s.id;
      form.shiftVersion = s.version;
      form.targetId = choices[0].id;
      fields.unshift(
        field("targetId", "select", {
          options: choices.map((a) => ({ value: a.id, label: a.name })),
        }),
      );
    }
    dialog.value = {
      kind: action === "request" ? "request" : "command",
      id: c ? c.id : s.id,
      resource: c ? "covers" : "shifts",
      action,
      title: pair(commands[action]),
      form,
      fields,
    };
    error.value = "";
  } catch (e) {
    fail(e);
  }
}
function openRecord(row = null) {
  const kind = page.value;
  const today = localInput(options.value.serverNow).slice(0, 10);
  let form = {},
    fields = [];
  if (kind === "posts") {
    form = row
      ? { ...row }
      : {
          code: "",
          name: "",
          departmentId: me.value.departmentId,
          enabled: true,
        };
    fields = [
      field("code"),
      field("name"),
      field("departmentId", "select", { options: departmentChoices() }),
      field("enabled", "checkbox"),
    ];
  } else if (kind === "qualifications") {
    form = row
      ? { ...row }
      : {
          accountId: accountChoices()[0]?.value,
          postId: postChoices()[0]?.value,
          validFrom: today,
          validUntil: today.slice(0, 4) + "-12-31",
          enabled: true,
        };
    fields = [
      field("accountId", "select", {
        options: accountChoices(),
        disabled: !!row,
      }),
      field("postId", "select", { options: postChoices(), disabled: !!row }),
      field("validFrom", "date"),
      field("validUntil", "date"),
      field("enabled", "checkbox"),
    ];
  } else {
    form = {
      startsAt: localInput(
        new Date(
          new Date(options.value.serverNow).getTime() + 172800000,
        ).toISOString(),
      ),
      endsAt: localInput(
        new Date(
          new Date(options.value.serverNow).getTime() + 176400000,
        ).toISOString(),
      ),
      note: "",
      requestKey: crypto.randomUUID(),
    };
    fields = [
      field("startsAt", "datetime-local"),
      field("endsAt", "datetime-local"),
      field("note", "textarea"),
    ];
  }
  dialog.value = {
    kind,
    id: row?.id,
    title: tr(
      row ? "编辑资料" : "新增资料",
      row ? "Edit record" : "New record",
    ),
    form,
    fields,
  };
  error.value = "";
}
function cancelAbsence(u) {
  dialog.value = {
    kind: "absence-cancel",
    id: u.id,
    title: tr("撤销不可排班", "Cancel unavailability"),
    form: { version: u.version, requestKey: crypto.randomUUID(), note: "" },
    fields: [field("note", "textarea")],
  };
  error.value = "";
}
async function openAdmin(row = null) {
  error.value = "";
  try {
    if (page.value === "users")
      roleChoices.value = (await api("/admin/roles")).map((r) => ({
        value: r.id,
        label: r.name,
      }));
    if (["roles", "menus"].includes(page.value))
      permissionChoices.value = (await api("/admin/permissions")).map((p) => ({
        value: p.code,
        label: p.name,
      }));
    dialog.value = {
      kind: "admin",
      id: row?.id,
      title: tr(
        row ? "编辑资料" : "新增资料",
        row ? "Edit record" : "New record",
      ),
      form: row
        ? {
            ...row,
            password: "",
            permissions: row.permissions ? [...row.permissions] : [],
          }
        : {
            name: "",
            nameEn: "",
            username: "",
            displayName: "",
            password: "",
            enabled: true,
            roleId: roleChoices.value[0]?.value,
            departmentId: me.value.departmentId,
            scope: "ASSIGNED",
            permissions: [],
            type: "shift",
            code: "",
          },
    };
    dialog.value.fields = adminFields[page.value]();
  } catch (e) {
    fail(e);
  }
}
function openPassword() {
  dialog.value = {
    kind: "password",
    title: tr("修改密码", "Change password"),
    form: { oldPassword: "", newPassword: "" },
    fields: [
      field("oldPassword", "password"),
      field("newPassword", "password"),
    ],
  };
}

/** 失败保留表单及重试键，成功刷新真实记录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function saveDialog() {
  saving.value = true;
  error.value = "";
  try {
    const d = dialog.value,
      f = { ...d.form };
    for (const x of d.fields) {
      if (x.type === "number") f[x.key] = Number(f[x.key]);
      if (x.type === "datetime-local") f[x.key] = instant(f[x.key]);
    }
    let result;
    if (["shifts", "posts", "qualifications", "absences"].includes(d.kind))
      result = await api(
        "/" + d.kind + (d.id ? "/" + d.id : ""),
        d.id ? "PUT" : "POST",
        f,
      );
    else if (d.kind === "request") result = await api("/covers", "POST", f);
    else if (d.kind === "command")
      result = await api(
        "/" + d.resource + "/" + d.id + "/commands/" + d.action,
        "POST",
        f,
      );
    else if (d.kind === "absence-cancel")
      result = await api("/absences/" + d.id + "/cancel", "POST", f);
    else if (d.kind === "admin")
      await api(
        "/admin/" + page.value + (d.id ? "/" + d.id : ""),
        d.id ? "PUT" : "POST",
        f,
      );
    else if (d.kind === "password") {
      await api("/auth/password", "POST", f);
      me.value = null;
      resetCsrf();
    } else if (d.kind === "delete") {
      await api(d.path, "DELETE");
      detail.value = null;
    }
    dialog.value = null;
    success.value = tr("已保存", "Saved");
    if (result && ["shifts", "command", "request"].includes(d.kind))
      await openDetail(
        result.id,
        d.kind === "request" || d.resource === "covers" ? "covers" : "shifts",
      );
    if (me.value) await load();
  } catch (e) {
    fail(e);
  } finally {
    saving.value = false;
  }
}
function openDelete(path) {
  dialog.value = {
    kind: "delete",
    path,
    title: tr("删除未使用资料", "Delete unused record"),
    form: {},
    fields: [],
  };
  error.value = "";
}
async function exportShift() {
  try {
    const data = await api("/shifts/" + selected.value.id + "/report.json");
    const u = URL.createObjectURL(
      new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }),
    );
    const a = document.createElement("a");
    a.href = u;
    a.download = "shift-" + selected.value.id + ".json";
    a.click();
    URL.revokeObjectURL(u);
  } catch (e) {
    fail(e);
  }
}
const genericColumns = computed(
  () =>
    ({
      users: ["username", "displayName", "roleId", "departmentId", "enabled"],
      roles: ["name", "scope", "permissions"],
      menus: ["code", "name", "permissionCode", "position", "enabled"],
      permissions: ["code", "name"],
      settings: ["code", "value"],
      dictionaries: ["type", "code", "name", "nameEn"],
      posts: ["code", "name", "departmentId", "enabled"],
      qualifications: [
        "accountId",
        "postId",
        "validFrom",
        "validUntil",
        "enabled",
      ],
    })[page.value] || ["name"],
);
function cell(row, key) {
  if (key === "departmentId") return departmentName(row[key]);
  if (key === "accountId") return accountName(row[key]);
  if (key === "postId") return postName(row[key]);
  if (key === "enabled")
    return tr(row[key] ? "启用" : "停用", row[key] ? "Enabled" : "Disabled");
  if (key === "roleId")
    return (
      roleChoices.value.find((r) => r.value === row[key])?.label ||
      "#" + row[key]
    );
  if (key === "permissions")
    return row[key]
      .map(
        (p) => permissionChoices.value.find((r) => r.value === p)?.label || p,
      )
      .join(" · ");
  if (key === "scope")
    return pair(
      {
        ALL: ["全部", "All"],
        DEPARTMENT: ["本部门", "Department"],
        ASSIGNED: ["本人安排及替班", "Own assignments and coverage"],
      }[row[key]],
    );
  return row[key];
}
function eventLabel(e) {
  return (
    pair(commands[e.action.toLowerCase().replace("cover_", "")]) ||
    pair(
      {
        CREATE: ["建立班次", "Create shift"],
        EDIT: ["修改草稿", "Edit draft"],
        REQUEST_COVER: ["申请替班", "Request coverage"],
        COVER_ASSIGNED: ["替班改派", "Coverage assignment"],
        COVER_STALE: ["替班失效", "Coverage stale"],
        POST_SAVE: ["维护岗位", "Save post"],
        QUALIFICATION_SAVE: ["维护资格", "Save qualification"],
        UNAVAILABILITY_CREATE: ["登记不可排班", "Record unavailability"],
        UNAVAILABILITY_CANCEL: ["撤销不可排班", "Cancel unavailability"],
        LOGIN: ["登录", "Sign in"],
        PASSWORD_CHANGE: ["修改密码", "Change password"],
      }[e.action],
    ) ||
    tr("系统管理操作", "Administration")
  );
}
const now = computed(() => options.value.serverNow);
const editable = computed(
  () =>
    selected.value?.status === "DRAFT" &&
    can("schedule.manage") &&
    (me.value.scope === "ALL" ||
      (me.value.scope === "DEPARTMENT" &&
        me.value.departmentId === selected.value.departmentId)),
);
onMounted(async () => {
  try {
    me.value = await api("/auth/me");
    await load();
  } catch (e) {
    if (e.message !== "UNAUTHENTICATED") fail(e);
  }
});
</script>
<template>
  <main v-if="!me" class="login-shell">
    <section class="login-brand">
      <img src="/brand/logo.jpg" alt="知华科技" /><span class="brand-sub"
        >SHIFTFLOW</span
      >
      <h1>
        {{ tr("企业排班", "Shift") }}<br />{{
          tr("与替班协作", "scheduling & coverage")
        }}
      </h1>
      <div class="brand-line"></div>
      <p class="small">
        {{
          tr(
            "公开源码学习版 · 非商业源码许可",
            "Learning source edition · Non-commercial license",
          )
        }}
      </p>
    </section>
    <section class="login-form">
      <form @submit.prevent="signIn">
        <span class="eyebrow">{{ tr("账号登录", "ACCOUNT SIGN IN") }}</span>
        <h2>{{ tr("欢迎回来", "Welcome back") }}</h2>
        <label
          >{{ tr("登录账号", "Username")
          }}<input
            v-model="login.username"
            autocomplete="username"
            required
            maxlength="60" /></label
        ><label
          >{{ tr("密码", "Password")
          }}<input
            v-model="login.password"
            type="password"
            autocomplete="current-password"
            required
            maxlength="128"
        /></label>
        <p v-if="error" role="alert" class="error">{{ error }}</p>
        <button class="primary wide" :disabled="saving">
          {{
            tr(saving ? "登录中…" : "登录", saving ? "Signing in…" : "Sign in")
          }}
        </button>
      </form>
      <footer>
        <button @click="language">
          {{ lang === "zh" ? "English" : "中文" }}</button
        ><a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
          >{{ tr("知华科技官网", "ZhuaTech website") }}
          <ArrowUpRight :size="13"
        /></a>
        <p>上海如静知华信息科技有限公司 · 微信 zhuatech / zhuatech2</p>
      </footer>
    </section>
  </main>
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <img src="/brand/logo.jpg" alt="知华科技" />
        <div>
          <strong>ShiftFlow</strong
          ><small>{{ tr("排班与替班协作", "Shift scheduling") }}</small>
        </div>
      </div>
      <nav aria-label="主导航">
        <button
          v-for="m in me.menus"
          :key="m.code"
          :class="{ active: page === m.code }"
          @click="navigate(m.code)"
        >
          <component :is="ClipboardList" :size="18" /><span>{{
            lang === "zh" ? m.name : m.nameEn
          }}</span>
        </button>
      </nav>
      <div class="sidebar-bottom">
        <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
          >{{ tr("知华科技 · 商业咨询", "ZhuaTech · Business") }}
          <ArrowUpRight :size="13" /></a
        ><small>zhuatech / zhuatech2</small>
      </div>
    </aside>
    <section class="workspace">
      <header class="topbar">
        <div class="breadcrumb">ShiftFlow <span>/</span> {{ pageLabel }}</div>
        <div class="account">
          <button @click="language">{{ lang === "zh" ? "EN" : "中文" }}</button
          ><button @click="openPassword">{{ me.displayName }}</button
          ><button :aria-label="tr('退出', 'Sign out')" @click="signOut">
            <LogOut :size="16" />
          </button>
        </div>
      </header>
      <div class="content">
        <div class="page-heading">
          <div>
            <span class="eyebrow">{{
              detail ? "SHIFT DETAIL" : "SHIFTFLOW"
            }}</span>
            <h1>{{ detail ? selected.title : pageLabel }}</h1>
          </div>
          <div class="heading-actions">
            <button :disabled="loading" @click="load">
              <RefreshCw :size="15" />{{ tr("刷新", "Refresh") }}
            </button>
            <button
              v-if="
                can('schedule.manage') &&
                !detail &&
                ['shifts', 'workbench'].includes(page)
              "
              class="primary"
              @click="openShift()"
            >
              <Plus :size="16" />{{ tr("建立班次", "New shift") }}</button
            ><button
              v-if="
                !detail &&
                ((page === 'posts' && can('post.manage')) ||
                  (page === 'qualifications' && can('qualification.manage')) ||
                  (page === 'absences' && can('roster.own')))
              "
              class="primary"
              @click="openRecord()"
            >
              <Plus :size="16" />{{ tr("新增", "New") }}</button
            ><button
              v-if="
                !detail &&
                adminPages.includes(page) &&
                !['menus', 'permissions', 'settings'].includes(page)
              "
              class="primary"
              @click="openAdmin()"
            >
              <Plus :size="16" />{{ tr("新增", "New") }}
            </button>
          </div>
        </div>
        <p v-if="error && !dialog" role="alert" class="error">{{ error }}</p>
        <p v-if="success && !dialog" role="status" class="success">
          {{ success }}
        </p>
        <p v-if="loading" class="muted">{{ tr("正在读取…", "Loading…") }}</p>

        <section v-if="detail" class="detail">
          <button
            class="back"
            @click="
              detail = null;
              load();
            "
          >
            <ChevronLeft :size="15" />{{ tr("返回列表", "Back to list") }}
          </button>
          <div class="panel">
            <div class="detail-summary">
              <span
                class="badge"
                :class="detail.coverage?.status || selected.status"
                >{{
                  pair(states[detail.coverage?.status || selected.status])
                }}</span
              ><span
                >{{ detail.coverage ? "C" : "S"
                }}{{ detail.coverage?.id || selected.id }}</span
              >
            </div>
            <div class="action-row">
              <button
                v-if="editable && !detail.coverage"
                @click="openShift(selected)"
              >
                {{ tr("编辑草稿", "Edit draft") }}</button
              ><button
                v-for="a in detail.coverage
                  ? coverActions(detail.coverage, selected, me, now)
                  : actions(selected, me, now)"
                :key="a"
                class="primary"
                @click="openCommand(a)"
              >
                {{ pair(commands[a]) }}</button
              ><button
                v-if="editable && !detail.coverage"
                @click="
                  openDelete(
                    '/shifts/' + selected.id + '?version=' + selected.version,
                  )
                "
              >
                {{ tr("删除草稿", "Delete draft") }}</button
              ><button
                v-if="can('export') && !detail.coverage"
                @click="exportShift"
              >
                {{ tr("导出记录", "Export record") }}
              </button>
            </div>
            <div class="detail-grid">
              <dl>
                <dt>{{ tr("岗位", "Post") }}</dt>
                <dd>{{ detail.post }}</dd>
                <dt>{{ tr("当前员工", "Current assignee") }}</dt>
                <dd>
                  {{ detail.employee || accountName(selected.employeeId) }}
                </dd>
                <dt>{{ tr("班次时间", "Shift time") }}</dt>
                <dd>
                  {{ date(selected.startsAt) }} → {{ date(selected.endsAt) }}
                </dd>
                <dt>{{ tr("员工知悉", "Acknowledgement") }}</dt>
                <dd>
                  {{
                    selected.acknowledgedAt
                      ? date(selected.acknowledgedAt)
                      : tr("尚未知悉", "Not acknowledged")
                  }}
                </dd>
                <dt>{{ tr("发布间隔规则", "Published gap rule") }}</dt>
                <dd>{{ selected.restGapHours }} h</dd>
              </dl>
              <dl v-if="detail.coverage">
                <dt>{{ tr("申请人", "Requester") }}</dt>
                <dd>{{ detail.requester }}</dd>
                <dt>{{ tr("替班同事", "Covering colleague") }}</dt>
                <dd>{{ detail.target }}</dd>
                <dt>{{ tr("申请说明", "Request note") }}</dt>
                <dd class="preserve">{{ detail.coverage.note }}</dd>
              </dl>
              <dl v-else>
                <dt>{{ tr("部门", "Department") }}</dt>
                <dd>{{ departmentName(selected.departmentId) }}</dd>
                <dt>{{ tr("班次说明", "Shift note") }}</dt>
                <dd class="preserve">{{ selected.note }}</dd>
              </dl>
            </div>
          </div>
          <div class="panel">
            <h2>{{ tr("变更记录", "Change history") }}</h2>
            <ol class="event-list">
              <li v-for="e in detail.events" :key="e.id">
                <div class="event-top">
                  <strong>{{ eventLabel(e) }}</strong
                  ><small>{{ date(e.createdAt) }} · {{ e.actor }}</small>
                </div>
                <p class="preserve">{{ e.note }}</p>
                <details>
                  <summary>
                    {{ tr("查看当时记录", "View recorded snapshot") }}
                  </summary>
                  <pre>{{
                    JSON.stringify(JSON.parse(e.snapshot), null, 2)
                  }}</pre>
                </details>
              </li>
            </ol>
          </div>
        </section>
        <section v-else-if="page === 'workbench'">
          <div class="work-grid">
            <article class="panel">
              <h2>{{ tr("我的班次", "My shifts") }}</h2>
              <div v-for="s in work.mine" :key="s.id" class="work-item">
                <div>
                  <strong>{{ s.title }}</strong>
                  <p>{{ postName(s.postId) }} · {{ date(s.startsAt) }}</p>
                  <p>
                    {{
                      tr(
                        s.acknowledgedAt ? "已知悉" : "待知悉",
                        s.acknowledgedAt ? "Acknowledged" : "Not acknowledged",
                      )
                    }}
                  </p>
                </div>
                <button @click="openDetail(s.id)">
                  {{ tr("查看", "View") }}
                </button>
              </div>
              <p v-if="!work.mine.length" class="empty">
                {{ tr("暂无安排", "No assignments") }}
              </p>
            </article>
            <article class="panel">
              <h2>{{ tr("替班待办", "Coverage tasks") }}</h2>
              <div v-for="c in work.reviews" :key="c.id" class="work-item">
                <div>
                  <strong>C{{ c.id }} · {{ pair(states[c.status]) }}</strong>
                  <p>
                    {{ accountName(c.requesterId) }} →
                    {{ accountName(c.targetId) }}
                  </p>
                </div>
                <button @click="openDetail(c.id, 'covers')">
                  {{ tr("处理", "Review") }}
                </button>
              </div>
              <p v-if="!work.reviews.length" class="empty">
                {{ tr("暂无待办", "No tasks") }}
              </p>
            </article>
          </div>
        </section>
        <section v-else-if="['shifts', 'covers', 'absences'].includes(page)">
          <div class="filters" v-if="page !== 'absences'">
            <input
              v-if="page === 'shifts'"
              v-model="search"
              :placeholder="tr('搜索班次名称', 'Search shift title')"
              @change="
                offset = 0;
                load();
              "
            /><select
              v-model="status"
              :aria-label="tr('状态筛选', 'Status filter')"
              @change="
                offset = 0;
                load();
              "
            >
              <option value="">{{ tr("全部状态", "All states") }}</option>
              <option
                v-for="s in page === 'shifts'
                  ? ['DRAFT', 'PUBLISHED', 'CANCELLED']
                  : [
                      'REQUESTED',
                      'ACCEPTED',
                      'APPROVED',
                      'REJECTED',
                      'WITHDRAWN',
                      'STALE',
                    ]"
                :key="s"
                :value="s"
              >
                {{ pair(states[s]) }}
              </option></select
            ><select
              v-if="page === 'shifts'"
              v-model="sort"
              :aria-label="tr('排序', 'Sort')"
              @change="
                offset = 0;
                load();
              "
            >
              <option value="time">{{ tr("班次时间", "Shift time") }}</option>
              <option value="newest">{{ tr("最新建立", "Newest") }}</option>
              <option value="title">{{ tr("名称", "Title") }}</option></select
            ><label v-if="page === 'shifts'" class="check-inline"
              ><input
                type="checkbox"
                v-model="mine"
                @change="
                  offset = 0;
                  load();
                "
              />{{ tr("仅当前指派给我", "Currently assigned to me") }}</label
            >
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ tr("记录", "Record") }}</th>
                  <th>{{ tr("人员", "People") }}</th>
                  <th>{{ tr("时间 / 岗位", "Time / post") }}</th>
                  <th>{{ tr("状态", "State") }}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in rows" :key="r.id">
                  <td>
                    <strong>{{
                      page === "shifts"
                        ? r.title
                        : (page === "covers" ? "C" : "U") + r.id
                    }}</strong
                    ><small>{{
                      page === "shifts"
                        ? "S" + r.id
                        : page === "covers"
                          ? "S" + r.shiftId
                          : r.note
                    }}</small>
                  </td>
                  <td>
                    {{
                      accountName(r.employeeId || r.accountId || r.requesterId)
                    }}<small v-if="r.targetId"
                      >→ {{ accountName(r.targetId) }}</small
                    >
                  </td>
                  <td v-if="page === 'covers'">
                    {{ tr("班次", "Shift") }} S{{ r.shiftId
                    }}<small>{{ date(r.createdAt) }}</small>
                  </td>
                  <td v-else>
                    {{ date(r.startsAt) }}<small>→ {{ date(r.endsAt) }}</small
                    ><small v-if="r.postId">{{ postName(r.postId) }}</small>
                  </td>
                  <td>
                    <span class="badge" :class="r.status">{{
                      pair(states[r.status])
                    }}</span
                    ><small
                      v-if="page === 'shifts' && r.status === 'PUBLISHED'"
                      >{{
                        new Date(r.endsAt) <= new Date(now)
                          ? tr("计划已结束", "Scheduled end passed")
                          : tr(
                              r.acknowledgedAt ? "已知悉" : "待知悉",
                              r.acknowledgedAt
                                ? "Acknowledged"
                                : "Not acknowledged",
                            )
                      }}</small
                    >
                  </td>
                  <td>
                    <button
                      v-if="page !== 'absences'"
                      @click="openDetail(r.id, page)"
                    >
                      {{ tr("查看", "View") }}</button
                    ><button
                      v-else-if="
                        can('roster.own') &&
                        r.accountId === me.id &&
                        r.status === 'ACTIVE' &&
                        new Date(r.startsAt) > new Date(now)
                      "
                      @click="cancelAbsence(r)"
                    >
                      {{ tr("撤销", "Cancel") }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <p v-if="!rows.length" class="empty">
              {{ tr("暂无记录", "No records") }}
            </p>
          </div>
          <div class="pagination">
            <span>{{ tr("共", "Total") }} {{ total }}</span
            ><button
              :disabled="offset === 0"
              :aria-label="tr('上一页', 'Previous page')"
              @click="
                offset--;
                load();
              "
            >
              <ChevronLeft :size="15" /></button
            ><span>{{ offset + 1 }}</span
            ><button
              :disabled="(offset + 1) * 20 >= total"
              :aria-label="tr('下一页', 'Next page')"
              @click="
                offset++;
                load();
              "
            >
              <ChevronRight :size="15" />
            </button>
          </div>
        </section>
        <section v-else-if="page === 'dashboard'">
          <div class="metrics">
            <article
              v-for="[key, zh, en] in [
                ['total', '授权班次', 'Visible shifts'],
                ['upcoming', '未结束班次', 'Upcoming shifts'],
                ['unacknowledged', '尚未知悉', 'Not acknowledged'],
                ['pending', '替班待处理', 'Pending coverage'],
              ]"
              :key="key"
            >
              <span>{{ tr(zh, en) }}</span
              ><strong>{{ stats[key] || 0 }}</strong>
            </article>
          </div>
          <div class="panel">
            <h2>{{ tr("班次状态分布", "Shifts by state") }}</h2>
            <div
              v-for="(value, key) in stats.states"
              :key="key"
              class="stat-row"
            >
              <strong>{{ pair(states[key]) }}</strong>
              <div class="bar">
                <span
                  :style="{
                    width: (value / Math.max(1, stats.total)) * 100 + '%',
                  }"
                ></span>
              </div>
              <span>{{ value }}</span>
            </div>
            <p class="small">
              {{
                tr(
                  "未结束班次的完整计划时长",
                  "Full planned duration of upcoming shifts",
                )
              }}
              · {{ stats.minutes || 0 }} min
            </p>
          </div>
        </section>
        <section v-else-if="page === 'audit'">
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ tr("时间", "Time") }}</th>
                  <th>{{ tr("操作人", "Actor") }}</th>
                  <th>{{ tr("动作", "Action") }}</th>
                  <th>{{ tr("记录编号", "Record") }}</th>
                  <th>{{ tr("部门", "Department") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="e in rows" :key="e.id">
                  <td>{{ date(e.createdAt) }}</td>
                  <td>{{ e.actor }}</td>
                  <td>{{ eventLabel(e) }}</td>
                  <td>{{ e.objectId }}</td>
                  <td>{{ departmentName(e.departmentId) }}</td>
                </tr>
              </tbody>
            </table>
            <p v-if="!rows.length" class="empty">
              {{ tr("暂无记录", "No records") }}
            </p>
          </div>
        </section>
        <section
          v-else-if="
            adminPages.includes(page) ||
            ['posts', 'qualifications'].includes(page)
          "
        >
          <div class="filters">
            <label class="search"
              ><Search :size="16" /><input
                v-model="search"
                :placeholder="tr('搜索资料', 'Search records')"
                @input="offset = 0"
            /></label>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th v-for="key in genericColumns" :key="key">
                    {{ pair(labels[key]) }}
                  </th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in adminVisible" :key="row.id">
                  <td v-for="key in genericColumns" :key="key">
                    {{ cell(row, key) }}
                  </td>
                  <td>
                    <button
                      v-if="
                        adminPages.includes(page) ||
                        (page === 'posts' && can('post.manage')) ||
                        (page === 'qualifications' &&
                          can('qualification.manage'))
                      "
                      @click="
                        adminPages.includes(page)
                          ? openAdmin(row)
                          : openRecord(row)
                      "
                    >
                      {{ tr("编辑", "Edit") }}</button
                    ><button
                      v-if="
                        !['menus', 'permissions', 'settings'].includes(page) &&
                        (adminPages.includes(page) ||
                          (page === 'posts' && can('post.manage')) ||
                          (page === 'qualifications' &&
                            can('qualification.manage')))
                      "
                      @click="
                        openDelete(
                          (adminPages.includes(page) ? '/admin/' : '/') +
                            page +
                            '/' +
                            row.id +
                            (adminPages.includes(page)
                              ? ''
                              : '?version=' + row.version),
                        )
                      "
                    >
                      {{ tr("删除", "Delete") }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination">
            <span>{{ tr("共", "Total") }} {{ adminRows.length }}</span
            ><button :disabled="offset === 0" @click="offset--">
              <ChevronLeft :size="15" /></button
            ><span>{{ offset + 1 }}</span
            ><button
              :disabled="(offset + 1) * 20 >= adminRows.length"
              @click="offset++"
            >
              <ChevronRight :size="15" />
            </button>
          </div>
        </section>
        <footer class="app-footer">
          <span>© 2026 上海如静知华信息科技有限公司</span
          ><span
            >ShiftFlow 0.1.0 ·
            {{
              tr(
                "公开源码学习版／非商业源码版，未经书面授权不得商用",
                "Learning source edition / non-commercial; written permission required for commercial use",
              )
            }}</span
          >
        </footer>
      </div>
    </section>
  </div>
  <div
    v-if="dialog"
    class="modal-backdrop"
    @click.self="!saving && (dialog = null)"
  >
    <section
      class="modal"
      role="dialog"
      aria-modal="true"
      :aria-label="dialog.title"
    >
      <header>
        <h2>{{ dialog.title }}</h2>
        <button
          :disabled="saving"
          :aria-label="tr('关闭', 'Close')"
          @click="dialog = null"
        >
          <X :size="20" />
        </button>
      </header>
      <form @submit.prevent="saveDialog">
        <p v-if="dialog.kind === 'delete'" class="muted">
          {{
            tr(
              "仅可删除没有业务引用的资料或未发布草稿。此操作会删除该记录。",
              "Only unused records or never-published drafts may be deleted. This removes the record.",
            )
          }}
        </p>
        <div class="form-grid">
          <label
            v-for="f in dialog.fields"
            :key="f.key"
            :class="{
              full: f.type === 'textarea' || f.type === 'permissions',
              'check-inline': f.type === 'checkbox',
            }"
            ><span>{{ pair(labels[f.key]) }}</span
            ><template v-if="f.type === 'permissions'"
              ><div class="permission-options">
                <label
                  v-for="p in f.options"
                  :key="p.value"
                  class="check-inline"
                  ><input
                    v-model="dialog.form[f.key]"
                    type="checkbox"
                    :value="p.value"
                  />{{ p.label }}</label
                >
              </div></template
            ><textarea
              v-else-if="f.type === 'textarea'"
              v-model="dialog.form[f.key]"
              :required="f.required !== false"
              :maxlength="
                dialog.kind === 'shifts'
                  ? 2000
                  : dialog.kind === 'absences'
                    ? 500
                    : 1000
              "
              rows="3"
            ></textarea
            ><select
              v-else-if="f.type === 'select'"
              v-model="dialog.form[f.key]"
              :disabled="f.disabled"
              :required="f.required !== false"
            >
              <option v-for="o in f.options" :key="o.value" :value="o.value">
                {{ o.label }}
              </option></select
            ><input
              v-else-if="f.type === 'checkbox'"
              v-model="dialog.form[f.key]"
              type="checkbox" />
            <input
              v-else
              v-model="dialog.form[f.key]"
              :type="f.type === 'readonly' ? 'text' : f.type"
              :readonly="f.type === 'readonly'"
              :required="f.required !== false && f.type !== 'readonly'"
              :min="f.min"
              :max="f.max"
              :step="f.type === 'datetime-local' ? 60 : f.step || 1"
              :maxlength="
                ['password', 'oldPassword', 'newPassword'].includes(f.key)
                  ? 128
                  : f.key === 'code'
                    ? 60
                    : f.key === 'title'
                      ? 160
                      : 120
              "
              :autocomplete="f.type === 'password' ? 'new-password' : 'off'"
          /></label>
        </div>
        <p v-if="error" role="alert" class="error">{{ error }}</p>
        <footer>
          <button type="button" :disabled="saving" @click="dialog = null">
            {{ tr("取消", "Cancel") }}</button
          ><button class="primary" :disabled="saving">
            {{
              tr(saving ? "保存中…" : "确认", saving ? "Saving…" : "Confirm")
            }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
