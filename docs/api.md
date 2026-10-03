知华科技（上海如静知华信息科技有限公司） · 官网 https://www.zhuatech.cn/ · 商业授权及定制开发微信 zhuatech / zhuatech2。

# API 与操作约定

同源 /api，GET /auth/csrf 取得令牌及头名称，POST /auth/login 使用username/password，后续写请求携带令牌；/auth/me、/auth/logout、/auth/password维护会话。权限不取自客户端角色，响应不返回密码哈希。错误返回HTTP状态与code，400输入、401会话、403权限、404记录、409版本/状态/引用/冲突。

| 接口 | 业务 |
|---|---|
| GET /options /workbench /dashboard /audit | 授权目录、本人待办、统计和审计 |
| GET /shifts | search/status/page/size/sort/mine；sort=time/newest/title，size1–100 |
| POST /shifts；PUT/GET/DELETE /shifts/{id} | 草稿；删除需version查询参数 |
| POST /shifts/{id}/commands/{action} | publish/acknowledge/cancel/reassign |
| GET /shifts/{id}/candidates | 当前班次可替班同事 |
| GET /shifts/{id}/report.json | 有export权限的JSON快照 |
| POST/GET /covers；GET /covers/{id} | 指定替班申请、分页与详情 |
| POST /covers/{id}/commands/{action} | accept/approve/reject/withdraw |
| GET/POST /absences；POST /absences/{id}/cancel | 不可排班 |
| GET/POST /posts /qualifications | 岗位与资格 |
| PUT/DELETE /posts/{id} /qualifications/{id} | 更新/无业务引用删除，需version |
| GET/POST /admin/{type}；PUT/DELETE /admin/{type}/{id} | users/roles/departments/menus/permissions/dictionaries/settings |

班次输入为postId、employeeId、title、category、note、startsAt、endsAt、requestKey，更新另加version。UTC带时区ISO时间，资格使用YYYY-MM-DD。班次命令输入version、requestKey、note，改派加employeeId。替班创建shiftId、shiftVersion、targetId、note、requestKey；替班命令version/requestKey/note。不可排班输入startsAt/endsAt/note/requestKey。

业务请求键8–80位字母数字下划线连字符，建议UUID；精确载荷重试会返回当前对象状态，载荷改变则409，不代表撤销原动作。权限仍重新检查。备注发布/知悉/接受可空，取消/改派/替班申请/批准/拒绝/撤回/不可排班要求说明。菜单与权限代码仅预注册，不允许随意创造无对应后端逻辑的代码。
