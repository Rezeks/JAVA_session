const U = window.DemoUI;

function setUsersState(step, explain, payload) {
  U.markStep(step, "step-ok");
  U.setExplain("users-explain", explain);
  U.setOut("users-out", payload);
}

U.$("btn-register").addEventListener("click", async () => {
  try {
    const login = U.$("reg-login").value.trim();
    const password = U.$("reg-pass").value;
    if (!login || !password) {
      throw new Error("Для регистрации заполни логин и пароль.");
    }

    U.markStep("u-step-reg", "step-run");
    const user = await U.api("/api/users/register", "POST", {
      login,
      password,
      role: U.$("reg-role").value,
      hardwareToken: U.$("reg-token").value.trim() || null,
    });
    U.upsertUser(user);

    U.$("confirm-login").value = user.login;
    U.$("auth-login").value = user.login;
    U.$("auth-token").value = user.hardwareTokenSecret || "";
    U.$("role-login").value = user.login;
    U.$("ops-login").value = user.login;

    setUsersState(
      "u-step-reg",
      "register: пользователь создан, ключи и токен готовы. Теперь можно подтвердить аккаунт.",
      user
    );
  } catch (e) {
    U.setOut("users-out", e.message);
    U.markStep("u-step-reg", "step-wait");
  }
});

U.$("btn-confirm").addEventListener("click", async () => {
  try {
    U.markStep("u-step-confirm", "step-run");
    const login = U.$("confirm-login").value.trim();
    if (!login) {
      throw new Error("Укажи логин для подтверждения аккаунта.");
    }
    const user = await U.api(`/api/users/${encodeURIComponent(login)}/confirm`, "POST");
    U.upsertUser(user);
    setUsersState(
      "u-step-confirm",
      "confirm: статус аккаунта становится активным, можно проходить 2FA.",
      user
    );
  } catch (e) {
    U.setOut("users-out", e.message);
    U.markStep("u-step-confirm", "step-wait");
  }
});

U.$("btn-auth").addEventListener("click", async () => {
  try {
    U.markStep("u-step-auth", "step-run");
    const login = U.$("auth-login").value.trim();
    const password = U.$("auth-pass").value;
    const token = U.$("auth-token").value.trim();
    if (!login || !password || !token) {
      throw new Error("Для 2FA заполни логин, пароль и hardware token.");
    }
    const ok = await U.api("/api/users/auth", "POST", {
      login,
      password,
      hardwareToken: token,
    });
    setUsersState(
      "u-step-auth",
      "auth: сервер проверяет пароль и аппаратный токен (2FA).",
      { login, ok }
    );
  } catch (e) {
    U.setOut("users-out", e.message);
    U.markStep("u-step-auth", "step-wait");
  }
});

U.$("btn-role").addEventListener("click", async () => {
  try {
    U.markStep("u-step-admin", "step-run");
    const login = U.$("role-login").value.trim();
    if (!login) {
      throw new Error("Укажи логин, для которого меняется роль.");
    }
    const user = await U.api(`/api/users/${encodeURIComponent(login)}/role`, "POST", {
      role: U.$("new-role").value,
    });
    U.upsertUser(user);
    setUsersState(
      "u-step-admin",
      "assign role: демонстрация RBAC. Роль пользователя обновлена.",
      user
    );
  } catch (e) {
    U.setOut("users-out", e.message);
    U.markStep("u-step-admin", "step-wait");
  }
});

U.$("btn-rotate-token").addEventListener("click", async () => {
  try {
    U.markStep("u-step-admin", "step-run");
    const login = U.$("ops-login").value.trim();
    if (!login) {
      throw new Error("Укажи логин для ротации токена.");
    }
    const user = await U.api(`/api/users/${encodeURIComponent(login)}/token/rotate`, "POST");
    U.upsertUser(user);
    U.$("auth-token").value = user.hardwareTokenSecret || "";
    setUsersState(
      "u-step-admin",
      "rotate token: новый аппаратный токен выдан, для следующего auth используй его.",
      user
    );
  } catch (e) {
    U.setOut("users-out", e.message);
    U.markStep("u-step-admin", "step-wait");
  }
});

U.$("btn-block").addEventListener("click", async () => {
  try {
    U.markStep("u-step-admin", "step-run");
    const login = U.$("ops-login").value.trim();
    if (!login) {
      throw new Error("Укажи логин для блокировки.");
    }
    const user = await U.api(`/api/users/${encodeURIComponent(login)}/block`, "POST", {
      reason: "demo-manual-block",
    });
    U.upsertUser(user);
    setUsersState(
      "u-step-admin",
      "block: пользователь принудительно заблокирован (админ сценарий).",
      user
    );
  } catch (e) {
    U.setOut("users-out", e.message);
    U.markStep("u-step-admin", "step-wait");
  }
});

U.$("btn-recover").addEventListener("click", async () => {
  try {
    U.markStep("u-step-admin", "step-run");
    const login = U.$("ops-login").value.trim();
    if (!login) {
      throw new Error("Укажи логин для восстановления.");
    }
    const user = await U.api(`/api/users/${encodeURIComponent(login)}/recover`, "POST");
    U.upsertUser(user);
    setUsersState(
      "u-step-admin",
      "recover: восстановление после компрометации и ротация ключевого контекста.",
      user
    );
  } catch (e) {
    U.setOut("users-out", e.message);
    U.markStep("u-step-admin", "step-wait");
  }
});

U.setOut("users-out", "Начни со сценария A: регистрация -> подтверждение -> 2FA.");


