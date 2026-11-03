import { spawn } from 'node:child_process';
import { dirname, resolve, delimiter } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const cp = ['src/engine-mcp/target/classes', 'src/engine-mcp/target/dependency/*'].map(path => resolve(root, path)).join(delimiter);
const session = resolve(root, process.argv[2] || 'src/xenoamess_chess/target/automation-session.json');
const child = spawn('java', ['-cp', cp, 'com.xenoamess.cyan_potion.mcp.McpServer', session], { cwd: root, windowsHide: true, stdio: 'inherit' });
child.on('error', error => { console.error(`Cannot start engine MCP: ${error.message}. Run scripts/build-chess.ps1 first.`); process.exitCode = 1; });
child.on('exit', code => { process.exitCode = code ?? 1; });
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => child.kill());
