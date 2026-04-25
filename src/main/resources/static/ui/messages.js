const M = window.DemoUI;

function explain(text) {
  M.setExplain("messages-explain", text);
}

function setMsgOut(data) {
  M.setOut("messages-out", data);
}

function resetSteps() {
  ["m-step-send", "m-step-deliver", "m-step-read", "m-step-error"].forEach((id) => M.markStep(id, "step-wait"));
}

function ensureUserSelects() {
  M.fillManyUserSelects(["sender-select", "recipient-select", "history-user-select", "reader-select"]);
}

M.$("btn-send").addEventListener("click", async () => {
  try {
    ensureUserSelects();
    resetSteps();
    M.markStep("m-step-send", "step-run");

    const senderId = M.$("sender-select").value;
    const recipientId = M.$("recipient-select").value;
    const text = M.$("msg-text").value;
    if (!senderId || !recipientId || !text) {
      throw new Error("Для отправки заполни отправителя, получателя и текст.");
    }

    const message = await M.api("/api/messages/send", "POST", {
      senderId,
      recipientId,
      text,
    });

    M.$("message-id").value = message.id;
    M.markStep("m-step-send", "step-ok");
    explain("send: сообщение зашифровано, подписано и сохранено со статусом SENT/QUEUED.");
    setMsgOut({ info: "sent", message: M.compactMessage(message) });
  } catch (e) {
    setMsgOut(e.message);
    M.markStep("m-step-send", "step-wait");
  }
});

M.$("btn-deliver").addEventListener("click", async () => {
  try {
    const id = M.$("message-id").value.trim();
    if (!id) {
      throw new Error("Укажи messageId для установки статуса DELIVERED.");
    }
    M.markStep("m-step-deliver", "step-run");
    await M.api(`/api/messages/${id}/deliver`, "POST");
    M.markStep("m-step-deliver", "step-ok");
    explain("deliver: статус доставки обновлен на DELIVERED.");
    setMsgOut({ info: "delivered", messageId: id });
  } catch (e) {
    setMsgOut(e.message);
    M.markStep("m-step-deliver", "step-wait");
  }
});

M.$("btn-read").addEventListener("click", async () => {
  try {
    const id = M.$("message-id").value.trim();
    const readerId = M.$("reader-select").value;
    if (!id || !readerId) {
      throw new Error("Для READ укажи messageId и читателя.");
    }
    M.markStep("m-step-read", "step-run");
    await M.api(`/api/messages/${id}/read?readerId=${readerId}`, "POST");
    M.markStep("m-step-read", "step-ok");
    explain("read: получатель прочитал сообщение, статус READ.");
    setMsgOut({ info: "read", messageId: id, readerId });
  } catch (e) {
    setMsgOut(e.message);
    M.markStep("m-step-read", "step-wait");
  }
});

M.$("btn-error").addEventListener("click", async () => {
  try {
    const id = M.$("message-id").value.trim();
    if (!id) {
      throw new Error("Укажи messageId для установки статуса ERROR.");
    }
    M.markStep("m-step-error", "step-run");
    await M.api(`/api/messages/${id}/error`, "POST", { reason: "demo-error" });
    M.markStep("m-step-error", "step-ok");
    explain("error: сообщение помечено как ERROR для сценария отказа/инцидента.");
    setMsgOut({ info: "error", messageId: id });
  } catch (e) {
    setMsgOut(e.message);
    M.markStep("m-step-error", "step-wait");
  }
});

M.$("btn-history").addEventListener("click", async () => {
  try {
    const userId = M.$("history-user-select").value;
    if (!userId) {
      throw new Error("Выбери пользователя для просмотра истории.");
    }
    const history = await M.api(`/api/messages/history/${userId}`);
    explain("history: синхронизация хранилища сообщений для выбранного пользователя.");
    setMsgOut({ info: "history", userId, messages: M.compactMessages(history) });
  } catch (e) {
    setMsgOut(e.message);
  }
});

M.$("btn-offline").addEventListener("click", async () => {
  try {
    const userId = M.$("history-user-select").value;
    if (!userId) {
      throw new Error("Выбери пользователя для считывания офлайн-очереди.");
    }
    const offline = await M.api(`/api/messages/offline/${userId}`);
    explain("offline pull: выдача накопленной очереди сообщений для офлайн-получателя.");
    setMsgOut({ info: "offline", userId, messages: M.compactMessages(offline) });
  } catch (e) {
    setMsgOut(e.message);
  }
});

ensureUserSelects();
setMsgOut("Сценарий: отправка -> статус -> история/офлайн. Если список пуст, сначала создай пользователей.");


