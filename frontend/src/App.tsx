import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import {
  Activity, AlertCircle, ArrowDownRight, ArrowUpRight, Bell, BriefcaseBusiness,
  Building2, CalendarDays, Check, ChevronDown, Clock3, Code2, ExternalLink,
  Filter, Gauge, LayoutDashboard, LoaderCircle, Menu, Plus, RefreshCw, Search,
  Send, Settings2, Sparkles, Target, Trash2, TrendingUp, X, Zap,
} from 'lucide-react'

type Status = 'WISHLIST'|'APPLIED'|'SCREENING'|'INTERVIEWING'|'OFFER'|'ACCEPTED'|'REJECTED'|'WITHDRAWN'
type WorkMode = 'REMOTE'|'HYBRID'|'ONSITE'
type Level = 'BEGINNER'|'INTERMEDIATE'|'ADVANCED'
type Job = { id:number; title:string; company:string; location?:string; workMode:WorkMode; salaryMin?:number; salaryMax?:number; deadline?:string; technologies:string[]; applicationStatus?:Status|null }
type Skill = { id:number; name:string; level:Level }
type Application = { id:number; jobId:number; jobTitle:string; company:string; status:Status; appliedAt?:string; followUpDate?:string; allowedTransitions:Status[]; stageCount:number; interviewCount:number; contactCount:number }
type Stats = { totalJobs:number; totalApplications:number; submittedApplications:number; activeApplications:number; respondedApplications:number; interviewedApplications:number; offers:number; responseRate:number; interviewRate:number; offerRate:number; upcomingInterviews:number; byStatus:Record<string,number>; jobsByWorkMode:Record<string,number>; funnel:{label:string;count:number}[]; topTechnologies:{name:string;count:number}[] }
type Alert = { type:string; severity:'CRITICAL'|'WARNING'|'INFO'; message:string; date?:string; jobId?:number; applicationId?:number; daysLeft:number }
type Ranking = { jobId:number; title:string; company:string; workMode:WorkMode; score:number; verdict:string; missingMandatory:string[]; applicationStatus?:Status|null }

const statusLabel:Record<Status,string> = { WISHLIST:'Salva', APPLIED:'Enviada', SCREENING:'Triagem', INTERVIEWING:'Entrevista', OFFER:'Proposta', ACCEPTED:'Aprovada', REJECTED:'Recusada', WITHDRAWN:'Retirada' }
const modeLabel:Record<WorkMode,string> = { REMOTE:'Remoto', HYBRID:'Híbrido', ONSITE:'Presencial' }
const levelLabel:Record<Level,string> = { BEGINNER:'Iniciante', INTERMEDIATE:'Intermediário', ADVANCED:'Avançado' }
const money = (n?:number) => n == null ? 'A combinar' : n.toLocaleString('pt-BR',{style:'currency',currency:'BRL',maximumFractionDigits:0})
const date = (s?:string) => s ? new Date(s+'T12:00:00').toLocaleDateString('pt-BR') : '—'

