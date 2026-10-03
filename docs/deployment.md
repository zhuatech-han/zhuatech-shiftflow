知华科技（上海如静知华信息科技有限公司） · 官网 https://www.zhuatech.cn/ · 商业授权及定制开发微信 zhuatech / zhuatech2。

# 部署与恢复

## 环境

Docker Engine、Compose v2、BuildKit、Python3；源码编译需要Java21/Maven3.9及Node24.19.0+。运行 python3 scripts/init-env.py 创建只供本项目使用的本地 .env（权限0600），或从 .env.example自行填写强密码。配置不得提交。MySQL8.4和Nginx1.29使用官方镜像，应用以非root账号运行。

```sh
docker compose -p shiftflow-local config --quiet
docker compose -p shiftflow-local up -d --build --wait
```

默认前端 http://127.0.0.1:8104，代理健康 http://127.0.0.1:8104/actuator/health。服务间使用mysql/backend容器名；MySQL和后端不向宿主机发布端口。更改 WEB_PORT 解决端口占用。构建后端执行 spotless:check clean package 全部测试，BuildKit锁定缓存/预取/重试；前端执行格式、lint、测试和build。

## 有序重启与验证

```sh
docker compose -p shiftflow-local restart mysql
docker compose -p shiftflow-local up -d --wait mysql
docker compose -p shiftflow-local restart backend frontend
docker compose -p shiftflow-local up -d --wait
```

先等待数据库健康再重启应用，减少连接失败。应用重启会丢失内存会话，需要重新登录；数据库卷保留业务、知悉及事件。查看 docker compose -p shiftflow-local ps 和当前项目日志排查，日志不能公开包含凭证或业务正文。

## HTTPS 与数据

企业网络使用前部署HTTPS反向代理，COOKIE_SECURE=true，合理绑定BIND_ADDRESS、防火墙及仅授权人员访问。不要为调试开放数据库公网端口。备份当前项目数据库、核验恢复；禁止对非测试卷使用 down -v。升级在备份副本运行新增迁移后确认健康、登录和流程再切换。

隔离验收使用 shiftflow-check 等独立项目名与新卷，验收完成仅清理该项目测试容器、网络、卷及忽略的测试配置；不清理其他项目。商业部署需取得知华书面授权。
