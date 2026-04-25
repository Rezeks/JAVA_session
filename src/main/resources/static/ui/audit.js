const A = window.DemoUI;

function fillSelect() {
  A.fillUserSelect("key-user-select");
}

function explain(text) {
  A.setExplain("audit-explain", text);
}

A.$("btn-export-pub").addEventListener("click", async () => {
  try {
    const ownerId = A.$("key-user-select").value;
    if (!ownerId) {
      throw new Error("Выбери пользователя для экспорта public key.");
    }
    const key = await A.api(`/api/keys/${ownerId}/public`);
    A.$("import-owner-id").value = ownerId;
    A.$("import-public").value = key;
    A.setOut("keys-out", { ownerId, publicKey: key });
    explain("export public: безопасный обмен публичным ключом для верификации подписи и шифрования.");
  } catch (e) {
    A.setOut("keys-out", e.message);
  }
});

A.$("btn-export-priv").addEventListener("click", async () => {
  try {
    const ownerId = A.$("key-user-select").value;
    if (!ownerId) {
      throw new Error("Выбери пользователя для экспорта private key.");
    }
    const key = await A.api(`/api/keys/${ownerId}/private`);
    A.$("import-owner-id").value = ownerId;
    A.$("import-private").value = key;
    A.setOut("keys-out", { ownerId, privateKey: key });
    explain("export private: доступно только в demo-режиме для наглядности операции import/export.");
  } catch (e) {
    A.setOut("keys-out", e.message);
  }
});

A.$("btn-import-keys").addEventListener("click", async () => {
  try {
    const ownerId = A.$("import-owner-id").value.trim();
    const publicKey = A.$("import-public").value;
    const privateKey = A.$("import-private").value;
    if (!ownerId || !publicKey || !privateKey) {
      throw new Error("Для импорта заполни ownerId, public key и private key.");
    }

    await A.api(`/api/keys/${ownerId}/import`, "POST", {
      publicKey,
      privateKey,
    });
    A.setOut("keys-out", { ownerId, imported: true });
    explain("import keys: ключевая пара записана в локальный KeyVault-адаптер.");
  } catch (e) {
    A.setOut("keys-out", e.message);
  }
});

A.$("btn-audit").addEventListener("click", async () => {
  try {
    const events = await A.api("/api/audit");
    A.setOut("audit-out", events);
    explain("audit: здесь виден журнал действий по пользователям, сообщениям, файлам и ключам.");
  } catch (e) {
    A.setOut("audit-out", e.message);
  }
});

fillSelect();
A.setOut("keys-out", "Сценарий: экспортируй ключи -> при необходимости отредактируй -> импортируй обратно.");


