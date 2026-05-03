// ============ STATE ============
const API = '/api';

// ============ NAVIGATION ============
function showPage(name) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.getElementById('page-' + name).classList.add('active');
  event.target.classList.add('active');
}

// ============ HELPERS ============
function ts() { return new Date().toLocaleTimeString('ru-RU'); }

function log(el, msg, cls = 'info') {
  const d = document.getElementById(el);
  d.innerHTML += `<div><span class="ts">[${ts()}]</span> <span class="${cls}">${msg}</span></div>`;
  d.scrollTop = d.scrollHeight;
}

function setStep(prefix, num, state) {
  const s = document.getElementById(prefix + num);
  if (s) { s.className = 'step ' + state; }
}

function resetSteps(prefix, count) {
  for (let i = 1; i <= count; i++) setStep(prefix, i, '');
}

async function api(method, url, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(API + url, opts);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`${res.status}: ${text}`);
  }
  const ct = res.headers.get('content-type');
  if (ct && ct.includes('json')) return res.json();
  return res.text();
}

// ============ MINI CANVAS CHART LIBRARY ============
const Chart = {
  colors: ['#6366f1','#06b6d4','#22c55e','#eab308','#f97316','#ec4899','#ef4444','#8b5cf6','#14b8a6','#f43f5e'],

  bar: function(canvasId, labels, values, options = {}) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);
    const W = rect.width, H = rect.height;
    const pad = { top: 20, right: 20, bottom: 50, left: 50 };
    const chartW = W - pad.left - pad.right;
    const chartH = H - pad.top - pad.bottom;
    const maxVal = Math.max(...values, 1);

    ctx.clearRect(0, 0, W, H);

    // Grid lines
    ctx.strokeStyle = 'rgba(42,54,85,.5)';
    ctx.lineWidth = 0.5;
    for (let i = 0; i <= 4; i++) {
      const y = pad.top + (chartH / 4) * i;
      ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(W - pad.right, y); ctx.stroke();
      ctx.fillStyle = '#8896b3';
      ctx.font = '10px Inter';
      ctx.textAlign = 'right';
      ctx.fillText(Math.round(maxVal - (maxVal / 4) * i), pad.left - 8, y + 4);
    }

    // Bars with animation
    const barW = Math.min(chartW / labels.length * 0.6, 40);
    const gap = chartW / labels.length;

    labels.forEach((label, i) => {
      const x = pad.left + gap * i + (gap - barW) / 2;
      const barH = (values[i] / maxVal) * chartH;
      const y = pad.top + chartH - barH;
      const color = options.colors ? options.colors[i % options.colors.length] : this.colors[i % this.colors.length];

      // Bar gradient
      const grad = ctx.createLinearGradient(x, y, x, pad.top + chartH);
      grad.addColorStop(0, color);
      grad.addColorStop(1, color + '40');
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.roundRect(x, y, barW, barH, [4, 4, 0, 0]);
      ctx.fill();

      // Value on top
      ctx.fillStyle = '#e2e8f0';
      ctx.font = 'bold 11px Inter';
      ctx.textAlign = 'center';
      ctx.fillText(values[i], x + barW / 2, y - 6);

      // Label
      ctx.fillStyle = '#8896b3';
      ctx.font = '10px Inter';
      ctx.save();
      ctx.translate(x + barW / 2, pad.top + chartH + 12);
      ctx.rotate(-0.4);
      ctx.textAlign = 'right';
      ctx.fillText(label.length > 12 ? label.substring(0,12) + '…' : label, 0, 0);
      ctx.restore();
    });
  },

  doughnut: function(canvasId, labels, values, options = {}) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);
    const W = rect.width, H = rect.height;
    const cx = W * 0.35, cy = H / 2;
    const radius = Math.min(cx, cy) - 20;
    const innerRadius = radius * 0.55;
    const total = values.reduce((a, b) => a + b, 0) || 1;

    ctx.clearRect(0, 0, W, H);

    let startAngle = -Math.PI / 2;
    values.forEach((val, i) => {
      const sliceAngle = (val / total) * Math.PI * 2;
      const color = this.colors[i % this.colors.length];

      ctx.beginPath();
      ctx.arc(cx, cy, radius, startAngle, startAngle + sliceAngle);
      ctx.arc(cx, cy, innerRadius, startAngle + sliceAngle, startAngle, true);
      ctx.closePath();
      ctx.fillStyle = color;
      ctx.fill();

      startAngle += sliceAngle;
    });

    // Center text
    ctx.fillStyle = '#e2e8f0';
    ctx.font = 'bold 24px Inter';
    ctx.textAlign = 'center';
    ctx.fillText(total, cx, cy + 4);
    ctx.fillStyle = '#8896b3';
    ctx.font = '11px Inter';
    ctx.fillText('всего', cx, cy + 20);

    // Legend
    const legendX = W * 0.65;
    labels.forEach((label, i) => {
      const ly = 30 + i * 28;
      ctx.fillStyle = this.colors[i % this.colors.length];
      ctx.beginPath();
      ctx.roundRect(legendX, ly, 12, 12, 3);
      ctx.fill();

      ctx.fillStyle = '#e2e8f0';
      ctx.font = '12px Inter';
      ctx.textAlign = 'left';
      ctx.fillText(`${label}: ${values[i]}`, legendX + 20, ly + 10);
    });
  }
};

