(() => {
  "use strict";

  const queueInput = document.getElementById("queue-input");
  const queueOptions = document.getElementById("queue-options");
  const filterInput = document.getElementById("filter-input");
  const countBtn = document.getElementById("count-btn");
  const browseBtn = document.getElementById("browse-btn");
  const refreshBtn = document.getElementById("refresh-btn");
  const statusEl = document.getElementById("status");

  const messagesSection = document.getElementById("messages-section");
  const activeQueueNameEl = document.getElementById("active-queue-name");
  const messagesBody = document.getElementById("messages-body");
  const emptyState = document.getElementById("empty-state");
  const selectAllCheckbox = document.getElementById("select-all");
  const selectedCountEl = document.getElementById("selected-count");
  const deleteSelectedBtn = document.getElementById("delete-selected-btn");
  const moveSelectedBtn = document.getElementById("move-selected-btn");
  const targetQueueInput = document.getElementById("target-queue-input");
  const targetQueueOptions = document.getElementById("target-queue-options");

  let currentMessages = [];
  let currentQueue = null;

  function debounce(fn, delayMs) {
    let timer;
    return (...args) => {
      clearTimeout(timer);
      timer = setTimeout(() => fn(...args), delayMs);
    };
  }

  function showStatus(message, kind) {
    statusEl.textContent = message || "";
    statusEl.className = "status" + (kind ? " " + kind : "");
  }

  async function apiRequest(path, options) {
    let response;
    try {
      response = await fetch(path, options);
    } catch (networkError) {
      throw new Error("Network error: " + networkError.message);
    }
    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json") ? await response.json() : null;
    if (!response.ok) {
      const message = body && body.message ? body.message : response.status + " " + response.statusText;
      throw new Error(message);
    }
    return body;
  }

  function populateDatalist(datalistEl, names) {
    datalistEl.innerHTML = "";
    for (const name of names) {
      const option = document.createElement("option");
      option.value = name;
      datalistEl.appendChild(option);
    }
  }

  async function refreshQueueOptions(searchTerm, datalistEl) {
    try {
      const names = await apiRequest("/api/dlq/queues?search=" + encodeURIComponent(searchTerm || ""));
      populateDatalist(datalistEl, names);
    } catch (err) {
      // Typeahead failures shouldn't interrupt the user; surface quietly via console only.
      console.warn("Failed to refresh queue list:", err.message);
    }
  }

  const debouncedQueueRefresh = debounce((value) => refreshQueueOptions(value, queueOptions), 250);
  const debouncedTargetQueueRefresh = debounce((value) => refreshQueueOptions(value, targetQueueOptions), 250);

  queueInput.addEventListener("input", () => debouncedQueueRefresh(queueInput.value));
  queueInput.addEventListener("focus", () => refreshQueueOptions(queueInput.value, queueOptions));
  targetQueueInput.addEventListener("input", () => debouncedTargetQueueRefresh(targetQueueInput.value));
  targetQueueInput.addEventListener("focus", () => refreshQueueOptions(targetQueueInput.value, targetQueueOptions));

  function requireQueueName() {
    const name = queueInput.value.trim();
    if (!name) {
      showStatus("Enter a queue name first.", "error");
      return null;
    }
    return name;
  }

  function buildMessagesUrl(queueName, suffix) {
    const filter = filterInput.value.trim();
    const query = filter ? "?filter=" + encodeURIComponent(filter) : "";
    return "/api/dlq/queues/" + encodeURIComponent(queueName) + "/messages" + suffix + query;
  }

  async function loadCount() {
    const name = requireQueueName();
    if (!name) return;
    showStatus("Counting…");
    try {
      const result = await apiRequest(buildMessagesUrl(name, "/count"));
      showStatus(`${result.count} message(s) on "${name}".`, "success");
    } catch (err) {
      showStatus(err.message, "error");
    }
  }

  async function loadMessages() {
    const name = requireQueueName();
    if (!name) return;
    showStatus("Loading messages…");
    try {
      const messages = await apiRequest(buildMessagesUrl(name, ""));
      currentQueue = name;
      currentMessages = messages;
      activeQueueNameEl.textContent = name;
      messagesSection.hidden = false;
      renderMessages(messages);
      showStatus(`Loaded ${messages.length} message(s) from "${name}".`, "success");
    } catch (err) {
      showStatus(err.message, "error");
    }
  }

  function formatTimestamp(epochMillis) {
    if (!epochMillis) return "-";
    try {
      return new Date(epochMillis).toLocaleString();
    } catch (e) {
      return String(epochMillis);
    }
  }

  function formatOriginalDestination(message) {
    if (message.originalQueue) return message.originalQueue;
    if (message.originalAddress) return message.originalAddress;
    return "-";
  }

  function renderMessages(messages) {
    messagesBody.innerHTML = "";
    emptyState.hidden = messages.length > 0;

    for (const message of messages) {
      const row = document.createElement("tr");

      const checkboxCell = document.createElement("td");
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.className = "row-checkbox";
      checkbox.dataset.messageId = String(message.messageId);
      checkbox.addEventListener("change", updateSelectionState);
      checkboxCell.appendChild(checkbox);
      row.appendChild(checkboxCell);

      row.appendChild(textCell(String(message.messageId)));
      row.appendChild(textCell(String(message.priority)));
      row.appendChild(textCell(message.durable ? "yes" : "no"));
      row.appendChild(textCell(formatTimestamp(message.timestamp)));
      row.appendChild(textCell(formatOriginalDestination(message)));

      const previewCell = textCell(message.textBodyPreview || "(no text body)");
      previewCell.classList.add("preview");
      previewCell.title = message.textBodyPreview || "";
      row.appendChild(previewCell);

      const detailsCell = document.createElement("td");
      const detailsBtn = document.createElement("button");
      detailsBtn.type = "button";
      detailsBtn.textContent = "Details";
      detailsBtn.addEventListener("click", () => toggleDetailsRow(row, message));
      detailsCell.appendChild(detailsBtn);
      row.appendChild(detailsCell);

      messagesBody.appendChild(row);
    }

    updateSelectionState();
  }

  function textCell(text) {
    const cell = document.createElement("td");
    cell.textContent = text;
    return cell;
  }

  function toggleDetailsRow(row, message) {
    const next = row.nextElementSibling;
    if (next && next.classList.contains("details-row")) {
      next.remove();
      return;
    }
    const detailsRow = document.createElement("tr");
    detailsRow.className = "details-row";
    const cell = document.createElement("td");
    cell.colSpan = 8;
    const pre = document.createElement("pre");
    pre.textContent = JSON.stringify(message.properties, null, 2);
    cell.appendChild(pre);
    detailsRow.appendChild(cell);
    row.after(detailsRow);
  }

  function getSelectedIds() {
    return Array.from(document.querySelectorAll(".row-checkbox:checked")).map((cb) => Number(cb.dataset.messageId));
  }

  function updateSelectionState() {
    const selected = getSelectedIds();
    selectedCountEl.textContent = `${selected.length} selected`;
    deleteSelectedBtn.disabled = selected.length === 0;
    moveSelectedBtn.disabled = selected.length === 0;

    const allCheckboxes = document.querySelectorAll(".row-checkbox");
    selectAllCheckbox.checked = allCheckboxes.length > 0 && selected.length === allCheckboxes.length;
    selectAllCheckbox.indeterminate = selected.length > 0 && selected.length < allCheckboxes.length;
  }

  selectAllCheckbox.addEventListener("change", () => {
    document.querySelectorAll(".row-checkbox").forEach((cb) => {
      cb.checked = selectAllCheckbox.checked;
    });
    updateSelectionState();
  });

  function summarizeResult(action, result) {
    const succeeded = result.succeeded.length;
    const failedIds = Object.keys(result.failed);
    if (failedIds.length === 0) {
      return `${action}: ${succeeded} message(s) succeeded.`;
    }
    return `${action}: ${succeeded} succeeded, ${failedIds.length} failed (${failedIds.join(", ")}).`;
  }

  async function deleteSelected() {
    if (!currentQueue) return;
    const ids = getSelectedIds();
    if (ids.length === 0) return;
    if (!window.confirm(`Permanently delete ${ids.length} message(s) from "${currentQueue}"? This cannot be undone.`)) {
      return;
    }
    showStatus("Deleting…");
    try {
      const result = await apiRequest("/api/dlq/queues/" + encodeURIComponent(currentQueue) + "/messages", {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ messageIds: ids }),
      });
      showStatus(summarizeResult("Delete", result), Object.keys(result.failed).length ? "error" : "success");
      await loadMessages();
    } catch (err) {
      showStatus(err.message, "error");
    }
  }

  async function moveSelected() {
    if (!currentQueue) return;
    const ids = getSelectedIds();
    const target = targetQueueInput.value.trim();
    if (ids.length === 0) return;
    if (!target) {
      showStatus("Enter a target queue to move selected messages to.", "error");
      return;
    }
    if (!window.confirm(`Move ${ids.length} message(s) from "${currentQueue}" to "${target}"?`)) {
      return;
    }
    showStatus("Moving…");
    try {
      const result = await apiRequest(
        "/api/dlq/queues/" + encodeURIComponent(currentQueue) + "/messages/move",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ messageIds: ids, targetQueue: target }),
        }
      );
      showStatus(summarizeResult("Move", result), Object.keys(result.failed).length ? "error" : "success");
      await loadMessages();
    } catch (err) {
      showStatus(err.message, "error");
    }
  }

  countBtn.addEventListener("click", loadCount);
  browseBtn.addEventListener("click", loadMessages);
  refreshBtn.addEventListener("click", loadMessages);
  deleteSelectedBtn.addEventListener("click", deleteSelected);
  moveSelectedBtn.addEventListener("click", moveSelected);

  queueInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") loadMessages();
  });

  refreshQueueOptions("", queueOptions);
})();
