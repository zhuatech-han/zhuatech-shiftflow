[中文](README.md) | [English](README.en.md)

<img src="frontend/public/brand/logo.jpg" width="160" alt="ZhiHua Technology official logo">

# ZhiHua ShiftFlow · Employee Shift Scheduling and Substitute Approval

**Public source learning edition 0.1.0 · Non-commercial use.**

ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.) · [Official website](https://www.zhuatech.cn/). Existing [ZhuaTech Non-Commercial Source License 1.0](LICENSE); commercial use requires written authorization.

ShiftFlow records department posts, employee eligibility, shift times, acknowledgement and independently approved substitution in one plan. It suits small service teams, operations departments and internal duty organizations, and enterprise software learning. Eligibility records are internal scheduling authorization, not legal qualification certification. Plans and acknowledgements do not establish actual attendance.

## Workflow and user/administration workspaces

1. Administrators create departments/accounts; schedulers maintain posts and qualification validity.
2. Schedulers create drafts. Publication checks enabled employees, qualifications covering the entire shift, unavailability, overlapping published plans and the required rest interval.
3. Employees view their own published assignments and acknowledge awareness. The server records the timestamp.
4. An employee proposes a designated eligible colleague. The original assignment remains effective.
5. The colleague accepts, then a supervisor independent of both participants reviews the request.
6. Approval rechecks qualification/conflicts and reassigns transactionally, clearing the previous acknowledgement. The new employee acknowledges again.

Published details freeze. Cancellation/reassignment require reasons before the start; they invalidate active substitution requests. Published history cannot be deleted, while unpublished drafts can be edited/deleted. After the start, substitution cannot be accepted/approved, but active requests can still be rejected/withdrawn to record their outcome. Acknowledgement ends at the shift's end.

| Module/workspace | Implemented operations |
| --- | --- |
| Employee workspace | Own unfinished shifts, acknowledgement, participating substitution requests and own unavailability |
| Scheduler/supervisor | Draft CRUD, publication, pre-start cancellation/reassignment, qualification/post maintenance, independent substitute approval |
| Substitution | Named candidate filtering by eligibility/time, acceptance, independent approval, rejection, withdrawal, invalidation and paginated status filters |
| Unavailability | Own registration/pre-start cancellation, published-shift conflict protection and authorized reading |
| Posts/qualification | Department posts and effective dates; protections against invalidating published plans through disabling/deletion or employee role/department changes |
| Reports | Scoped statistics, full planned minutes, authorized shift JSON reports and immutable event snapshots |
| Administration | Users, roles, registered permissions/navigation, departments, dictionaries, parameters, last-administrator protection and audit |
| Shared features | BCrypt, server sessions, CSRF, login-failure limits, structured errors, health, empty initialization and versioned migrations |
| UI | Chinese/English switching, responsive pages, official logo, website and authorization entry |

Functional permissions and `ALL`/`DEPARTMENT`/`ASSIGNED` scopes are enforced by the backend. Assigned scope includes current published assignments, approved original substitution history, participating requests and own unavailability. A proposed substitute can see the associated shift to make a decision, without unrestricted access to other requests or colleagues' unavailability text.

## Limits and unfinished functions

One employee per shift; maximum twelve hours across midnight. The registration horizon is 1–180 days, default90. Rest interval is 0–24 hours, default8, captured on publication; comparisons use the larger interval of the two plans. Qualifications use inclusive Shanghai calendar dates, with an ending instant excluded from the following date. Intervals are half-open. Unavailability is at most31days per segment and starts no later than180days ahead; medical/leave evidence is not collected.

Timestamps persist in UTC; UI uses fixed Shanghai time. An elapsed plan remains `PUBLISHED` with an ended indicator; it does not automatically certify attendance. Planned minutes cover complete unfinished shifts and are not payroll/attendance figures.

Automatic scheduling, two-way swaps, open bidding, work-hour accounting, clock-in, payroll, leave approval, labor-compliance decisions, notifications, AI scheduling and multi-tenancy are not implemented. There are no paid model or external business calls and no static business demonstration mode. Empty deployment creates no employee/post/qualification/shift examples; acceptance scripts create marked fictional records in isolated test databases.

All business and account writes serialize on the base department row under READ COMMITTED and reread current account/role after locking. Versions and per-actor command keys protect updates; the same key with different content conflicts, while exact retry returns the current object without repeating events. Published assignments protect affected accounts, roles, posts and qualifications. This single-instance design has no high-concurrency guarantee, distributed locking or SaaS isolation. Management/post/qualification/audit reads are capped at10,000; statistics above10,000 reject with `REPORT_LIMIT`. Large deployments need bounded task queries, retention and capacity assessment.

## Actual running pages

Images show the actual learning version with fictional acceptance data. They illustrate authentication, own tasks, scoped plans, accepted/reviewed substitution, access scopes and planned-time statistics.

### Login
![Login](docs/screenshots/login.jpg)

### Supervisor workbench
Own assignments and substitution acceptance/review tasks.
![Workbench](docs/screenshots/workbench.jpg)

### Shift plans
Authorized search, filters and current assignments.
![Shifts](docs/screenshots/shifts.jpg)

### Substitution handover
Recorded colleague acceptance and independent supervisor review.
![Coverage detail](docs/screenshots/coverage.jpg)

### Roles and scope
Functional permissions plus all/department/assigned access.
![Roles](docs/screenshots/roles.jpg)

### Scoped statistics
Planned statuses and full planned minutes, without actual attendance claims.
![Statistics](docs/screenshots/dashboard.jpg)

## Architecture, environment and layout

Browser → same-origin Nginx → Spring Boot API → MySQL8.4, with Flyway migrations and JPA validation. Backend sessions are memory-local and require login after restart; business data stays in the database volume. Only the frontend exposes a default loopback host port.

| Layer | Version |
| --- | --- |
| Backend | Java21, Maven3.9, Spring Boot4.0.7, Security, JPA, Flyway |
| Database | MySQL8.4, MariaDB JDBC3.5.10; H2 MySQL mode for integration tests only |
| Frontend | Node24.19.0+, Vue3.5.40, Vite8.1.5, Lucide1.48.0, ESLint/Prettier |
| Deployment | Docker Engine/Desktop, Composev2, BuildKit, Nginx1.29; non-root application/frontend |
| Configuration/acceptance | Python3 and access to official registries |

```text
backend/                         Java API, authorization and roster rules
  src/main/resources/db/migration/ V1 identity directory, V2 roster
  src/test/                      HTTP integration and time-boundary tests
frontend/                        Vue, same-origin API and UI-rule tests
compose.yaml                     Independent database volume and health dependencies
scripts/                         Configuration, isolated acceptance and release checks
docs/                            Operations, architecture, API, database, security, deployment and screenshots
```

See [architecture](docs/architecture.md) and [API](docs/api.md). Server-registered menu/permission codes cannot invent unimplemented routes; hiding a menu does not replace API checks.

## Installation and database initialization

```sh
git clone https://github.com/zhuatech-han/zhuatech-shiftflow.git
cd zhuatech-shiftflow
python3 scripts/init-env.py
docker compose -p shiftflow-local config --quiet
docker compose -p shiftflow-local up -d --build --wait
```

Open [http://127.0.0.1:8104](http://127.0.0.1:8104), with [health](http://127.0.0.1:8104/actuator/health). Login is `admin`; privately read generated `ADMIN_PASSWORD` from ignored `.env`. There is no fixed demonstration password. The script creates independent strong database/app/admin credentials, uses0600 access and refuses existing-file overwrite. Restart does not reset passwords or data.

Flyway applies V1/V2 before initializing headquarters, four roles, eleven permissions, fifteen menus, three shift categories, four parameters and the administrator. It does not create business examples. Create departments, at least two employees and one independent supervisor, then posts and qualifications; follow [operations](docs/operations.md).

| Variable | Purpose |
| --- | --- |
| `DATABASE_PASSWORD` | Dedicated shiftflow database-user password |
| `MYSQL_ROOT_PASSWORD` | MySQL initialization maintenance password |
| `ADMIN_PASSWORD` | Empty-database admin password; later environment changes do not reset accounts |
| `WEB_PORT` | Default8104; choose a free port without stopping unrelated services |
| `BIND_ADDRESS` | Default127.0.0.1; configure authorized HTTPS access for other machines |
| `COOKIE_SECURE` | False locally on HTTP; true for properly configured HTTPS |

[.env.example](.env.example) publishes names only. Source backend additionally accepts `DATABASE_URL`, `DATABASE_USER` and `DATABASE_CATALOG`; catalog must match the actual database. Compose fixes its database/user to `zhuatech_shiftflow`/`shiftflow`.

For local source development use an independent host-accessible MySQL8.4 database, configure those variables plus `DATABASE_PASSWORD`/`ADMIN_PASSWORD`, run `mvn spring-boot:run` in `backend`, and `npm ci`/`npm run dev` in a separate `frontend` terminal. Vite binds127.0.0.1 and proxies `/api`/`/actuator` to127.0.0.1:8080. Docker VM internal MySQL addresses usually cannot be reached from macOS/Windows; full Compose is recommended there. Linux can use the documented container-address method under controlled local configuration. See [deployment](docs/deployment.md).

## Migration, deployment and recovery

Database `zhuatech_shiftflow`: V1 identity/account/role/permissions/menu/department/dictionary/settings/audit and V2 posts/qualification/shifts/coverage/unavailability/events/idempotency, with foreign keys, indexes and uniqueness. See [database](docs/database.md). JPA validates, without auto-repairing schemas.

Before upgrades pause writes, back up the project database and private configuration, rehearse restoration to a separate empty instance, inspect/add increasing migrations such asV3, and verify health/login/full workflow. Never edit executed V1/V2, delete migration history, disable validation or delete real data volumes to hide upgrade failures. Restore matching backup and application images when rolling back.

```sh
docker compose -p shiftflow-local exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction zhuatech_shiftflow' > shiftflow-backup.sql
docker compose -p shiftflow-local restart mysql
docker compose -p shiftflow-local up -d --wait mysql
docker compose -p shiftflow-local restart backend frontend
docker compose -p shiftflow-local up -d --wait
docker compose -p shiftflow-local down
```

Keep backups private; verify original assignments, acknowledgements, approved coverage, events and unavailability after independent restoration. `down` retains the database volume; only a named disposable test instance may use `down -v`.

Commercial deployment needs written authorization, trusted HTTPS/secure cookies, appropriate binding/firewalls, database isolation, least privilege, backups, audit retention and independent security/capacity review. Learning database TLS uses `sslMode=trust` without CA/hostname validation; deployed connections need trusted certificates and suitable `verify-full` configuration. Backend runtime includes official Java21/Maven; application/frontend run non-root. No production availability or compliance certification is provided.

## Verification, errors and contribution

```sh
cd frontend
npm ci
npm run format:check
npm run lint
npm test
npm run build
cd ../backend
# Privately inject a strong TEST_ADMIN_PASSWORD containing uppercase/lowercase/digits.
mvn spotless:check test package
cd ..
docker compose -p shiftflow-check config --quiet
docker compose -p shiftflow-check up -d --build --wait
# Independent, fresh, disposable localhost database only:
python3 scripts/smoke.py --run
# Restart MySQL and wait for health before restarting applications as documented above.
python3 scripts/smoke.py --verify
python3 scripts/release-check.py
git diff --check
```

The script creates marked fictional records and saves restricted credentials in ignored `.smoke-state.json`; it refuses repeat creation and never prints passwords. Do not use a business database. Use the matching `--base http://127.0.0.1:<port>` for a custom test port. Ordinary `--verify` checks automated assignments, acknowledgement, approved coverage, eight events and unavailability. `--verify --verify-ui` additionally requires the separate handover fixture to have been completed through actual UI operations. H2 tests are not real MySQL deployment acceptance. Docker builds retain all tests; [testing](docs/testing.md) records methods and existing coverage without production certification.

| Symptom | Check |
| --- | --- |
| Startup fails | Required private variables, available port and scoped logs without exposing credentials |
| Migration/entity mismatch | Actual migration/database/schema; do not overwrite verification history |
| Login fails | Original empty-database password and current account status; restart requires login |
| Publication refused | Post, enabled employee, department, full qualification dates, unavailability and rest interval |
| Employee/qualification change refused | Reassign/cancel future shifts; wait for ongoing plans to end |
| Substitute approval refused | Target acceptance, independent supervisor, unstarted shift and current eligibility |
| State/version conflict | Refresh details; different input must not reuse an old request key |

Authentication uses BCrypt12, 30-minute HttpOnly/SameSite=Strict sessions, CSRF and eight failed IP/account attempts followed by five-minute restriction. Account disabling/password/role changes are checked on later requests. Password hashes are hidden and logged authentication payloads are excluded. There is no file upload or variable download path. See [security](docs/security.md).

Issues need version, steps and redacted error codes. Contributions require passing format/tests/build/diff checks and respecting data/permission boundaries; never submit customer data, credentials, logs or generated dependencies. Report vulnerabilities privately through the contacts below. This is not a labor-compliance, wage, employment-status or legal-qualification determination tool.

## License and contact

The existing [ZhuaTech Non-Commercial Source License1.0](LICENSE) permits personal learning, research and non-commercial exchange while preserving notices. Written authorization from Shanghai Rujing Zhihua Information Technology Co., Ltd. is required for paid deployment, enterprise deployment, commercial delivery, SaaS, resale, commercial customization and paid services. This is publicly available non-commercial source, not an OSI open-source license or MIT/Apache free-commercial grant for the project's own code. Vue/Lucide third-party licenses remain in [docs/licenses](docs/licenses) and distributed static assets. Software is supplied as-is; users remain responsible for applicable rules, backups, capacity and security.

**ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)**. Commercial authorization, customization, deployment and system integration:

- Website: [https://www.zhuatech.cn/](https://www.zhuatech.cn/)
- Email: [han@zhuatech.cn](mailto:han@zhuatech.cn)
- Email: [jack@zhuatech.cn](mailto:jack@zhuatech.cn)
- WhatsApp: [+86 17521234993](https://wa.me/8617521234993)