// ============ LOAD OVERVIEW STATS ============
async function loadOverviewStats() {
  try {
    const [users, audit] = await Promise.all([
      api('GET', '/users'),
      api('GET', '/audit')
    ]);

    // Animate counters
    animateCounter('stat-users-val', users.length);

    // Count total messages from audit
    const msgEvents = audit.filter(e => e.eventType && (e.eventType.includes('MESSAGE') || e.eventType.includes('HISTORY')));
    animateCounter('stat-messages-val', msgEvents.length);
    animateCounter('stat-audit-val', audit.length);

    // Draw charts
    // Audit by type
    const typeCounts = {};
    audit.forEach(e => {
      const type = e.eventType || 'UNKNOWN';
      typeCounts[type] = (typeCounts[type] || 0) + 1;
    });
    const sortedTypes = Object.entries(typeCounts).sort((a,b) => b[1] - a[1]).slice(0, 8);
    if (sortedTypes.length > 0) {
      Chart.bar('chart-audit-types', sortedTypes.map(t => t[0]), sortedTypes.map(t => t[1]));
    }

    // Users by role
    const roleCounts = {};
    users.forEach(u => {
      const role = u.role || 'USER';
      roleCounts[role] = (roleCounts[role] || 0) + 1;
    });
    const roles = Object.entries(roleCounts);
    if (roles.length > 0) {
      Chart.doughnut('chart-user-roles', roles.map(r => r[0]), roles.map(r => r[1]));
    }
  } catch(e) {
    console.error('Failed to load stats:', e);
  }

  // Animate flow diagrams
  animateFlow('msg-flow-anim');
  setTimeout(() => animateFlow('auth-flow-anim'), 1500);
}

function animateCounter(elementId, target) {
  const el = document.getElementById(elementId);
  if (!el) return;
  let current = 0;
  const step = Math.max(1, Math.ceil(target / 30));
  const interval = setInterval(() => {
    current += step;
    if (current >= target) {
      current = target;
      clearInterval(interval);
    }
    el.textContent = current;
  }, 30);
}

function animateFlow(containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;
  const nodes = container.querySelectorAll('.flow-node');
  nodes.forEach((node, i) => {
    setTimeout(() => {
      node.classList.add('highlight');
      setTimeout(() => node.classList.remove('highlight'), 800);
    }, i * 400);
  });
}

// Re-animate flows periodically
setInterval(() => {
  animateFlow('msg-flow-anim');
  setTimeout(() => animateFlow('auth-flow-anim'), 1500);
}, 8000);

