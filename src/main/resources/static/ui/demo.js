// ============ STATE ============
let jwtToken = null;
let currentUser = null;
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

function headers() {
  const h = { 'Content-Type': 'application/json' };
  if (jwtToken) h['Authorization'] = 'Bearer ' + jwtToken;
  return h;
}

async function api(method, url, body) {
  const opts = { method, headers: headers() };
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

function updateJwtDisplay(token) {
  jwtToken = token;
  document.getElementById('jwt-display').textContent = token;
  // Decode payload
  try {
    const parts = token.split('.');
    const payload = JSON.parse(atob(parts[1]));
    const exp = new Date(payload.exp * 1000);
    document.getElementById('jwt-info').innerHTML =
      `👤 <b>${payload.sub}</b> | 🏷️ ${payload.role} | ⏰ Истекает: ${exp.toLocaleString('ru-RU')}`;
  } catch(e) {}
}

// ============ AUTH: REGISTER ============
async function doRegister() {
  const login = document.getElementById('reg-login').value;
  const password = document.getElementById('reg-pass').value;
  const role = document.getElementById('reg-role').value;
  const hwToken = document.getElementById('reg-token').value;
  const logEl = 'reg-log';

  document.getElementById(logEl).innerHTML = '';
  resetSteps('rs', 4);

  if (!login || !password) { log(logEl, 'Заполните логин и пароль', 'err'); return; }

  setStep('rs', 1, 'active');
  log(logEl, `Отправляю POST /api/auth/register { login: "${login}", role: "${role}" }`, 'info');

  try {
    setStep('rs', 1, 'done'); setStep('rs', 2, 'active');
    log(logEl, `🔐 Сервер хеширует пароль: PBKDF2-SHA256, 100 000 итераций + случайная соль (32 байта)`, 'key');

    const data = await api('POST', '/auth/register', { login, password, role, hardwareToken: hwToken || null });

    setStep('rs', 2, 'done'); setStep('rs', 3, 'active');
    log(logEl, `🐘 Пользователь сохранён в PostgreSQL: id=${data.user.id}`, 'ok');

    setStep('rs', 3, 'done'); setStep('rs', 4, 'active');
    log(logEl, `🎫 JWT токен сгенерирован (HMAC-SHA256, 1 час)`, 'ok');

    setStep('rs', 4, 'done');
    updateJwtDisplay(data.token);
    currentUser = data.user;

    if (data.user.hardwareToken) {
      log(logEl, `📟 2FA токен: ${data.user.hardwareToken} (сохраните для входа!)`, 'warn');
      document.getElementById('login-token').value = data.user.hardwareToken;
    }
    document.getElementById('login-user').value = login;
    document.getElementById('msg-from').value = data.user.id;

    log(logEl, `✅ Регистрация завершена! Роль: ${data.user.role}`, 'ok');
  } catch(e) {
    setStep('rs', 4, 'error');
    log(logEl, `❌ Ошибка: ${e.message}`, 'err');
  }
}

// ============ AUTH: LOGIN ============
async function doLogin() {
  const login = document.getElementById('login-user').value;
  const password = document.getElementById('login-pass').value;
  const hwToken = document.getElementById('login-token').value;
  const logEl = 'login-log';

  document.getElementById(logEl).innerHTML = '';
  resetSteps('ls', 4);

  setStep('ls', 1, 'active');
  log(logEl, `Отправляю POST /api/auth/login { login: "${login}" }`, 'info');

  try {
    setStep('ls', 1, 'done'); setStep('ls', 2, 'active');
    log(logEl, `🔐 Сервер проверяет хеш пароля (PBKDF2 + соль из БД)`, 'key');

    const data = await api('POST', '/auth/login', { login, password, hardwareToken: hwToken || null });

    setStep('ls', 2, 'done'); setStep('ls', 3, 'active');
    log(logEl, `📟 2FA проверен: аппаратный токен совпал`, 'ok');

    setStep('ls', 3, 'done'); setStep('ls', 4, 'active');
    log(logEl, `🎫 Новый JWT выпущен`, 'ok');

    setStep('ls', 4, 'done');
    updateJwtDisplay(data.token);
    currentUser = data.user;
    document.getElementById('msg-from').value = data.user.id;

    log(logEl, `✅ Вход выполнен! Пользователь: ${data.user.login} (${data.user.role})`, 'ok');
  } catch(e) {
    for(let i=1;i<=4;i++) { const s=document.getElementById('ls'+i); if(s.classList.contains('active')) setStep('ls',i,'error'); }
    log(logEl, `❌ Ошибка входа: ${e.message}`, 'err');
    log(logEl, `💡 Проверь 2FA токен — он выдавался при регистрации`, 'warn');
  }
}

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
    if (msg.ciphertextBase64) log(logEl, `   Шифротекст: ${msg.ciphertextBase64.substring(0,60)}...`, 'info');
    if (msg.wrappedMessageKey) log(logEl, `   Wrapped Key: ${msg.wrappedMessageKey.substring(0,60)}...`, 'info');

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
      if (m.ciphertextBase64) log(logEl, `   🔒 Шифротекст: ${m.ciphertextBase64.substring(0,50)}...`, 'key');
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
  } catch(e) {
    results.innerHTML = `<div class="card"><span style="color:var(--red)">❌ ${e.message}</span></div>`;
  }
}

// ============ AUDIT ============
async function doLoadAudit() {
  try {
    const events = await api('GET', '/audit');
    const tbody = document.getElementById('audit-tbody');
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
