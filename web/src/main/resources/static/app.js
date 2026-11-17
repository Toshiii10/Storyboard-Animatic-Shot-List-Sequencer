'use strict';

const $ = id => document.getElementById(id);
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const paths = {
  film:'<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 10h18M8 4v6m8-6v6"/>',
  plus:'<path d="M12 5v14M5 12h14"/>',close:'<path d="m6 6 12 12M18 6 6 18"/>',
  folder:'<path d="M3 7V5a1 1 0 0 1 1-1h5l2 3h9a1 1 0 0 1 1 1v11H3Z"/>',
  upload:'<path d="M12 16V3m-5 5 5-5 5 5M4 15v5h16v-5"/>',download:'<path d="M12 3v13m-5-5 5 5 5-5M4 17v4h16v-4"/>',
  help:'<circle cx="12" cy="12" r="9"/><path d="M9 9a3 3 0 0 1 6 0c0 2-3 2-3 5m0 3h.01"/>',
  layers:'<path d="m12 3 10 5-10 5L2 8Zm-9 9 9 5 9-5M3 16l9 5 9-5"/>',
  clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v6l4 2"/>',scene:'<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 9h18M7 5l3 4m3-4 3 4"/>',
  edit:'<path d="m15 4 5 5M4 20l5-1L21 7l-5-5L4 14Z"/>',
  grid:'<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
  list:'<path d="M8 5h13M8 12h13M8 19h13M3 5h.01M3 12h.01M3 19h.01"/>',search:'<circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/>',
  play:'<path d="m8 4 12 8-12 8Z"/>',pause:'<path d="M8 4v16M16 4v16"/>',restart:'<path d="M4 10a8 8 0 1 1 1 8M4 4v6h6"/>',
  frame:'<path d="M9 3H3v6m12-6h6v6M3 15v6h6m12-6v6h-6"/><circle cx="12" cy="12" r="3"/>',
  camera:'<path d="m8 6 2-3h4l2 3h5v14H3V6Z"/><circle cx="12" cy="13" r="4"/>',
  move:'<path d="M12 2v20M2 12h20M8 6l4-4 4 4M8 18l4 4 4-4M6 8l-4 4 4 4M18 8l4 4-4 4"/>',
  grip:'<path d="M9 5h.01M15 5h.01M9 12h.01M15 12h.01M9 19h.01M15 19h.01"/>',
  arrowup:'<path d="M12 20V4m-6 6 6-6 6 6"/>',arrowdown:'<path d="M12 4v16m-6-6 6 6 6-6"/>',
  trash:'<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7"/>',
  copy:'<rect x="8" y="8" width="13" height="13" rx="2"/><path d="M16 8V3H3v13h5"/>',spark:'<path d="m12 2 2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5Z"/>'
};
const icon = name => `<svg viewBox="0 0 24 24" aria-hidden="true">${paths[name] || paths.frame}</svg>`;
document.querySelectorAll('[data-icon]').forEach(el => el.innerHTML = icon(el.dataset.icon));
const typeNames = {ESTABLISHING:'Establishing',DIALOGUE:'Dialogue close-up',ACTION:'Action tracking'};
const angles = ['Eye level','High angle','Low angle',"Bird's eye",'Dutch angle','Over the shoulder'];
const movements = ['Static','Pan','Tilt','Dolly','Tracking','Handheld'];
const transitions = ['Cut','Dissolve','Fade to black'];
let project = null, projects = [], selected = null, view = 'grid', revision = 0, savedRevision = 0;
let saveTimer, savePromise = null, blocked = false, dragging = null, editingProject = false;
let noticeTimer;