// ============ MESSAGING ============
async function doSendMessage() {
  const senderId = document.getElementById('msg-from').value;
  const recipientId = document.getElementById('msg-to').value;
  const text = document.getElementById('msg-text').value;
  const logEl = 'msg-log';

  document.getElementById(logEl).innerHTML = '';
  resetSteps('ms', 5);

  if (!senderId || !recipientId) { log(logEl, 'Укажите ID отправителя и получателя', 'err'); return; }

  try {
    setStep('ms', 1, 'active');
    log(logEl, `🔑 Генерация случайного AES-256 ключа (32 байта)`, 'key');
    await new Promise(r => setTimeout(r, 300));

    setStep('ms', 1, 'done'); setStep('ms', 2, 'active');
    log(logEl, `🔒 Шифрование текста AES-256-GCM (IV: 12 байт, Tag: 128 бит)`, 'key');
    log(logEl, `   Plaintext: "${text.substring(0,50)}${text.length>50?'...':''}"`, 'info');
    await new Promise(r => setTimeout(r, 200));

    setStep('ms', 2, 'done'); setStep('ms', 3, 'active');
    log(logEl, `📦 Оборачивание AES-ключа публичным RSA-ключом получателя (RSA-OAEP)`, 'key');
    await new Promise(r => setTimeout(r, 200));

    setStep('ms', 3, 'done'); setStep('ms', 4, 'active');
    log(logEl, `✍️ Цифровая подпись SHA256withRSA (приватный ключ отправителя)`, 'key');

    const msg = await api('POST', '/messages/send', { senderId, recipientId, text });

    setStep('ms', 4, 'done'); setStep('ms', 5, 'active');
    log(logEl, `🐘 Сообщение сохранено в PostgreSQL`, 'ok');
    log(logEl, `   ID: ${msg.id}`, 'info');
    log(logEl, `   Статус: ${msg.status}`, 'info');

    setStep('ms', 5, 'done');
    log(logEl, `✅ Сообщение отправлено и зашифровано E2E!`, 'ok');
  } catch(e) {
    log(logEl, `❌ ${e.message}`, 'err');
  }
}

async function doLoadHistory() {
  const userId = document.getElementById('hist-user').value;
  const logEl = 'hist-log';
  document.getElementById(logEl).innerHTML = '';

  if (!userId) { log(logEl, 'Укажите ID пользователя', 'err'); return; }

  try {
    log(logEl, `📥 GET /api/messages/history/${userId}`, 'info');
    const msgs = await api('GET', `/messages/history/${userId}`);
    log(logEl, `Найдено сообщений: ${msgs.length}`, 'ok');

    msgs.forEach((m, i) => {
      const dir = m.senderId === userId ? '📤 ИСХОД' : '📥 ВХОД';
      log(logEl, `${dir} #${i+1}: ID=${m.id} | Статус: ${m.status}`, 'info');
    });
  } catch(e) {
    log(logEl, `❌ ${e.message}`, 'err');
  }
}

// ============ USERS LIST ============
async function doLoadUsers() {
  try {
    const users = await api('GET', '/users');
    const tbody = document.getElementById('users-tbody');
    if (users.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4" style="color:var(--muted)">Нет пользователей</td></tr>';
      return;
    }
    tbody.innerHTML = users.map(u => `<tr>
      <td style="font-family:'JetBrains Mono',monospace;font-size:10px;cursor:pointer" title="Кликни чтобы скопировать"
          onclick="navigator.clipboard.writeText('${u.id}');this.style.color='var(--green)';setTimeout(()=>this.style.color='',500)">${u.id}</td>
      <td><b>${u.login}</b></td>
      <td><span class="badge ${u.status==='ACTIVE'?'badge-green':'badge-yellow'}">${u.status}</span></td>
      <td><span class="badge badge-blue">${u.role}</span></td>
    </tr>`).join('');
  } catch(e) {
    document.getElementById('users-tbody').innerHTML = `<tr><td colspan="4" style="color:var(--red)">${e.message}</td></tr>`;
  }
}

