const STORAGE_KEY = "securemsg.users.v1";

function $(id) {
  return document.getElementById(id);
}

function setOut(id, data) {
  const el = $(id);
  if (!el) {
    return;
  }
  const text = typeof data === "string" ? data : JSON.stringify(data, null, 2);
  el.textContent = text;
}

function setExplain(id, text) {
  const el = $(id);
  if (el) {
    el.textContent = text;
  }
}

async function api(path, method = "GET", body, headers = {}) {
  const options = { method, headers: { ...headers } };
  if (body !== undefined) {
    if (body instanceof ArrayBuffer || body instanceof Blob) {
      options.body = body;
    } else {
      options.headers["Content-Type"] = "application/json";
      options.body = JSON.stringify(body);
    }
  }

  const response = await fetch(path, options);
  const text = await response.text();
  const payload = text ? tryParseJson(text) : null;
  if (!response.ok) {
    const errorText = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
    throw new Error(`HTTP ${response.status}: ${errorText}`);
  }
  return payload;
}

function tryParseJson(text) {
  try {
    return JSON.parse(text);
  } catch (_) {
    return text;
  }
}

function loadUsers() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (_) {
    return [];
  }
}

function saveUsers(users) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(users));
}

function upsertUser(user) {
  const users = loadUsers();
  const payload = {
    login: user.login,
    id: user.id,
    token: user.hardwareTokenSecret || "",
    role: user.role || "USER",
  };

  const idx = users.findIndex((u) => u.login === user.login);
  if (idx >= 0) {
    users[idx] = { ...users[idx], ...payload };
  } else {
    users.push(payload);
  }
  saveUsers(users);
  return users;
}

function getUserByLogin(login) {
  return loadUsers().find((u) => u.login === login.trim()) || null;
}

function getUserById(id) {
  return loadUsers().find((u) => u.id === id) || null;
}

function getIdByLogin(login) {
  const user = getUserByLogin(login);
  return user ? user.id : null;
}

function fillUserSelect(selectId) {
  const el = $(selectId);
  if (!el) {
    return;
  }
  const users = loadUsers();
  el.innerHTML = users
    .map((u) => `<option value="${u.id}">${u.login} (${u.id.slice(0, 8)})</option>`)
    .join("");
}

function fillManyUserSelects(selectIds) {
  selectIds.forEach((id) => fillUserSelect(id));
}

function compactMessage(message) {
  if (!message) {
    return message;
  }
  return {
    id: message.id,
    senderId: message.senderId,
    recipientId: message.recipientId,
    groupId: message.groupId,
    status: message.status,
    ratchetStep: message.ratchetStep,
    createdAt: message.createdAt,
    payloadSize: message.encryptedPayload ? message.encryptedPayload.length : 0,
    signatureSize: message.signature ? message.signature.length : 0,
  };
}

function compactMessages(messages) {
  if (!Array.isArray(messages)) {
    return messages;
  }
  return messages.map(compactMessage);
}

function markStep(stepId, state) {
  const el = $(stepId);
  if (!el) {
    return;
  }
  el.classList.remove("step-ok", "step-run", "step-wait");
  el.classList.add(state);
}

window.DemoUI = {
  $, setOut, setExplain, api,
  loadUsers, saveUsers, upsertUser, getUserByLogin, getUserById, getIdByLogin,
  fillUserSelect, fillManyUserSelects,
  compactMessage, compactMessages, markStep,
};

