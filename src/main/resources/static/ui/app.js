const STORAGE_KEY = "securemsg.users.v1";

function $(id) {
  return document.getElementById(id);
}

function setOut(id, data) {
  const text = typeof data === "string" ? data : JSON.stringify(data, null, 2);
  $(id).textContent = text;
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
    const error = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
    throw new Error(`HTTP ${response.status}: ${error}`);
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
  const existingIdx = users.findIndex((x) => x.login === user.login);
  const payload = {
    login: user.login,
    id: user.id,
    token: user.hardwareTokenSecret || "",
  };
  if (existingIdx >= 0) {
    users[existingIdx] = payload;
  } else {
    users.push(payload);
  }
  saveUsers(users);
  return users;
}

function renderUsers() {
  const users = loadUsers();
  const options = users
    .map((u) => `<option value="${u.id}">${u.login} (${u.id.slice(0, 8)})</option>`)
    .join("");

  [
    "sender-select",
    "recipient-select",
    "history-user-select",
    "group-owner-select",
    "group-sender-select",
    "file-sender-select",
    "file-recipient-select",
  ].forEach((id) => {
    const el = $(id);
    el.innerHTML = options;
  });

  setOut("users-out", users.length ? users : "No local users yet. Register users first.");
}

function compactMessages(messages) {
  if (!Array.isArray(messages)) {
    return messages;
  }
  return messages.map((m) => ({
    id: m.id,
    senderId: m.senderId,
    recipientId: m.recipientId,
    groupId: m.groupId,
    status: m.status,
    ratchetStep: m.ratchetStep,
    createdAt: m.createdAt,
    payloadSize: m.encryptedPayload ? m.encryptedPayload.length : 0,
    signatureSize: m.signature ? m.signature.length : 0,
  }));
}

function getLoginById(id) {
  const user = loadUsers().find((u) => u.id === id);
  return user ? user.login : id;
}

function getIdByLogin(login) {
  const user = loadUsers().find((u) => u.login === login.trim());
  return user ? user.id : null;
}

$("btn-register").addEventListener("click", async () => {
  try {
    const payload = {
      login: $("reg-login").value.trim(),
      password: $("reg-pass").value,
      role: $("reg-role").value,
      hardwareToken: $("reg-token").value.trim() || null,
    };
    const user = await api("/api/users/register", "POST", payload);
    upsertUser(user);
    renderUsers();
    setOut("users-out", {
      info: "Registered",
      login: user.login,
      id: user.id,
      role: user.role,
      hardwareTokenSecret: user.hardwareTokenSecret,
      status: user.status,
    });
    $("confirm-login").value = user.login;
    $("auth-login").value = user.login;
    $("auth-token").value = user.hardwareTokenSecret || "";
  } catch (e) {
    setOut("users-out", e.message);
  }
});

$("btn-confirm").addEventListener("click", async () => {
  try {
    const login = $("confirm-login").value.trim();
    const user = await api(`/api/users/${encodeURIComponent(login)}/confirm`, "POST");
    upsertUser(user);
    renderUsers();
    setOut("users-out", { info: "Confirmed", login: user.login, status: user.status });
  } catch (e) {
    setOut("users-out", e.message);
  }
});

$("btn-auth").addEventListener("click", async () => {
  try {
    const login = $("auth-login").value.trim();
    const ok = await api("/api/users/auth", "POST", {
      login,
      password: $("auth-pass").value,
      hardwareToken: $("auth-token").value.trim(),
    });
    setOut("users-out", { info: "Auth result", login, ok });
  } catch (e) {
    setOut("users-out", e.message);
  }
});

$("btn-send-msg").addEventListener("click", async () => {
  try {
    const message = await api("/api/messages/send", "POST", {
      senderId: $("sender-select").value,
      recipientId: $("recipient-select").value,
      text: $("msg-text").value,
    });
    setOut("messages-out", { info: "Message sent", message: compactMessages([message])[0] });
  } catch (e) {
    setOut("messages-out", e.message);
  }
});

$("btn-history").addEventListener("click", async () => {
  try {
    const userId = $("history-user-select").value;
    const history = await api(`/api/messages/history/${userId}`);
    setOut("messages-out", {
      info: `History for ${getLoginById(userId)}`,
      messages: compactMessages(history),
    });
  } catch (e) {
    setOut("messages-out", e.message);
  }
});

$("btn-offline").addEventListener("click", async () => {
  try {
    const userId = $("history-user-select").value;
    const offline = await api(`/api/messages/offline/${userId}`);
    setOut("messages-out", {
      info: `Offline pull for ${getLoginById(userId)}`,
      messages: compactMessages(offline),
    });
  } catch (e) {
    setOut("messages-out", e.message);
  }
});

$("btn-create-group").addEventListener("click", async () => {
  try {
    const ownerId = $("group-owner-select").value;
    const requestedLogins = $("group-members").value
      .split(",")
      .map((x) => x.trim())
      .filter(Boolean);

    const memberIds = requestedLogins
      .map((login) => getIdByLogin(login))
      .filter((x) => x !== null);

    if (!memberIds.includes(ownerId)) {
      memberIds.push(ownerId);
    }

    const group = await api("/api/messages/group", "POST", {
      ownerId,
      name: $("group-name").value.trim(),
      members: memberIds,
    });
    $("group-id").value = group.id;
    setOut("groups-out", { info: "Group created", group });
  } catch (e) {
    setOut("groups-out", e.message);
  }
});

$("btn-send-group").addEventListener("click", async () => {
  try {
    const groupId = $("group-id").value.trim();
    const messages = await api(`/api/messages/group/${groupId}/send`, "POST", {
      senderId: $("group-sender-select").value,
      text: $("group-text").value,
    });
    setOut("groups-out", { info: "Group message sent", messages: compactMessages(messages) });
  } catch (e) {
    setOut("groups-out", e.message);
  }
});

$("btn-upload-file").addEventListener("click", async () => {
  try {
    const senderId = $("file-sender-select").value;
    const recipientId = $("file-recipient-select").value;
    const file = $("file-input").files[0];
    if (!file) {
      throw new Error("Select a file first");
    }

    const init = await api("/api/files/init", "POST", {
      senderId,
      recipientId,
      fileName: file.name,
      totalSize: file.size,
    });

    const chunk = await api(
      `/api/files/${init.id}/chunk?offset=0`,
      "POST",
      await file.arrayBuffer(),
      { "Content-Type": "application/octet-stream" }
    );

    const finalized = await api(`/api/files/${init.id}/finalize`, "POST");
    const verified = await api(`/api/files/${init.id}/verify?checksum=${finalized.checksumSha256}`);
    await api(`/api/files/${init.id}/delivered`, "POST");

    setOut("files-out", {
      info: "File uploaded in single-chunk demo",
      init,
      chunk,
      finalized,
      verified,
    });
  } catch (e) {
    setOut("files-out", e.message);
  }
});

$("btn-audit").addEventListener("click", async () => {
  try {
    const events = await api("/api/audit");
    setOut("audit-out", events);
  } catch (e) {
    setOut("audit-out", e.message);
  }
});

renderUsers();

