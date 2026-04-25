const F = window.DemoUI;

function explain(text) {
  F.setExplain("files-explain", text);
}

function setOut(data) {
  F.setOut("files-out", data);
}

function fillSelects() {
  F.fillManyUserSelects(["file-sender-select", "file-recipient-select"]);
}

function resetSteps() {
  ["f-step-init", "f-step-chunk", "f-step-final", "f-step-verify", "f-step-delivered"]
    .forEach((id) => F.markStep(id, "step-wait"));
}

F.$("btn-upload-file").addEventListener("click", async () => {
  try {
    fillSelects();
    resetSteps();

    const senderId = F.$("file-sender-select").value;
    const recipientId = F.$("file-recipient-select").value;
    const file = F.$("file-input").files[0];
    if (!senderId || !recipientId || !file) {
      throw new Error("Выбери отправителя, получателя и файл перед запуском pipeline.");
    }

    F.markStep("f-step-init", "step-run");
    const init = await F.api("/api/files/init", "POST", {
      senderId,
      recipientId,
      fileName: file.name,
      totalSize: file.size,
    });
    F.markStep("f-step-init", "step-ok");

    F.markStep("f-step-chunk", "step-run");
    const chunk = await F.api(
      `/api/files/${init.id}/chunk?offset=0`,
      "POST",
      await file.arrayBuffer(),
      { "Content-Type": "application/octet-stream" }
    );
    F.markStep("f-step-chunk", "step-ok");

    F.markStep("f-step-final", "step-run");
    const finalized = await F.api(`/api/files/${init.id}/finalize`, "POST");
    F.markStep("f-step-final", "step-ok");

    F.markStep("f-step-verify", "step-run");
    const verified = await F.api(`/api/files/${init.id}/verify?checksum=${finalized.checksumSha256}`);
    F.markStep("f-step-verify", "step-ok");

    F.markStep("f-step-delivered", "step-run");
    await F.api(`/api/files/${init.id}/delivered`, "POST");
    F.markStep("f-step-delivered", "step-ok");

    explain("pipeline: файл загружен, зашифрован, проверен по checksum и отмечен как delivered.");
    setOut({ init, chunk, finalized, verified });
  } catch (e) {
    setOut(e.message);
  }
});

fillSelects();
setOut("Сценарий: подготовка -> запуск pipeline -> наблюдение шагов INIT..DELIVERED.");


