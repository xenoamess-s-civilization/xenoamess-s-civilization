import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { delimiter, resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { writeFile, mkdir } from 'node:fs/promises';

export const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
export const gameClassPath = ['src/xenoamess_chess/target/classes', 'src/engine-mcp/target/classes', 'src/base/target/classes', 'src/xenoamess_chess/target/dependency/*'].map(path => resolve(root, path)).join(delimiter);
export class EngineMcpClient {
  constructor(session = 'src/xenoamess_chess/target/automation-session.json') {
    this.process = spawn(process.execPath, [resolve(root, 'scripts/mcp-engine.mjs'), resolve(root, session)], { cwd: root, windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] });
    this.pending = new Map(); this.sequence = 0; this.stderr = '';
    this.process.stderr.on('data', data => { this.stderr = (this.stderr + data).slice(-12000); });
    createInterface({ input: this.process.stdout }).on('line', line => {
      try {
        const message = JSON.parse(line); const call = this.pending.get(message.id);
        if (!call) return;
        this.pending.delete(message.id); clearTimeout(call.timeout);
        message.error ? call.reject(new Error(JSON.stringify(message.error))) : call.resolve(message.result);
      } catch (error) { this.fail(new Error(`Invalid MCP stdout: ${line}: ${error.message}`)); }
    });
    this.process.on('error', error => this.fail(error));
    this.process.on('exit', code => this.fail(new Error(`MCP process exited (${code}): ${this.stderr}`)));
  }
  fail(error) { for (const call of this.pending.values()) { clearTimeout(call.timeout); call.reject(error); } this.pending.clear(); }
  request(method, params = {}) {
    const id = ++this.sequence;
    return new Promise((resolveCall, reject) => {
      const timeout = setTimeout(() => { this.pending.delete(id); reject(new Error(`MCP request timed out: ${method}`)); }, 20000);
      this.pending.set(id, { resolve: resolveCall, reject, timeout });
      this.process.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
    });
  }
  async initialize() {
    const result = await this.request('initialize', { protocolVersion: '2025-11-25', capabilities: {}, clientInfo: { name: 'cyan-potion-test-client', version: '0.1.0' } });
    if (result.protocolVersion !== '2025-11-25') throw new Error('Unsupported MCP protocol');
    this.process.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' }) + '\n');
    return this.request('tools/list');
  }
  async tool(name, args = {}) {
    const result = await this.request('tools/call', { name, arguments: args });
    if (result.isError) throw new Error(result.content.map(content => content.text || '').join('\n'));
    return result;
  }
  async state() { return (await this.tool('engine_status')).structuredContent.data; }
  async command(name, args = {}) { return (await this.tool('engine_command', { name, arguments: args })).structuredContent.data; }
  async screenshot(path) {
    const result = await this.tool('engine_screenshot'); const image = result.content.find(content => content.type === 'image');
    if (!image || image.mimeType !== 'image/png') throw new Error('Missing PNG screenshot');
    await mkdir(dirname(resolve(path)), { recursive: true }); await writeFile(path, Buffer.from(image.data, 'base64')); return path;
  }
  close() { this.process.stdin.end(); }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const client = new EngineMcpClient(process.env.CYAN_POTION_SESSION);
  try {
    await client.initialize();
    const name = process.argv[2] || 'engine_status'; const args = JSON.parse(process.argv[3] || '{}');
    if (name === 'engine_screenshot' && process.argv[4]) console.log(await client.screenshot(process.argv[4]));
    else console.log(JSON.stringify(await client.tool(name, args), null, 2));
  } finally { client.close(); }
}