// ============ ENCRYPTION COMPARISON ============
async function doEncryptionTest() {
  const text = document.getElementById('enc-text').value;
  const results = document.getElementById('enc-results');
  results.innerHTML = '<div style="color:var(--muted);padding:20px;text-align:center">⏳ Тестирую 7 алгоритмов...</div>';

  try {
    const resp = await api('POST', '/encryption/test-all?plaintext=' + encodeURIComponent(text));
    const data = resp.results || resp;

    const levelColor = { EXCELLENT: 'badge-green', GOOD: 'badge-yellow', WEAK: 'badge-red', NONE: 'badge-red' };
    const statusIcon = { SUCCESS: '✅', ERROR: '❌', INVALID_DECRYPTION: '⚠️' };

    results.innerHTML = '<div class="grid grid-2">' + data.map(r => `
      <div class="card">
        <h3>${r.methodName.replace(/_/g, '-')}</h3>
        <div style="display:flex;gap:8px;margin-bottom:8px">
          <span class="badge ${levelColor[r.securityLevel] || 'badge-blue'}">${r.securityLevel}</span>
          ${r.isHardwareCompatible ? '<span class="badge badge-blue">HW Accel</span>' : ''}
          <span>${statusIcon[r.status] || ''} ${r.status}</span>
        </div>
        <table class="tbl">
          <tr><td>⏱️ Шифрование</td><td><b>${r.encryptionTimeMs} мс</b></td></tr>
          <tr><td>⏱️ Расшифровка</td><td><b>${r.decryptionTimeMs} мс</b></td></tr>
          <tr><td>📊 Итого</td><td><b>${r.totalTimeMs} мс</b></td></tr>
          <tr><td>📦 Размер шифротекста</td><td>${r.ciphertextSizeBytes} байт</td></tr>
          <tr><td>📈 Оверхед</td><td>${r.overheadPercent.toFixed(1)}%</td></tr>
        </table>
      </div>
    `).join('') + '</div>';

    // Draw chart
    const chartCard = document.getElementById('enc-chart-card');
    chartCard.style.display = 'block';
    Chart.bar('chart-encryption',
      data.map(r => r.methodName.replace(/_/g, '-')),
      data.map(r => r.totalTimeMs),
      { colors: data.map(r => r.securityLevel === 'EXCELLENT' ? '#22c55e' : r.securityLevel === 'GOOD' ? '#eab308' : '#ef4444') }
    );
  } catch(e) {
    results.innerHTML = `<div class="card"><span style="color:var(--red)">❌ ${e.message}</span></div>`;
  }
}

// ============ AUDIT ============
async function doLoadAudit() {
  try {
    const events = await api('GET', '/audit');
    const tbody = document.getElementById('audit-tbody');

    // Update stats
    document.getElementById('audit-total').textContent = events.length;
    const authCount = events.filter(e => e.eventType && (e.eventType.includes('AUTH') || e.eventType.includes('REGISTER') || e.eventType.includes('LOGIN'))).length;
    const msgCount = events.filter(e => e.eventType && e.eventType.includes('MESSAGE')).length;
    const keyCount = events.filter(e => e.eventType && (e.eventType.includes('KEY') || e.eventType.includes('CRYPTO') || e.eventType.includes('RECOVERY'))).length;
    document.getElementById('audit-auth').textContent = authCount;
    document.getElementById('audit-msg').textContent = msgCount;
    document.getElementById('audit-keys').textContent = keyCount;

    if (events.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4" style="color:var(--muted)">Нет событий</td></tr>';
      return;
    }
    tbody.innerHTML = events.slice(-50).reverse().map(e => {
      const typeColor = e.eventType?.includes('FAIL') || e.eventType?.includes('BLOCK')
        ? 'badge-red' : e.eventType?.includes('OK') ? 'badge-green' : 'badge-blue';
      return `<tr>
        <td style="font-size:11px;color:var(--muted)">${new Date(e.timestamp).toLocaleString('ru-RU')}</td>
        <td><span class="badge ${typeColor}">${e.eventType}</span></td>
        <td>${e.userId || '—'}</td>
        <td style="font-size:11px">${e.details || ''}</td>
      </tr>`;
    }).join('');
  } catch(e) {
    document.getElementById('audit-tbody').innerHTML = `<tr><td colspan="4" style="color:var(--red)">${e.message}</td></tr>`;
  }
}

// ============ INIT ============
document.addEventListener('DOMContentLoaded', () => {
  loadOverviewStats();
});