const total = () => project?.shots.reduce((sum,s) => sum + s.duration,0) || 0;
const time = seconds => `${String(Math.floor(seconds/60)).padStart(2,'0')}:${String(Math.floor(seconds%60)).padStart(2,'0')}`;
const currentShot = () => project?.shots.find(s => s.id === selected);
function notify(text) { $('notice').textContent=text; $('notice').hidden=false; clearTimeout(noticeTimer); noticeTimer=setTimeout(() => $('notice').hidden=true,3500); }
function showError(message) { $('error-message').textContent=message; $('connection-error').hidden=false; }
async function api(path, options={}) {
  const response = await fetch(path, {headers:{'Content-Type':'application/json'},...options});
  if (!response.ok) {
    let message = 'The studio could not connect. Your current edits are still on this page.';
    try { message = (await response.json()).message || message; } catch (_) { /* non-JSON network error */ }
    const error = new Error(message); error.status=response.status; throw error;
  }
  return response.status === 204 ? null : response.json();
}
function setSaveLabel(text,pending=false) { $('save-label').textContent=text; $('save-status').classList.toggle('pending',pending); }
function changed() {
  revision++; setSaveLabel('Unsaved changes',true); clearTimeout(saveTimer);
  if (!blocked) saveTimer=setTimeout(() => saveProject().catch(() => {}),650);
  updateStats(); renderTimeline(); updateLocalSummary();
}
async function saveProject() {
  clearTimeout(saveTimer);
  if (savePromise) { await savePromise; if (revision !== savedRevision) return saveProject(); return; }
  if (!project || revision === savedRevision) return;
  if (blocked) throw new Error('Reload this storyboard before saving. Export a backup to preserve your edits.');
  const snapshot=structuredClone(project), snapshotRevision=revision;
  setSaveLabel('Saving…',true);
  savePromise=(async () => {
    try {
      const result=await api(`/api/projects/${snapshot.id}`,{method:'PUT',body:JSON.stringify(snapshot)});
      project.version=result.version; project.updatedAt=result.updatedAt;
      result.shots.forEach(s => {const local=project.shots.find(x=>x.id===s.id); if(local) {local.recommendedDuration=s.recommendedDuration;local.checklist=s.checklist;}});
      savedRevision=snapshotRevision;
      setSaveLabel(revision===savedRevision ? 'All changes saved' : 'Unsaved changes',revision!==savedRevision);
      $('connection-error').hidden=true;
      const recommendation=$('recommend-time'); if(recommendation && currentShot()) recommendation.textContent=`Use suggested timing · ${currentShot().recommendedDuration}s`;
      updateLocalSummary();
    } catch(error) {
      if(error.status===409) blocked=true;
      setSaveLabel('Changes not saved',true); showError(error.message); throw error;
    } finally {savePromise=null;}
  })();
  await savePromise;
  if(revision!==savedRevision) return saveProject();
}
async function safeSwitch(action) {
  try { await saveProject(); await action(); } catch(error) {showError(error.message);}
}
function updateLocalSummary() {
  if(!project) return;
  const summary={id:project.id,title:project.title,description:project.description,shotCount:project.shots.length,totalDuration:total(),updatedAt:project.updatedAt};
  const index=projects.findIndex(p=>p.id===project.id); if(index<0) projects.unshift(summary); else projects[index]=summary;
  renderProjects();
}
function renderProjects() {
  $('project-list').innerHTML=projects.map(p=>`<button class="project-link ${project?.id===p.id?'active':''}" data-project="${p.id}" ${project?.id===p.id?'aria-current="page"':''} title="${esc(p.title)}">${icon('scene')}<span>${esc(p.title)}</span></button>`).join('');
}
async function openProject(id) {
  const next=await api(`/api/projects/${id}`);
  project=next; selected=next.shots[0]?.id || null; revision=0; savedRevision=0; blocked=false;
  try {localStorage.setItem('boardflow-project',id);} catch(_) {}
  $('connection-error').hidden=true; setSaveLabel('All changes saved'); renderAll();
}
function updateStats() {
  if(!project) return;
  $('project-title').textContent=project.title; $('breadcrumb-title').textContent=project.title;
  $('project-description').textContent=project.description || 'A blank canvas for your next big idea.';
  $('shot-count').textContent=project.shots.length; $('runtime').textContent=time(total());
  $('scene-count').textContent=new Set(project.shots.map(s=>s.scene.trim()).filter(Boolean)).size;
  $('timeline-end').textContent=time(total()); $('play').disabled=!project.shots.length;
  document.title=`${project.title} — BoardFlow`;
}
function renderAll() {renderProjects();updateStats();renderBoard();renderInspector();renderTimeline();}

