// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** 排班与替班接口，权限在服务端检查。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api")
public class ApiController {
  final RosterService service;
  final AdminService admin;

  public ApiController(RosterService service, AdminService admin) {
    this.service = service;
    this.admin = admin;
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/options")
  public Object options() {
    return service.options();
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/workbench")
  public Object workbench() {
    return service.workbench();
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/dashboard")
  public Object dashboard() {
    return service.dashboard();
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/audit")
  public Object audit() {
    return service.audit();
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/posts")
  public Object posts() {
    return service.posts();
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/qualifications")
  public Object qualifications() {
    return service.qualifications();
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/shifts")
  public Object shifts(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "time") String sort,
      @RequestParam(defaultValue = "false") boolean mine) {
    return service.shifts(search, status, page, size, sort, mine);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/covers")
  public Object covers(
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.covers(status, page, size);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/absences")
  public Object absences(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.absences(page, size);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/shifts/{id}")
  public Object detail(@PathVariable Long id) {
    return service.detail(id);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/covers/{id}")
  public Object coverDetail(@PathVariable Long id) {
    return service.coverDetail(id);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/shifts/{id}/candidates")
  public Object candidates(@PathVariable Long id) {
    return service.candidates(id);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/shifts")
  public Object createshifts(@RequestBody RosterService.ShiftInput v) {
    return service.create(v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/shifts/{id}")
  public Object saveshifts(@PathVariable Long id, @RequestBody RosterService.ShiftInput v) {
    return service.save(id, v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/shifts/{id}")
  public Object deleteshifts(@PathVariable Long id, @RequestParam Long version) {
    service.deleteShift(id, version);
    return Map.of("ok", true);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/posts")
  public Object createposts(@RequestBody RosterService.PostInput v) {
    return service.post(null, v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/posts/{id}")
  public Object saveposts(@PathVariable Long id, @RequestBody RosterService.PostInput v) {
    return service.post(id, v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/posts/{id}")
  public Object deleteposts(@PathVariable Long id, @RequestParam Long version) {
    service.deletePost(id, version);
    return Map.of("ok", true);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/qualifications")
  public Object createqualifications(@RequestBody RosterService.QualificationInput v) {
    return service.qualification(null, v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/qualifications/{id}")
  public Object savequalifications(
      @PathVariable Long id, @RequestBody RosterService.QualificationInput v) {
    return service.qualification(id, v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/qualifications/{id}")
  public Object deletequalifications(@PathVariable Long id, @RequestParam Long version) {
    service.deleteQualification(id, version);
    return Map.of("ok", true);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/shifts/{id}/commands/{action}")
  public Object actshifts(
      @PathVariable Long id, @PathVariable String action, @RequestBody RosterService.Command v) {
    return service.command(id, action, v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/covers/{id}/commands/{action}")
  public Object actcovers(
      @PathVariable Long id, @PathVariable String action, @RequestBody RosterService.Command v) {
    return service.coverCommand(id, action, v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/covers")
  public Object request(@RequestBody RosterService.CoverInput v) {
    return service.request(v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/absences")
  public Object absence(@RequestBody RosterService.AbsenceInput v) {
    return service.absence(v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/absences/{id}/cancel")
  public Object cancelAbsence(@PathVariable Long id, @RequestBody RosterService.Command v) {
    return service.cancelAbsence(id, v);
  }

  /** 处理授权业务请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/shifts/{id}/report.json")
  public ResponseEntity<String> export(@PathVariable Long id) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shift-" + id + ".json")
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(service.export(id));
  }

  /** 查询系统资源。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/{type}")
  public Object listAdmin(@PathVariable String type) {
    return admin.list(type);
  }

  /** 创建系统资源。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/admin/{type}")
  public Object createAdmin(@PathVariable String type, @RequestBody AdminService.Input v) {
    return admin.save(type, null, v);
  }

  /** 修改系统资源。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/admin/{type}/{id}")
  public Object updateAdmin(
      @PathVariable String type, @PathVariable Long id, @RequestBody AdminService.Input v) {
    return admin.save(type, id, v);
  }

  /** 删除无引用的系统资源。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/admin/{type}/{id}")
  public Object deleteAdmin(@PathVariable String type, @PathVariable Long id) {
    admin.delete(type, id);
    return Map.of("ok", true);
  }
}
