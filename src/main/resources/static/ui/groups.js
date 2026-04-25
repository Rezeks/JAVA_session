const G = window.DemoUI;

function explain(text) {
  G.setExplain("groups-explain", text);
}

function setOut(data) {
  G.setOut("groups-out", data);
}

function fillSelects() {
  G.fillManyUserSelects(["group-owner-select", "group-sender-select"]);
}

G.$("btn-create-group").addEventListener("click", async () => {
  try {
    fillSelects();
    G.markStep("g-step-create", "step-run");

    const ownerId = G.$("group-owner-select").value;
    const name = G.$("group-name").value.trim();
    if (!ownerId || !name) {
      throw new Error("Для создания группы выбери владельца и укажи название.");
    }

    const requestedLogins = G.$("group-members").value
      .split(",")
      .map((x) => x.trim())
      .filter(Boolean);

    const memberIds = requestedLogins
      .map((login) => G.getIdByLogin(login))
      .filter((id) => id !== null);

    if (!memberIds.includes(ownerId)) {
      memberIds.push(ownerId);
    }

    const group = await G.api("/api/messages/group", "POST", {
      ownerId,
      name,
      members: memberIds,
    });

    G.$("group-id").value = group.id;
    G.markStep("g-step-create", "step-ok");
    explain("group create: сервер создал чат и зафиксировал состав участников.");
    setOut({ info: "group created", group });
  } catch (e) {
    setOut(e.message);
    G.markStep("g-step-create", "step-wait");
  }
});

G.$("btn-send-group").addEventListener("click", async () => {
  try {
    G.markStep("g-step-send", "step-run");
    const groupId = G.$("group-id").value.trim();
    const senderId = G.$("group-sender-select").value;
    const text = G.$("group-text").value;
    if (!groupId || !senderId || !text) {
      throw new Error("Для отправки в группу укажи groupId, отправителя и текст.");
    }

    const messages = await G.api(`/api/messages/group/${groupId}/send`, "POST", {
      senderId,
      text,
    });
    G.markStep("g-step-send", "step-ok");
    explain("group send: одно сообщение раскладывается в персональные сообщения участникам.");
    setOut({ info: "group sent", generatedMessages: G.compactMessages(messages) });
  } catch (e) {
    setOut(e.message);
    G.markStep("g-step-send", "step-wait");
  }
});

fillSelects();
setOut("Сначала выполни сценарий A (создание), затем сценарий B (отправка в группу).");