/* Original SVG storyboard sketches. They are examples; uploaded frames replace them. */
function artwork(key) {
  const base=`<defs><linearGradient id="sky-${key}" x2="0" y2="1"><stop stop-color="#a3a786"/><stop offset="1" stop-color="#ded5ad"/></linearGradient><pattern id="grain-${key}" width="7" height="7" patternUnits="userSpaceOnUse"><circle cx="1" cy="2" r=".5" fill="#e8e1c3" opacity=".12"/><circle cx="5" cy="6" r=".4" fill="#222e24" opacity=".2"/></pattern></defs>`;
  const person=(x,y,scale=1)=>`<g transform="translate(${x} ${y}) scale(${scale})"><ellipse cx="0" cy="0" rx="7" ry="9" fill="#283a32"/><path d="M-8 9Q-17 15-15 39L-10 43-8 70h6L0 45 4 70h6L9 42 15 38Q16 16 8 9Z" fill="#2e4035"/><path d="M-5-6q7-9 12 2" stroke="#202f29" stroke-width="5" fill="none"/></g>`;
  const scenes={
    city:`<rect width="640" height="360" fill="url(#sky-${key})"/><circle cx="454" cy="100" r="49" fill="#e9dab0" opacity=".8"/><g fill="#87927c"><path d="M0 160h55v-30h48v60h50v-85h40v55h35v-40h31v72h48v-32h46v39h50v-51h26v24h65v-56h46v77h50v-31h50v-21h40v180H0Z"/></g><g fill="#4c6152" stroke="#d7d0ae" stroke-width="1"><path d="M0 220h43V168h66v58h19V139h65v81h27v-45h80v70h42v-76h61v64h27v-34h77v-72h69v88h64v145H0Z"/></g><g stroke="#a3b08b" opacity=".65"><path d="M52 184h45m-45 16h45m-45 16h45m43-61h40m-40 17h40m-40 17h40m-40 17h40m51-15h55m-55 18h55m-55 18h55m69-42h40m-40 18h40m-40 18h40m120-67h45m-45 17h45m-45 17h45m-45 17h45"/></g><path d="M0 299 640 257v103H0Z" fill="#263d34"/><path d="m0 297 640-42M0 324l640-47" stroke="#acb493" opacity=".45"/><path d="M49 290v-28h70v24m334-13v-39h49v36" stroke="#71866e" fill="none" stroke-width="3"/>`,
    street:`<rect width="640" height="360" fill="#b6b394"/><path d="M250 0h140v210H250Z" fill="#d9d1ac"/><path d="M0 0h158l121 205L90 360H0Zm640 0H490L367 205l190 155h83Z" fill="#4f6253"/><g stroke="#a3af8c" fill="none"><path d="m17 0 229 204M80 0l172 204M147 0l117 204M0 85l252 127M0 176l244 51M0 263l238-16M623 0 394 204M560 0 388 204M493 0 376 204M640 85 388 212M640 176 396 227M640 263 402 247"/></g><path d="M278 195h90l190 165H89Z" fill="#808d73"/><path d="m306 205-81 155m111-155 81 155" stroke="#d8d2ad" opacity=".7"/><path d="m0 352 280-156m360 156L367 196" stroke="#d9d5b5" stroke-width="3"/><path d="m315 239-8 28m-9 24-14 50" stroke="#d6d1ad" stroke-width="3"/>${person(333,213,1.32)}<path d="m167 259-25 7m33 1-25 7m322-14 28 8m-35 1 24 8" stroke="#344b3c" stroke-width="2"/>`,
    cafe:`<rect width="640" height="360" fill="#8d8368"/><path d="M0 0h300v259H0Z" fill="#d3cc9f"/><path d="M20 0v258m135-258v258M0 120h299M297 0v268" stroke="#596750" stroke-width="10"/><path d="m22 242 132-76 141 40v55H22Z" fill="#adb18c"/><path d="M0 282h640v78H0Z" fill="#3e4d3d"/><path d="m0 283 640-16M25 308l560-21" stroke="#b2b28b" stroke-width="2"/><path d="M377 129q-26 4-31 32-10 43 29 48 49 2 49-41-3-35-47-39Z" fill="#c1b68b"/><path d="M345 165q-11-57 40-58 49 3 44 63l-17-20q-15-4-33-23-2 29-34 38Z" fill="#2e4033"/><path d="M361 200q-66 10-83 80h199q-16-72-72-82l-22 24Z" fill="#46583f"/><path d="m348 277 44-29 57 30" fill="#a79d75"/><path d="M373 170h6m24-1h6m-24 21q8 5 16 0" stroke="#58654a" stroke-width="2"/><ellipse cx="201" cy="281" rx="27" ry="7" fill="#cbc196"/><path d="M180 257h36l-4 26h-29Zm36 3q24-2 14 16h-17" fill="#d3c89d" stroke="#687453" stroke-width="2"/>`,
    station:`<rect width="640" height="360" fill="#aeb393"/><path d="M0 0h640L357 126h-65Z" fill="#3b5144"/><g stroke="#9faf8c" stroke-width="2"><path d="M18 0 299 125M186 0 312 125M374 0 326 125M560 0 343 125M0 34h563M98 74h373M215 105h185"/></g><path d="M0 237 290 149h70l280 88v123H0Z" fill="#818f72"/><path d="M0 310 303 159h42l295 151v50H0Z" fill="#52694f"/><path d="M99 360 313 160m228 200L335 160M212 360l109-200m113 200-106-200" stroke="#d4d3ad" stroke-width="3"/><g stroke="#2a4539" stroke-width="13"><path d="M49 0v278M184 69v156M255 101v95M591 0v278M456 69v156M385 101v95"/></g><path d="M0 238 292 149m348 89L359 149" stroke="#d4d8ad" stroke-width="3"/>${person(153,174,.8)}${person(425,168,.65)}`,
    portrait:`<rect width="640" height="360" fill="#9b9e7c"/><path d="M13 0v360M112 0v360m65-360v360M8 107h198M8 220h198" stroke="#697c5b" stroke-width="8"/><path d="M263 169q-48-111 27-144 109-42 155 57l-17 109Z" fill="#293f32"/><path d="M290 75q-11 83-37 103l22 14q-2 70 61 88 68 13 78-83l3-73q-61 14-100-57Z" fill="#c4bd91"/><path d="M293 274q-96 2-139 86h353q-40-83-103-91l-54 46Z" fill="#3f583e"/><path d="m302 164 17-2m46 0 18-2m-43 4-7 36 14 4m-32 26q24 13 40-3" stroke="#5b7050" stroke-width="3" fill="none"/><path d="m288 144 36-4m34-2 31 2" stroke="#344d37" stroke-width="5"/><path d="M286 86q-27 61-19 101" stroke="#203c2d" stroke-width="13"/><path d="m517 169 112-24m-106 42 105-22m-93 39 95-18" stroke="#bdc39c" opacity=".6" stroke-width="2"/>`,
    train:`<rect width="640" height="360" fill="#a3ae8c"/><path d="m0 82 640 37v163L0 300Z" fill="#637b60"/><path d="m0 211 640 14v58L0 300Z" fill="#334e3c"/><path d="m0 110 640 30M0 242l640 4" stroke="#d0d4a9" stroke-width="3"/><g fill="#b8c19b" stroke="#415f45" stroke-width="3"><path d="m23 126 80 4v63l-80-1ZM128 131l80 5v59l-80-1ZM233 138l80 4v56l-80-1ZM338 144l80 4v53l-80-1ZM443 150l80 4v50l-80-1ZM548 156l72 4v47l-72-1Z"/></g><path d="m0 312 640-16v64H0Z" fill="#71865f"/><path d="m0 329 640-14M0 350l640-21" stroke="#bac49b"/><path d="m3 68 178 7M0 57l109 6m408 26 123 7M3 271l82 1" stroke="#dbe0b6" stroke-width="2"/>${person(259,220,1.7)}${person(357,216,1.8)}`
  };
  if(!scenes[key]) return `<div class="image-empty">${icon('frame')}<span>Your frame goes here</span></div>`;
  return `<svg class="scene-art" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 360" role="img" aria-label="Example storyboard sketch: ${key}">${base}${scenes[key]}<rect width="640" height="360" fill="url(#grain-${key})"/><rect x="12" y="12" width="616" height="336" fill="none" stroke="#e4e1bd" opacity=".2"/></svg>`;
}
function frameContent(s) {return s.image ? `<img src="${esc(s.image)}" alt="${esc(s.title)} storyboard frame">` : artwork(s.illustration);}
function frame(s,overlays=true) {return `<div class="shot-image">${frameContent(s)}${overlays?`<span class="frame-duration">${s.duration}s</span>${!s.image&&s.illustration?'<span class="art-label">Example sketch</span>':''}`:''}</div>`;}
function card(s,index) {
  return `<article class="shot-card ${s.id===selected?'selected':''}" draggable="true" tabindex="0" role="button" aria-pressed="${s.id===selected}" aria-label="Shot ${index+1}: ${esc(s.title)}. ${s.duration} seconds. Select to edit." data-shot="${s.id}">
    <div class="card-top"><span class="shot-code mono">${icon('grip')}SHOT ${String(index+1).padStart(2,'0')}</span><span class="shot-type ${s.type}">${typeNames[s.type]}</span></div>${frame(s)}
    <div class="card-body"><h3>${esc(s.title)}</h3><p>${esc(s.notes||s.dialogue||'Select this shot to add action notes and dialogue.')}</p></div><div class="card-meta"><span>${icon('camera')}${esc(s.angle)}</span><span>${icon('move')}${esc(s.movement)}</span></div></article>`;
}
function renderBoard() {
  if(!project) return;
  const query=$('search').value.trim().toLowerCase();
  const visible=project.shots.map((s,i)=>({s,i})).filter(({s})=>[s.title,s.scene,s.notes,s.dialogue,typeNames[s.type]].some(x=>x.toLowerCase().includes(query)));
  $('board').className=`shot-grid ${view==='list'?'list-mode':''}`;
  let html=visible.map(({s,i})=>card(s,i)).join('');
  if(!project.shots.length) html=`<div class="empty-state">${icon('frame')}<h2>Every story starts somewhere.</h2><p>Add your first shot. A sketch, a camera angle, a few words — that's all it takes to begin.</p><button class="button primary" data-action="add">${icon('plus')}Add your first shot</button></div>`;
  else if(!visible.length) html=`<div class="empty-state">${icon('search')}<h2>No frames found.</h2><p>Try another title, scene, or line of dialogue.</p><button class="button quiet" data-action="clear-search">Clear search</button></div>`;
  else if(!query) html+=`<button class="new-shot-card" data-action="add">${icon('plus')}<span>The next moment</span><small>Add a shot to your story</small></button>`;
  $('board').innerHTML=html;
  $('visible-count').textContent=query?`${visible.length} of ${project.shots.length} shots`: `${project.shots.length} frames · ${time(total())}`;
}
function refreshCard(id) {
  const element=$('board').querySelector(`[data-shot="${id}"]`), index=project.shots.findIndex(s=>s.id===id);
  if(element && index>=0) element.outerHTML=card(project.shots[index],index);
  $('visible-count').textContent=`${project.shots.length} frames · ${time(total())}`;
}
function selectShot(id,scroll=false) {
  selected=id; renderBoard();renderInspector();renderTimeline();
  if(scroll) { const el=$('board').querySelector(`[data-shot="${id}"]`); el?.scrollIntoView({block:'nearest',behavior:'smooth'}); }
  if(matchMedia('(max-width:730px)').matches) document.querySelector('.inspector').scrollIntoView({block:'start',behavior:'smooth'});
}
$('back-to-board').addEventListener('click',()=>$('board').querySelector(`[data-shot="${selected}"]`)?.scrollIntoView({block:'center',behavior:'smooth'}));
const options=(items,value)=>items.map(v=>`<option ${v===value?'selected':''}>${esc(v)}</option>`).join('');
function renderInspector() {
  const s=currentShot(),index=project?.shots.findIndex(s=>s.id===selected);
  $('inspector-number').textContent=s?`${String(index+1).padStart(2,'0')} / ${String(project.shots.length).padStart(2,'0')}`:'—';
  if(!s) { $('inspector-content').innerHTML=`<div class="inspector-empty">${icon('frame')}<p>Select a frame.<br>Make it your own.</p></div>`;return; }
  $('inspector-content').innerHTML=`<div class="inspector-preview">${frame(s,false)}</div><div class="image-actions"><button class="button quiet" data-action="upload">${icon('upload')}${s.image?'Replace image':'Upload a frame'}</button>${s.image?`<button class="icon-button" data-action="remove-image" aria-label="Remove image" title="Remove image">${icon('trash')}</button>`:''}</div>
    <label>Shot title<input data-field="title" value="${esc(s.title)}" maxlength="120" required></label>
    <label>Scene / location<input data-field="scene" value="${esc(s.scene)}" maxlength="100" placeholder="01 · EXT. LOCATION / DAY"></label>
    <label>Shot type<select data-field="type">${Object.entries(typeNames).map(([v,n])=>`<option value="${v}" ${v===s.type?'selected':''}>${n}</option>`).join('')}</select></label>
    <div class="field-grid"><label>Camera angle<select data-field="angle">${options(angles,s.angle)}</select></label><label>Movement<select data-field="movement">${options(movements,s.movement)}</select></label></div>
    <div class="field-grid"><label>Duration (seconds)<input data-field="duration" type="number" min="1" max="3600" step="1" required value="${s.duration}"></label><label>Exit transition<select data-field="transition">${options(transitions,s.transition)}</select></label></div>
    <button class="recommend-button" data-action="recommend">${icon('spark')}<span id="recommend-time">Use suggested timing · ${s.recommendedDuration||suggested(s)}s</span></button>
    <hr class="section-divider"><div class="inspector-section-title">THE MOMENT</div><label>Action / direction<textarea data-field="notes" rows="3" maxlength="20000" placeholder="What do we see?">${esc(s.notes)}</textarea></label><label>Dialogue<textarea data-field="dialogue" rows="3" maxlength="20000" placeholder="What do we hear?">${esc(s.dialogue)}</textarea></label>
    <hr class="section-divider"><div class="inspector-section-title">CAMERA CHECKLIST ${icon('camera')}</div><ul id="camera-checklist" class="checklist">${(s.checklist||[]).map(x=>`<li>${esc(x)}</li>`).join('')}</ul>
    <div class="inspector-actions"><button class="icon-button" data-action="up" ${index===0?'disabled':''} title="Move earlier" aria-label="Move shot earlier">${icon('arrowup')}</button><button class="icon-button" data-action="down" ${index===project.shots.length-1?'disabled':''} title="Move later" aria-label="Move shot later">${icon('arrowdown')}</button><button class="button quiet" data-action="duplicate">${icon('copy')}Duplicate</button><button class="icon-button" data-action="delete" title="Delete shot" aria-label="Delete shot">${icon('trash')}</button></div>`;
}
function suggested(s) {return s.type==='ESTABLISHING'?6:s.type==='ACTION'?8:Math.min(3600,Math.max(3,Math.ceil((s.dialogue.trim()?s.dialogue.trim().split(/\s+/).length:0)/2.5)+1));}
function renderTimeline() {
  $('timeline-track').innerHTML=project?.shots.map((s,i)=>`<button class="timeline-segment ${s.type} ${s.id===selected?'active':''}" style="flex:${s.duration}" data-shot="${s.id}" aria-label="Shot ${i+1}: ${esc(s.title)}, ${s.duration} seconds" title="${esc(s.title)} · ${s.duration}s"><span class="mono">${String(i+1).padStart(2,'0')}</span><small>${s.duration}s</small></button>`).join('')||'';
}
function openShotDialog() {if(!project)return; if(project.shots.length>=250){notify('This storyboard has reached its limit of 250 shots.');return;} $('shot-form').reset();$('shot-dialog').showModal();$('shot-name').focus();}
async function enrichShot(s) {
  // Timing/checklists are always recalculated by the domain layer on save.
  await saveProject();
  if(selected===s.id && $('camera-checklist')) {
    $('camera-checklist').innerHTML=(s.checklist||[]).map(item=>`<li>${esc(item)}</li>`).join('');
    $('recommend-time').textContent=`Use suggested timing · ${s.recommendedDuration}s`;
  }
}
function mutateSequence(action) {action();changed();renderBoard();renderInspector();}
function moveShot(id,delta) {const i=project.shots.findIndex(s=>s.id===id),next=i+delta;if(next<0||next>=project.shots.length)return;mutateSequence(()=>{[project.shots[i],project.shots[next]]=[project.shots[next],project.shots[i]];});}

