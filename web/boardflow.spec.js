const { test, expect } = require('@playwright/test');

let projectId;
test.beforeEach(async ({ page, request }) => {
  const response=await request.post('/api/projects',{data:{title:'Before the first train',demo:true}});
  expect(response.status()).toBe(201);projectId=(await response.json()).id;
  await page.addInitScript(id=>localStorage.setItem('boardflow-project',id),projectId);
  await page.goto('/');await expect(page.locator('.shot-card')).toHaveCount(6);
});
test.afterEach(async ({request}) => {if(projectId)await request.delete('/api/projects/'+projectId);});
const saved=async page=>expect(page.locator('#save-label')).toHaveText('All changes saved');

test('desktop editor renders without overflow or browser errors',async({page})=>{
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  await expect(page.locator('#runtime')).toHaveText('00:36');
  await expect(page.locator('#scene-count')).toHaveText('4');
  await expect(page.locator('#camera-checklist li')).toHaveCount(4);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:'test-results/boardflow-desktop.png',fullPage:true});
  expect(errors).toEqual([]);
});
test('shot edits, timing and images survive a browser reload',async({page,request})=>{
  await page.locator('[data-field="title"]').fill('A new opening');
  await page.locator('[data-field="duration"]').fill('12');
  await page.locator('[data-field="notes"]').fill('A new line of direction.\n你好 — keep this note.');
  await page.locator('[data-field="angle"]').selectOption('Low angle');
  const png=await page.evaluate(()=>{const canvas=document.createElement('canvas');canvas.width=160;canvas.height=90;const ctx=canvas.getContext('2d');ctx.fillStyle='#d9ed92';ctx.fillRect(0,0,160,90);return canvas.toDataURL('image/png').split(',')[1];});
  await page.locator('#image-upload').setInputFiles({name:'frame.png',mimeType:'image/png',buffer:Buffer.from(png,'base64')});
  await expect(page.locator('.inspector-preview img')).toBeVisible();await saved(page);
  await expect(page.locator('#runtime')).toHaveText('00:42');
  await page.reload();await expect(page.locator('[data-field="title"]')).toHaveValue('A new opening');
  await expect(page.locator('[data-field="notes"]')).toHaveValue('A new line of direction.\n你好 — keep this note.');
  await expect(page.locator('[data-field="angle"]')).toHaveValue('Low angle');
  const result=await(await request.get('/api/projects/'+projectId)).json();expect(result.shots[0].image).toContain('data:image/jpeg;base64,');
});
test('add, change type, recommend, duplicate, reorder and remove a shot',async({page})=>{
  await page.locator('#add-shot').click();await page.locator('#shot-name').fill('A final conversation');
  await page.locator('#shot-type').selectOption('DIALOGUE');await page.locator('#shot-form button[type=submit]').click();
  await expect(page.locator('.shot-card')).toHaveCount(7);await saved(page);
  await page.locator('[data-field="dialogue"]').fill('one two three four five six seven eight nine ten');
  await page.locator('[data-action="recommend"]').click();await expect(page.locator('[data-field="duration"]')).toHaveValue('5');
  await page.locator('[data-field="type"]').selectOption('ACTION');await saved(page);
  await expect(page.locator('#camera-checklist')).toContainText('tracking path');
  await page.locator('[data-action="recommend"]').click();await expect(page.locator('[data-field="duration"]')).toHaveValue('8');
  await page.locator('[data-action="duplicate"]').click();await expect(page.locator('.shot-card')).toHaveCount(8);
  await page.locator('[data-action="up"]').click();await expect(page.locator('.shot-card').nth(6)).toContainText('(copy)');
  page.once('dialog',dialog=>dialog.accept());await page.locator('[data-action="delete"]').click();await expect(page.locator('.shot-card')).toHaveCount(7);await saved(page);
});
test('dragging changes the persistent sequence',async({page,request})=>{
  const cards=page.locator('.shot-card'),firstId=await cards.nth(0).getAttribute('data-shot');
  await cards.nth(0).dragTo(cards.nth(2));await saved(page);
  const result=await(await request.get('/api/projects/'+projectId)).json();expect(result.shots[2].id).toBe(firstId);
});
test('search and shot list operate on the same storyboard',async({page})=>{
  await page.locator('#search').fill('quiet skyline');await expect(page.locator('.shot-card')).toHaveCount(1);
  await page.locator('#search').fill('no matching frames');await expect(page.locator('#board')).toContainText('No frames found.');
  await page.locator('[data-action="clear-search"]').click();await expect(page.locator('.shot-card')).toHaveCount(6);
  await page.locator('#list-view').click();await expect(page.locator('#board')).toHaveClass(/list-mode/);
  await page.locator('#grid-view').click();await expect(page.locator('#board')).not.toHaveClass(/list-mode/);
});
test('animatic plays, pauses, seeks and restarts',async({page})=>{
  await page.locator('#play').click();await expect(page.locator('#player-dialog')).toBeVisible();
  await expect(page.locator('#player-shot')).toContainText('The city wakes');
  await page.locator('#player-toggle').click();await expect(page.locator('#player-toggle')).toHaveText('Play');
  const paused=await page.locator('#player-seek').inputValue();await page.waitForTimeout(250);expect(await page.locator('#player-seek').inputValue()).toBe(paused);
  await page.locator('#player-seek').fill('6.1');await page.locator('#player-seek').dispatchEvent('input');await expect(page.locator('#player-shot')).toContainText('A familiar route');
  await page.locator('#player-restart').click();await expect(page.locator('#player-shot')).toContainText('The city wakes');
  await page.keyboard.press('Escape');await expect(page.locator('#player-dialog')).not.toBeVisible();
});
test('project backup exports and imports with all shots',async({page,request})=>{
  await page.locator('#export').click();const downloadPromise=page.waitForEvent('download');await page.locator('#export-json').click();
  const download=await downloadPromise;expect(download.suggestedFilename()).toBe('Before-the-first-train.boardflow.json');
  const file=await download.path();await page.locator('#import-file').setInputFiles(file);
  await expect(page.locator('#notice')).toContainText('Storyboard imported.');
  await expect(page.locator('.shot-card')).toHaveCount(6);
  const newId=await page.evaluate(()=>localStorage.getItem('boardflow-project'));expect(newId).not.toBe(projectId);
  await request.delete('/api/projects/'+newId);
});
test('edits made while a save is in flight are not lost',async({page,request})=>{
  await page.route('**/api/projects/*',async route=>{if(route.request().method()==='PUT')await new Promise(resolve=>setTimeout(resolve,400));await route.continue();});
  await page.locator('[data-field="title"]').fill('First edit');await expect(page.locator('#save-label')).toHaveText('Saving…');
  await page.locator('[data-field="notes"]').fill('Second edit during the save');await saved(page);
  const result=await(await request.get('/api/projects/'+projectId)).json();expect(result.shots[0].title).toBe('First edit');expect(result.shots[0].notes).toBe('Second edit during the save');
});
test('stale-tab conflicts preserve edits and offer a backup',async({page,request})=>{
  const server=await(await request.get('/api/projects/'+projectId)).json();server.title='Changed elsewhere';
  expect((await request.put('/api/projects/'+projectId,{data:server})).status()).toBe(200);
  await page.locator('[data-field="notes"]').fill('Keep my unsaved work');
  await expect(page.locator('#connection-error')).toBeVisible();await expect(page.locator('#error-message')).toContainText('another tab');
  await expect(page.locator('[data-field="notes"]')).toHaveValue('Keep my unsaved work');
  const downloadPromise=page.waitForEvent('download');await page.locator('#backup-work').click();expect((await downloadPromise).suggestedFilename()).toContain('.json');
});
test('phone layout remains usable without horizontal scrolling',async({page})=>{
  await page.setViewportSize({width:390,height:844});await expect(page.locator('#add-shot')).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.locator('.shot-card').nth(2).click();await page.locator('.inspector').scrollIntoViewIfNeeded();
  await expect(page.locator('[data-field="title"]')).toHaveValue('One last message');
  await page.screenshot({path:'test-results/boardflow-mobile.png',fullPage:true});
});
test('projects can be created, renamed and deleted from the browser',async({page,request})=>{
  await page.locator('#new-project').click();await expect(page.locator('#delete-project')).not.toBeVisible();
  await page.locator('#project-name').fill('A different story');await page.locator('#project-desc').fill('A blank project for the next film.');
  await page.locator('#project-submit').click();await expect(page.locator('#project-title')).toHaveText('A different story');
  await expect(page.locator('.shot-card')).toHaveCount(0);await expect(page.locator('#play')).toBeDisabled();
  const id=await page.evaluate(()=>localStorage.getItem('boardflow-project'));
  await page.locator('#edit-project').click();await expect(page.locator('#demo-option')).not.toBeVisible();
  await page.locator('#project-name').fill('Renamed story');await page.locator('#project-submit').click();
  await expect(page.locator('#project-title')).toHaveText('Renamed story');
  await page.locator('#edit-project').click();page.once('dialog',dialog=>dialog.accept());await page.locator('#delete-project').click();
  await expect(page.locator('#project-dialog')).not.toBeVisible();expect((await request.get('/api/projects/'+id)).status()).toBe(404);
});
test('dissolve preview uses the configured shot timing',async({page})=>{
  await page.locator('[data-field="transition"]').selectOption('Dissolve');await saved(page);await page.locator('#play').click();
  await page.locator('#player-toggle').click();await page.locator('#player-seek').fill('5.7');await page.locator('#player-seek').dispatchEvent('input');
  const opacity=await page.locator('#player-next').evaluate(el=>Number(el.style.opacity));expect(opacity).toBeCloseTo(.5,1);
  await expect(page.locator('#player-next svg')).toHaveAttribute('aria-label','Example storyboard sketch: street');
  await page.keyboard.press('Escape');
});
