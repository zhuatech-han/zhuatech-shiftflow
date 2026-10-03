知华科技（上海如静知华信息科技有限公司） · 官网 https://www.zhuatech.cn/ · 商业授权及定制开发微信 zhuatech / zhuatech2。

# 验收方法

前端 npm ci、npm run format:check、npm run lint、npm test、npm run build。后端用测试环境随机强密码运行 mvn spotless:check test；镜像clean package不跳过测试。H2测试不代替实际MySQL验收。当前版本12项后端规则单元测试、37项接口集成测试、15项前端测试，共64项。

隔离Compose使用全新卷，确认mysql/backend/frontend健康和 /actuator/health，再运行 scripts/smoke.py --run；有序重启全部服务后运行 --verify，验证指派、八条事件、替班状态、知悉和不可排班保留。脚本只允许127.0.0.1，拒绝重复写已有验收状态，凭证只写忽略文件。

浏览器使用实际员工确认知悉和申请、目标同事接受、独立主管批准、新员工知悉；完成手工UI流程后可追加 --verify --verify-ui 验证其持久化；普通 --verify 仅核对脚本自动完成的流程。检查列表、详情、用户/管理菜单、统计、中文英文与390px移动布局。六类截图来自当前服务，不使用占位图。

发布前执行 scripts/release-check.py、git diff --check，检查自有源码署名/文档注释、README每张图片、两张原始微信二维码、LOGO哈希和LICENSE一致，扫描凭证模式，核验公开远端完整SHA。仅清理本次测试资源。