$('board').addEventListener('click',event=>{const action=event.target.closest('[data-action]');if(action?.dataset.action==='add')openShotDialog();else if(action?.dataset.action==='clear-search'){$('search').value='';renderBoard();}else{const card=event.target.closest('[data-shot]');if(card)selectShot(card.dataset.shot);}});
$('board').addEventListener('keydown',event=>{if(event.target.matches('[data-shot]')&&(event.key==='Enter'||event.key===' ')){event.preventDefault();selectShot(event.target.dataset.shot);}});
$('board').addEventListener('dragstart',event=>{const card=event.target.closest('[data-shot]');if(!card)return;dragging=card.dataset.shot;event.dataTransfer.setData('text/plain',dragging);event.dataTransfer.effectAllowed='move';card.classList.add('dragging');});
$('board').addEventListener('dragover',event=>{const card=event.target.closest('[data-shot]');if(card&&dragging&&card.dataset.shot!==dragging){event.preventDefault();event.dataTransfer.dropEffect='move';card.classList.add('drag-over');}});
$('board').addEventListener('dragleave',event=>{const card=event.target.closest('[data-shot]');if(card&&!card.contains(event.relatedTarget))card.classList.remove('drag-over');});
$('board').addEventListener('drop',event=>{event.preventDefault();const target=event.target.closest('[data-shot]');if(!target||!dragging||target.dataset.shot===dragging)return;const source=project.shots.findIndex(s=>s.id===dragging),dest=project.shots.findIndex(s=>s.id===target.dataset.shot);if(source<0||dest<0)return;mutateSequence(()=>{project.shots.splice(dest,0,project.shots.splice(source,1)[0]);});dragging=null;});
$('board').addEventListener('dragend',()=>{dragging=null;document.querySelectorAll('.dragging,.drag-over').forEach(el=>el.classList.remove('dragging','drag-over'));});
$('timeline-track').addEventListener('click',event=>{const shot=event.target.closest('[data-shot]');if(shot)selectShot(shot.dataset.shot,true);});
$('search').addEventListener('input',renderBoard);
for(const mode of ['grid','list']) $(mode+'-view').addEventListener('click',()=>{view=mode;for(const m of ['grid','list']){$(m+'-view').classList.toggle('active',m===mode);$(m+'-view').setAttribute('aria-pressed',String(m===mode));}renderBoard();});
$('add-shot').addEventListener('click',openShotDialog);
$('shot-form').addEventListener('submit',event=>{
  event.preventDefault();const title=$('shot-name').value.trim();if(!title)return;
  const type=$('shot-type').value;
  const s={id:crypto.randomUUID(),type,title,scene:'',notes:'',dialogue:'',duration:type==='ESTABLISHING'?6:type==='ACTION'?8:3,angle:'Eye level',movement:type==='ACTION'?'Tracking':'Static',transition:'Cut',image:'',illustration:'',recommendedDuration:0,checklist:[]};
  project.shots.push(s);selected=s.id;changed();renderBoard();renderInspector();$('shot-dialog').close();enrichShot(s).catch(()=>{});notify('A new moment added.');
});
$('inspector-content').addEventListener('input',event=>{
  const field=event.target.dataset.field,s=currentShot();if(!field||!s||event.target.tagName==='SELECT')return;
  let value=event.target.value;
  if(field==='duration') {if(!event.target.validity.valid)return;value=Number(value);}
  if(field==='title')value=value.trim()||'Untitled shot';
  s[field]=value;if(field==='dialogue'){s.recommendedDuration=suggested(s);$('recommend-time').textContent=`Use suggested timing · ${s.recommendedDuration}s`;}
  changed();refreshCard(s.id);
});
$('inspector-content').addEventListener('change',event=>{
  const field=event.target.dataset.field,s=currentShot();if(!field||!s)return;
  if(field==='duration'&&!event.target.validity.valid){event.target.value=s.duration;notify('Duration must be a whole number from 1 to 3600 seconds.');return;}
  if(event.target.tagName==='SELECT'){s[field]=event.target.value;changed();refreshCard(s.id);if(field==='type'){s.recommendedDuration=suggested(s);enrichShot(s).catch(()=>{});}}
  if(field==='title'&&!event.target.value.trim())event.target.value=s.title;
});
$('inspector-content').addEventListener('click',async event=>{
  const button=event.target.closest('[data-action]'),s=currentShot();if(!button||!s)return;
  switch(button.dataset.action){
    case 'upload':$('image-upload').click();break;
    case 'remove-image':s.image='';changed();refreshCard(s.id);renderInspector();break;
    case 'recommend':s.duration=suggested(s);changed();refreshCard(s.id);renderInspector();notify(`Suggested timing applied: ${s.duration} seconds.`);break;
    case 'up':moveShot(s.id,-1);break;
    case 'down':moveShot(s.id,1);break;
    case 'duplicate':if(project.shots.length>=250){notify('Maximum 250 shots per storyboard.');break;}mutateSequence(()=>{const copy=structuredClone(s);copy.id=crypto.randomUUID();copy.title=(s.title+' (copy)').slice(0,120);project.shots.splice(project.shots.indexOf(s)+1,0,copy);selected=copy.id;});notify('Shot duplicated.');break;
    case 'delete':if(confirm(`Remove “${s.title}” from this storyboard?`)){mutateSequence(()=>{const i=project.shots.indexOf(s);project.shots.splice(i,1);selected=project.shots[Math.min(i,project.shots.length-1)]?.id||null;});}break;
  }
});
$('image-upload').addEventListener('change',async event=>{
  const file=event.target.files[0],s=currentShot();event.target.value='';if(!file||!s)return;
  if(!['image/png','image/jpeg','image/webp'].includes(file.type)||file.size>20*1024*1024){notify('Choose a PNG, JPEG, or WebP image under 20 MB.');return;}
  try {
    const bitmap=await createImageBitmap(file),scale=Math.min(1,1280/Math.max(bitmap.width,bitmap.height));
    const canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(bitmap.width*scale));canvas.height=Math.max(1,Math.round(bitmap.height*scale));
    const ctx=canvas.getContext('2d');ctx.fillStyle='#fff';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(bitmap,0,0,canvas.width,canvas.height);bitmap.close();
    s.image=canvas.toDataURL('image/jpeg',.88);changed();refreshCard(s.id);if(selected===s.id)renderInspector();notify('Frame uploaded.');
  } catch(_){notify('This image could not be opened. Try a different PNG or JPEG.');}
});

