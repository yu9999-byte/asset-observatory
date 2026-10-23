let csrfToken:string|null=null;
let authCredentials:string|null=null;
export function setAuthCredentials(username:string,password:string|null){
 csrfToken=null;
 if(password===null){authCredentials=null;csrfToken=null;return;}
 const bytes=new TextEncoder().encode(`${username}:${password}`);
 authCredentials=`Basic ${btoa(Array.from(bytes,b=>String.fromCharCode(b)).join(''))}`;
}
export async function api<T>(url:string,method='GET',body?:unknown):Promise<T>{
 const headers:Record<string,string>={Accept:'application/json'};
 if(authCredentials)headers.Authorization=authCredentials;
 if(method!=='GET'){
  if(!csrfToken){const csrf=await fetch('/api/csrf',{credentials:'same-origin',signal:AbortSignal.timeout(10000)});if(!csrf.ok)throw new Error('无法获取安全令牌');csrfToken=(await csrf.json()).token;}
  headers['X-CSRF-TOKEN']=csrfToken!;
  if(body!==undefined)headers['Content-Type']='application/json';
 }
 const response=await fetch(url,{method,headers,body:body===undefined?undefined:JSON.stringify(body),credentials:'same-origin',signal:AbortSignal.timeout(22000)});
 if(!response.ok){if(response.status===403)csrfToken=null;const error=await response.json().catch(()=>null);throw Object.assign(new Error(error?.message||`请求失败 (${response.status})`),{status:response.status});}
 if(response.status===204)return undefined as T;
 return response.json();
}
export function number(value:string|null|undefined,max=6){
 if(value===null||value===undefined)return '—';
 const [whole,fraction='']=value.split('.');
 const parts=whole.replace(/\B(?=(\d{3})+(?!\d))/g,',');
 if(fraction&&max>0){const trimmed=fraction.slice(0,max).replace(/0+$/,'');return trimmed?`${parts}.${trimmed}`:parts+(fraction.match(/[1-9]/)?` (< ${'0.'+'0'.repeat(max-1)+'1'})`:'');}
 return parts;
}
export function time(value:string|null|undefined){return value?new Intl.DateTimeFormat('zh-CN',{timeZone:'Asia/Shanghai',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false}).format(new Date(value)):'源未提供';}
export function short(value:string){return value.length>20?`${value.slice(0,8)}…${value.slice(-6)}`:value;}
