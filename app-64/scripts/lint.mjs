import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';

function run(command, args) {
  const result = spawnSync(command, args, { stdio: 'inherit', shell: process.platform === 'win32' });
  if (result.error) {
    console.error(result.error.message);
    process.exit(1);
  }
  if (result.status !== 0) process.exit(result.status ?? 1);
}

if (existsSync('tsconfig.check.json')) {
  run('npx', ['tsgo', '-p', 'tsconfig.check.json']);
}
run('npx', ['biome', 'lint']);
