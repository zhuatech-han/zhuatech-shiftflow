#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""Disposable MySQL acceptance run. Never run against production. Contact: www.zhuatech.cn."""
from pathlib import Path
import argparse, concurrent.futures, datetime as dt, http.cookiejar, json, os, secrets, threading, urllib.request, urllib.error, uuid
ROOT=Path(__file__).resolve().parents[1]
STATE=ROOT/'.smoke-state.json'
class Client:
    """Same-origin cookie + CSRF client; credentials never printed. 微信 zhuatech / zhuatech2。"""
    def __init__(self, base):
        self.base=base; self.jar=http.cookiejar.CookieJar(); self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar)); self.csrf=None
    def call(self,path,method='GET',body=None,expected=200):
        if self.csrf is None and path!='/auth/csrf': self.csrf=self.call('/auth/csrf')
        headers={'Content-Type':'application/json'}
        if method!='GET': headers[self.csrf['header']]=self.csrf['token']
        req=urllib.request.Request(self.base+'/api'+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
        try:
            with self.opener.open(req,timeout=30) as res: status=res.status; value=json.load(res)
        except urllib.error.HTTPError as e: status=e.code; value=json.load(e)
        assert status==expected, f'{method} {path}: expected {expected}, got {status}, code={value.get("code") if isinstance(value,dict) else ""}'
        return value
    def login(self,name,password): return self.call('/auth/login','POST',{'username':name,'password':password})
def stamp(value): return value.isoformat().replace('+00:00','Z')
def key(): return str(uuid.uuid4())
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--base',default='http://127.0.0.1:8104');p.add_argument('--run',action='store_true');p.add_argument('--verify',action='store_true');p.add_argument('--verify-ui',action='store_true',help='Also verify the manually completed UI fixture');args=p.parse_args()
    assert args.run != args.verify,'Choose --run or --verify'
    from urllib.parse import urlparse
    parsed=urlparse(args.base);assert parsed.scheme=='http' and parsed.hostname=='127.0.0.1' and parsed.port,'Isolated loopback only'
    env=dict(line.split('=',1) for line in (ROOT/'.env').read_text().splitlines() if '=' in line and not line.startswith('#'))
    admin=Client(args.base);admin.login('admin',env['ADMIN_PASSWORD'])
    if args.verify:
        state=json.loads(STATE.read_text());d=admin.call('/shifts/'+str(state['approved']));assert d['shift']['employeeId']==state['accounts']['target'] and d['shift']['acknowledgedAt'] is not None and len(d['events'])==8
        assert admin.call('/covers/'+str(state['cover']))['coverage']['status']=='APPROVED'
        if args.verify_ui:
            ui=admin.call('/shifts/'+str(state['ui']));assert ui['shift']['employeeId']==state['accounts']['target'] and ui['shift']['acknowledgedAt'] is not None and len(ui['events'])==8
        assert any(u['id']==state['absence'] and u['status']=='ACTIVE' for u in admin.call('/absences')['items'])
        print('PASS: restart retained assignment, acknowledgement, approved coverage, eight events and unavailability');return
    assert not STATE.exists(),'Use fresh database'
    roles={v['name']:v['id'] for v in admin.call('/admin/roles')};dep=admin.call('/admin/departments','POST',{'name':'排班验收测试部门'})['id'];pw='Aa9'+secrets.token_urlsafe(24);accounts={};clients={}
    for name,role,dept,label in [('employee','员工',dep,'原排班员工'),('target','员工',dep,'替班员工'),('manager','主管',dep,'排班主管'),('manager2','主管',dep,'第二主管'),('outsider','员工',1,'其他部门员工'),('other-manager','主管',1,'其他部门主管')]:
        accounts[name]=admin.call('/admin/users','POST',{'username':'shift-test-'+name,'displayName':label+'（验收测试）','password':pw,'roleId':roles[role],'departmentId':dept,'enabled':True})['id'];clients[name]=Client(args.base);clients[name].login('shift-test-'+name,pw)
    employee=clients['employee'];target=clients['target'];manager=clients['manager'];manager2=clients['manager2'];outsider=clients['outsider'];other=clients['other-manager']
    post=manager.call('/posts','POST',{'code':'TEST-POST','name':'服务岗位（验收测试）','departmentId':dep,'enabled':True})
    now=dt.datetime.fromisoformat(manager.call('/options')['serverNow'].replace('Z','+00:00'));start=(now+dt.timedelta(days=2)).replace(hour=1,minute=0,second=0,microsecond=0);end=start+dt.timedelta(hours=8)
    qualifications={}
    for name in ['employee','target']:
        qualifications[name]=manager.call('/qualifications','POST',{'accountId':accounts[name],'postId':post['id'],'validFrom':now.date().isoformat(),'validUntil':(now+dt.timedelta(days=100)).date().isoformat(),'enabled':True})
    def draft(title,person='employee',day=0):return manager.call('/shifts','POST',{'title':title+'（验收测试）','employeeId':accounts[person],'postId':post['id'],'category':'REGULAR','note':'隔离验收测试班次','startsAt':stamp(start+dt.timedelta(days=day)),'endsAt':stamp(end+dt.timedelta(days=day)),'requestKey':key()})
    def payload(v):return {'version':v['version'],'requestKey':key(),'note':'验收测试处理','employeeId':accounts['target']}
    def act(who,v,a,expected=200,c=None):return who.call(f'/shifts/{v["id"]}/commands/{a}','POST',c or payload(v),expected)
    def request(v):return employee.call('/covers','POST',{'shiftId':v['id'],'shiftVersion':v['version'],'targetId':accounts['target'],'note':'指定同事替班验收测试','requestKey':key()})
    def cover(who,v,a,expected=200):return who.call(f'/covers/{v["id"]}/commands/{a}','POST',payload(v),expected)
    v=draft('替班全流程');employee.call('/shifts/'+str(v['id']),expected=403);act(employee,v,'publish',403)
    c=payload(v);v=act(manager,v,'publish',c=c);assert act(manager,v,'publish',c=c)['version']==v['version'];act(manager,v,'publish',409,{**c,'note':'不同内容'})
    for who in [outsider,other]:
        who.call('/shifts/'+str(v['id']),expected=403);who.call('/shifts/'+str(v['id'])+'/report.json',expected=403);who.call('/admin/users',expected=403)
        assert all(x['id']!=v['id'] for x in who.call('/shifts')['items'])
    act(manager,v,'acknowledge',403);v=act(employee,v,'acknowledge');c=request(v);cover(employee,c,'accept',403);cover(manager,c,'approve',409);c=cover(target,c,'accept');assert manager.call('/shifts/'+str(v['id']))['shift']['employeeId']==accounts['employee']
    c=cover(manager,c,'approve');v=manager.call('/shifts/'+str(v['id']))['shift'];assert v['employeeId']==accounts['target'] and v['acknowledgedAt'] is None;v=act(target,v,'acknowledge');assert len(manager.call('/shifts/'+str(v['id']))['events'])==8
    raw=json.dumps(target.call('/shifts/'+str(v['id'])+'/report.json')).lower();assert 'password' not in raw and 'zhuatech' not in raw
    manager.call('/qualifications/'+str(qualifications['target']['id']),'PUT',{**qualifications['target'],'enabled':False},409)
    admin.call('/admin/users/'+str(accounts['target']),'PUT',{'username':'shift-test-target','displayName':'替班员工（验收测试）','departmentId':dep,'roleId':roles['员工'],'enabled':False},409)
    manager.call('/posts/'+str(post['id']),'PUT',{**post,'enabled':False},409)
    target.call('/absences','POST',{'startsAt':stamp(start),'endsAt':stamp(end),'note':'冲突验收','requestKey':key()},409)
    # Actual MySQL concurrent publication: shared global lock allows only one overlapping plan.
    a=draft('并发班次甲',day=2);b=draft('并发班次乙',day=2);gate=threading.Barrier(2)
    def compete(who,plan):
        gate.wait()
        try:return act(who,plan,'publish')
        except AssertionError as e:assert 'got 409' in str(e);return None
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        results=[f.result() for f in [pool.submit(compete,manager,a),pool.submit(compete,manager2,b)]]
    assert sum(x is not None for x in results)==1
    unavailable=employee.call('/absences','POST',{'startsAt':stamp(start+dt.timedelta(days=4)),'endsAt':stamp(end+dt.timedelta(days=4)),'note':'不可排班（验收测试）','requestKey':key()});assert target.call('/absences')['total']==0
    blocked=draft('不可排班冲突',day=4);act(manager,blocked,'publish',409);target.call('/absences/'+str(unavailable['id'])+'/cancel','POST',payload(unavailable),403)
    # UI-only acceptance fixture, separately acknowledged/requested/accepted/approved via actual pages.
    ui=draft('页面替班验收',day=6);ui=act(manager,ui,'publish')
    draft('计划草稿',day=8)
    state={'approved':v['id'],'cover':c['id'],'absence':unavailable['id'],'ui':ui['id'],'department':dep,'post':post['id'],'accounts':accounts,'testPassword':pw}
    fd=os.open(STATE,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(fd,'w') as f:json.dump(state,f)
    print('PASS: MySQL full coverage, versions, replay, privacy, permissions, qualification/account/post guards, unavailability and concurrent publication; isolated UI fixture ready')
if __name__=='__main__':main()
