import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile, unlink } from 'node:fs/promises';
import { resolve } from 'node:path';
import { createHash } from 'node:crypto';
import { EngineMcpClient, root, gameClassPath } from './mcp-client.mjs';

const headless = process.argv.includes('--headless');
const mode = headless ? 'headless' : 'native';
const artifacts = resolve(root, 'src/xenoamess_chess/target/automation-artifacts', mode);
const sessionPath = `src/xenoamess_chess/target/smoke-${mode}-${process.pid}.json`;
await mkdir(artifacts, { recursive: true });
const engine = spawn('java', ['-cp', gameClassPath, 'com.xenoamess.cyan_potion.xenoamess_chess.ChessDemo', '--session', sessionPath, ...(headless ? ['--headless'] : [])], { cwd: root, windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] });
let log = ''; engine.stdout.on('data', data => { log = (log + data).slice(-120000); }); engine.stderr.on('data', data => { log = (log + data).slice(-120000); });
const client = new EngineMcpClient(sessionPath); const checks = [];
const passed = name => { checks.push(name); console.log(`PASS ${name}`); };
const find = (state, kind, owner = 0) => state.game.units.find(unit => unit.kind === kind && unit.owner === owner);
const at = (state, x, y) => state.game.units.find(unit => unit.cells.some(cell => cell.x === x && cell.y === y));
const clickCell = (x, y) => client.command('ui_click', { x: 28 + x * 36 + 18, y: 104 + y * 36 + 18 });
const nextOwnTurn = async () => { await client.command('end_turn'); await client.command('end_turn'); };
let state;
try {
  const tools = await client.initialize(); assert.equal(tools.tools.length, 5); passed('stdio MCP initialize, notification, tools/list');
  const deadline = Date.now() + 45000;
  while (Date.now() < deadline) {
    if (engine.exitCode !== null) throw new Error(`Engine exited (${engine.exitCode}): ${log}`);
    try { state = await client.state(); break; } catch { await new Promise(resolve => setTimeout(resolve, 200)); }
  }
  assert.ok(state, `Engine did not become ready: ${log}`); assert.equal(state.runtime, mode); assert.equal(state.game.units.length, 6);
  const commands = (await client.tool('engine_commands')).structuredContent.data;
  assert.equal(commands.length, 16); passed(`${mode} engine attached with discoverable command schemas`);
  const firstImage = resolve(artifacts, '01-standard.png'); await client.screenshot(firstImage);
  const png = await readFile(firstImage); assert.equal(png.subarray(1, 4).toString(), 'PNG'); assert.ok(png.readUInt32BE(16) > 500); passed('PNG frame captured through MCP');

  await client.command('ui_click', { controlId: 'deploy:MINION' }); state = await clickCell(8, 8);
  const deployed = at(state, 8, 8); assert.equal(deployed.kind, 'MINION'); assert.equal(deployed.waiting, true);
  const before = JSON.stringify(state.game);
  await assert.rejects(client.command('move', { unitId: deployed.id, dx: 1, dy: 0 })); assert.equal(JSON.stringify((await client.state()).game), before);
  passed('UI deployment and waiting rule; rejected command leaves game unchanged');
  await client.command('ui_key', { key: 'SPACE' }); await client.command('ui_key', { key: 'SPACE' }); await clickCell(8, 8);
  state = await client.command('ui_key', { key: 'RIGHT' }); assert.equal(at(state, 9, 8).id, deployed.id); assert.equal(at(state, 9, 8).acted, true);
  passed('pixel selection and keyboard movement after next own turn');
  await clickCell(2, 6); await client.command('ui_click', { controlId: 'produce' }); state = await clickCell(2, 5);
  assert.equal(at(state, 2, 5).waiting, true); passed('factory production through visible UI controls');
  await client.command('save', { slot: `smoke-${mode}` }); const savedPly = state.game.ply;
  await client.command('end_turn'); state = await client.command('load', { slot: `smoke-${mode}` }); assert.equal(state.game.ply, savedPly);
  await assert.rejects(client.command('save', { slot: '../escape' })); passed('save/load round trip and slot validation');

  state = await client.command('ui_click', { controlId: 'showcase' }); assert.equal(state.game.units.length, 20); assert.equal(new Set(state.game.units.map(unit => unit.kind)).size, 10);
  const showcaseImage = resolve(artifacts, '02-ten-units.png'); await client.screenshot(showcaseImage);
  assert.notEqual(createHash('sha256').update(png).digest('hex'), createHash('sha256').update(await readFile(showcaseImage)).digest('hex'));
  passed('ten unit types displayed in the rendered showcase');
  const factory = find(state, 'FACTORY'); await client.command('produce', { unitId: factory.id, x: 3, y: 4 });
  await client.command('deploy', { kind: 'MINION', x: 3, y: 5 });
  state = await client.command('deploy', { kind: 'ASSAULT', x: 3, y: 4, rotation: 1 }); assert.equal(at(state, 3, 4).kind, 'ASSAULT'); assert.equal(at(state, 3, 4).components.length, 2);
  passed('rotated recipe combines complete materials and waits');
  await client.command('ui_click', { controlId: 'expand' }); state = await clickCell(18, 8); assert.equal(state.game.boards.length, 2); passed('board expansion through UI coordinates');
  const archer = find(state, 'ARCHER'), enemyFactory = find(state, 'FACTORY', 1);
  state = await client.command('attack', { unitId: archer.id, targetId: enemyFactory.id }); assert.equal(find(state, 'FACTORY', 1).hp, 3);
  await client.command('end_turn'); state = await client.command('produce', { unitId: enemyFactory.id, x: 11, y: 4 }); assert.equal(at(state, 11, 4).waiting, true);
  passed('wounded factory retains full production functionality');

  state = await client.command('new_game', { scenario: 'showcase' });
  const cannon = find(state, 'CANNON'), engineer = find(state, 'ENGINEER'), guardian = find(state, 'GUARDIAN');
  const enemyCannon = find(state, 'CANNON', 1), enemyLancer = find(state, 'LANCER', 1);
  for (let turn = 0; turn < 2; turn++) {
    await client.command('move', { unitId: engineer.id, dx: 0, dy: -1 }); await client.command('move', { unitId: cannon.id, dx: 1, dy: 0 });
    await client.command('end_turn'); await client.command('move', { unitId: enemyLancer.id, dx: -1, dy: 0 }); await client.command('end_turn');
  }
  state = await client.command('attack', { unitId: cannon.id, targetId: enemyCannon.id });
  assert.ok(!state.game.units.some(unit => unit.id === enemyCannon.id)); assert.equal(at(state, 9, 11).kind, 'MINION'); assert.equal(at(state, 9, 11).owner, 1); assert.equal(at(state, 9, 11).waiting, true);
  const component = at(state, 9, 11); passed('high damage triggers recursive breakup with ownership and no overflow');
  await client.command('end_turn'); state = await client.command('attack', { unitId: enemyLancer.id, targetId: guardian.id }); assert.equal(find(state, 'GUARDIAN').hp, 3);
  for (let x = 1; x <= 3; x++) await client.command('deploy', { kind: 'MINION', x, y: 13 });
  await client.command('end_turn'); state = await client.command('repair', { unitId: engineer.id, targetId: guardian.id }); assert.equal(find(state, 'GUARDIAN').hp, 4); passed('lancer damage and engineer repair use current durability');
  state = await client.command('sweep', { unitId: find(state, 'SWEEPER').id, dx: 0, dy: -1 }); assert.equal(state.game.scores[0], 3); passed('sweeper clears three distinct minions once each');
  state = await client.command('attack', { unitId: cannon.id, targetId: component.id }); assert.equal(state.game.scores[0], 4); passed('destroyed base component is removed and scored');
  const lancer = find(state, 'LANCER'); state = await client.command('rotate', { unitId: lancer.id, x: 1, y: 8 }); assert.ok(find(state, 'LANCER').cells.some(cell => cell.y === 10)); passed('rotation transforms a multi-cell unit');

  await client.command('deploy', { kind: 'MINION', x: 8, y: 1 });
  for (const [x, y] of [[11, 0], [10, 1], [11, 1], [12, 1], [11, 2]]) await client.command('deploy', { kind: 'MINION', x, y });
  state = await client.command('deploy', { kind: 'RELAY', x: 10, y: 0 });
  const source = state.game.units.find(unit => unit.kind === 'RELAY' && unit.owner === 0 && unit.cells.some(cell => cell.x === 5));
  const receiver = at(state, 11, 0), cargo = at(state, 8, 1); await nextOwnTurn();
  state = await client.command('teleport', { unitId: source.id, targetId: cargo.id, receiverId: receiver.id, x: 12, y: 2 });
  assert.equal(at(state, 12, 2).id, cargo.id); assert.equal(at(state, 12, 2).acted, false); assert.equal(state.game.units.find(unit => unit.id === source.id).acted, true);
  await clickCell(12, 2); state = await client.command('ui_key', { key: 'RIGHT' }); assert.equal(at(state, 13, 2).id, cargo.id); passed('relay moves ready cargo without refreshing or spending cargo action');
  await client.screenshot(resolve(artifacts, '03-combat-and-relay.png'));
  while (state.game.winner < 0) state = await client.command('end_turn');
  assert.equal(state.game.winner, 0); assert.equal(state.game.ply, 60); passed('complete match ends at round limit with the higher score winning');
  const events = (await client.tool('engine_events')).structuredContent.data; assert.ok(events.length >= 60); assert.ok(events.every((event, i) => i === 0 || event.sequence > events[i - 1].sequence));
  passed('monotonic MCP event history records real commands');
  await client.screenshot(resolve(artifacts, '04-victory.png'));
  await writeFile(resolve(artifacts, 'report.json'), JSON.stringify({ mode, passed: checks.length, checks, finishedAt: new Date().toISOString(), winner: state.game.winner, scores: state.game.scores }, null, 2));
  console.log(`Completed ${checks.length} ${mode} MCP integration checks. Artifacts: ${artifacts}`);
} finally {
  client.close(); engine.kill(); await writeFile(resolve(artifacts, 'engine.log'), log);
  await unlink(resolve(root, sessionPath)).catch(() => {});
}