async function api<T>(path:string, init?:RequestInit):Promise<T> {
  const response = await fetch(path, { ...init, headers:{'Content-Type':'application/json', ...init?.headers} })
  if (!response.ok) {
    let message = `Erro ${response.status} ao consultar a API`
    try { const body = await response.json(); message = body.detail || body.message || body.title || message } catch { /* resposta sem JSON */ }
    throw new Error(message)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

function App() {
  const [section,setSection] = useState('overview')
  const [stats,setStats] = useState<Stats|null>(null)
  const [jobs,setJobs] = useState<Job[]>([])
  const [skills,setSkills] = useState<Skill[]>([])
  const [applications,setApplications] = useState<Application[]>([])
  const [alerts,setAlerts] = useState<Alert[]>([])
  const [ranking,setRanking] = useState<Ranking[]>([])
  const [loading,setLoading] = useState(true)
  const [error,setError] = useState('')
  const [query,setQuery] = useState('')
  const [mode,setMode] = useState('')
  const [modal,setModal] = useState<'job'|'skill'|null>(null)
  const [mobileNav,setMobileNav] = useState(false)
  const [saving,setSaving] = useState(false)
  const [toast,setToast] = useState('')

  const refresh = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const [s,j,sk,a,al,r] = await Promise.all([
        api<Stats>('/api/stats'),
        api<{content:Job[]}>('/api/jobs?size=100&sortBy=createdAt&direction=desc'),
        api<Skill[]>('/api/skills'),
        api<Application[]>('/api/applications'),
        api<Alert[]>('/api/alerts?days=7'),
        api<Ranking[]>('/api/gap-analysis/ranking'),
      ])
      setStats(s); setJobs(j.content || []); setSkills(sk); setApplications(a); setAlerts(al); setRanking(r)
    } catch(e) { setError(e instanceof Error ? e.message : 'Não foi possível conectar à API.') }
    finally { setLoading(false) }
  },[])
  useEffect(()=>{ void refresh() },[refresh])
  useEffect(()=>{ if(toast){const t=setTimeout(()=>setToast(''),3200);return()=>clearTimeout(t)} },[toast])

  const filteredJobs = useMemo(()=>jobs.filter(j=>{
    const text = `${j.title} ${j.company} ${j.location||''} ${(j.technologies||[]).join(' ')}`.toLowerCase()
    return text.includes(query.toLowerCase()) && (!mode || j.workMode===mode)
  }),[jobs,query,mode])
  const openModal = (kind:'job'|'skill') => setModal(kind)
  const changeStatus = async (app:Application,status:Status) => {
    try { await api(`/api/applications/${app.id}/status`,{method:'POST',body:JSON.stringify({status,note:'Atualizado pelo painel VagaRadar'})}); setToast('Etapa da candidatura atualizada.'); await refresh() }
    catch(e){setToast(e instanceof Error?e.message:'Falha ao atualizar candidatura.')}
  }
  const deleteSkill = async(id:number) => {
    if(!window.confirm('Remover esta competência do seu perfil?')) return
    try { await api(`/api/skills/${id}`,{method:'DELETE'}); setToast('Competência removida.'); await refresh() }
    catch(e){setToast(e instanceof Error?e.message:'Falha ao remover competência.')}
  }
  const createSkill = async(data:{name:string;level:Level}) => {
    setSaving(true)
    try { await api('/api/skills',{method:'POST',body:JSON.stringify(data)}); setModal(null); setToast('Competência adicionada ao perfil.'); await refresh() }
    catch(e){setToast(e instanceof Error?e.message:'Falha ao salvar competência.')}
    finally{setSaving(false)}
  }
  const createApplication = async(jobId:number) => {
    try { await api('/api/applications',{method:'POST',body:JSON.stringify({jobId,status:'APPLIED',appliedAt:new Date().toISOString().slice(0,10)})}); setToast('Candidatura iniciada.'); await refresh() }
    catch(e){setToast(e instanceof Error?e.message:'Falha ao iniciar candidatura.')}
  }
  const createJob = async(data:Record<string,unknown>) => {
    setSaving(true)
    try { await api('/api/jobs',{method:'POST',body:JSON.stringify(data)}); setModal(null); setToast('Oportunidade cadastrada.'); await refresh() }
    catch(e){setToast(e instanceof Error?e.message:'Falha ao cadastrar vaga.')}
    finally{setSaving(false)}
  }

  const nav = [
    {id:'overview',label:'Visão geral',icon:LayoutDashboard},
    {id:'jobs',label:'Oportunidades',icon:BriefcaseBusiness,count:jobs.length},
    {id:'applications',label:'Candidaturas',icon:Send,count:applications.length},
    {id:'skills',label:'Minhas competências',icon:Code2,count:skills.length},
    {id:'insights',label:'Análise de aderência',icon:Target},
  ]
  const pageTitle = nav.find(n=>n.id===section)?.label || 'Visão geral'

  return <div className="app-shell">
    <aside className={`sidebar ${mobileNav?'sidebar-open':''}`}>
      <div className="brand"><div className="brand-mark"><Activity size={22}/></div><div><strong>vaga<span>radar</span></strong><small>CAREER INTELLIGENCE</small></div><button className="mobile-close" onClick={()=>setMobileNav(false)}><X size={18}/></button></div>
      <div className="workspace"><div className="workspace-avatar">HW</div><div><b>Meu espaço</b><small>Plano individual</small></div><ChevronDown size={15}/></div>
      <div className="nav-label">WORKSPACE</div>
      <nav>{nav.map(item=><button key={item.id} className={section===item.id?'nav-item active':'nav-item'} onClick={()=>{setSection(item.id);setMobileNav(false)}}><item.icon size={18}/><span>{item.label}</span>{item.count!==undefined&&<em>{item.count}</em>}</button>)}</nav>
      <div className="sidebar-bottom"><div className="tip-card"><div className="tip-icon"><Sparkles size={16}/></div><b>Seu próximo passo</b><p>Conheça suas lacunas e priorize o que estudar para chegar mais perto da vaga ideal.</p><button onClick={()=>setSection('insights')}>Ver análise <ArrowUpRight size={14}/></button></div><div className="profile"><div className="profile-avatar">H</div><div><b>HernandesWolf</b><small>Perfil profissional</small></div><Settings2 size={17}/></div></div>
    </aside>
    {mobileNav&&<button className="nav-backdrop" onClick={()=>setMobileNav(false)} aria-label="Fechar menu"/>}
    <main className="main-area">
      <header className="topbar"><div className="top-left"><button className="mobile-menu" onClick={()=>setMobileNav(true)}><Menu/></button><span className="breadcrumb">Workspace</span><span className="crumb-sep">/</span><b>{pageTitle}</b></div><div className="top-actions"><span className="system-status"><i/> API conectada quando disponível</span><button className="icon-button" onClick={()=>void refresh()} title="Atualizar dados"><RefreshCw size={17}/></button><button className="icon-button notification" onClick={()=>setSection('overview')} title="Alertas"><Bell size={17}/>{alerts.length>0&&<i/>}</button><div className="top-avatar">H</div></div></header>
      <div className="content">
        {error&&<div className="error-banner"><AlertCircle size={18}/><div><b>Não foi possível carregar os dados</b><p>{error}. Confirme se o backend está ativo em localhost:8080.</p></div><button onClick={()=>void refresh()}>Tentar novamente</button></div>}
        {loading&&<div className="loading-line"><LoaderCircle className="spin" size={18}/> Sincronizando com a API...</div>}
        {section==='overview'&&<Overview stats={stats} alerts={alerts} ranking={ranking} jobs={jobs} applications={applications} onNavigate={setSection}/>}
        {section==='jobs'&&<Jobs jobs={filteredJobs} query={query} setQuery={setQuery} mode={mode} setMode={setMode} onAdd={()=>openModal('job')} onRefresh={()=>void refresh()} onApply={createApplication}/>}
        {section==='applications'&&<Applications applications={applications} onChange={changeStatus}/>}
        {section==='skills'&&<Skills skills={skills} onAdd={()=>openModal('skill')} onDelete={deleteSkill}/>}
        {section==='insights'&&<Insights ranking={ranking} skills={skills}/>}
        <footer>VagaRadar <span>·</span> Inteligência para sua próxima oportunidade <span>·</span> <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">Documentação da API <ExternalLink size={12}/></a></footer>
      </div>
    </main>
    {modal&&<Modal title={modal==='job'?'Adicionar oportunidade':'Nova competência'} onClose={()=>setModal(null)}><Form modal={modal} saving={saving} onClose={()=>setModal(null)} onJob={createJob} onSkill={createSkill}/></Modal>}
    {toast&&<div className="toast"><Check size={17}/>{toast}<button onClick={()=>setToast('')}><X size={15}/></button></div>}
  </div>
}