function showProjectDialog(edit=false){editingProject=edit;$('project-dialog-title').textContent=edit?'Your storyboard':'Start a storyboard';$('project-submit').textContent=edit?'Save details':'Create storyboard';$('project-name').value=edit?project.title:'';$('project-desc').value=edit?project.description:'';$('demo-option').hidden=edit;$('delete-project').hidden=!edit;$('with-demo').checked=false;$('project-dialog').showModal();$('project-name').focus();}
$('new-project').addEventListener('click',()=>safeSwitch(()=>showProjectDialog()));
$('edit-project').addEventListener('click',()=>{if(project)showProjectDialog(true);});
$('project-form').addEventListener('submit',async event=>{
  event.preventDefault();const title=$('project-name').value.trim();if(!title)return;
  $('project-submit').disabled=true;
  try {
    if(editingProject){project.title=title;project.description=$('project-desc').value;changed();renderAll();await saveProject();}
    else {await saveProject();const created=await api('/api/projects',{method:'POST',body:JSON.stringify({title,demo:$('with-demo').checked})});projects.unshift(created);await openProject(created.id);project.description=$('project-desc').value;changed();renderAll();await saveProject();}
    $('project-dialog').close();notify(editingProject?'Storyboard details saved.':'Your new story starts here.');
  }catch(error){showError(error.message);}finally{$('project-submit').disabled=false;}
});
$('delete-project').addEventListener('click',async()=>{
  if(!project||!confirm(`Delete “${project.title}” and all its shots? Export a backup first if you need to keep it.`))return;
  clearTimeout(saveTimer);
  try {
    if(savePromise)await savePromise;
    await api(`/api/projects/${project.id}`,{method:'DELETE'});projects=projects.filter(p=>p.id!==project.id);project=null;revision=savedRevision=0;blocked=false;
    if(projects.length)await openProject(projects[0].id);else{const created=await api('/api/projects',{method:'POST',body:JSON.stringify({title:'Untitled storyboard',demo:false})});projects=[created];await openProject(created.id);}
    $('project-dialog').close();notify('Storyboard deleted.');
  }catch(error){showError(error.message);}
});
$('project-list').addEventListener('click',event=>{const link=event.target.closest('[data-project]');if(link&&link.dataset.project!==project?.id)safeSwitch(()=>openProject(link.dataset.project));});
$('save-status').addEventListener('click',()=>saveProject().catch(error=>showError(error.message)));
$('retry-save').addEventListener('click',()=>{if(blocked){if(confirm('Export a backup first if you need your unsaved changes. Reload the saved storyboard now?'))openProject(project.id).catch(error=>showError(error.message));}else if(!project)initialize();else saveProject().catch(error=>showError(error.message));});

