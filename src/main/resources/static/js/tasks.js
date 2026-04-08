// ============================================================
// DOCFLOW — Логика поручений
// ============================================================

const Tasks = {

  // Загрузить мои поручения (как исполнителя)
  async loadMyTasks(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';

    try {
      const tasks = await API.tasks.getMyTasks();

      if (!tasks.length) {
        container.innerHTML = `<div class="empty-state"><i class="bi bi-check2-all"></i><p>Нет активных поручений</p></div>`;
        return;
      }

      container.innerHTML = `
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Поручение</th>
                <th>От кого</th>
                <th>Срок</th>
                <th>Статус</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${tasks.map(t => this.renderTaskRow(t, 'assignee')).join('')}
            </tbody>
          </table>
        </div>
      `;
    } catch (e) {
      container.innerHTML = `<div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i>${e.message}</div>`;
    }
  },

  // Загрузить поручения которые я выдал
  async loadIssuedByMe(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';

    try {
      const tasks = await API.tasks.getIssuedByMe();

      if (!tasks.length) {
        container.innerHTML = `<div class="empty-state"><i class="bi bi-list-task"></i><p>Нет выданных поручений</p></div>`;
        return;
      }

      container.innerHTML = `
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Поручение</th>
                <th>Исполнитель</th>
                <th>Срок</th>
                <th>Статус</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${tasks.map(t => this.renderTaskRow(t, 'issuer')).join('')}
            </tbody>
          </table>
        </div>
      `;
    } catch (e) {
      container.innerHTML = `<div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i>${e.message}</div>`;
    }
  },

  // Строка таблицы поручений
  renderTaskRow(t, role) {
    const person = role === 'assignee' ? t.assignedByFullName : t.assignedToFullName;
    const personLabel = role === 'assignee' ? 'От' : 'Кому';
    const deadlineClass = t.overdue && t.status !== 'COMPLETED' && t.status !== 'CANCELLED' ? 'text-danger fw-600' : 'text-sec';

    const actions = role === 'assignee'
      ? this.renderAssigneeActions(t)
      : this.renderIssuerActions(t);

    const docLink = t.documentId
      ? `<div class="doc-row-meta"><a href="/pages/document-view.html?id=${t.documentId}" class="text-muted">→ Документ: ${t.documentTitle || '#' + t.documentId}</a></div>`
      : '';

    return `
      <tr class="${t.overdue && t.status !== 'COMPLETED' && t.status !== 'CANCELLED' ? 'overdue-row' : ''}">
        <td>
          <div class="doc-row-title">${t.title}</div>
          ${t.description ? `<div class="doc-row-meta text-muted">${t.description.substring(0, 80)}${t.description.length > 80 ? '...' : ''}</div>` : ''}
          ${docLink}
        </td>
        <td class="text-sec">${person}</td>
        <td><span class="${deadlineClass}">${formatDate(t.deadline)}</span></td>
        <td>${taskStatusBadge(t.status)}</td>
        <td class="actions">${actions}</td>
      </tr>
    `;
  },

  renderAssigneeActions(t) {
    if (t.status === 'COMPLETED' || t.status === 'CANCELLED') return '';
    const btns = [];
    if (t.status === 'NEW') {
      btns.push(`<button class="btn btn-secondary btn-sm" onclick="Tasks.startTask(${t.id})">В работу</button>`);
    }
    if (t.status === 'IN_PROGRESS' || t.status === 'NEW') {
      btns.push(`<button class="btn btn-success btn-sm" onclick="Tasks.openCompleteModal(${t.id})">Выполнено</button>`);
    }
    return btns.join(' ');
  },

  renderIssuerActions(t) {
    if (t.status === 'CANCELLED') return '';
    if (t.status === 'COMPLETED') {
      return t.report ? `<button class="btn btn-ghost btn-sm" onclick="Tasks.showReport('${encodeURIComponent(t.report)}')">Отчёт</button>` : '';
    }
    return `<button class="btn btn-ghost btn-sm text-danger" onclick="Tasks.cancelTask(${t.id})">Отменить</button>`;
  },

  // Взять в работу
  async startTask(taskId) {
    try {
      await API.tasks.start(taskId);
      showToast('Поручение взято в работу', 'success');
      window.location.reload();
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  // Открыть модалку завершения
  openCompleteModal(taskId) {
    const modal = document.getElementById('complete-modal');
    if (!modal) return;
    modal.dataset.taskId = taskId;
    document.getElementById('complete-report').value = '';
    showModal('complete-modal');
  },

  // Завершить поручение
  async completeTask() {
    const modal = document.getElementById('complete-modal');
    if (!modal) return;

    const taskId = modal.dataset.taskId;
    const report = document.getElementById('complete-report')?.value || '';

    try {
      await API.tasks.complete(taskId, report);
      hideModal('complete-modal');
      showToast('Поручение отмечено выполненным', 'success');
      window.location.reload();
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  // Отменить поручение
  async cancelTask(taskId) {
    confirmAction('Отменить поручение?', async () => {
      try {
        await API.tasks.cancel(taskId);
        showToast('Поручение отменено', 'info');
        window.location.reload();
      } catch (e) {
        showToast(e.message, 'error');
      }
    });
  },

  // Показать отчёт
  showReport(encodedReport) {
    alert(decodeURIComponent(encodedReport));
  },

  // Создать поручение
  async createTask(formData) {
    try {
      await API.tasks.create(formData);
      showToast('Поручение создано', 'success');
      return true;
    } catch (e) {
      showToast(e.message, 'error');
      return false;
    }
  },
};