function SectionHeading({eyebrow,title,description,action}:{eyebrow:string;title:string;description:string;action?:React.ReactNode}) {
  return <div className="section-heading"><div><div className="eyebrow">{eyebrow}</div><h1>{title}</h1><p>{description}</p></div>{action}</div>
}
function Overview({stats,alerts,ranking,jobs,applications,onNavigate}:{stats:Stats|null;alerts:Alert[];ranking:Ranking[];jobs:Job[];applications:Application[];onNavigate:(s:string)=>void}) {
  const cards=[
    {label:'Oportunidades mapeadas',value:stats?.totalJobs??jobs.length,icon:BriefcaseBusiness,tone:'blue',hint:'Vagas no seu radar'},
    {label:'Candidaturas ativas',value:stats?.activeApplications??0,icon:Send,tone:'violet',hint:'Processos em andamento'},
    {label:'Taxa de resposta',value:`${Math.round(stats?.responseRate??0)}%`,icon:TrendingUp,tone:'green',hint:'Retorno das empresas'},
    {label:'Entrevistas próximas',value:stats?.upcomingInterviews??0,icon:CalendarDays,tone:'orange',hint:'Agenda de entrevistas'},
  ]
  return <>
    <SectionHeading eyebrow="SEU PAINEL DE CONTROLE" title="Bom ter você de volta." description="Acompanhe suas oportunidades, evolua seu perfil e mantenha o próximo passo sempre à vista." action={<button className="btn-primary" onClick={()=>onNavigate('jobs')}><Plus size={17}/> Nova oportunidade</button>}/>
    <div className="welcome-strip"><div className="welcome-symbol"><Gauge size={25}/></div><div><b>Seu próximo movimento começa com clareza.</b><p>O VagaRadar cruza suas competências com os requisitos das oportunidades para transformar informação em estratégia.</p></div><button onClick={()=>onNavigate('insights')}>Explorar aderência <ArrowUpRight size={15}/></button></div>
    <div className="metric-grid">{cards.map((c,i)=><div className="metric-card" key={c.label}><div className="metric-top"><span>{c.label}</span><div className={`metric-icon ${c.tone}`}><c.icon size={18}/></div></div><strong>{c.value}</strong><small>{c.hint}</small><div className="metric-index">0{i+1}</div></div>)}</div>
    <div className="dashboard-grid">
      <section className="panel funnel-panel"><div className="panel-head"><div><span className="eyebrow">VISÃO DO PROCESSO</span><h2>Funil de candidaturas</h2></div><button className="text-button" onClick={()=>onNavigate('applications')}>Ver candidaturas <ArrowUpRight size={14}/></button></div>
        <div className="funnel-stats">{(stats?.funnel||[{label:'Salvas',count:stats?.totalApplications||0},{label:'Enviadas',count:stats?.submittedApplications||0},{label:'Respostas',count:stats?.respondedApplications||0},{label:'Entrevistas',count:stats?.interviewedApplications||0},{label:'Propostas',count:stats?.offers||0}]).map((step,i)=><div className="funnel-item" key={step.label}><div className="funnel-value">{step.count}</div><div className="funnel-track"><i style={{width:`${Math.max(3,Math.min(100,step.count/Math.max(1,stats?.totalApplications||step.count)*100))}%`}}/></div><span>{step.label}</span>{i<4&&<ArrowDownRight className="funnel-arrow" size={14}/>}</div>)}</div>
        <div className="conversion-row"><div><small>Taxa de entrevistas</small><b>{Math.round(stats?.interviewRate||0)}%</b></div><div><small>Taxa de propostas</small><b>{Math.round(stats?.offerRate||0)}%</b></div><div><small>Enviadas</small><b>{stats?.submittedApplications||0}</b></div></div>
      </section>
      <section className="panel alerts-panel"><div className="panel-head"><div><span className="eyebrow">ATENÇÃO AOS PRAZOS</span><h2>Radar de alertas</h2></div><span className="count-pill">{alerts.length} ativos</span></div>
        {alerts.length===0?<div className="empty-state compact"><div className="empty-icon"><Check size={20}/></div><b>Tudo sob controle</b><p>Nenhum alerta para os próximos dias.</p></div>:<div className="alert-list">{alerts.slice(0,5).map((a,i)=><div className="alert-row" key={i}><div className={`alert-dot ${a.severity.toLowerCase()}`}/><div><b>{a.message}</b><small>{a.type.replaceAll('_',' ')} · {a.date?date(a.date):'Acompanhar'}</small></div><span className={`severity ${a.severity.toLowerCase()}`}>{a.severity==='CRITICAL'?'Urgente':a.severity==='WARNING'?'Atenção':'Info'}</span></div>)}</div>}
      </section>
      <section className="panel ranking-panel"><div className="panel-head"><div><span className="eyebrow">MATCH COM SEU PERFIL</span><h2>Oportunidades mais aderentes</h2></div><button className="text-button" onClick={()=>onNavigate('insights')}>Ranking completo <ArrowUpRight size={14}/></button></div>
        {ranking.length===0?<div className="empty-state"><div className="empty-icon"><Target size={20}/></div><b>Seu ranking está esperando</b><p>Cadastre competências e oportunidades para calcular a aderência.</p></div>:<div className="ranking-list">{ranking.slice(0,4).map((r,i)=><div className="ranking-row" key={r.jobId}><div className="rank-number">{String(i+1).padStart(2,'0')}</div><div className="rank-info"><b>{r.title}</b><small>{r.company} · {modeLabel[r.workMode]}</small></div><div className="score-wrap"><div className="score-track"><i style={{width:`${r.score}%`}}/></div><b>{r.score}%</b></div></div>)}</div>}
      </section>
      <section className="panel tech-panel"><div className="panel-head"><div><span className="eyebrow">MERCADO EM FOCO</span><h2>Competências em alta</h2></div><div className="panel-icon"><Zap size={17}/></div></div>
        {(stats?.topTechnologies||[]).slice(0,5).length===0?<div className="empty-state compact"><p>Cadastre vagas para descobrir as tecnologias mais requisitadas.</p></div>:(stats?.topTechnologies||[]).slice(0,5).map((t,i)=><div className="tech-row" key={t.name}><span className="tech-rank">{String(i+1).padStart(2,'0')}</span><b>{t.name}</b><div className="tech-bar"><i style={{width:`${t.count/Math.max(1,stats?.topTechnologies?.[0]?.count||1)*100}%`}}/></div><small>{t.count} vagas</small></div>)}
      </section>
    </div>
  </>
}
function Jobs({jobs,query,setQuery,mode,setMode,onAdd,onRefresh,onApply}:{jobs:Job[];query:string;setQuery:(s:string)=>void;mode:string;setMode:(s:string)=>void;onAdd:()=>void;onRefresh:()=>void;onApply:(jobId:number)=>void}) {
  return <><SectionHeading eyebrow="GESTÃO DE OPORTUNIDADES" title="Seu radar de vagas." description="Centralize oportunidades, compare requisitos e mantenha cada prazo sob controle." action={<button className="btn-primary" onClick={onAdd}><Plus size={17}/> Adicionar vaga</button>}/>
    <div className="toolbar"><div className="search-box"><Search size={17}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Buscar cargo, empresa ou tecnologia..."/></div><div className="select-wrap"><Filter size={15}/><select value={mode} onChange={e=>setMode(e.target.value)}><option value="">Todas as modalidades</option><option value="REMOTE">Remoto</option><option value="HYBRID">Híbrido</option><option value="ONSITE">Presencial</option></select></div><button className="btn-quiet" onClick={onRefresh}><RefreshCw size={16}/></button></div>
    <div className="panel table-panel"><div className="table-top"><b>{jobs.length} oportunidades</b><span>Dados sincronizados com seu backend</span></div>{jobs.length===0?<div className="empty-state"><div className="empty-icon"><BriefcaseBusiness size={22}/></div><b>Nenhuma oportunidade encontrada</b><p>Adicione sua primeira vaga ou ajuste os filtros.</p><button className="btn-primary" onClick={onAdd}><Plus size={16}/> Cadastrar vaga</button></div>:<div className="table-scroll"><table><thead><tr><th>OPORTUNIDADE</th><th>MODALIDADE</th><th>FAIXA SALARIAL</th><th>PRAZO</th><th>STATUS</th><th>AÇÃO</th></tr></thead><tbody>{jobs.map(j=><tr key={j.id}><td><div className="job-cell"><div className="company-icon"><Building2 size={17}/></div><div><b>{j.title}</b><small>{j.company}{j.location? ` · ${j.location}`:''}</small><div className="tag-list">{(j.technologies||[]).slice(0,3).map(t=><span key={t}>{t}</span>)}</div></div></div></td><td><span className="mode-badge">{modeLabel[j.workMode]}</span></td><td className="salary-cell">{j.salaryMin||j.salaryMax?money(j.salaryMin)+' – '+money(j.salaryMax):'A combinar'}</td><td>{date(j.deadline)}</td><td><span className={`status-badge ${(j.applicationStatus||'WISHLIST').toLowerCase()}`} >{statusLabel[j.applicationStatus||'WISHLIST']}</span></td><td>{j.applicationStatus?<span className="already-tracking">Em acompanhamento</span>:<button className="table-action" onClick={()=>onApply(j.id)}><Send size={13}/> Candidatar-se</button>}</td></tr>)}</tbody></table></div>}</div>
  </>
}
function Applications({applications,onChange}:{applications:Application[];onChange:(a:Application,s:Status)=>void}) {
  return <><SectionHeading eyebrow="ACOMPANHAMENTO" title="Candidaturas em movimento." description="Acompanhe cada etapa do processo seletivo e mantenha seu histórico organizado."/><div className="application-grid">{applications.length===0?<div className="panel empty-state"><div className="empty-icon"><Send size={22}/></div><b>Nenhuma candidatura cadastrada</b><p>Ao cadastrar uma oportunidade, acompanhe sua evolução por aqui.</p></div>:applications.map(a=><div className="panel application-card" key={a.id}><div className="application-card-top"><div className="company-icon"><Building2 size={18}/></div><span className={`status-badge ${a.status.toLowerCase()}`}>{statusLabel[a.status]}</span></div><h3>{a.jobTitle}</h3><p>{a.company}</p><div className="application-meta"><span><CalendarDays size={14}/> Enviada: {date(a.appliedAt)}</span><span><Clock3 size={14}/> Follow-up: {date(a.followUpDate)}</span></div><div className="application-counts"><span>{a.stageCount} etapas</span><span>{a.interviewCount} entrevistas</span><span>{a.contactCount} contatos</span></div><label className="field-label">Atualizar etapa</label><select value={a.status} onChange={e=>onChange(a,e.target.value as Status)} disabled={!a.allowedTransitions?.length}><option value={a.status}>{statusLabel[a.status]}</option>{a.allowedTransitions?.map(s=><option key={s} value={s}>{statusLabel[s]}</option>)}</select></div>)}</div></>
}
function Skills({skills,onAdd,onDelete}:{skills:Skill[];onAdd:()=>void;onDelete:(id:number)=>void}) {
  const counts = {BEGINNER:0,INTERMEDIATE:0,ADVANCED:0}; skills.forEach(s=>counts[s.level]++)
  return <><SectionHeading eyebrow="SEU CAPITAL TÉCNICO" title="Competências do seu perfil." description="Declare seu nível de domínio para que o motor de aderência compare seu perfil com os requisitos das vagas." action={<button className="btn-primary" onClick={onAdd}><Plus size={17}/> Nova competência</button>}/><div className="level-summary">{Object.entries(counts).map(([k,v])=><div className="level-card" key={k}><span>{levelLabel[k as Level]}</span><b>{v}</b><small>competências</small></div>)}</div><div className="panel skills-panel"><div className="panel-head"><div><span className="eyebrow">INVENTÁRIO DE SKILLS</span><h2>Seu repertório técnico</h2></div><span className="count-pill">{skills.length} cadastradas</span></div>{skills.length===0?<div className="empty-state"><div className="empty-icon"><Code2 size={22}/></div><b>Comece pelo que você já sabe</b><p>Adicione linguagens, frameworks, ferramentas e conhecimentos.</p><button className="btn-primary" onClick={onAdd}><Plus size={16}/> Adicionar competência</button></div>:<div className="skills-grid">{skills.map(s=><div className="skill-chip" key={s.id}><div className="skill-symbol"><Code2 size={17}/></div><div><b>{s.name}</b><small>{levelLabel[s.level]}</small></div><span className={`level-dot ${s.level.toLowerCase()}`}/><button title="Remover competência" onClick={()=>onDelete(s.id)}><Trash2 size={15}/></button></div>)}</div>}</div></>
}
function Insights({ranking,skills}:{ranking:Ranking[];skills:Skill[]}) {
  return <><SectionHeading eyebrow="INTELIGÊNCIA DE CARREIRA" title="Aderência com estratégia." description="Veja onde seu perfil já é competitivo e quais requisitos merecem atenção antes de se candidatar."/><div className="insight-callout"><div className="insight-icon"><Sparkles size={21}/></div><div><b>Uma análise objetiva, sem caixa-preta.</b><p>O motor determinístico pondera requisitos obrigatórios e desejáveis, calcula o score e aponta lacunas que você pode trabalhar.</p></div><div className="insight-mini"><strong>{skills.length}</strong><small>skills no perfil</small></div></div><div className="panel ranking-full"><div className="panel-head"><div><span className="eyebrow">RANKING DE OPORTUNIDADES</span><h2>Seu melhor encaixe primeiro</h2></div><span className="count-pill">{ranking.length} analisadas</span></div>{ranking.length===0?<div className="empty-state"><div className="empty-icon"><Target size={22}/></div><b>Dados insuficientes para o ranking</b><p>Cadastre pelo menos uma competência e uma vaga com requisitos.</p></div>:ranking.map((r,i)=><div className="insight-row" key={r.jobId}><div className="rank-number">{String(i+1).padStart(2,'0')}</div><div className="insight-job"><b>{r.title}</b><small>{r.company} · {modeLabel[r.workMode]}</small><div className="gap-tags">{r.missingMandatory.length?r.missingMandatory.map(t=><span key={t}>{t}</span>):<span className="all-good"><Check size={12}/> Sem lacunas obrigatórias</span>}</div></div><div className="insight-score"><strong>{r.score}<small>%</small></strong><div className="score-track"><i style={{width:`${r.score}%`}}/></div><span>{r.verdict}</span></div></div>)}</div></>
}
function Modal({title,onClose,children}:{title:string;onClose:()=>void;children:ReactNode}) {
  return <div className="modal-backdrop" onMouseDown={e=>{if(e.target===e.currentTarget)onClose()}}><div className="modal"><div className="modal-head"><div><span className="eyebrow">VAGARADAR · CADASTRO</span><h2>{title}</h2></div><button className="icon-button" onClick={onClose}><X size={18}/></button></div>{children}</div></div>
}
function Form({modal,saving,onClose,onJob,onSkill}:{modal:'job'|'skill';saving:boolean;onClose:()=>void;onJob:(d:Record<string,unknown>)=>void;onSkill:(d:{name:string;level:Level})=>void}) {
  const [name,setName]=useState('');const [level,setLevel]=useState<Level>('BEGINNER')
  const [title,setTitle]=useState('');const [company,setCompany]=useState('');const [location,setLocation]=useState('');const [workMode,setWorkMode]=useState<WorkMode>('REMOTE');const [salaryMin,setSalaryMin]=useState('');const [salaryMax,setSalaryMax]=useState('');const [deadline,setDeadline]=useState('');const [sourceUrl,setSourceUrl]=useState('');const [description,setDescription]=useState('');const [requirements,setRequirements]=useState('')
  const submit=(e:FormEvent)=>{e.preventDefault();if(modal==='skill'){onSkill({name,level});return}onJob({title,company,location,workMode,salaryMin:salaryMin?Number(salaryMin):null,salaryMax:salaryMax?Number(salaryMax):null,deadline:deadline||null,sourceUrl:sourceUrl||null,description:description||null,requirements:requirements.split(',').map(x=>x.trim()).filter(Boolean).map(name=>({name,mandatory:true,minLevel:'BEGINNER'}))})}
  return <form className="modal-form" onSubmit={submit}>{modal==='skill'?<><div className="form-field"><label>Nome da competência</label><input required maxLength={80} value={name} onChange={e=>setName(e.target.value)} placeholder="Ex.: Java, Spring Boot, SQL"/></div><div className="form-field"><label>Nível de domínio</label><select value={level} onChange={e=>setLevel(e.target.value as Level)}><option value="BEGINNER">Iniciante</option><option value="INTERMEDIATE">Intermediário</option><option value="ADVANCED">Avançado</option></select></div></>:<><div className="form-two"><div className="form-field"><label>Cargo / título</label><input required maxLength={150} value={title} onChange={e=>setTitle(e.target.value)} placeholder="Desenvolvedor Java Jr."/></div><div className="form-field"><label>Empresa</label><input required maxLength={120} value={company} onChange={e=>setCompany(e.target.value)} placeholder="Nome da empresa"/></div></div><div className="form-two"><div className="form-field"><label>Localidade</label><input value={location} onChange={e=>setLocation(e.target.value)} placeholder="Remoto / Natal - RN"/></div><div className="form-field"><label>Modalidade</label><select value={workMode} onChange={e=>setWorkMode(e.target.value as WorkMode)}><option value="REMOTE">Remoto</option><option value="HYBRID">Híbrido</option><option value="ONSITE">Presencial</option></select></div></div><div className="form-two"><div className="form-field"><label>Salário mínimo (R$)</label><input type="number" min="0" value={salaryMin} onChange={e=>setSalaryMin(e.target.value)} placeholder="Ex.: 3500"/></div><div className="form-field"><label>Salário máximo (R$)</label><input type="number" min="0" value={salaryMax} onChange={e=>setSalaryMax(e.target.value)} placeholder="Ex.: 5000"/></div></div><div className="form-two"><div className="form-field"><label>Prazo de inscrição</label><input type="date" value={deadline} onChange={e=>setDeadline(e.target.value)}/></div><div className="form-field"><label>Link da oportunidade</label><input type="url" value={sourceUrl} onChange={e=>setSourceUrl(e.target.value)} placeholder="https://..."/></div></div><div className="form-field"><label>Tecnologias / requisitos</label><input value={requirements} onChange={e=>setRequirements(e.target.value)} placeholder="Java, Spring Boot, PostgreSQL"/><small>Separe os requisitos por vírgula. Eles serão cadastrados como obrigatórios em nível iniciante.</small></div><div className="form-field"><label>Descrição da vaga</label><textarea rows={3} maxLength={5000} value={description} onChange={e=>setDescription(e.target.value)} placeholder="Cole aqui a descrição da oportunidade..."/></div></>}<div className="modal-actions"><button type="button" className="btn-quiet" onClick={onClose}>Cancelar</button><button className="btn-primary" type="submit" disabled={saving}>{saving?<LoaderCircle className="spin" size={16}/>:<Check size={16}/>} Salvar</button></div></form>
}
export default App