function download(name,content,type){const url=URL.createObjectURL(new Blob([content],{type}));const a=document.createElement('a');a.href=url;a.download=name;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);}
function filename(){return(project?.title||'storyboard').replace(/[^a-z0-9\-_ ]/gi,'').trim().replace(/\s+/g,'-').slice(0,80)||'storyboard';}
function exportJSON(){if(!project)return;download(filename()+'.boardflow.json',JSON.stringify({format:'boardflow-web',version:1,project},null,2),'application/json');notify('Project backup exported.');}
$('export').addEventListener('click',()=>{if(project)$('export-dialog').showModal();});
$('export-json').addEventListener('click',()=>{exportJSON();$('export-dialog').close();});
$('backup-work').addEventListener('click',exportJSON);
$('export-csv').addEventListener('click',()=>{
  if(!project)return;
  // Prefix spreadsheet formula markers so notes remain plain text when opened in Excel.
  const cell=value=>{let text=String(value??'');if(/^[\s]*[=+@-]/.test(text))text="'"+text;return '"'+text.replace(/"/g,'""')+'"';};
  const rows=[['Shot','Title','Type','Scene','Camera angle','Movement','Duration (seconds)','Transition','Action / direction','Dialogue'],...project.shots.map((s,i)=>[i+1,s.title,typeNames[s.type],s.scene,s.angle,s.movement,s.duration,s.transition,s.notes,s.dialogue])];
  download(filename()+'.csv','\ufeff'+rows.map(row=>row.map(cell).join(',')).join('\r\n'),'text/csv;charset=utf-8');$('export-dialog').close();notify('Shot list exported.');
});
$('import-project').addEventListener('click',()=>$('import-file').click());
$('import-file').addEventListener('change',async event=>{
  const file=event.target.files[0];event.target.value='';if(!file)return;
  if(file.size>60*1024*1024){notify('Choose a project backup under 60 MB.');return;}
  try {
    const backup=JSON.parse(await file.text());
    if(backup.format!=='boardflow-web'||backup.version!==1||!backup.project||!Array.isArray(backup.project.shots)||backup.project.shots.length>250)throw new Error('Choose a BoardFlow web project JSON backup.');
    await saveProject();
    const created=await api('/api/projects',{method:'POST',body:JSON.stringify({title:String(backup.project.title||'Imported storyboard').slice(0,120),demo:false})});
    try {
      const imported={...backup.project,id:created.id,version:created.version};
      const saved=await api(`/api/projects/${created.id}`,{method:'PUT',body:JSON.stringify(imported)});
      projects.unshift(saved);await openProject(saved.id);notify('Storyboard imported.');
    }catch(error){await api(`/api/projects/${created.id}`,{method:'DELETE'});throw error;}
  }catch(error){showError(error.message||'The backup could not be imported.');}
});
$('help').addEventListener('click',()=>$('help-dialog').showModal());
document.querySelectorAll('.close-dialog').forEach(button=>button.addEventListener('click',()=>button.closest('dialog').close()));
document.querySelectorAll('dialog').forEach(dialog=>dialog.addEventListener('click',event=>{if(event.target===dialog){const r=dialog.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)dialog.close();}}));

let playerShots=[],playerTotal=0,playerElapsed=0,playerStarted=0,playerRunning=false,playerRAF=0,playerCurrent=-1;
function shotAt(seconds){let start=0;for(let i=0;i<playerShots.length;i++){if(seconds<start+playerShots[i].duration)return{i,start};start+=playerShots[i].duration;}return{i:playerShots.length-1,start:playerTotal-playerShots.at(-1).duration};}
function playerFrame(){
  if(playerRunning){playerElapsed=Math.min(playerTotal,(performance.now()-playerStarted)/1000);if(playerElapsed>=playerTotal){playerRunning=false;$('player-toggle').textContent='Play again';}}
  const {i,start}=shotAt(playerElapsed),s=playerShots[i];
  if(playerCurrent!==i){playerCurrent=i;$('player-stage').innerHTML=`<div class="player-layer">${frameContent(s)}</div><div id="player-next" class="player-layer" style="opacity:0">${playerShots[i+1]&&s.transition==='Dissolve'?frameContent(playerShots[i+1]):''}</div>`;$('player-shot').textContent=`${String(i+1).padStart(2,'0')} / ${String(playerShots.length).padStart(2,'0')} · ${s.title}`;$('player-angle').textContent=`${s.angle} · ${s.movement} · ${s.transition}`;$('player-notes').textContent=s.dialogue||s.notes;}
  const mix=Math.max(0,Math.min(1,(playerElapsed-start-s.duration+.6)/.6));
  $('player-next').style.opacity=s.transition==='Cut'?'0':String(mix);
  $('player-seek').value=playerElapsed;$('player-time').textContent=`${time(playerElapsed)} / ${time(playerTotal)}`;
  if(playerRunning)playerRAF=requestAnimationFrame(playerFrame);
}
function openPlayer(){if(!project?.shots.length)return;playerShots=structuredClone(project.shots);playerTotal=total();playerElapsed=0;playerCurrent=-1;playerStarted=performance.now();playerRunning=true;$('player-seek').max=playerTotal;$('player-title').textContent=project.title;$('player-toggle').textContent='Pause';$('player-dialog').showModal();cancelAnimationFrame(playerRAF);playerFrame();}
function togglePlayer(){
  if(playerRunning){playerElapsed=Math.min(playerTotal,(performance.now()-playerStarted)/1000);playerRunning=false;cancelAnimationFrame(playerRAF);$('player-toggle').textContent='Play';}
  else{if(playerElapsed>=playerTotal){playerElapsed=0;playerCurrent=-1;}playerStarted=performance.now()-playerElapsed*1000;playerRunning=true;$('player-toggle').textContent='Pause';playerFrame();}
}
$('play').addEventListener('click',openPlayer);
$('player-toggle').addEventListener('click',togglePlayer);
$('player-restart').addEventListener('click',()=>{cancelAnimationFrame(playerRAF);playerElapsed=0;playerCurrent=-1;playerStarted=performance.now();playerRunning=true;$('player-toggle').textContent='Pause';playerFrame();});
$('player-seek').addEventListener('input',event=>{playerElapsed=Number(event.target.value);playerStarted=performance.now()-playerElapsed*1000;cancelAnimationFrame(playerRAF);playerFrame();});
$('player-dialog').addEventListener('close',()=>{playerRunning=false;cancelAnimationFrame(playerRAF);$('player-stage').innerHTML='';});
document.addEventListener('keydown',event=>{
  const typing=event.target.matches('input,textarea,select,[contenteditable]');
  if((event.ctrlKey||event.metaKey)&&event.key.toLowerCase()==='s'){event.preventDefault();saveProject().then(()=>notify('Storyboard saved.')).catch(error=>showError(error.message));return;}
  if(typing)return;
  if($('player-dialog').open&&event.code==='Space'){event.preventDefault();togglePlayer();return;}
  if(document.querySelector('dialog[open]'))return;
  if(event.key.toLowerCase()==='n'){event.preventDefault();openShotDialog();}
  else if(event.code==='Space'){event.preventDefault();openPlayer();}
  else if(event.key==='?'){$('help-dialog').showModal();}
});
window.addEventListener('beforeunload',event=>{if(revision!==savedRevision){event.preventDefault();event.returnValue='';}});
async function initialize(){
  try {
    projects=await api('/api/projects');
    if(!projects.length){const demo=await api('/api/projects',{method:'POST',body:JSON.stringify({title:'Before the first train',demo:true})});projects=[demo];}
    let last=null;try{last=localStorage.getItem('boardflow-project');}catch(_){}
    await openProject(projects.some(p=>p.id===last)?last:projects[0].id);
  }catch(error){setSaveLabel('Connection unavailable',true);$('board').innerHTML=`<div class="empty-state">${icon('folder')}<h2>The studio is taking a moment.</h2><p>Could not reach the BoardFlow server. Use Retry to reconnect.</p></div>`;showError(error.message);}
}
initialize();
